package com.dragonminez.common.init.item.weapons.render;

import com.dragonminez.common.init.item.weapons.TamagamiSwordItem;
import com.dragonminez.common.init.item.weapons.model.TamagamiSwordModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class TamagamiSwordRenderer extends GeoItemRenderer<TamagamiSwordItem> {

	public TamagamiSwordRenderer() {
		super(new TamagamiSwordModel());
	}

	@Override
	public RenderType getRenderType(TamagamiSwordItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityCutoutNoCull(texture);
	}
}
