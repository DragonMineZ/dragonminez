package com.dragonminez.client.render;

import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.client.flight.FlightRollHandler;
import com.dragonminez.client.animation.IPlayerAnimatable;
import com.dragonminez.client.render.layer.*;
import com.dragonminez.client.render.shader.TransformationPostShaderManager;
import com.dragonminez.client.render.shader.TransformationMaskBufferSource;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.OutlineBufferSource;
import com.dragonminez.client.render.util.OutlineConfigResolver;
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
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Objects;

public class DMZPlayerRenderer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoEntityRenderer<T> {

	protected GeoRenderLayer<T> caller = null;
	private boolean renderingMaskPass = false;
	private final OutlineBufferSource outlineBuffers = new OutlineBufferSource();

	public DMZPlayerRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
		super(renderManager, model);

		this.addRenderLayer(new DMZPlayerItemInHandLayer(this));
		this.addRenderLayer(new DMZPlayerArmorLayer<>(this));
		this.addRenderLayer(new DMZCapeLayer<>(this));
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
						 int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		this.caller = calledFrom;
		super.reRender(model, poseStack, bufferSource, animatable, renderType, buffer, partialTick,
				packedLight, packedOverlay, red, green, blue, alpha);
		this.caller = null;
	}

	@Override
	public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		float finalAlpha = animatable.isSpectator() ? 0.15f : alpha;
		super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, finalAlpha);
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
		var activeForm = character.getActiveFormData();
		String race = character.getRaceName().toLowerCase();
		String currentForm = character.getActiveForm();

		var raceConfig = ConfigManager.getRaceCharacter(race);
		String raceCustomModel = (raceConfig != null && raceConfig.getCustomModel() != null) ? raceConfig.getCustomModel().toLowerCase() : "";
		String formCustomModel = (character.hasActiveForm() && activeForm != null && activeForm.hasCustomModel())
				? activeForm.getCustomModel().toLowerCase() : "";

		String logicKey = formCustomModel.isEmpty() ? raceCustomModel : formCustomModel;
		if (logicKey.isEmpty()) logicKey = race;

		float configScaleX, configScaleY, configScaleZ;
		if (activeForm != null) {
			configScaleX = activeForm.getModelScaling()[0];
			configScaleY = activeForm.getModelScaling()[1];
			configScaleZ = activeForm.getModelScaling()[2];
		} else {
			configScaleX = character.getModelScaling()[0];
			configScaleY = character.getModelScaling()[1];
			configScaleZ = character.getModelScaling()[2];
		}

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

		// The PostChain outline cannot composite while a shaderpack (Oculus) is
		// active, so under a shaderpack we skip it and use the geometry hull
		// outline instead; without a shaderpack we keep the original PostChain.
		boolean shaderPack = IrisCompat.isShaderPackInUse();

		TransformationPostShaderManager.MaskData maskData = shaderPack ? null : TransformationPostShaderManager.getEntityMaskData(entity);
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
			maskBufferSource.setEntityNoiseAndMix(
					maskData.noiseScale(),
					maskData.noiseIntensity(),
					maskData.noiseScrollX(),
					maskData.noiseScrollY(),
					maskData.colorMixSpeed()
			);
		}

		// Geometry inverted-hull outline, only under a shaderpack and not for the
		// local player in first person (it would fill the view).
		boolean localFirstPerson = entity == Minecraft.getInstance().player
				&& Minecraft.getInstance().options.getCameraType().isFirstPerson();
		OutlineConfigResolver.OutlineData outlineData = (shaderPack && !localFirstPerson)
				? OutlineConfigResolver.resolve(entity) : null;

		if (FlySkillEvent.getInstance().isFlyingFast(entity)) {
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

		// Avoid rotating the full body root during attack; that tilts legs unnaturally.
		// Torso/head pitch is still handled by the animation/model layers.

		poseStack.scale(scalingX, scalingY, scalingZ);

		// Shader-only outline: draw the inflated silhouette BEFORE the body and
		// without depth writes, so the normal-size body is painted over its centre
		// and only the surrounding ring remains. Tinted to the form's primary colour.
		if (outlineData != null) {
			float inflate = 1.0f + Mth.clamp(outlineData.thickness() * 0.02f, 0.01f, 0.08f);
			this.outlineBuffers.configure(getTextureLocation(entity), outlineData.primary());
			poseStack.pushPose();
			poseStack.scale(inflate, inflate, inflate);
			try {
				// Reuse the mask-pass flag so applyRenderLayers keeps only the
				// body-silhouette layers (skin/hair/race parts).
				this.renderingMaskPass = true;
				super.render(entity, entityYaw, partialTick, poseStack, this.outlineBuffers, packedLight);
			} finally {
				this.renderingMaskPass = false;
			}
			poseStack.popPose();
			this.outlineBuffers.flush();
		}

		boolean isAuraActive = stats.getStatus().isAuraActive() || stats.getStatus().isPermanentAura();

		if (isAuraActive) {
			if (bufferSource instanceof MultiBufferSource.BufferSource bs) bs.endBatch();
			GL11.glEnable(GL11.GL_STENCIL_TEST);
			RenderSystem.stencilMask(0xFF);
			RenderSystem.stencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
			RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
		}

		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

		if (maskBufferSource != null) {
			try {
				this.renderingMaskPass = true;
				maskBufferSource.wrap(bufferSource);
				maskBufferSource.setIncludeOriginal(false);
				maskBufferSource.setMaskCaptureEnabled(false);
				super.render(entity, entityYaw, partialTick, poseStack, maskBufferSource, packedLight);
			} finally {
				this.renderingMaskPass = false;
				maskBufferSource.setIncludeOriginal(true);
				maskBufferSource.setMaskCaptureEnabled(true);
			}
		}

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
		TransformationMaskBufferSource maskBufferSource = bufferSource instanceof TransformationMaskBufferSource mask ? mask : null;

		for (GeoRenderLayer<T> renderLayer : getRenderLayers()) {
			boolean captureInMask = this.renderingMaskPass && (renderLayer instanceof DMZSkinLayer<?> || renderLayer instanceof DMZHairLayer<?> || renderLayer instanceof DMZRacePartsLayer<?>);
			if (this.renderingMaskPass && !captureInMask) {
				continue;
			}

			if (maskBufferSource != null) {
				maskBufferSource.setMaskCaptureEnabled(captureInMask);
			}

			try {
				renderLayer.render(poseStack, animatable, model, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
			} finally {
				if (maskBufferSource != null) {
					maskBufferSource.setMaskCaptureEnabled(true);
				}
			}
		}
	}

	@Override
	public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
		return super.getRenderType(animatable, texture, bufferSource, partialTick);
	}
}