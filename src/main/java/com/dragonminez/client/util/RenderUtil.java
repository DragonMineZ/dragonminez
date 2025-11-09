package com.dragonminez.client.util;

import com.eliotlash.mclib.utils.MathHelper;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.UseAnim;
import org.joml.Vector3d;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.molang.MolangQueries;

public class RenderUtil {

    public static void rotateHead(AbstractClientPlayer animatable, GeoBone bone, float partialTick) {
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

    public static void animateHand(AbstractClientPlayer animatable, GeoBone armBone,
                                   float partialTick, float ageInTicks) {

        UseAnim useAction = animatable.getUseItem().getUseAnimation();

        if (useAction == UseAnim.BOW) {
            RenderUtil.animateBowHand(animatable, armBone, ageInTicks);
            return;
        }

        if (animatable.getUseItem().getItem() instanceof CrossbowItem) {
            RenderUtil.animateCrossbowHand(animatable, armBone, ageInTicks);
            return;
        }

        final float handSwingProgress = animatable.getAttackAnim(partialTick);
        final boolean armBoneIsLeft = armBone.getName().equals("left_arm");
        final boolean isPlayerLeftHanded = animatable.getMainArm() == HumanoidArm.LEFT;

        if ((armBoneIsLeft && !isPlayerLeftHanded) || (!armBoneIsLeft && isPlayerLeftHanded)) {
            return;
        }

        if (handSwingProgress == 0) {
            return;
        }

        final float animTime = ageInTicks;

        final float rotX = (-Mth.cos(handSwingProgress * 2.0F * Mth.PI) + 1.0F)
                * 0.7925268F;
        float rotY = Mth.sin(handSwingProgress * Mth.PI) * handSwingProgress;
        float rotZ = Mth.cos(animTime * 3.60F) * 0.1F + 0.1F;

        if (armBoneIsLeft) {
            rotY = -rotY;
            rotZ = -rotZ;
        }

        armBone.setRotX(rotX);
        armBone.setRotY(rotY);
        armBone.setRotZ(rotZ);
    }

    private static void animateBowHand(AbstractClientPlayer player, GeoBone armBone, float ageInTicks) {
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

    private static void animateCrossbowHand(AbstractClientPlayer player, GeoBone armBone, float ageInTicks) {
        if (!CrossbowItem.isCharged(player.getUseItem())) {
            return;
        }
        final boolean armIsLeft = armBone.getName().equals("left_arm");

        final float animTime = ageInTicks;

        float pitch = player.getXRot() * Mth.DEG_TO_RAD; // getXRot()
        float yawDelta = (player.getYHeadRot() - player.yBodyRot) * Mth.DEG_TO_RAD; // yHeadRot, yBodyRot

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
        armBone.setRotZ(-0.15F);
        armBone.setRotZ(armBone.getRotZ() + Mth.sin(animTime * 1.5F) * 0.01F);
    }

    public static void playProceduralAnimations(AbstractClientPlayer player, GeoBone bone,
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