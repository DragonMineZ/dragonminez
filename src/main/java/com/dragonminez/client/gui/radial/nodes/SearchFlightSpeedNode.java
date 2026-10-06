package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.common.network.C2S.SetSearchFlightSpeedC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class SearchFlightSpeedNode extends AbstractRadialNode {
	@Override
	public Component label(StatsData stats) {
		return Component.translatable("gui.dragonminez.radial.search_flight_speed");
	}

	@Override
	public ResourceLocation icon(StatsData stats) {
		return null;
	}

	@Override
	public String faceText(StatsData stats) {
		return stats.getStatus().getSearchFlightSpeedLimit() + "%";
	}

	@Override
	public boolean active(StatsData stats) {
		return stats.getStatus().getSearchFlightSpeedLimit() < 100;
	}

	@Override
	public boolean visible(StatsData stats) {
		return stats.getSkills().hasSkill("fly");
	}

	@Override
	public int labelColor(StatsData stats) {
		return active(stats) ? GREEN : 0xFFFFFF;
	}

	public List<RadialNode> buildOptions(StatsData stats) {
		List<RadialNode> options = new ArrayList<>();
		for (int value = 100; value >= 5; value -= 5) options.add(new FlightSpeedLimitOptionNode(value));
		return options;
	}

	public void adjustByWheel(double delta) {
		if (delta == 0) return;
		NetworkHandler.sendToServer(new SetSearchFlightSpeedC2S(delta > 0 ? 5 : -5, true));
		playClick();
	}
}
