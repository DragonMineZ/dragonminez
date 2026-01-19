package com.dragonminez.client.render.data;

import com.dragonminez.client.animation.IPlayerAnimatable;
import com.dragonminez.common.stats.Cooldowns;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.animatable.GeoReplacedEntity;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import static com.dragonminez.client.animation.Animations.*;

public class DMZAnimatable implements GeoReplacedEntity {

    public static final DMZAnimatable INSTANCE = new DMZAnimatable();
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

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
        if (!(state.getData(DataTickets.ENTITY) instanceof AbstractClientPlayer player)) return PlayState.STOP;
        if (!(player instanceof IPlayerAnimatable animatable)) return PlayState.STOP;

        AnimationController<T> ctl = state.getController();
        RawAnimation playing = ctl.getCurrentRawAnimation();

        return StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> {
            String raceName = data.getCharacter().getRace();
            // Si no es humano no hace ninguna animacion, esto lo usare luego para la raza majin o no se
//            if (!raceName.equals("human")) {
//                return PlayState.STOP;
//            }

			if (data.getCooldowns().hasCooldown(Cooldowns.DRAIN_ACTIVE)) {
				if (playing != DRAIN) ctl.setAnimation(DRAIN);
				return PlayState.CONTINUE;
			}

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
        if (!(state.getData(DataTickets.ENTITY) instanceof AbstractClientPlayer player)) return PlayState.STOP;

        return StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> {
            String race = data.getCharacter().getRace();
            boolean hasTail = race.equals("saiyan") && data.getStatus().isTailVisible() || race.equals("frostdemon") || race.equals("bioandroid");
            if (!hasTail) return PlayState.STOP;
			if (data.getCooldowns().hasCooldown(Cooldowns.DRAIN_ACTIVE)) return PlayState.STOP;

//
//            // 2. CONFLICTO DE MOVIMIENTO
//            // Si el jugador camina/corre, generalmente la animación "walk" ya incluye el movimiento de la cola.
//            // Por lo tanto, detenemos esta animación "idle" para que no peleen entre sí.
//            if (state.isMoving()) {
//                return PlayState.STOP;
//            }
//
//            // 3. REPRODUCIR (Idle)
//            // Si tiene cola y está quieto, reproducimos la animación definida.
            // Si NO tiene cola, paramos la animación para ahorrar recursos.

            return state.setAndContinue(TAIL);

        }).orElse(PlayState.STOP);
    }

    @Unique
    private <T extends GeoAnimatable> PlayState attackPredicate(AnimationState<T> state) {
        if (!(state.getData(DataTickets.ENTITY) instanceof AbstractClientPlayer player)) return PlayState.STOP;
        if (!(player instanceof IPlayerAnimatable animatable)) return PlayState.STOP;


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
		if (!(state.getData(DataTickets.ENTITY) instanceof AbstractClientPlayer player)) return PlayState.STOP;

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
        if (!(state.getData(DataTickets.ENTITY) instanceof AbstractClientPlayer player)) return PlayState.STOP;

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
    public EntityType<?> getReplacingEntityType() {
        return EntityType.PLAYER;
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}