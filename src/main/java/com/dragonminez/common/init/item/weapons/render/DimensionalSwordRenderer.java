package com.dragonminez.common.init.item.weapons.render;

import com.dragonminez.common.init.item.weapons.BraveSwordItem;
import com.dragonminez.common.init.item.weapons.DimensionalSwordItem;
import com.dragonminez.common.init.item.weapons.model.BraveSwordModel;
import com.dragonminez.common.init.item.weapons.model.DimensionalSwordModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class DimensionalSwordRenderer extends GeoItemRenderer<DimensionalSwordItem> {

	public DimensionalSwordRenderer() {
		super(new DimensionalSwordModel());
	}

	@Override
	public void actuallyRender(PoseStack poseStack, DimensionalSwordItem animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

		super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

	}

	@Override
	public RenderType getRenderType(DimensionalSwordItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityCutoutNoCull(texture);
	}
}
