package com.dragonminez.client.render.camera;

import com.dragonminez.Reference;
import com.dragonminez.client.clash.BeamClashCinematicCamera;
import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.client.events.LockOnEvent;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralUserConfig;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class OverShoulderCamera {

	public static final int MODE_NONE = 0;
	public static final int MODE_LOCK_ON = 1;
	public static final int MODE_ALWAYS = 2;

	private static final float VANILLA_TURN_SCALE = 0.15F;
	private static final double AIM_BLOCK_RANGE = 64.0;
	private static final double AIM_ENTITY_RANGE = 48.0;
	private static final double AIM_MIN_AHEAD = 1.0;
	private static final double AIM_FOCUS_IN_RATE = 28.0;
	private static final double AIM_FOCUS_OUT_RATE = 7.0;
	private static final float AIM_MAX_YAW_OFFSET = 50.0F;
	private static final float AIM_MAX_PITCH_OFFSET = 35.0F;
	private static final double MAX_FRAME_SECONDS = 0.1;

	private static double curBack;
	private static double curUp;
	private static double curRight;
	private static boolean active;
	@Getter
	@Setter
	private static boolean previewOverride;

	@Getter
	private static boolean decoupled;
	@Getter
	private static float cameraYaw;
	@Getter
	private static float cameraPitch;
	private static float writtenYaw;
	private static float writtenPitch;
	private static Vec3 appliedMove = Vec3.ZERO;
	private static double focus;
	private static double targetFocus;
	private static long lastFrameNanos;

	private OverShoulderCamera() {}

	public static boolean isActive(Entity entity, boolean thirdPersonReverse) {
		if (thirdPersonReverse || !(entity instanceof Player)) return false;
		if (entity instanceof LivingEntity living && living.isSleeping()) return false;
		if (previewOverride) return true;
		int mode = ConfigManager.getUserConfig().getOverShoulderMode();
		if (mode == MODE_ALWAYS) return true;
		if (mode == MODE_LOCK_ON) return LockOnEvent.getLockedTarget() != null;
		return false;
	}

	private static boolean shouldDecouple(LocalPlayer player) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getCameraEntity() != player || mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) return false;
		if (!isActive(player, false)) return false;
		if (player.isPassenger() || player.isSpectator()) return false;
		if (BeamClashCinematicCamera.isActive()) return false;
		return !FlySkillEvent.getInstance().isFlyingFast(player);
	}

	public static boolean turn(double yawDelta, double pitchDelta) {
		if (!decoupled) return false;
		cameraYaw += (float) yawDelta * VANILLA_TURN_SCALE;
		cameraPitch = Mth.clamp(cameraPitch + (float) pitchDelta * VANILLA_TURN_SCALE, -90.0F, 90.0F);
		return true;
	}

	public static void beforeSetup(LocalPlayer player, float partialTick) {
		long now = System.nanoTime();
		double dt = lastFrameNanos == 0L ? 0.0 : Mth.clamp((now - lastFrameNanos) / 1.0E9, 0.0, MAX_FRAME_SECONDS);
		lastFrameNanos = now;

		if (!shouldDecouple(player)) {
			if (decoupled) {
				decoupled = false;
				writeRotation(player, cameraYaw, cameraPitch);
			}
			return;
		}

		if (!decoupled) {
			decoupled = true;
			cameraYaw = player.getYRot();
			cameraPitch = Mth.clamp(player.getXRot(), -90.0F, 90.0F);
			appliedMove = Vec3.ZERO;
			focus = 0.0;
			targetFocus = 0.0;
		} else {
			cameraYaw += player.getYRot() - writtenYaw;
			cameraPitch = Mth.clamp(cameraPitch + player.getXRot() - writtenPitch, -90.0F, 90.0F);
		}

		double rate = targetFocus > focus ? AIM_FOCUS_IN_RATE : AIM_FOCUS_OUT_RATE;
		focus += (targetFocus - focus) * (1.0 - Math.exp(-dt * rate));

		aim(player, partialTick);
	}

	private static void aim(LocalPlayer player, float partialTick) {
		Vec3 eye = player.getEyePosition(partialTick);
		Vec3 forward = Vec3.directionFromRotation(cameraPitch, cameraYaw);
		Vec3 camera = cameraPosition(eye);

		LivingEntity locked = LockOnEvent.getLockedTarget();
		boolean lockedOn = locked != null && locked.isAlive();
		Vec3 point;
		if (lockedOn) point = locked.getPosition(partialTick).add(0.0, locked.getBbHeight() * 0.5, 0.0);
		else point = camera.add(forward.scale(focus > 1.0 / AIM_BLOCK_RANGE ? 1.0 / focus : AIM_BLOCK_RANGE * 4.0));

		Vec3 direction = point.subtract(eye);
		double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
		if (direction.lengthSqr() < 0.25) {
			writeRotation(player, cameraYaw, cameraPitch);
			return;
		}

		float yaw = horizontal < 1.0E-4 ? cameraYaw : (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F;
		float pitch = (float) -(Mth.atan2(direction.y, horizontal) * Mth.RAD_TO_DEG);

		float yawOffset = Mth.wrapDegrees(yaw - cameraYaw);
		float pitchOffset = pitch - cameraPitch;
		if (!lockedOn) {
			yawOffset = Mth.clamp(yawOffset, -AIM_MAX_YAW_OFFSET, AIM_MAX_YAW_OFFSET);
			pitchOffset = Mth.clamp(pitchOffset, -AIM_MAX_PITCH_OFFSET, AIM_MAX_PITCH_OFFSET);
		}

		writeRotation(player, cameraYaw + yawOffset, Mth.clamp(cameraPitch + pitchOffset, -90.0F, 90.0F));
	}

	private static void writeRotation(LocalPlayer player, float yaw, float pitch) {
		float yawDelta = yaw - player.getYRot();
		float pitchDelta = pitch - player.getXRot();
		player.setYRot(yaw);
		player.setXRot(pitch);
		player.yRotO += yawDelta;
		player.xRotO = Mth.clamp(player.xRotO + pitchDelta, -90.0F, 90.0F);
		writtenYaw = yaw;
		writtenPitch = pitch;
	}

	private static Vec3 cameraPosition(Vec3 eye) {
		Vec3 forward = Vec3.directionFromRotation(cameraPitch, cameraYaw);
		Vec3 up = Vec3.directionFromRotation(cameraPitch - 90.0F, cameraYaw);
		Vec3 left = up.cross(forward);
		return eye.add(forward.scale(appliedMove.x)).add(up.scale(appliedMove.y)).add(left.scale(appliedMove.z));
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !decoupled) return;
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || player.level() == null) {
			decoupled = false;
			return;
		}

		Vec3 eye = player.getEyePosition();
		Vec3 forward = Vec3.directionFromRotation(cameraPitch, cameraYaw);
		Vec3 camera = cameraPosition(eye);
		double ahead = Math.max(0.0, eye.subtract(camera).dot(forward));
		Vec3 start = camera.add(forward.scale(ahead));

		double reach = AIM_BLOCK_RANGE;
		boolean found = false;

		HitResult block = player.level().clip(new ClipContext(start, start.add(forward.scale(AIM_BLOCK_RANGE)),
				ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		if (block.getType() != HitResult.Type.MISS) {
			reach = block.getLocation().distanceTo(start);
			found = true;
		}

		double entityReach = Math.min(reach, AIM_ENTITY_RANGE);
		Vec3 end = start.add(forward.scale(entityReach));
		EntityHitResult entity = ProjectileUtil.getEntityHitResult(player, start, end, new AABB(start, end).inflate(1.0),
				candidate -> !candidate.isSpectator() && candidate.isPickable(), entityReach * entityReach);
		if (entity != null) {
			reach = entity.getLocation().distanceTo(start);
			found = true;
		}

		targetFocus = found ? 1.0 / (ahead + Math.max(AIM_MIN_AHEAD, reach)) : 0.0;
	}

	public static void steerToward(LocalPlayer player, Vec3 target, float partialTick, float amount) {
		Vec3 direction = target.subtract(cameraPosition(player.getEyePosition(partialTick)));
		double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
		if (direction.lengthSqr() < 1.0E-4) return;

		float yaw = (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F;
		float pitch = (float) -(Mth.atan2(direction.y, horizontal) * Mth.RAD_TO_DEG);
		cameraYaw += Mth.wrapDegrees(yaw - cameraYaw) * amount;
		cameraPitch = Mth.clamp(cameraPitch + (pitch - cameraPitch) * amount, -90.0F, 90.0F);
	}

	public static Vec3 computeMove(Camera camera, BlockGetter level, Entity entity, boolean thirdPersonReverse,
	                               double vanillaForward, double vanillaUp, double vanillaLeft, float partialTick, float roll) {
		if (!isActive(entity, thirdPersonReverse)) {
			active = false;
			return new Vec3(vanillaForward, vanillaUp, vanillaLeft);
		}

		if (!active) {
			curBack = -vanillaForward;
			curUp = 0.0;
			curRight = 0.0;
			active = true;
		}

		GeneralUserConfig config = ConfigManager.getUserConfig();
		double ease = config.getOverShoulderSmoothing();
		double targetBack = config.getOverShoulderBack();
		double targetUp = config.getOverShoulderUp();
		double targetRight = config.getOverShoulderSide() * (config.getOverShoulderLeft() ? -1.0 : 1.0);

		curBack += (targetBack - curBack) * ease;
		curUp += (targetUp - curUp) * ease;
		curRight += (targetRight - curRight) * ease;

		double cos = Math.cos(roll * Mth.DEG_TO_RAD);
		double sin = Math.sin(roll * Mth.DEG_TO_RAD);

		double moveForward = -curBack;
		double moveUp = curUp * cos - curRight * sin;
		double moveLeft = -(curRight * cos + curUp * sin);

		double allowed = clampToObstruction(camera, level, entity, moveForward, moveUp, moveLeft, partialTick);
		double desired = Math.sqrt(moveForward * moveForward + moveUp * moveUp + moveLeft * moveLeft);
		if (desired > 1.0E-4 && allowed < desired) {
			double scale = allowed / desired;
			moveForward *= scale;
			moveUp *= scale;
			moveLeft *= scale;
		}

		appliedMove = new Vec3(moveForward, moveUp, moveLeft);
		return appliedMove;
	}

	private static double clampToObstruction(Camera camera, BlockGetter level, Entity entity,
	                                         double moveForward, double moveUp, double moveLeft, float partialTick) {
		Vector3f look = camera.getLookVector();
		Vector3f up = camera.getUpVector();
		Vector3f left = camera.getLeftVector();

		Vec3 worldOffset = new Vec3(
				look.x() * moveForward + up.x() * moveUp + left.x() * moveLeft,
				look.y() * moveForward + up.y() * moveUp + left.y() * moveLeft,
				look.z() * moveForward + up.z() * moveUp + left.z() * moveLeft
		);

		double distance = worldOffset.length();
		if (distance < 1.0E-4) return distance;

		Vec3 eyePosition = entity.getEyePosition(partialTick);

		for (int i = 0; i < 8; i++) {
			Vec3 corner = new Vec3(i & 1, i >> 1 & 1, i >> 2 & 1).scale(2).subtract(1, 1, 1);
			Vec3 fromOffset = corner.scale(Mth.clamp(entity.getBbWidth() / 2.0F / Mth.sqrt(2), 0.0F, 0.15F))
					.xRot(-camera.getXRot() * Mth.DEG_TO_RAD)
					.yRot(-camera.getYRot() * Mth.DEG_TO_RAD);
			Vec3 from = eyePosition.add(fromOffset);
			Vec3 to = from.add(worldOffset);

			ClipContext context = new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity);
			HitResult hit = level.clip(context);
			if (hit.getType() != HitResult.Type.MISS) {
				double hitDistance = hit.getLocation().distanceTo(from);
				if (hitDistance < distance) distance = hitDistance;
			}
		}

		return distance;
	}
}
