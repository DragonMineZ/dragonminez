package com.dragonminez.client.flight;

import com.dragonminez.common.config.CombatConfig;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.network.C2S.CombatFlyImpulseC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.util.GravityLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;


public class CombatFlightHandler {

	private static final int DIR_FORWARD = 0;
	private static final int DIR_BACK = 1;
	private static final int DIR_LEFT = 2;
	private static final int DIR_RIGHT = 3;
	private static final int DIR_UP = 4;
	private static final int DIR_DOWN = 5;
	private static final int DIR_COUNT = 6;

	private static final long DOUBLE_TAP_WINDOW_MS = 250;
	private static final float BASE_ATTRIBUTE_FLY_SPEED = 0.35F;
	private static final float SMOOTHING = 0.35F;
	private static final float BURST_DECAY = 0.88F;
	private static final double PASSIVE_DESCENT = -0.05D;
	private static final double VERTICAL_RATE = 0.55D;
	private static final double MIN_GROUND_CLEARANCE = 0.25D;

	private static Vec3 velocity = Vec3.ZERO;
	private static Vec3 burstVelocity = Vec3.ZERO;

	private static final boolean[] wasDown = new boolean[DIR_COUNT];
	private static final long[] lastTapTime = new long[DIR_COUNT];

	private static int sustainedDir = -1;
	private static long lastImpulseTime = 0;

	public static void handle(LocalPlayer player, StatsData data) {
		CombatConfig config = ConfigManager.getCombatConfig();
		int flyLevel = data.getSkills().getSkillLevel("fly");
		float speedScale = getFlySpeedScale(player);
		float levelMultiplier = 1.0F + (0.20F * flyLevel);

		double baseSpeed = config.getCombatFlyBaseSpeed() * levelMultiplier * speedScale;
		double sprintSpeed = config.getCombatFlySprintSpeed() * levelMultiplier * speedScale;

		Minecraft mc = Minecraft.getInstance();
		boolean forward = mc.options.keyUp.isDown();
		boolean back = mc.options.keyDown.isDown();
		boolean left = mc.options.keyLeft.isDown();
		boolean right = mc.options.keyRight.isDown();
		boolean jump = mc.options.keyJump.isDown();
		boolean descend = mc.options.keyShift.isDown();
		boolean sprint = mc.options.keySprint.isDown();

		float yaw = player.getYRot();
		Vec3 fwdDir = Vec3.directionFromRotation(0, yaw).normalize();
		Vec3 rightDir = fwdDir.cross(new Vec3(0, 1, 0)).normalize();

		boolean[] pressed = {forward, back, left, right, jump, descend};
		handleDoubleTaps(player, pressed, fwdDir, rightDir, config, speedScale, flyLevel);

		Vec3 horizontal = Vec3.ZERO;
		if (forward) horizontal = horizontal.add(fwdDir);
		if (back) horizontal = horizontal.add(fwdDir.scale(-1));
		if (left) horizontal = horizontal.add(rightDir.scale(-1));
		if (right) horizontal = horizontal.add(rightDir);

		boolean hasHorizontal = horizontal.lengthSqr() > 1.0E-4;
		double maxHorizontal = sprint ? sprintSpeed : baseSpeed;
		if (isSustainedHorizontal() && pressed[sustainedDir]) maxHorizontal *= config.getCombatFlyHoldSpeedMultiplier();

		Vec3 targetHorizontal = hasHorizontal ? horizontal.normalize().scale(maxHorizontal) : Vec3.ZERO;

		double maxVertical = VERTICAL_RATE * speedScale;
		if ((sustainedDir == DIR_UP || sustainedDir == DIR_DOWN) && pressed[sustainedDir]) maxVertical *= config.getCombatFlyHoldSpeedMultiplier();

		double targetY;
		if (jump) targetY = maxVertical;
		else if (descend) targetY = -maxVertical;
		else targetY = PASSIVE_DESCENT;

		Vec3 target = new Vec3(targetHorizontal.x, targetY, targetHorizontal.z);

		velocity = new Vec3(
				Mth.lerp(SMOOTHING, velocity.x, target.x),
				Mth.lerp(SMOOTHING, velocity.y, target.y),
				Mth.lerp(SMOOTHING, velocity.z, target.z)
		);

		burstVelocity = burstVelocity.scale(BURST_DECAY);
		if (burstVelocity.lengthSqr() < 1.0E-6) burstVelocity = Vec3.ZERO;

		Vec3 combined = velocity.add(burstVelocity);

		if (!jump && combined.y < 0 && getGroundDistance(player) <= MIN_GROUND_CLEARANCE) {
			velocity = new Vec3(velocity.x, Math.max(0.0D, velocity.y), velocity.z);
			burstVelocity = new Vec3(burstVelocity.x, 0.0, burstVelocity.z);
			combined = velocity.add(burstVelocity);
		}

		if (GravityLogic.isFlightHardStopped(player)) {
			velocity = Vec3.ZERO;
			burstVelocity = Vec3.ZERO;
			player.setDeltaMovement(0, -1.5, 0);
			return;
		}

		double flyFactor = GravityLogic.getFlyFactor(player);
		if (flyFactor < 1.0) combined = combined.scale(flyFactor);

		player.setDeltaMovement(combined);
		player.fallDistance = 0F;
	}

	private static void handleDoubleTaps(LocalPlayer player, boolean[] pressed, Vec3 fwdDir, Vec3 rightDir, CombatConfig config, float speedScale, int flyLevel) {
		long now = System.currentTimeMillis();
		long impulseCooldownMs = config.getCombatFlyImpulseCooldownTicks() * 50L;

		if (sustainedDir != -1 && !pressed[sustainedDir]) sustainedDir = -1;

		for (int dir = 0; dir < DIR_COUNT; dir++) {
			boolean down = pressed[dir];
			if (down && !wasDown[dir]) {
				boolean doubleTap = (now - lastTapTime[dir]) <= DOUBLE_TAP_WINDOW_MS;
				lastTapTime[dir] = now;

				if (doubleTap && (now - lastImpulseTime) >= impulseCooldownMs) {
					applyImpulse(player, dir, fwdDir, rightDir, config, speedScale, flyLevel);
					lastImpulseTime = now;
					sustainedDir = dir;
				}
			}
			wasDown[dir] = down;
		}
	}

	private static void applyImpulse(LocalPlayer player, int dir, Vec3 fwdDir, Vec3 rightDir, CombatConfig config, float speedScale, int flyLevel) {
		Vec3 impulseDir = switch (dir) {
			case DIR_FORWARD -> fwdDir;
			case DIR_BACK -> fwdDir.scale(-1);
			case DIR_LEFT -> rightDir.scale(-1);
			case DIR_RIGHT -> rightDir;
			case DIR_UP -> new Vec3(0, 1, 0);
			case DIR_DOWN -> new Vec3(0, -1, 0);
			default -> Vec3.ZERO;
		};

		double levelScale = 1.0 + (0.20 * flyLevel);
		double strength = config.getCombatFlyImpulseStrength() * speedScale * levelScale;
		burstVelocity = burstVelocity.add(impulseDir.scale(strength));
		player.fallDistance = 0F;

		NetworkHandler.sendToServer(new CombatFlyImpulseC2S(dir));
	}

	private static boolean isSustainedHorizontal() {
		return sustainedDir == DIR_FORWARD || sustainedDir == DIR_BACK || sustainedDir == DIR_LEFT || sustainedDir == DIR_RIGHT;
	}

	private static float getFlySpeedScale(LocalPlayer player) {
		double attrValue = player.getAttributes().hasAttribute(EntityAttributes.FLY_SPEED.get()) ? player.getAttributeValue(EntityAttributes.FLY_SPEED.get()) : 0.0;
		if (attrValue <= 0.0) return 1.0F;
		double scale = attrValue / BASE_ATTRIBUTE_FLY_SPEED;
		return (float) Mth.clamp(scale, 0.25, 4.0);
	}

	private static double getGroundDistance(LocalPlayer player) {
		Vec3 start = new Vec3(player.getX(), player.getBoundingBox().minY, player.getZ());
		Vec3 end = start.add(0.0D, -1.5D, 0.0D);
		HitResult hit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (hit.getType() == HitResult.Type.MISS) return Double.MAX_VALUE;
		return start.y - hit.getLocation().y;
	}

	public static void initFromMotion(LocalPlayer player) {
		velocity = player.getDeltaMovement();
		burstVelocity = Vec3.ZERO;
	}

	public static void reset() {
		velocity = Vec3.ZERO;
		burstVelocity = Vec3.ZERO;
		sustainedDir = -1;
		lastImpulseTime = 0;
		for (int i = 0; i < DIR_COUNT; i++) {
			wasDown[i] = false;
			lastTapTime[i] = 0;
		}
	}
}
