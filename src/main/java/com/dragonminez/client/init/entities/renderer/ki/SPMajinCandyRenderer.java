package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.render.effects.AuraRenderer;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.init.entities.ki.SPMajinCandyEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Random;

public class SPMajinCandyRenderer extends EntityRenderer<SPMajinCandyEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png");

    public SPMajinCandyRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(SPMajinCandyEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        float ageInTicks = entity.tickCount + partialTick;
        boolean isFiring = entity.isFiring();

        float[] color = new float[]{1.0f, 0.4f, 0.8f};
        float alpha = 1.0f;

        if (!isFiring) {
            renderCastingLightning(poseStack, entity, buffer, color, alpha, ageInTicks);
        } else {
            LivingEntity target = entity.getTargetEntity();
            if (target != null && target.isAlive()) {
                renderBeamLightning(poseStack, entity, target, buffer, color, alpha, ageInTicks, partialTick);
            }
        }
    }

    private void renderCastingLightning(PoseStack poseStack, SPMajinCandyEntity entity, MultiBufferSource buffer, float[] color, float alpha, float ageInTicks) {
        ShaderInstance shader = DMZShaders.lightningShader;
        VertexBuffer mesh = AuraRenderer.getLightningMesh();

        if (shader == null || mesh == null) return;

        setupShader(shader, color, alpha, ageInTicks, false);

        RenderType lightningType = ModRenderTypes.getCustomLightning(TEXTURE);
        lightningType.setupRenderState();

        shader.apply();
        mesh.bind();

        Random seededRand = new Random((long) (entity.getId() + ((int)ageInTicks * 5)));
        float dynamicWidth = 0.6f;

        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0D, -0.2D, 0.0D);

            poseStack.mulPose(Axis.XP.rotationDegrees(seededRand.nextFloat() * 360));
            poseStack.mulPose(Axis.YP.rotationDegrees(seededRand.nextFloat() * 360));
            poseStack.mulPose(Axis.ZP.rotationDegrees(seededRand.nextFloat() * 360));

            float individualScale = dynamicWidth * (0.8f + seededRand.nextFloat() * 0.4f);
            poseStack.scale(individualScale, individualScale, individualScale);

            shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());
            shader.safeGetUniform("normalMatrix").set(new Matrix4f(new Matrix3f(poseStack.last().normal())));
            shader.apply();

            mesh.drawWithShader(poseStack.last().pose(), RenderSystem.getProjectionMatrix(), shader);
            poseStack.popPose();
        }

        VertexBuffer.unbind();
        shader.clear();
        lightningType.clearRenderState();
    }

    private void renderBeamLightning(PoseStack poseStack, SPMajinCandyEntity entity, LivingEntity target, MultiBufferSource buffer, float[] color, float alpha, float ageInTicks, float partialTick) {
        ShaderInstance shader = DMZShaders.lightningShader;
        VertexBuffer mesh = AuraRenderer.getLightningMesh();

        if (shader == null || mesh == null) return;

        Vec3 startPos = entity.getPosition(partialTick);
        Vec3 targetPos = target.getEyePosition(partialTick);

        double dx = targetPos.x - startPos.x;
        double dy = targetPos.y - startPos.y;
        double dz = targetPos.z - startPos.z;

        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float horizontalDistance = (float) Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Mth.atan2(dz, dx) * (180.0F / (float) Math.PI)) - 90.0F;
        float pitch = (float) -(Mth.atan2(dy, horizontalDistance) * (180.0F / (float) Math.PI));

        setupShader(shader, color, alpha, ageInTicks, true);

        RenderType lightningType = ModRenderTypes.getCustomLightning(TEXTURE);
        lightningType.setupRenderState();

        shader.apply();
        mesh.bind();

        Random seededRand = new Random((long) (entity.getId() + ((int)ageInTicks * 5)));
        float thickness = 0.8f; // Grosor del rayo

        for (int i = 0; i < 6; i++) {
            poseStack.pushPose();

            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

            poseStack.mulPose(Axis.ZP.rotationDegrees(seededRand.nextFloat() * 360));

            float scaleX = thickness * (0.8f + seededRand.nextFloat() * 0.4f);
            float scaleY = thickness * (0.8f + seededRand.nextFloat() * 0.4f);
            float scaleZ = distance;

            poseStack.scale(scaleX, scaleY, scaleZ);

            shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());
            shader.safeGetUniform("normalMatrix").set(new Matrix4f(new Matrix3f(poseStack.last().normal())));
            shader.apply();

            mesh.drawWithShader(poseStack.last().pose(), RenderSystem.getProjectionMatrix(), shader);
            poseStack.popPose();
        }

        VertexBuffer.unbind();
        shader.clear();
        lightningType.clearRenderState();
    }

    private void setupShader(ShaderInstance shader, float[] color, float alpha, float ageInTicks, boolean isFiring) {
        shader.safeGetUniform("time").set(ageInTicks / 20.0f);
        shader.safeGetUniform("speedModifier").set(isFiring ? 2.5f : 1.5f);
        shader.safeGetUniform("color1").set(1.0f, 1.0f, 1.0f);
        shader.safeGetUniform("color2").set(color[0], color[1], color[2]);
        shader.safeGetUniform("alp1").set(alpha);
        shader.safeGetUniform("alp2").set(0.1f * alpha);
        shader.safeGetUniform("projectionMatrix").set(RenderSystem.getProjectionMatrix());
    }

    @Override
    public ResourceLocation getTextureLocation(SPMajinCandyEntity entity) {
        return TEXTURE;
    }
}