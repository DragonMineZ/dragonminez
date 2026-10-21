package com.dragonminez.common.init.item.weapons.render;

import com.dragonminez.common.init.item.weapons.TamagamiTridentItem;
import com.dragonminez.common.init.item.weapons.model.TamagamiTridentModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class TamagamiTridentRenderer extends GeoItemRenderer<TamagamiTridentItem> {

	public TamagamiTridentRenderer() {
		super(new TamagamiTridentModel());
	}

	@Override
	public void actuallyRender(PoseStack poseStack, TamagamiTridentItem animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

		super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

	}

	@Override
	public RenderType getRenderType(TamagamiTridentItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityCutoutNoCull(texture);
	}
}
