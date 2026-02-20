package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.flight.FlightSoundInstance;
import com.dragonminez.client.gui.hud.ScouterHUD;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.C2S.*;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.*;
import com.dragonminez.server.events.players.StatsEvents;
import com.dragonminez.server.util.GravityLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientStatsEvents {
	private static FlightSoundInstance flightSound;

	private static int transformDoubleTapTimer = 0;
	private static int kiBlastTimer = 0;
	private static boolean wasTransformKeyDown = false;
	private static long lastDashTime = 0;
	private static boolean wasDashKeyDown = false;
	private static boolean wasRightClickDown = false;
	private static boolean wasDescendActionDown = false;

	@SubscribeEvent
	public static void onMouseInput(InputEvent.MouseButton.Pre event) {
		if (Minecraft.getInstance().player == null) return;
		if (!ConfigManager.getServerConfig().getCombat().isEnableComboAttacks()) return;

		StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player).ifPresent(data -> {
			if (!data.getStatus().hasCreatedCharacter()) return;
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
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;

		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;

		if (player == null || mc.screen != null) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!data.getStatus().hasCreatedCharacter()) return;

			boolean isStunned = data.getStatus().isStunned();
			boolean isKiChargeKeyPressed = KeyBinds.KI_CHARGE.isDown() && !isStunned;
			boolean isDescendKeyPressed = KeyBinds.SECOND_FUNCTION_KEY.isDown() && !isStunned;
			boolean isActionKeyPressed = KeyBinds.ACTION_KEY.isDown() && !isStunned;
			boolean mainHandEmpty = player.getMainHandItem().isEmpty();
			boolean offHandEmpty = player.getOffhandItem().isEmpty();
			boolean isRightClickDown = mc.options.keyUse.isDown();

			boolean shouldBlock = isRightClickDown && mainHandEmpty && offHandEmpty && !isStunned && !isDescendKeyPressed;
			if (shouldBlock != data.getStatus().isBlocking()) {
				data.getStatus().setBlocking(shouldBlock);
				NetworkHandler.sendToServer(new UpdateStatC2S("isBlocking", shouldBlock));
			}

			if (isDescendKeyPressed && isRightClickDown && !wasRightClickDown && mainHandEmpty) {
				String hexColor = data.getCharacter().getAuraColor();
				int colorMain = ColorUtils.hexToInt(hexColor);
				int colorBorder = ColorUtils.darkenColor(colorMain, 0.85f);
				NetworkHandler.sendToServer(new KiBlastC2S(true, colorMain, colorBorder));
				kiBlastTimer = 10;
			}
			wasRightClickDown = isRightClickDown;

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
					NetworkHandler.sendToServer(new ExecuteActionC2S("instant_transform"));
					transformDoubleTapTimer = 0;
				} else {
					transformDoubleTapTimer = 20;
				}
			}
			wasTransformKeyDown = isActionKeyPressed;

			if (isKiChargeKeyPressed != data.getStatus().isChargingKi()) {
				NetworkHandler.sendToServer(new UpdateStatC2S("isChargingKi", isKiChargeKeyPressed));
			}

			if (isDescendKeyPressed != data.getStatus().isDescending()) {
				NetworkHandler.sendToServer(new UpdateStatC2S("isDescending", isDescendKeyPressed));
			}

			if (isActionKeyPressed != data.getStatus().isActionCharging()) {
				NetworkHandler.sendToServer(new UpdateStatC2S("isActionCharging", isActionKeyPressed));
			}

			boolean isDescendActionDown = isDescendKeyPressed && isActionKeyPressed;
			if (isDescendActionDown && !wasDescendActionDown && (data.getStatus().getSelectedAction().equals(ActionMode.FORM) || data.getStatus().getSelectedAction().equals(ActionMode.STACK))) {
				NetworkHandler.sendToServer(new ExecuteActionC2S("descend"));
			}
			wasDescendActionDown = isDescendActionDown;

			boolean isFlying = data.getSkills().isSkillActive("fly") && !player.onGround() && !player.isInWater();

			if (isFlying) {
				if (flightSound == null || !mc.getSoundManager().isActive(flightSound)) {
					flightSound = new FlightSoundInstance(player);
					mc.getSoundManager().play(flightSound);
				}
			} else {
				flightSound = null;
			}

			boolean hasScouter = player.getItemBySlot(EquipmentSlot.HEAD).getDescriptionId().contains("scouter");
			if (KeyBinds.KI_SENSE.consumeClick()) {
				if (!hasScouter) {
					Skill kiSense = data.getSkills().getSkill("kisense");
					if (kiSense == null) return;
					int kiSenseLevel = kiSense.getLevel();
					if (kiSenseLevel > 0) NetworkHandler.sendToServer(new UpdateSkillC2S("toggle", kiSense.getName(), 0));
				} else {
					ScouterHUD.setRenderingInfo(!ScouterHUD.isRenderingInfo());
				}
			}

			if (hasScouter) {
				if (ScouterHUD.getScouterColor() != player.getItemBySlot(EquipmentSlot.HEAD).getItem()) ScouterHUD.setScouterColor(player.getItemBySlot(EquipmentSlot.HEAD).getItem());
			}
		});
	}

	@SubscribeEvent
	public static void onKeyPressed(InputEvent.Key event) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {if (!data.getStatus().hasCreatedCharacter()) return;
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
}