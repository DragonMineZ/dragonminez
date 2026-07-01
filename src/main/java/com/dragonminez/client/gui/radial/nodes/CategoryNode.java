package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public abstract class CategoryNode extends AbstractRadialNode {

	protected final Component label;
	protected final ResourceLocation icon;

	protected CategoryNode(Component label, ResourceLocation icon) {
		this.label = label;
		this.icon = icon;
	}

	@Override
	public Component label(StatsData stats) {
		return label;
	}

	@Override
	public ResourceLocation icon(StatsData stats) {
		return icon;
	}

	@Override
	public boolean visible(StatsData stats) {
		for (RadialNode child : children(stats)) {
			if (child.visible(stats)) return true;
		}
		return false;
	}
}
