package com.dragonminez.server.events.players.combat;

import com.dragonminez.Reference;
import com.dragonminez.common.combat.SilentDamage;
import com.dragonminez.common.combat.util.MultipartTargeting;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.HellzoneGrenadeEntity;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import com.dragonminez.common.init.entities.ki.KillDriverEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.TriBeamPackets;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KiTechniqueHandler {

	private static final Map<UUID, Integer> FIRE_ANIMATION_UNTIL = new ConcurrentHashMap<>();

	public static void holdFireAnimation(Player player, int ticks) {
		MinecraftServer server = player.getServer();
		if (server == null) return;
		FIRE_ANIMATION_UNTIL.merge(player.getUUID(), server.getTickCount() + ticks, Math::max);
	}

	public static void onLaserFired(Player player, String techniqueId) {
		if (player.level().isClientSide || techniqueId == null) return;
		if (EmperorDeathBeam.is(techniqueId)) EmperorDeathBeam.holdFireAnimation(player);
		else if (Dodonpa.is(techniqueId)) Dodonpa.holdFireAnimation(player);
	}

	public static int getFireDelayTicks(String techniqueId) {
		if (Dodonpa.is(techniqueId)) return Dodonpa.FIRE_DELAY_TICKS;
		return 0;
	}

	public static boolean isFireAnimationHeld(Player player) {
		Integer until = FIRE_ANIMATION_UNTIL.get(player.getUUID());
		if (until == null) return false;
		MinecraftServer server = player.getServer();
		if (server != null && server.getTickCount() < until) return true;
		FIRE_ANIMATION_UNTIL.remove(player.getUUID());
		return false;
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;

		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) {
			clearAll();
			return;
		}

		EmperorDeathBeam.tick(server);
		TriBeam.tick(server);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		clearAll();
	}

	private static void clearAll() {
		FIRE_ANIMATION_UNTIL.clear();
		EmperorDeathBeam.CHAINS.clear();
		TriBeam.FOLLOW_UPS.clear();
	}

	public static final class EmperorDeathBeam {

		public static final String TECHNIQUE_ID = "emperor_death_beam";
		public static final float SIZE_MULTIPLIER = 1.75F;

		private static final int TOTAL_SHOTS = 10;
		private static final int SHOT_INTERVAL_TICKS = 7;
		private static final float FOLLOW_UP_DAMAGE_RATIO = 0.2F;
		private static final int FIRE_ANIMATION_HOLD_TICKS = 14;

		private static final Map<UUID, Chain> CHAINS = new ConcurrentHashMap<>();

		private EmperorDeathBeam() {}

		private static final class Chain {
			final ResourceKey<Level> dimension;
			final UUID ownerId;
			final float damage;
			final float speed;
			final int color;
			final int colorBorder;
			final int colorOutline;
			final float size;
			final int armorPenetration;
			final int kiType;
			int shotsFired = 1;
			int ticksUntilNext = SHOT_INTERVAL_TICKS;

			Chain(LivingEntity owner, KiLaserEntity leader) {
				this.dimension = owner.level().dimension();
				this.ownerId = owner.getUUID();
				this.damage = leader.getKiDamage() * FOLLOW_UP_DAMAGE_RATIO;
				this.speed = leader.getKiSpeed();
				this.color = leader.getColor();
				this.colorBorder = leader.getColorBorder();
				this.colorOutline = leader.getColorOutline();
				this.size = leader.getSize();
				this.armorPenetration = leader.getArmorPenetration();
				this.kiType = leader.getKiType().ordinal();
			}
		}

		public static boolean is(String techniqueId) {
			return TECHNIQUE_ID.equals(techniqueId);
		}

		public static void start(LivingEntity owner, KiLaserEntity leader) {
			if (owner.level().isClientSide) return;
			CHAINS.put(owner.getUUID(), new Chain(owner, leader));
		}

		public static void holdFireAnimation(Player player) {
			KiTechniqueHandler.holdFireAnimation(player, FIRE_ANIMATION_HOLD_TICKS);
		}

		private static void tick(MinecraftServer server) {
			if (CHAINS.isEmpty()) return;

			Iterator<Map.Entry<UUID, Chain>> it = CHAINS.entrySet().iterator();
			while (it.hasNext()) {
				Chain chain = it.next().getValue();
				if (--chain.ticksUntilNext > 0) continue;

				if (!fireNextShot(server, chain)) {
					it.remove();
					continue;
				}

				chain.shotsFired++;
				if (chain.shotsFired >= TOTAL_SHOTS) it.remove();
				else chain.ticksUntilNext = SHOT_INTERVAL_TICKS;
			}
		}

		private static boolean fireNextShot(MinecraftServer server, Chain chain) {
			ServerLevel level = server.getLevel(chain.dimension);
			if (level == null) return false;

			Entity ownerEntity = level.getEntity(chain.ownerId);
			if (!(ownerEntity instanceof LivingEntity owner) || !owner.isAlive()) return false;

			KiLaserEntity shot = new KiLaserEntity(level, owner);
			shot.setupEmperorChainShot(owner, chain.damage, chain.speed, chain.color, chain.colorBorder,
					chain.colorOutline, chain.size, chain.armorPenetration, chain.kiType);
			level.addFreshEntity(shot);
			return true;
		}
	}

	public static final class HeatDome {

		public static final String TECHNIQUE_ID = "heat_dome";
		public static final float CHARGE_SPHERE_SIZE = 1.0F;
		public static final float BEAM_HEAD_OFFSET = 0.3F;

		private static final double LAUNCH_RANGE = 8.0D;
		private static final double LAUNCH_MIN_FACING = 0.3D;
		private static final double LAUNCH_UP_SPEED = 1.6D;
		private static final double LAUNCH_PULL = 0.09D;

		private HeatDome() {}

		public static boolean is(String techniqueId) {
			return TECHNIQUE_ID.equals(techniqueId);
		}

		public static void launchTarget(LivingEntity owner, AbstractKiProjectile attack) {
			LivingEntity target = findLaunchTarget(owner, attack);
			if (target == null) return;

			Vec3 pull = new Vec3(owner.getX() - target.getX(), 0.0D, owner.getZ() - target.getZ()).scale(LAUNCH_PULL);
			target.setDeltaMovement(pull.x, LAUNCH_UP_SPEED, pull.z);
			target.hasImpulse = true;
			target.hurtMarked = true;
			target.fallDistance = 0.0F;
		}

		private static LivingEntity findLaunchTarget(LivingEntity owner, AbstractKiProjectile attack) {
			Vec3 look = owner.getLookAngle();
			Vec3 flatLook = new Vec3(look.x, 0.0D, look.z);
			if (flatLook.lengthSqr() < 1.0E-4D) flatLook = Vec3.directionFromRotation(0.0F, owner.getYRot());
			flatLook = flatLook.normalize();

			AABB area = owner.getBoundingBox().inflate(LAUNCH_RANGE);
			LivingEntity best = null;
			double bestDistance = Double.MAX_VALUE;
			for (LivingEntity candidate : MultipartTargeting.collectTargets(owner.level(), area)) {
				if (candidate == owner || !candidate.isAlive() || !attack.shouldDamage(candidate)) continue;

				Vec3 toCandidate = new Vec3(candidate.getX() - owner.getX(), 0.0D, candidate.getZ() - owner.getZ());
				double distance = toCandidate.length();
				if (distance > LAUNCH_RANGE) continue;
				if (distance > 0.5D && toCandidate.scale(1.0D / distance).dot(flatLook) < LAUNCH_MIN_FACING) continue;

				if (distance < bestDistance) {
					bestDistance = distance;
					best = candidate;
				}
			}
			return best;
		}
	}

	public static final class TriBeam {

		public static final String KIKOHO_ID = "kikoho";
		public static final String NEO_KIKOHO_ID = "neo_kikoho";
		public static final float TRIANGLE_SCALE = 4.8F;
		public static final float TRIANGLE_DEPTH = 0.6F;
		public static final int NEO_CHARGE_TICKS = 40;
		public static final float CHARGE_BALL_RADIUS = 0.3F;
		public static final float CHARGE_LIGHTNING_RADIUS = 0.75F;
		public static final float CHARGE_FORWARD_OFFSET = 0.6F;
		public static final int LIGHTNING_COLOR = 0x6FE8FF;
		public static final int LIGHTNING_COLOR_DEEP = 0x1F7BFF;
		public static final float BLOCK_BREAK_RADIUS = 0.6F;

		private static final int FOLLOW_UP_WINDOW_TICKS = 60;
		private static final int FOLLOW_UP_MIN_INTERVAL_TICKS = 8;
		private static final float HEALTH_COST_RATIO = 0.10F;
		private static final float MIN_HEALTH = 1.0F;
		private static final float FOLLOW_UP_MIN_HEALTH_RATIO = 0.20F;

		private static final Map<UUID, FollowUp> FOLLOW_UPS = new ConcurrentHashMap<>();

		private TriBeam() {}

		private static final class FollowUp {
			final float damage;
			final float speed;
			final float size;
			final int color;
			final int colorBorder;
			final int colorOutline;
			final int armorPenetration;
			final int kiType;
			final int maxLife;
			final String techniqueId;
			final int expiresTick;
			final int nextShotTick;

			FollowUp(KiBlastEntity shot, int maxLife, int now) {
				this.damage = shot.getKiDamage();
				this.speed = shot.getKiSpeed();
				this.size = shot.getSize();
				this.color = shot.getColor();
				this.colorBorder = shot.getColorBorder();
				this.colorOutline = shot.getColorOutline();
				this.armorPenetration = shot.getArmorPenetration();
				this.kiType = shot.getKiType().ordinal();
				this.maxLife = maxLife;
				this.techniqueId = shot.getTechniqueId();
				this.expiresTick = now + FOLLOW_UP_WINDOW_TICKS;
				this.nextShotTick = now + FOLLOW_UP_MIN_INTERVAL_TICKS;
			}
		}

		public static boolean is(String techniqueId) {
			return KIKOHO_ID.equals(techniqueId) || NEO_KIKOHO_ID.equals(techniqueId);
		}

		public static boolean isNeo(String techniqueId) {
			return NEO_KIKOHO_ID.equals(techniqueId);
		}

		public static void onNeoFired(ServerPlayer player, KiBlastEntity shot, int maxLife) {
			MinecraftServer server = player.getServer();
			if (server == null) return;

			if (canAffordFollowUp(player)) {
				FOLLOW_UPS.put(player.getUUID(), new FollowUp(shot, maxLife, server.getTickCount()));
				NetworkHandler.sendToPlayer(new TriBeamPackets.FollowUpWindowS2C(FOLLOW_UP_WINDOW_TICKS), player);
			} else {
				close(player);
			}
		}

		public static void requestFollowUp(ServerPlayer player) {
			MinecraftServer server = player.getServer();
			FollowUp followUp = FOLLOW_UPS.get(player.getUUID());
			if (server == null || followUp == null) return;

			int now = server.getTickCount();
			if (now > followUp.expiresTick || !player.isAlive()) {
				close(player);
				return;
			}
			if (now < followUp.nextShotTick) return;
			if (!canAffordFollowUp(player)) {
				close(player);
				return;
			}

			if (!player.isCreative()) {
				float cost = player.getMaxHealth() * HEALTH_COST_RATIO;
				SilentDamage.apply(player, cost, MIN_HEALTH);
			}

			KiBlastEntity shot = new KiBlastEntity(player.level(), player);
			shot.setupTriBeamPlayer(player, followUp.damage, followUp.speed, followUp.color, followUp.colorBorder,
					followUp.colorOutline, followUp.size, false);
			shot.setFiring(true);
			shot.setKiType(followUp.kiType);
			shot.setTechniqueId(followUp.techniqueId);
			shot.setArmorPenetration(followUp.armorPenetration);
			player.level().addFreshEntity(shot);
			shot.fireHability(followUp.maxLife);
		}

		private static boolean canAffordFollowUp(ServerPlayer player) {
			return player.isCreative() || player.getHealth() >= player.getMaxHealth() * FOLLOW_UP_MIN_HEALTH_RATIO;
		}

		private static void close(ServerPlayer player) {
			FOLLOW_UPS.remove(player.getUUID());
			NetworkHandler.sendToPlayer(new TriBeamPackets.FollowUpWindowS2C(0), player);
		}

		private static void tick(MinecraftServer server) {
			if (FOLLOW_UPS.isEmpty()) return;
			int now = server.getTickCount();
			FOLLOW_UPS.entrySet().removeIf(entry -> now > entry.getValue().expiresTick);
		}
	}

	public static final class Dodonpa {

		public static final String TECHNIQUE_ID = "dodonpa";

		private static final int FIRE_DELAY_TICKS = 2;
		private static final int FIRE_ANIMATION_HOLD_TICKS = 12;

		private Dodonpa() {}

		public static boolean is(String techniqueId) {
			return TECHNIQUE_ID.equals(techniqueId);
		}

		public static void holdFireAnimation(Player player) {
			KiTechniqueHandler.holdFireAnimation(player, FIRE_ANIMATION_HOLD_TICKS);
		}
	}

	public static final class KillDriver {

		public static final String TECHNIQUE_ID = KillDriverEntity.TECHNIQUE_ID;

		private static final float MIN_RADIUS_SCALE = 0.75F;
		private static final float MAX_RADIUS_SCALE = 1.5F;

		private KillDriver() {}

		public static boolean is(String techniqueId) {
			return TECHNIQUE_ID.equals(techniqueId);
		}

		public static float radiusScale(float castSize) {
			return Mth.clamp(castSize, MIN_RADIUS_SCALE, MAX_RADIUS_SCALE);
		}
	}

	public static final class HellzoneGrenade {

		public static final String TECHNIQUE_ID = HellzoneGrenadeEntity.TECHNIQUE_ID;
		private static final String NO_TARGET_MESSAGE = "message.dragonminez.hellzone_grenade.no_target";

		private HellzoneGrenade() {}

		public static boolean is(String techniqueId) {
			return TECHNIQUE_ID.equals(techniqueId);
		}

		public static boolean hasTarget(LivingEntity caster, int lockedTargetId) {
			if (HellzoneGrenadeEntity.findTarget(caster, lockedTargetId) != null) return true;
			notifyNoTarget(caster);
			return false;
		}

		public static void notifyNoTarget(LivingEntity caster) {
			if (caster instanceof Player player) player.displayClientMessage(Component.translatable(NO_TARGET_MESSAGE), true);
		}
	}
}
