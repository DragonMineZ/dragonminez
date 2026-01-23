package com.dragonminez.client.flight;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.Skill;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public class FlightRollHandler {

    private static final float ROLL_SPEED = 2.5F;
    private static final float ROLL_SMOOTHING = 0.2F;

    private static float currentRoll = 0F;
    private static float prevRoll = 0F;
    private static float targetRoll = 0F;
    private static float lastYaw = 0F;
    private static boolean wasFlying = false;

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) {
            reset();
            return;
        }

        if (!ConfigManager.getUserConfig().getHud().isCameraMovementDurintFlight()) {
            if (currentRoll != 0F) {
                prevRoll = currentRoll;
                currentRoll = Mth.lerp(0.1F, currentRoll, 0F);
            }
            return;
        }

        boolean isFlying = isPlayerFlying(player);

        if (!isFlying) {
            if (Math.abs(currentRoll) > 0.1F) {
                prevRoll = currentRoll;
                float normalizedRoll = normalizeAngle(currentRoll);
                currentRoll = Mth.lerp(0.08F, currentRoll, currentRoll - normalizedRoll);
            } else {
                currentRoll = 0F;
                prevRoll = 0F;
            }
            targetRoll = 0F;
            lastYaw = player.getYRot();
            wasFlying = false;
            return;
        }

        if (!wasFlying) {
            lastYaw = player.getYRot();
            wasFlying = true;
        }

        prevRoll = currentRoll;

        float currentYaw = player.getYRot();
        float deltaYaw = currentYaw - lastYaw;
        lastYaw = currentYaw;

        deltaYaw = normalizeAngle(deltaYaw);

        boolean isLeft = mc.options.keyLeft.isDown();
        boolean isRight = mc.options.keyRight.isDown();

        float rollChange = 0F;

        if (isLeft) {
            rollChange = -ROLL_SPEED;
        } else if (isRight) {
            rollChange = ROLL_SPEED;
        } else if (Math.abs(deltaYaw) > 0.1F) {
            rollChange = deltaYaw * 0.8F;
        }

        if (Math.abs(rollChange) > 0.01F) {
            targetRoll = currentRoll + rollChange;
        } else {
            targetRoll = currentRoll * 0.85F;
            if (Math.abs(targetRoll) < 0.5F) {
                targetRoll = 0F;
            }
        }

        currentRoll = Mth.lerp(ROLL_SMOOTHING, currentRoll, targetRoll);
    }

    private static float normalizeAngle(float angle) {
        while (angle > 180F) angle -= 360F;
        while (angle < -180F) angle += 360F;
        return angle;
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

    public static float getRoll(float partialTicks) {
        return Mth.lerp(partialTicks, prevRoll, currentRoll);
    }

    public static float getCurrentRoll() {
        return currentRoll;
    }

    public static boolean isRolling() {
        if (!ConfigManager.getUserConfig().getHud().isCameraMovementDurintFlight()) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) return false;

        return isPlayerFlying(player);
    }

    public static boolean hasActiveRoll() {
        return Math.abs(currentRoll) > 0.1F;
    }

    public static void reset() {
        currentRoll = 0F;
        prevRoll = 0F;
        targetRoll = 0F;
        wasFlying = false;
    }
}
