package com.yuseix.dragonminez.init.entity.client.model;

import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.init.entity.custom.KarinEntity;
import com.yuseix.dragonminez.init.entity.custom.ShenlongEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class ShenlongModel extends GeoModel<ShenlongEntity> {
    @Override
    public ResourceLocation getModelResource(ShenlongEntity shenlongEntity) {
        return new ResourceLocation(DragonMineZ.MOD_ID, "geo/shenlong.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ShenlongEntity shenlongEntity) {
        return new ResourceLocation(DragonMineZ.MOD_ID, "textures/entity/shenlong.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ShenlongEntity shenlongEntity) {
        return null;
    }

    
}