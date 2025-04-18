package com.yuseix.dragonminez.common.init.entity.client.renderer.masters;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yuseix.dragonminez.common.Reference;
import com.yuseix.dragonminez.common.init.entity.client.model.masters.EnmaModel;
import com.yuseix.dragonminez.common.init.entity.custom.masters.EnmaEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class EnmaRenderer extends GeoEntityRenderer<EnmaEntity> {
	public EnmaRenderer(EntityRendererProvider.Context renderManager) {
		super(renderManager, new EnmaModel());
	}

	@Override
	public void render(EnmaEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		poseStack.pushPose();
		poseStack.scale(4.5f,4.5f,4.5f);
		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
		poseStack.popPose();
	}

	@Override
	public ResourceLocation getTextureLocation(EnmaEntity animatable) {
		return new ResourceLocation(Reference.MOD_ID, "textures/entity/masters/enma.png");
	}

}
