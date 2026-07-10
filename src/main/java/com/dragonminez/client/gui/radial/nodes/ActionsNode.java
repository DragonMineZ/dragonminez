package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ActionsNode extends CategoryNode {

	public ActionsNode() {
		super(Component.translatable("gui.dragonminez.radial.actions"), icon("actions"));
	}

	@Override
	protected List<RadialNode> buildChildren(StatsData stats) {
		List<RadialNode> out = new ArrayList<>();
		out.add(new RacialSkillNode());
		out.add(new KiWeaponsNode());
		out.add(new KiActionsNode());
		out.add(new ReleaseNode());
		out.add(new FlightSpeedNode());
		return out;
	}
}
