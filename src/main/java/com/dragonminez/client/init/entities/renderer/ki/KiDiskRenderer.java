package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.KiMeshFactory;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.common.init.entities.ki.KiDiskEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class KiDiskRenderer extends EntityRenderer<KiDiskEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kidisc.png");

    public KiDiskRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(KiDiskEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Matrix4f basePose = new Matrix4f(poseStack.last().pose());

        PlayerEffectQueue.addKiAttack((stack, proj) -> {
            stack.pushPose();
            stack.last().pose().set(basePose);

            float ageInTicks = entity.tickCount + partialTick;
            float scale = entity.getSize();
            boolean isFiring = entity.isFiring();
            float castTime = (float) entity.getCastTime();

            if (!isFiring) {
                if (castTime > 0.1F && ageInTicks <= castTime) {
                    scale *= (ageInTicks / castTime);
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

                VertexBuffer mesh = KiMeshFactory.getCylinderMesh();
                mesh.bind();

                stack.pushPose();

                stack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));

                if (isFiring) {
                    stack.mulPose(Axis.XP.rotationDegrees(entity.getXRot() + 90.0F));
                } else {
                    stack.mulPose(Axis.XP.rotationDegrees(90.0F));
                }

                stack.mulPose(Axis.ZP.rotationDegrees(ageInTicks * 35.0f));

                stack.translate(0, 0, -0.025D);
                stack.scale(scale, scale, 0.05f);

                shader.safeGetUniform("ModelViewMat").set(stack.last().pose());
                shader.safeGetUniform("alphaMult").set(1.0f);
                shader.apply();
                mesh.drawWithShader(stack.last().pose(), proj, shader);

                stack.scale(1.2f, 1.2f, 1.5f);
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
    public ResourceLocation getTextureLocation(KiDiskEntity entity) {
        return TEXTURE;
    }
}