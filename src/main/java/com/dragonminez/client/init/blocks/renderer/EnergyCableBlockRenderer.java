package com.dragonminez.client.init.blocks.renderer;

import com.dragonminez.client.init.blocks.model.EnergyCableBlockModel;
import com.dragonminez.common.init.block.custom.EnergyCableBlock;
import com.dragonminez.common.init.block.entity.EnergyCableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class EnergyCableBlockRenderer extends GeoBlockRenderer<EnergyCableBlockEntity> {
	public EnergyCableBlockRenderer(BlockEntityRendererProvider.Context context) {
		super(new EnergyCableBlockModel());
	}

	@Override
	public void preRender(PoseStack poseStack, EnergyCableBlockEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

		BlockState state = animatable.getBlockState();

		if (state.getBlock() instanceof EnergyCableBlock) {

			for (Direction dir : Direction.values()) {
				String boneName = dir.getName().toLowerCase();

				this.getGeoModel().getBone(boneName).ifPresent(bone -> {
					boolean isConnected = state.getValue(EnergyCableBlock.PROPERTY_BY_DIRECTION.get(dir));

					bone.setHidden(!isConnected);
				});
			}
		}
	}
}