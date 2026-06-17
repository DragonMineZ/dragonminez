package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.clash.ClientBeamClashState;
import com.dragonminez.client.flight.FlightSoundInstance;
import com.dragonminez.client.gui.hud.ScouterHUD;
import com.dragonminez.client.systems.kisense.CombatIndicators;
import com.dragonminez.client.systems.kisense.KiSenseScan;
import com.dragonminez.client.systems.kisense.KiSenseState;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.particles.AuraParticle;
import com.dragonminez.common.init.particles.DivineParticle;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.network.C2S.*;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.stats.skills.Skill;
import com.dragonminez.common.stats.techniques.*;
import com.dragonminez.common.util.BetaWhitelist;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.server.events.players.StatsEvents;
import com.dragonminez.server.util.GravityLogic;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientStatsEvents {
	private static final int TECHNIQUE_VISIBLE_SLOTS = Techniques.SLOT_COUNT;
	private static final int BAR_SLOTS = 4;

	private static FlightSoundInstance flightSound;

	private static long lastTransformTapTime = 0;
	private static long lastKiChargeTapTime = 0;
	private static int kiBlastTimer = 0;
	private static int blockLockTicks = 0;
	private static boolean wasTransformKeyDown = false;
	private static boolean wasKiChargeKeyDown = false;
	private static long lastDashTime = 0;
	private static boolean wasDashKeyDown = false;
	private static boolean wasDescendActionDown = false;
	private static long itKeyDownTime = 0;
	private static boolean wasITKeyDown = false;
	private static boolean itMenuOpened = false;
	private static boolean wasRightClickDown = false;

	private static int activeChargeSlot = -1;
	private static boolean chargeReleaseSent = false;
	private static boolean chargePending = false;
	private static int chargePendingTicks = 0;
	private static final boolean[] wasSlotKeyDown = new boolean[TECHNIQUE_VISIBLE_SLOTS];

	@SubscribeEvent
	public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
		if (Minecraft.getInstance().player == null) return;
		StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player).ifPresent(data -> {
			boolean isChargingTechnique = data.getTechniques().isTechniqueCharging() || data.getTechniques().isTechniqueChargeActive();
			if (isChargingTechnique) event.setCanceled(true);
		});
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;

		Minecraft mc = Minecraft.getInstance();
		LocalPlayer localPlayer = mc.player;

		if (localPlayer == null) return;

		if (mc.level != null && !mc.isPaused()) {
			for (Player player : mc.level.players()) {
				var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
				if (stats == null || !stats.getStatus().isHasCreatedCharacter()) continue;

				boolean isAuraActive = stats.getStatus().isAuraActive() || stats.getStatus().isPermanentAura();
				if (!isAuraActive) continue;

				float totalScale = getBodyScale(stats)[0];

				if (player.onGround()) {
					spawnGroundDust(player, totalScale);
					spawnFloatingRubble(player, totalScale);
				}

				if (!BetaWhitelist.isAllowed(player.getGameProfile().getName())) continue;

				if (characterHasAuraColor(stats.getCharacter())) {
					int particleColor = getAuraColor(stats.getCharacter());
					spawnCalmAuraParticle(player, totalScale, particleColor);
				}

				if (player.getRandom().nextInt(20) == 0) {
					int divineCount = 5 + player.getRandom().nextInt(10);
					for (int i = 0; i < divineCount; i++) spawnPassiveDivineParticle(player, totalScale, 0xFFFFFF);
				}
			}
		}

		if (mc.screen != null) {
			StatsProvider.get(StatsCapability.INSTANCE, localPlayer).ifPresent(data -> {
				if (data.getStatus().isBlocking()) {
					data.getStatus().setBlocking(false);
					NetworkHandler.sendToServer(new UpdateStatC2S(UpdateStatC2S.StatAction.BLOCK, false));
				}
			});
			return;
		}

		StatsProvider.get(StatsCapability.INSTANCE, localPlayer).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;

			if (data.getTechniques().getSelectedSlot() >= TECHNIQUE_VISIBLE_SLOTS) {
				data.getTechniques().selectSlot(0);
				NetworkHandler.sendToServer(new SelectTechniqueSlotC2S(0));
			}

			Character character = data.getCharacter();

			boolean isStunned = data.getStatus().isStunned() || data.getStatus().isStrikeLocked() || data.getStatus().isKnockedDown();
			boolean isKiChargeKeyPressed = KeyBinds.KI_CHARGE.isDown() && !isStunned;
			boolean isDescendKeyPressed = KeyBinds.SECOND_FUNCTION_KEY.isDown() && !isStunned;
			boolean isActionKeyPressed = KeyBinds.ACTION_KEY.isDown() && !isStunned;
			boolean isRightClickDown = mc.options.keyUse.isDown() && !isStunned;
			boolean isBlockKeyDown = KeyBinds.BLOCK_KEY.isDown() && !isStunned;
			boolean isITKeyDown = KeyBinds.INSTANT_TRANSMISSION.isDown() && !isStunned;

			boolean isActionRestricted = TechniqueDispatcher.isActionRestrictedKiAttack(localPlayer, data);
			boolean isMovementRestricted = TechniqueDispatcher.isMovementRestrictedKiAttack(localPlayer, data);

			if (isActionRestricted) {
				isKiChargeKeyPressed = false;
				isActionKeyPressed = false;
				isBlockKeyDown = false;

				if (isMovementRestricted && TechniqueDispatcher.isFiringKiAttack(localPlayer)) {
					localPlayer.setYRot(localPlayer.yRotO);
					localPlayer.setXRot(localPlayer.xRotO);
					localPlayer.yHeadRot = localPlayer.yHeadRotO;
					localPlayer.yBodyRot = localPlayer.yBodyRotO;
				}
			}

			boolean isChargingTechnique = data.getTechniques().isTechniqueCharging() || data.getTechniques().isTechniqueChargeActive();
			if (blockLockTicks > 0) blockLockTicks--;
			if (isChargingTechnique || isDescendKeyPressed || blockLockTicks > 0) isBlockKeyDown = false;

			var nextForm = TransformationsHelper.getNextAvailableForm(data);
			boolean isOozaruNextForm = TransformationsHelper.isOozaruForm(nextForm);
			boolean canAutoChargeOozaru = !isActionRestricted && TransformationsHelper.shouldAutoChargeOozaru(localPlayer, data);
			boolean shouldChargeAction = isActionKeyPressed || canAutoChargeOozaru;

			boolean kiWeaponActive = PlayerAttackHelper.isKiWeaponActive(localPlayer);
			if ((kiWeaponActive || isChargingTechnique) && data.getStatus().isBlocking() || Minecraft.getInstance().screen != null) {
				data.getStatus().setBlocking(false);
				NetworkHandler.sendToServer(new UpdateStatC2S(UpdateStatC2S.StatAction.BLOCK, false));
			} else if (isBlockKeyDown != data.getStatus().isBlocking() && !PlayerAttackHelper.isKiWeaponActive(localPlayer)) {
				if (isBlockKeyDown && !kiWeaponActive && localPlayer.getItemInHand(InteractionHand.MAIN_HAND).isEmpty() && localPlayer.getItemInHand(InteractionHand.OFF_HAND).isEmpty()) {
					data.getStatus().setBlocking(isBlockKeyDown);
					NetworkHandler.sendToServer(new UpdateStatC2S(UpdateStatC2S.StatAction.BLOCK, isBlockKeyDown));
				} else if (!isBlockKeyDown) {
					data.getStatus().setBlocking(isBlockKeyDown);
					NetworkHandler.sendToServer(new UpdateStatC2S(UpdateStatC2S.StatAction.BLOCK, isBlockKeyDown));
				}
			}

			boolean chargeSessionActive = data.getTechniques().isTechniqueCharging() || data.getTechniques().isTechniqueChargeActive();
			if (isDescendKeyPressed && isRightClickDown && !wasRightClickDown && !chargeSessionActive
					&& !TechniqueDispatcher.isFiringKiAttack(localPlayer) && (!data.getStatus().isFused() && !data.getStatus().isFusionLeader())) {
				float[] kiRgb;
				if (character.hasActiveStackForm()
						&& character.getActiveStackFormData() != null
						&& character.getActiveStackFormData().getAuraColor() != null
						&& !character.getActiveStackFormData().getAuraColor().isEmpty()) {
					kiRgb = character.getActiveStackFormData().getRgbAuraColor();
				} else if (character.hasActiveForm()
						&& character.getActiveFormData() != null
						&& character.getActiveFormData().getAuraColor() != null
						&& !character.getActiveFormData().getAuraColor().isEmpty()) {
					kiRgb = character.getActiveFormData().getRgbAuraColor();
				} else kiRgb = character.getRgbAuraColor();
				int colorMain = ColorUtils.rgbToInt(kiRgb[0], kiRgb[1], kiRgb[2]);
				int colorBorder = ColorUtils.darkenColor(colorMain, 0.85f);
				NetworkHandler.sendToServer(new KiBlastC2S(true, colorMain, colorBorder));
				kiBlastTimer = 10;
				blockLockTicks = 20;
			}
			// Beam clash QTE: while clashing, a fresh fire-key (right click) tap pushes the meter.
			if (ClientBeamClashState.isActive() && isRightClickDown && !wasRightClickDown) {
				NetworkHandler.sendToServer(new BeamClashInputC2S());
			}

			wasRightClickDown = isRightClickDown;

			handleTechniqueSlotInput(data, isStunned);

			if (kiBlastTimer > 0) {
				if (kiBlastTimer == 1) NetworkHandler.sendToServer(new KiBlastC2S(false, 0, 0));
				kiBlastTimer--;
			}

			long currentTime = System.currentTimeMillis();
			if (isOozaruNextForm) lastTransformTapTime = 0;

			if (isOozaruNextForm) {
				lastTransformTapTime = 0;
			} else if (isActionKeyPressed && !wasTransformKeyDown) {
				if ((currentTime - lastTransformTapTime) <= 500) {
					NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.INSTANT_TRANSFORM));
					lastTransformTapTime = 0;
				} else lastTransformTapTime = currentTime;

				if (data.getStatus().getSelectedAction() == ActionMode.FORM && TransformationsHelper.isNextFormMasteryBlocked(data)) {
					FormConfig.FormData blocked = TransformationsHelper.getNextFormCandidate(data);
					if (blocked != null) {
						String group = character.hasActiveForm() ? character.getActiveFormGroup() : character.getSelectedFormGroup();
						Component formName = Component.translatable("race.dragonminez." + character.getRaceName() + ".form." + group + "." + blocked.getName());
						localPlayer.displayClientMessage(Component.translatable("message.dragonminez.form.no_mastery", blocked.getUnlockOnMastery().intValue(), formName), true);
					}
				}
			}
			wasTransformKeyDown = isActionKeyPressed;

			if (isKiChargeKeyPressed && !wasKiChargeKeyDown) {
				if ((currentTime - lastKiChargeTapTime) <= 500) {
					NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.INSTANT_RELEASE));
					lastKiChargeTapTime = 0;
				} else lastKiChargeTapTime = currentTime;
			}
			wasKiChargeKeyDown = isKiChargeKeyPressed;

			if (isITKeyDown && !wasITKeyDown) {
				itKeyDownTime = currentTime;
				itMenuOpened = false;
			} else if (!isITKeyDown && wasITKeyDown) {
				long duration = currentTime - itKeyDownTime;
				if (duration < 500 && !itMenuOpened) {
					var lockedTarget = LockOnEvent.getLockedTarget();
					java.util.UUID targetId = lockedTarget != null ? lockedTarget.getUUID() : null;
					NetworkHandler.sendToServer(new InstantTransmissionTapC2S(targetId));
				}
			} else if (isITKeyDown && (currentTime - itKeyDownTime) >= 500 && !itMenuOpened) {
				itMenuOpened = true;
				Skill itSkill = data.getSkills().getSkill("instant_transmission");
				if (itSkill != null && itSkill.getLevel() >= 5) {
					NetworkHandler.sendToServer(new RequestITTargetsC2S());
				}
			}
			wasITKeyDown = isITKeyDown;

			if (isKiChargeKeyPressed != data.getStatus().isChargingKi()) {
				NetworkHandler.sendToServer(new UpdateStatC2S(UpdateStatC2S.StatAction.CHARGE_KI, isKiChargeKeyPressed));
			}

			if (isDescendKeyPressed != data.getStatus().isDescending()) {
				NetworkHandler.sendToServer(new UpdateStatC2S(UpdateStatC2S.StatAction.DESCEND, isDescendKeyPressed));
			}

			if (shouldChargeAction != data.getStatus().isActionCharging()) {
				NetworkHandler.sendToServer(new UpdateStatC2S(UpdateStatC2S.StatAction.ACTION_CHARGE, shouldChargeAction));
			}

			boolean isDescendActionDown = isDescendKeyPressed && isActionKeyPressed;
			if (isDescendActionDown && !wasDescendActionDown && (data.getStatus().getSelectedAction().equals(ActionMode.FORM) || data.getStatus().getSelectedAction().equals(ActionMode.STACK))) {
				NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.DESCEND));
			}
			wasDescendActionDown = isDescendActionDown;

			boolean isFlying = data.getSkills().isSkillActive("fly") && !localPlayer.onGround() && !localPlayer.isInWater();

			if (isFlying) {
				if (flightSound == null || !mc.getSoundManager().isActive(flightSound)) {
					flightSound = new FlightSoundInstance(localPlayer);
					mc.getSoundManager().play(flightSound);
				}
			} else flightSound = null;

			boolean hasScouter = !getScouterStack(localPlayer).isEmpty();
			if (KeyBinds.KI_SENSE.consumeClick()) {
				if (!hasScouter) {
					Skill kiSense = data.getSkills().getSkill("kisense");
					if (kiSense != null && kiSense.getLevel() > 0) KiSenseState.cycle();
				} else ScouterHUD.setRenderingInfo(!ScouterHUD.isRenderingInfo());
			}
		});
	}

	private static void handleTechniqueSlotInput(StatsData data, boolean isStunned) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return;

		var techniques = data.getTechniques();
		boolean sessionActive = techniques.isTechniqueCharging() || techniques.isTechniqueChargeActive();

		boolean ctrlHeld = Screen.hasControlDown();

		boolean[] downNow = new boolean[TECHNIQUE_VISIBLE_SLOTS];
		boolean altHeld = KeyBinds.isSecondFunctionDown();
		long window = Minecraft.getInstance().getWindow().getWindow();
		for (int i = 0; i < TECHNIQUE_VISIBLE_SLOTS; i++) {
			InputConstants.Key key = KeyBinds.TECHNIQUE_SLOTS[i].getKey();
			boolean rawDown = key.getType() == InputConstants.Type.KEYSYM
					? InputConstants.isKeyDown(window, key.getValue())
					: KeyBinds.TECHNIQUE_SLOTS[i].isDown();
			boolean barAllows = (i < BAR_SLOTS) ? (altHeld && !ctrlHeld) : (ctrlHeld && altHeld);
			downNow[i] = rawDown && barAllows;
		}

		if (activeChargeSlot < 0) {
			resetChargeSession();
			boolean firingRestricted = TechniqueDispatcher.isMovementRestrictedKiAttack(player, data);

			for (int i = 0; i < TECHNIQUE_VISIBLE_SLOTS; i++) {
				boolean pressed = downNow[i] && !wasSlotKeyDown[i];
				wasSlotKeyDown[i] = downNow[i];
				if (!pressed || isStunned || firingRestricted) continue;

				String id = techniques.getEquippedSlots()[i];
				TechniqueData t = (id == null || id.isEmpty()) ? null : techniques.getUnlockedTechniques().get(id);
				if (t == null) continue;
				if (!canActivateTechnique(data, player)) continue;

				if (t instanceof StrikeAttackData) {
					techniques.selectSlot(i);
					NetworkHandler.sendToServer(new SelectTechniqueSlotC2S(i));
					var lockedTarget = LockOnEvent.getLockedTarget();
					int targetId = lockedTarget != null ? lockedTarget.getId() : -1;
					NetworkHandler.sendToServer(new StrikeAttackC2S(targetId));
					net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new DMZClientEvent.StrikeAttack(player, targetId));
				} else if (t instanceof KiAttackData ki && !data.getCooldowns().hasCooldown("TechniqueCooldown_" + id)) { if (player.isPassenger() && TechniqueDispatcher.restrictsMovementWhileCharging(ki.getKiType())) continue; var lockedKiTarget = LockOnEvent.getLockedTarget(); int kiTargetId = lockedKiTarget != null ? lockedKiTarget.getId() : -1;
					if (ki.isInstantCast()) NetworkHandler.sendToServer(TechniqueChargeC2S.start(i, kiTargetId));
					else {
						activeChargeSlot = i;
						chargeReleaseSent = false;
						chargePending = true;
						chargePendingTicks = 0;
						NetworkHandler.sendToServer(TechniqueChargeC2S.start(i, kiTargetId));
					}
					net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new DMZClientEvent.KiAttackCast(player, i));
				}
			}
			return;
		}

		for (int i = 0; i < TECHNIQUE_VISIBLE_SLOTS; i++) wasSlotKeyDown[i] = downNow[i];

		if (!sessionActive) {
			if (chargePending) {
				if (++chargePendingTicks > 30) { resetChargeSession(); return; }
			} else {
				resetChargeSession();
				return;
			}
		} else {
			chargePending = false;
			chargePendingTicks = 0;
		}

		boolean slotDown = activeChargeSlot < TECHNIQUE_VISIBLE_SLOTS && downNow[activeChargeSlot];
		if (!slotDown && !chargeReleaseSent) {
			NetworkHandler.sendToServer(TechniqueChargeC2S.setHolding(false));
			chargeReleaseSent = true;
			net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new DMZClientEvent.KiAttackRelease(player));
		}
	}

	private static boolean canActivateTechnique(StatsData data, LocalPlayer player) {
		if (player.isSpectator()) return false;
		if (data.getStatus().isFused() && !data.getStatus().isFusionLeader()) return false;
		if (data.getSkills().getSkillLevel("kicontrol") <= 0) {
			player.displayClientMessage(Component.translatable("message.dragonminez.technique.no_ki_control")
					.withStyle(ChatFormatting.RED), true);
			return false;
		}
		if (data.getResources().getPowerRelease() < 5) return false;
		if (!player.getMainHandItem().isEmpty()) return false;
		return true;
	}

	private static void resetChargeSession() {
		activeChargeSlot = -1;
		chargeReleaseSent = false;
		chargePending = false;
		chargePendingTicks = 0;
	}

	private static void resetChargeTracking() {
		resetChargeSession();
		for (int i = 0; i < TECHNIQUE_VISIBLE_SLOTS; i++) wasSlotKeyDown[i] = false;
	}

	private static ItemStack getScouterStack(Player player) {
		return CuriosApi.getCuriosInventory(player)
				.map(inv -> inv.getCurios().get("head_tech"))
				.map(handler -> handler.getStacks().getStackInSlot(0))
				.orElse(ItemStack.EMPTY);
	}

	@SubscribeEvent
	public static void onKeyPressed(InputEvent.Key event) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;
			boolean isStunned = data.getStatus().isStunned() || data.getStatus().isStrikeLocked() || data.getStatus().isKnockedDown();
			if (TechniqueDispatcher.isMovementRestrictedKiAttack(player, data) || data.getStatus().isStrikeLocked()) return;

			boolean isDashKeyDown = KeyBinds.DASH_KEY.isDown();
			if (isDashKeyDown && !wasDashKeyDown && !isStunned) {
				long currentTime = System.currentTimeMillis();
				boolean isDoubleDash = (currentTime - lastDashTime) <= 300 && data.getCooldowns().hasCooldown(Cooldowns.DASH_ACTIVE);
				lastDashTime = currentTime;

				float xInput = 0;
				float zInput = 0;

				if (player.input.up) zInput += 1;
				if (player.input.down) zInput -= 1;
				if (player.input.left) xInput -= 1;
				if (player.input.right) xInput += 1;

				if (xInput == 0 && zInput == 0) {
					zInput = 1;
				}

				NetworkHandler.sendToServer(new DashC2S(xInput, zInput, isDoubleDash));
			}
			wasDashKeyDown = isDashKeyDown;

			if (KeyBinds.LOCK_ON.consumeClick() && !isStunned) {
				Skill kiSense = data.getSkills().getSkill("kisense");
				if (kiSense == null) return;
				LockOnEvent.toggleLock();
			}
		});
	}

	@SubscribeEvent
	public static void onComputeFovModifier(ComputeFovModifierEvent event) {
		if (event.getPlayer() instanceof LocalPlayer player) {
			AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
			if (speedAttr != null) {
				double totalModFactor = 1.0;

				AttributeModifier formMod = speedAttr.getModifier(StatsEvents.FORM_SPEED_UUID);
				if (formMod != null) totalModFactor *= (1.0 + formMod.getAmount());

				AttributeModifier gravityMod = speedAttr.getModifier(GravityLogic.GRAVITY_SPEED_UUID);
				if (gravityMod != null) totalModFactor *= (1.0 + gravityMod.getAmount());

				AttributeModifier sprintMod = speedAttr.getModifier(MovementSkillsHandler.SPRINT_SPEED_UUID);
				if (sprintMod != null) totalModFactor *= (1.0 + sprintMod.getAmount());

				AttributeModifier weightMod = speedAttr.getModifier(StatsEvents.WEIGHT_MOVEMENT_SPEED_MOD_UUID);
				if (weightMod != null) totalModFactor *= (1.0 + weightMod.getAmount());

				if (totalModFactor != 1.0 && totalModFactor > 0.0) {
					float walkSpeed = player.getAbilities().getWalkingSpeed();
					double currentSpeed = speedAttr.getValue();

					double currentSpeedRatio = currentSpeed / walkSpeed;
					double currentVanillaFovFactor = (currentSpeedRatio + 1.0) / 2.0;

					double cleanSpeedRatio = (currentSpeed / totalModFactor) / walkSpeed;
					double cleanVanillaFovFactor = (cleanSpeedRatio + 1.0) / 2.0;

					if (currentVanillaFovFactor > 0) {
						float rawFov = event.getFovModifier();
						float cleanRawFov = (float) (rawFov / currentVanillaFovFactor * cleanVanillaFovFactor);
						float fovScale = Minecraft.getInstance().options.fovEffectScale().get().floatValue();
						float finalFov = Mth.lerp(fovScale, 1.0F, cleanRawFov);
						event.setNewFovModifier(finalFov);
					}
				}
			}
		}
	}

	@SubscribeEvent
	public static void onMovementInput(MovementInputUpdateEvent event) {
		boolean techMenu = KeyBinds.isSecondFunctionDown();
		if (techMenu) {
			event.getInput().shiftKeyDown = false;
			event.getEntity().setSprinting(false);
		}

		StatsProvider.get(StatsCapability.INSTANCE, event.getEntity()).ifPresent(data -> {
			if (TechniqueDispatcher.isMovementRestrictedKiAttack(event.getEntity(), data) || data.getStatus().isStrikeLocked()
					|| data.getStatus().isKnockedDown() || data.getStatus().isStunned() || data.getStatus().isActionCharging()) {
				event.getInput().forwardImpulse = 0;
				event.getInput().leftImpulse = 0;
				event.getInput().jumping = false;
				event.getInput().shiftKeyDown = false;
				event.getInput().up = false;
				event.getInput().down = false;
				event.getInput().left = false;
				event.getInput().right = false;
			}
		});
	}

	@SubscribeEvent
	public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		wasRightClickDown = false;
		lastTransformTapTime = 0;
		lastKiChargeTapTime = 0;
		resetChargeTracking();
		KiSenseState.reset();
		KiSenseScan.clear();
		CombatIndicators.clear();
		StatsCapability.clearClientCache();
	}

	private static float[] getBodyScale(StatsData stats) {
		float sX = 1.0f, sY = 1.0f, sZ = 1.0f;
		var character = stats.getCharacter();

		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null) {
			sX = character.getActiveStackFormData().getModelScaling()[0];
			sY = character.getActiveStackFormData().getModelScaling()[1];
			sZ = character.getActiveStackFormData().getModelScaling()[2];
		} else if (character.hasActiveForm() && character.getActiveFormData() != null) {
			sX = character.getActiveFormData().getModelScaling()[0];
			sY = character.getActiveFormData().getModelScaling()[1];
			sZ = character.getActiveFormData().getModelScaling()[2];
		} else {
			sX = character.getModelScaling()[0];
			sY = character.getModelScaling()[1];
			sZ = character.getModelScaling()[2];
		}

		String currentForm = character.getActiveForm() != null ? character.getActiveForm().toLowerCase() : "";
		if (currentForm.contains("ozaru")) {
			sX = Math.max(0.1f, sX - 2.8f);
			sY = Math.max(0.1f, sY - 2.8f);
			sZ = Math.max(0.1f, sZ - 2.8f);
		}

		return new float[]{sX, sY, sZ};
	}

	private static boolean characterHasAuraColor(Character character) {
		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null && character.getActiveStackFormData().getAuraColor() != null) return true;
		if (character.hasActiveForm() && character.getActiveFormData() != null && character.getActiveFormData().getAuraColor() != null) return true;
		return character.getAuraColor() != null;
	}

	private static int getAuraColor(Character character) {
		String hex = character.getAuraColor();
		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null && character.getActiveStackFormData().getAuraColor() != null) {
			hex = character.getActiveStackFormData().getAuraColor();
		} else if (character.hasActiveForm() && character.getActiveFormData() != null && character.getActiveFormData().getAuraColor() != null) {
			hex = character.getActiveFormData().getAuraColor();
		}
		return ColorUtils.hexToInt(hex != null ? hex : "#FFFFFF");
	}

	private static void spawnCalmAuraParticle(Player player, float totalScale, int colorHex) {
		var mc = Minecraft.getInstance();
		var random = player.getRandom();

		float r = ((colorHex >> 16) & 0xFF) / 255f;
		float g = ((colorHex >> 8) & 0xFF) / 255f;
		float b = (colorHex & 0xFF) / 255f;

		int particlesCount = 0 + random.nextInt(3);

		for (int i = 0; i < particlesCount; i++) {
			double radius = (0.15f + random.nextDouble() * 0.45f) * totalScale;
			double angle = random.nextDouble() * 2 * Math.PI;

			double offsetX = Math.cos(angle) * radius;
			double offsetZ = Math.sin(angle) * radius;
			double heightOffset = (random.nextDouble() * 2.0f) * totalScale;

			double x = player.getX() + offsetX;
			double y = player.getY() + heightOffset;
			double z = player.getZ() + offsetZ;

			Particle p = mc.particleEngine.createParticle(MainParticles.AURA.get(), x, y, z, r, g, b);

			if (p instanceof AuraParticle auraP) {
				auraP.resize(totalScale);
				double driftSpeed = 0.03f;
				double velX = (offsetX / radius) * driftSpeed;
				double velZ = (offsetZ / radius) * driftSpeed;
				double velY = 0.02f + (random.nextDouble() * 0.04f);
				auraP.setParticleSpeed(velX, velY, velZ);
			}
		}
	}

	private static void spawnPassiveDivineParticle(Player player, float totalScale, int colorHex) {
		var random = player.getRandom();

		double widthSpread = player.getBbWidth() * totalScale * 2.0;
		double offsetX = (random.nextDouble() - 0.5) * widthSpread;
		double offsetZ = (random.nextDouble() - 0.5) * widthSpread;

		double x = player.getX() + offsetX;
		double z = player.getZ() + offsetZ;
		double heightSpread = (random.nextDouble() * 1.2) * totalScale;
		double y = player.getY() + heightSpread;

		float r = ((colorHex >> 16) & 0xFF) / 255f;
		float g = ((colorHex >> 8) & 0xFF) / 255f;
		float b = (colorHex & 0xFF) / 255f;

		Particle p = Minecraft.getInstance().particleEngine.createParticle(MainParticles.DIVINE.get(), x, y, z, r, g, b);

		if (p instanceof DivineParticle divineP) {
			divineP.resize(totalScale);
			double velY = 0.02 + (random.nextDouble() * 0.03);
			divineP.setParticleSpeed(0, velY, 0);
		}
	}

	private static void spawnGroundDust(Player player, float totalScale) {
		if (player.getRandom().nextFloat() > 0.5f) return;
		var level = player.level();
		var random = player.getRandom();

		for (int i = 0; i < 8; i++) {
			double angle = random.nextDouble() * 2 * Math.PI;
			double radius = (0.4f + random.nextDouble() * 0.7f) * totalScale;

			double offsetX = Math.cos(angle) * radius;
			double offsetZ = Math.sin(angle) * radius;

			double x = player.getX() + offsetX;
			double y = player.getY() + 0.15;
			double z = player.getZ() + offsetZ;

			double speedBase = 0.12f;
			double velX = Math.cos(angle) * speedBase;
			double velY = 0.05f + (random.nextDouble() * 0.1f);
			double velZ = Math.sin(angle) * speedBase;

			level.addParticle(MainParticles.DUST.get(), x, y, z, velX, velY, velZ);
		}
	}

	private static void spawnFloatingRubble(Player player, float totalScale) {
		if (player.getRandom().nextFloat() > 0.4f) return;
		var level = player.level();
		var random = player.getRandom();
		int rocksCount = 2 + random.nextInt(3);

		for (int i = 0; i < rocksCount; i++) {
			double angle = random.nextDouble() * 2 * Math.PI;
			double radius = (0.4f + random.nextDouble() * 1.5f) * totalScale;

			double offsetX = Math.cos(angle) * radius;
			double offsetZ = Math.sin(angle) * radius;

			double x = player.getX() + offsetX;
			double y = player.getY() + 0.1;
			double z = player.getZ() + offsetZ;

			double velX = (random.nextDouble() - 0.5) * 0.08;
			double velZ = (random.nextDouble() - 0.5) * 0.08;
			double velY = 0.08 + (random.nextDouble() * 0.15);

			level.addParticle(MainParticles.ROCK.get(), x, y, z, velX, velY, velZ);
		}
	}
}