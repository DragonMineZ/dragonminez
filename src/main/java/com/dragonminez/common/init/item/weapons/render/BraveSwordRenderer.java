package com.dragonminez.common.init.item.weapons.render;

import com.dragonminez.common.init.item.weapons.BraveSwordItem;
import com.dragonminez.common.init.item.weapons.model.BraveSwordModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class BraveSwordRenderer extends GeoItemRenderer<BraveSwordItem> {

	public BraveSwordRenderer() {
		super(new BraveSwordModel());
	}

	@Override
	public void actuallyRender(PoseStack poseStack, BraveSwordItem animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

		// Hide the scabbard while the sword is held, then put it back. BakedGeoModel
		// instances are cached per model file in GeckoLibCache and shared by every
		// renderer that uses them, so leaving the bone hidden here also hides it in
		// DMZRacePartsLayer, which draws the sheathed sword on the player's back.
		GeoBone sheath = model.getBone("cubretodo").orElse(null);
		boolean wasHidden = sheath != null && sheath.isHidden();
		if (sheath != null) sheath.setHidden(true);

		super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

		if (sheath != null) sheath.setHidden(wasHidden);

	}

	@Override
	public RenderType getRenderType(BraveSwordItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityCutoutNoCull(texture);
	}
}
