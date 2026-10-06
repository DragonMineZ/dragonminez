package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.common.network.C2S.FlightModeC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Status;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class FlightModeNode extends AbstractRadialNode {
	@Override
	public Component label(StatsData stats) {
		return Component.translatable(stats.getStatus().getFlightMode() == Status.FLIGHT_COMBAT
				? "gui.dragonminez.radial.flight_mode.combat"
				: "gui.dragonminez.radial.flight_mode.search");
	}

	@Override
	public ResourceLocation icon(StatsData stats) {
		return icon("fly");
	}

	@Override
	public boolean visible(StatsData stats) {
		return stats.getSkills().hasSkill("fly");
	}

	@Override
	public boolean active(StatsData stats) {
		return stats.getStatus().getFlightMode() == Status.FLIGHT_COMBAT;
	}

	@Override
	public void onSelect(StatsData stats) {
		NetworkHandler.sendToServer(new FlightModeC2S(active(stats) ? Status.FLIGHT_SEARCH : Status.FLIGHT_COMBAT));
		playToggle(!active(stats));
	}
}
