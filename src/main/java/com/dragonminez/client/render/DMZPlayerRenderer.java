package com.dragonminez.client.render;

import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.client.flight.FlightRollHandler;
import com.dragonminez.client.animation.IPlayerAnimatable;
import com.dragonminez.client.render.layer.*;
import com.dragonminez.client.render.shader.TransformationPostShaderManager;
import com.dragonminez.client.render.shader.TransformationMaskBufferSource;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.util.BoneVisibilityHandler;
import com.dragonminez.mixin.client.GeoModelAccessor;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.lists.SaiyanForms;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.animatable.GeoAnimatable;
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
		this.addRenderLayer(new DMZCapeLayer<>(this));
		this.addRenderLayer(new DMZWeightCapeLayer<>(this));
		this.addRenderLayer(new DMZCustomArmorLayer(this));
		this.addRenderLayer(new DMZRacePartsLayer(this));
		this.addRenderLayer(new DMZWeaponsLayer<>(this));
		this.addRenderLayer(new DMZAuraLayer<>(this));
        this.addRenderLayer(new DMZSkinLayer<>(this));
        this.addRenderLayer(new DMZHairLayer<>(this));
		this.addRenderLayer(new DMZThirdPartyLayerForwarder<>(this));
	}

	public void reRender(GeoRenderLayer<T> calledFrom, BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
						 T animatable, RenderType renderType, VertexConsumer buffer, float partialTick,
						 int packedLight, int packedOverlay, int colour) {
		this.caller = calledFrom;
		super.reRender(model, poseStack, bufferSource, animatable, renderType, buffer, partialTick,
				packedLight, packedOverlay, colour);
		this.caller = null;
	}

	@Override
	public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
		int finalColour = colour;
		if (animatable.isSpectator()) {
			finalColour = (38 << 24) | (colour & 0x00FFFFFF); // ~0.15 alpha
		}
		super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, finalColour);
		BoneVisibilityHandler.updateVisibility(model, animatable, this.caller);
	}

	@Override
	public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		if (entity == null) {
			super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
			return;
		}

		((GeoModelAccessor) (Object) getGeoModel()).dmz$setLastRenderedInstance(-1L);

		var statsCap = StatsProvider.get(StatsCapability.INSTANCE, entity);
		var stats = statsCap.orElse(new StatsData(entity));
		var character = stats.getCharacter();
		String race = character.getRaceName().toLowerCase();
		String currentForm = character.getActiveForm();

		String logicKey = character.getRenderLogicKey();

		Float[] resolved = character.getResolvedModelScaling();
		float configScaleX = resolved[0];
		float configScaleY = resolved[1];
		float configScaleZ = resolved[2];

		float scalingX, scalingY, scalingZ;

		boolean isOozaru = logicKey.startsWith("oozaru") || (race.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU)));

		if (isOozaru) {
			scalingX = Math.max(0.1f, configScaleX - 2.8f);
			scalingY = Math.max(0.1f, configScaleY - 2.8f);
			scalingZ = Math.max(0.1f, configScaleZ - 2.8f);
		} else {
			scalingX = configScaleX;
			scalingY = configScaleY;
			scalingZ = configScaleZ;
		}

		poseStack.pushPose();

		boolean hudPortrait = EntityPreviewRenderContext.isHudPortrait();
		boolean shaderPack = !hudPortrait && IrisCompat.isShaderPackInUse();
		boolean captureMask = !shaderPack || TransformationPostShaderManager.isShaderpackMainPass();

		TransformationPostShaderManager.MaskData maskData = !hudPortrait && captureMask ? TransformationPostShaderManager.getEntityMaskData(entity) : null;
		TransformationMaskBufferSource maskBufferSource = null;
		if (maskData != null) {
			maskBufferSource = TransformationPostShaderManager.getMaskBufferSource();
			maskBufferSource.setEntityColors(
					maskData.primaryR(),
					maskData.primaryG(),
					maskData.primaryB(),
					maskData.secondaryR(),
					maskData.secondaryG(),
					maskData.secondaryB()
			);
		}

		if (!hudPortrait && FlySkillEvent.getInstance().isFlyingFast(entity)) {
			float roll = entity == Minecraft.getInstance().player ? FlightRollHandler.getRoll(partialTick) : 0f;
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

		boolean isAuraActive = !hudPortrait && (stats.getStatus().isAuraActive() || stats.getStatus().isPermanentAura());

		if (isAuraActive) {
			if (bufferSource instanceof MultiBufferSource.BufferSource bs) bs.endBatch();
			GL11.glEnable(GL11.GL_STENCIL_TEST);
			RenderSystem.stencilMask(0xFF);
			RenderSystem.stencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
			RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
		}

		if (maskBufferSource != null) {
			maskBufferSource.wrap(bufferSource);
			maskBufferSource.setForceCaptureAll(true);
			try {
				super.render(entity, entityYaw, partialTick, poseStack, maskBufferSource, packedLight);
			} finally {
				maskBufferSource.setForceCaptureAll(false);
				maskBufferSource.setMaskCaptureEnabled(true);
			}
		} else super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

		if (isAuraActive) {
			if (bufferSource instanceof MultiBufferSource.BufferSource bs) bs.endBatch();
			GL11.glDisable(GL11.GL_STENCIL_TEST);
			RenderSystem.stencilMask(0x00);
		}

		this.shadowRadius = 0.4f * ((scalingX + scalingZ) / 2.0f);

		poseStack.popPose();
	}

	@Override
	public void applyRenderLayers(PoseStack poseStack, T animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
		for (GeoRenderLayer<T> renderLayer : getRenderLayers()) {
			if (EntityPreviewRenderContext.isHudPortrait()
					&& !(renderLayer instanceof DMZPlayerArmorLayer
					|| renderLayer instanceof DMZCustomArmorLayer
					|| renderLayer instanceof DMZRacePartsLayer
					|| renderLayer instanceof DMZSkinLayer
					|| renderLayer instanceof DMZHairLayer)) continue;
			renderLayer.render(poseStack, animatable, model, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
		}
	}

	@Override
	public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
		return super.getRenderType(animatable, texture, bufferSource, partialTick);
	}

	@Override
	public boolean shouldShowName(T animatable) {
		if (animatable == Minecraft.getInstance().getCameraEntity()) return false;
		return super.shouldShowName(animatable);
	}
}
