package com.dragonminez.client.render;

import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.client.flight.FlightRollHandler;
import com.dragonminez.client.animation.IPlayerAnimatable;
import com.dragonminez.client.render.hair.HairRenderCapture;
import com.dragonminez.client.render.layer.*;
import com.dragonminez.client.systems.BioSwellRenderState;
import com.dragonminez.client.render.shader.TransformationPostShaderManager;
import com.dragonminez.client.render.effects.AuraBorderRenderer;
import com.dragonminez.client.render.effects.DimensionalFistEffect;
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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Objects;

public class DMZPlayerRenderer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoEntityRenderer<T> {

	private static final String HEAD_BONE = "head";

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

		HairRenderCapture.beginEntity(entity, poseStack);
		((GeoModelAccessor) (Object) getGeoModel()).dmz$setLastRenderedInstance(-1L);

		if (HeadPortraitRenderer.isActive()) {
			float previousShadowRadius = this.shadowRadius;
			super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
			this.shadowRadius = previousShadowRadius;
			return;
		}

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

		boolean shaderPack = IrisCompat.isShaderPackInUse();
		boolean captureMask = !shaderPack || TransformationPostShaderManager.isShaderpackMainPass();

		TransformationPostShaderManager.MaskData maskData = captureMask ? TransformationPostShaderManager.getEntityMaskData(entity) : null;
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

		if (FlySkillEvent.getInstance().isFlyingFast(entity)) {
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

		float swellScale = BioSwellRenderState.bodyScale(entity);
		poseStack.scale(scalingX * swellScale, scalingY * swellScale, scalingZ * swellScale);

		boolean isAuraActive = stats.getStatus().isAuraActive() || stats.getStatus().isPermanentAura() || stats.getStatus().isForcedAura();

		if (isAuraActive) {
			if (bufferSource instanceof MultiBufferSource.BufferSource bs) bs.endBatch();
			GL11.glEnable(GL11.GL_STENCIL_TEST);
			RenderSystem.stencilMask(0xFF);
			RenderSystem.stencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
			RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
		}

		MultiBufferSource renderSource = bufferSource;
		if (maskBufferSource != null) {
			maskBufferSource.wrap(bufferSource);
			maskBufferSource.setForceCaptureAll(true);
			renderSource = maskBufferSource;
		}
		MultiBufferSource borderSource = AuraBorderRenderer.begin(entity, renderSource, AuraBorderRenderer.playerColor(entity, stats), partialTick);
		try {
			super.render(entity, entityYaw, partialTick, poseStack, borderSource, packedLight);
		} finally {
			AuraBorderRenderer.end(borderSource);
			if (maskBufferSource != null) {
				maskBufferSource.setForceCaptureAll(false);
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
		boolean portrait = HeadPortraitRenderer.isActive();
		boolean armOnly = DimensionalFistEffect.isRenderingArm();
		for (GeoRenderLayer<T> renderLayer : getRenderLayers()) {
			if (armOnly && !(renderLayer instanceof DMZSkinLayer<?>)) continue;
			if (portrait && !isPortraitLayer(renderLayer)) continue;
			renderLayer.render(poseStack, animatable, model, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
		}
	}

	@Override
	public void preApplyRenderLayers(PoseStack poseStack, T animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
		if (DimensionalFistEffect.isRenderingArm()) {
			for (GeoRenderLayer<T> renderLayer : getRenderLayers()) {
				if (renderLayer instanceof DMZSkinLayer<?>) renderLayer.preRender(poseStack, animatable, model, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
			}
			return;
		}
		if (!HeadPortraitRenderer.isActive()) {
			super.preApplyRenderLayers(poseStack, animatable, model, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
			return;
		}
		for (GeoRenderLayer<T> renderLayer : getRenderLayers()) {
			if (isPortraitLayer(renderLayer)) renderLayer.preRender(poseStack, animatable, model, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
		}
	}

	@Override
	public void applyRenderLayersForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
		if (DimensionalFistEffect.isRenderingArm()) return;
		if (!HeadPortraitRenderer.isActive()) {
			super.applyRenderLayersForBone(poseStack, animatable, bone, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
			return;
		}
		for (GeoRenderLayer<T> renderLayer : getRenderLayers()) {
			if (isPortraitLayer(renderLayer)) renderLayer.renderForBone(poseStack, animatable, bone, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
		}
	}

	private static boolean isPortraitLayer(GeoRenderLayer<?> renderLayer) {
		return renderLayer instanceof DMZSkinLayer<?> || renderLayer instanceof DMZHairLayer<?> || renderLayer instanceof DMZRacePartsLayer;
	}

	@Override
	public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		if (!HeadPortraitRenderer.isActive()) {
			float[] fistPose = DimensionalFistEffect.beginBonePose(bone);
			try {
				super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
			} finally {
				DimensionalFistEffect.endBonePose(bone, fistPose);
			}
			return;
		}

		if (isHeadOrHeadChild(bone)) {
			if (!HEAD_BONE.equals(bone.getName())) {
				super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
				return;
			}
			float rotX = bone.getRotX(), rotY = bone.getRotY(), rotZ = bone.getRotZ();
			float posX = bone.getPosX(), posY = bone.getPosY(), posZ = bone.getPosZ();
			bone.setRotX(0.0f); bone.setRotY(0.0f); bone.setRotZ(0.0f);
			bone.setPosX(0.0f); bone.setPosY(0.0f); bone.setPosZ(0.0f);
			try {
				super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
			} finally {
				bone.setRotX(rotX); bone.setRotY(rotY); bone.setRotZ(rotZ);
				bone.setPosX(posX); bone.setPosY(posY); bone.setPosZ(posZ);
			}
			return;
		}

		if (!containsHead(bone)) return;
		for (GeoBone child : bone.getChildBones()) {
			renderRecursively(poseStack, animatable, child, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
		}
	}

	private static boolean isHeadOrHeadChild(GeoBone bone) {
		for (GeoBone current = bone; current != null; current = current.getParent()) {
			if (HEAD_BONE.equals(current.getName())) return true;
		}
		return false;
	}

	private static boolean containsHead(GeoBone bone) {
		for (GeoBone child : bone.getChildBones()) {
			if (HEAD_BONE.equals(child.getName()) || containsHead(child)) return true;
		}
		return false;
	}

	@Override
	protected void applyRotations(T animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
		if (HeadPortraitRenderer.isActive()) return;
		super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick);
	}

	@Override
	public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
		return super.getRenderType(animatable, texture, bufferSource, partialTick);
	}

	@Override
	public int getPackedOverlay(T animatable, float u) {
		if (BioSwellRenderState.isFlashingWhite(animatable)) {
			return OverlayTexture.pack(OverlayTexture.u(1.0F), 10);
		}
		return super.getPackedOverlay(animatable, u);
	}

	@Override
	public boolean shouldShowName(T animatable) {
		if (HeadPortraitRenderer.isActive()) return false;
		if (animatable == Minecraft.getInstance().getCameraEntity()) return false;
		return super.shouldShowName(animatable);
	}
}
