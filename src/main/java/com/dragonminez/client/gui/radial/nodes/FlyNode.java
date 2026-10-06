package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;

import java.util.List;

public class FlyNode extends SkillToggleNode {

	public FlyNode() {
		super(Component.translatable("skill.dragonminez.fly"), icon("fly"),
				FlyNode::canFly,
				s -> s.getSkills().isSkillActive("fly"),
				(s, was) -> FlySkillEvent.toggleFlightFromMenu());
	}

	public static boolean canFly(StatsData stats) {
		return stats.getSkills().getSkillLevel("fly") > 0 && stats.getSkills().getSkillLevel("kicontrol") > 0;
	}

	@Override
	protected List<RadialNode> buildChildren(StatsData stats) {
		return List.of(new FlightModeNode(), new FlightSpeedNode(), new FlightModeLockNode());
	}
}
