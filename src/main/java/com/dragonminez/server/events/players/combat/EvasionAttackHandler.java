package com.dragonminez.server.events.players.combat;

import com.dragonminez.Reference;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.combat.util.MultipartTargeting;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.RageScreamVfxS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TaiyokenBlindS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.racial.RacialStatUtil;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.EvasionAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EvasionAttackHandler {

	public static final String BLIND_UNTIL_TAG = "dmz_taiyoken_blind_until";

	private static final double TAIYOKEN_RANGE = 20.0;
	private static final double TAIYOKEN_FULL_LOOK_DOT = 0.93;
	private static final double TAIYOKEN_PARTIAL_LOOK_DOT = 0.5;

	private static final int RAGE_SCREAM_PULSE_INTERVAL_TICKS = 5;
	private static final double RAGE_SCREAM_BASE_RANGE = 5.0;
	private static final float DEFAULT_HITBOX = 1.8f;
	private static final double RAGE_SCREAM_PULSE_KNOCKBACK = 0.35;
	private static final double RAGE_SCREAM_VERTICAL_LIFT = 0.12;

	private static final Set<LivingEntity> BLINDED_MOBS = new HashSet<>();
	private static final Map<UUID, ActiveEvasion> ACTIVE = new ConcurrentHashMap<>();

	private static final class ActiveEvasion {
		final String techniqueId;
		int ticksRemaining;
		int ticksSincePulse;

		ActiveEvasion(String techniqueId, int totalTicks) {
			this.techniqueId = techniqueId;
			this.ticksRemaining = totalTicks;
			this.ticksSincePulse = 0;
		}
	}

	public static void cast(ServerPlayer player, String techniqueId) {
		if (player.level().isClientSide || techniqueId == null || techniqueId.isEmpty()) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			if (!stats.getStatus().isHasCreatedCharacter()) return;
			if (stats.getStatus().isStunned()) return;
			if (stats.getStatus().isFused() && !stats.getStatus().isFusionLeader()) return;
			if (player.isSpectator()) return;
			if (!player.getMainHandItem().isEmpty()) return;

			TechniqueData unlocked = stats.getTechniques().getUnlockedTechniques().get(techniqueId);
			if (!(unlocked instanceof EvasionAttackData technique)) return;

			String cooldownKey = "TechniqueCooldown_" + techniqueId;
			if (stats.getCooldowns().hasCooldown(cooldownKey)) return;

			double cost = technique.getCalculatedCost(stats);
			if (!player.isCreative() && stats.getResources().getCurrentEnergy() < cost) return;
			if (!player.isCreative() && cost > 0) stats.getResources().removeEnergy((int) Math.ceil(cost));
			stats.getCooldowns().setCooldown(cooldownKey, technique.getActualCooldown());

			int xpGain = RacialStatUtil.applyTechniqueXpBonus(stats, technique.getXpGainPerHit());
			if (xpGain > 0) stats.getTechniques().addExperienceToTechnique(techniqueId, xpGain);

			int durationTicks = technique.getActualDurationTicks();
			stats.getStatus().setEvasionLockTicks(durationTicks);

			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			NetworkHandler.sendToTrackingEntityAndSelf(
					new TriggerAnimationS2C(player.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION, 0, -1, technique.getAnimationId()), player);

			ACTIVE.put(player.getUUID(), new ActiveEvasion(techniqueId, durationTicks));

			switch (techniqueId) {
				case "taiyoken" -> castTaiyoken(player);
				case "rage_scream" -> castRageScream(player, durationTicks);
				default -> { }
			}
		});
	}

	private static void castTaiyoken(ServerPlayer caster) {
		caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				MainSounds.KI_EXPLOSION_CHARGE.get(), SoundSource.PLAYERS, 1.2F, 1.6F);
		applyTaiyokenBlind(caster);
	}

	private static void applyTaiyokenBlind(ServerPlayer caster) {
		Vec3 casterEye = caster.getEyePosition();
		AABB box = caster.getBoundingBox().inflate(TAIYOKEN_RANGE);

		for (LivingEntity victim : caster.level().getEntitiesOfClass(LivingEntity.class, box,
				e -> e != caster && e.isAlive() && e.isPickable())) {

			double distance = caster.distanceTo(victim);
			if (distance > TAIYOKEN_RANGE) continue;
			if (!victim.hasLineOfSight(caster)) continue;

			Vec3 victimLook = victim.getViewVector(1.0F).normalize();
			Vec3 toCaster = casterEye.subtract(victim.getEyePosition());
			if (toCaster.lengthSqr() < 1.0E-6) continue;
			double dot = victimLook.dot(toCaster.normalize());
			if (dot < TAIYOKEN_PARTIAL_LOOK_DOT) continue;

			boolean fullLook = dot >= TAIYOKEN_FULL_LOOK_DOT;
			double distanceFrac = Mth.clamp(distance / TAIYOKEN_RANGE, 0.0, 1.0);
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

	private static void castRageScream(ServerPlayer caster, int durationTicks) {
		caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				MainSounds.OOZARU_GROWL_PLAYER.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
		NetworkHandler.sendToTrackingEntityAndSelf(new RageScreamVfxS2C(caster.getId(), durationTicks), caster);
	}

	private static void rageScreamPulse(ServerPlayer caster, EvasionAttackData technique) {
		StatsProvider.get(StatsCapability.INSTANCE, caster).ifPresent(stats -> {
			float hitbox = Math.max(caster.getBbWidth(), caster.getBbHeight());
			double range = RAGE_SCREAM_BASE_RANGE * (hitbox / DEFAULT_HITBOX);

			float pulseDamage = Math.max(1.0f, (float) technique.getActualHitDamage(stats));

			AABB box = caster.getBoundingBox().inflate(range);
			for (LivingEntity victim : MultipartTargeting.collectTargets(caster.level(), box)) {
				if (victim == caster || !victim.isAlive() || !victim.isPickable()) continue;
				if (victim instanceof Player && TargetHelper.getRelation(caster, victim) == TargetHelper.Relation.FRIENDLY) continue;

				double distance = caster.distanceTo(victim);
				if (distance > range) continue;

				if (pulseDamage > 0) {
					victim.invulnerableTime = 0;
					victim.hurt(MainDamageTypes.kiblast(caster.level(), caster, caster), pulseDamage);
				}

				Vec3 dir = victim.position().subtract(caster.position());
				if (dir.lengthSqr() < 1.0E-6) dir = new Vec3(1, 0, 0);
				dir = dir.normalize();
				double falloff = 1.0 - Mth.clamp(distance / range, 0.0, 1.0) * 0.5;
				Vec3 velocity = new Vec3(dir.x, 0, dir.z).scale(RAGE_SCREAM_PULSE_KNOCKBACK * falloff).add(0, RAGE_SCREAM_VERTICAL_LIFT, 0);
				KnockbackHelper.apply(victim, velocity);
			}
		});
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
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) return;

		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) return;

		Iterator<Map.Entry<UUID, ActiveEvasion>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, ActiveEvasion> entry = it.next();
			ActiveEvasion active = entry.getValue();
			ServerPlayer caster = server.getPlayerList().getPlayer(entry.getKey());

			if (caster == null || !caster.isAlive()) {
				it.remove();
				continue;
			}

			if ("rage_scream".equals(active.techniqueId)) {
				active.ticksSincePulse++;
				if (active.ticksSincePulse >= RAGE_SCREAM_PULSE_INTERVAL_TICKS) {
					active.ticksSincePulse = 0;
					StatsProvider.get(StatsCapability.INSTANCE, caster).ifPresent(stats -> {
						TechniqueData unlocked = stats.getTechniques().getUnlockedTechniques().get("rage_scream");
						if (unlocked instanceof EvasionAttackData technique) {
							rageScreamPulse(caster, technique);
						}
					});
				}
			}

			active.ticksRemaining--;
			if (active.ticksRemaining <= 0) {
				StatsProvider.get(StatsCapability.INSTANCE, caster).ifPresent(stats -> {
					stats.getStatus().setEvasionLockTicks(0);
					NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(caster), caster);
				});
				NetworkHandler.sendToTrackingEntityAndSelf(
						new TriggerAnimationS2C(caster.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP, 0), caster);
				it.remove();
			}
		}
	}

	@SubscribeEvent
	public static void onLivingAttack(LivingAttackEvent event) {
		if (event.getEntity().level().isClientSide()) return;
		if (event.getSource().getEntity() == null) return;
		if (ACTIVE.containsKey(event.getEntity().getUUID())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		Player player = event.getEntity();
		BLINDED_MOBS.remove(player);
		if (ACTIVE.remove(player.getUUID()) != null) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> stats.getStatus().setEvasionLockTicks(0));
		}
	}
}
