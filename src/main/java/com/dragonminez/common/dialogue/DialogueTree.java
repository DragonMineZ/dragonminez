package com.dragonminez.common.dialogue;

import com.dragonminez.common.quest.QuestPrerequisites;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class DialogueTree {

	private final String npcId;
	private final String startNodeId;
	private final Map<String, DialogueNode> nodes;

	public DialogueTree(String npcId, String startNodeId, Map<String, DialogueNode> nodes) {
		this.npcId = npcId;
		this.startNodeId = startNodeId;
		this.nodes = nodes;
	}

	public DialogueNode getNode(String nodeId) {
		return nodeId == null ? null : nodes.get(nodeId);
	}

	@Getter
	public static class DialogueNode {
		private final String id;
		private final String line;
		private final List<DialogueChoice> choices;

		public DialogueNode(String id, String line, List<DialogueChoice> choices) {
			this.id = id;
			this.line = line;
			this.choices = choices;
		}
	}

	@Getter
	public static class DialogueChoice {
		private final String text;
		private final String gotoNodeId;
		private final QuestPrerequisites conditions;
		private final List<DialogueAction> actions;

		public DialogueChoice(String text, String gotoNodeId, QuestPrerequisites conditions, List<DialogueAction> actions) {
			this.text = text;
			this.gotoNodeId = gotoNodeId;
			this.conditions = conditions;
			this.actions = actions;
		}
	}
}
