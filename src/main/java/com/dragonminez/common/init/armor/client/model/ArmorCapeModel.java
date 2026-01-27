package com.dragonminez.common.init.armor.client.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.armor.DbzArmorCapeItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ArmorCapeModel extends GeoModel<DbzArmorCapeItem> {
    @Override
    public ResourceLocation getModelResource(DbzArmorCapeItem animatable) {
        return new ResourceLocation(Reference.MOD_ID, "geo/armor/armorcape.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DbzArmorCapeItem animatable) {
        return new ResourceLocation(Reference.MOD_ID, "textures/armor/blank.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DbzArmorCapeItem animatable) {
        return new ResourceLocation(Reference.MOD_ID, "animations/armorcape.animation.json");
    }
}
