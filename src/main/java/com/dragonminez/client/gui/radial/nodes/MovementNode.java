package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.common.network.C2S.UpdateSkillC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class MovementNode extends CategoryNode {

	public MovementNode() {
		super(Component.translatable("gui.dragonminez.radial.movement"), icon("movement"));
	}

	@Override
	protected List<RadialNode> buildChildren(StatsData stats) {
		List<RadialNode> out = new ArrayList<>();

		out.add(new SkillToggleNode(
				Component.translatable("skill.dragonminez.fly"), icon("fly"),
				s -> s.getSkills().hasSkill("fly"),
				s -> s.getSkills().isSkillActive("fly"),
				(s, was) -> FlySkillEvent.toggleFlightFromMenu()
		));

		out.add(new SkillToggleNode(
				Component.translatable("skill.dragonminez.jump"), icon("jump"),
				s -> s.getSkills().hasSkill("jump"),
				s -> s.getSkills().isSkillActive("jump"),
				(s, was) -> NetworkHandler.sendToServer(new UpdateSkillC2S(UpdateSkillC2S.SkillAction.TOGGLE, "jump", 0))
		));

		out.add(new SkillToggleNode(
				Component.translatable("skill.dragonminez.sprint"), icon("sprint"),
				s -> s.getSkills().hasSkill("sprint"),
				s -> s.getSkills().isSkillActive("sprint"),
				(s, was) -> NetworkHandler.sendToServer(new UpdateSkillC2S(UpdateSkillC2S.SkillAction.TOGGLE, "sprint", 0))
		));

		return out;
	}
}
