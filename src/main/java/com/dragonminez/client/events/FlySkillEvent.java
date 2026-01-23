package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.flight.FlightRollHandler;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.network.C2S.FlyToggleC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.Skill;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class FlySkillEvent {

    private static boolean pendingFlightActivation = false;
    private static Vec3 flightVector = Vec3.ZERO;
    private static Vec3 prevFlightVector = Vec3.ZERO;
    private static int verticalHover = 0;
    private static float hovering = 0F;
    private static float prevHovering = 0F;

    private static final float NORMAL_MAX_SPEED = 0.9F;
    private static final float SPRINT_MAX_SPEED = 1.8F;
    private static final float ACCELERATION = 0.08F;
    private static final float DECELERATION = 0.02F;
    private static final float SLOW_DESCENT_RATE = -0.02F;
    private static final float TURN_SPEED = 0.15F;

    private static int kiConsumptionTicks = 0;
    private static final int KI_CONSUMPTION_INTERVAL = 20;

    @SubscribeEvent
    public static void onKeyPress(InputEvent.Key event) {
        if (KeyBinds.FLY_KEY.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;

            if (player != null && mc.screen == null) {
                StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                    if (!data.getStatus().hasCreatedCharacter()) return;
                    if (data.getStatus().isStunned()) return;

                    Skill flySkill = data.getSkills().getSkill("fly");
                    if (flySkill == null || flySkill.getLevel() <= 0) return;

                    int flyLevel = flySkill.getLevel();
                    int maxEnergy = data.getMaxEnergy();

                    double energyCostPercent = Math.max(0.01, 0.04 - (flyLevel * 0.003));
                    int energyCost = (int) Math.ceil(maxEnergy * energyCostPercent);

                    if (!flySkill.isActive() && data.getResources().getCurrentEnergy() < energyCost) {
                        return;
                    }

                    if (player.onGround() && !flySkill.isActive()) {
                        player.jumpFromGround();
                        Vec3 motion = player.getDeltaMovement();
                        player.setDeltaMovement(motion.x, 0.42D, motion.z);
                        pendingFlightActivation = true;
                    } else {
                        NetworkHandler.sendToServer(new FlyToggleC2S(false));
                        if (flySkill.isActive()) {
                            resetFlightState();
                        }
                    }
                });
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) return;

        if (pendingFlightActivation) {
            if (player.getDeltaMovement().y < 0 && !player.onGround()) {
                NetworkHandler.sendToServer(new FlyToggleC2S(true));
                pendingFlightActivation = false;
            } else if (player.onGround()) {
                pendingFlightActivation = false;
            }
        }

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (!data.getStatus().hasCreatedCharacter()) return;

            Skill flySkill = data.getSkills().getSkill("fly");
            if (flySkill == null) return;

            boolean isFlying = flySkill.isActive();

            if (isFlying) {
                handleFlightMovement(player, data.getSkills().getSkillLevel("fly"));
                handleKiConsumption(player, data, flySkill);
            } else {
                resetFlightState();
            }
        });
    }

    private static void handleFlightMovement(LocalPlayer player, int flyLevel) {
        prevHovering = hovering;
        prevFlightVector = flightVector;

        float levelMultiplier = 1.0F + (0.20F * flyLevel);
        float maxNormalSpeed = NORMAL_MAX_SPEED * levelMultiplier;
        float maxSprintSpeed = SPRINT_MAX_SPEED * levelMultiplier;

        Minecraft mc = Minecraft.getInstance();
        boolean isForward = mc.options.keyUp.isDown();
        boolean isBack = mc.options.keyDown.isDown();
        boolean isLeft = mc.options.keyLeft.isDown();
        boolean isRight = mc.options.keyRight.isDown();
        boolean isJump = mc.options.keyJump.isDown();
        boolean isCrouch = mc.options.keyShift.isDown();
        boolean isSprinting = player.isSprinting();

        boolean hasInput = isForward || isBack || isLeft || isRight;
        float currentMaxSpeed = isSprinting ? maxSprintSpeed : maxNormalSpeed;
        float currentAccel = isSprinting ? ACCELERATION * 1.5F : ACCELERATION;

        Vec3 lookDir = player.getLookAngle();
        Vec3 targetDirection = Vec3.ZERO;

        if (isForward) {
            targetDirection = targetDirection.add(lookDir);
        }
        if (isBack) {
            targetDirection = targetDirection.add(lookDir.scale(-0.5));
        }
        if (isLeft) {
            Vec3 leftDir = lookDir.yRot((float) Math.toRadians(90)).normalize();
            targetDirection = targetDirection.add(new Vec3(leftDir.x, 0, leftDir.z).scale(0.7));
        }
        if (isRight) {
            Vec3 rightDir = lookDir.yRot((float) Math.toRadians(-90)).normalize();
            targetDirection = targetDirection.add(new Vec3(rightDir.x, 0, rightDir.z).scale(0.7));
        }

        double currentSpeed = flightVector.length();

        if (hasInput && targetDirection.length() > 0.001) {
            targetDirection = targetDirection.normalize();
            double targetSpeed = Math.min(currentSpeed + currentAccel, currentMaxSpeed);
            Vec3 targetVelocity = targetDirection.scale(targetSpeed);

            flightVector = new Vec3(
                Mth.lerp(TURN_SPEED, flightVector.x, targetVelocity.x),
                Mth.lerp(TURN_SPEED, flightVector.y, targetVelocity.y),
                Mth.lerp(TURN_SPEED, flightVector.z, targetVelocity.z)
            );

            hovering = Math.min(1F, hovering + 0.1F);
        } else {
            if (currentSpeed > 0.01) {
                double newSpeed = Math.max(0, currentSpeed - DECELERATION);
                if (currentSpeed > 0.001) {
                    flightVector = flightVector.normalize().scale(newSpeed);
                } else {
                    flightVector = Vec3.ZERO;
                }
            } else {
                flightVector = Vec3.ZERO;
            }
        }

        FlightRollHandler.tick();

        if (flightVector.length() > 0.01) {
            player.setDeltaMovement(flightVector);
            player.fallDistance = 0F;
            verticalHover = 0;
        } else {
            handleHovering(player, isJump, isCrouch);
        }

        if (player.onGround()) {
            NetworkHandler.sendToServer(new FlyToggleC2S(false));
            resetFlightState();
        }
    }

    private static void handleHovering(LocalPlayer player, boolean isJump, boolean isCrouch) {
        if (isJump) {
            if (verticalHover < 20) {
                verticalHover = Mth.clamp(verticalHover + 1, -20, 20);
            }
        } else if (isCrouch) {
            if (verticalHover > -20) {
                verticalHover = Mth.clamp(verticalHover - 1, -20, 20);
            }
        } else {
            if (verticalHover > -3) {
                verticalHover = Mth.clamp(verticalHover - 1, -3, 20);
            } else if (verticalHover < -3) {
                verticalHover = Mth.clamp(verticalHover + 1, -20, -3);
            }
        }

        double yMovement;
        if (verticalHover >= -3 && verticalHover <= 0 && !isJump && !isCrouch) {
            yMovement = SLOW_DESCENT_RATE + (Math.sin(player.tickCount / 10F) / 200F);
        } else if (verticalHover == 0) {
            yMovement = Math.sin(player.tickCount / 10F) / 100F;
        } else {
            yMovement = verticalHover / 60D;
        }

        player.setDeltaMovement(new Vec3(
            player.getDeltaMovement().x * 0.9,
            yMovement,
            player.getDeltaMovement().z * 0.9
        ));
        player.fallDistance = 0F;

        if (hovering < 1F) {
            hovering = Math.min(1F, hovering + 0.1F);
        }
    }

    private static void handleKiConsumption(LocalPlayer player, StatsData data, Skill flySkill) {
        kiConsumptionTicks++;

        if (kiConsumptionTicks >= KI_CONSUMPTION_INTERVAL) {
            kiConsumptionTicks = 0;

            int flyLevel = flySkill.getLevel();
            int maxEnergy = data.getMaxEnergy();

            double basePercent = player.isSprinting() ? 0.08 : 0.04;
            double energyCostPercent = Math.max(0.002, basePercent - (flyLevel * 0.001));
            int energyCost = (int) Math.ceil(maxEnergy * energyCostPercent);

            if (data.getResources().getCurrentEnergy() <= energyCost) {
                NetworkHandler.sendToServer(new FlyToggleC2S(false));
                resetFlightState();
            }
        }
    }

    private static void resetFlightState() {
        flightVector = Vec3.ZERO;
        prevFlightVector = Vec3.ZERO;
        verticalHover = 0;
        hovering = 0F;
        prevHovering = 0F;
        kiConsumptionTicks = 0;
        FlightRollHandler.reset();
    }

    public static float getFlightAnimation(float partialTicks) {
        return (float) Mth.lerp(partialTicks, prevFlightVector.length(), flightVector.length());
    }

    public static float getHoveringAnimation(float partialTicks) {
        return Mth.lerp(partialTicks, prevHovering, hovering);
    }

    public static Vec3 getFlightVector(float partialTicks) {
        return prevFlightVector.lerp(flightVector, partialTicks);
    }

    public static boolean isFlying() {
        return flightVector.length() > 0.01F || hovering > 0F;
    }

    public static float getFlightSpeed() {
        return (float) flightVector.length();
    }

    public static boolean isFlyingFast() {
        return flightVector.length() > NORMAL_MAX_SPEED;
    }
}
