package com.dragonminez.common.dialogue;

import com.dragonminez.common.quest.QuestParser;
import com.dragonminez.common.quest.QuestPrerequisites;
import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lenient parser for world-editable dialogue JSON. Malformed nodes/choices/actions are skipped
 * with a log line instead of failing the whole file, matching QuestParser's tolerance for
 * player-authored content.
 */
public final class DialogueParser {

	private DialogueParser() {
	}

	public static DialogueTree parse(JsonObject json, String fileName) {
		if (json == null || !json.has("npc") || !json.has("nodes")) {
			LogUtil.error(Env.COMMON, "Dialogue file '{}' is missing required fields 'npc'/'nodes'", fileName);
			return null;
		}

		String npcId = json.get("npc").getAsString().toLowerCase(Locale.ROOT);
		String start = json.has("start") ? json.get("start").getAsString() : "start";

		Map<String, DialogueTree.DialogueNode> nodes = new LinkedHashMap<>();
		JsonObject nodesJson = json.getAsJsonObject("nodes");
		for (Map.Entry<String, JsonElement> entry : nodesJson.entrySet()) {
			if (!entry.getValue().isJsonObject()) continue;
			DialogueTree.DialogueNode node = parseNode(entry.getKey(), entry.getValue().getAsJsonObject(), fileName);
			if (node != null) nodes.put(entry.getKey(), node);
		}

		if (nodes.isEmpty() || !nodes.containsKey(start)) {
			LogUtil.error(Env.COMMON, "Dialogue file '{}' has no valid start node '{}'", fileName, start);
			return null;
		}

		return new DialogueTree(npcId, start, nodes);
	}

	private static DialogueTree.DialogueNode parseNode(String nodeId, JsonObject json, String fileName) {
		if (!json.has("line")) {
			LogUtil.error(Env.COMMON, "Dialogue node '{}' in '{}' is missing 'line' — skipped", nodeId, fileName);
			return null;
		}
		String line = json.get("line").getAsString();

		List<DialogueTree.DialogueChoice> choices = new ArrayList<>();
		if (json.has("choices") && json.get("choices").isJsonArray()) {
			for (JsonElement element : json.getAsJsonArray("choices")) {
				if (!element.isJsonObject()) continue;
				DialogueTree.DialogueChoice choice = parseChoice(element.getAsJsonObject(), nodeId, fileName);
				if (choice != null) choices.add(choice);
			}
		}
		return new DialogueTree.DialogueNode(nodeId, line, choices);
	}

	private static DialogueTree.DialogueChoice parseChoice(JsonObject json, String nodeId, String fileName) {
		if (!json.has("text")) {
			LogUtil.error(Env.COMMON, "Dialogue choice without 'text' in node '{}' of '{}' — skipped", nodeId, fileName);
			return null;
		}
		String text = json.get("text").getAsString();
		String gotoNode = json.has("goto") && !json.get("goto").isJsonNull() ? json.get("goto").getAsString() : null;

		QuestPrerequisites conditions = null;
		if (json.has("conditions") && json.get("conditions").isJsonObject()) {
			try {
				conditions = QuestParser.parsePrerequisites(json.getAsJsonObject("conditions"));
			} catch (Exception e) {
				// A gate that fails to parse must not open unconditionally — drop the whole choice.
				LogUtil.error(Env.COMMON, "Invalid 'conditions' on a choice in node '{}' of '{}' ({}) — choice skipped. "
						+ "Condition fields must match quest prerequisite syntax (e.g. LEVEL uses \"minLevel\").",
						nodeId, fileName, e.toString());
				return null;
			}
		}

		List<DialogueAction> actions = new ArrayList<>();
		if (json.has("actions") && json.get("actions").isJsonArray()) {
			for (JsonElement element : json.getAsJsonArray("actions")) {
				if (!element.isJsonObject()) continue;
				DialogueAction action = parseAction(element.getAsJsonObject(), nodeId, fileName);
				if (action != null) actions.add(action);
			}
		}
		return new DialogueTree.DialogueChoice(text, gotoNode, conditions, actions);
	}

	private static DialogueAction parseAction(JsonObject json, String nodeId, String fileName) {
		if (!json.has("type")) return null;
		DialogueAction.Type type;
		try {
			type = DialogueAction.Type.valueOf(json.get("type").getAsString().trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			LogUtil.error(Env.COMMON, "Unknown dialogue action type '{}' in node '{}' of '{}' — skipped",
					json.get("type").getAsString(), nodeId, fileName);
			return null;
		}

		int amount = json.has("amount") ? json.get("amount").getAsInt() : 0;
		String value = json.has("command") ? json.get("command").getAsString()
				: json.has("quest") ? json.get("quest").getAsString()
				: json.has("value") ? json.get("value").getAsString()
				: "";
		return new DialogueAction(type, amount, value);
	}
}
