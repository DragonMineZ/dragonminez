package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.network.C2S.UpdateSkillC2S;
import com.dragonminez.common.network.C2S.UpdateStatC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.Skill;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientStatsEvents {

	@SubscribeEvent
	public static void onKeyPressed(InputEvent.Key event) {
		boolean isKiChargeKeyPressed = KeyBinds.KI_CHARGE.isDown();
		boolean isDescendKeyPressed = KeyBinds.DESCEND_KEY.isDown();
		boolean isTransformKeyPressed = KeyBinds.TRANSFORM_KEY.isDown();

		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!data.getStatus().hasCreatedCharacter()) return;

			if (isKiChargeKeyPressed != data.getStatus().isChargingKi()) {
				NetworkHandler.sendToServer(new UpdateStatC2S("isChargingKi", isKiChargeKeyPressed));
			}

			if (isDescendKeyPressed != data.getStatus().isDescending()) {
				NetworkHandler.sendToServer(new UpdateStatC2S("isDescending", isDescendKeyPressed));
			}

			if (isTransformKeyPressed != data.getStatus().isTransforming()) {
				NetworkHandler.sendToServer(new UpdateStatC2S("isTransforming", isTransformKeyPressed));
			}

			if (KeyBinds.KI_SENSE.consumeClick()) {
				Skill kiSense = data.getSkills().getSkill("kisense");
				if (kiSense == null) return;
				int kiSenseLevel = kiSense.getLevel();
				if (kiSenseLevel > 0) {
					NetworkHandler.sendToServer(new UpdateSkillC2S("toggle", kiSense.getName(), 0));
				}
			}

			if (KeyBinds.LOCK_ON_TARGET.consumeClick()) {
				Skill kiSense = data.getSkills().getSkill("kisense");
				if (kiSense == null) return;
				LockOnEvent.toggleLock();
			}
		});
	}
}
