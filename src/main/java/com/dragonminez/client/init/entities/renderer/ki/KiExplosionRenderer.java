package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.KiMeshFactory;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.common.init.entities.ki.KiExplosionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class KiExplosionRenderer extends EntityRenderer<KiExplosionEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser.png");

    public KiExplosionRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(KiExplosionEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Matrix4f basePose = new Matrix4f(poseStack.last().pose());

        PlayerEffectQueue.addKiAttack((stack, proj) -> {
            stack.pushPose();
            stack.last().pose().set(basePose);

            float ageInTicks = entity.tickCount + partialTick;
            boolean isFiring = entity.isFiring();

            float scale;
            if (!isFiring) {
                scale = entity.getSize();
            } else {
                int activeTicks = entity.tickCount - entity.getFireTick();
                scale = entity.getSize() + (activeTicks * 0.4f);
                if (scale > entity.getMaxRadius()) {
                    scale = entity.getMaxRadius();
                }
            }

            float[] coreColor = entity.getRgbColorMain();
            float[] borderColor = entity.getRgbColorBorder();
            float[] outlineColor = entity.getRgbColorOutline();

            ShaderInstance shader = DMZShaders.ki3dShader;
            if (shader != null) {
                shader.safeGetUniform("colorCore").set(coreColor[0], coreColor[1], coreColor[2]);
                shader.safeGetUniform("colorBorder").set(borderColor[0], borderColor[1], borderColor[2]);
                shader.safeGetUniform("colorOutline").set(outlineColor[0], outlineColor[1], outlineColor[2]);
                shader.safeGetUniform("time").set(ageInTicks / 20.0f);
                shader.safeGetUniform("ProjMat").set(proj);

                VertexBuffer mesh = KiMeshFactory.getSphereMesh();
                mesh.bind();

                stack.pushPose();
                stack.translate(0.0D, entity.getBbHeight() / 2.0D, 0.0D);
                stack.scale(scale, scale, scale);

                shader.safeGetUniform("ModelViewMat").set(stack.last().pose());
                shader.safeGetUniform("alphaMult").set(0.65f);
                shader.apply();
                mesh.drawWithShader(stack.last().pose(), proj, shader);

                stack.scale(1.25f, 1.25f, 1.25f);
                shader.safeGetUniform("ModelViewMat").set(stack.last().pose());
                shader.safeGetUniform("alphaMult").set(0.15f);
                shader.apply();
                mesh.drawWithShader(stack.last().pose(), proj, shader);

                stack.popPose();
                VertexBuffer.unbind();
                shader.clear();
            }

            stack.popPose();
        });
    }

    @Override
    public ResourceLocation getTextureLocation(KiExplosionEntity pEntity) {
        return TEXTURE;
    }
}