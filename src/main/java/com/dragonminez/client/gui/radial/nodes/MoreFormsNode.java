package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;

import java.util.List;

public class MoreFormsNode extends CategoryNode {

	public MoreFormsNode() {
		super(Component.translatable("gui.dragonminez.radial.extraforms"), icon("godforms"));
	}

	@Override
	protected List<RadialNode> buildChildren(StatsData stats) {
		return RadialForms.moreForms(stats);
	}
}
