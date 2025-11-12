package com.dragonminez.client.util;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.UseAnim;
import org.joml.Vector3d;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;

public class RenderUtil {

    public static void rotateHead(AbstractClientPlayer animatable, CoreGeoBone bone, float partialTick) {
        final float lerpBodyRot = Mth.lerp(partialTick, animatable.yBodyRotO,
                animatable.yBodyRot);
        final float lerpHeadRot = Mth.lerp(partialTick, animatable.yHeadRotO,
                animatable.yHeadRot);
        final float netHeadYaw = lerpHeadRot - lerpBodyRot;
        final float netHeadPitch = Mth.lerp(partialTick, animatable.xRotO,
                animatable.getXRot());

        bone.setRotX(-netHeadPitch * 0.017453292F);
        bone.setRotY(-netHeadYaw * 0.017453292F);
    }

    public static void animateHand(AbstractClientPlayer animatable, CoreGeoBone armBone,
                                   float partialTick, float ageInTicks) {

        // Solo aplicar animaciones procedurales para arco y ballesta
        // Las animaciones de ataque normal son manejadas por GeckoLib
        UseAnim useAction = animatable.getUseItem().getUseAnimation();

        if (useAction == UseAnim.BOW) {
            RenderUtil.animateBowHand(animatable, armBone, ageInTicks);
            return;
        }

        // Verificar si tiene ballesta en mano (cargada, cargando o disparando)
        if (animatable.getUseItem().getItem() instanceof CrossbowItem ||
            animatable.getMainHandItem().getItem() instanceof CrossbowItem) {
            RenderUtil.animateCrossbowHand(animatable, armBone, ageInTicks);
            return;
        }

        // No aplicar otras animaciones procedurales, dejar que GeckoLib maneje el resto
    }

    private static void animateBowHand(AbstractClientPlayer player, CoreGeoBone armBone, float ageInTicks) {
        final boolean armIsLeft = armBone.getName().equals("left_arm");

        final float animTime = ageInTicks;

        float pitch = player.getXRot() * Mth.DEG_TO_RAD; // getXRot()
        float yawDelta = (player.getYHeadRot() - player.yBodyRot) * Mth.DEG_TO_RAD; // yHeadRot, yBodyRot

        float minPitch = -0.1F;
        float maxPitch = 1.0F;
        pitch = Mth.clamp(pitch, minPitch, maxPitch);

        float yawScale = 0.3F;
        float minYaw = -0.5F;
        float maxYaw = 0.5F;
        float yaw = Mth.clamp(yawDelta * yawScale, minYaw, maxYaw);

        float baseRotX = 1.5F - pitch * 0.5F;
        armBone.setRotX(baseRotX);
        armBone.setRotY(armIsLeft ? -0.5F + yaw : 0.3F + yaw);
        armBone.setRotZ(-0.2F);
        armBone.setRotZ(armBone.getRotZ() + Mth.sin(animTime * 2.0F) * 0.02F);
    }

    private static void animateCrossbowHand(AbstractClientPlayer player, CoreGeoBone armBone, float ageInTicks) {
        final boolean armIsLeft = armBone.getName().equals("left_arm");
        final float animTime = ageInTicks;

        // Verificar si tiene ballesta en mano principal u offhand
        boolean hasMainHandCrossbow = player.getMainHandItem().getItem() instanceof CrossbowItem;
        boolean hasOffHandCrossbow = player.getOffhandItem().getItem() instanceof CrossbowItem;

        // Verificar estados
        boolean isCharging = player.isUsingItem() && player.getUseItem().getItem() instanceof CrossbowItem;
        boolean isMainHandCharged = hasMainHandCrossbow && CrossbowItem.isCharged(player.getMainHandItem());
        boolean isOffHandCharged = hasOffHandCrossbow && CrossbowItem.isCharged(player.getOffhandItem());

        // Si está cargando, cargada en cualquier mano, o usando
        if (isCharging || isMainHandCharged || isOffHandCharged) {
            float pitch = player.getXRot() * Mth.DEG_TO_RAD;
            float yawDelta = (player.getYHeadRot() - player.yBodyRot) * Mth.DEG_TO_RAD;

            float minPitch = -0.2F;
            float maxPitch = 0.8F;
            pitch = Mth.clamp(pitch, minPitch, maxPitch);

            float yawScale = 0.25F;
            float minYaw = -0.4F;
            float maxYaw = 0.4F;
            float yaw = Mth.clamp(yawDelta * yawScale, minYaw, maxYaw);

            float baseRotX = 1.2F - pitch * 0.5F;
            armBone.setRotX(baseRotX);
            armBone.setRotY(armIsLeft ? -0.4F + yaw : 0.4F + yaw);
            armBone.setRotZ(armIsLeft ? 0.15F : -0.15F);

            // Pequeña animación de respiración cuando está apuntando
            if (!isCharging) {
                armBone.setRotZ(armBone.getRotZ() + Mth.sin(animTime * 1.5F) * 0.01F);
            }
        }
    }

    public static void playProceduralAnimations(AbstractClientPlayer player, CoreGeoBone bone,
                                                float partialTick, float ageInTicks) {
        if (bone.getName().equals("head")) {
            RenderUtil.rotateHead(player, bone, partialTick);
        }
        if (bone.getName().equals("right_arm") || bone.getName().equals("left_arm")) {
            RenderUtil.animateHand(player, bone, partialTick, ageInTicks);
        }
    }

    public static boolean isMoving(LivingEntity entity) {
        final Vector3d currentPos = new Vector3d(entity.getX(), entity.getY(), entity.getZ());
        final Vector3d lastPos = new Vector3d(entity.xo, entity.yo, entity.zo);
        final Vector3d expectedVelocity = currentPos.sub(lastPos);
        float avgVelocity = (float) (Math.abs(expectedVelocity.x) + Math.abs(expectedVelocity.z) / 2.0);
        return avgVelocity >= 0.015;
    }
}