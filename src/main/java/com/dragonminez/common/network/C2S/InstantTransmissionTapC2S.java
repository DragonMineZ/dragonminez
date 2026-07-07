package com.dragonminez.common.network.C2S;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.util.ITTeleportHelper;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.server.events.players.combat.CombatEvent;
import com.dragonminez.server.events.players.combat.DashHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class InstantTransmissionTapC2S {
	private final UUID targetId;

	public InstantTransmissionTapC2S(UUID targetId) {
		this.targetId = targetId;
	}

	public InstantTransmissionTapC2S(FriendlyByteBuf buf) {
		this.targetId = buf.readBoolean() ? buf.readUUID() : null;
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeBoolean(targetId != null);
		if (targetId != null) buf.writeUUID(targetId);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				int skillLevel = data.getSkills().getSkillLevel("instant_transmission");
				if (skillLevel <= 0) return;

				boolean bypassCosts = player.isCreative() || player.isSpectator();
				if (!bypassCosts && data.getCooldowns().hasCooldown(Cooldowns.DASH_CD)) {
					fail(player, "dash_cooldown");
					return;
				}

				ServerLevel level = player.serverLevel();
				LivingEntity finalTarget = null;

				if (targetId != null) {
					if (level.getEntity(targetId) instanceof LivingEntity le && !isBlockedPlayer(data, le)) finalTarget = le;
				} else {
					double range = 25.0 + (skillLevel * 10.0);
					Vec3 eyePos = player.getEyePosition();
					Vec3 viewVec = player.getViewVector(1.0F).normalize();
					AABB searchBox = player.getBoundingBox().inflate(range);

					List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox,
							e -> e != player && e.isAlive() && player.hasLineOfSight(e) && !isBlockedPlayer(data, e)
									&& isNearCrosshair(eyePos, viewVec, e, range));

					LivingEntity recentHit = getRecentHitTarget(player, level);
					if (recentHit != null && candidates.contains(recentHit)) {
						finalTarget = recentHit;
					} else if (!candidates.isEmpty()) {
						finalTarget = candidates.stream()
								.max(Comparator.comparingDouble(InstantTransmissionTapC2S::entityPower)).orElse(null);
					}
				}

				if (finalTarget == null) {
					fail(player, "no_target");
					return;
				}

				double distance = player.position().distanceTo(finalTarget.position());
				int kiCost = DashHandler.getFlyDashKiCost() + ITTeleportHelper.extraKiCostForDistance(distance);
				if (!bypassCosts) {
					if (data.getResources().getCurrentEnergy() < kiCost) {
						fail(player, "no_ki");
						return;
					}
					data.getResources().removeEnergy(kiCost);
					NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(player), player);
				}

				float targetYaw = finalTarget.getYRot();
				double rad = Math.toRadians(targetYaw);
				double xOffset = -Math.sin(rad) * -1.5;
				double zOffset = Math.cos(rad) * -1.5;

				double newX = finalTarget.getX() + xOffset;
				double newY = finalTarget.getY();
				double newZ = finalTarget.getZ() + zOffset;

				player.teleportTo(level, newX, newY, newZ, finalTarget.getYRot(), player.getXRot());
				player.playNotifySound(MainSounds.TP_SHORT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

				if (!bypassCosts) {
					int dashCdTicks = ConfigManager.getCombatConfig().getDashCooldownSeconds() * 20;
					data.getCooldowns().setCooldown(Cooldowns.DASH_CD, dashCdTicks);
					player.addEffect(new MobEffectInstance(MainEffects.DASH_CD.get(), dashCdTicks, 0, false, false, true));
				}
			});
		});
		ctx.get().setPacketHandled(true);
	}

	private static void fail(ServerPlayer player, String reason) {
		player.displayClientMessage(Component.translatable("gui.dragonminez.transmission.fail." + reason), true);
	}

	private static final double CROSSHAIR_COS_THRESHOLD = 0.93;
	private static final long RECENT_HIT_WINDOW_MS = 6000L;

	private static boolean isNearCrosshair(Vec3 eyePos, Vec3 viewVec, LivingEntity entity, double range) {
		Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
		Vec3 toEntity = center.subtract(eyePos);
		double dist = toEntity.length();
		if (dist > range) return false;
		if (dist < 1.0E-4) return true;
		return toEntity.scale(1.0 / dist).dot(viewVec) >= CROSSHAIR_COS_THRESHOLD;
	}

	private static LivingEntity getRecentHitTarget(ServerPlayer player, ServerLevel level) {
		long lastTime = player.getPersistentData().getLong(CombatEvent.DMZ_LAST_HIT_TARGET_TIME_TAG);
		if (lastTime <= 0 || System.currentTimeMillis() - lastTime > RECENT_HIT_WINDOW_MS) return null;
		int id = player.getPersistentData().getInt(CombatEvent.DMZ_LAST_HIT_TARGET_ID_TAG);
		return level.getEntity(id) instanceof LivingEntity le && le.isAlive() ? le : null;
	}

	private static double entityPower(LivingEntity entity) {
		if (entity instanceof ServerPlayer sp) {
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, sp).orElse(null);
			if (data != null) return data.getBattlePowerExact();
		}
		return (long) entity.getMaxHealth();
	}

	private static boolean isBlockedPlayer(StatsData requesterData, LivingEntity entity) {
		if (!(entity instanceof ServerPlayer targetPlayer)) return false;
		StatsData targetData = StatsProvider.get(StatsCapability.INSTANCE, targetPlayer).orElse(null);
		return targetData != null && TransformationsHelper.isInstantTransmissionBlocked(requesterData, targetData);
	}
}