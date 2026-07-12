package com.dragonminez.common.alignment;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.diagnostics.JsonKeys;
import com.dragonminez.common.diagnostics.JsonLoadReport;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NpcAlignmentRules {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String NPC_FOLDER = Reference.MOD_ID + java.io.File.separator + "npcs";
	private static final String RULES_FILE = "alignment_rules.json";

	private static Map<String, NpcAlignmentRule> rules = defaultRules();
	private static Path loadedFrom = null;

	private NpcAlignmentRules() {
	}

	public static void load(MinecraftServer server) {
		if (server == null) {
			return;
		}

		Path npcDir = server.getWorldPath(LevelResource.ROOT).resolve(NPC_FOLDER);
		Path file = npcDir.resolve(RULES_FILE);

		try {
			Files.createDirectories(npcDir);
			if (!Files.exists(file)) {
				writeDefaults(file);
			}

			try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				JsonObject root = GSON.fromJson(reader, JsonObject.class);
				rules = parseRules(root);
				loadedFrom = file;
				LogUtil.info(Env.SERVER, "NpcAlignmentRules: loaded {} rule(s)", rules.size());
			}
		} catch (Exception e) {
			rules = defaultRules();
			loadedFrom = file;
			LogUtil.error(Env.SERVER, "NpcAlignmentRules: failed to load NPC alignment rules from {}", file, e);
		}
	}

	@Nullable
	public static NpcAlignmentRule get(String npcId) {
		if (npcId == null || npcId.isBlank()) {
			return null;
		}
		return rules.get(normalize(npcId));
	}

	public static Map<String, NpcAlignmentRule> all() {
		return Map.copyOf(rules);
	}

	private static final String RULES_LABEL = "npcs/" + RULES_FILE;
	private static final java.util.Set<String> ROOT_KEYS = java.util.Set.of("npcs");
	private static final java.util.Set<String> RULE_KEYS = java.util.Set.of("default_relation", "interaction",
			"hostile_below", "hostile_above", "min_alignment", "max_alignment");
	private static final java.util.Set<String> INTERACTION_KEYS = java.util.Set.of("min_alignment", "max_alignment");

	private static Map<String, NpcAlignmentRule> parseRules(@Nullable JsonObject root) {
		Map<String, NpcAlignmentRule> parsed = defaultRules();
		if (root == null || !root.has("npcs") || !root.get("npcs").isJsonObject()) {
			return parsed;
		}

		JsonLoadReport.clear("npc-alignment");
		JsonKeys.checkObject("npc-alignment", RULES_LABEL, "", root, ROOT_KEYS);

		JsonObject npcs = root.getAsJsonObject("npcs");
		for (Map.Entry<String, com.google.gson.JsonElement> entry : npcs.entrySet()) {
			if (!entry.getValue().isJsonObject()) {
				continue;
			}
			JsonObject ruleJson = entry.getValue().getAsJsonObject();
			JsonKeys.checkObject("npc-alignment", RULES_LABEL, "npcs." + entry.getKey(), ruleJson, RULE_KEYS);
			if (ruleJson.has("interaction") && ruleJson.get("interaction").isJsonObject()) {
				JsonKeys.checkObject("npc-alignment", RULES_LABEL, "npcs." + entry.getKey() + ".interaction",
						ruleJson.getAsJsonObject("interaction"), INTERACTION_KEYS);
			}
			NpcAlignmentRule rule = parseRule(entry.getValue().getAsJsonObject());
			if (rule != null) {
				parsed.put(normalize(entry.getKey()), rule);
			}
		}
		return parsed;
	}

	@Nullable
	private static NpcAlignmentRule parseRule(JsonObject json) {
		TargetHelper.Relation defaultRelation = parseRelation(getString(json, "default_relation", "NEUTRAL"));
		JsonObject interaction = json.has("interaction") && json.get("interaction").isJsonObject()
				? json.getAsJsonObject("interaction")
				: json;
		return new NpcAlignmentRule(
				defaultRelation,
				getInt(interaction, "min_alignment", null),
				getInt(interaction, "max_alignment", null),
				getInt(json, "hostile_below", null),
				getInt(json, "hostile_above", null)
		);
	}

	private static void writeDefaults(Path file) throws java.io.IOException {
		try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			GSON.toJson(defaultRulesJson(), writer);
		}
	}

	private static JsonObject defaultRulesJson() {
		JsonObject root = new JsonObject();
		JsonObject npcs = new JsonObject();
		addRule(npcs, "goku", TargetHelper.Relation.FRIENDLY, 61, null, 25, null);
		addRule(npcs, "roshi", TargetHelper.Relation.FRIENDLY, 41, null, 25, null);
		addRule(npcs, "karin", TargetHelper.Relation.FRIENDLY, 41, null, 25, null);
		addRule(npcs, "guru", TargetHelper.Relation.FRIENDLY, 61, null, 25, null);
		addRule(npcs, "dende", TargetHelper.Relation.FRIENDLY, 61, null, 25, null);
		addRule(npcs, "popo", TargetHelper.Relation.FRIENDLY, 41, null, 25, null);
		addRule(npcs, "kingkai", TargetHelper.Relation.FRIENDLY, 61, null, 25, null);
		addRule(npcs, "gero", TargetHelper.Relation.NEUTRAL, null, 60, null, null);
		addRule(npcs, "enma", TargetHelper.Relation.NEUTRAL, null, null, null, null);
		addRule(npcs, "baba", TargetHelper.Relation.NEUTRAL, null, null, null, null);
		addRule(npcs, "toribot", TargetHelper.Relation.NEUTRAL, null, null, null, null);
		addRule(npcs, "bulma", TargetHelper.Relation.FRIENDLY, 41, null, 25, null);
		addRule(npcs, "krillin", TargetHelper.Relation.FRIENDLY, 41, null, 25, null);
		addRule(npcs, "yamcha", TargetHelper.Relation.FRIENDLY, 41, null, 25, null);
		addRule(npcs, "tien", TargetHelper.Relation.FRIENDLY, 41, null, 25, null);
		addRule(npcs, "chiaotzu", TargetHelper.Relation.FRIENDLY, 41, null, 25, null);
		addRule(npcs, "gohan", TargetHelper.Relation.FRIENDLY, 41, null, 25, null);
		addRule(npcs, "trunks", TargetHelper.Relation.FRIENDLY, 41, null, 25, null);
		addRule(npcs, "chi_chi", TargetHelper.Relation.FRIENDLY, 41, null, 25, null);
		addRule(npcs, "videl", TargetHelper.Relation.FRIENDLY, 41, null, 25, null);
		addRule(npcs, "namek_elder", TargetHelper.Relation.FRIENDLY, 61, null, 25, null);
		addRule(npcs, "shin", TargetHelper.Relation.FRIENDLY, 41, null, 25, null);
		addRule(npcs, "piccolo", TargetHelper.Relation.NEUTRAL, 41, null, 20, null);
		addRule(npcs, "vegeta", TargetHelper.Relation.NEUTRAL, null, null, null, null);
		root.add("npcs", npcs);
		return root;
	}

	private static Map<String, NpcAlignmentRule> defaultRules() {
		Map<String, NpcAlignmentRule> parsed = new LinkedHashMap<>();
		JsonObject npcs = defaultRulesJson().getAsJsonObject("npcs");
		for (Map.Entry<String, com.google.gson.JsonElement> entry : npcs.entrySet()) {
			if (!entry.getValue().isJsonObject()) {
				continue;
			}
			NpcAlignmentRule rule = parseRule(entry.getValue().getAsJsonObject());
			if (rule != null) {
				parsed.put(normalize(entry.getKey()), rule);
			}
		}
		return parsed;
	}

	private static void addRule(JsonObject npcs, String id, TargetHelper.Relation relation, Integer min, Integer max,
								Integer hostileBelow, Integer hostileAbove) {
		JsonObject rule = new JsonObject();
		rule.addProperty("default_relation", relation.name());
		JsonObject interaction = new JsonObject();
		if (min != null) interaction.addProperty("min_alignment", min);
		if (max != null) interaction.addProperty("max_alignment", max);
		if (!interaction.entrySet().isEmpty()) {
			rule.add("interaction", interaction);
		}
		if (hostileBelow != null) rule.addProperty("hostile_below", hostileBelow);
		if (hostileAbove != null) rule.addProperty("hostile_above", hostileAbove);
		npcs.add(id, rule);
	}

	private static String normalize(String id) {
		String normalized = id.trim().toLowerCase();
		if (normalized.contains(":")) {
			normalized = normalized.substring(normalized.indexOf(':') + 1);
		}
		return normalized;
	}

	private static String getString(JsonObject json, String key, String fallback) {
		if (!json.has(key) || json.get(key).isJsonNull()) {
			return fallback;
		}
		return json.get(key).getAsString();
	}

	@Nullable
	private static Integer getInt(JsonObject json, String key, @Nullable Integer fallback) {
		if (!json.has(key) || json.get(key).isJsonNull()) {
			return fallback;
		}
		return json.get(key).getAsInt();
	}

	private static TargetHelper.Relation parseRelation(String raw) {
		if (raw == null || raw.isBlank()) {
			return TargetHelper.Relation.NEUTRAL;
		}
		try {
			return TargetHelper.Relation.valueOf(raw.trim().toUpperCase());
		} catch (IllegalArgumentException ignored) {
			return TargetHelper.Relation.NEUTRAL;
		}
	}
}
