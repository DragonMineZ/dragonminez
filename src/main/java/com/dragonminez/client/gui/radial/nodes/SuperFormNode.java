package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;

import java.util.List;

public class SuperFormNode extends CategoryNode {

	public SuperFormNode() {
		super(Component.translatable("gui.dragonminez.radial.superforms"), icon("superforms"));
	}

	@Override
	protected List<RadialNode> buildChildren(StatsData stats) {
		return RadialForms.superForms(stats);
	}
}
