package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.render.effects.LightningBoltRenderer;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.common.init.entities.ki.SPMajinCandyEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SPMajinCandyRenderer extends EntityRenderer<SPMajinCandyEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png");
    private static final float[] COLOR = new float[]{1.0f, 0.4f, 0.8f};
    private static final float CAST_HEIGHT = 0.9f;
    private static final float CAST_RADIUS = 0.35f;
    private static final float BEAM_RADIUS = 0.3f;
    private static final float CAST_SPEED = 1.2f;
    private static final float BEAM_SPEED = 1.8f;

    public SPMajinCandyRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(SPMajinCandyEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        float ageInTicks = entity.tickCount + partialTick;
        Matrix4f pose;
        float length;
        float radius;
        float speed;

        if (!entity.isFiring()) {
            pose = new Matrix4f(poseStack.last().pose()).translate(0.0f, -CAST_HEIGHT * 0.5f, 0.0f);
            length = CAST_HEIGHT;
            radius = CAST_RADIUS;
            speed = CAST_SPEED;
        } else {
            LivingEntity target = entity.getTargetEntity();
            if (target == null || !target.isAlive()) return;

            Vec3 delta = target.getEyePosition(partialTick).subtract(entity.getPosition(partialTick));
            length = (float) delta.length();
            if (length < 0.01f) return;

            Quaternionf toTarget = new Quaternionf().rotationTo(new Vector3f(0.0f, 1.0f, 0.0f), delta.toVector3f().normalize());
            pose = new Matrix4f(poseStack.last().pose()).rotate(toTarget);
            radius = BEAM_RADIUS;
            speed = BEAM_SPEED;
        }

        int seed = entity.getId();
        if (IrisCompat.isShaderPackInUse()) {
            PlayerEffectQueue.addEntityEffect(() -> LightningBoltRenderer.drawColumn(pose, RenderSystem.getProjectionMatrix(), seed, ageInTicks, length, radius, COLOR, true, speed, 1.0f));
        } else {
            LightningBoltRenderer.drawColumn(pose, RenderSystem.getProjectionMatrix(), seed, ageInTicks, length, radius, COLOR, true, speed, 1.0f);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(SPMajinCandyEntity entity) {
        return TEXTURE;
    }
}
