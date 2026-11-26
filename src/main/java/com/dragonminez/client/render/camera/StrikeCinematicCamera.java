package com.dragonminez.client.render.camera;

import com.dragonminez.Reference;
import com.dragonminez.client.events.LockOnEvent;
import com.dragonminez.client.render.firstperson.dto.DMZCameraBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class StrikeCinematicCamera {
	private static final double BLEND_IN_SECONDS = 0.35;
	private static final double BLEND_OUT_SECONDS = 0.45;
	private static final double MAX_FRAME_SECONDS = 0.1;
	private static final double FOCUS_TAU = 0.1;
	private static final double DISTANCE_TAU = 0.3;
	private static final double YAW_TAU = 0.5;
	private static final double YAW_MAX_DEGREES_PER_SECOND = 75.0;
	private static final double DISTANCE_BASE = 3.6;
	private static final double DISTANCE_PER_SEPARATION = 0.85;
	private static final double DISTANCE_PER_EXTRA_HEIGHT = 0.8;
	private static final double DISTANCE_MIN = 3.2;
	private static final double DISTANCE_MAX = 16.0;
	private static final double HEIGHT_BASE = 0.6;
	private static final double HEIGHT_PER_DISTANCE = 0.14;
	private static final double THREE_QUARTER_DEGREES = 32.0;
	private static final double SWAY_DEGREES = 10.0;
	private static final double SWAY_PERIOD_SECONDS = 7.0;
	private static final double AXIS_MIN_SEPARATION = 0.35;
	private static final double FAR_START = 9.0;
	private static final double FAR_END = 18.0;
	private static final double CRANE_STEP = 1.5;
	private static final int CRANE_STEPS = 4;
	private static final double CRANE_ACCEPT = 0.85;
	private static final double CRANE_UP_TAU = 0.12;
	private static final double CRANE_DOWN_TAU = 0.6;
	private static final double REACH_OUT_TAU = 0.3;
	private static final double FIRST_PERSON_PULLBACK = 0.6;
	private static final double ARC_MIN_RADIUS = 0.5;
	private static final double LOOK_TAU = 0.06;
	private static final float PITCH_LIMIT = 80.0F;
	private static final double ANCHOR_STEP = 0.5;
	private static final int ANCHOR_STEPS = 8;
	private static final double WALL_MARGIN = 0.35;
	private static final double SIDE_PREFERENCE_MARGIN = 0.1;
	private static final float FOV_FACTOR = 0.9F;
	private static final float FOV_EASE = 0.2F;
	private static final int MAX_FILMING_TICKS = 600;
	private static final int MISSING_ATTACKER_TICKS = 10;

	private enum Phase { IDLE, FILMING, RELEASING }

	public record Shot(Vec3 pos, float yaw, float pitch) {
	}

	private static Phase phase = Phase.IDLE;
	private static int attackerId = -1;
	private static int victimId = -1;
	private static CameraType previousType = null;
	private static long phaseStartNanos = 0L;
	private static long lastFrameNanos = 0L;
	private static int filmingTicks = 0;
	private static int missingTicks = 0;
	private static boolean framed = false;
	private static double orbitAngle = 0.0;
	private static double filmSeconds = 0.0;
	private static Vec3 focus = null;
	private static Vec3 anchor = null;
	private static double distance = DISTANCE_BASE;
	private static double crane = 0.0;
	private static double reach = -1.0;
	private static float lookYaw = 0.0F;
	private static float lookPitch = 0.0F;
	private static Shot blendOrigin = null;
	private static Shot lastShown = null;
	private static Shot lastCinematic = null;
	private static float fovFactor = 1.0F;

	private StrikeCinematicCamera() {
	}

	public static boolean isActive() {
		return phase != Phase.IDLE;
	}

	public static boolean isFilming() {
		return phase == Phase.FILMING;
	}

	public static void start(int attacker, int victim) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		if (phase == Phase.IDLE) {
			previousType = mc.options.getCameraType();
			Camera camera = mc.gameRenderer.getMainCamera();
			if (camera.isInitialized()) {
				Vec3 pos = camera.getPosition();
				if (previousType.isFirstPerson()) pos = pos.subtract(new Vec3(camera.getLookVector()).scale(FIRST_PERSON_PULLBACK));
				blendOrigin = new Shot(pos, camera.getYRot(), camera.getXRot());
			} else {
				blendOrigin = null;
			}
		} else {
			blendOrigin = lastShown;
		}
		LockOnEvent.suspend();
		mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		attackerId = attacker;
		victimId = victim;
		phase = Phase.FILMING;
		phaseStartNanos = System.nanoTime();
		filmingTicks = 0;
		missingTicks = 0;
		framed = false;
		filmSeconds = 0.0;
		focus = null;
		lastCinematic = null;
	}

	public static void stop(int attacker) {
		if (phase != Phase.FILMING || attacker != attackerId) return;
		release();
	}

	public static void reset() {
		if (phase != Phase.IDLE && previousType != null) Minecraft.getInstance().options.setCameraType(previousType);
		phase = Phase.IDLE;
		previousType = null;
		attackerId = -1;
		victimId = -1;
		focus = null;
		blendOrigin = null;
		lastShown = null;
		lastCinematic = null;
		fovFactor = 1.0F;
	}

	private static void release() {
		phase = Phase.RELEASING;
		phaseStartNanos = System.nanoTime();
		LockOnEvent.resume();
	}

	private static void finish() {
		Minecraft mc = Minecraft.getInstance();
		if (previousType != null) mc.options.setCameraType(previousType);
		previousType = null;
		phase = Phase.IDLE;
		attackerId = -1;
		victimId = -1;
		focus = null;
		blendOrigin = null;
		lastShown = null;
		lastCinematic = null;
		DMZCameraBuffer.reset();
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		float fovTarget = phase == Phase.FILMING ? FOV_FACTOR : 1.0F;
		fovFactor += (fovTarget - fovFactor) * FOV_EASE;
		if (Math.abs(fovFactor - fovTarget) < 0.001F) fovFactor = fovTarget;
		if (phase == Phase.IDLE) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			reset();
			return;
		}

		if (phase == Phase.FILMING) {
			filmingTicks++;
			missingTicks = mc.level.getEntity(attackerId) == null ? missingTicks + 1 : 0;
			if (filmingTicks > MAX_FILMING_TICKS || missingTicks > MISSING_ATTACKER_TICKS) {
				release();
			} else if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
				mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			}
		} else if (secondsInPhase() >= BLEND_OUT_SECONDS) {
			finish();
		}
	}

	@SubscribeEvent
	public static void onComputeFov(ViewportEvent.ComputeFov event) {
		if (fovFactor != 1.0F) event.setFOV(event.getFOV() * fovFactor);
	}

	public static Shot computeShot(BlockGetter level, LocalPlayer player, float partialTick, Vec3 naturalPos, float naturalYaw, float naturalPitch) {
		if (phase == Phase.IDLE) return null;

		long now = System.nanoTime();
		double dt = lastFrameNanos == 0L ? 0.0 : Mth.clamp((now - lastFrameNanos) / 1.0E9, 0.0, MAX_FRAME_SECONDS);
		lastFrameNanos = now;

		Shot natural = new Shot(naturalPos, naturalYaw, naturalPitch);
		Shot cinematic = film(level, player, partialTick, dt, natural);
		if (cinematic != null) lastCinematic = cinematic;
		else cinematic = lastCinematic != null ? lastCinematic : natural;

		Shot shown;
		if (phase == Phase.FILMING) {
			double w = smoothstep(secondsInPhase() / BLEND_IN_SECONDS);
			shown = blend(blendOrigin != null ? blendOrigin : natural, cinematic, w);
		} else {
			double w = smoothstep(secondsInPhase() / BLEND_OUT_SECONDS);
			shown = blend(cinematic, natural, w);
		}
		lastShown = shown;
		return shown;
	}

	private static Shot film(BlockGetter level, LocalPlayer player, float partialTick, double dt, Shot natural) {
		ClientLevel clientLevel = Minecraft.getInstance().level;
		if (clientLevel == null) return null;
		Entity attacker = clientLevel.getEntity(attackerId);
		Entity victim = victimId >= 0 ? clientLevel.getEntity(victimId) : null;
		if (attacker == null && victim == null) return null;

		Vec3 attackerPos = attacker != null ? bodyCentre(attacker, partialTick) : bodyCentre(victim, partialTick);
		Vec3 victimPos = victim != null ? bodyCentre(victim, partialTick) : attackerPos;
		double tallest = Math.max(attacker != null ? attacker.getBbHeight() : 0.0, victim != null ? victim.getBbHeight() : 0.0);

		Vec3 axis = victimPos.subtract(attackerPos);
		double separation = axis.length();
		Vec3 flat = new Vec3(axis.x, 0.0, axis.z);
		double flatLength = flat.length();
		double far = Mth.clamp((separation - FAR_START) / (FAR_END - FAR_START), 0.0, 1.0);

		Vec3 target = attackerPos.add(victimPos).scale(0.5).lerp(bodyCentre(player, partialTick), far);
		focus = focus == null ? target : focus.add(target.subtract(focus).scale(ease(dt, FOCUS_TAU)));
		anchor = openAnchor(level, focus);

		double framedSeparation = Math.min(separation, FAR_START) * (1.0 - far);
		double wanted = Mth.clamp(DISTANCE_BASE + DISTANCE_PER_SEPARATION * framedSeparation
				+ DISTANCE_PER_EXTRA_HEIGHT * Math.max(0.0, tallest - 2.0), DISTANCE_MIN, DISTANCE_MAX);

		boolean opening = !framed;
		if (!framed) {
			Vec3 fromFocus = natural.pos().subtract(focus);
			double current = fromFocus.horizontalDistanceSqr() > 1.0E-4 ? Math.atan2(fromFocus.z, fromFocus.x) : 0.0;
			Vec3 facing = flatLength > AXIS_MIN_SEPARATION ? flat.scale(1.0 / flatLength)
					: Vec3.directionFromRotation(0.0F, attacker != null ? attacker.getYRot() : player.getYRot());
			orbitAngle = pickOpeningSide(level, player, facing, current, wanted);
			distance = wanted;
			crane = 0.0;
			reach = -1.0;
			framed = true;
		} else {
			filmSeconds += dt;
			if (flatLength > AXIS_MIN_SEPARATION) {
				double desired = closestThreeQuarter(flat.scale(1.0 / flatLength), orbitAngle);
				double delta = Mth.wrapDegrees(Math.toDegrees(desired - orbitAngle));
				double step = delta * ease(dt, YAW_TAU);
				double cap = YAW_MAX_DEGREES_PER_SECOND * dt;
				orbitAngle += Math.toRadians(Mth.clamp(step, -cap, cap));
			}
			distance += (wanted - distance) * ease(dt, DISTANCE_TAU);
		}

		double sway = Math.toRadians(SWAY_DEGREES) * Math.sin(filmSeconds * Math.PI * 2.0 / SWAY_PERIOD_SECONDS);
		double angle = orbitAngle + sway;
		Vec3 radial = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
		Vec3 ground = focus.add(radial.scale(distance)).add(0.0, HEIGHT_BASE + HEIGHT_PER_DISTANCE * distance, 0.0);
		double lift = craneNeeded(level, player, ground);
		if (lift > crane) {
			double next = crane + (lift - crane) * ease(dt, CRANE_UP_TAU);
			crane = isClear(level, player, ground.add(0.0, next, 0.0)) ? next : lift;
		} else {
			double next = crane + (lift - crane) * ease(dt, CRANE_DOWN_TAU);
			if (isClear(level, player, ground.add(0.0, next, 0.0))) crane = next;
		}
		Vec3 wantedPos = ground.add(0.0, crane, 0.0);
		Vec3 offset = wantedPos.subtract(anchor);
		double full = offset.length();
		double free = clampToLineOfSight(level, player, anchor, wantedPos).distanceTo(anchor);
		reach = reach < 0.0 || free < reach ? free : reach + (free - reach) * ease(dt, REACH_OUT_TAU);
		Vec3 camPos = full < 1.0E-4 ? wantedPos : anchor.add(offset.scale(Math.min(reach, full) / full));

		Vec3 look = focus.subtract(camPos);
		double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
		float yaw = (float) (Mth.atan2(look.z, look.x) * (180.0 / Math.PI) - 90.0);
		float pitch = Mth.clamp((float) -(Mth.atan2(look.y, horizontal) * (180.0 / Math.PI)), -PITCH_LIMIT, PITCH_LIMIT);
		if (opening) {
			lookYaw = yaw;
			lookPitch = pitch;
		} else {
			float turn = (float) ease(dt, LOOK_TAU);
			lookYaw += Mth.wrapDegrees(yaw - lookYaw) * turn;
			lookPitch += (pitch - lookPitch) * turn;
		}
		return new Shot(camPos, lookYaw, lookPitch);
	}

	private static Vec3 openAnchor(BlockGetter level, Vec3 point) {
		for (int step = 0; step <= ANCHOR_STEPS; step++) {
			Vec3 candidate = point.add(0.0, step * ANCHOR_STEP, 0.0);
			BlockPos pos = BlockPos.containing(candidate);
			if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) return candidate;
		}
		return point;
	}

	private static boolean isClear(BlockGetter level, Entity viewer, Vec3 position) {
		double length = position.distanceTo(anchor);
		return length < 1.0E-4 || clampToLineOfSight(level, viewer, anchor, position).distanceTo(anchor) / length >= CRANE_ACCEPT;
	}

	private static double craneNeeded(BlockGetter level, Entity viewer, Vec3 ground) {
		double best = 0.0;
		double bestFree = -1.0;
		for (int step = 0; step <= CRANE_STEPS; step++) {
			double lift = step * CRANE_STEP;
			Vec3 wanted = ground.add(0.0, lift, 0.0);
			double length = wanted.distanceTo(anchor);
			double free = length < 1.0E-4 ? 1.0 : clampToLineOfSight(level, viewer, anchor, wanted).distanceTo(anchor) / length;
			if (free >= CRANE_ACCEPT) return lift;
			if (free > bestFree) {
				bestFree = free;
				best = lift;
			}
		}
		return best;
	}

	private static double pickOpeningSide(BlockGetter level, Entity viewer, Vec3 facing, double currentAngle, double wanted) {
		double first = threeQuarterAngle(facing, 1.0);
		double second = threeQuarterAngle(facing, -1.0);
		double firstFree = freeFraction(level, viewer, first, wanted);
		double secondFree = freeFraction(level, viewer, second, wanted);
		if (Math.abs(firstFree - secondFree) > SIDE_PREFERENCE_MARGIN) return firstFree > secondFree ? first : second;
		double firstTurn = Math.abs(Mth.wrapDegrees(Math.toDegrees(first - currentAngle)));
		double secondTurn = Math.abs(Mth.wrapDegrees(Math.toDegrees(second - currentAngle)));
		return firstTurn <= secondTurn ? first : second;
	}

	private static double closestThreeQuarter(Vec3 facing, double currentAngle) {
		double first = threeQuarterAngle(facing, 1.0);
		double second = threeQuarterAngle(facing, -1.0);
		double firstTurn = Math.abs(Mth.wrapDegrees(Math.toDegrees(first - currentAngle)));
		double secondTurn = Math.abs(Mth.wrapDegrees(Math.toDegrees(second - currentAngle)));
		return firstTurn <= secondTurn ? first : second;
	}

	private static double threeQuarterAngle(Vec3 facing, double side) {
		double tilt = Math.toRadians(THREE_QUARTER_DEGREES);
		double x = -facing.z * side * Math.cos(tilt) - facing.x * Math.sin(tilt);
		double z = facing.x * side * Math.cos(tilt) - facing.z * Math.sin(tilt);
		return Math.atan2(z, x);
	}

	private static double freeFraction(BlockGetter level, Entity viewer, double angle, double wanted) {
		Vec3 radial = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
		Vec3 end = focus.add(radial.scale(wanted)).add(0.0, HEIGHT_BASE + HEIGHT_PER_DISTANCE * wanted, 0.0);
		double length = end.distanceTo(anchor);
		if (length < 1.0E-4) return 1.0;
		return clampToLineOfSight(level, viewer, anchor, end).distanceTo(anchor) / length;
	}

	private static Vec3 bodyCentre(Entity entity, float partialTick) {
		return entity.getPosition(partialTick).add(0.0, entity.getBbHeight() * 0.5, 0.0);
	}

	private static Shot blend(Shot from, Shot to, double w) {
		if (w <= 0.0) return from;
		if (w >= 1.0) return to;
		Vec3 pos = from.pos().lerp(to.pos(), w);
		if (focus != null) {
			Vec3 a = from.pos().subtract(focus);
			Vec3 b = to.pos().subtract(focus);
			double radiusA = Math.sqrt(a.x * a.x + a.z * a.z);
			double radiusB = Math.sqrt(b.x * b.x + b.z * b.z);
			if (radiusA > ARC_MIN_RADIUS && radiusB > ARC_MIN_RADIUS) {
				double angleA = Math.atan2(a.z, a.x);
				double turn = Math.toRadians(Mth.wrapDegrees(Math.toDegrees(Math.atan2(b.z, b.x) - angleA)));
				double angle = angleA + turn * w;
				double radius = Mth.lerp(w, radiusA, radiusB);
				pos = focus.add(Math.cos(angle) * radius, Mth.lerp(w, a.y, b.y), Math.sin(angle) * radius);
			}
		}
		float yaw = from.yaw() + (float) (Mth.wrapDegrees(to.yaw() - from.yaw()) * w);
		float pitch = (float) Mth.lerp(w, from.pitch(), to.pitch());
		return new Shot(pos, yaw, pitch);
	}

	private static double secondsInPhase() {
		return (System.nanoTime() - phaseStartNanos) / 1.0E9;
	}

	private static double ease(double dt, double tau) {
		return 1.0 - Math.exp(-dt / tau);
	}

	private static double smoothstep(double x) {
		double t = Mth.clamp(x, 0.0, 1.0);
		return t * t * (3.0 - 2.0 * t);
	}

	private static Vec3 clampToLineOfSight(BlockGetter level, Entity viewer, Vec3 from, Vec3 to) {
		Vec3 delta = to.subtract(from);
		double length = delta.length();
		if (length < 1.0E-4) return to;
		Vec3 dir = delta.scale(1.0 / length);
		BlockHitResult hit = level.clip(new ClipContext(from, from.add(dir.scale(length + WALL_MARGIN)),
				ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, viewer));
		if (hit.getType() == HitResult.Type.MISS) return to;
		double allowed = Math.max(0.0, hit.getLocation().distanceTo(from) - WALL_MARGIN);
		return allowed < length ? from.add(dir.scale(allowed)) : to;
	}
}
