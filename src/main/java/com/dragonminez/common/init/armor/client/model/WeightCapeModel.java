package com.dragonminez.common.init.armor.client.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.WeightItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WeightCapeModel extends GeoModel<WeightItem> {
    @Override
    public ResourceLocation getModelResource(WeightItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/armor/weighted_cape.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(WeightItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/weighted_items.png");
    }

    @Override
    public ResourceLocation getAnimationResource(WeightItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/armorcape.animation.json");
    }
}
