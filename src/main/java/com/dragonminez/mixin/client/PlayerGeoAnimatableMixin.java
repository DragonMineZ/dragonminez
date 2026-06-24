package com.dragonminez.mixin.client;

import com.dragonminez.client.animation.AnimationCache;
import com.dragonminez.client.animation.IPlayerAnimatable;
import com.dragonminez.client.animation.CombatAnimationResolver;
import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import com.dragonminez.common.combat.logic.player.PlayerAttackProperties;
import com.dragonminez.common.combat.player.AttackHand;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import static com.dragonminez.client.animation.BaseAnimations.*;

@Mixin(AbstractClientPlayer.class)
public abstract class PlayerGeoAnimatableMixin implements GeoAnimatable, IPlayerAnimatable {

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	@Unique private double dragonminez$lastPosX = Double.NaN;
	@Unique private double dragonminez$lastPosZ = Double.NaN;
	@Unique private int dragonminez$stoppedTicks = 0;
	@Unique private boolean dragonminez$isMovingState = false;
	@Unique private int dragonminez$lastTickCount = -1;
	@Unique private static final int STOPPED_THRESHOLD_TICKS = 3;
	@Unique private double dragonminez$horizSpeed = 0.0;
	@Unique private boolean dragonminez$wasAirborne = false;
	@Unique private float dragonminez$maxFallDistance = 0.0F;
	@Unique private boolean dragonminez$landingTriggered = false;
	@Unique private int dragonminez$landingTicks = 0;
	@Unique private boolean dragonminez$wasEating = false;
	@Unique private static final double WALK_SPEED_BASELINE = 0.2158;
	@Unique private static final double RUN_SPEED_BASELINE = 0.2806;
	@Unique private static final int LANDING_ANIM_TICKS = 13;
	@Unique private static final float FALL_TRIGGER_DISTANCE = 2.0F;
	@Unique private static final double LANDING_LEAD_TICKS = 7.0;
	@Unique private static final double LANDING_RAY_DISTANCE = 7.0;
	@Unique private int dragonminez$lastDashTickRun = -1;
	@Unique private int dragonminez$dashAnimTicks = 0;
	@Unique private int dragonminez$lastAttackTickRun = -1;
	@Unique private boolean dragonminez$isFlying = false;
	@Unique private int dragonminez$dashDirection = 0;
	@Unique private boolean dragonminez$isEvading = false;
	@Unique private int dragonminez$evasionVariant = 0;
	@Unique private int dragonminez$attackAnimTicks = 0;
	@Unique private int dragonminez$combatGraceFrames = 0;
	@Unique private boolean dragonminez$isOffhandAttack = false;
	@Unique private int dragonminez$miningAnimTicks = 0;
	@Unique private int dragonminez$lastMiningTickRun = -1;
	@Unique private boolean dragonminez$isShootingKi = false;
	@Unique private String dragonminez$currentMeleeAnim = null;
	@Unique private String dragonminez$currentPoseAnim = null;
	@Unique private float dragonminez$currentMeleeSpeed = 1.0F;

	@Unique private String dragonminez$currentKiAnim = null;
	@Unique private String dragonminez$lastKiAnim = null;
	@Unique private boolean dragonminez$kiAnimHold = true;
	@Unique private int dragonminez$kiAnimTicks = 0;
	@Unique private int dragonminez$lastKiTickRun = -1;
	@Unique private String dragonminez$lastKiCtlAnim = null;
	@Unique private static final int KI_FIRE_ONESHOT_TICKS = 14;

	@Unique
	private boolean dragonminez$isActuallyMoving(AbstractClientPlayer player) {
		int currentTick = player.tickCount;

		if (currentTick == dragonminez$lastTickCount) return dragonminez$isMovingState;
		dragonminez$lastTickCount = currentTick;

		if (Double.isNaN(dragonminez$lastPosX)) {
			dragonminez$lastPosX = player.getX();
			dragonminez$lastPosZ = player.getZ();
			dragonminez$wasAirborne = !player.onGround();
			return false;
		}

		double deltaX = player.getX() - dragonminez$lastPosX;
		double deltaZ = player.getZ() - dragonminez$lastPosZ;
		double distanceSq = deltaX * deltaX + deltaZ * deltaZ;

		dragonminez$lastPosX = player.getX();
		dragonminez$lastPosZ = player.getZ();
		dragonminez$horizSpeed = Math.sqrt(distanceSq);

		dragonminez$updateLanding(player);

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

	@Unique
	private void dragonminez$updateLanding(AbstractClientPlayer player) {
		boolean onGround = player.onGround();

		if (!onGround) {
			dragonminez$maxFallDistance = Math.max(dragonminez$maxFallDistance, player.fallDistance);
			double descentSpeed = -player.getDeltaMovement().y;
			if (!dragonminez$landingTriggered
					&& dragonminez$maxFallDistance >= FALL_TRIGGER_DISTANCE
					&& descentSpeed > 0.1
					&& dragonminez$groundClearance(player) / descentSpeed <= LANDING_LEAD_TICKS) {
				dragonminez$landingTicks = LANDING_ANIM_TICKS;
				dragonminez$landingTriggered = true;
			}
		} else {
			dragonminez$maxFallDistance = 0.0F;
			dragonminez$landingTriggered = false;
		}

		dragonminez$wasAirborne = !onGround;
		if (dragonminez$landingTicks > 0) dragonminez$landingTicks--;
	}

	@Unique
	private double dragonminez$groundClearance(AbstractClientPlayer player) {
		Vec3 start = new Vec3(player.getX(), player.getBoundingBox().minY, player.getZ());
		Vec3 end = start.add(0.0, -LANDING_RAY_DISTANCE, 0.0);
		HitResult hit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (hit.getType() == HitResult.Type.MISS) return Double.MAX_VALUE;
		return start.y - hit.getLocation().y;
	}

	@Unique
	private float dragonminez$movementSpeedFactor(double baseline) {
		return (float) Mth.clamp(dragonminez$horizSpeed / baseline, 0.6, 1.8);
	}

	@Unique
	private RawAnimation dragonminez$resolveFlyAnimation(AbstractClientPlayer player) {
		Minecraft mc = Minecraft.getInstance();

		if (player == mc.player) {
			boolean forward = mc.options.keyUp.isDown();
			boolean back = mc.options.keyDown.isDown();
			boolean left = mc.options.keyLeft.isDown();
			boolean right = mc.options.keyRight.isDown();

			if (forward) return FLY_FRONT;
			if (back) return FLY_BACK;
			if (left) return FLY_LEFT;
			if (right) return FLY_RIGHT;
			return FLY_IDLE;
		}

		Vec3 motion = player.getDeltaMovement();
		double horizontal = motion.horizontalDistance();
		if (horizontal < 0.04) return FLY_IDLE;

		float yawRad = player.yBodyRot * Mth.DEG_TO_RAD;
		double sin = Mth.sin(yawRad);
		double cos = Mth.cos(yawRad);
		double forwardComp = -motion.x * sin + motion.z * cos;
		double rightComp = -motion.x * cos - motion.z * sin;

		if (Math.abs(forwardComp) >= Math.abs(rightComp)) {
			return forwardComp >= 0 ? FLY_FRONT : FLY_BACK;
		}
		return rightComp >= 0 ? FLY_RIGHT : FLY_LEFT;
	}

	@Unique
	private static boolean dragonminez$isEatingFood(AbstractClientPlayer player) {
		if (!player.isUsingItem()) return false;
		UseAnim anim = player.getUseItem().getUseAnimation();
		return anim == UseAnim.EAT || anim == UseAnim.DRINK;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
		registrar.add(new AnimationController<>(this, "controller", 4, this::predicate));
		registrar.add(new AnimationController<>(this, "attack_controller", 0, this::attackPredicate));
		registrar.add(new AnimationController<>(this, "mining_controller", 0, this::miningPredicate));
		registrar.add(new AnimationController<>(this, "block_controller", 3, this::blockPredicate));
		registrar.add(new AnimationController<>(this, "shield_controller", 3, this::shieldPredicate));
		registrar.add(new AnimationController<>(this, "tailcontroller", 0, this::tailpredicate));
		registrar.add(new AnimationController<>(this, "dash_controller", 0, this::dashPredicate));
		registrar.add(new AnimationController<>(this, "pose_controller", 4, this::posePredicate));
		registrar.add(new AnimationController<>(this, "ki_controller", 4, this::kiPredicate));
		registrar.add(new AnimationController<>(this, "eat_controller", 3, this::eatPredicate));
	}

	@Unique
	private <T extends GeoAnimatable> PlayState eatPredicate(AnimationState<T> state) {
		AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
		AnimationController<T> ctl = state.getController();

		if (dragonminez$isEatingFood(player)) {
			if (!dragonminez$wasEating) {
				ctl.setAnimation(EAT);
				ctl.forceAnimationReset();
				dragonminez$wasEating = true;
			}
			return PlayState.CONTINUE;
		}

		dragonminez$wasEating = false;
		return PlayState.STOP;
	}

	@Unique
	private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> state) {
		AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;

		state.getController().setAnimationSpeed(1.0D);

		if (dragonminez$currentKiAnim != null && dragonminez$kiAnimHold) {
			if (!dragonminez$currentKiAnim.equals(dragonminez$lastKiAnim)) {
				state.getController().setAnimation(AnimationCache.getPlayAndHold(dragonminez$currentKiAnim));
				state.getController().forceAnimationReset();
				dragonminez$lastKiAnim = dragonminez$currentKiAnim;
			}
			return PlayState.CONTINUE;
		}
		dragonminez$lastKiAnim = null;

		IPlayerAnimatable animatable = (IPlayerAnimatable) this;
		if (dragonminez$dashAnimTicks > 0) return PlayState.STOP;

		boolean isMoving = dragonminez$isActuallyMoving(player);
		dragonminez$currentPoseAnim = CombatAnimationResolver.resolvePlayerPose(player);

		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (data == null) return state.setAndContinue(IDLE);

		boolean isDraining = data.getCooldowns().hasCooldown(Cooldowns.DRAIN_ACTIVE);
		boolean flySkillActive = data.getSkills().isSkillActive("fly");
		boolean isChargingKi = data.getStatus().isChargingKi();
		boolean isBlocking = data.getStatus().isBlocking();
		boolean isKnockedDown = data.getStatus().isKnockedDown();
		boolean isOozaru = data.getCharacter().isOozaruCached();
		var nextFormConfig = TransformationsHelper.getNextAvailableForm(data);
		var nextStackFormConfig = TransformationsHelper.getNextAvailableStackForm(data);
		String nextForm = nextFormConfig != null ? nextFormConfig.getName().toLowerCase() : "";
		boolean isTransforming = data.getStatus().isActionCharging();
		ActionMode actionMode = data.getStatus().getSelectedAction();

		if (isKnockedDown) return state.setAndContinue(KNOCKBACK_HORIZONTAL);

		if (isDraining) return state.setAndContinue(DRAIN);

		if (player.isPassenger()) return state.setAndContinue(SIT);

		if (isChargingKi && !isMoving && !isBlocking) return state.setAndContinue(KI_CHARGE);

		if (isTransforming && actionMode.equals(ActionMode.FORM)) {
			if (nextFormConfig != null && nextFormConfig.hasTransformationAnimation()) {
				return state.setAndContinue(AnimationCache.getPlay(nextFormConfig.getTransformationAnimation()));
			}
			if (nextForm.contains("ozaru")) return state.setAndContinue(OOZARU_TRANSFORMATION);
			return state.setAndContinue(TRANSFORMATION);
		} else if (isTransforming && actionMode.equals(ActionMode.STACK)) {
			if (nextStackFormConfig != null && nextStackFormConfig.hasTransformationAnimation()) {
				return state.setAndContinue(AnimationCache.getPlay(nextStackFormConfig.getTransformationAnimation()));
			}
			return state.setAndContinue(TRANSFORMATION);
		} else if (isTransforming && actionMode.equals(ActionMode.RACIAL)) {
			return state.setAndContinue(ABSORB);
		} else if (isTransforming) {
			return state.setAndContinue(TRANSFORMATION);
		}


		if (player.isSwimming()) return state.setAndContinue(SWIMMING);

		if (player.isVisuallyCrawling()) {
			if (isMoving) return state.setAndContinue(CRAWLING_MOVE);
			return state.setAndContinue(CRAWLING);
		}

		if (flySkillActive || player.isFallFlying() || animatable.dragonminez$isFlying() || player.getAbilities().flying) {
			if (FlySkillEvent.getInstance().isFlyingFast(player)) return state.setAndContinue(FLY_FAST);
			return state.setAndContinue(dragonminez$resolveFlyAnimation(player));
		}

		if (player.onClimbable() && !player.onGround()) return state.setAndContinue(CLIMB);

		if (dragonminez$landingTicks > 0 && !player.isCrouching()) return state.setAndContinue(LANDING);

		if (player.onGround()) {
			if (player.isCrouching()) {
				if (isMoving) return state.setAndContinue(CROUCHING_WALK);
				return state.setAndContinue(CROUCHING);
			} else if (isMoving && player.isSprinting()) {
				state.getController().setAnimationSpeed(dragonminez$movementSpeedFactor(RUN_SPEED_BASELINE));
				return state.setAndContinue(RUN);
			} else if (isMoving) {
				state.getController().setAnimationSpeed(dragonminez$movementSpeedFactor(WALK_SPEED_BASELINE));
				if (isOozaru) return state.setAndContinue(WALK_OOZARU);
				return state.setAndContinue(WALK);
			} else {
				if (isOozaru) return state.setAndContinue(IDLE_OOZARU);
				return state.setAndContinue(IDLE);
			}
		}

		if (player.getDeltaMovement().y < -0.08) return state.setAndContinue(FLY_IDLE);

		return state.setAndContinue(JUMP);
	}

	@Unique
	private <T extends GeoAnimatable> PlayState kiPredicate(AnimationState<T> state) {
		AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;

		if (dragonminez$currentKiAnim == null || dragonminez$kiAnimHold) {
			dragonminez$lastKiCtlAnim = null;
			return PlayState.STOP;
		}

		AnimationController<T> ctl = state.getController();
		boolean isFire = dragonminez$currentKiAnim.endsWith("_fire");

		if (!isFire) {
			if (!dragonminez$currentKiAnim.equals(dragonminez$lastKiCtlAnim)) {
				ctl.setAnimation(AnimationCache.getPlayAndHold(dragonminez$currentKiAnim));
				ctl.forceAnimationReset();
				dragonminez$lastKiCtlAnim = dragonminez$currentKiAnim;
			}
			return PlayState.CONTINUE;
		}

		if (!dragonminez$currentKiAnim.equals(dragonminez$lastKiCtlAnim)) {
			ctl.setAnimation(AnimationCache.getPlay(dragonminez$currentKiAnim));
			ctl.forceAnimationReset();
			dragonminez$lastKiCtlAnim = dragonminez$currentKiAnim;
			dragonminez$kiAnimTicks = KI_FIRE_ONESHOT_TICKS;
		}
		if (player.tickCount != dragonminez$lastKiTickRun) {
			dragonminez$lastKiTickRun = player.tickCount;
			if (dragonminez$kiAnimTicks > 0) dragonminez$kiAnimTicks--;
		}
		if (dragonminez$kiAnimTicks > 0) return PlayState.CONTINUE;

		dragonminez$currentKiAnim = null;
		dragonminez$lastKiCtlAnim = null;
		return PlayState.STOP;
	}

	@Unique
	private <T extends GeoAnimatable> PlayState posePredicate(AnimationState<T> state) {
		AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;

		if (dragonminez$attackAnimTicks > 0) return PlayState.STOP;
		if (dragonminez$dashAnimTicks > 0) return PlayState.STOP;
		if (dragonminez$isShootingKi) return PlayState.STOP;
		if (dragonminez$currentKiAnim != null) return PlayState.STOP;

		if (player.isSwimming() || player.isVisuallyCrawling() || player.isPassenger()) return PlayState.STOP;

		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (data != null && (data.getStatus().isBlocking() || data.getStatus().isChargingKi())) return PlayState.STOP;

		if (dragonminez$currentPoseAnim == null || dragonminez$currentPoseAnim.isEmpty()) return PlayState.STOP;

		return state.setAndContinue(AnimationCache.getLoop(dragonminez$currentPoseAnim));
	}

	@Unique
	private <T extends GeoAnimatable> PlayState tailpredicate(AnimationState<T> state) {
		AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;

		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (data == null) return PlayState.STOP;

		String race = data.getCharacter().getRaceName().toLowerCase();
		if (race.equals("bioandroid") && data.getCooldowns().hasCooldown(Cooldowns.DRAIN_ACTIVE)) return PlayState.STOP;

		state.getController().setAnimation(TAIL);
		return PlayState.CONTINUE;
	}

	@Unique
	private <T extends GeoAnimatable> PlayState attackPredicate(AnimationState<T> state) {
		AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
		if (player.isSleeping()) {
			dragonminez$attackAnimTicks = 0;
			dragonminez$currentMeleeAnim = null;
			return PlayState.STOP;
		}
		if (player.tickCount != dragonminez$lastAttackTickRun) {
			dragonminez$lastAttackTickRun = player.tickCount;
			if (dragonminez$attackAnimTicks > 0) dragonminez$attackAnimTicks--;
		}

		AnimationController<T> ctl = state.getController();
		boolean hasSwingFrame = player.attackAnim > 0.0F || player.swinging || player.swingTime > 0;
		if (dragonminez$currentMeleeAnim != null) {

			ctl.setAnimationSpeed(dragonminez$currentMeleeSpeed);

			if (!dragonminez$currentMeleeAnim.equals("fallback")) {
				ctl.setAnimation(AnimationCache.getPlay(dragonminez$currentMeleeAnim));
			} else ctl.setAnimation(ATTACK);

			ctl.forceAnimationReset();
			dragonminez$currentMeleeAnim = null;
			dragonminez$attackAnimTicks = Math.max(8, Math.round(12.0F / Math.max(dragonminez$currentMeleeSpeed, 0.1F)));
			return PlayState.CONTINUE;
		}

		if (dragonminez$attackAnimTicks <= 0 && hasSwingFrame && (shouldPlayUnarmedAttack(player) || shouldPlayGenericAttack(player))) {
			ctl.setAnimationSpeed(1.0D);
			ctl.setAnimation(MINING1);
			ctl.forceAnimationReset();
			dragonminez$attackAnimTicks = 8;
			return PlayState.CONTINUE;
		}

		if (dragonminez$attackAnimTicks > 0) return PlayState.CONTINUE;

		ctl.setAnimationSpeed(1.0D);
		return PlayState.STOP;
	}

	@Unique
	private <T extends GeoAnimatable> PlayState miningPredicate(AnimationState<T> state) {
		AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
		AnimationController<T> ctl = state.getController();
		if (player.isSleeping() || dragonminez$isEatingFood(player)) {
			dragonminez$miningAnimTicks = 0;
			return PlayState.STOP;
		}
		boolean hasMiningSwing = player.attackAnim > 0.0F || player.swinging || player.swingTime > 0;

		if (player.tickCount != dragonminez$lastMiningTickRun) {
			dragonminez$lastMiningTickRun = player.tickCount;
			if (dragonminez$miningAnimTicks > 0) dragonminez$miningAnimTicks--;
		}

		if (isPlacingBlock(player) && !isBlocking(player)) {
			if (ctl.getAnimationState() == AnimationController.State.STOPPED) {
				RawAnimation placeAnim = isMainHandBlock(player) ? ATTACK : ATTACK2;
				ctl.setAnimation(placeAnim);
				ctl.forceAnimationReset();
			}
			dragonminez$miningAnimTicks = 8;
			return PlayState.CONTINUE;
		}

		if (hasMiningSwing && isUsingTool(player) && !isPlacingBlock(player) && !isBlocking(player)) {
			if (ctl.getAnimationState() == AnimationController.State.STOPPED) {
				if (isMainHandTool(player)) {
					ctl.setAnimation(MINING1);
				} else if (isOffHandTool(player)) {
					ctl.setAnimation(MINING2);
				}
				ctl.forceAnimationReset();
			}
			dragonminez$miningAnimTicks = 10;
			return PlayState.CONTINUE;
		}

		if (dragonminez$miningAnimTicks > 0) {
			return PlayState.CONTINUE;
		}

		return PlayState.STOP;
	}

	@Unique
	private <T extends GeoAnimatable> PlayState blockPredicate(AnimationState<T> state) {
		AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
		AnimationController<T> ctl = state.getController();

		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (data != null && data.getStatus().isBlocking()) {
			if (ctl.getAnimationState() == AnimationController.State.STOPPED || !ctl.getCurrentRawAnimation().equals(BLOCK)) {
				ctl.setAnimation(BLOCK);
				ctl.forceAnimationReset();
			}
			return PlayState.CONTINUE;
		}
		return PlayState.STOP;
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
				case 1 -> EVASION_FRONT;
				case 2 -> EVASION_BACK;
				case 3 -> EVASION_LEFT;
				default -> EVASION_RIGHT;
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
	private static boolean isMainHandBlock(AbstractClientPlayer player) {
		return player.getMainHandItem().getItem() instanceof BlockItem;
	}

	@Unique
	private static boolean isPlacingBlock(AbstractClientPlayer player) {
		ItemStack mainHand = player.getMainHandItem();
		ItemStack offHand = player.getOffhandItem();

		boolean hasBlockInMainHand = mainHand.getItem() instanceof BlockItem;
		boolean hasBlockInOffHand = offHand.getItem() instanceof BlockItem;
		boolean hasPlaceActionFrame = (player.isUsingItem() && !dragonminez$isEatingFood(player)) || player.swinging || player.swingTime > 0;

		return (hasBlockInMainHand || hasBlockInOffHand) && hasPlaceActionFrame;
	}

	@Unique
	private static boolean isBlocking(AbstractClientPlayer player) {
		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		return data != null && data.getStatus().isBlocking();
	}

	@Unique
	private static boolean shouldPlayUnarmedAttack(AbstractClientPlayer player) {
		return player.getMainHandItem().isEmpty()
				&& player.getOffhandItem().isEmpty()
				&& !player.isUsingItem()
				&& !isBlocking(player)
				&& !isPlacingBlock(player)
				&& !isUsingTool(player);
	}

	@Unique
	private static boolean shouldPlayGenericAttack(AbstractClientPlayer player) {
		if (player.isUsingItem()) return false;
		if (isBlocking(player) || isPlacingBlock(player) || isUsingTool(player)) return false;
		if (player instanceof PlayerAttackProperties props) {
			AttackHand hand = PlayerAttackHelper.getCurrentAttack(player, props.getComboCount());
			if (hand != null) return false;
		}
		return true;
	}

	@Override
	public double getTick(Object o) {
		return ((AbstractClientPlayer) o).tickCount;
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
	public void dragonminez$playMeleeAnimation(String animationName, boolean isOffhand, float speedMultiplier) {
		AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
		boolean isLocal = self == Minecraft.getInstance().player;
		boolean vanillaFirstPerson = Minecraft.getInstance().options.getCameraType().isFirstPerson() && !FirstPersonManager.shouldRenderFirstPerson(self);
		if (isLocal && vanillaFirstPerson) {
			this.dragonminez$currentMeleeAnim = null;
			this.dragonminez$isOffhandAttack = isOffhand;
			return;
		}

		String resolved = CombatAnimationResolver.resolveAttack(animationName, isOffhand);
		this.dragonminez$currentMeleeAnim = resolved.isEmpty() ? "fallback" : resolved;
		this.dragonminez$currentMeleeSpeed = Math.max(0.15F, speedMultiplier);
		this.dragonminez$isOffhandAttack = isOffhand;
	}

	@Override
	public boolean dragonminez$isPlayingCombatAnimation() {
		if (dragonminez$attackAnimTicks > 0) {
			dragonminez$combatGraceFrames = 8;
			return true;
		}
		if (dragonminez$combatGraceFrames > 0) {
			dragonminez$combatGraceFrames--;
			return true;
		}
		return dragonminez$currentPoseAnim != null && !dragonminez$currentPoseAnim.isEmpty();
	}

	@Override
	public boolean dragonminez$isAttackingWithOffhand() {
		return this.dragonminez$isOffhandAttack;
	}

	@Override
	public float dragonminez$getCombatPlacementWeight() {
		if (dragonminez$attackAnimTicks > 0) return 1.0F;
		if (dragonminez$combatGraceFrames > 0) return Math.min(1.0F, dragonminez$combatGraceFrames / 8.0F);
		return 0.0F;
	}

	@Override
	public void dragonminez$playKiAnimation(String animationName, boolean hold) {
		this.dragonminez$currentKiAnim = animationName;
		this.dragonminez$kiAnimHold = hold;
		this.dragonminez$kiAnimTicks = 0;
	}

	@Override
	public void dragonminez$stopKiAnimation() {
		this.dragonminez$currentKiAnim = null;
		this.dragonminez$lastKiAnim = null;
		this.dragonminez$lastKiCtlAnim = null;
		this.dragonminez$kiAnimHold = true;
		this.dragonminez$kiAnimTicks = 0;
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}

	@Override
	public String dragonminez$getCurrentPlayingAnimation() {
		long instanceId = ((AbstractClientPlayer) (Object) this).getId();
		var manager = this.geoCache.getManagerForId(instanceId);
		if (manager != null) {
			AnimationController<?> controller = manager.getAnimationControllers().get("controller");
			if (controller != null && controller.getCurrentAnimation() != null) {
				return controller.getCurrentAnimation().animation().name();
			}
		}
		return "";
	}
}