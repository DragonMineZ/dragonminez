package com.dragonminez.client.init.entities.model;


import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.MastersEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.util.Mth;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.HashMap;
import java.util.Map;

public class MasterGlobalModel<T extends MastersEntity> extends GeoModel<T> {

    private static final Map<String, Boolean> MODEL_CACHE = new HashMap<>();
    private static final Map<String, Boolean> TEXTURE_CACHE = new HashMap<>();
    private static final Map<String, Boolean> ANIM_CACHE = new HashMap<>();

    @Override
    public ResourceLocation getModelResource(T animatable) {
        String name = ForgeRegistries.ENTITY_TYPES.getKey(animatable.getType()).getPath();
        ResourceLocation original = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/master/" + name + ".geo.json");

        boolean exists = MODEL_CACHE.computeIfAbsent(name, k -> resourceExists(original));
        return exists ? original : ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/enemies/robotxv.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        String name = ForgeRegistries.ENTITY_TYPES.getKey(animatable.getType()).getPath();
        ResourceLocation original = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/master/" + name + ".png");

        boolean exists = TEXTURE_CACHE.computeIfAbsent(name, k -> resourceExists(original));
        return exists ? original : ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/enemies/robotxv.png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        String name = ForgeRegistries.ENTITY_TYPES.getKey(animatable.getType()).getPath();
        ResourceLocation original = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/master/" + name + ".animation.json");

        boolean exists = ANIM_CACHE.computeIfAbsent(name, k -> resourceExists(original));
        return exists ? original : ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/master/master_goku.animation.json");
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        CoreGeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
            head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
        }
    }

    private boolean resourceExists(ResourceLocation location) {
        return Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
    }
}
