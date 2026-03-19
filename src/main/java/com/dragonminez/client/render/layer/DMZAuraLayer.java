package com.dragonminez.client.render.layer;

import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class DMZAuraLayer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoRenderLayer<T> {
	public DMZAuraLayer(GeoRenderer<T> entityRendererIn) {
		super(entityRendererIn);
	}

	@Override
	public void render(PoseStack poseStack, T animatable, BakedGeoModel playerModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
		if (animatable.isSpectator()) return;
		var stats = StatsProvider.get(StatsCapability.INSTANCE, animatable).orElse(null);
		if (stats == null) return;

		boolean isAuraActive = stats.getStatus().isAuraActive() || stats.getStatus().isPermanentAura();
		boolean isAndroidChargingForm = stats.getStatus().isAndroidUpgraded() && stats.getStatus().isActionCharging() && stats.getStatus().getSelectedAction().equals(ActionMode.FORM);

		var character = stats.getCharacter();
		boolean hasLightning = false;

		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null) {
			hasLightning = character.getActiveStackFormData().getHasLightnings();
		} else if (character.hasActiveForm() && character.getActiveFormData() != null) {
			hasLightning = character.getActiveFormData().getHasLightnings();
		}

		if (stats.getStatus().isAndroidUpgraded() && !isAndroidChargingForm && !hasLightning) return;

		if (isAuraActive && !isAndroidChargingForm) PlayerEffectQueue.addAura(animatable, playerModel, poseStack, partialTick, packedLight);

		if (isAuraActive || hasLightning) {
			PlayerEffectQueue.addSpark(animatable, playerModel, poseStack, partialTick, packedLight);
		}
	}
}
