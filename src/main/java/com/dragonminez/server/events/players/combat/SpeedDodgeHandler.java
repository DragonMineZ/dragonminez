package com.dragonminez.server.events.players.combat;

import com.dragonminez.Reference;
import com.dragonminez.common.config.CombatConfig;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Cooldowns;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SpeedDodgeHandler {

	public static final String DODGE_CD = "SpeedDodgeCooldown";
	private static final int MAX_MEDITATION_LEVEL = 10;

	private static final int VARIANT_FRONT = 1;
	private static final int VARIANT_BACK = 2;
	private static final int VARIANT_LEFT = 3;
	private static final int VARIANT_RIGHT = 4;

	private static final Map<UUID, Streak> STREAKS = new ConcurrentHashMap<>();

	private record Streak(int count, long lastTick) {}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onLivingAttack(LivingAttackEvent event) {
		if (event.getEntity().level().isClientSide()) return;
		if (!(event.getEntity() instanceof ServerPlayer defender)) return;

		CombatConfig config = ConfigManager.getCombatConfig();
		if (config == null || !config.getEnableSpeedSystem() || !config.getEnableSpeedDodge()) return;

		DamageSource source = event.getSource();
		if (!isBasicMelee(source)) return;

		if (!(source.getEntity() instanceof LivingEntity attacker)) return;
		if (attacker == defender) return;

		StatsProvider.get(StatsCapability.INSTANCE, defender).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;
			if (data.getStatus().isStunned()) return;
			if (defender.hasEffect(MainEffects.STUN.get())) return;
			if (defender.isSpectator()) return;

			if (data.getCooldowns().hasCooldown(Cooldowns.DASH_ACTIVE)) return;
			if (data.getCooldowns().hasCooldown(DODGE_CD)) return;

			int meditationLevel = data.getSkills().getSkillLevel("meditation");
			if (meditationLevel <= 0) return;

			double chance = dodgeChance(data, attacker, meditationLevel, config);
			if (chance <= 0.0) return;

			long now = defender.level().getGameTime();
			Streak streak = STREAKS.get(defender.getUUID());
			int streakCount = streak != null && now - streak.lastTick() <= config.getSpeedDodgeStreakResetTicks() ? streak.count() : 0;
			chance *= Math.pow(config.getSpeedDodgeStreakMultiplier(), streakCount);

			float staminaCost = (float) (data.getMaxStamina() * config.getSpeedDodgeStaminaCostPct());
			if (data.getResources().getCurrentStamina() < staminaCost) return;

			if (defender.getRandom().nextDouble() >= chance) return;

			event.setCanceled(true);
			data.getCooldowns().setCooldown(DODGE_CD, config.getSpeedDodgeCooldownTicks());
			if (staminaCost > 0) data.getResources().removeStamina(staminaCost);
			STREAKS.put(defender.getUUID(), new Streak(streakCount + 1, now));

			defender.level().playSound(null, defender.getX(), defender.getY(), defender.getZ(),
					MainSounds.EVASION1.get(), SoundSource.PLAYERS,
					1.0F, 1.2F + defender.getRandom().nextFloat() * 0.2F);
			NetworkHandler.sendToTrackingEntityAndSelf(
					new TriggerAnimationS2C(defender.getUUID(), TriggerAnimationS2C.AnimationType.EVASION, dodgeVariant(defender, attacker)), defender);
			NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(defender), defender);
		});
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		STREAKS.remove(event.getEntity().getUUID());
	}

	public static double maxChance(int meditationLevel, boolean againstPlayers, CombatConfig config) {
		double cap = againstPlayers ? config.getSpeedDodgeMaxChancePvp() : config.getSpeedDodgeMaxChancePve();
		double floor = config.getSpeedDodgeMeditationChanceFloor();
		return cap * (floor + (1.0 - floor) * meditationProgress(meditationLevel));
	}

	private static double dodgeChance(StatsData data, LivingEntity attacker, int meditationLevel, CombatConfig config) {
		if (attacker instanceof Player player) {
			StatsData attackerData = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
			if (attackerData != null && attackerData.getStatus().isHasCreatedCharacter()) {
				return speedChance(data.getSpeed(), attackerData.getSpeed(), meditationLevel, true, config);
			}
		}

		double attackerBp = attacker instanceof IBattlePower bp ? bp.getBattlePower() : 0.0;
		if (attackerBp <= 0.0) {
			return speedChance(data.getSpeed(), config.getNonPlayerAttackerSpeed(), meditationLevel, false, config);
		}

		double required = lerpByMeditation(config.getSpeedDodgeBpRatioAtLevel1(), config.getSpeedDodgeBpRatioAtMaxLevel(), meditationLevel);
		double ratio = data.getBattlePower() / attackerBp;
		if (ratio < required) return 0.0;

		double progress = Math.log(ratio / required) / Math.log(config.getSpeedDodgeFullChanceBpRatio());
		return scaleChance(progress, meditationLevel, false, config);
	}

	private static double speedChance(double defenderSpeed, double attackerSpeed, int meditationLevel, boolean againstPlayers, CombatConfig config) {
		if (attackerSpeed <= 0.0) return 0.0;

		double required = lerpByMeditation(config.getSpeedDodgeRatioAtLevel1(), config.getSpeedDodgeRatioAtMaxLevel(), meditationLevel);
		double ratio = defenderSpeed / attackerSpeed;
		if (ratio < required) return 0.0;

		double progress = Math.log(ratio / required) / Math.log(config.getSpeedDodgeFullChanceSpeedRatio());
		return scaleChance(progress, meditationLevel, againstPlayers, config);
	}

	private static double scaleChance(double progress, int meditationLevel, boolean againstPlayers, CombatConfig config) {
		double max = maxChance(meditationLevel, againstPlayers, config);
		double base = Math.min(config.getSpeedDodgeBaseChance(), max);
		double t = Math.max(0.0, Math.min(1.0, progress));
		return base + (max - base) * t;
	}

	private static double lerpByMeditation(double atLevel1, double atMaxLevel, int meditationLevel) {
		return atLevel1 + (atMaxLevel - atLevel1) * meditationProgress(meditationLevel);
	}

	private static double meditationProgress(int meditationLevel) {
		int clamped = Math.min(Math.max(meditationLevel, 1), MAX_MEDITATION_LEVEL);
		return (clamped - 1.0) / (MAX_MEDITATION_LEVEL - 1.0);
	}

	private static int dodgeVariant(LivingEntity defender, LivingEntity attacker) {
		Vec3 toAttacker = new Vec3(attacker.getX() - defender.getX(), 0.0, attacker.getZ() - defender.getZ());
		if (toAttacker.lengthSqr() < 1.0E-6) return VARIANT_BACK;
		toAttacker = toAttacker.normalize();

		Vec3 look = Vec3.directionFromRotation(0.0F, defender.getYRot());
		Vec3 right = new Vec3(-look.z, 0.0, look.x);

		double forward = look.dot(toAttacker);
		double side = right.dot(toAttacker);

		if (Math.abs(forward) >= Math.abs(side)) return forward > 0.0 ? VARIANT_BACK : VARIANT_FRONT;
		return side > 0.0 ? VARIANT_LEFT : VARIANT_RIGHT;
	}

	private static boolean isBasicMelee(DamageSource source) {
		if (source == null) return false;
		if (source.getEntity() == null) return false;

		Entity direct = source.getDirectEntity();
		if (direct == null || direct != source.getEntity()) return false;

		return source.is(DamageTypes.PLAYER_ATTACK)
				|| source.is(DamageTypes.MOB_ATTACK)
				|| source.is(DamageTypes.MOB_ATTACK_NO_AGGRO);
	}
}
