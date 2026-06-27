package com.dragonminez.client.init.entities.model.ki;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.ki.OzaruFistEntity;
import com.dragonminez.common.init.entities.ki.SPDragonFistEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class SPOzaruFistModel<T extends OzaruFistEntity> extends GeoModel<T> {

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/skills/sp_ozarufist.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/skills/sp_ozarufist.png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/skills/sp_ozarufist.animation.json");
    }

}
