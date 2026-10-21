package com.dragonminez.client.init.blocks.renderer;

import com.dragonminez.client.init.blocks.model.DragonBallBlockModel;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.init.block.custom.DragonBallBlock;
import com.dragonminez.common.init.block.entity.DragonBallBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class DragonBallBlockRenderer extends GeoBlockRenderer<DragonBallBlockEntity> {
	private static final float BALL_CENTER_Y = 5.625F / 16.0F;
	private static final float HALO_SCALE = 1.12F;

	public DragonBallBlockRenderer(BlockEntityRendererProvider.Context context) {
		super(new DragonBallBlockModel());
		addRenderLayer(new GeoRenderLayer<>(this) {
			@Override
			public void render(PoseStack poseStack, DragonBallBlockEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
				if (!isGathered(animatable)) return;
				long time = animatable.getLevel() == null ? 0L : animatable.getLevel().getGameTime();
				float pulse = 0.35F + 0.25F * Mth.sin((time + partialTick) * 0.15F);
				float[] color = glowColor(animatable.getBallSetId());
				RenderType glow = ModRenderTypes.whiteFlash(getTextureLocation(animatable));
				poseStack.pushPose();
				poseStack.translate(0.0F, BALL_CENTER_Y, 0.0F);
				poseStack.scale(HALO_SCALE, HALO_SCALE, HALO_SCALE);
				poseStack.translate(0.0F, -BALL_CENTER_Y, 0.0F);
				getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glow, bufferSource.getBuffer(glow), partialTick, LightTexture.FULL_BRIGHT, packedOverlay, color[0] * pulse, color[1] * pulse, color[2] * pulse, 1.0F);
				poseStack.popPose();
			}
		});
	}

	@Override
	public void actuallyRender(PoseStack poseStack, DragonBallBlockEntity animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		int light = !isReRender && isGathered(animatable) ? LightTexture.FULL_BRIGHT : packedLight;
		super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, light, packedOverlay, red, green, blue, alpha);
	}

	private static boolean isGathered(DragonBallBlockEntity animatable) {
		BlockState state = animatable.getBlockState();
		return state.hasProperty(DragonBallBlock.GATHERED) && state.getValue(DragonBallBlock.GATHERED);
	}

	private static float[] glowColor(String ballSetId) {
		return switch (ballSetId == null ? "" : ballSetId) {
			case "namek" -> new float[]{1.0F, 0.6F, 0.15F};
			case "demon" -> new float[]{1.0F, 0.35F, 0.1F};
			default -> new float[]{1.0F, 0.8F, 0.3F};
		};
	}
}
