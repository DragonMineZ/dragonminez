package com.dragonminez.client.flight;

import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.common.stats.Skill;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public class FlightRollHandler {

	private static final float ROLL_ACCELERATION = 1.5F;
	private static final float MAX_ROLL_SPEED = 4.0F;
	private static final float ROLL_FRICTION = 0.9F;
	private static final float STABILIZE_SPEED = 0.5F;
	private static final float MOUSE_SENSITIVITY = 0.05F;

	private static float currentRoll = 0F;
	private static float prevRoll = 0F;
	private static float rollVelocity = 0F;
	private static float lastYaw = 0F;

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;

		if (player == null) {
			reset();
			return;
		}

		prevRoll = currentRoll;
		float currentYaw = player.getYRot();

		float deltaYaw = currentYaw - lastYaw;
		while (deltaYaw >= 180) deltaYaw -= 360;
		while (deltaYaw < -180) deltaYaw += 360;

		if (isPlayerFlying(player) && FlySkillEvent.isFlyingFast()) {

			float input = 0;
			if (player.input.left) input += 1;
			if (player.input.right) input -= 1;

			float keyForce = input * ROLL_ACCELERATION;
			float mouseForce = deltaYaw * MOUSE_SENSITIVITY;
			float totalForce = keyForce - mouseForce;

			if (Math.abs(totalForce) > 0.01F) {
				rollVelocity += totalForce;
			} else {
				float normalizedRoll = Mth.wrapDegrees(currentRoll);

				if (Math.abs(normalizedRoll) < 90) {
					float distTo0 = -normalizedRoll;
					if (Math.abs(distTo0) > 0.1f) {
						float stabilizeForce = Mth.clamp(distTo0 * 0.1f, -STABILIZE_SPEED, STABILIZE_SPEED);
						rollVelocity += stabilizeForce;
					}
				}
			}

			rollVelocity *= ROLL_FRICTION;
			rollVelocity = Mth.clamp(rollVelocity, -MAX_ROLL_SPEED, MAX_ROLL_SPEED);

			currentRoll += rollVelocity;
			rebaseRoll();
		} else {
			float target = Math.round(currentRoll / 360f) * 360f;
			currentRoll = Mth.lerp(0.1F, currentRoll, target);
			rollVelocity = 0;
		}

		lastYaw = currentYaw;
	}

	private static void rebaseRoll() {
		float wrapped = Mth.wrapDegrees(currentRoll);
		float offset = currentRoll - wrapped;
		if (offset != 0F) {
			currentRoll = wrapped;
			prevRoll -= offset;
		}
	}

	private static boolean isPlayerFlying(LocalPlayer player) {
		var statsOpt = StatsProvider.get(StatsCapability.INSTANCE, player);
		if (statsOpt.isPresent()) {
			var data = statsOpt.resolve().orElse(null);
			if (data != null) {
				Skill flySkill = data.getSkills().getSkill("fly");
				return flySkill != null && flySkill.isActive();
			}
		}
		return false;
	}

	public static void reset() {
		currentRoll = 0F;
		prevRoll = 0F;
		rollVelocity = 0F;
		lastYaw = 0F;
	}

	public static float getRoll(float partialTicks) {
		return Mth.lerp(partialTicks, prevRoll, currentRoll);
	}

	public static boolean hasActiveRoll() {
		return Math.abs(currentRoll) > 0.1f || Math.abs(rollVelocity) > 0.01f;
	}
}
