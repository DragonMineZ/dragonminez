package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.common.network.C2S.FlightModeLockC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class FlightModeLockNode extends AbstractRadialNode {
	@Override
	public Component label(StatsData stats) {
		return Component.translatable(stats.getStatus().isFlightModeLocked()
				? "gui.dragonminez.radial.flight_mode.locked"
				: "gui.dragonminez.radial.flight_mode.unlocked");
	}

	@Override
	public ResourceLocation icon(StatsData stats) {
		return icon(stats.getStatus().isFlightModeLocked() ? "flight_locked" : "flight_unlocked");
	}

	@Override
	public boolean visible(StatsData stats) {
		return stats.getSkills().hasSkill("fly");
	}

	@Override
	public boolean active(StatsData stats) {
		return stats.getStatus().isFlightModeLocked();
	}

	@Override
	public int labelColor(StatsData stats) {
		return active(stats) ? GREEN : RED;
	}

	@Override
	public void onSelect(StatsData stats) {
		NetworkHandler.sendToServer(new FlightModeLockC2S());
		playToggle(!active(stats));
	}
}
