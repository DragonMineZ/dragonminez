package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.ShenronModel;
import com.dragonminez.common.init.entities.ShenronEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ShenronRenderer extends GeoEntityRenderer<ShenronEntity> {

	public ShenronRenderer(EntityRendererProvider.Context renderManager) {
		super(renderManager, new ShenronModel<>());
	}

	@Override
	public void render(ShenronEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		poseStack.pushPose();
		poseStack.scale(1.5f,1.5f,1.5f);
		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
		poseStack.popPose();
	}

	@Override
	public RenderType getRenderType(ShenronEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityCutoutNoCull(texture);
	}
}
