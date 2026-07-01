package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class MoreNode extends AbstractRadialNode {

	private final String categoryKey;
	private final List<RadialNode> options;

	public MoreNode(String categoryKey, List<RadialNode> options) {
		this.categoryKey = categoryKey;
		this.options = options;
	}

	public String categoryKey() {
		return categoryKey;
	}

	public List<RadialNode> options() {
		return options;
	}

	@Override
	public Component label(StatsData stats) {
		return Component.translatable("gui.dragonminez.radial.more");
	}

	@Override
	public ResourceLocation icon(StatsData stats) {
		return icon("radial_more");
	}

	@Override
	public boolean expandable(StatsData stats) {
		return false;
	}
}
