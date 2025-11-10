package com.dragonminez.mixin.common;

import com.dragonminez.common.stats.Character;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
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

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.base.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.base.walk");
    private static final RawAnimation RUN  = RawAnimation.begin().thenLoop("animation.base.run");
    private static final RawAnimation ATTACK  = RawAnimation.begin().thenLoop("animation.base.attack1");
    private static final RawAnimation FLY  = RawAnimation.begin().thenLoop("animation.base.fly");


    @Unique
    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);

    @Unique
    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> state) {

        AbstractClientPlayer player = (AbstractClientPlayer) state.getAnimatable();
        AnimationController<T> ctl = state.getController();
        RawAnimation playing = ctl.getCurrentRawAnimation();

        return StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> {

            int raceId = data.getCharacter().getRace();

            // Si no es humano no hace ninguna animacion, esto lo usare luego para la raza majin o no se
//            if (raceId != Character.RACE_HUMAN) {
//                return PlayState.STOP;
//            }

            if (player.swinging) { //Ataque
                if (playing != ATTACK) {
                    ctl.setAnimation(ATTACK);
                }
                return PlayState.CONTINUE;
            }

            // El jugador vuela (por el momento toma como vuelo el del creativo xd)
            if (player.isFallFlying() || player.getAbilities().flying) {
                if (playing != FLY) {
                    ctl.setAnimation(FLY);
                }
                return PlayState.CONTINUE;
            }

            if (player.onGround()) {
                boolean isMoving = player.getDeltaMovement().horizontalDistanceSqr() > 0.0025;
                if (isMoving && player.isSprinting()) { //corriendo
                    if (playing != RUN) {
                        ctl.setAnimation(RUN);
                    }
                } else if (isMoving) { //walk normal
                    if (playing != WALK) {
                        ctl.setAnimation(WALK);
                    }
                } else { //idle
                    if (playing != IDLE) {
                        ctl.setAnimation(IDLE);
                    }
                }
                return PlayState.CONTINUE;
            }

            // Aca es si no pasa nada de lo anterior, lo ideal seria poner una animacion para eso :v por ejemplo si esta cayendo
//            if (playing != IDLE) {
//                ctl.setAnimation(FALL);
//            }
            return PlayState.CONTINUE;

        }).orElse(PlayState.STOP);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "controller", 3, this::predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    @Override
    public double getTick(Object animatable) {
        return ((AbstractClientPlayer) animatable).tickCount
                + net.minecraft.client.Minecraft.getInstance().getPartialTick();
    }
}