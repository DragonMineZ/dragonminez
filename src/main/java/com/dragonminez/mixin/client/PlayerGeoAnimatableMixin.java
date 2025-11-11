package com.dragonminez.mixin.client;

import com.dragonminez.client.animation.IPlayerAnimatable;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3d;
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
public abstract class PlayerGeoAnimatableMixin implements GeoAnimatable, IPlayerAnimatable {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.base.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.base.walk");
    private static final RawAnimation RUN  = RawAnimation.begin().thenLoop("animation.base.run");
    private static final RawAnimation ATTACK  = RawAnimation.begin().thenPlay("animation.base.attack1");
    private static final RawAnimation ATTACK2 = RawAnimation.begin().thenPlay("animation.base.attack2");
    private static final RawAnimation FLY  = RawAnimation.begin().thenLoop("animation.base.fly");
    private static final RawAnimation JUMP = RawAnimation.begin().thenPlay("animation.base.jump");

    @Unique
    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);

    @Unique
    private boolean useAttack2 = false;

    @Unique
    private int lastAttackTime = 0;

    @Unique
    private boolean isPlayingAttack = false;

    @Unique
    private int attackAnimStartTime = 0;

    @Unique
    private boolean isCreativeFlying = false;


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

            // Ataque - usando attackAnim que está sincronizado en multiplayer
            // attackAnim > 0 significa que el jugador está en medio de un swing
            if (player.attackAnim > 0) {
                int currentTime = player.tickCount;
                if (!isPlayingAttack) {
                    int timeSinceLastAttack = currentTime - lastAttackTime;
                    if (timeSinceLastAttack > 40) {
                        useAttack2 = false;
                    } else if (timeSinceLastAttack >= 10) {
                        useAttack2 = !useAttack2;
                    } else {
                        useAttack2 = false;
                    }

                    lastAttackTime = currentTime;
                    attackAnimStartTime = currentTime;
                    isPlayingAttack = true;
                    ctl.setAnimation(useAttack2 ? ATTACK2 : ATTACK);
                }
                return PlayState.CONTINUE;
            } else {
                isPlayingAttack = false;
            }

            if (player.isFallFlying() || isCreativeFlying) {
                if (playing != FLY) {
                    ctl.setAnimation(FLY);
                }
                return PlayState.CONTINUE;
            }

            if (player.onGround()) {
                if (isMoving(player) && player.isSprinting()) { //corriendo
                    if (playing != RUN) {
                        ctl.setAnimation(RUN);
                    }
                } else if (isMoving(player)) { //walk normal
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

            if (!player.onGround()) {
                if (playing != JUMP) {
                    ctl.setAnimation(JUMP);
                }
                return PlayState.CONTINUE;
            }

            return PlayState.CONTINUE;

        }).orElse(PlayState.STOP);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        // Usar transitionLength de 0 para las animaciones de ataque para evitar interrupciones
        registrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
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

    @Unique
    private static boolean isMoving(LivingEntity entity) {
        final Vector3d currentPos = new Vector3d(entity.getX(), entity.getY(), entity.getZ());
        final Vector3d lastPos = new Vector3d(entity.xOld, entity.yOld, entity.zOld);
        final Vector3d expectedVelocity = currentPos.sub(lastPos);
        float avgVelocity = (float) (Math.abs(expectedVelocity.x) + Math.abs(expectedVelocity.z) / 2.0);
        return avgVelocity >= 0.015;
    }

    @Override
    public void dragonminez$setCreativeFlying(boolean flying) {
        this.isCreativeFlying = flying;
    }
}