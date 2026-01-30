package com.dragonminez.common.init.item.weapons.render;

import com.dragonminez.common.init.item.YajirobeKatanaItem;
import com.dragonminez.common.init.item.ZSwordItem;
import com.dragonminez.common.init.item.weapons.model.YajirobeKatanaModel;
import com.dragonminez.common.init.item.weapons.model.ZSwordModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ZSwordRenderer extends GeoItemRenderer<ZSwordItem> {

    public ZSwordRenderer() {
        super(new ZSwordModel());
    }

    @Override
    public RenderType getRenderType(ZSwordItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
