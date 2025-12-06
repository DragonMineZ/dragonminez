package com.dragonminez.mixin.client;

import com.dragonminez.client.animation.IPlayerAnimatable;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
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
    private static final RawAnimation SWIMMING = RawAnimation.begin().thenLoop("animation.base.swimming");
    private static final RawAnimation CROUCHING = RawAnimation.begin().thenLoop("animation.base.crouching");
    private static final RawAnimation CROUCHING_WALK = RawAnimation.begin().thenLoop("animation.base.crouching_walk");
    private static final RawAnimation SHIELD_RIGHT = RawAnimation.begin().thenLoop("animation.base.shield_right");
    private static final RawAnimation SHIELD_LEFT = RawAnimation.begin().thenLoop("animation.base.shield_left");
    private static final RawAnimation CRAWLING = RawAnimation.begin().thenLoop("animation.base.crawling");
    private static final RawAnimation CRAWLING_MOVE = RawAnimation.begin().thenLoop("animation.base.crawling_move");



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

            // Attacking - Skip if placing blocks
//            if (player.attackAnim > 0 && !isPlacingBlock(player)) {
//                int currentTime = player.tickCount;
//                if (!isPlayingAttack) {
//                    int timeSinceLastAttack = currentTime - lastAttackTime;
//                    if (timeSinceLastAttack > 40) {
//                        useAttack2 = false;
//                    } else if (timeSinceLastAttack >= 10) {
//                        useAttack2 = !useAttack2;
//                    } else {
//                        useAttack2 = false;
//                    }
//
//                    lastAttackTime = currentTime;
//                    attackAnimStartTime = currentTime;
//                    isPlayingAttack = true;
//                    ctl.setAnimation(useAttack2 ? ATTACK2 : ATTACK);
//                }
//                return PlayState.CONTINUE;
//            } else {
//                isPlayingAttack = false;
//            }

            // Swimming
            if (player.isSwimming()) {
                return state.setAndContinue(SWIMMING);
            }

            if (player.isVisuallyCrawling()) {
                if (isMoving(player)) {
                    return state.setAndContinue(CRAWLING_MOVE);
                } else {
                    return state.setAndContinue(CRAWLING);
                }
            }

            // Flying
            if (player.isFallFlying() || isCreativeFlying) {
                if (playing != FLY) {
                    ctl.setAnimation(FLY);
                }
                return PlayState.CONTINUE;
            }

            if (player.onGround()) {
                // Crouching
                if (player.isCrouching()) {
                    if (isMoving(player)) { // Walking
                        if (playing != CROUCHING_WALK) {
                            ctl.setAnimation(CROUCHING_WALK);
                        }
                    } else { // Still
                        if (playing != CROUCHING) {
                            ctl.setAnimation(CROUCHING);
                        }
                    }
                } else if (isMoving(player) && player.isSprinting()) { // Running
                    if (playing != RUN) {
                        ctl.setAnimation(RUN);
                    }
                } else if (isMoving(player)) { // Walking
                    if (playing != WALK) {
                        ctl.setAnimation(WALK);
                    }
                } else { // Idle
                    if (playing != IDLE) {
                        ctl.setAnimation(IDLE);
                    }
                }
                return PlayState.CONTINUE;
            }

            // Jumping/Falling
            if (!player.onGround()) {
                if (playing != JUMP) {
                    ctl.setAnimation(JUMP);
                }
                return PlayState.CONTINUE;
            }

            return PlayState.CONTINUE;

        }).orElse(PlayState.STOP);
    }

    @Unique
    private <T extends GeoAnimatable> PlayState attackPredicate(AnimationState<T> state) {
        AbstractClientPlayer player = (AbstractClientPlayer) state.getAnimatable();
        AnimationController<T> ctl = state.getController();
        RawAnimation playing = ctl.getCurrentRawAnimation();

        if (player.attackAnim > 0 && !isPlacingBlock(player)) {
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
            return PlayState.STOP;
        }
    }

    @Unique
    private <T extends GeoAnimatable> PlayState shieldPredicate(AnimationState<T> state) {
        AbstractClientPlayer player = (AbstractClientPlayer) state.getAnimatable();
        AnimationController<T> ctl = state.getController();

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean mainHandIsShield = mainHand.getItem() instanceof ShieldItem;
        boolean offHandIsShield = offHand.getItem() instanceof ShieldItem;

        if (player.isUsingItem()) {
            if (mainHandIsShield) {
                if (player.getMainArm() == HumanoidArm.RIGHT) {
                    ctl.setAnimation(SHIELD_RIGHT);
                } else {
                    ctl.setAnimation(SHIELD_LEFT);
                }
                return PlayState.CONTINUE;
            } else if (offHandIsShield) {
                if (player.getMainArm() == HumanoidArm.RIGHT) {
                    ctl.setAnimation(SHIELD_LEFT);
                } else {
                    ctl.setAnimation(SHIELD_RIGHT);
                }
                return PlayState.CONTINUE;
            }
        }

        return PlayState.STOP;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "controller", 4, this::predicate));
        registrar.add(new AnimationController<>(this, "attack_controller", 0, this::attackPredicate));
        registrar.add(new AnimationController<>(this, "shield_controller", 0, this::shieldPredicate));
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

    @Unique
    private static boolean isPlacingBlock(AbstractClientPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean hasBlockInMainHand = mainHand.getItem() instanceof BlockItem;
        boolean hasBlockInOffHand = offHand.getItem() instanceof BlockItem;

        return (hasBlockInMainHand || hasBlockInOffHand) && player.isUsingItem();
    }

    @Override
    public void dragonminez$setCreativeFlying(boolean flying) {
        this.isCreativeFlying = flying;
    }
}
