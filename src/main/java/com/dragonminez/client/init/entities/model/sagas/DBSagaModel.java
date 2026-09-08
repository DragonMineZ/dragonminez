package com.dragonminez.client.init.entities.model.sagas;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
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

public class DBSagaModel<T extends DBSagasEntity> extends GeoModel<T> {

    private static final Map<ResourceLocation, Boolean> RESOURCE_CACHE = new HashMap<>();
    private static final Map<String, Integer> VARIANT_COUNT_CACHE = new HashMap<>();

    private static final int MAX_VARIANT_PROBE = 16;

    public static void clearCache() {
        RESOURCE_CACHE.clear();
        VARIANT_COUNT_CACHE.clear();
    }

    @Override
    public ResourceLocation getModelResource(T animatable) {
        ResourceLocation original = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/sagas/" + animatable.getGeckolibModelName() + ".geo.json");

        boolean exists = RESOURCE_CACHE.computeIfAbsent(original, this::resourceExists);
        return exists ? original : ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/enemies/robotxv.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        String name = ForgeRegistries.ENTITY_TYPES.getKey(animatable.getType()).getPath();
        int variant = animatable.getTextureVariant();

        // Variant 0 means "nothing assigned it a look"; entities that opt in roll their own here.
        // Anything that was assigned explicitly (quest objectives) is left alone.
        if (variant == 0 && animatable.usesRandomTextureVariant()) {
            variant = rollTextureVariant(animatable, name);
        }

        String variantSuffix = (variant == 0) ? "" : "_" + variant;
        ResourceLocation variantTexture = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/sagas/" + name + variantSuffix + ".png");

        boolean variantExists = RESOURCE_CACHE.computeIfAbsent(variantTexture, this::resourceExists);
        if (variantExists) {
            return variantTexture;
        }

        if (variant > 0) {
            ResourceLocation baseTexture = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/sagas/" + name + ".png");
            boolean baseExists = RESOURCE_CACHE.computeIfAbsent(baseTexture, this::resourceExists);
            if (baseExists) {
                return baseTexture;
            }
        }

        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/enemies/robotxv.png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/sagas/saga_base.animation.json");
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

    /**
     * Picks one of the textures that actually ship for this entity. Keyed off the entity's UUID so the
     * choice is stable across frames and reloads, and identical on every client that sees the mob.
     */
    private int rollTextureVariant(T animatable, String name) {
        int count = VARIANT_COUNT_CACHE.computeIfAbsent(name, this::countTextureVariants);
        if (count <= 1) return 0;
        return Math.floorMod(animatable.getUUID().hashCode(), count);
    }

    /** The base texture plus every consecutive {@code _1}, {@code _2}… variant that exists. */
    private int countTextureVariants(String name) {
        int count = 1;
        while (count <= MAX_VARIANT_PROBE && resourceExists(ResourceLocation.fromNamespaceAndPath(
                Reference.MOD_ID, "textures/entity/sagas/" + name + "_" + count + ".png"))) {
            count++;
        }
        return count;
    }

    private boolean resourceExists(ResourceLocation location) {
        return Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
    }
}