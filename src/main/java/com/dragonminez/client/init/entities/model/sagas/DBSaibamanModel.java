package com.dragonminez.client.init.entities.model.sagas;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.core.registries.BuiltInRegistries;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class DBSaibamanModel<T extends DBSagasEntity> extends GeoModel<T> {

    @Override
    public ResourceLocation getModelResource(T animatable) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(animatable.getType()).getPath();
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/sagas/saga_saibaman.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(animatable.getType()).getPath();
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/sagas/" + name + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(animatable.getType()).getPath();
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/sagas/saga_saibaman.animation.json");
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        GeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
            head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
        }
    }

}
