package com.dragonminez.common.init.item.weapons.render;

import com.dragonminez.common.init.item.weapons.TamagamiTridentItem;
import com.dragonminez.common.init.item.weapons.model.TamagamiTridentModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class TamagamiTridentRenderer extends GeoItemRenderer<TamagamiTridentItem> {

	public TamagamiTridentRenderer() {
		super(new TamagamiTridentModel());
	}

	@Override
	public RenderType getRenderType(TamagamiTridentItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityCutoutNoCull(texture);
	}
}
