package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.flight.CombatFlightHandler;
import com.dragonminez.client.flight.FlightOrientationHandler;
import com.dragonminez.client.flight.FlightRollHandler;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.C2S.FlightModeC2S;
import com.dragonminez.common.network.C2S.FlyToggleC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.common.stats.skills.Skill;
import com.dragonminez.common.stats.techniques.TechniqueDispatcher;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.util.GravityLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class FlySkillEvent {
	private static final FlySkillEvent INSTANCE = new FlySkillEvent();

	private static boolean pendingFlightActivation = false;
	private static Vec3 flightVector = Vec3.ZERO;
	private static int verticalHover = 0;
	private static float hovering = 0F;

	private static final float NORMAL_MAX_SPEED = 0.6F;
	private static final float SPRINT_MAX_SPEED = 1.05F;
	private static final float ACCELERATION = 0.055F;
	private static final float DECELERATION = 0.035F;
	private static final float EXIT_DECELERATION = 0.08F;
	private static final float SLOW_DESCENT_RATE = -0.02F;
	private static final float TURN_SPEED_NORMAL = 0.15F;
	private static final float TURN_SPEED_FAST = 0.3F;
	private static final float BASE_ATTRIBUTE_FLY_SPEED = 0.35F;
	private static final float FAST_FLYING_THRESHOLD = 0.55F;
	private static final double MIN_GROUND_CLEARANCE = 0.25D;

	private static int kiConsumptionTicks = 0;
	private static final int KI_CONSUMPTION_INTERVAL = 20;

	private static long pendingGroundActivationStartTime = 0;
	private static boolean pendingGroundActivation = false;

	private static final long DOUBLE_TAP_WINDOW_MS = 250;

	private static boolean wasFlyingSkillActive = false;
	private static boolean pendingFlightDisable = false;
	private static boolean wasSprintingInAir = false;
	private static int lastFlightMode = Status.FLIGHT_SEARCH;

	public static FlySkillEvent getInstance() {
		return INSTANCE;
	}

	@SubscribeEvent
	public static void onKeyPress(InputEvent.Key event) {
		if (KeyBinds.FLY_KEY.consumeClick()) {
			Minecraft mc = Minecraft.getInstance();
			LocalPlayer player = mc.player;

			if (player != null && mc.screen == null) {
				if (KeyBinds.isSecondFunctionDown()) {
					StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
						if (!data.getStatus().isHasCreatedCharacter() || data.getStatus().isStunned()) return;
						Skill flySkill = data.getSkills().getSkill("fly");
						Skill kiControlSkill = data.getSkills().getSkill("kicontrol");
						if (kiControlSkill == null || kiControlSkill.getLevel() <= 0) {
							player.displayClientMessage(Component.translatable("message.dragonminez.flight.no_kicontrol"), true);
							return;
						}
						if (flySkill == null || flySkill.getLevel() <= 0) {
							player.displayClientMessage(Component.translatable("message.dragonminez.flight.no_fly"), true);
							return;
						}
						NetworkHandler.sendToServer(new FlightModeC2S());
						player.playSound(MainSounds.UI_MENU_SWITCH.get(), 0.7F, 1.0F);
					});
					return;
				}

				long currentTime = System.currentTimeMillis();

				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
					if (!data.getStatus().isHasCreatedCharacter() || data.getStatus().isStunned()) return;

					Skill flySkill = data.getSkills().getSkill("fly");
					Skill kiControlSkill = data.getSkills().getSkill("kicontrol");
					boolean flyActive = flySkill != null && flySkill.isActive();

					if (kiControlSkill == null || kiControlSkill.getLevel() <= 0) {
						if (!flyActive) player.displayClientMessage(Component.translatable("message.dragonminez.flight.no_kicontrol"), true);
						return;
					}
					if (flySkill == null || flySkill.getLevel() <= 0) {
						if (!flyActive) player.displayClientMessage(Component.translatable("message.dragonminez.flight.no_fly"), true);
						return;
					}

					if (!flyActive && data.getResources().getPowerRelease() < 5) {
						player.displayClientMessage(Component.translatable("message.dragonminez.flight.low_power_release"), true);
						return;
					}

					int flyLevel = flySkill.getLevel();
					double energyCostPercent = getActivationEnergyPercent(flyLevel);
					int energyCost = (int) Math.ceil(ConfigManager.getCombatConfig().getBaselineFormDrain() * energyCostPercent);

					if (!flySkill.isActive()) {
						if (data.getResources().getCurrentEnergy() < energyCost) return;

						if (player.onGround()) {
							pendingGroundActivation = true;
							pendingGroundActivationStartTime = currentTime;
							return;
						} else NetworkHandler.sendToServer(new FlyToggleC2S(true));
					} else pendingFlightDisable = !pendingFlightDisable;
				});
			}
		}
	}

	public static void toggleFlightFromMenu() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) return;
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> performStandardToggle(player, data));
	}

	public static void performStandardToggle(LocalPlayer player, StatsData data) {
		if (!data.getStatus().isHasCreatedCharacter() || data.getStatus().isStunned()) return;

		Skill flySkill = data.getSkills().getSkill("fly");
		Skill kiControlSkill = data.getSkills().getSkill("kicontrol");
		boolean flyActive = flySkill != null && flySkill.isActive();

		if (kiControlSkill == null || kiControlSkill.getLevel() <= 0) {
			if (!flyActive) player.displayClientMessage(Component.translatable("message.dragonminez.flight.no_kicontrol"), true);
			return;
		}
		if (flySkill == null || flySkill.getLevel() <= 0) {
			if (!flyActive) player.displayClientMessage(Component.translatable("message.dragonminez.flight.no_fly"), true);
			return;
		}
		if (!flyActive && data.getResources().getPowerRelease() < 5) {
			player.displayClientMessage(Component.translatable("message.dragonminez.flight.low_power_release"), true);
			return;
		}

		if (!flySkill.isActive()) {
			int flyLevel = flySkill.getLevel();
			double energyCostPercent = getActivationEnergyPercent(flyLevel);
			int energyCost = (int) Math.ceil(ConfigManager.getCombatConfig().getBaselineFormDrain() * energyCostPercent);
			if (data.getResources().getCurrentEnergy() < energyCost) return;

			if (player.onGround()) {
				pendingGroundActivation = true;
				pendingGroundActivationStartTime = System.currentTimeMillis();
			} else NetworkHandler.sendToServer(new FlyToggleC2S(true));
		} else pendingFlightDisable = !pendingFlightDisable;
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;

		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;

		if (player == null) return;

		boolean startedGroundActivation = false;
		if (pendingGroundActivation) {
			long elapsed = System.currentTimeMillis() - pendingGroundActivationStartTime;
			if (elapsed >= DOUBLE_TAP_WINDOW_MS) {
				player.jumpFromGround();
				Vec3 motion = player.getDeltaMovement();
				player.setDeltaMovement(motion.x, 0.42D, motion.z);
				pendingFlightActivation = true;
				pendingGroundActivation = false;
				startedGroundActivation = true;
			} else if (!player.onGround()) pendingGroundActivation = false;
		}

		if (pendingFlightActivation) {
			if (!startedGroundActivation && player.getDeltaMovement().y < 0 && !player.onGround()) {
				NetworkHandler.sendToServer(new FlyToggleC2S(true));
				pendingFlightActivation = false;
			} else if (!startedGroundActivation && player.onGround()) {
				pendingFlightActivation = false;
			}
		}

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;

			Skill flySkill = data.getSkills().getSkill("fly");
			if (flySkill == null) return;

			boolean isFlying = flySkill.isActive();
			int flightMode = data.getStatus().getFlightMode();
			boolean isCombatFly = flightMode == Status.FLIGHT_COMBAT;

			if (isFlying && !wasFlyingSkillActive) {
				if (isCombatFly) CombatFlightHandler.initFromMotion(player);
				else initializeFlightVectorFromCurrentMotion(player, data.getSkills().getSkillLevel("fly"));
				lastFlightMode = flightMode;
			}

			if (isFlying && flightMode != lastFlightMode) {
				if (isCombatFly) {
					resetFlightState();
					CombatFlightHandler.initFromMotion(player);
				} else {
					CombatFlightHandler.reset();
					initializeFlightVectorFromCurrentMotion(player, data.getSkills().getSkillLevel("fly"));
				}
				player.displayClientMessage(Component.translatable(
						isCombatFly ? "dragonminez.flight.mode.switched.combat" : "dragonminez.flight.mode.switched.search"), true);
				lastFlightMode = flightMode;
			}

			boolean movementRestricted = TechniqueDispatcher.isMovementRestrictedKiAttack(player, data) || data.getStatus().isStunned() || data.getStatus().isActionCharging();

			if (isFlying) {
				if (isCombatFly) {
					if (pendingFlightDisable) {
						pendingFlightDisable = false;
						NetworkHandler.sendToServer(new FlyToggleC2S(false));
						CombatFlightHandler.reset();
						resetFlightState();
						return;
					}
					CombatFlightHandler.handle(player, data, movementRestricted);
				} else handleFlightMovement(player, data.getSkills().getSkillLevel("fly"), movementRestricted);
				handleKiConsumption(player, data, flySkill);
			} else if (!pendingFlightDisable) {
				resetFlightState();
				CombatFlightHandler.reset();
				lastFlightMode = Status.FLIGHT_SEARCH;
			}

			wasFlyingSkillActive = isFlying;
		});
	}

	private static void handleFlightMovement(LocalPlayer player, int flyLevel, boolean movementRestricted) {
		float flySpeedScale = getFlySpeedScale(player);
		float levelMultiplier = 1.0F + (0.20F * flyLevel);
		float maxNormalSpeed = NORMAL_MAX_SPEED * levelMultiplier * flySpeedScale;
		float maxSprintSpeed = SPRINT_MAX_SPEED * levelMultiplier * flySpeedScale;

		Minecraft mc = Minecraft.getInstance();
		boolean isForward = !movementRestricted && mc.options.keyUp.isDown();
		boolean isBack = !movementRestricted && mc.options.keyDown.isDown();
		boolean isLeft = !movementRestricted && mc.options.keyLeft.isDown();
		boolean isRight = !movementRestricted && mc.options.keyRight.isDown();
		boolean isJump = !movementRestricted && mc.options.keyJump.isDown();
		boolean isCrouch = !movementRestricted && mc.options.keyShift.isDown();
		boolean isSprintingInput = !movementRestricted && player.isSprinting();

		boolean hasInput = isForward || isBack || isLeft || isRight;

		boolean isFastFlight = INSTANCE.isFlyingFast(player);
		boolean canSprint = isSprintingInput && isFastFlight;
		float currentMaxSpeed = canSprint ? maxSprintSpeed : maxNormalSpeed;
		float currentAccel = ACCELERATION * flySpeedScale;

		if (canSprint && !wasSprintingInAir) {
			player.playSound(MainSounds.TRANSFORM_ON.get(), 0.7F, 1.2F);
		}
		wasSprintingInAir = canSprint;

		if (!isFastFlight) FlightOrientationHandler.reset();

		Vec3 lookDir = isFastFlight ? FlightOrientationHandler.getForwardVector(player) : player.getLookAngle();
		Vec3 targetDirection = Vec3.ZERO;

		if (isForward) targetDirection = targetDirection.add(lookDir);
		if (isBack) targetDirection = targetDirection.add(lookDir.scale(-0.5));
		if (isLeft) {
			Vec3 leftDir = lookDir.yRot((float) Math.toRadians(90)).normalize();
			targetDirection = targetDirection.add(new Vec3(leftDir.x, 0, leftDir.z).scale(0.7));
		}
		if (isRight) {
			Vec3 rightDir = lookDir.yRot((float) Math.toRadians(-90)).normalize();
			targetDirection = targetDirection.add(new Vec3(rightDir.x, 0, rightDir.z).scale(0.7));
		}

		double currentSpeed = flightVector.length();

		if (pendingFlightDisable) {
			double newSpeed = Math.max(0.0, currentSpeed - (EXIT_DECELERATION * flySpeedScale));
			Vec3 normalized = currentSpeed > 0.0001 ? flightVector.normalize() : Vec3.ZERO;
			flightVector = normalized.scale(newSpeed);

			if (flightVector.lengthSqr() < 0.03) {
				pendingFlightDisable = false;
				NetworkHandler.sendToServer(new FlyToggleC2S(false));
				resetFlightState();
				return;
			}
		} else if (hasInput && targetDirection.length() > 0.001) {
			targetDirection = targetDirection.normalize();
			if (canSprint) {
				Vec3 targetVelocity = targetDirection.scale(maxSprintSpeed);
				flightVector = targetVelocity;
			} else {
				double minSpeed = ConfigManager.getCombatConfig().getCombatFlyBaseSpeed() * levelMultiplier * flySpeedScale;
				double targetSpeed = Math.max(minSpeed, Math.min(currentSpeed + currentAccel, currentMaxSpeed));
				Vec3 targetVelocity = targetDirection.scale(targetSpeed);
				float turnSpeed = isFastFlight ? TURN_SPEED_FAST : TURN_SPEED_NORMAL;
				flightVector = new Vec3(Mth.lerp(turnSpeed, flightVector.x, targetVelocity.x), Mth.lerp(turnSpeed, flightVector.y, targetVelocity.y), Mth.lerp(turnSpeed, flightVector.z, targetVelocity.z));
			}
			hovering = Math.min(1F, hovering + 0.1F);
		} else {
			if (currentSpeed > 0.01) {
				double newSpeed = Math.max(0, currentSpeed - DECELERATION);
				if (currentSpeed > 0.001) flightVector = flightVector.normalize().scale(newSpeed);
				else flightVector = Vec3.ZERO;
			} else flightVector = Vec3.ZERO;
		}

		FlightRollHandler.tick();

		if (GravityLogic.isFlightHardStopped(player)) {
			flightVector = Vec3.ZERO;
			player.setDeltaMovement(0, -1.5, 0);
		} else {
			double flyFactor = GravityLogic.getFlyFactor(player);
			if (flyFactor < 1.0) flightVector = flightVector.scale(flyFactor);
		}

		if (flightVector.length() > 0.01 || pendingFlightDisable) {
			player.setDeltaMovement(flightVector);
			player.fallDistance = 0F;
			verticalHover = 0;
		} else handleHovering(player, isJump, isCrouch);

		if (player.onGround() && !pendingFlightActivation) {
			pendingFlightDisable = false;
			NetworkHandler.sendToServer(new FlyToggleC2S(false));
			resetFlightState();
		}
	}

	private static float getFlySpeedScale(LocalPlayer player) {
		double attrValue = player.getAttributes().hasAttribute(EntityAttributes.FLY_SPEED.get()) ? player.getAttributeValue(EntityAttributes.FLY_SPEED.get()) : 0.0;
		if (attrValue <= 0.0) return 1.0F;
		double scale = attrValue / BASE_ATTRIBUTE_FLY_SPEED;
		return (float) Mth.clamp(scale, 0.25, 4.0);
	}

	private static void initializeFlightVectorFromCurrentMotion(LocalPlayer player, int flyLevel) {
		float speedScale = getFlySpeedScale(player);
		float levelMultiplier = 1.0F + (0.20F * flyLevel);
		float maxNormalSpeed = NORMAL_MAX_SPEED * levelMultiplier * speedScale;
		float maxSprintSpeed = SPRINT_MAX_SPEED * levelMultiplier * speedScale;
		float capSpeed = Math.max(maxNormalSpeed, maxSprintSpeed);

		Vec3 currentMotion = player.getDeltaMovement();
		if (currentMotion.lengthSqr() < 1.0E-5) {
			flightVector = player.getLookAngle().scale(maxNormalSpeed * 0.35F);
			return;
		}

		double clamped = Math.min(currentMotion.length(), capSpeed);
		flightVector = currentMotion.normalize().scale(clamped);
	}

	private static void handleHovering(LocalPlayer player, boolean isJump, boolean isCrouch) {
		if (isJump) {
			if (verticalHover < 20) verticalHover = Mth.clamp(verticalHover + 1, -20, 20);
		} else if (isCrouch) {
			if (verticalHover > -20) verticalHover = Mth.clamp(verticalHover - 1, -20, 20);
		} else {
			if (verticalHover > -3) verticalHover = Mth.clamp(verticalHover - 1, -3, 20);
			else if (verticalHover < -3) verticalHover = Mth.clamp(verticalHover + 1, -20, -3);
		}

		double yMovement;
		if (verticalHover >= -3 && verticalHover <= 0 && !isJump && !isCrouch) yMovement = SLOW_DESCENT_RATE + (Math.sin(player.tickCount / 10F) / 200F);
		else if (verticalHover == 0) yMovement = Math.sin(player.tickCount / 10F) / 100F;
		else yMovement = verticalHover / 60D;

		if (!isJump && !isCrouch && getGroundDistance(player) <= MIN_GROUND_CLEARANCE) {
			yMovement = Math.max(0.0D, yMovement);
		}

		player.setDeltaMovement(new Vec3(
				player.getDeltaMovement().x * 0.9,
				yMovement,
				player.getDeltaMovement().z * 0.9
		));
		player.fallDistance = 0F;

		if (hovering < 1F) hovering = Math.min(1F, hovering + 0.1F);
	}

	private static void handleKiConsumption(LocalPlayer player, StatsData data, Skill flySkill) {
		kiConsumptionTicks++;
		if (kiConsumptionTicks >= KI_CONSUMPTION_INTERVAL) {
			kiConsumptionTicks = 0;

			int flyLevel = flySkill.getLevel();
			float maxEnergy = data.getMaxEnergy();

			boolean isFastFlight = INSTANCE.isFlyingFast(player);
			boolean isSprintFlight = player.isSprinting() && isFastFlight;
			double basePercent = 0.03;
			double energyCostPercent = Math.max(0.002, basePercent - (flyLevel * 0.005));
			energyCostPercent *= getFlyCostMultiplier(flyLevel);
			if (isSprintFlight) energyCostPercent *= 2.0;
			int energyCost = (int) Math.ceil(maxEnergy * energyCostPercent);

			if (data.getResources().getCurrentEnergy() <= energyCost) {
				NetworkHandler.sendToServer(new FlyToggleC2S(false));
				resetFlightState();
			}
		}
	}

	private static void resetFlightState() {
		flightVector = Vec3.ZERO;
		verticalHover = 0;
		hovering = 0F;
		kiConsumptionTicks = 0;
		pendingFlightDisable = false;
		wasFlyingSkillActive = false;
		wasSprintingInAir = false;
		FlightRollHandler.reset();
		FlightOrientationHandler.reset();
	}

	private boolean isFlyingFast() {
		return flightVector.length() > FAST_FLYING_THRESHOLD;
	}

	public boolean isFlyingFast(AbstractClientPlayer player) {
		if (player == null) return false;

		LocalPlayer localPlayer = Minecraft.getInstance().player;
		if (localPlayer != null && player == localPlayer) return isFlyingFast();

		boolean flyActive = StatsProvider.get(StatsCapability.INSTANCE, player)
				.map(data -> {
					Skill flySkill = data.getSkills().getSkill("fly");
					return flySkill != null && flySkill.isActive() && data.getStatus().getFlightMode() != Status.FLIGHT_COMBAT;
				}).orElse(false);

		return flyActive && player.getDeltaMovement().lengthSqr() > (FAST_FLYING_THRESHOLD * FAST_FLYING_THRESHOLD);
	}

	private static double getActivationEnergyPercent(int flyLevel) {
		double basePercent = Math.max(0.01, 0.04 - (flyLevel * 0.003));
		return basePercent * getFlyCostMultiplier(flyLevel);
	}

	private static float getFlyCostMultiplier(int flyLevel) {
		int clampedLevel = Mth.clamp(flyLevel, 1, 10);
		float t = (clampedLevel - 1) / (float) (10 - 1);
		return Mth.lerp(t, 4.0F, 1.0F);
	}

	private static double getGroundDistance(LocalPlayer player) {
		Vec3 start = new Vec3(player.getX(), player.getBoundingBox().minY, player.getZ());
		Vec3 end = start.add(0.0D, -1.5D, 0.0D);
		HitResult hit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (hit.getType() == HitResult.Type.MISS) return Double.MAX_VALUE;
		return start.y - hit.getLocation().y;
	}
}