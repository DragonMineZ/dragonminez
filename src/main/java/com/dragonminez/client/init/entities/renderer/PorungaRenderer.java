package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.PorungaModel;
import com.dragonminez.common.init.entities.PorungaEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PorungaRenderer extends GeoEntityRenderer<PorungaEntity> {

	public PorungaRenderer(EntityRendererProvider.Context renderManager) {
		super(renderManager, new PorungaModel<>());
	}

	@Override
	public void render(PorungaEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		poseStack.pushPose();
		poseStack.scale(1.1f,1.1f,1.1f);
		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
		poseStack.popPose();
	}

	@Override
	public RenderType getRenderType(PorungaEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(texture);
	}
}
