package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class EmptyNode extends AbstractRadialNode {

	@Override
	public Component label(StatsData stats) {
		return Component.empty();
	}

	@Override
	public ResourceLocation icon(StatsData stats) {
		return null;
	}

	@Override
	public boolean interactive(StatsData stats) {
		return false;
	}
}
