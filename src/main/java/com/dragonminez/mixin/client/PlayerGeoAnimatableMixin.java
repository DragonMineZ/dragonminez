package com.dragonminez.mixin.client;

import com.dragonminez.client.animation.IPlayerAnimatable;
import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.common.stats.Cooldowns;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.lists.MajinForms;
import com.dragonminez.common.util.lists.SaiyanForms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;
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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.dragonminez.client.animation.Animations.*;

@Mixin(AbstractClientPlayer.class)
public abstract class  PlayerGeoAnimatableMixin implements GeoAnimatable, IPlayerAnimatable {

	private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);

	@Unique
	private double dragonminez$lastPosX = Double.NaN;
	@Unique
	private double dragonminez$lastPosZ = Double.NaN;
	@Unique
	private int dragonminez$stoppedTicks = 0;
	@Unique
	private boolean dragonminez$isMovingState = false;
	@Unique
	private int dragonminez$lastTickCount = -1;
	@Unique
	private static final int STOPPED_THRESHOLD_TICKS = 3;
	@Unique
	private int dragonminez$lastDashTickRun = -1;
	@Unique
	private int dragonminez$lastAttackTick = -1;
	@Unique
	private int dragonminez$lastAttackTickRun = -1;
	@Unique
	private int dragonminez$comboVariant = 0;
	@Unique
	private int dragonminez$comboAnimTicks = 0;
	@Unique
	private int dragonminez$lastComboTickRun = -1;

	@Unique
	private boolean dragonminez$isActuallyMoving(AbstractClientPlayer player) {
		int currentTick = player.tickCount;

		if (currentTick == dragonminez$lastTickCount) {
			return dragonminez$isMovingState;
		}
		dragonminez$lastTickCount = currentTick;

		if (Double.isNaN(dragonminez$lastPosX)) {
			dragonminez$lastPosX = player.getX();
			dragonminez$lastPosZ = player.getZ();
			return false;
		}

		double deltaX = player.getX() - dragonminez$lastPosX;
		double deltaZ = player.getZ() - dragonminez$lastPosZ;
		double distanceSq = deltaX * deltaX + deltaZ * deltaZ;

		dragonminez$lastPosX = player.getX();
		dragonminez$lastPosZ = player.getZ();

		boolean isCurrentlyMoving = distanceSq > 0.0001;

		if (isCurrentlyMoving) {
			dragonminez$isMovingState = true;
			dragonminez$stoppedTicks = 0;
		} else {
			dragonminez$stoppedTicks++;
			if (dragonminez$stoppedTicks >= STOPPED_THRESHOLD_TICKS) {
				dragonminez$isMovingState = false;
			}
		}

		return dragonminez$isMovingState;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
		registrar.add(new AnimationController<>(this, "controller", 4, this::predicate));
		registrar.add(new AnimationController<>(this, "combo_controller", 0, this::comboPredicate));
		registrar.add(new AnimationController<>(this, "attack_controller", 0, this::attackPredicate));
		registrar.add(new AnimationController<>(this, "mining_controller", 0, this::miningPredicate));
		registrar.add(new AnimationController<>(this, "block_controller", 3, this::blockPredicate));
		registrar.add(new AnimationController<>(this, "shield_controller", 3, this::shieldPredicate));
		registrar.add(new AnimationController<>(this, "tailcontroller", 0, this::tailpredicate));
		registrar.add(new AnimationController<>(this, "dash_controller", 0, this::dashPredicate));
	}

	@Unique
	private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> state) {
		AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
		IPlayerAnimatable animatable = (IPlayerAnimatable) this;
		if (dragonminez$dashAnimTicks > 0) return PlayState.STOP;

		boolean isMoving = dragonminez$isActuallyMoving(player);

		AtomicBoolean isDraining = new AtomicBoolean(false), flySkillActive = new AtomicBoolean(false),
				isChargingKi = new AtomicBoolean(false), isBlocking = new AtomicBoolean(false);
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			isDraining.set(data.getCooldowns().hasCooldown(Cooldowns.DRAIN_ACTIVE));
			flySkillActive.set(data.getSkills().isSkillActive("fly"));
			isChargingKi.set(data.getStatus().isChargingKi() || data.getStatus().isActionCharging());
			isBlocking.set(data.getStatus().isBlocking());
		});

		if (isDraining.get()) return state.setAndContinue(DRAIN);

		if (player.isPassenger()) return state.setAndContinue(SIT);

		if (isChargingKi.get() && !isMoving && !isBlocking.get()) return state.setAndContinue(KI_CHARGE);


		// Swimming
		if (player.isSwimming()) return state.setAndContinue(SWIMMING);

		if (player.isVisuallyCrawling()) {
			if (isMoving) return state.setAndContinue(CRAWLING_MOVE);
			else return state.setAndContinue(CRAWLING);
		}

		// Flying
		if (flySkillActive.get() || player.isFallFlying() || animatable.dragonminez$isFlying()) {
			if (FlySkillEvent.isFlyingFast()) return state.setAndContinue(FLY_FAST);
			else return state.setAndContinue(FLY);
		}

		if (player.onGround()) {
			// Crouching
			if (player.isCrouching()) {
				if (isMoving) return state.setAndContinue(CROUCHING_WALK);
				else return state.setAndContinue(CROUCHING);
			} else if (isMoving && player.isSprinting()) {
				// Running
				return state.setAndContinue(RUN);
			} else if (isMoving) {
				// Walking
				return state.setAndContinue(WALK);
			} else {
				// Idle
				return state.setAndContinue(IDLE);
			}
		}

		// Jumping/Falling
		return state.setAndContinue(JUMP);
	}

	@Unique
	private <T extends GeoAnimatable> PlayState tailpredicate(AnimationState<T> state) {
		AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;

		return StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> {
			var character = data.getCharacter();
			String race = character.getRace().toLowerCase();
			String gender = character.getGender().toLowerCase();
			String currentForm = character.getActiveForm();

			boolean isOozaru = race.equals("saiyan") && (
					Objects.equals(currentForm, SaiyanForms.OOZARU) ||
							Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU)
			);

			boolean isMajinWithTail = race.equals("majin") &&
					(gender.equals("female") || gender.equals("mujer")) &&
					(Objects.equals(currentForm, MajinForms.SUPER) || Objects.equals(currentForm, MajinForms.ULTRA));

			boolean hasTail = isOozaru
					|| (race.equals("saiyan") && data.getStatus().isTailVisible())
					|| race.equals("frostdemon")
					|| (race.equals("bioandroid") && !data.getCooldowns().hasCooldown(Cooldowns.DRAIN_ACTIVE))
					|| isMajinWithTail;

			if (!hasTail) {
				return PlayState.STOP;
			}

			return state.setAndContinue(TAIL);

		}).orElse(PlayState.STOP);
	}

	@Unique
	private <T extends GeoAnimatable> PlayState comboPredicate(AnimationState<T> state) {
		AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;

		if (player.tickCount != dragonminez$lastComboTickRun) {
			dragonminez$lastComboTickRun = player.tickCount;
			if (dragonminez$comboAnimTicks > 0) dragonminez$comboAnimTicks--;
		}

		if (dragonminez$comboAnimTicks > 0) {
			RawAnimation anim = switch (dragonminez$comboVariant) {
				case 2 -> COMBO2;
				case 3 -> COMBO3;
				case 4 -> COMBO4;
				default -> COMBO1;
			};

			state.getController().setAnimation(anim);
			return PlayState.CONTINUE;
		}
		return PlayState.STOP;
	}

	@Unique
	private <T extends GeoAnimatable> PlayState attackPredicate(AnimationState<T> state) {
		if (dragonminez$comboAnimTicks > 0) return PlayState.STOP;

		AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
		IPlayerAnimatable animatable = (IPlayerAnimatable) this;
		AnimationController<T> ctl = state.getController();

		if (player.tickCount != dragonminez$lastAttackTickRun) {
			dragonminez$lastAttackTickRun = player.tickCount;
			if (dragonminez$attackAnimTicks > 0) dragonminez$attackAnimTicks--;
		}

		boolean isValidAttackContext = player.swinging && !isPlacingBlock(player) && !isBlocking(player) && !isUsingTool(player);

		if (isValidAttackContext) {
			boolean isNewAttackFrame = player.swingTime == 0;
			boolean alreadyProcessedThisTick = player.tickCount == dragonminez$lastAttackTick;

			if (isNewAttackFrame && !alreadyProcessedThisTick) {
				dragonminez$lastAttackTick = player.tickCount;
				animatable.dragonminez$setPlayingAttack(true);
				animatable.dragonminez$setUseAttack2(!animatable.dragonminez$useAttack2());
				ctl.setAnimation(animatable.dragonminez$useAttack2() ? ATTACK2 : ATTACK);
				ctl.forceAnimationReset();
				dragonminez$attackAnimTicks = 12;

				return PlayState.CONTINUE;
			}
		}

		if (dragonminez$attackAnimTicks > 0) return PlayState.CONTINUE;
		animatable.dragonminez$setPlayingAttack(false);
		return PlayState.STOP;
	}

	@Unique
	private <T extends GeoAnimatable> PlayState miningPredicate(AnimationState<T> state) {
		AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
		AnimationController<T> ctl = state.getController();

		if (player.attackAnim > 0 && isUsingTool(player) && !isPlacingBlock(player) && !isBlocking(player)) {
			if (ctl.getAnimationState() == AnimationController.State.STOPPED) {
				if (isMainHandTool(player)) {
					ctl.setAnimation(MINING1);
				} else if (isOffHandTool(player)) {
					ctl.setAnimation(MINING2);
				}
			}
			dragonminez$miningAnimTicks = 10;
			return PlayState.CONTINUE;
		}

		if (dragonminez$miningAnimTicks > 0) {
			dragonminez$miningAnimTicks--;
			return PlayState.CONTINUE;
		}

		return PlayState.STOP;
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
	private int dragonminez$dashAnimTicks = 0;

	@Unique
	private <T extends GeoAnimatable> PlayState dashPredicate(AnimationState<T> state) {
		AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
		AnimationController<T> ctl = state.getController();

		if (player.tickCount != dragonminez$lastDashTickRun) {
			dragonminez$lastDashTickRun = player.tickCount;
			if (dragonminez$dashAnimTicks > 0) dragonminez$dashAnimTicks--;
		}

		if (dragonminez$dashAnimTicks > 0) return PlayState.CONTINUE;

		if (dragonminez$isEvading) {
			RawAnimation evasionAnim = switch (dragonminez$evasionVariant) {
				case 1 -> EVASION1;
				case 2 -> EVASION2;
				case 3 -> EVASION3;
				default -> EVASION4;
			};
			ctl.setAnimation(evasionAnim);
			ctl.forceAnimationReset();
			dragonminez$dashAnimTicks = 12;
			dragonminez$isEvading = false;
			return PlayState.CONTINUE;
		}

		if (dragonminez$dashDirection != 0) {
			RawAnimation dashAnim = switch (dragonminez$dashDirection) {
				case 2 -> DASH_BACKWARD;
				case 3 -> DASH_RIGHT;
				case 4 -> DASH_LEFT;

				case 6 -> DOUBLEDASH_BACKWARD;
				case 7 -> DOUBLEDASH_RIGHT;
				case 8 -> DOUBLEDASH_LEFT;

				default -> DASH_FORWARD;
			};
			ctl.setAnimation(dashAnim);
			ctl.forceAnimationReset();
			dragonminez$dashAnimTicks = 12;
			dragonminez$dashDirection = 0;
			return PlayState.CONTINUE;
		}

		return PlayState.STOP;
	}

	@Unique
	private static boolean isUsingTool(AbstractClientPlayer player) {
		Item mainHand = player.getMainHandItem().getItem();
		Item offHand = player.getOffhandItem().getItem();

		return mainHand.getDescriptionId().contains("pickaxe") || mainHand.getDescriptionId().contains("axe") || mainHand.getDescriptionId().contains("shovel") || mainHand.getDescriptionId().contains("hoe")
				|| offHand.getDescriptionId().contains("pickaxe") || offHand.getDescriptionId().contains("axe") || offHand.getDescriptionId().contains("shovel") || offHand.getDescriptionId().contains("hoe");
	}

	@Unique
	private static boolean isMainHandTool(AbstractClientPlayer player) {
		Item mainHand = player.getMainHandItem().getItem();
		return mainHand.getDescriptionId().contains("pickaxe") || mainHand.getDescriptionId().contains("axe") || mainHand.getDescriptionId().contains("shovel") || mainHand.getDescriptionId().contains("hoe");
	}

	@Unique
	private static boolean isOffHandTool(AbstractClientPlayer player) {
		Item offHand = player.getOffhandItem().getItem();
		return offHand.getDescriptionId().contains("pickaxe") || offHand.getDescriptionId().contains("axe") || offHand.getDescriptionId().contains("shovel") || offHand.getDescriptionId().contains("hoe");
	}

	@Unique
	private static boolean isPlacingBlock(AbstractClientPlayer player) {
		ItemStack mainHand = player.getMainHandItem();
		ItemStack offHand = player.getOffhandItem();

		boolean hasBlockInMainHand = mainHand.getItem() instanceof BlockItem;
		boolean hasBlockInOffHand = offHand.getItem() instanceof BlockItem;

		return (hasBlockInMainHand || hasBlockInOffHand) && player.isUsingItem();
	}

	@Unique
	private static boolean isBlocking(AbstractClientPlayer player) {
		AtomicBoolean isBlocking = new AtomicBoolean(false);
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			isBlocking.set(data.getStatus().isBlocking());
		});
		return isBlocking.get();
	}


	@Override
	public double getTick(Object o) {
		return ((AbstractClientPlayer) o).tickCount;
	}

	@Unique
	private boolean dragonminez$useAttack2 = false;

	@Unique
	private boolean dragonminez$isPlayingAttack = false;

	@Unique
	private boolean dragonminez$isFlying = false;

	@Unique
	private int dragonminez$dashDirection = 0;

	@Unique
	private boolean dragonminez$isEvading = false;

	@Unique
	private int dragonminez$evasionVariant = 0;

	@Unique
	private int dragonminez$attackAnimTicks = 0;

	@Unique
	private int dragonminez$miningAnimTicks = 0;

	@Unique
	private boolean dragonminez$isShootingKi = false;

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
	public void dragonminez$setFlying(boolean flying) {
		this.dragonminez$isFlying = flying;
	}

	@Override
	public boolean dragonminez$isFlying() {
		return dragonminez$isFlying;
	}

	@Override
	public void dragonminez$triggerDash(int direction) {
		this.dragonminez$dashDirection = direction;
	}

	@Override
	public void dragonminez$triggerEvasion() {
		this.dragonminez$isEvading = true;
		this.dragonminez$evasionVariant = (int) (Math.random() * 4) + 1;
	}

	@Override
	public void dragonminez$setShootingKi(boolean shootingKi) {
		this.dragonminez$isShootingKi = shootingKi;
	}

	@Override
	public boolean dragonminez$isShootingKi() {
		return dragonminez$isShootingKi;
	}

	@Override
	public void dragonminez$triggerCombo(int variant) {
		this.dragonminez$comboVariant = variant;
		this.dragonminez$comboAnimTicks = 10;
	}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
