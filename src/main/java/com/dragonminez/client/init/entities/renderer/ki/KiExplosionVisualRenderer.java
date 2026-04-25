package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.KiMeshFactory;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.entities.ki.KiExplosionVisualEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class KiExplosionVisualRenderer extends EntityRenderer<KiExplosionVisualEntity> {
    private static final ResourceLocation TEXTURE_EXPLOSION = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser.png");

    public KiExplosionVisualRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(KiExplosionVisualEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Matrix4f basePose = new Matrix4f(poseStack.last().pose());

        PlayerEffectQueue.addKiAttack((stack, proj) -> {
            stack.pushPose();
            stack.last().pose().set(basePose);

            float ageInTicks = entity.tickCount + partialTick;
            float lifeTime = 25.0F;

            float progress = Math.min(ageInTicks / lifeTime, 1.0F);
            float currentScale = entity.getMaxSize() * (0.5F + (progress * 0.5F));

            float fadeStart = 0.55F;
            float maxAlpha = 1.0F;
            float currentAlpha = maxAlpha;

            if (progress > fadeStart) {
                currentAlpha = maxAlpha * (1.0F - ((progress - fadeStart) / (1.0F - fadeStart)));
            }

            float[] coreColor = entity.getRgbColorMain();
            float[] borderColor = ColorUtils.lightenColor(coreColor, 0.2f);
            float[] outlineColor = ColorUtils.lightenColor(coreColor, 0.6f);

            ShaderInstance shader = DMZShaders.ki3dShader;
            if (shader == null) {
                stack.popPose();
                return;
            }

            shader.safeGetUniform("colorCore").set(coreColor[0], coreColor[1], coreColor[2]);
            shader.safeGetUniform("colorBorder").set(borderColor[0], borderColor[1], borderColor[2]);
            shader.safeGetUniform("colorOutline").set(outlineColor[0], outlineColor[1], outlineColor[2]);
            shader.safeGetUniform("time").set(ageInTicks / 20.0f);
            shader.safeGetUniform("ProjMat").set(proj);

            VertexBuffer mesh = KiMeshFactory.getSphereMesh();
            mesh.bind();

            stack.pushPose();
            stack.translate(0.0D, -1.25D, 0.0D);
            stack.scale(currentScale, currentScale, currentScale);

            shader.safeGetUniform("ModelViewMat").set(stack.last().pose());
            shader.safeGetUniform("alphaMult").set(currentAlpha);
            shader.apply();
            mesh.drawWithShader(stack.last().pose(), proj, shader);

            stack.scale(1.2f, 1.2f, 1.2f);
            shader.safeGetUniform("ModelViewMat").set(stack.last().pose());
            shader.safeGetUniform("alphaMult").set(currentAlpha * 0.15f);
            shader.apply();
            mesh.drawWithShader(stack.last().pose(), proj, shader);

            stack.popPose();
            VertexBuffer.unbind();
            shader.clear();
            stack.popPose();
        });
    }

    @Override
    public ResourceLocation getTextureLocation(KiExplosionVisualEntity entity) {
        return TEXTURE_EXPLOSION;
    }
}