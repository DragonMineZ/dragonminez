package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.flight.FlightSoundInstance;
import com.dragonminez.client.gui.TrainingScreen;
import com.dragonminez.client.gui.hud.ScouterHUD;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.particles.AuraParticle;
import com.dragonminez.common.init.particles.DivineParticle;
import com.dragonminez.common.network.C2S.*;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.stats.skills.Skill;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.dragonminez.common.util.BetaWhitelist;
import com.dragonminez.common.util.lists.SaiyanForms;
import com.dragonminez.server.events.players.StatsEvents;
import com.dragonminez.server.util.GravityLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Objects;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientStatsEvents {
	private static FlightSoundInstance flightSound;

	private static int transformDoubleTapTimer = 0;
	private static int kiChargeDoubleTapTimer = 0;
	private static int kiBlastTimer = 0;
	private static boolean wasTransformKeyDown = false;
	private static boolean wasKiChargeKeyDown = false;
	private static long lastDashTime = 0;
	private static boolean wasDashKeyDown = false;
	private static boolean wasRightClickDown = false;
	private static boolean wasDescendActionDown = false;
	private static boolean wasTechniqueChargeDown = false;
	private static int lockedVanillaHotbarSlot = -1;
	private static int lockedTechniqueSlot = -1;

	@SubscribeEvent
	public static void onMouseInput(InputEvent.MouseButton.Pre event) {
		if (Minecraft.getInstance().player == null) return;
		if (!ConfigManager.getServerConfig().getCombat().getEnableComboAttacks()) return;

		StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;
			if (data.getStatus().isStunned()) return;
			if (data.getCooldowns().hasCooldown(Cooldowns.COMBO_ATTACK_CD)) return;

			if (event.getButton() == 0 && event.getAction() == 1) {
				if (KeyBinds.SECOND_FUNCTION_KEY.isDown()) {
					NetworkHandler.sendToServer(new ComboAttackC2S());
				}
			}
		});
	}

	@SubscribeEvent
	public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
		if (Minecraft.getInstance().player != null) {
			StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player).ifPresent(data -> {
				boolean isChargingTechnique = data.getTechniques().isTechniqueCharging() || data.getTechniques().isTechniqueChargeActive();
				if (isChargingTechnique) {
					event.setCanceled(true);
				}
			});
			if (event.isCanceled()) return;
		}

		if (KeyBinds.SECOND_FUNCTION_KEY.isDown() && Minecraft.getInstance().player != null) {
			StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player).ifPresent(data -> {
				boolean isChargingTechnique = data.getTechniques().isTechniqueCharging() || data.getTechniques().isTechniqueChargeActive();
				if (isChargingTechnique) {
					event.setCanceled(true);
					return;
				}

				int delta = (int) Math.signum(event.getScrollDelta());
				if (delta != 0) {
					int currentSlot = data.getTechniques().getSelectedSlot();
					int newSlot = currentSlot - delta;

					if (newSlot < 0) newSlot = 7;
					if (newSlot > 7) newSlot = 0;

					data.getTechniques().selectSlot(newSlot);
					NetworkHandler.sendToServer(new SelectTechniqueSlotC2S(newSlot));

					event.setCanceled(true);
				}
			});
		}
	}

	@SubscribeEvent
	public static void onKeyInputHotbar(InputEvent.Key event) {
		if (Minecraft.getInstance().player == null) return;

		if (event.getAction() == 1) {
			int key = event.getKey();
			if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_8) {
				StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player).ifPresent(data -> {
					boolean isChargingTechnique = data.getTechniques().isTechniqueCharging() || data.getTechniques().isTechniqueChargeActive();
					if (isChargingTechnique) {
						event.setCanceled(true);
					}
				});
				if (event.isCanceled()) return;
			}
		}

		if (event.getAction() == 1 && KeyBinds.SECOND_FUNCTION_KEY.isDown()) {
			int key = event.getKey();
			if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_8) {
				int slot = key - GLFW.GLFW_KEY_1;
				StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player).ifPresent(data -> {
					data.getTechniques().selectSlot(slot);
					NetworkHandler.sendToServer(new SelectTechniqueSlotC2S(slot));
				});
			}
		}
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;

		Minecraft mc = Minecraft.getInstance();
		LocalPlayer localPlayer = mc.player;

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
					for (int i = 0; i < divineCount; i++) {
						spawnPassiveDivineParticle(player, totalScale, 0xFFFFFF);
					}
				}
			}
		}

		if (localPlayer == null || mc.screen != null) return;

		StatsProvider.get(StatsCapability.INSTANCE, localPlayer).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;
			Character character = data.getCharacter();
			TechniqueData selectedTechnique = data.getTechniques().getSelectedTechnique();
			boolean hasSelectedKiTechnique = selectedTechnique instanceof KiAttackData;
			boolean isChargingTechnique = data.getTechniques().isTechniqueCharging() || data.getTechniques().isTechniqueChargeActive();

			if (isChargingTechnique) {
				if (lockedVanillaHotbarSlot == -1) lockedVanillaHotbarSlot = localPlayer.getInventory().selected;
				if (localPlayer.getInventory().selected != lockedVanillaHotbarSlot) {
					localPlayer.getInventory().selected = lockedVanillaHotbarSlot;
				}

				if (lockedTechniqueSlot == -1) lockedTechniqueSlot = data.getTechniques().getSelectedSlot();
				if (data.getTechniques().getSelectedSlot() != lockedTechniqueSlot) {
					data.getTechniques().selectSlot(lockedTechniqueSlot);
					NetworkHandler.sendToServer(new SelectTechniqueSlotC2S(lockedTechniqueSlot));
				}
			} else {
				lockedVanillaHotbarSlot = -1;
				lockedTechniqueSlot = -1;
			}

			boolean isStunned = data.getStatus().isStunned();
			boolean isKiChargeKeyPressed = KeyBinds.KI_CHARGE.isDown() && !isStunned;
			boolean isDescendKeyPressed = KeyBinds.SECOND_FUNCTION_KEY.isDown() && !isStunned;
			boolean isActionKeyPressed = KeyBinds.ACTION_KEY.isDown() && !isStunned;
			boolean mainHandEmpty = localPlayer.getMainHandItem().isEmpty();
			boolean offHandEmpty = localPlayer.getOffhandItem().isEmpty();
			boolean isRightClickDown = mc.options.keyUse.isDown();

			boolean shouldBlock = isRightClickDown && mainHandEmpty && offHandEmpty && !isStunned && !isDescendKeyPressed;
			if (shouldBlock != data.getStatus().isBlocking()) {
				data.getStatus().setBlocking(shouldBlock);
				NetworkHandler.sendToServer(new UpdateStatC2S(UpdateStatC2S.StatAction.BLOCK, shouldBlock));
			}

			if (isDescendKeyPressed && isRightClickDown && !wasRightClickDown && mainHandEmpty && !hasSelectedKiTechnique) {
				String kiHex;
				if (character.hasActiveStackForm()
						&& character.getActiveStackFormData() != null
						&& character.getActiveStackFormData().getAuraColor() != null
						&& !character.getActiveStackFormData().getAuraColor().isEmpty()) {
					kiHex = character.getActiveStackFormData().getAuraColor();
				} else if (character.hasActiveForm()
						&& character.getActiveFormData() != null
						&& character.getActiveFormData().getAuraColor() != null
						&& !character.getActiveFormData().getAuraColor().isEmpty()) {
					kiHex = character.getActiveFormData().getAuraColor();
				} else {
					kiHex = character.getAuraColor();
				}
				int colorMain = ColorUtils.hexToInt(kiHex);
				int colorBorder = ColorUtils.darkenColor(colorMain, 0.85f);
				NetworkHandler.sendToServer(new KiBlastC2S(true, colorMain, colorBorder));
				kiBlastTimer = 10;
			}
			wasRightClickDown = isRightClickDown;

			boolean techniqueChargeDown = isDescendKeyPressed && isRightClickDown && mainHandEmpty && offHandEmpty && hasSelectedKiTechnique;

			if (techniqueChargeDown && !wasTechniqueChargeDown) {
				NetworkHandler.sendToServer(new TechniqueChargeC2S(true));
			} else if (!techniqueChargeDown && wasTechniqueChargeDown) {
				NetworkHandler.sendToServer(new TechniqueChargeC2S(false));
			}
			wasTechniqueChargeDown = techniqueChargeDown;

			if (kiBlastTimer > 0) {
				if (kiBlastTimer == 1) {
					NetworkHandler.sendToServer(new KiBlastC2S(false, 0, 0));
				}
				kiBlastTimer--;
			}

			if (transformDoubleTapTimer > 0) {
				transformDoubleTapTimer--;
			}

			if (isActionKeyPressed && !wasTransformKeyDown) {
				if (transformDoubleTapTimer > 0) {
					NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.INSTANT_TRANSFORM));
					transformDoubleTapTimer = 0;
				} else transformDoubleTapTimer = 10;
			}
			wasTransformKeyDown = isActionKeyPressed;

			if (isKiChargeKeyPressed && !wasKiChargeKeyDown) {
				if (kiChargeDoubleTapTimer > 0) {
					NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.INSTANT_RELEASE));
					kiChargeDoubleTapTimer = 0;
				} else kiChargeDoubleTapTimer = 10;
			}
			wasKiChargeKeyDown = isKiChargeKeyPressed;

			if (isKiChargeKeyPressed != data.getStatus().isChargingKi()) {
				NetworkHandler.sendToServer(new UpdateStatC2S(UpdateStatC2S.StatAction.CHARGE_KI, isKiChargeKeyPressed));
			}

			if (isDescendKeyPressed != data.getStatus().isDescending()) {
				NetworkHandler.sendToServer(new UpdateStatC2S(UpdateStatC2S.StatAction.DESCEND, isDescendKeyPressed));
			}

			if (isActionKeyPressed != data.getStatus().isActionCharging()) {
				NetworkHandler.sendToServer(new UpdateStatC2S(UpdateStatC2S.StatAction.ACTION_CHARGE, isActionKeyPressed));
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

			boolean hasScouter = localPlayer.getItemBySlot(EquipmentSlot.HEAD).getDescriptionId().contains("scouter");
			if (KeyBinds.KI_SENSE.consumeClick()) {
				if (!hasScouter) {
					Skill kiSense = data.getSkills().getSkill("kisense");
					if (kiSense == null) return;
					int kiSenseLevel = kiSense.getLevel();
					if (kiSenseLevel > 0)
						NetworkHandler.sendToServer(new UpdateSkillC2S(UpdateSkillC2S.SkillAction.TOGGLE, kiSense.getName(), 0));
				} else {
					ScouterHUD.setRenderingInfo(!ScouterHUD.isRenderingInfo());
				}
			}

			if (hasScouter) {
				if (ScouterHUD.getScouterColor() != localPlayer.getItemBySlot(EquipmentSlot.HEAD).getItem())
					ScouterHUD.setScouterColor(localPlayer.getItemBySlot(EquipmentSlot.HEAD).getItem());
			}

			if (!(mc.screen instanceof TrainingScreen)) {
				if (!data.getTraining().getCurrentTrainingStat().isEmpty()) {
					data.getTraining().setCurrentTrainingStat("");
					NetworkHandler.sendToServer(new TrainingRewardC2S(TrainingRewardC2S.TrainStat.NONE, -1));
				}
			}
		});
	}

	@SubscribeEvent
	public static void onKeyPressed(InputEvent.Key event) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;
			boolean isStunned = data.getStatus().isStunned();

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
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				var character = data.getCharacter();
				var activeForm = character.getActiveFormData();
				String currentForm = character.getActiveForm();
				String race = character.getRaceName().toLowerCase();

				var raceConfig = ConfigManager.getRaceCharacter(race);
				String raceCustomModel = (raceConfig != null && raceConfig.getCustomModel() != null) ? raceConfig.getCustomModel().toLowerCase() : "";
				String formCustomModel = (character.hasActiveForm() && activeForm != null && activeForm.hasCustomModel())
						? activeForm.getCustomModel().toLowerCase() : "";

				String logicKey = formCustomModel.isEmpty() ? raceCustomModel : formCustomModel;
				if (logicKey.isEmpty()) {
					logicKey = race;
				}

				boolean isOozaru = logicKey.startsWith("oozaru") ||
						(race.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU)));

				if (isOozaru) {
					float newFov = event.getFovModifier() * 1.5f;
					event.setNewFovModifier(newFov);
				}
			});

			AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
			if (speedAttr != null) {
				AttributeModifier formMod = speedAttr.getModifier(StatsEvents.FORM_SPEED_UUID);
				if (formMod != null) {
					double factor = 1.0 + formMod.getAmount();
					if (factor > 1.0) {
						float newFov = (float) (event.getFovModifier() / factor);
						event.setNewFovModifier(newFov);
					}
				}
				AttributeModifier gravityMod = speedAttr.getModifier(GravityLogic.GRAVITY_SPEED_UUID);
				if (gravityMod != null) {
					double factor = 1.0 + Math.abs(gravityMod.getAmount());
					if (factor > 1.0) {
						float newFov = (float) (event.getFovModifier() * factor);
						event.setNewFovModifier(newFov);
					}
				}
			}
		}
	}

	@SubscribeEvent
	public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		wasTechniqueChargeDown = false;
		wasRightClickDown = false;
		lockedVanillaHotbarSlot = -1;
		lockedTechniqueSlot = -1;
		StatsCapability.clearClientCache();
	}

	private static float[] getBodyScale(StatsData stats) {
		float sX = 1.0f, sY = 1.0f, sZ = 1.0f;
		var character = stats.getCharacter();

		if (character.hasActiveForm() && character.getActiveFormData() != null) {
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