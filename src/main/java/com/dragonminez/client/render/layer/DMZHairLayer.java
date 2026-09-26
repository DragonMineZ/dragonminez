package com.dragonminez.client.render.layer;

import com.dragonminez.Reference;
import com.dragonminez.client.render.compat.CosmeticArmorCompat;
import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.dragonminez.client.render.HeadPortraitRenderer;
import com.dragonminez.client.render.hair.HairColliders;
import com.dragonminez.client.render.hair.HairEntityState;
import com.dragonminez.client.render.hair.HairMeshBuilder;
import com.dragonminez.client.render.hair.HairRenderCapture;
import com.dragonminez.client.render.hair.HairRenderContext;
import com.dragonminez.client.render.hair.HairSimulation;
import com.dragonminez.client.render.hair.HairStyleResolver;
import com.dragonminez.client.render.shader.TransformationMaskBufferSource;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralUserConfig;
import com.dragonminez.common.hair.HairManager;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.List;

public class DMZHairLayer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoRenderLayer<T> {
	private static final ResourceLocation HAIR_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/hair.png");
	private static final double FULL_PHYSICS_DISTANCE_SQR = 24.0 * 24.0;
	private static final double REDUCED_PHYSICS_DISTANCE_SQR = 48.0 * 48.0;
	private static final double PIXEL_DETAIL_DISTANCE_SQR = 32.0 * 32.0;
	private static final float SPECTATOR_ALPHA = 0.15f;

	private final HairStyleResolver resolver = new HairStyleResolver();
	private final HairStyleResolver.Resolved resolved = new HairStyleResolver.Resolved();
	private final HairMeshBuilder meshBuilder = new HairMeshBuilder();
	private final HairEntityState staticState = new HairEntityState();

	private final Matrix4f headPose = new Matrix4f();
	private final Matrix3f headNormal = new Matrix3f();
	private final Matrix4f headBonePose = new Matrix4f();
	private final Matrix4f bodyBonePose = new Matrix4f();
	private final Matrix4f inverseBase = new Matrix4f();
	private final Matrix4f scratch = new Matrix4f();
	private float[] headBounds;
	private float[] bodyBounds;
	private int headSequence = -1;
	private int bodySequence = -1;
	private boolean prepared;
	private StatsData preparedStats;

	public DMZHairLayer(GeoRenderer<T> renderer) {
		super(renderer);
	}

	public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
		String name = bone.getName();
		if ("head".equals(name)) {
			headBonePose.set(poseStack.last().pose());
			headBounds = HairColliders.boundsOf(bone);
			poseStack.pushPose();
			RenderUtils.translateToPivotPoint(poseStack, bone);
			headPose.set(poseStack.last().pose());
			headNormal.set(poseStack.last().normal());
			poseStack.popPose();
			headSequence = HairRenderCapture.sequence();
			prepared = prepare(animatable);
		} else if ("body".equals(name)) {
			bodyBonePose.set(poseStack.last().pose());
			bodyBounds = HairColliders.boundsOf(bone);
			bodySequence = HairRenderCapture.sequence();
		}
	}

	public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
		if (!HairRenderCapture.isCurrent(animatable, headSequence)) return;
		headSequence = -1;
		if (!prepared) return;
		prepared = false;

		TransformationMaskBufferSource maskBuffer = bufferSource instanceof TransformationMaskBufferSource mask ? mask : null;
		if (maskBuffer != null) maskBuffer.setMaskCaptureEnabled(true);
		try {
			renderHair(animatable, bufferSource, partialTick, packedLight, packedOverlay);
		} finally {
			bufferSource.getBuffer(renderType);
			if (maskBuffer != null) maskBuffer.setMaskCaptureEnabled(false);
		}
	}

	private boolean prepare(T animatable) {
		if (!shouldRenderHair(animatable)) return false;
		StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, animatable).orElse(null);
		if (stats == null) return false;
		Character character = stats.getCharacter();
		if (!HairManager.canUseHair(character)) return false;
		preparedStats = stats;
		if (editorPreview(animatable) == null) resolver.resolve(animatable, stats, resolved);
		return true;
	}

	private HairRenderContext.Preview editorPreview(T animatable) {
		boolean editing = HairRenderContext.mode() == HairRenderContext.Mode.EDITOR_PREVIEW && animatable == Minecraft.getInstance().player;
		return editing ? HairRenderContext.preview() : null;
	}

	private void renderHair(T animatable, MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
		Character character = preparedStats.getCharacter();
		HairRenderContext.Preview preview = editorPreview(animatable);

		HairEntityState state;
		boolean simulate = false;
		boolean useSimulation = false;
		int substeps = HairSimulation.FULL_SUBSTEPS;
		int globalFrom;
		int globalTo;
		boolean forceFrom = false;
		boolean forceTo = false;
		float charge = 0.0f;
		float aura = 0.0f;

		if (preview != null) {
			state = preview.simulationState();
			state.updatePoses(preview.style(), preview.style(), 0.0f, preview.slot(), preview.slot());
			globalFrom = toRgb(character.getRgbHairColor());
			globalTo = globalFrom;
			simulate = preview.physicsEnabled();
			useSimulation = simulate;
		} else {
			if (resolved.from.isEmpty() && resolved.to.isEmpty()) return;
			globalFrom = toRgb(resolved.rgbFrom);
			globalTo = toRgb(resolved.rgbTo);
			forceFrom = resolved.forceColorFrom;
			forceTo = resolved.forceColorTo;
			charge = resolved.kiChargeProgress;
			aura = resolved.auraIntensity;
			state = selectState(animatable);
			if (state != staticState) {
				substeps = physicsSubsteps(animatable);
				useSimulation = substeps > 0;
				simulate = useSimulation && isPerspectivePass();
			}
		}

		if (simulate) simulate(animatable, state, preview != null, partialTick, charge, aura, substeps);

		float alpha = animatable.isSpectator() ? SPECTATOR_ALPHA : 1.0f;
		RenderType hairType = alpha < 1.0f ? RenderType.entityTranslucent(HAIR_TEXTURE) : RenderType.entityCutoutNoCull(HAIR_TEXTURE);
		VertexConsumer hairBuffer = bufferSource.getBuffer(hairType);
		meshBuilder.emit(hairBuffer, headPose, headNormal, state, useSimulation, globalFrom, globalTo, forceFrom, forceTo,
				packedLight, packedOverlay, alpha, wantsPixelDetail(animatable),
				preview != null ? preview::highlight : null,
				preview != null ? preview::isHidden : null,
				preview != null ? preview.pickRecorder() : null);
	}

	private HairEntityState selectState(T animatable) {
		if (HairRenderContext.mode() == HairRenderContext.Mode.MENU_PREVIEW) return updateStatic();
		HairEntityState worldState = HairSimulation.stateFor(animatable.getId(), Util.getMillis());
		int substeps = physicsSubsteps(animatable);
		if (isPerspectivePass()) {
			if (substeps == 0) worldState.invalidateSimulation();
			worldState.updatePoses(resolved.from, resolved.to, resolved.factor, resolved.fromSlot, resolved.toSlot);
			return worldState;
		}
		if (substeps > 0 && worldState.matchesInputs(resolved.from, resolved.to, resolved.factor, resolved.fromSlot, resolved.toSlot)) return worldState;
		return updateStatic();
	}

	private HairEntityState updateStatic() {
		staticState.updatePoses(resolved.from, resolved.to, resolved.factor, resolved.fromSlot, resolved.toSlot);
		return staticState;
	}

	private void simulate(T animatable, HairEntityState state, boolean previewSpace, float partialTick, float charge, float aura, int substeps) {
		HairRenderCapture.basePose().invert(inverseBase);
		state.colliders().setHead(scratch.set(inverseBase).mul(headBonePose), headBounds);
		if (HairRenderCapture.isCurrent(animatable, bodySequence)) {
			state.colliders().setBody(scratch.set(inverseBase).mul(bodyBonePose), bodyBounds);
		} else {
			state.colliders().setBody(scratch, null);
		}
		scratch.set(inverseBase).mul(headPose);
		if (previewSpace) {
			state.simulate(scratch, 0.0, 0.0, 0.0, Util.getNanos(), charge, aura, substeps);
		} else {
			Vec3 anchor = animatable.getPosition(partialTick);
			state.simulate(scratch, anchor.x, anchor.y, anchor.z, Util.getNanos(), charge, aura, substeps);
		}
	}

	private boolean shouldRenderHair(T animatable) {
		if (animatable.isInvisible() && !animatable.isSpectator()) return false;
		if (animatable.hasEffect(MainEffects.CANDY.get())) return false;
		if (HeadPortraitRenderer.isActive()) return true;
		if (FirstPersonManager.shouldRenderFirstPerson(animatable)) return false;

		ItemStack headItem = resolveHeadArmorStack(animatable);
		if (!headItem.isEmpty()) {
			ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(headItem.getItem());
			if (itemId != null) {
				List<String> allowedHelmets = ConfigManager.getServerConfig().getGameplay().getHelmetsThatKeepHair();
				if (!allowedHelmets.contains(itemId.toString())) return false;
			}
		}
		return true;
	}

	private int physicsSubsteps(T animatable) {
		String quality = ConfigManager.getUserConfig().getHairPhysicsQuality();
		if (GeneralUserConfig.HAIR_PHYSICS_OFF.equals(quality)) return 0;
		Entity camera = Minecraft.getInstance().getCameraEntity();
		double distanceSqr = camera != null ? animatable.distanceToSqr(camera) : 0.0;
		if (distanceSqr > REDUCED_PHYSICS_DISTANCE_SQR) return 0;
		if (GeneralUserConfig.HAIR_PHYSICS_LOW.equals(quality) || distanceSqr > FULL_PHYSICS_DISTANCE_SQR) return HairSimulation.REDUCED_SUBSTEPS;
		return HairSimulation.FULL_SUBSTEPS;
	}

	private static boolean wantsPixelDetail(Entity animatable) {
		if (HairRenderContext.mode() != HairRenderContext.Mode.WORLD) return true;
		if (IrisCompat.isRenderingShadowPass()) return false;
		Entity camera = Minecraft.getInstance().getCameraEntity();
		return camera == null || !isPerspectivePass() || animatable.distanceToSqr(camera) <= PIXEL_DETAIL_DISTANCE_SQR;
	}

	private static boolean isPerspectivePass() {
		Matrix4f projection = RenderSystem.getProjectionMatrix();
		return projection.m23() != 0.0f && projection.m33() == 0.0f;
	}

	private static int toRgb(float[] rgb) {
		int r = Math.round(Math.max(0.0f, Math.min(1.0f, rgb[0])) * 255.0f);
		int g = Math.round(Math.max(0.0f, Math.min(1.0f, rgb[1])) * 255.0f);
		int b = Math.round(Math.max(0.0f, Math.min(1.0f, rgb[2])) * 255.0f);
		return (r << 16) | (g << 8) | b;
	}

	private ItemStack resolveHeadArmorStack(T animatable) {
		ItemStack stack = animatable.getItemBySlot(EquipmentSlot.HEAD);
		if (CosmeticArmorCompat.isLoaded()) {
			ItemStack cosmeticStack = CosmeticArmorCompat.getCosmeticStack(animatable, EquipmentSlot.HEAD);
			if (cosmeticStack != null) return cosmeticStack;
		}
		return stack;
	}
}
