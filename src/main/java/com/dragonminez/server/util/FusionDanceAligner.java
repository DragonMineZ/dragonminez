package com.dragonminez.server.util;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.extras.ActionMode;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FusionDanceAligner {
	private static final double PAIR_RANGE_SQ = 64.0;
	private static final int GLIDE_TICKS = 8;
	private static final double GROUND_PROBE = 0.2;
	private static final float DEFAULT_REACH = 22.60f;
	private static final float SLIM_REACH = 22.08f;
	private static final float MAJIN_FAT_REACH = 24.42f;
	private static final Map<String, Float> REACH = Map.ofEntries(
			Map.entry("buffed", 23.02f), Map.entry("ssj4gt", 23.02f), Map.entry("4arms", 23.02f), Map.entry("majin_ultra", 23.02f),
			Map.entry("namekian_buffed", 23.02f), Map.entry("namekian_orange", 23.02f), Map.entry("bioandroid_semi", 23.02f),
			Map.entry("buffedg3", 24.19f), Map.entry("ssj4d", 24.19f), Map.entry("frostdemon_fp", 24.19f), Map.entry("bioandroid_ultra", 24.19f),
			Map.entry("frostdemon_second", 23.99f), Map.entry("frostdemon_fifth", 23.99f), Map.entry("frostdemon_metalcore", 18.67f),
			Map.entry("bioandroid", 22.79f), Map.entry("bioandroid_base", 22.79f), Map.entry("bioandroid_perfect", 22.79f),
			Map.entry("bioandroid_xeno", 24.59f), Map.entry("bioandroid_xenofp", 25.45f),
			Map.entry("janemba_imperfect", 24.42f), Map.entry("janemba_fat", 24.97f), Map.entry("janemba_super", 22.99f),
			Map.entry("glindtrueform", 23.91f), Map.entry("trascended", 22.92f), Map.entry("oozaru", 24.59f));

	private static final Map<UUID, Slot> SLOTS = new HashMap<>();

	private static final class Slot {
		private final UUID partner;
		private final boolean aligned;
		private final double fromX, fromZ, toX, toZ;
		private final float yaw;
		private int ticks;

		private Slot(UUID partner, boolean aligned, double fromX, double fromZ, double toX, double toZ, float yaw) {
			this.partner = partner;
			this.aligned = aligned;
			this.fromX = fromX;
			this.fromZ = fromZ;
			this.toX = toX;
			this.toZ = toZ;
			this.yaw = yaw;
		}

		private static Slot blocked(UUID partner) {
			return new Slot(partner, false, 0, 0, 0, 0, 0);
		}
	}

	private FusionDanceAligner() {}

	public static void tick(ServerPlayer player, StatsData data) {
		UUID id = player.getUUID();
		if (!isDancing(data)) {
			SLOTS.remove(id);
			return;
		}
		Slot slot = SLOTS.get(id);
		if (slot != null && !partnerStillDancing(player, slot)) {
			SLOTS.remove(id);
			slot = null;
		}
		if (slot == null) slot = pair(player, data);
		if (slot != null && slot.aligned) advance(player, slot);
	}

	private static boolean isDancing(StatsData data) {
		return data.getStatus().isActionCharging() && data.getStatus().getSelectedAction() == ActionMode.FUSION && !data.getStatus().isFused();
	}

	private static boolean partnerStillDancing(ServerPlayer player, Slot slot) {
		ServerPlayer partner = player.server.getPlayerList().getPlayer(slot.partner);
		if (partner == null || partner.level() != player.level() || partner.distanceToSqr(player) > PAIR_RANGE_SQ) return false;
		StatsData partnerData = StatsProvider.get(StatsCapability.INSTANCE, partner).orElse(null);
		return partnerData != null && isDancing(partnerData);
	}

	private static Slot pair(ServerPlayer player, StatsData data) {
		ServerPlayer partner = null;
		StatsData partnerData = null;
		double best = PAIR_RANGE_SQ;
		for (ServerPlayer other : player.serverLevel().players()) {
			if (other == player || SLOTS.containsKey(other.getUUID())) continue;
			double distance = other.distanceToSqr(player);
			if (distance > best) continue;
			StatsData otherData = StatsProvider.get(StatsCapability.INSTANCE, other).orElse(null);
			if (otherData == null || !isDancing(otherData)) continue;
			best = distance;
			partner = other;
			partnerData = otherData;
		}
		if (partner == null) return null;

		String blocker = FusionLogic.metamoruBlocker(data, partnerData);
		if (blocker != null) {
			player.displayClientMessage(Component.translatable(blocker), true);
			partner.displayClientMessage(Component.translatable(blocker), true);
			SLOTS.put(partner.getUUID(), Slot.blocked(player.getUUID()));
			Slot own = Slot.blocked(partner.getUUID());
			SLOTS.put(player.getUUID(), own);
			return own;
		}

		data.getResources().setActionCharge(0);
		partnerData.getResources().setActionCharge(0);

		double ax = player.getX(), az = player.getZ();
		double bx = partner.getX(), bz = partner.getZ();
		double dx = bx - ax, dz = bz - az;
		double length = Math.sqrt(dx * dx + dz * dz);
		double fx, fz;
		if (length < 1.0E-3) {
			fx = -Mth.sin(player.getYRot() * Mth.DEG_TO_RAD);
			fz = Mth.cos(player.getYRot() * Mth.DEG_TO_RAD);
		} else {
			fx = -dz / length;
			fz = dx / length;
			double lookX = -Mth.sin(player.getYRot() * Mth.DEG_TO_RAD) - Mth.sin(partner.getYRot() * Mth.DEG_TO_RAD);
			double lookZ = Mth.cos(player.getYRot() * Mth.DEG_TO_RAD) + Mth.cos(partner.getYRot() * Mth.DEG_TO_RAD);
			if (fx * lookX + fz * lookZ < 0.0) {
				fx = -fx;
				fz = -fz;
			}
		}
		float yaw = (float) (Mth.atan2(-fx, fz) * Mth.RAD_TO_DEG);
		double rx = -Mth.cos(yaw * Mth.DEG_TO_RAD);
		double rz = -Mth.sin(yaw * Mth.DEG_TO_RAD);

		double mx = (ax + bx) * 0.5, mz = (az + bz) * 0.5;
		double side = (ax - mx) * rx + (az - mz) * rz;
		double sign = side > 0.0 || (side == 0.0 && player.getUUID().compareTo(partner.getUUID()) < 0) ? 1.0 : -1.0;
		double playerReach = reach(data.getCharacter());
		double partnerReach = reach(partnerData.getCharacter());
		double px = mx + rx * sign * playerReach, pz = mz + rz * sign * playerReach;
		double qx = mx - rx * sign * partnerReach, qz = mz - rz * sign * partnerReach;
		if (!canStand(player, px, pz) || !canStand(partner, qx, qz)) {
			px = ax;
			pz = az;
			qx = bx;
			qz = bz;
		}

		Slot own = new Slot(partner.getUUID(), true, ax, az, px, pz, yaw);
		SLOTS.put(player.getUUID(), own);
		SLOTS.put(partner.getUUID(), new Slot(player.getUUID(), true, bx, bz, qx, qz, yaw));
		return own;
	}

	private static double reach(Character character) {
		String key = character.getRenderLogicKey().toLowerCase();
		boolean female = Character.GENDER_FEMALE.equalsIgnoreCase(character.getGender());
		float pixels;
		if (key.equals("majin")) pixels = female ? SLIM_REACH : character.getBodyType() == 2 ? DEFAULT_REACH : MAJIN_FAT_REACH;
		else if (female && (key.equals("human") || key.equals("saiyan"))) pixels = SLIM_REACH;
		else pixels = REACH.getOrDefault(key, DEFAULT_REACH);
		Float[] scaling = character.getResolvedModelScaling();
		float scale = scaling != null && scaling.length > 0 && scaling[0] != null ? scaling[0] : 0.9375f;
		return pixels * scale / 16.0;
	}

	private static boolean canStand(ServerPlayer player, double x, double z) {
		AABB box = player.getBoundingBox().move(x - player.getX(), 0.0, z - player.getZ());
		if (!player.level().noCollision(player, box)) return false;
		if (!player.onGround()) return true;
		return !player.level().noCollision(player, new AABB(box.minX, box.minY - GROUND_PROBE, box.minZ, box.maxX, box.minY, box.maxZ));
	}

	private static void advance(ServerPlayer player, Slot slot) {
		if (slot.ticks >= GLIDE_TICKS) return;
		slot.ticks++;
		float t = (float) slot.ticks / GLIDE_TICKS;
		float eased = t * t * (3.0f - 2.0f * t);
		double x = Mth.lerp(eased, slot.fromX, slot.toX);
		double z = Mth.lerp(eased, slot.fromZ, slot.toZ);
		player.connection.teleport(x, player.getY(), z, slot.yaw, 0.0f);
		player.setYHeadRot(slot.yaw);
		player.yHeadRotO = slot.yaw;
		player.yBodyRot = slot.yaw;
		player.yBodyRotO = slot.yaw;
	}
}
