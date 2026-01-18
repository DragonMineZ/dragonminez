package com.dragonminez.client.init.blocks.renderer;

import com.dragonminez.client.init.blocks.model.KikonoStationBlockModel;
import com.dragonminez.common.init.block.entity.KikonoStationBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class KikonoStationBlockRenderer extends GeoBlockRenderer<KikonoStationBlockEntity> {
	public KikonoStationBlockRenderer(BlockEntityRendererProvider.Context context) {
		super(new KikonoStationBlockModel());
	}
}