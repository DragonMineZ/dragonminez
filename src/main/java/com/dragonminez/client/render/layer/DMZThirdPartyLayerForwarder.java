package com.dragonminez.client.render.layer;

import com.dragonminez.client.render.VanillaModelSync;
import com.dragonminez.mixin.client.LivingEntityRendererAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.List;
import java.util.Set;

public class DMZThirdPartyLayerForwarder<T extends AbstractClientPlayer & GeoAnimatable> extends GeoRenderLayer<T> {

	private static final Set<Class<?>> VANILLA_LAYER_CLASSES = Set.of(
			HumanoidArmorLayer.class,
			ItemInHandLayer.class,
			PlayerItemInHandLayer.class,
			ArrowLayer.class,
			Deadmau5EarsLayer.class,
			CapeLayer.class,
			CustomHeadLayer.class,
			ElytraLayer.class,
			ParrotOnShoulderLayer.class,
			SpinAttackEffectLayer.class,
			BeeStingerLayer.class
	);

	public DMZThirdPartyLayerForwarder(GeoRenderer<T> renderer) {
		super(renderer);
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
		if (!"body".equals(bone.getName())) return;

		var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
		var vanillaRenderer = dispatcher.getSkinMap().get(animatable.getModelName());
		if (!(vanillaRenderer instanceof PlayerRenderer playerRenderer)) return;

		List<RenderLayer<?, ?>> layers;
		try {
			layers = ((LivingEntityRendererAccessor) playerRenderer).dragonminez$getLayers();
		} catch (Exception e) {
			return;
		}

		if (layers == null || layers.isEmpty()) return;

		boolean hasThirdParty = false;
		for (RenderLayer<?, ?> layer : layers) {
			if (!VANILLA_LAYER_CLASSES.contains(layer.getClass())) {
				hasThirdParty = true;
				break;
			}
		}
		if (!hasThirdParty) return;

		BakedGeoModel geoModel = this.getRenderer().getGeoModel().getBakedModel(this.getRenderer().getGeoModel().getModelResource(animatable));
		PlayerModel<AbstractClientPlayer> vanillaModel = playerRenderer.getModel();
		if (geoModel != null) VanillaModelSync.sync(geoModel, vanillaModel, animatable);
		vanillaModel.attackTime = animatable.getAttackAnim(partialTick);
		vanillaModel.riding = animatable.isPassenger();
		vanillaModel.young = false;

		float bodyYaw = Mth.rotLerp(partialTick, animatable.yBodyRotO, animatable.yBodyRot);
		float headYaw = Mth.rotLerp(partialTick, animatable.yHeadRotO, animatable.yHeadRot);
		float netHeadYaw = headYaw - bodyYaw;
		float headPitch = Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot());
		float limbSwing = animatable.walkAnimation.position(partialTick);
		float limbSwingAmount = animatable.walkAnimation.speed(partialTick);
		float ageInTicks = animatable.tickCount + partialTick;

		poseStack.pushPose();

		if (geoModel != null)
			geoModel.getBone("waist").ifPresent(waistBone -> RenderUtils.translateToPivotPoint(poseStack, waistBone));

		for (RenderLayer layer : layers) {
			if (VANILLA_LAYER_CLASSES.contains(layer.getClass())) continue;
			try {
				layer.render(poseStack, bufferSource, packedLight, animatable,
						limbSwing, limbSwingAmount, partialTick, ageInTicks,
						netHeadYaw, headPitch);
			} catch (Exception ignore) {
			}
		}

		poseStack.popPose();
	}
}

