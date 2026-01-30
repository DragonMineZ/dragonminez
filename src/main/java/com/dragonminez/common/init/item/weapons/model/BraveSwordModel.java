package com.dragonminez.common.init.item.weapons.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.ZSwordItem;
import com.dragonminez.common.init.item.weapons.BraveSwordItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BraveSwordModel extends GeoModel<BraveSwordItem> {
    @Override
    public ResourceLocation getModelResource(BraveSwordItem animatable) {
        return new ResourceLocation(Reference.MOD_ID, "geo/weapons/brave_sword.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BraveSwordItem animatable) {
        return new ResourceLocation(Reference.MOD_ID, "textures/item/armas/brave_sword.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BraveSwordItem animatable) {
        return null;
    }
}
