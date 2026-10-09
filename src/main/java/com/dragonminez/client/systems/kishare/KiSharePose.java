package com.dragonminez.client.systems.kishare;

import com.dragonminez.client.systems.FormVisualTransition;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Status;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class KiSharePose {
	private static final float SHOULDER_HEIGHT_PX = 22.0f;
	private static final float SHOULDER_SIDE_PX = 5.0f;
	private static final float HAND_REACH_PX = 9.0f;
	private static final double RING_DROOP = 0.35;
	private static final double RECEIVER_CHEST_RATIO = 0.6;
	private static final double MAX_SIDE_ANGLE = Math.toRadians(100.0);

	public record Aim(Vec3 right, Vec3 left) {}

	public record Ring(Player center, List<Player> donors) {}

	private KiSharePose() {}

	public static Aim resolve(Player player, StatsData data, float partialTick) {
		Status status = data.getStatus();
		if (status.getGodRitualRole() == Status.GOD_RITUAL_DONOR) return resolveRing(player, data, partialTick);
		if (status.isSharingKi() && player.level().getEntity(status.getKiTransferTarget()) instanceof Player receiver) {
			Vec3 chest = position(receiver, partialTick).add(0.0, receiver.getBbHeight() * RECEIVER_CHEST_RATIO, 0.0);
			return new Aim(clampToFront(player, data, partialTick, true, chest), clampToFront(player, data, partialTick, false, chest));
		}
		return null;
	}

	public static void apply(AbstractClientPlayer player, CoreGeoBone rightArm, CoreGeoBone leftArm, float partialTick) {
		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (data == null || !data.getStatus().isKiShareLocked()) return;
		Aim aim = resolve(player, data, partialTick);
		if (aim == null) return;
		if (rightArm != null && aim.right() != null) aimBone(player, data, partialTick, true, aim.right(), rightArm);
		if (leftArm != null && aim.left() != null) aimBone(player, data, partialTick, false, aim.left(), leftArm);
	}

	public static Vec3 shoulder(Player player, StatsData data, float partialTick, boolean right) {
		float[] scale = FormVisualTransition.modelScale(data);
		float yaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
		double side = (right ? 1.0 : -1.0) * SHOULDER_SIDE_PX / 16.0 * scale[0];
		double rightX = -Mth.cos(yaw);
		double rightZ = -Mth.sin(yaw);
		return position(player, partialTick).add(rightX * side, SHOULDER_HEIGHT_PX / 16.0 * scale[1], rightZ * side);
	}

	public static Vec3 hand(Player player, StatsData data, float partialTick, boolean right, Vec3 target) {
		Vec3 shoulder = shoulder(player, data, partialTick, right);
		Vec3 dir = target.subtract(shoulder);
		if (dir.lengthSqr() < 1.0E-6) return shoulder;
		float[] scale = FormVisualTransition.modelScale(data);
		return shoulder.add(dir.normalize().scale(HAND_REACH_PX / 16.0 * scale[1]));
	}

	public static Ring ring(Player donor, StatsData data) {
		UUID anchor = data.getStatus().getGodRitualAnchor();
		if (anchor == null) return null;
		Player center = donor.level().getPlayerByUUID(anchor);
		List<Player> donors = new ArrayList<>();
		for (Player other : donor.level().players()) {
			StatsData otherData = StatsProvider.get(StatsCapability.INSTANCE, other).orElse(null);
			if (otherData == null || otherData.getStatus().getGodRitualRole() != Status.GOD_RITUAL_DONOR) continue;
			if (anchor.equals(otherData.getStatus().getGodRitualAnchor())) donors.add(other);
		}
		Vec3 c = center != null ? center.position() : centroid(donors);
		donors.sort(Comparator.comparingDouble(p -> Math.atan2(p.getZ() - c.z, p.getX() - c.x)));
		return new Ring(center, donors);
	}

	public static Vec3 meetingPoint(Player a, Player b, StatsData aData, float partialTick) {
		Vec3 mid = position(a, partialTick).add(position(b, partialTick)).scale(0.5);
		float[] scale = FormVisualTransition.modelScale(aData);
		return mid.add(0.0, SHOULDER_HEIGHT_PX / 16.0 * scale[1] - RING_DROOP, 0.0);
	}

	public static Vec3 position(Entity entity, float partialTick) {
		return new Vec3(Mth.lerp(partialTick, entity.xo, entity.getX()), Mth.lerp(partialTick, entity.yo, entity.getY()), Mth.lerp(partialTick, entity.zo, entity.getZ()));
	}

	private static Aim resolveRing(Player player, StatsData data, float partialTick) {
		Ring ring = ring(player, data);
		if (ring == null) return null;
		List<Player> donors = ring.donors();
		int index = donors.indexOf(player);
		if (index < 0) return null;
		if (donors.size() < 3) {
			if (ring.center() == null) return null;
			Vec3 chest = position(ring.center(), partialTick).add(0.0, ring.center().getBbHeight() * RECEIVER_CHEST_RATIO, 0.0);
			return new Aim(chest, chest);
		}
		Player previous = donors.get((index - 1 + donors.size()) % donors.size());
		Player next = donors.get((index + 1) % donors.size());
		Vec3 toPrevious = meetingPoint(player, previous, data, partialTick);
		Vec3 toNext = meetingPoint(player, next, data, partialTick);
		float yaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
		Vec3 here = position(player, partialTick);
		double rightX = -Mth.cos(yaw);
		double rightZ = -Mth.sin(yaw);
		double previousSide = (toPrevious.x - here.x) * rightX + (toPrevious.z - here.z) * rightZ;
		double nextSide = (toNext.x - here.x) * rightX + (toNext.z - here.z) * rightZ;
		return previousSide >= nextSide ? new Aim(toPrevious, toNext) : new Aim(toNext, toPrevious);
	}

	private static Vec3 clampToFront(Player player, StatsData data, float partialTick, boolean right, Vec3 target) {
		Vec3 shoulder = shoulder(player, data, partialTick, right);
		Vec3 dir = target.subtract(shoulder);
		double horizontal = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
		if (horizontal < 1.0E-4) return target;
		float yaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
		double forwardX = -Mth.sin(yaw);
		double forwardZ = Mth.cos(yaw);
		double rightX = -Mth.cos(yaw);
		double rightZ = -Mth.sin(yaw);
		double fwd = (dir.x * forwardX + dir.z * forwardZ) / horizontal;
		double side = (dir.x * rightX + dir.z * rightZ) / horizontal;
		double angle = Math.atan2(side, fwd);
		if (Math.abs(angle) <= MAX_SIDE_ANGLE) return target;
		double clamped = Math.copySign(MAX_SIDE_ANGLE, angle);
		double nx = forwardX * Math.cos(clamped) + rightX * Math.sin(clamped);
		double nz = forwardZ * Math.cos(clamped) + rightZ * Math.sin(clamped);
		return shoulder.add(nx * horizontal, dir.y, nz * horizontal);
	}

	private static void aimBone(Player player, StatsData data, float partialTick, boolean right, Vec3 target, CoreGeoBone bone) {
		Vec3 dir = target.subtract(shoulder(player, data, partialTick, right));
		if (dir.lengthSqr() < 1.0E-6) return;
		dir = dir.normalize();
		float yaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
		double forward = dir.x * -Mth.sin(yaw) + dir.z * Mth.cos(yaw);
		double side = dir.x * -Mth.cos(yaw) + dir.z * -Mth.sin(yaw);
		double up = dir.y;
		float rotX = (float) Math.asin(Mth.clamp(forward, -1.0, 1.0));
		float rotZ = (float) Math.atan2(side, -up);
		bone.setRotX(rotX);
		bone.setRotY(0.0f);
		bone.setRotZ(rotZ);
	}

	private static Vec3 centroid(List<Player> players) {
		if (players.isEmpty()) return Vec3.ZERO;
		Vec3 sum = Vec3.ZERO;
		for (Player p : players) sum = sum.add(p.position());
		return sum.scale(1.0 / players.size());
	}
}
