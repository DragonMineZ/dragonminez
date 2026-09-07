package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.common.network.C2S.NamekRegenC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Cooldowns;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class NamekRegenNode extends AbstractRadialNode {

	@Override
	public Component label(StatsData stats) {
		return Component.translatable("gui.action.dragonminez.racial.namekian.regen");
	}

	@Override
	public ResourceLocation icon(StatsData stats) {
		return icon("racial");
	}

	@Override
	public boolean visible(StatsData stats) {
		return "namekian".equals(stats.getCharacter().getRaceName());
	}

	@Override
	public boolean active(StatsData stats) {
		return stats.getCooldowns().hasCooldown(Cooldowns.NAMEK_REGEN_ACTIVE);
	}

	@Override
	public int labelColor(StatsData stats) {
		if (stats.getCooldowns().hasCooldown(Cooldowns.NAMEK_REGEN)) return RED;
		return active(stats) ? GREEN : RED;
	}

	@Override
	public void onSelect(StatsData stats) {
		if (stats.getCooldowns().hasCooldown(Cooldowns.NAMEK_REGEN)) return;
		NetworkHandler.sendToServer(new NamekRegenC2S());
		playToggle(true);
	}
}
