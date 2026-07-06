package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.common.network.C2S.ExecuteActionC2S;
import com.dragonminez.common.network.C2S.UpdateSkillC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class KiActionsNode extends CategoryNode {

	public KiActionsNode() {
		super(Component.translatable("gui.dragonminez.radial.kiactions"), icon("kiactions"));
	}

	@Override
	protected List<RadialNode> buildChildren(StatsData stats) {
		List<RadialNode> out = new ArrayList<>();

		out.add(new SkillToggleNode(
				Component.translatable("skill.dragonminez.aurastatus"), icon("aura"),
				s -> s.getSkills().hasSkill("kicontrol"),
				s -> s.getStatus().isPermanentAura(),
				(s, was) -> NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.TOGGLE_AURA, was))
		));

		out.add(new SkillToggleNode(
				Component.translatable("skill.dragonminez.friendlyfist"), icon("friendlyfist"),
				s -> s.getSkills().hasSkill("kicontrol"),
				s -> s.getStatus().isFriendlyFistEnabled(),
				(s, was) -> NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.TOGGLE_FRIENDLY_FIST, was))
		));

		out.add(new SkillToggleNode(
				Component.translatable("skill.dragonminez.kiprotection"), icon("kiprotection"),
				s -> s.getSkills().hasSkill("kiprotection"),
				s -> s.getSkills().isSkillActive("kiprotection"),
				(s, was) -> NetworkHandler.sendToServer(new UpdateSkillC2S(UpdateSkillC2S.SkillAction.TOGGLE, "kiprotection", 0))
		));

		out.add(new SkillToggleNode(
				Component.translatable("skill.dragonminez.ki_infusion"), icon("kifist"),
				s -> s.getSkills().hasSkill("ki_infusion"),
				s -> s.getSkills().isSkillActive("ki_infusion"),
				(s, was) -> NetworkHandler.sendToServer(new UpdateSkillC2S(UpdateSkillC2S.SkillAction.TOGGLE, "ki_infusion", 0))
		));

		return out;
	}
}
