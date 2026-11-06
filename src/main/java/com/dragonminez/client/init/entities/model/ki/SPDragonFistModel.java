package com.dragonminez.client.init.entities.model.ki;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.ki.SPDragonFistEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.math.MolangQueries;
import software.bernie.geckolib.model.GeoModel;

public class SPDragonFistModel<T extends SPDragonFistEntity> extends GeoModel<T> {

    public static final int STRIKE_TICKS = 8;

    static {
        MolangQueries.<SPDragonFistEntity>setActorVariable("variable.dmz_df_strike", actor -> {
            SPDragonFistEntity entity = actor.animatable();
            double age = entity.tickCount + actor.mc().getTimer().getGameTimeDeltaPartialTick(false);
            float strike = Mth.clamp((float) ((age - (entity.getMaxLife() - STRIKE_TICKS)) / STRIKE_TICKS), 0.0f, 1.0f);
            return strike * strike * (3.0f - 2.0f * strike);
        });
    }

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/dragon/shenron.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/skills/sp_dragonfist_shenron.png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/skills/sp_dragonfist_shenron.animation.json");
    }

    @Override
    public void applyMolangQueries(AnimationState<T> animationState, double animTime) {
        super.applyMolangQueries(animationState, animTime);

        // 1. Busca el hueso principal de tu modelo (Asegúrate de que se llame así en Blockbench)
        // Si tu hueso principal se llama "cuerpo" o "body", cámbialo aquí:
        GeoBone rootBone = this.getAnimationProcessor().getBone("root");

        if (rootBone != null) {
            // 2. Obtenemos los ángulos bloqueados desde la entidad
            float lockedYaw = animationState.getAnimatable().getLockedYaw();
            float lockedPitch = animationState.getAnimatable().getLockedPitch();

            // 3. Forzamos la rotación en el hueso usando radianes
            // GeckoLib requiere radianes y a veces invierte ejes. Si ves que rota al revés, quita el signo negativo (-).
            rootBone.setRotX(-lockedPitch * Mth.DEG_TO_RAD);
            rootBone.setRotY(-lockedYaw * Mth.DEG_TO_RAD);
        }

    }
}
