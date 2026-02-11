package com.dragonminez.common.init.item.weapons.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.YajirobeKatanaItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class YajirobeKatanaModel extends GeoModel<YajirobeKatanaItem> {
    @Override
    public ResourceLocation getModelResource(YajirobeKatanaItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/yajirobe_katana.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(YajirobeKatanaItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/armas/yajirobe_katana.png");
    }

    @Override
    public ResourceLocation getAnimationResource(YajirobeKatanaItem animatable) {
        return null;
    }
}
