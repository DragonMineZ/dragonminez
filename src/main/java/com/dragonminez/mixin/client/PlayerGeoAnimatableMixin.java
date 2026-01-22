package com.dragonminez.mixin.client;

import com.dragonminez.client.animation.IPlayerAnimatable;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.lists.MajinForms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.Objects;

import static com.dragonminez.client.animation.Animations.*;
import static com.dragonminez.client.animation.Animations.ATTACK;
import static com.dragonminez.client.animation.Animations.ATTACK2;
import static com.dragonminez.client.animation.Animations.BLOCK;
import static com.dragonminez.client.animation.Animations.CROUCHING;
import static com.dragonminez.client.animation.Animations.CROUCHING_WALK;
import static com.dragonminez.client.animation.Animations.FLY;
import static com.dragonminez.client.animation.Animations.IDLE;
import static com.dragonminez.client.animation.Animations.JUMP;
import static com.dragonminez.client.animation.Animations.RUN;
import static com.dragonminez.client.animation.Animations.SHIELD_LEFT;
import static com.dragonminez.client.animation.Animations.SHIELD_RIGHT;
import static com.dragonminez.client.animation.Animations.TAIL;
import static com.dragonminez.client.animation.Animations.WALK;

@Mixin(AbstractClientPlayer.class)
public abstract class PlayerGeoAnimatableMixin implements GeoAnimatable, IPlayerAnimatable {

    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "controller", 4, this::predicate));
        registrar.add(new AnimationController<>(this, "attack_controller", 0, this::attackPredicate));
        registrar.add(new AnimationController<>(this, "block_controller", 3, this::blockPredicate));
        registrar.add(new AnimationController<>(this, "shield_controller", 3, this::shieldPredicate));
        registrar.add(new AnimationController<>(this, "tailcontroller", 0, this::tailpredicate));
    }

    @Unique
    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> state) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        IPlayerAnimatable animatable = (IPlayerAnimatable) this;

        AnimationController<T> ctl = state.getController();
        RawAnimation playing = ctl.getCurrentRawAnimation();

        return StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> {
            String raceName = data.getCharacter().getRace();
            // Si no es humano no hace ninguna animacion, esto lo usare luego para la raza majin o no se
//            if (!raceName.equals("human")) {
//                return PlayState.STOP;
//            }

            // Swimming
            if (player.isSwimming()) return state.setAndContinue(SWIMMING);

            if (player.isVisuallyCrawling()) {
                if (state.isMoving()) return state.setAndContinue(CRAWLING_MOVE);
                else return state.setAndContinue(CRAWLING);
            }

            // Flying
            if (player.isFallFlying() || animatable.dragonminez$isCreativeFlying()) {
                if (playing != FLY) ctl.setAnimation(FLY);
                return PlayState.CONTINUE;
            }

            if (player.onGround()) {
                // Crouching
                if (player.isCrouching()) {
                    if (state.isMoving()) {
                        // Walking
                        if (playing != CROUCHING_WALK) ctl.setAnimation(CROUCHING_WALK);
                    } else {
                        // Still
                        if (playing != CROUCHING) ctl.setAnimation(CROUCHING);

                    }
                } else if (state.isMoving() && player.isSprinting()) {
                    // Running
                    if (playing != RUN) ctl.setAnimation(RUN);
                } else if (state.isMoving()) {
                    // Walking
                    if (playing != WALK) ctl.setAnimation(WALK);
                } else {
                    // Idle
                    if (playing != IDLE) ctl.setAnimation(IDLE);
                }
                return PlayState.CONTINUE;
            }

            if (playing != JUMP) {
                ctl.setAnimation(JUMP);
            }
            return PlayState.CONTINUE;

        }).orElse(PlayState.STOP);
    }

    @Unique
    private <T extends GeoAnimatable> PlayState tailpredicate(AnimationState<T> state) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;

        return StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> {
            var character = data.getCharacter();
            // Usar toLowerCase() para evitar errores de mayúsculas/minúsculas
            String race = character.getRace().toLowerCase();
            String gender = character.getGender().toLowerCase();
            String currentForm = character.getActiveForm();

            // 1. LÓGICA DE VISIBILIDAD DE COLA
            // Para Majin, verificamos si es mujer Y está en Super o Ultra
            boolean isMajinWithTail = race.equals("majin") &&
                    (gender.equals("female") || gender.equals("mujer")) &&
                    (Objects.equals(currentForm, MajinForms.SUPER) || Objects.equals(currentForm, MajinForms.ULTRA));

            boolean hasTail = (race.equals("saiyan") && data.getStatus().isTailVisible())
                    || race.equals("frostdemon")
                    || race.equals("bioandroid")
                    || isMajinWithTail;

            if (!hasTail) {
                return PlayState.STOP;
            }

            // 2. PRIORIDAD DE ANIMACIÓN
            // Si el jugador se está moviendo, es mejor dejar que las animaciones de
            // walk/run controlen la cola para que el movimiento sea natural.
//            if (state.isMoving()) {
//                return PlayState.STOP;
//            }

            // 3. REPRODUCIR IDLE
            // setAndContinue es ideal para animaciones en bucle como el movimiento suave de la cola
            return state.setAndContinue(TAIL);

        }).orElse(PlayState.STOP);
    }

    @Unique
    private <T extends GeoAnimatable> PlayState attackPredicate(AnimationState<T> state) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        IPlayerAnimatable animatable = (IPlayerAnimatable) this;

        AnimationController<T> ctl = state.getController();
        if (player.attackAnim > 0 && !isPlacingBlock(player)) {
            if (!animatable.dragonminez$isPlayingAttack()) {
                animatable.dragonminez$setPlayingAttack(true);
                ctl.setAnimation(animatable.dragonminez$useAttack2() ? ATTACK2 : ATTACK);
                animatable.dragonminez$setUseAttack2(!animatable.dragonminez$useAttack2());
            }
            return PlayState.CONTINUE;
        } else {
            animatable.dragonminez$setPlayingAttack(false);
            return PlayState.STOP;
        }
    }

    @Unique
    private <T extends GeoAnimatable> PlayState blockPredicate(AnimationState<T> state) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;

        AnimationController<T> ctl = state.getController();

        return StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> {
            if (data.getStatus().isBlocking()) {
                if (ctl.getAnimationState() == AnimationController.State.STOPPED || !ctl.getCurrentRawAnimation().equals(BLOCK)) {
                    ctl.setAnimation(BLOCK);
                    ctl.forceAnimationReset();
                }
                return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }).orElse(PlayState.STOP);
    }

    @Unique
    private <T extends GeoAnimatable> PlayState shieldPredicate(AnimationState<T> state) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;

        AnimationController<T> ctl = state.getController();

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean mainHandIsShield = mainHand.getItem() instanceof ShieldItem;
        boolean offHandIsShield = offHand.getItem() instanceof ShieldItem;

        if (player.isUsingItem()) {
            if (mainHandIsShield || offHandIsShield) {
                RawAnimation targetAnimation;
                boolean isRightHanded = player.getMainArm() == HumanoidArm.RIGHT;

                if (mainHandIsShield) targetAnimation = isRightHanded ? SHIELD_RIGHT : SHIELD_LEFT;
                else targetAnimation = isRightHanded ? SHIELD_LEFT : SHIELD_RIGHT;

                if (ctl.getAnimationState() == AnimationController.State.STOPPED || !ctl.getCurrentRawAnimation().equals(targetAnimation)) {
                    ctl.setAnimation(targetAnimation);
                    ctl.forceAnimationReset();
                }
                return PlayState.CONTINUE;
            }
        }

        return PlayState.STOP;
    }

    @Unique
    private static boolean isPlacingBlock(AbstractClientPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean hasBlockInMainHand = mainHand.getItem() instanceof BlockItem;
        boolean hasBlockInOffHand = offHand.getItem() instanceof BlockItem;

        return (hasBlockInMainHand || hasBlockInOffHand) && player.isUsingItem();
    }


    @Override
    public double getTick(Object o) {
        return ((AbstractClientPlayer) o).tickCount + Minecraft.getInstance().getPartialTick();
    }



    @Unique
    private boolean dragonminez$useAttack2 = false;

    @Unique
    private boolean dragonminez$isPlayingAttack = false;

    @Unique
    private boolean dragonminez$isCreativeFlying = false;

    @Override
    public void dragonminez$setUseAttack2(boolean useAttack2) {
        this.dragonminez$useAttack2 = useAttack2;
    }

    @Override
    public boolean dragonminez$useAttack2() {
        return this.dragonminez$useAttack2;
    }

    @Override
    public void dragonminez$setPlayingAttack(boolean playingAttack) {
        dragonminez$isPlayingAttack = playingAttack;
    }

    @Override
    public boolean dragonminez$isPlayingAttack() {
        return dragonminez$isPlayingAttack;
    }

    @Override
    public void dragonminez$isCreativeFlying(boolean flying) {
        this.dragonminez$isCreativeFlying = flying;
    }

    @Override
    public boolean dragonminez$isCreativeFlying() {
        return dragonminez$isCreativeFlying;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

}
