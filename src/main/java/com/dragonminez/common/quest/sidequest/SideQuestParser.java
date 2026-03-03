package com.dragonminez.common.quest.sidequest;

import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestParser;
import com.dragonminez.common.quest.QuestReward;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class SideQuestParser {

	public static SideQuest parse(JsonObject json) {
		String id = json.get("id").getAsString();
		String name = json.get("name").getAsString();
		String description = json.get("description").getAsString();
		String category = json.has("category") ? json.get("category").getAsString() : "general";
		boolean parallelObjectives = json.has("parallelObjectives") && json.get("parallelObjectives").getAsBoolean();
		String questGiver = json.has("questGiver") ? json.get("questGiver").getAsString() : null;
		String turnIn = json.has("turnIn") ? json.get("turnIn").getAsString() : null;

		SideQuestPrerequisites prerequisites = null;
		if (json.has("prerequisites")) {
			prerequisites = parsePrerequisites(json.getAsJsonObject("prerequisites"));
		}

		List<QuestObjective> objectives = new ArrayList<>();
		if (json.has("objectives")) {
			JsonArray objArray = json.getAsJsonArray("objectives");
			for (JsonElement element : objArray) {
				QuestObjective obj = QuestParser.parseObjective(element.getAsJsonObject());
				if (obj != null) {
					objectives.add(obj);
				}
			}
		}

		List<QuestReward> rewards = new ArrayList<>();
		if (json.has("rewards")) {
			JsonArray rewardArray = json.getAsJsonArray("rewards");
			for (JsonElement element : rewardArray) {
				QuestReward reward = QuestParser.parseReward(element.getAsJsonObject());
				if (reward != null) {
					rewards.add(reward);
				}
			}
		}

		return new SideQuest(id, name, description, category, parallelObjectives, objectives, rewards, prerequisites, questGiver, turnIn);
	}

	private static SideQuestPrerequisites parsePrerequisites(JsonObject json) {
		SideQuestPrerequisites.Operator operator = SideQuestPrerequisites.Operator.AND;
		if (json.has("operator")) {
			String op = json.get("operator").getAsString().toUpperCase();
			if (op.equals("OR")) {
				operator = SideQuestPrerequisites.Operator.OR;
			}
		}

		List<SideQuestPrerequisites.Condition> conditions = new ArrayList<>();
		if (json.has("conditions")) {
			JsonArray condArray = json.getAsJsonArray("conditions");
			for (JsonElement element : condArray) {
				JsonObject condJson = element.getAsJsonObject();
				SideQuestPrerequisites.Condition condition = parseCondition(condJson);
				if (condition != null) {
					conditions.add(condition);
				}
			}
		}

		return new SideQuestPrerequisites(operator, conditions);
	}

	private static SideQuestPrerequisites.Condition parseCondition(JsonObject json) {
		// If it has an "operator" field, it's a nested group
		if (json.has("operator")) {
			SideQuestPrerequisites nested = parsePrerequisites(json);
			return SideQuestPrerequisites.Condition.nestedGroup(nested);
		}

		// Otherwise it's a leaf condition
		if (!json.has("type")) return null;
		String type = json.get("type").getAsString().toUpperCase();

		return switch (type) {
			case "SAGA_QUEST" -> {
				String sagaId = json.get("sagaId").getAsString();
				int questId = json.get("questId").getAsInt();
				yield SideQuestPrerequisites.Condition.sagaQuest(sagaId, questId);
			}
			case "SIDE_QUEST" -> {
				String sideQuestId = json.get("sideQuestId").getAsString();
				yield SideQuestPrerequisites.Condition.sideQuest(sideQuestId);
			}
			case "STAT" -> {
				String stat = json.get("stat").getAsString().toUpperCase();
				int minValue = json.get("minValue").getAsInt();
				yield SideQuestPrerequisites.Condition.stat(stat, minValue);
			}
			case "RACE" -> {
				String race = json.get("race").getAsString().toLowerCase();
				yield SideQuestPrerequisites.Condition.race(race);
			}
			case "LEVEL" -> {
				int minLevel = json.get("minLevel").getAsInt();
				yield SideQuestPrerequisites.Condition.level(minLevel);
			}
			default -> null;
		};
	}
}

