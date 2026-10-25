package com.dragonminez.common.init.item.weapons.render;

import com.dragonminez.common.init.item.weapons.TamagamiHammerItem;
import com.dragonminez.common.init.item.weapons.model.TamagamiHammerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class TamagamiHammerRenderer extends GeoItemRenderer<TamagamiHammerItem> {

	public TamagamiHammerRenderer() {
		super(new TamagamiHammerModel());
	}

	@Override
	public RenderType getRenderType(TamagamiHammerItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityCutoutNoCull(texture);
	}
}
