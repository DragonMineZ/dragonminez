package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.systems.kisense.CombatIndicators;
import com.dragonminez.client.systems.kisense.KiSenseScan;
import com.dragonminez.client.systems.kisense.KiSenseState;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class KiSenseEvent {

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null || mc.level == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) {
				KiSenseScan.clear();
				CombatIndicators.clear();
				return;
			}
			if (KiSenseState.isActive() && (!data.getSkills().isSkillActive("kisense") || data.getSkills().getSkillLevel("kisense") <= 0)) {
				KiSenseState.reset();
			}
			if (KiSenseState.isActive() && KiSenseScan.hasScouter(player)) {
				KiSenseState.set(KiSenseState.Mode.NONE);
				return;
			}
			KiSenseScan.tick(player, data, KiSenseState.getMode());
			if (KiSenseState.isCombat()) CombatIndicators.tick();
			else CombatIndicators.clear();
		});
	}
}
