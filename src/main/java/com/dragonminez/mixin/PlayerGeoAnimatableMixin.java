package com.dragonminez.mixin;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

@Mixin(AbstractClientPlayer.class)
public abstract class PlayerGeoAnimatableMixin implements GeoAnimatable {

    // 1. El 'Cache' que Geckolib necesita
    @Unique
    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);

    // 2. Definiciones de Animaciones (¡Necesitas definirlas para CADA raza!)
    //    Esto es un ejemplo, deberías mover esto a tus clases de Modelo
    @Unique
    private static final RawAnimation RACE1_IDLE = RawAnimation.begin().thenLoop("animation.race1.idle");
    @Unique
    private static final RawAnimation RACE1_WALK = RawAnimation.begin().thenLoop("animation.race1.walk");
    @Unique
    private static final RawAnimation RACE1_CHARGE_KI = RawAnimation.begin().thenLoop("animation.race1.charge_ki");

    @Unique
    private static final RawAnimation RACE2_IDLE = RawAnimation.begin().thenLoop("animation.race2.idle");
    @Unique
    private static final RawAnimation RACE2_WALK = RawAnimation.begin().thenLoop("animation.race2.walk");


    // 3. Método requerido: Registrar los controladores
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        // Registramos un controlador principal.
        // La lógica está en el método 'predicate' de abajo
        //registrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }



    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    @Override
    public double getTick(Object o) {
        return ((AbstractClientPlayer) o).tickCount
                + net.minecraft.client.Minecraft.getInstance().getPartialTick();
    }
}