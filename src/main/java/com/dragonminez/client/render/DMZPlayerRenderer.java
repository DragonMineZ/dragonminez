package com.dragonminez.client.render;

import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.client.flight.FlightRollHandler;
import com.dragonminez.client.render.layer.*;
import com.dragonminez.client.util.BoneVisibilityHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.lists.SaiyanForms;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Objects;

public class DMZPlayerRenderer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoEntityRenderer<T> {

    protected GeoRenderLayer<T> caller = null;

    public DMZPlayerRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
        super(renderManager, model);

            this.addRenderLayer(new DMZPlayerItemInHandLayer(this));
            this.addRenderLayer(new DMZPlayerArmorLayer<>(this));
            this.addRenderLayer(new DMZCustomArmorLayer(this));
            this.addRenderLayer(new DMZSkinLayer<>(this));
            this.addRenderLayer(new DMZHairLayer<>(this));
            this.addRenderLayer(new DMZRacePartsLayer(this));
            this.addRenderLayer(new DMZWeaponsLayer<>(this));
            this.addRenderLayer(new DMZAuraLayer<>(this));
    }

  public void reRender(GeoRenderLayer<T> calledFrom, BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
      T animatable, RenderType renderType, VertexConsumer buffer, float partialTick,
      int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
      this.caller = calledFrom;
    super.reRender(model, poseStack, bufferSource, animatable, renderType, buffer, partialTick,
        packedLight, packedOverlay, red, green, blue, alpha);
    this.caller = null;
  }

  @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        float finalAlpha = animatable.isSpectator() ? 0.15f : alpha;
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        BoneVisibilityHandler.updateVisibility(model, animatable, this.caller);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (entity == null) {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            return;
        }

        var statsCap = StatsProvider.get(StatsCapability.INSTANCE, entity);
        var stats = statsCap.orElse(new StatsData(entity));
        var character = stats.getCharacter();
        var activeForm = character.getActiveFormData();
        String race = character.getRaceName().toLowerCase();
        String currentForm = character.getActiveForm();

        float scalingX, scalingY, scalingZ;

        if (race.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU)) || (Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU))) {
            scalingX = 1.0f; scalingY = 1.0f; scalingZ = 1.0f;
        } else {
            if (activeForm != null) {
                scalingX = activeForm.getModelScaling()[0];
				scalingY = activeForm.getModelScaling()[1];
				scalingZ = activeForm.getModelScaling()[2];
            } else {
                scalingX = character.getModelScaling()[0];
				scalingY = character.getModelScaling()[1];
				scalingZ = character.getModelScaling()[2];
            }
        }

        poseStack.pushPose();

		if (FlySkillEvent.isFlyingFast()) {
			float roll = FlightRollHandler.getRoll(partialTick);
			float pitch = entity.getViewXRot(partialTick);
			float pivotY = entity.getBbHeight() / 2f;
			poseStack.translate(0, pivotY, 0);
			poseStack.mulPose(Axis.YP.rotationDegrees(180 - entityYaw));
			poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
			poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
			poseStack.mulPose(Axis.YP.rotationDegrees(-(180 - entityYaw)));
			poseStack.translate(0, -pivotY, 0);
		}

        poseStack.scale(scalingX, scalingY, scalingZ);

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        this.shadowRadius = 0.4f * ((scalingX + scalingZ) / 2.0f);

        poseStack.popPose();
    }

    @Override
    public void applyRenderLayers(PoseStack poseStack, T animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        for (GeoRenderLayer<T> renderLayer : getRenderLayers()) {
                renderLayer.render(poseStack, animatable, model, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
        }
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return super.getRenderType(animatable, texture, bufferSource, partialTick);
    }
}
