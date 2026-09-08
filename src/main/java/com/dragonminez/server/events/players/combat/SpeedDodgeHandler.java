package com.dragonminez.server.events.players.combat;

import com.dragonminez.Reference;
import com.dragonminez.common.config.CombatConfig;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
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
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SpeedDodgeHandler {

	public static final String DODGE_CD = "SpeedDodgeCooldown";
	private static final int MAX_MEDITATION_LEVEL = 10;

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

			double attackerSpeed = getSpeedOf(attacker, config);
			if (attackerSpeed <= 0.0) return;

			double ratio = data.getSpeed() / attackerSpeed;
			if (ratio < getRequiredRatio(meditationLevel, config)) return;

			event.setCanceled(true);
			data.getCooldowns().setCooldown(DODGE_CD, config.getSpeedDodgeCooldownTicks());

			defender.level().playSound(null, defender.getX(), defender.getY(), defender.getZ(),
					MainSounds.EVASION1.get(), SoundSource.PLAYERS,
					1.0F, 1.2F + defender.getRandom().nextFloat() * 0.2F);
			NetworkHandler.sendToTrackingEntityAndSelf(
					new TriggerAnimationS2C(defender.getUUID(), TriggerAnimationS2C.AnimationType.EVASION, 0), defender);
		});
	}

	private static double getRequiredRatio(int meditationLevel, CombatConfig config) {
		double atLevel1 = config.getSpeedDodgeRatioAtLevel1();
		double atMaxLevel = config.getSpeedDodgeRatioAtMaxLevel();
		int clamped = Math.min(Math.max(meditationLevel, 1), MAX_MEDITATION_LEVEL);
		double progress = (clamped - 1.0) / (MAX_MEDITATION_LEVEL - 1.0);
		return atLevel1 + (atMaxLevel - atLevel1) * progress;
	}

	private static double getSpeedOf(LivingEntity entity, CombatConfig config) {
		if (entity instanceof Player player) {
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
			if (data != null && data.getStatus().isHasCreatedCharacter()) return data.getSpeed();
		}
		return config.getNonPlayerAttackerSpeed();
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
