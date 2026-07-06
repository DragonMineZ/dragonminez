package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;

import java.util.List;

public class StackSkillNode extends CategoryNode {

	public StackSkillNode() {
		super(Component.translatable("gui.dragonminez.radial.stackskills"), icon("kaioken"));
	}

	@Override
	protected List<RadialNode> buildChildren(StatsData stats) {
		return RadialForms.stackForms(stats);
	}
}
