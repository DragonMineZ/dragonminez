package com.dragonminez.client.render;

import com.dragonminez.Reference;
import com.dragonminez.client.flight.FlightRollHandler;
import com.dragonminez.client.render.layer.*;
import com.dragonminez.client.util.BoneVisibilityHandler;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.init.armor.DbzArmorItem;
import com.dragonminez.common.stats.Skill;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.lists.BioAndroidForms;
import com.dragonminez.common.util.lists.FrostDemonForms;
import com.dragonminez.common.util.lists.SaiyanForms;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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

        if (race.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU)) ||
                (Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU))) {
            scaling = 1.0f;
        } else {
            if (activeForm != null) {
                scaling = activeForm.getModelScaling();
            } else {
                scaling = (float) character.getModelScaling();
            }
        }

        poseStack.pushPose();

		if (FlightRollHandler.hasActiveRoll()) {
			float roll = FlightRollHandler.getRoll(partialTick);
			float pivotY = entity.getBbHeight() / 2f;
			poseStack.translate(0, pivotY, 0);
			float yawRad = entityYaw * Mth.DEG_TO_RAD;
			Vector3f rotationAxis = new Vector3f((float) -Math.sin(yawRad), 0, (float) Math.cos(yawRad));
			poseStack.mulPose(Axis.of(rotationAxis).rotationDegrees(-roll));
			poseStack.translate(0, -pivotY, 0);
		}

        poseStack.scale(scaling, scaling, scaling);

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        this.shadowRadius = 0.4f * scaling;

        poseStack.popPose();
    }
}
