package com.dragonminez.common.init.item.weapons.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.YajirobeKatanaItem;
import com.dragonminez.common.init.item.ZSwordItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ZSwordModel extends GeoModel<ZSwordItem> {
    @Override
    public ResourceLocation getModelResource(ZSwordItem animatable) {
        return new ResourceLocation(Reference.MOD_ID, "geo/weapons/z_sword.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ZSwordItem animatable) {
        return new ResourceLocation(Reference.MOD_ID, "textures/item/armas/z_sword.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ZSwordItem animatable) {
        return null;
    }
}
