package com.dragonminez.client.init.entities.renderer.vehicle;

import com.dragonminez.client.render.util.BoneRenderState;
import com.dragonminez.common.init.entities.vehicle.VehicleEntity;
import com.dragonminez.common.init.entities.vehicle.VehicleProperties;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.ArrayList;
import java.util.List;

public class VehicleRenderer<T extends VehicleEntity> extends GeoEntityRenderer<T> {

	private final List<PosedBone> posedBones = new ArrayList<>();

	public VehicleRenderer(EntityRendererProvider.Context context, GeoModel<T> model, float shadowRadius) {
		super(context, model);
		this.shadowRadius = shadowRadius;
	}

	@Override
	public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		float hover = entity.getHoverOffset(partialTick);
		if (hover == 0.0F) {
			super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
			return;
		}
		poseStack.pushPose();
		poseStack.translate(0.0F, hover, 0.0F);
		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
		poseStack.popPose();
	}

	@Override
	protected void applyRotations(T animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
		super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
		VehicleProperties properties = animatable.getProperties();
		float roll = animatable.getTiltRoll(partialTick);
		float pitch = animatable.getTiltPitch(partialTick);
		if (roll != 0.0F || pitch != 0.0F) {
			float pivot = (float) properties.tilt().pivotHeight();
			poseStack.translate(0.0F, pivot, 0.0F);
			poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
			poseStack.mulPose(Axis.ZP.rotationDegrees(-roll));
			poseStack.translate(0.0F, -pivot, 0.0F);
		}
		float scale = properties.modelScale();
		if (scale != 1.0F) poseStack.scale(scale, scale, scale);
		Vec3 offset = properties.modelOffset();
		if (!offset.equals(Vec3.ZERO)) poseStack.translate(-offset.x / 16.0D, offset.y / 16.0D, offset.z / 16.0D);
	}

	@Override
	public void actuallyRender(PoseStack poseStack, T animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int packedColor) {
		poseBones(animatable, model, partialTick);
		try {
			super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, packedColor);
		} finally {
			for (int i = this.posedBones.size() - 1; i >= 0; i--) this.posedBones.get(i).restore();
			this.posedBones.clear();
		}
	}

	protected void poseBones(T entity, BakedGeoModel model, float partialTick) {
	}

	@Nullable
	protected GeoBone pose(BakedGeoModel model, String name) {
		GeoBone bone = model.getBone(name).orElse(null);
		if (bone != null) this.posedBones.add(new PosedBone(bone, BoneRenderState.capture(bone), bone.isHidden()));
		return bone;
	}

	@Override
	public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(texture);
	}

	private record PosedBone(GeoBone bone, BoneRenderState state, boolean hidden) {
		void restore() {
			state.restore();
			bone.setHidden(hidden);
		}
	}
}
