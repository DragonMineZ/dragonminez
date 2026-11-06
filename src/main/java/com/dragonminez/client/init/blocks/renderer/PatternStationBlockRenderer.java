package com.dragonminez.client.init.blocks.renderer;

import com.dragonminez.client.init.blocks.model.PatternStationBlockModel;
import com.dragonminez.common.init.block.entity.PatternStationBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class PatternStationBlockRenderer extends GeoBlockRenderer<PatternStationBlockEntity> {
	public PatternStationBlockRenderer(BlockEntityRendererProvider.Context context) {
		super(new PatternStationBlockModel());
	}

	@Override
	protected void rotateBlock(Direction facing, PoseStack poseStack) {
		super.rotateBlock(facing, poseStack);
		poseStack.mulPose(Axis.YP.rotationDegrees(90));
	}
}
