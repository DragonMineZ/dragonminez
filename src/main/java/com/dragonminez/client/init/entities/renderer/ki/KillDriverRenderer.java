package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.KiMeshFactory;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.common.init.entities.ki.KillDriverEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class KillDriverRenderer extends EntityRenderer<KillDriverEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kidisc.png");
    private static final float SPIN_SPEED = 18.0F;
    private static final float PULSE_SPEED = 0.45F;
    private static final float PULSE_AMOUNT = 0.06F;

    public KillDriverRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(KillDriverEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float growth = entity.getGrowth(partialTick);
        if (growth <= 0.01F) return;

        Matrix4f basePose = new Matrix4f(poseStack.last().pose());
        float age = entity.tickCount + partialTick;
        float pulse = 1.0F + PULSE_AMOUNT * (float) Math.sin(age * PULSE_SPEED);
        float tube = entity.getSize() * KillDriverEntity.TUBE_RATIO * growth * pulse;
        float yaw = entity.getRingYaw();
        float pitch = entity.getRingPitch();
        float[] core = entity.getRgbColorMain();
        float[] border = entity.getRgbColorBorder();
        float[] outline = entity.getRgbColorOutline();

        PlayerEffectQueue.addKiAttack((stack, proj) -> {
            ShaderInstance shader = DMZShaders.ki3dShader;
            if (shader == null) return;

            stack.pushPose();
            stack.last().pose().set(basePose);
            stack.mulPose(Axis.YP.rotationDegrees(-yaw));
            stack.mulPose(Axis.XP.rotationDegrees(pitch));
            stack.mulPose(Axis.ZP.rotationDegrees(age * SPIN_SPEED));
            stack.scale(tube, tube, tube);

            shader.safeGetUniform("colorCore").set(core[0], core[1], core[2]);
            shader.safeGetUniform("colorBorder").set(border[0], border[1], border[2]);
            shader.safeGetUniform("colorOutline").set(outline[0], outline[1], outline[2]);
            shader.safeGetUniform("time").set(age / 20.0F);
            shader.safeGetUniform("ProjMat").set(proj);
            shader.safeGetUniform("shapeMode").set(0.0F);
            shader.safeGetUniform("texBlend").set(0.0F);
            shader.safeGetUniform("zCut").set(-1.0F);
            shader.safeGetUniform("zCutFar").set(2.0F);

            VertexBuffer mesh = KiMeshFactory.getTorusMesh(1.0F / KillDriverEntity.TUBE_RATIO);
            mesh.bind();
            shader.safeGetUniform("ModelViewMat").set(stack.last().pose());
            shader.safeGetUniform("alphaMult").set(1.0F);
            shader.apply();
            mesh.drawWithShader(stack.last().pose(), proj, shader);
            VertexBuffer.unbind();
            shader.clear();

            stack.popPose();
        });
    }

    @Override
    public ResourceLocation getTextureLocation(KillDriverEntity entity) {
        return TEXTURE;
    }
}
