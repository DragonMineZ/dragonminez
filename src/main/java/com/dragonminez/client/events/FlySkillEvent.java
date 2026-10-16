package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.clash.ClientBeamClashState;
import com.dragonminez.client.flight.CombatFlightHandler;
import com.dragonminez.client.flight.FlightOrientationHandler;
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
import com.dragonminez.common.stats.techniques.TechniqueDispatcher;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.client.render.shader.ClientGravityState;
import com.dragonminez.server.util.GravityLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class FlySkillEvent {
	private static final FlySkillEvent INSTANCE = new FlySkillEvent();

	private static boolean pendingFlightActivation = false;
	private static Vec3 flightVector = Vec3.ZERO;
	private static int verticalHover = 0;
	private static float hovering = 0F;

	private static final float SEARCH_SPEED_SCALE = 0.82F;
	private static final float BASE_SPRINT_SPEED = 1.05F;
	private static final float NORMAL_MAX_SPEED = 0.6F * SEARCH_SPEED_SCALE;
	private static final float SPRINT_MAX_SPEED = BASE_SPRINT_SPEED * SEARCH_SPEED_SCALE;
	private static final float ACCELERATION = 0.055F;
	private static final float DECELERATION = 0.035F;
	private static final float EXIT_DECELERATION = 0.08F;
	private static final float SLOW_DESCENT_RATE = -0.02F;
	private static final float TURN_SPEED_NORMAL = 0.15F;
	private static final float TURN_SPEED_FAST = 0.6F;
	private static final double TURN_REVERSAL_DOT = -0.2D;
	private static final float BASE_ATTRIBUTE_FLY_SPEED = 0.35F;
	private static final float FAST_FLYING_THRESHOLD = 0.55F * SEARCH_SPEED_SCALE;
	private static final float FAST_FLYING_EXIT_THRESHOLD = 0.4F * SEARCH_SPEED_SCALE;
	private static final double REMOTE_TELEPORT_STEP = 32.0D;
	private static final double REMOTE_SPEED_SMOOTHING = 0.2D;
	private static final Map<AbstractClientPlayer, RemoteFlight> REMOTE_FLIGHT = new WeakHashMap<>();
	private static final double MIN_GROUND_CLEARANCE = 0.25D;

	private static long pendingGroundActivationStartTime = 0;
	private static boolean pendingGroundActivation = false;

	private static final long DOUBLE_TAP_WINDOW_MS = 250;

	private static boolean wasFlyingSkillActive = false;
	private static boolean pendingFlightDisable = false;
	private static boolean wasSprintingInAir = false;
	private static boolean fastFlight = false;
	private static boolean wasStrafingLeft = false;
	private static boolean wasStrafingRight = false;
	private static long lastLeftTapTime = 0;
	private static long lastRightTapTime = 0;
	private static int lastFlightMode = Status.FLIGHT_SEARCH;

	private static final double STRIKE_RUSH_HOLD_DISTANCE = 0.8D;
	private static final double STRIKE_RUSH_MISS_CARRY = 0.35D;
	private static final double STRIKE_RUSH_SPEED_SCALE = 0.6D;
	private static final double STRIKE_RUSH_MIN_SPEED = 0.9D;
	private static final double STRIKE_RUSH_MAX_SPEED = 1.5D;
	private static int strikeRushTicks = 0;
	private static int strikeRushTargetId = -1;
	private static final java.util.Map<Integer, Long> REMOTE_STRIKE_RUSHES = new java.util.HashMap<>();
	private static Vec3 strikeClashAnchor = null;
	private static float strikeClashYaw = 0.0F;

	public static FlySkillEvent getInstance() {
		return INSTANCE;
	}

	public static void startStrikeRush(int ticks, int targetId) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null && strikeRushTicks <= 0) player.playSound(MainSounds.TRANSFORM_ON.get(), 0.7F, 1.2F);
		strikeRushTicks = ticks;
		strikeRushTargetId = targetId;
		wasSprintingInAir = true;
	}

	public static void setRemoteStrikeRush(int playerId, int ticks) {
		var level = Minecraft.getInstance().level;
		if (ticks > 0 && level != null) REMOTE_STRIKE_RUSHES.put(playerId, level.getGameTime() + ticks);
		else REMOTE_STRIKE_RUSHES.remove(playerId);
	}

	public static boolean isStrikeRushing(AbstractClientPlayer player) {
		if (player == null) return false;
		if (player == Minecraft.getInstance().player) return strikeRushTicks > 0;
		Long until = REMOTE_STRIKE_RUSHES.get(player.getId());
		if (until == null) return false;
		if (player.level().getGameTime() < until) return true;
		REMOTE_STRIKE_RUSHES.remove(player.getId());
		return false;
	}

	public static void stopStrikeRush(boolean connected) {
		if (strikeRushTicks <= 0) return;
		strikeRushTicks = 0;
		strikeRushTargetId = -1;
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return;
		player.setDeltaMovement(connected ? Vec3.ZERO : player.getDeltaMovement().scale(STRIKE_RUSH_MISS_CARRY));
		player.fallDistance = 0F;
		flightVector = player.getDeltaMovement();
		CombatFlightHandler.initFromMotion(player);
	}

	public static void holdStrikeClash(Vec3 anchor, float yaw) {
		strikeRushTicks = 0;
		strikeRushTargetId = -1;
		strikeClashAnchor = anchor;
		strikeClashYaw = yaw;
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null) applyStrikeClashHold(player);
	}

	public static void releaseStrikeClash() {
		if (strikeClashAnchor == null) return;
		strikeClashAnchor = null;
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return;
		player.setDeltaMovement(Vec3.ZERO);
		flightVector = Vec3.ZERO;
		CombatFlightHandler.initFromMotion(player);
	}

	public static boolean isStrikeClashHeld() {
		return strikeClashAnchor != null;
	}

	private static void applyStrikeClashHold(LocalPlayer player) {
		player.setPos(strikeClashAnchor.x, strikeClashAnchor.y, strikeClashAnchor.z);
		player.setDeltaMovement(Vec3.ZERO);
		player.fallDistance = 0F;
		player.setYRot(strikeClashYaw);
		player.yRotO = strikeClashYaw;
		player.setXRot(0.0F);
		player.xRotO = 0.0F;
		player.setYHeadRot(strikeClashYaw);
		player.yBodyRot = strikeClashYaw;
		player.yBodyRotO = strikeClashYaw;
		flightVector = Vec3.ZERO;
	}

	private static double strikeRushSpeed(LocalPlayer player) {
		return StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> {
			float levelMultiplier = 1.0F + (0.20F * data.getSkills().getSkillLevel("fly"));
			double sprintSpeed = BASE_SPRINT_SPEED * levelMultiplier * getFlySpeedScale(player) * data.getResources().getFlightSpeedLimit() / 100;
			return Mth.clamp(sprintSpeed * STRIKE_RUSH_SPEED_SCALE, STRIKE_RUSH_MIN_SPEED, STRIKE_RUSH_MAX_SPEED);
		}).orElse(STRIKE_RUSH_MIN_SPEED);
	}

	private static void tickStrikeRush(LocalPlayer player) {
		if (!player.isAlive()) {
			stopStrikeRush(false);
			return;
		}
		double speed = strikeRushSpeed(player);
		Vec3 velocity;
		Entity target = strikeRushTargetId > 0 ? player.level().getEntity(strikeRushTargetId) : null;
		if (target != null && target.isAlive()) {
			Vec3 from = player.getBoundingBox().getCenter();
			AABB box = target.getBoundingBox();
			Vec3 closest = new Vec3(Mth.clamp(from.x, box.minX, box.maxX), Mth.clamp(from.y, box.minY, box.maxY), Mth.clamp(from.z, box.minZ, box.maxZ));
			double gap = closest.distanceTo(from) - STRIKE_RUSH_HOLD_DISTANCE;
			Vec3 toCenter = box.getCenter().subtract(from);
			velocity = gap <= 0.0D || toCenter.lengthSqr() < 1.0E-6 ? Vec3.ZERO : toCenter.normalize().scale(Math.min(speed, gap));
		} else velocity = FlightOrientationHandler.getForwardVector(player).scale(speed);

		player.setDeltaMovement(velocity);
		player.fallDistance = 0F;
		flightVector = velocity;
		fastFlight = true;
		if (--strikeRushTicks <= 0) stopStrikeRush(false);
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

						if (!player.isCreative() && !player.isSpectator() && data.getResources().getCurrentEnergy() < energyCost) return;

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

	private static boolean isStationaryStrike(StatsData data) {
		if (!data.getStatus().isStrikeLocked()) return false;
		var selected = data.getTechniques().getSelectedTechnique();
		if (selected == null) return false;
		return "dimensional_punch".equals(selected.getId()) || "dimensional_sword_attack".equals(selected.getId())
				|| "meteor_burst".equals(selected.getId()) || "mystic_ball".equals(selected.getId())
				|| "darkness_eye_beam".equals(selected.getId());
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
			if (!player.isCreative() && !player.isSpectator() && data.getResources().getCurrentEnergy() < energyCost) return;

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

		if (strikeClashAnchor != null) {
			applyStrikeClashHold(player);
			return;
		}

		if (strikeRushTicks > 0) {
			tickStrikeRush(player);
			return;
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
				else initializeFlightVectorFromCurrentMotion(player, data.getSkills().getSkillLevel("fly"), data.getResources().getFlightSpeedLimit());
				lastFlightMode = flightMode;
			}

			if (isFlying && flightMode != lastFlightMode) {
				if (isCombatFly) {
					resetFlightState();
					CombatFlightHandler.initFromMotion(player);
				} else {
					CombatFlightHandler.reset();
					initializeFlightVectorFromCurrentMotion(player, data.getSkills().getSkillLevel("fly"), data.getResources().getFlightSpeedLimit());
				}
				Component switchHint = Component.empty()
						.append(KeyBinds.SECOND_FUNCTION_KEY.getTranslatedKeyMessage())
						.append(" + ")
						.append(KeyBinds.FLY_KEY.getTranslatedKeyMessage());
				player.displayClientMessage(Component.translatable(
						isCombatFly ? "dragonminez.flight.mode.switched.combat" : "dragonminez.flight.mode.switched.search",
						switchHint), true);
				lastFlightMode = flightMode;
			}

			boolean movementRestricted = TechniqueDispatcher.isMovementRestrictedKiAttack(player, data) || data.getStatus().isStunned() || data.getStatus().isActionCharging() || data.getStatus().isChargingKi();

			if (isFlying) {
				if (TechniqueDispatcher.isMovementRestrictedKiAttack(player, data) || isStationaryStrike(data) || ClientBeamClashState.isExhausted()) {
					flightVector = Vec3.ZERO;
					player.setDeltaMovement(0.0D, 0.0D, 0.0D);
					player.fallDistance = 0F;
				} else if (isCombatFly) {
					if (pendingFlightDisable) {
						pendingFlightDisable = false;
						NetworkHandler.sendToServer(new FlyToggleC2S(false));
						CombatFlightHandler.reset();
						resetFlightState();
						return;
					}
					CombatFlightHandler.handle(player, data, movementRestricted);
				} else handleFlightMovement(player, data.getSkills().getSkillLevel("fly"), data.getResources().getFlightSpeedLimit(), movementRestricted);
			} else if (!pendingFlightDisable) {
				resetFlightState();
				CombatFlightHandler.reset();
				lastFlightMode = Status.FLIGHT_SEARCH;
			}

			wasFlyingSkillActive = isFlying;
		});
	}

	private static void handleFlightMovement(LocalPlayer player, int flyLevel, int flightSpeedLimit, boolean movementRestricted) {
		float flySpeedScale = getFlySpeedScale(player);
		float levelMultiplier = 1.0F + (0.20F * flyLevel);
		float maxNormalSpeed = NORMAL_MAX_SPEED * levelMultiplier * flySpeedScale * flightSpeedLimit / 100;
		float maxSprintSpeed = SPRINT_MAX_SPEED * levelMultiplier * flySpeedScale * flightSpeedLimit / 100;

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

		Vec3 lookDir = isFastFlight ? FlightOrientationHandler.getForwardVector(player) : player.getLookAngle();
		Vec3 targetDirection = Vec3.ZERO;

		if (isForward) targetDirection = targetDirection.add(lookDir);
		if (isBack) targetDirection = targetDirection.add(lookDir.scale(-0.5));
		if (isFastFlight) {
			Vec3 rightDir = FlightOrientationHandler.getRightVector(player);
			if (isLeft) targetDirection = targetDirection.add(rightDir.scale(-0.7));
			if (isRight) targetDirection = targetDirection.add(rightDir.scale(0.7));
		} else {
			if (isLeft) {
				Vec3 leftDir = lookDir.yRot((float) Math.toRadians(90)).normalize();
				targetDirection = targetDirection.add(new Vec3(leftDir.x, 0, leftDir.z).scale(0.7));
			}
			if (isRight) {
				Vec3 rightDir = lookDir.yRot((float) Math.toRadians(-90)).normalize();
				targetDirection = targetDirection.add(new Vec3(rightDir.x, 0, rightDir.z).scale(0.7));
			}
		}

		handleFastFlightManeuvers(isFastFlight, isLeft, isRight);

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
				Vec3 currentDirection = currentSpeed > 0.001 ? flightVector.scale(1.0 / currentSpeed) : Vec3.ZERO;
				if (isFastFlight && currentDirection.dot(targetDirection) > TURN_REVERSAL_DOT) {
					Vec3 steered = currentDirection.lerp(targetDirection, TURN_SPEED_FAST).normalize();
					flightVector = steered.scale(Mth.lerp(TURN_SPEED_FAST, currentSpeed, targetSpeed));
				} else {
					flightVector = new Vec3(Mth.lerp(TURN_SPEED_NORMAL, flightVector.x, targetVelocity.x), Mth.lerp(TURN_SPEED_NORMAL, flightVector.y, targetVelocity.y), Mth.lerp(TURN_SPEED_NORMAL, flightVector.z, targetVelocity.z));
				}
			}
			hovering = Math.min(1F, hovering + 0.1F);
		} else {
			if (currentSpeed > 0.01) {
				double newSpeed = Math.max(0, currentSpeed - DECELERATION * levelMultiplier);
				if (currentSpeed > 0.001) flightVector = flightVector.normalize().scale(newSpeed);
				else flightVector = Vec3.ZERO;
			} else flightVector = Vec3.ZERO;
		}

		double speedAfterInput = flightVector.length();
		if (speedAfterInput > currentMaxSpeed) {
			flightVector = flightVector.normalize().scale(Math.max(currentMaxSpeed, speedAfterInput - DECELERATION * levelMultiplier));
		}

		if (GravityLogic.isFlightHardStopped(ClientGravityState.getNetGravity())) {
			flightVector = Vec3.ZERO;
			player.setDeltaMovement(0, -1.5, 0);
		} else {
			double flyFactor = GravityLogic.getFlyFactor(ClientGravityState.getNetGravity());
			if (flyFactor < 1.0) flightVector = flightVector.scale(flyFactor);
		}

		if (flightVector.length() > 0.01 || pendingFlightDisable) {
			player.setDeltaMovement(flightVector);
			player.fallDistance = 0F;
			verticalHover = 0;
		} else handleHovering(player, isJump, isCrouch, flyLevel, maxNormalSpeed);

		if (player.onGround() && !pendingFlightActivation) {
			pendingFlightDisable = false;
			NetworkHandler.sendToServer(new FlyToggleC2S(false));
			resetFlightState();
		}
	}

	private static void handleFastFlightManeuvers(boolean isFastFlight, boolean isLeft, boolean isRight) {
		long now = System.currentTimeMillis();
		if (isFastFlight) {
			FlightOrientationHandler.setStrafe((isRight ? 1F : 0F) - (isLeft ? 1F : 0F));
			if (isLeft && !wasStrafingLeft) {
				if (now - lastLeftTapTime <= DOUBLE_TAP_WINDOW_MS) FlightOrientationHandler.barrelRoll(-1F);
				lastLeftTapTime = now;
			}
			if (isRight && !wasStrafingRight) {
				if (now - lastRightTapTime <= DOUBLE_TAP_WINDOW_MS) FlightOrientationHandler.barrelRoll(1F);
				lastRightTapTime = now;
			}
		}
		wasStrafingLeft = isLeft;
		wasStrafingRight = isRight;
	}

	private static float getFlySpeedScale(LocalPlayer player) {
		double attrValue = player.getAttributes().hasAttribute(EntityAttributes.FLY_SPEED.get()) ? player.getAttributeValue(EntityAttributes.FLY_SPEED.get()) : 0.0;
		double scale = attrValue <= 0.0 ? 1.0 : Mth.clamp(attrValue / BASE_ATTRIBUTE_FLY_SPEED, 0.25, 4.0);
		return (float) (scale * flightStateSpeedMultiplier(player));
	}

	public static double flightStateSpeedMultiplier(LocalPlayer player) {
		return StatsProvider.get(StatsCapability.INSTANCE, player)
				.map(StatsData::getFlightStateSpeedMultiplier)
				.orElse(1.0);
	}

	private static void initializeFlightVectorFromCurrentMotion(LocalPlayer player, int flyLevel, int flightSpeedLimit) {
		float speedScale = getFlySpeedScale(player);
		float levelMultiplier = 1.0F + (0.20F * flyLevel);
		float maxNormalSpeed = NORMAL_MAX_SPEED * levelMultiplier * speedScale * flightSpeedLimit / 100;
		float maxSprintSpeed = SPRINT_MAX_SPEED * levelMultiplier * speedScale * flightSpeedLimit / 100;
		float capSpeed = Math.max(maxNormalSpeed, maxSprintSpeed);

		Vec3 currentMotion = player.getDeltaMovement();
		if (currentMotion.lengthSqr() < 1.0E-5) {
			flightVector = player.getLookAngle().scale(maxNormalSpeed * 0.35F);
			return;
		}

		double clamped = Math.min(currentMotion.length(), capSpeed);
		flightVector = currentMotion.normalize().scale(clamped);
	}

	public static double hoverDescentFactor(int flyLevel) {
		int clampedLevel = Mth.clamp(flyLevel, 1, 10);
		float t = (clampedLevel - 1) / (float) (10 - 1);
		return Mth.lerp(t, 1.0F, 0.15F);
	}

	private static void handleHovering(LocalPlayer player, boolean isJump, boolean isCrouch, int flyLevel, float maxVerticalSpeed) {
		if (isJump) {
			if (verticalHover < 20) verticalHover = Mth.clamp(verticalHover + 1, -20, 20);
		} else if (isCrouch) {
			if (verticalHover > -20) verticalHover = Mth.clamp(verticalHover - 1, -20, 20);
		} else {
			if (verticalHover > -3) verticalHover = Mth.clamp(verticalHover - 1, -3, 20);
			else if (verticalHover < -3) verticalHover = Mth.clamp(verticalHover + 1, -20, -3);
		}

		double yMovement;
		if (verticalHover >= -3 && verticalHover <= 0 && !isJump && !isCrouch) yMovement = SLOW_DESCENT_RATE * hoverDescentFactor(flyLevel) + (Math.sin(player.tickCount / 10F) / 200F);
		else if (verticalHover == 0) yMovement = Math.sin(player.tickCount / 10F) / 100F;
		else yMovement = verticalHover / 60D;

		if (!isJump && !isCrouch && getGroundDistance(player) <= MIN_GROUND_CLEARANCE) {
			yMovement = Math.max(0.0D, yMovement);
		}

		yMovement = Mth.clamp(yMovement, -maxVerticalSpeed, maxVerticalSpeed);
		player.setDeltaMovement(new Vec3(
				player.getDeltaMovement().x * 0.9,
				yMovement,
				player.getDeltaMovement().z * 0.9
		));
		player.fallDistance = 0F;

		if (hovering < 1F) hovering = Math.min(1F, hovering + 0.1F);
	}

	public static void injectKnockback(Vec3 knockback) {
		flightVector = flightVector.add(knockback);
	}

	private static void resetFlightState() {
		flightVector = Vec3.ZERO;
		verticalHover = 0;
		hovering = 0F;
		pendingFlightDisable = false;
		wasFlyingSkillActive = false;
		wasSprintingInAir = false;
		fastFlight = false;
		FlightOrientationHandler.reset();
	}

	private boolean isFlyingFast() {
		double speed = flightVector.length();
		if (fastFlight) {
			if (speed < FAST_FLYING_EXIT_THRESHOLD) fastFlight = false;
		} else if (speed > FAST_FLYING_THRESHOLD) fastFlight = true;
		return fastFlight;
	}

	public boolean isFlyingFast(AbstractClientPlayer player) {
		if (player == null) return false;
		if (isStrikeRushing(player)) return true;

		LocalPlayer localPlayer = Minecraft.getInstance().player;
		if (localPlayer != null && player == localPlayer) return isFlyingFast();

		boolean flyActive = StatsProvider.get(StatsCapability.INSTANCE, player)
				.map(data -> {
					Skill flySkill = data.getSkills().getSkill("fly");
					return flySkill != null && flySkill.isActive() && data.getStatus().getFlightMode() != Status.FLIGHT_COMBAT;
				}).orElse(false);
		if (!flyActive) {
			REMOTE_FLIGHT.remove(player);
			return false;
		}

		RemoteFlight state = REMOTE_FLIGHT.computeIfAbsent(player, key -> new RemoteFlight());
		if (state.tick != player.tickCount) {
			state.tick = player.tickCount;
			double dx = player.getX() - player.xo;
			double dy = player.getY() - player.yo;
			double dz = player.getZ() - player.zo;
			double step = Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (step > REMOTE_TELEPORT_STEP) step = state.speed;
			state.speed = Mth.lerp(REMOTE_SPEED_SMOOTHING, state.speed, step);
			if (state.fast) state.fast = state.speed >= FAST_FLYING_EXIT_THRESHOLD;
			else state.fast = state.speed > FAST_FLYING_THRESHOLD;
		}
		return state.fast;
	}

	private static final class RemoteFlight {
		private int tick = Integer.MIN_VALUE;
		private double speed;
		private boolean fast;
	}

	private static double getActivationEnergyPercent(int flyLevel) {
		return Math.max(0.01, 0.04 - (flyLevel * 0.003));
	}

	private static double getGroundDistance(LocalPlayer player) {
		Vec3 start = new Vec3(player.getX(), player.getBoundingBox().minY, player.getZ());
		Vec3 end = start.add(0.0D, -1.5D, 0.0D);
		HitResult hit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (hit.getType() == HitResult.Type.MISS) return Double.MAX_VALUE;
		return start.y - hit.getLocation().y;
	}
}
