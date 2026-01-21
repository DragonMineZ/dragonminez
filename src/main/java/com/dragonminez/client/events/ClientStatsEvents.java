package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.network.C2S.ExecuteActionC2S;
import com.dragonminez.common.network.C2S.UpdateSkillC2S;
import com.dragonminez.common.network.C2S.UpdateStatC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.ActionMode;
import com.dragonminez.common.stats.Skill;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.events.players.StatsEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
	private static int transformDoubleTapTimer = 0;
	private static boolean wasTransformKeyDown = false;

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;

		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;

		if (player == null || mc.screen != null) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!data.getStatus().hasCreatedCharacter()) return;

			boolean isStunned = data.getStatus().isStunned();
			boolean mainHandEmpty = player.getMainHandItem().isEmpty();

			boolean isRightClickDown = mc.options.keyUse.isDown();

			boolean shouldBlock = isRightClickDown && mainHandEmpty && !isStunned;

			if (shouldBlock != data.getStatus().isBlocking()) {
				data.getStatus().setBlocking(shouldBlock);
				NetworkHandler.sendToServer(new UpdateStatC2S("isBlocking", shouldBlock));
			}

			if (data.getSkills().isSkillActive("kaioken")) {
				if (player.hurtTime > 0 && player.invulnerableTime == 0) {
					player.hurtTime = 0;
					player.hurtDuration = 0;
					player.attackAnim = 0;
				}
			}
		});
	}

	@SubscribeEvent
	public static void onKeyPressed(InputEvent.Key event) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!data.getStatus().hasCreatedCharacter()) return;
			boolean isStunned = data.getStatus().isStunned();

			boolean isKiChargeKeyPressed = KeyBinds.KI_CHARGE.isDown() && !isStunned;
			boolean isDescendKeyPressed = KeyBinds.DESCEND_KEY.isDown() && !isStunned;
			boolean isActionKeyPressed = KeyBinds.TRANSFORM_KEY.isDown() && !isStunned;

			if (transformDoubleTapTimer > 0) {
				transformDoubleTapTimer--;
			}

			if (isActionKeyPressed && !wasTransformKeyDown) {
				if (transformDoubleTapTimer > 0) {
					NetworkHandler.sendToServer(new ExecuteActionC2S("instant_transform"));
					transformDoubleTapTimer = 0;
				} else {
					transformDoubleTapTimer = 8;
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

			if (isDescendKeyPressed && isActionKeyPressed && (data.getStatus().getSelectedAction().equals(ActionMode.FORM) || data.getStatus().getSelectedAction().equals(ActionMode.KAIOKEN))) {
				NetworkHandler.sendToServer(new ExecuteActionC2S("descend"));
			}

			if (KeyBinds.KI_SENSE.consumeClick()) {
				Skill kiSense = data.getSkills().getSkill("kisense");
				if (kiSense == null) return;
				int kiSenseLevel = kiSense.getLevel();
				if (kiSenseLevel > 0) {
					NetworkHandler.sendToServer(new UpdateSkillC2S("toggle", kiSense.getName(), 0));
				}
			}

			if (KeyBinds.LOCK_ON_TARGET.consumeClick() && !isStunned) {
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
				AttributeModifier mod = speedAttr.getModifier(StatsEvents.FORM_SPEED_UUID);
				if (mod != null) {
					double factor = 1.0 + mod.getAmount();
					if (factor > 1.0) {
						float newFov = (float) (event.getFovModifier() / factor);
						event.setNewFovModifier(newFov);
					}
				}
			}
		}
	}


}
