package com.dragonminez.server.events.players.combat;

import com.dragonminez.Reference;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TaiyokenBlindS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TaiyokenHandler {

	public static final String TECHNIQUE_ID = "taiyoken";
	public static final String BLIND_UNTIL_TAG = "dmz_taiyoken_blind_until";

	private static final double RANGE = 20.0;
	private static final int COOLDOWN_TICKS = 45 * 20;
	private static final double FULL_LOOK_DOT = 0.93;
	private static final double PARTIAL_LOOK_DOT = 0.5;
	private static final String CAST_ANIMATION = "ki.solarflare_fire";

	private static final Set<LivingEntity> BLINDED_MOBS = new HashSet<>();

	public static void cast(ServerPlayer player) {
		if (player.level().isClientSide) return;
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			if (!stats.getStatus().isHasCreatedCharacter()) return;
			if (stats.getStatus().isStunned()) return;
			if (stats.getStatus().isFused() && !stats.getStatus().isFusionLeader()) return;
			if (player.isSpectator()) return;

			if (stats.getSkills().getSkillLevel("kicontrol") <= 0) return;
			if (stats.getResources().getPowerRelease() < 5) return;
			if (!player.getMainHandItem().isEmpty()) return;

			TechniqueData unlocked = stats.getTechniques().getUnlockedTechniques().get(TECHNIQUE_ID);
			if (!(unlocked instanceof KiAttackData technique)) return;

			String cooldownKey = "TechniqueCooldown_" + TECHNIQUE_ID;
			if (stats.getCooldowns().hasCooldown(cooldownKey)) return;

			double cost = technique.getCalculatedCost(stats);
			if (!player.isCreative() && stats.getResources().getCurrentEnergy() < cost) return;

			if (!player.isCreative() && cost > 0) stats.getResources().removeEnergy((int) Math.ceil(cost));
			stats.getCooldowns().setCooldown(cooldownKey, COOLDOWN_TICKS);

			int xpGain = technique.getXpGainPerHit();
			if (xpGain > 0) stats.getTechniques().addExperienceToTechnique(TECHNIQUE_ID, xpGain);

			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);

			NetworkHandler.sendToTrackingEntityAndSelf(
					new TriggerAnimationS2C(player.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION, 0, -1, CAST_ANIMATION), player);
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
					MainSounds.KI_EXPLOSION_CHARGE.get(), SoundSource.PLAYERS, 1.2F, 1.6F);

			applyBlind(player);
		});
	}

	private static void applyBlind(ServerPlayer caster) {
		Vec3 casterEye = caster.getEyePosition();
		AABB box = caster.getBoundingBox().inflate(RANGE);

		for (LivingEntity victim : caster.level().getEntitiesOfClass(LivingEntity.class, box,
				e -> e != caster && e.isAlive() && e.isPickable())) {

			double distance = caster.distanceTo(victim);
			if (distance > RANGE) continue;
			if (!victim.hasLineOfSight(caster)) continue;

			Vec3 victimLook = victim.getViewVector(1.0F).normalize();
			Vec3 toCaster = casterEye.subtract(victim.getEyePosition());
			if (toCaster.lengthSqr() < 1.0E-6) continue;
			double dot = victimLook.dot(toCaster.normalize());
			if (dot < PARTIAL_LOOK_DOT) continue;

			boolean fullLook = dot >= FULL_LOOK_DOT;
			double distanceFrac = Mth.clamp(distance / RANGE, 0.0, 1.0);
			double seconds = fullLook ? 12.0 - 3.0 * distanceFrac : 9.0 - 3.0 * distanceFrac;

			if (TargetHelper.getRelation(caster, victim) == TargetHelper.Relation.FRIENDLY) seconds *= 0.5;

			int durationTicks = Math.max(1, (int) Math.round(seconds * 20.0));

			if (victim instanceof ServerPlayer victimPlayer) {
				NetworkHandler.sendToPlayer(new TaiyokenBlindS2C(durationTicks), victimPlayer);
			} else {
				blindMob(victim, durationTicks);
			}
		}
	}

	private static void blindMob(LivingEntity victim, int durationTicks) {
		long until = victim.level().getGameTime() + durationTicks;
		victim.getPersistentData().putLong(BLIND_UNTIL_TAG, until);
		if (victim instanceof Mob mob) mob.setTarget(null);
		BLINDED_MOBS.add(victim);
	}

	public static boolean isBlinded(LivingEntity entity) {
		if (entity == null) return false;
		return entity.getPersistentData().getLong(BLIND_UNTIL_TAG) > entity.level().getGameTime();
	}

	@SubscribeEvent
	public static void onLevelTick(TickEvent.LevelTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
		if (BLINDED_MOBS.isEmpty()) return;

		Iterator<LivingEntity> it = BLINDED_MOBS.iterator();
		while (it.hasNext()) {
			LivingEntity entity = it.next();
			if (entity == null || !entity.isAlive() || entity.isRemoved() || !isBlinded(entity)) {
				it.remove();
				continue;
			}
			if (entity instanceof Mob mob && mob.getTarget() != null) mob.setTarget(null);
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof Player) BLINDED_MOBS.remove(event.getEntity());
	}
}
