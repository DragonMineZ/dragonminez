package com.dragonminez.client.render;

import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.client.flight.FlightRollHandler;
import com.dragonminez.client.render.firstperson.dto.FirstPersonListener;
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
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.Objects;

public class PlayerDMZRenderer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoEntityRenderer<T> {

    public PlayerDMZRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
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

    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        BoneVisibilityHandler.updateVisibility(model, animatable);
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

        float scaling;

        if (race.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU)) || (Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU))) {
            scaling = 1.0f;
        } else {
            if (activeForm != null) {
                scaling = activeForm.getModelScaling();
            } else {
                scaling = (float) character.getModelScaling();
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

        poseStack.scale(scaling, scaling, scaling);

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        this.shadowRadius = 0.4f * scaling;

        poseStack.popPose();
    }
}
