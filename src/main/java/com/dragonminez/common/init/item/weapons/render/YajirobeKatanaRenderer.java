package com.dragonminez.common.init.item.weapons.render;

import com.dragonminez.common.init.item.YajirobeKatanaItem;
import com.dragonminez.common.init.item.weapons.model.YajirobeKatanaModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class YajirobeKatanaRenderer extends GeoItemRenderer<YajirobeKatanaItem> {

    public YajirobeKatanaRenderer() {
        super(new YajirobeKatanaModel());
    }

    @Override
    public RenderType getRenderType(YajirobeKatanaItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
