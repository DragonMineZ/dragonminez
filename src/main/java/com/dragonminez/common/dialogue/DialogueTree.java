package com.dragonminez.common.dialogue;

import com.dragonminez.common.quest.QuestPrerequisites;
import java.util.List;
import java.util.Map;

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


	public static class DialogueNode {
		private final String id;
		private final String line;
		private final List<DialogueChoice> choices;

		public DialogueNode(String id, String line, List<DialogueChoice> choices) {
			this.id = id;
			this.line = line;
			this.choices = choices;
		}

		public String getId() {
			return this.id;
		}

		public String getLine() {
			return this.line;
		}

		public List<DialogueChoice> getChoices() {
			return this.choices;
		}
	}


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

		public String getText() {
			return this.text;
		}

		public String getGotoNodeId() {
			return this.gotoNodeId;
		}

		public QuestPrerequisites getConditions() {
			return this.conditions;
		}

		public List<DialogueAction> getActions() {
			return this.actions;
		}
	}

	public String getNpcId() {
		return this.npcId;
	}

	public String getStartNodeId() {
		return this.startNodeId;
	}

	public Map<String, DialogueNode> getNodes() {
		return this.nodes;
	}
}
