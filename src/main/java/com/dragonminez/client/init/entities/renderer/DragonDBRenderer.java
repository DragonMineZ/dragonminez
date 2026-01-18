package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.DragonDBModel;
import com.dragonminez.common.init.entities.dragon.DragonWishEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DragonDBRenderer extends GeoEntityRenderer<DragonWishEntity> {

    private static final float HALF_SQRT_3 = (float)(Math.sqrt(3.0) / 2.0);


    public DragonDBRenderer(EntityRendererProvider.Context renderManager) {
		super(renderManager, new DragonDBModel<>());
	}

	@Override
	public void render(DragonWishEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		poseStack.pushPose();
		poseStack.scale(1.5f,1.5f,1.5f);
		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
		poseStack.popPose();

        if (!entity.hasGrantedWish()) {
            renderWishEffect(entity, partialTick, poseStack, bufferSource, 255, 215, 0);
        }
	}

	@Override
	public RenderType getRenderType(DragonWishEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityCutoutNoCull(texture);
	}

    private void renderWishEffect(DragonWishEntity entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int r, int g, int b) {
        float rotationTime = (entity.tickCount + partialTicks) * 0.01F;
        float rawSin = Mth.sin((entity.tickCount + partialTicks) * 0.1F);
        float normalizedFade = (rawSin + 1.0F) / 2.0F;
        float fade = 0.4F + (normalizedFade * 0.6F);
        float intensity = 0.6F;

        RandomSource randomsource = RandomSource.create(432L);
        VertexConsumer vertexconsumer = buffer.getBuffer(RenderType.lightning());

        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.0F);
        poseStack.scale(1.0F, 1.0F, 1.0F);

        for(int i = 0; (float)i < (intensity + intensity * intensity) / 2.0F * 60.0F; ++i) {
            poseStack.mulPose(Axis.XP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(randomsource.nextFloat() * 360.0F + rotationTime * 90.0F));

            float width = randomsource.nextFloat() * 5.0F + 4.0F;
            float length = randomsource.nextFloat() * 1.0F + 0.5F;

            Matrix4f matrix4f = poseStack.last().pose();

            int alpha = (int)(255.0F * fade);

            vertex01(vertexconsumer, matrix4f, alpha, r, g, b);
            vertex2(vertexconsumer, matrix4f, width, length, r, g, b, alpha);
            vertex3(vertexconsumer, matrix4f, width, length, r, g, b, alpha);
            vertex01(vertexconsumer, matrix4f, alpha, r, g, b);
            vertex3(vertexconsumer, matrix4f, width, length, r, g, b, alpha);
            vertex4(vertexconsumer, matrix4f, width, length, r, g, b, alpha);
            vertex01(vertexconsumer, matrix4f, alpha, r, g, b);
            vertex4(vertexconsumer, matrix4f, width, length, r, g, b, alpha);
            vertex2(vertexconsumer, matrix4f, width, length, r, g, b, alpha);
        }

        poseStack.popPose();
    }

    private static void vertex01(VertexConsumer pConsumer, Matrix4f pMatrix, int pAlpha, int r, int g, int b) {
        pConsumer.vertex(pMatrix, 0.0F, 0.0F, 0.0F).color(255, 255, 255, pAlpha).endVertex();
    }

    private static void vertex2(VertexConsumer pConsumer, Matrix4f pMatrix, float pWidth, float pLength, int r, int g, int b, int alpha) {
        pConsumer.vertex(pMatrix, -HALF_SQRT_3 * pLength, pWidth, -0.5F * pLength).color(r, g, b, alpha).endVertex();
    }

    private static void vertex3(VertexConsumer pConsumer, Matrix4f pMatrix, float pWidth, float pLength, int r, int g, int b, int alpha) {
        pConsumer.vertex(pMatrix, HALF_SQRT_3 * pLength, pWidth, -0.5F * pLength).color(r, g, b, alpha).endVertex();
    }

    private static void vertex4(VertexConsumer pConsumer, Matrix4f pMatrix, float pWidth, float pLength, int r, int g, int b, int alpha) {
        pConsumer.vertex(pMatrix, 0.0F, pWidth, 1.0F * pLength).color(r, g, b, alpha).endVertex();
    }

}
