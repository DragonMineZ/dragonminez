package com.dragonminez.client.render.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.state.BoneSnapshot;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class DonorBoneRenderer {
	private static final float TAIL_PITCH_OFFSET = 10.0f;
	private static final float TAIL_PITCH_SPEED = 180.0f;
	private static final float TAIL_YAW_SPEED = 100.0f;
	private static final float[] TAIL_PITCH_PHASE = {30.0f, -30.0f, -60.0f, -90.0f, -120.0f, -120.0f};
	private static final float[] TAIL_PITCH_AMPLITUDE = {1.0f, -8.0f, -10.0f, -12.0f, -14.0f, -5.0f};
	private static final float[] TAIL_YAW_PHASE = {0.0f, -60.0f, -90.0f, -120.0f, -150.0f, -150.0f};
	private static final float[] TAIL_YAW_AMPLITUDE = {22.0f, 25.0f, 28.0f, 31.0f, 34.0f, 22.0f};

	public record Layer(RenderType type, float red, float green, float blue, float alpha) {}

	public interface BoneVisitor {
		void visit(GeoBone bone, PoseStack poseStack);
	}

	private DonorBoneRenderer() {}

	public static <T extends AbstractClientPlayer & GeoAnimatable> void render(GeoRenderer<T> renderer, T animatable, BakedGeoModel playerModel,
			ResourceLocation donorModel, String boneName, boolean tail, List<Layer> layers, PoseStack poseStack,
			MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
		if (layers.isEmpty()) return;
		withDonor(animatable, playerModel, donorModel, boneName, tail, partialTick, root -> {
			for (Layer layer : layers) {
				renderer.renderRecursively(poseStack, animatable, root, layer.type(), bufferSource, bufferSource.getBuffer(layer.type()), true,
						partialTick, packedLight, packedOverlay, layer.red(), layer.green(), layer.blue(), layer.alpha());
			}
		});
	}

	public static void visit(Entity animatable, BakedGeoModel playerModel, ResourceLocation donorModel, String boneName, boolean tail,
			PoseStack poseStack, float partialTick, BoneVisitor visitor) {
		withDonor(animatable, playerModel, donorModel, boneName, tail, partialTick, root -> walk(root, poseStack, visitor));
	}

	private static void walk(GeoBone bone, PoseStack poseStack, BoneVisitor visitor) {
		poseStack.pushPose();
		RenderUtils.prepMatrixForBone(poseStack, bone);
		if (!bone.isHidden()) visitor.visit(bone, poseStack);
		if (!bone.isHidingChildren()) {
			for (GeoBone child : bone.getChildBones()) walk(child, poseStack, visitor);
		}
		poseStack.popPose();
	}

	private static void withDonor(Entity animatable, BakedGeoModel playerModel, ResourceLocation donorModel, String boneName, boolean tail,
			float partialTick, Consumer<GeoBone> action) {
		BakedGeoModel donor = GeckoLibCache.getBakedModels().get(donorModel);
		if (donor == null) return;
		GeoBone root = donor.getBone(boneName).orElse(null);
		if (root == null) return;

		List<GeoBone> bones = new ArrayList<>();
		collect(root, bones);
		BoneRenderState[] saved = new BoneRenderState[bones.size()];
		boolean[] hidden = new boolean[bones.size()];
		boolean[] hidingChildren = new boolean[bones.size()];
		for (int i = 0; i < bones.size(); i++) {
			GeoBone bone = bones.get(i);
			saved[i] = BoneRenderState.capture(bone);
			hidden[i] = bone.isHidden();
			hidingChildren[i] = bone.isHidingChildren();
		}

		float seconds = (animatable.tickCount + partialTick) / 20.0f;
		try {
			for (GeoBone bone : bones) {
				bone.setHidden(false);
				if (donor != playerModel) pose(bone, playerModel.getBone(bone.getName()).orElse(null), tail, seconds);
			}
			action.accept(root);
		} finally {
			for (int i = 0; i < bones.size(); i++) {
				saved[i].restore();
				bones.get(i).setHidden(hidden[i]);
				bones.get(i).setChildrenHidden(hidingChildren[i]);
			}
		}
	}

	private static void collect(GeoBone bone, List<GeoBone> out) {
		out.add(bone);
		for (GeoBone child : bone.getChildBones()) collect(child, out);
	}

	private static void pose(GeoBone bone, GeoBone source, boolean tail, float seconds) {
		if (source != null) {
			bone.setRotX(source.getRotX());
			bone.setRotY(source.getRotY());
			bone.setRotZ(source.getRotZ());
			bone.setPosX(source.getPosX());
			bone.setPosY(source.getPosY());
			bone.setPosZ(source.getPosZ());
			bone.setScaleX(source.getScaleX());
			bone.setScaleY(source.getScaleY());
			bone.setScaleZ(source.getScaleZ());
			return;
		}

		BoneSnapshot rest = bone.getInitialSnapshot();
		if (rest != null) {
			bone.setRotX(rest.getRotX());
			bone.setRotY(rest.getRotY());
			bone.setRotZ(rest.getRotZ());
			bone.setPosX(rest.getOffsetX());
			bone.setPosY(rest.getOffsetY());
			bone.setPosZ(rest.getOffsetZ());
			bone.setScaleX(rest.getScaleX());
			bone.setScaleY(rest.getScaleY());
			bone.setScaleZ(rest.getScaleZ());
		}

		int segment = tail ? tailSegment(bone.getName()) : 0;
		if (segment <= 0) return;
		int i = Math.min(segment, TAIL_PITCH_PHASE.length) - 1;
		float pitch = TAIL_PITCH_OFFSET + Mth.sin((seconds * TAIL_PITCH_SPEED + TAIL_PITCH_PHASE[i]) * Mth.DEG_TO_RAD) * TAIL_PITCH_AMPLITUDE[i];
		float yaw = Mth.cos((seconds * TAIL_YAW_SPEED + TAIL_YAW_PHASE[i]) * Mth.DEG_TO_RAD) * TAIL_YAW_AMPLITUDE[i];
		bone.setRotX(bone.getRotX() - pitch * Mth.DEG_TO_RAD);
		bone.setRotY(bone.getRotY() - yaw * Mth.DEG_TO_RAD);
	}

	private static int tailSegment(String name) {
		if (name == null || !name.startsWith("tail") || name.length() == 4) return 0;
		int value = 0;
		for (int i = 4; i < name.length(); i++) {
			char digit = name.charAt(i);
			if (digit < '0' || digit > '9') return 0;
			value = value * 10 + (digit - '0');
		}
		return value;
	}
}
