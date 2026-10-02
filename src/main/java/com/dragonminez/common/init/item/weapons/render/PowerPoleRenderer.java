package com.dragonminez.common.init.item.weapons.render;

import com.dragonminez.client.render.layer.DMZPlayerItemInHandLayer;
import com.dragonminez.common.init.item.weapons.PowerPoleItem;
import com.dragonminez.common.init.item.weapons.model.PowerPoleModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class PowerPoleRenderer extends GeoItemRenderer<PowerPoleItem> {

	private static final String POLE_BONE = "palo";
	private static final float POLE_PIVOT_Y = 19.0F / 16.0F;
	private static final float POLE_PIVOT_Z = 3.0F / 16.0F;
	private static final float POLE_TILT = 15.0F;
	private static final float POLE_ANCHOR = (21.15F - 19.0F) / 16.0F;
	private static final float MAX_EXTENSION = 1.2F;

	public PowerPoleRenderer() {
		super(new PowerPoleModel());
	}

	@Override
	public void actuallyRender(PoseStack poseStack, PowerPoleItem animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

		// Hide the holster while the pole is held, then put it back. BakedGeoModel
		// instances are cached per model file in GeckoLibCache and shared by every
		// renderer that uses them, so leaving the bone hidden here also hides it in
		// DMZRacePartsLayer, which draws the stowed pole on the player's back.
		GeoBone holster = model.getBone("cubretodo").orElse(null);
		boolean wasHidden = holster != null && holster.isHidden();
		if (holster != null) holster.setHidden(true);

		super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

		if (holster != null) holster.setHidden(wasHidden);

	}

	@Override
	public void renderRecursively(PoseStack poseStack, PowerPoleItem animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		float extension = bone.getName().equals(POLE_BONE) ? extensionAt(DMZPlayerItemInHandLayer.renderingAttackProgress()) : 0.0F;
		if (extension <= 0.0F) {
			super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
			return;
		}

		poseStack.pushPose();
		poseStack.translate(0.0F, POLE_PIVOT_Y, POLE_PIVOT_Z);
		poseStack.mulPose(Axis.ZP.rotationDegrees(POLE_TILT));
		poseStack.translate(0.0F, POLE_ANCHOR, 0.0F);
		poseStack.scale(1.0F, 1.0F + extension, 1.0F);
		poseStack.translate(0.0F, -POLE_ANCHOR, 0.0F);
		poseStack.mulPose(Axis.ZP.rotationDegrees(-POLE_TILT));
		poseStack.translate(0.0F, -POLE_PIVOT_Y, -POLE_PIVOT_Z);
		super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
		poseStack.popPose();
	}

	private static float extensionAt(float attackProgress) {
		if (attackProgress < 0.0F) return 0.0F;
		return MAX_EXTENSION * smoothstep(0.08F, 0.33F, attackProgress) * (1.0F - smoothstep(0.75F, 0.97F, attackProgress));
	}

	private static float smoothstep(float edge0, float edge1, float x) {
		float t = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}

	@Override
	public RenderType getRenderType(PowerPoleItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityCutoutNoCull(texture);
	}
}
