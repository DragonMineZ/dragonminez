package com.dragonminez.common.init.item.weapons.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.PowerPoleItem;
import com.dragonminez.common.init.item.ZSwordItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class PowerPoleModel extends GeoModel<PowerPoleItem> {
    @Override
    public ResourceLocation getModelResource(PowerPoleItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/power_pole.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PowerPoleItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/armas/power_pole.png");
    }

    @Override
    public ResourceLocation getAnimationResource(PowerPoleItem animatable) {
        return null;
    }
}
