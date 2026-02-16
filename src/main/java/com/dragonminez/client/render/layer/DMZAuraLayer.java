package com.dragonminez.client.render.layer;

import com.dragonminez.client.util.AuraRenderQueue;
import com.dragonminez.common.stats.ActionMode;
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
        var stats = StatsProvider.get(StatsCapability.INSTANCE, animatable).orElse(null);

        if (stats == null || !stats.getStatus().isAuraActive()) return;
		if (stats.getStatus().isAndroidUpgraded() && (!stats.getStatus().isActionCharging() || !stats.getStatus().getSelectedAction().equals(ActionMode.FORM))) return;
		AuraRenderQueue.addAura(animatable, playerModel, poseStack, partialTick, packedLight);
        AuraRenderQueue.addSpark(animatable, playerModel, poseStack, partialTick, packedLight);
    }
}
