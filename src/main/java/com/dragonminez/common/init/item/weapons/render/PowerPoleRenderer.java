package com.dragonminez.common.init.item.weapons.render;

import com.dragonminez.common.init.item.weapons.PowerPoleItem;
import com.dragonminez.common.init.item.weapons.model.PowerPoleModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class PowerPoleRenderer extends GeoItemRenderer<PowerPoleItem> {

	public PowerPoleRenderer() {
		super(new PowerPoleModel());
	}

	@Override
	public void actuallyRender(PoseStack poseStack, PowerPoleItem animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {

		// Hide the holster while the pole is held, then put it back. BakedGeoModel
		// instances are cached per model file in GeckoLibCache and shared by every
		// renderer that uses them, so leaving the bone hidden here also hides it in
		// DMZRacePartsLayer, which draws the stowed pole on the player's back.
		GeoBone holster = model.getBone("cubretodo").orElse(null);
		boolean wasHidden = holster != null && holster.isHidden();
		if (holster != null) holster.setHidden(true);

		super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);

		if (holster != null) holster.setHidden(wasHidden);

	}

	@Override
	public RenderType getRenderType(PowerPoleItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityCutoutNoCull(texture);
	}
}
