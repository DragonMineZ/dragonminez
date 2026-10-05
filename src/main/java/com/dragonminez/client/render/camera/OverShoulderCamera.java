package com.dragonminez.client.render.camera;

import com.dragonminez.Reference;
import com.dragonminez.client.clash.BeamClashCinematicCamera;
import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.client.events.LockOnEvent;
import com.dragonminez.client.flight.RollCamera;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.combat.util.Minecraft_DMZ;
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
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

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
	private static final double MAX_FRAME_SECONDS = 0.1;

	private static final float ENTITY_LOCK_MAX_DEGREES = 45.0F;
	private static final float BLOCK_LOCK_FULL_DEGREES = 2.5F;
	private static final float BLOCK_LOCK_NONE_DEGREES = 5.5F;
	private static final double LOCK_RATE = 18.0;
	private static final double CROSSHAIR_DEPTH_IN_RATE = 18.0;
	private static final double CROSSHAIR_DEPTH_OUT_RATE = 9.0;
	private static final double CROSSHAIR_CENTRE_RATE = 14.0;

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

	@Getter
	private static float lockYaw;
	@Getter
	private static float lockPitch;
	@Getter
	private static float lockNeeded;

	private static Entity aimEntity;
	private static boolean aimEntityLockable;
	private static double lookDistance = -1.0;

	private static final Matrix4f PROJECTION = new Matrix4f();
	private static final Matrix4f VIEW = new Matrix4f();
	private static Vec3 projectionCamera = Vec3.ZERO;
	private static boolean projectionReady;
	private static double crosshairFocus;
	private static float crosshairCentring;
	private static long lastCrosshairNanos;
	private static boolean crosshairPlaced;

	private OverShoulderCamera() {}

	public static boolean isActive(Entity entity, boolean thirdPersonReverse) {
		if (thirdPersonReverse || !(entity instanceof Player)) return false;
		if (TechniquePreviewCamera.isActive()) return false;
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
			lockYaw = 0.0F;
			lockPitch = 0.0F;
			crosshairPlaced = false;
		} else {
			cameraYaw += player.getYRot() - writtenYaw;
			cameraPitch = Mth.clamp(cameraPitch + player.getXRot() - writtenPitch, -90.0F, 90.0F);
		}

		double rate = targetFocus > focus ? AIM_FOCUS_IN_RATE : AIM_FOCUS_OUT_RATE;
		focus += (targetFocus - focus) * (1.0 - Math.exp(-dt * rate));

		aim(player, partialTick, dt);
	}

	private static void aim(LocalPlayer player, float partialTick, double dt) {
		Vec3 eye = player.getEyePosition(partialTick);
		Vec3 forward = Vec3.directionFromRotation(cameraPitch, cameraYaw);
		Vec3 camera = cameraPosition(eye);

		LivingEntity locked = LockOnEvent.getLockedTarget();
		boolean lockedOn = locked != null && locked.isAlive();
		double depth = focus > 1.0 / AIM_BLOCK_RANGE ? 1.0 / focus : AIM_BLOCK_RANGE * 4.0;
		Vec3 point = lockedOn
				? locked.getPosition(partialTick).add(0.0, locked.getBbHeight() * 0.5, 0.0)
				: camera.add(forward.scale(depth));

		float targetYaw = 0.0F;
		float targetPitch = 0.0F;
		lockNeeded = 0.0F;

		Vec3 direction = point.subtract(eye);
		if (direction.lengthSqr() >= 0.25) {
			double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
			float yaw = horizontal < 1.0E-4 ? cameraYaw : (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F;
			float pitch = Mth.clamp((float) -(Mth.atan2(direction.y, horizontal) * Mth.RAD_TO_DEG), -90.0F, 90.0F);
			float yawOffset = Mth.wrapDegrees(yaw - cameraYaw);
			float pitchOffset = pitch - cameraPitch;
			lockNeeded = (float) Math.sqrt(yawOffset * yawOffset + pitchOffset * pitchOffset);

			float strength;
			if (lockedOn) strength = 1.0F;
			else if (aimEntityLockable) strength = lockNeeded > ENTITY_LOCK_MAX_DEGREES ? ENTITY_LOCK_MAX_DEGREES / lockNeeded : 1.0F;
			else strength = 1.0F - smoothstep(BLOCK_LOCK_FULL_DEGREES, BLOCK_LOCK_NONE_DEGREES, lockNeeded);

			targetYaw = yawOffset * strength;
			targetPitch = pitchOffset * strength;
		}

		float ease = lockedOn ? 1.0F : (float) (1.0 - Math.exp(-dt * LOCK_RATE));
		lockYaw += (targetYaw - lockYaw) * ease;
		lockPitch += (targetPitch - lockPitch) * ease;

		writeRotation(player, cameraYaw + lockYaw, Mth.clamp(cameraPitch + lockPitch, -90.0F, 90.0F));
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
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || player.level() == null) {
			decoupled = false;
			return;
		}

		Vec3 eye = player.getEyePosition();
		Vec3 forward = Vec3.directionFromRotation(cameraPitch, cameraYaw);
		Vec3 camera = cameraPosition(eye);
		double ahead = Math.max(0.0, eye.subtract(camera).dot(forward));
		Vec3 start = camera.add(forward.scale(ahead));

		HitResult cameraHit = trace(player, start, forward, AIM_BLOCK_RANGE);
		aimEntity = cameraHit instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
		aimEntityLockable = aimEntity != null && isLockable(player, aimEntity);
		targetFocus = cameraHit.getType() == HitResult.Type.MISS ? 0.0
				: 1.0 / (ahead + Math.max(AIM_MIN_AHEAD, cameraHit.getLocation().distanceTo(start)));

		HitResult lookHit = trace(player, eye, player.getLookAngle(), AIM_BLOCK_RANGE);
		lookDistance = lookHit.getType() == HitResult.Type.MISS ? -1.0 : lookHit.getLocation().distanceTo(eye);
	}

	private static HitResult trace(LocalPlayer player, Vec3 start, Vec3 direction, double range) {
		Vec3 end = start.add(direction.scale(range));
		HitResult block = player.level().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		double reach = block.getType() == HitResult.Type.MISS ? range : block.getLocation().distanceTo(start);

		double entityReach = Math.min(reach, AIM_ENTITY_RANGE);
		Vec3 entityEnd = start.add(direction.scale(entityReach));
		EntityHitResult entity = ProjectileUtil.getEntityHitResult(player, start, entityEnd, new AABB(start, entityEnd).inflate(1.0),
				candidate -> !candidate.isSpectator() && candidate.isPickable(), entityReach * entityReach);
		return entity != null ? entity : block;
	}

	private static boolean isLockable(LocalPlayer player, Entity entity) {
		Entity target = TargetHelper.resolveHittable(entity);
		if (!(target instanceof LivingEntity) || target instanceof ArmorStand || !target.isAttackable()) return false;
		return TargetHelper.getRelation(player, target) == TargetHelper.Relation.HOSTILE;
	}

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
		Camera camera = event.getCamera();
		PROJECTION.set(event.getProjectionMatrix());
		VIEW.identity()
				.rotateZ(((RollCamera) camera).dragonminez$getRoll() * Mth.DEG_TO_RAD)
				.rotateX(camera.getXRot() * Mth.DEG_TO_RAD)
				.rotateY((camera.getYRot() + 180.0F) * Mth.DEG_TO_RAD);
		projectionCamera = camera.getPosition();
		projectionReady = true;
	}

	public static float[] crosshairOffset(int guiWidth, int guiHeight) {
		long now = System.nanoTime();
		double dt = lastCrosshairNanos == 0L ? 0.0 : Math.min(MAX_FRAME_SECONDS, (now - lastCrosshairNanos) / 1.0E9);
		lastCrosshairNanos = now;

		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (!decoupled || player == null || !projectionReady) {
			crosshairPlaced = false;
			return new float[]{0.0F, 0.0F};
		}

		float partialTick = mc.getFrameTime();
		Vec3 eye = player.getEyePosition(partialTick);
		HitResult pick = mc.hitResult;
		double distance = pick != null && pick.getType() != HitResult.Type.MISS ? pick.getLocation().distanceTo(eye)
				: lookDistance > 0.0 ? lookDistance : AIM_BLOCK_RANGE * 4.0;
		double targetFocus = 1.0 / Math.max(AIM_MIN_AHEAD, distance);

		boolean centre = aimEntityLockable && aimEntity != null && ((Minecraft_DMZ) mc).isTargetInReach(aimEntity);
		if (!crosshairPlaced) {
			crosshairFocus = targetFocus;
			crosshairCentring = centre ? 1.0F : 0.0F;
			crosshairPlaced = true;
		} else {
			double rate = targetFocus > crosshairFocus ? CROSSHAIR_DEPTH_IN_RATE : CROSSHAIR_DEPTH_OUT_RATE;
			crosshairFocus += (targetFocus - crosshairFocus) * (1.0 - Math.exp(-dt * rate));
			crosshairCentring += ((centre ? 1.0F : 0.0F) - crosshairCentring) * (float) (1.0 - Math.exp(-dt * CROSSHAIR_CENTRE_RATE));
		}

		Vec3 point = eye.add(Vec3.directionFromRotation(writtenPitch, writtenYaw).scale(1.0 / crosshairFocus));
		Vector4f clip = new Vector4f(
				(float) (point.x - projectionCamera.x),
				(float) (point.y - projectionCamera.y),
				(float) (point.z - projectionCamera.z), 1.0F);
		VIEW.transform(clip);
		PROJECTION.transform(clip);
		if (clip.w <= 1.0E-4F) return new float[]{0.0F, 0.0F};

		float keep = 1.0F - crosshairCentring;
		float x = Mth.clamp(clip.x / clip.w, -1.0F, 1.0F) * guiWidth * 0.5F * keep;
		float y = -Mth.clamp(clip.y / clip.w, -1.0F, 1.0F) * guiHeight * 0.5F * keep;
		double scale = mc.getWindow().getGuiScale();
		return new float[]{(float) (Math.round(x * scale) / scale), (float) (Math.round(y * scale) / scale)};
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

	private static float smoothstep(float edge0, float edge1, float value) {
		float t = Mth.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}
}
