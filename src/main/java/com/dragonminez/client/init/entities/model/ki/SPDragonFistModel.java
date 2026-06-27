package com.dragonminez.client.init.entities.model.ki;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.animal.DinoFlyEntity;
import com.dragonminez.common.init.entities.ki.SPBlueHurricaneEntity;
import com.dragonminez.common.init.entities.ki.SPDragonFistEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class SPDragonFistModel<T extends SPDragonFistEntity> extends GeoModel<T> {

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/skills/sp_dragonfist.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/skills/sp_dragonfist.png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/skills/sp_dragonfist.animation.json");
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        // 1. Busca el hueso principal de tu modelo (Asegúrate de que se llame así en Blockbench)
        // Si tu hueso principal se llama "cuerpo" o "body", cámbialo aquí:
        CoreGeoBone rootBone = this.getAnimationProcessor().getBone("root");

        if (rootBone != null) {
            // 2. Obtenemos los ángulos bloqueados desde la entidad
            float lockedYaw = animatable.getLockedYaw();
            float lockedPitch = animatable.getLockedPitch();

            // 3. Forzamos la rotación en el hueso usando radianes
            // GeckoLib requiere radianes y a veces invierte ejes. Si ves que rota al revés, quita el signo negativo (-).
            rootBone.setRotX(-lockedPitch * Mth.DEG_TO_RAD);
            rootBone.setRotY(-lockedYaw * Mth.DEG_TO_RAD);
        }
    }

}
