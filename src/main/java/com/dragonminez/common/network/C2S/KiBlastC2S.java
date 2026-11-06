package com.dragonminez.common.network.C2S;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.compat.SableCompat;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.training.MinigameSessionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import com.dragonminez.compat.network.NetworkEvent;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class KiBlastC2S {
	public static final int VOLLEY_SHOTS = 6;
	public static final int VOLLEY_INTERVAL_TICKS = 3;
	public static final int VOLLEY_WINDUP_TICKS = 4;
	public static final int VOLLEY_RECOVERY_TICKS = 4;
	public static final int VOLLEY_COOLDOWN_TICKS = 600;
	private static final int SINGLE_COOLDOWN_TICKS = 32;
	private static final int VOLLEY_COST_MULTIPLIER = 3;
	private static final float VOLLEY_DAMAGE_MULTIPLIER = 0.6F;
	private static final float HAND_OFFSET = 0.35F;
	private static final float VOLLEY_SPREAD = 2.5F;
	private static final String VOLLEY_ANIMATION = "ki.barrage_fire";

	private static final Map<UUID, Volley> ACTIVE_VOLLEYS = new HashMap<>();
	private static final Set<UUID> LEFT_HAND_NEXT = new HashSet<>();

	private final boolean isShooting;
	private final boolean volley;
	private final int colorMain;
	private final int colorBorder;
	private final float aimX;
	private final float aimY;
	private final float aimZ;

	public KiBlastC2S(boolean isShooting, int colorMain, int colorBorder) {
		this(isShooting, false, colorMain, colorBorder, Vec3.ZERO);
	}

	public KiBlastC2S(boolean isShooting, int colorMain, int colorBorder, Vec3 aim) {
		this(isShooting, false, colorMain, colorBorder, aim);
	}

	public KiBlastC2S(boolean isShooting, boolean volley, int colorMain, int colorBorder) {
		this(isShooting, volley, colorMain, colorBorder, Vec3.ZERO);
	}

	public KiBlastC2S(boolean isShooting, boolean volley, int colorMain, int colorBorder, Vec3 aim) {
		this.isShooting = isShooting;
		this.volley = volley;
		this.colorMain = colorMain;
		this.colorBorder = colorBorder;
		this.aimX = (float) aim.x;
		this.aimY = (float) aim.y;
		this.aimZ = (float) aim.z;
	}

	public KiBlastC2S(FriendlyByteBuf buffer) {
		this.isShooting = buffer.readBoolean();
		this.volley = buffer.readBoolean();
		this.colorMain = buffer.readInt();
		this.colorBorder = buffer.readInt();
		this.aimX = buffer.readFloat();
		this.aimY = buffer.readFloat();
		this.aimZ = buffer.readFloat();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBoolean(isShooting);
		buffer.writeBoolean(volley);
		buffer.writeInt(colorMain);
		buffer.writeInt(colorBorder);
		buffer.writeFloat(aimX);
		buffer.writeFloat(aimY);
		buffer.writeFloat(aimZ);
	}

	public static void handle(KiBlastC2S msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;
			if (player.isDeadOrDying() || player.isSpectator()) return;

			if (msg.isShooting) {
				MinigameSessionManager.onPlayerAction(player, "ki blast");
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
					if (ACTIVE_VOLLEYS.containsKey(player.getUUID())) return;
					if (data.getCooldowns().hasCooldown(Cooldowns.KI_BLAST_CD)) return;
					if (data.getStatus().isStunned()) return;
					if (!data.getSkills().hasSkill("kicontrol")) return;
					int baseCost = ConfigManager.getCombatConfig().getBaselineFormDrain();

					int cost = (int) (data.getMaxEnergy() * 0.02 + 0.1 * baseCost);
					boolean volley = msg.volley && !data.getCooldowns().hasCooldown(Cooldowns.KI_VOLLEY_CD);
					if (volley) cost *= VOLLEY_COST_MULTIPLIER;
					if (player.isCreative()) cost = 0;
					if (data.getResources().getCurrentEnergy() < cost) return;
					data.getResources().removeEnergy(cost);

					float damage = (float) (data.getKiDamage() * (0.5f + (0.05f * data.getSkills().getSkillLevel("kimanipulation"))));

					if (volley) {
						int lockTicks = VOLLEY_WINDUP_TICKS + VOLLEY_SHOTS * VOLLEY_INTERVAL_TICKS + VOLLEY_RECOVERY_TICKS;
						setKiBlastCooldown(player, data, lockTicks + SINGLE_COOLDOWN_TICKS / 2);
						data.getCooldowns().setCooldown(Cooldowns.KI_VOLLEY_CD, VOLLEY_COOLDOWN_TICKS);
						ACTIVE_VOLLEYS.put(player.getUUID(), new Volley(damage * VOLLEY_DAMAGE_MULTIPLIER, msg.colorMain, msg.colorBorder));
						NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION, 1, -1, VOLLEY_ANIMATION), player);
						NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
						return;
					}

					setKiBlastCooldown(player, data, SINGLE_COOLDOWN_TICKS);

					KiBlastEntity kiBlast = new KiBlastEntity(player.level(), player);
					kiBlast.setup(player, damage, 0.5F, 1.5f, msg.colorMain, msg.colorBorder);
					boolean leftHand = LEFT_HAND_NEXT.remove(player.getUUID());
					if (!leftHand) LEFT_HAND_NEXT.add(player.getUUID());
					Vec3 worldAim = new Vec3(msg.aimX, msg.aimY, msg.aimZ);
					if (!Double.isFinite(worldAim.x) || !Double.isFinite(worldAim.y) || !Double.isFinite(worldAim.z)
							|| worldAim.lengthSqr() < 1.0E-6D) {
						worldAim = player.getLookAngle();
					} else {
						worldAim = worldAim.normalize();
					}
					Vec3 localAim = SableCompat.worldDirectionToEntitySpace(player, worldAim).normalize();
					Vec3 right = Vec3.directionFromRotation(0.0F, player.getYRot() + 90.0F);
					double side = (leftHand ? -1.0 : 1.0) * HAND_OFFSET;
					Vec3 localSpawnOffset = SableCompat.worldDirectionToEntitySpace(
							player, worldAim.scale(0.5D).add(0.0D, -0.4D, 0.0D)).add(right.scale(side));
					Vec3 spawn = player.getEyePosition().add(localSpawnOffset);
					kiBlast.setPos(spawn.x, spawn.y, spawn.z);
					kiBlast.shoot(localAim.x, localAim.y, localAim.z, kiBlast.getKiSpeed(), 0.5F);
					player.level().addFreshEntity(kiBlast);
					LogUtil.info(Env.SERVER,
							"Basic ki blast spawn: sableSubLevel={}, player={}, worldAim={}, localAim={}, spawn={}",
							SableCompat.isEntityInSubLevel(player), player.position(), worldAim, localAim, spawn);

					NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(), TriggerAnimationS2C.AnimationType.KI_BLAST_SHOT, leftHand ? 2 : 0, player.getId()), player);
					NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
				});
			} else if (!ACTIVE_VOLLEYS.containsKey(player.getUUID())) {
				NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(), TriggerAnimationS2C.AnimationType.KI_BLAST_SHOT, 1, player.getId()), player);
			}
		});

		ctx.get().setPacketHandled(true);
	}
	private static void setKiBlastCooldown(ServerPlayer player, StatsData data, int ticks) {
		data.getCooldowns().setCooldown(Cooldowns.KI_BLAST_CD, ticks);
		player.addEffect(new MobEffectInstance(MainEffects.KI_BLAST_CD, ticks, 0, false, false, true));
	}

	public static void tickVolley(ServerPlayer player, StatsData data, boolean interrupted) {
		Volley volley = ACTIVE_VOLLEYS.get(player.getUUID());
		if (volley == null) return;

		if (interrupted || player.isDeadOrDying() || player.isSpectator()) {
			finishVolley(player);
			return;
		}

		int elapsed = volley.ticks++ - VOLLEY_WINDUP_TICKS;
		if (elapsed >= 0 && volley.shotsFired < VOLLEY_SHOTS && elapsed % VOLLEY_INTERVAL_TICKS == 0) {
			fireVolleyShot(player, volley);
		}

		if (volley.shotsFired >= VOLLEY_SHOTS && elapsed >= VOLLEY_SHOTS * VOLLEY_INTERVAL_TICKS + VOLLEY_RECOVERY_TICKS) {
			finishVolley(player);
		}
	}

	private static void fireVolleyShot(ServerPlayer player, Volley volley) {
		Vec3 right = Vec3.directionFromRotation(0.0F, player.getYRot() + 90.0F);
		double side = (volley.shotsFired % 2 == 0 ? 1.0 : -1.0) * HAND_OFFSET;

		KiBlastEntity kiBlast = new KiBlastEntity(player.level(), player);
		kiBlast.setup(player, volley.damage, 0.5F, 1.5f, volley.colorMain, volley.colorBorder);
		kiBlast.setPos(player.getX() + right.x * side, player.getEyeY() - 0.4, player.getZ() + right.z * side);
		kiBlast.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, kiBlast.getKiSpeed(), VOLLEY_SPREAD);
		player.level().addFreshEntity(kiBlast);
		volley.shotsFired++;
	}

	private static void finishVolley(ServerPlayer player) {
		if (ACTIVE_VOLLEYS.remove(player.getUUID()) == null) return;
		NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP, 0, -1, ""), player);
	}

	public static void clearVolley(UUID playerId) {
		ACTIVE_VOLLEYS.remove(playerId);
		LEFT_HAND_NEXT.remove(playerId);
	}

	private static final class Volley {
		private final float damage;
		private final int colorMain;
		private final int colorBorder;
		private int ticks = 0;
		private int shotsFired = 0;

		private Volley(float damage, int colorMain, int colorBorder) {
			this.damage = damage;
			this.colorMain = colorMain;
			this.colorBorder = colorBorder;
		}
	}
}
