package com.dragonminez.client.init.blocks.renderer;

import com.dragonminez.client.init.blocks.model.PatternStationBlockModel;
import com.dragonminez.common.init.block.entity.PatternStationBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class PatternStationBlockRenderer extends GeoBlockRenderer<PatternStationBlockEntity> {
	public PatternStationBlockRenderer(BlockEntityRendererProvider.Context context) {
		super(new PatternStationBlockModel());
	}
}
