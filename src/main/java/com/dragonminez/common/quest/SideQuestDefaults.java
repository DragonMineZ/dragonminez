package com.dragonminez.common.quest;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.google.gson.*;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Generates the default side-quest JSON files in the unified schema.
 * <p>
 * Side-quests use the same schema as saga quests & only the {@code "type"} field
 * is set to {@code "SIDEQUEST"} and there is no {@code "chain"} block.
 * <p>
 * Default generation also supports optional start-only {@code "requirements"} blocks
 * separate from unlock {@code "prerequisites"}.
 * <p>
 * Called by {@link QuestRegistry} during side-quest loading.
 *
 * @since 2.1
 */
final class SideQuestDefaults {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Set<String> GOOD_PATH_NPCS = Set.of(
			"goku", "roshi", "karin", "guru", "dende", "popo", "kingkai",
			"bulma", "krillin", "yamcha", "tien", "chiaotzu", "gohan", "trunks",
			"chi_chi", "videl", "namek_elder"
	);

	private SideQuestDefaults() {} // utility class

	static void createDefaultSideQuestFiles(Path sideQuestDir) {
		if (!ConfigManager.getServerConfig().getGameplay().getCreateDefaultSideQuests()) return;
		createTrainingCategory(sideQuestDir);
		createExplorationCategory(sideQuestDir);
		createCombatCategory(sideQuestDir);
		createStoryCategory(sideQuestDir);
		createCollectionCategory(sideQuestDir);
		createBulmaTechCategory(sideQuestDir);
		createBulmaErrandsCategory(sideQuestDir);
		createMoviesCategory(sideQuestDir);
	}

	// ---- Helpers ----

	private static void writeQuestFile(Path dir, String filename, JsonObject quest) {
		Path file = dir.resolve(filename);
		try {
			Files.createDirectories(dir);
			if (Files.exists(file)) {
				return;
			}
			try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) { GSON.toJson(quest, w); }
		} catch (IOException e) { LogUtil.error(Env.COMMON, "Failed to create default side-quest file: {}", filename, e); }
	}

	/** Builds a sidequest in the unified schema. */
	private static JsonObject sidequest(String id, String title, String desc, String category,
										boolean parallelObjectives,
										String questGiver, String turnIn,
										JsonObject prerequisites,
										JsonObject[] objectives, JsonObject[] rewards) {
		return sidequest(id, title, desc, category, parallelObjectives, questGiver, turnIn,
				prerequisites, null, objectives, rewards);
	}

	private static JsonObject sidequest(String id, String title, String desc, String category,
										boolean parallelObjectives,
										String questGiver, String turnIn,
										JsonObject prerequisites,
										JsonObject startRequirements,
										JsonObject[] objectives, JsonObject[] rewards) {
		JsonObject q = new JsonObject();
		q.addProperty("id", id);
		q.addProperty("title", title);
		q.addProperty("description", desc);
		q.addProperty("type", "SIDEQUEST");
		q.addProperty("category", category);
		q.addProperty("parallel_objectives", parallelObjectives);
		q.addProperty("party_scaling", true);
		q.addProperty("secret", false);
		q.addProperty("claim_mode", "TREE_OR_NPC");
		if (questGiver != null) q.addProperty("quest_giver", questGiver);
		else q.add("quest_giver", JsonNull.INSTANCE);
		if (turnIn != null) q.addProperty("turn_in", turnIn);
		else q.add("turn_in", JsonNull.INSTANCE);

		JsonObject effectiveStartRequirements = startRequirements;
//		JsonObject effectiveStartRequirements = requiresGoodPath(questGiver, turnIn)
//				? withAlignmentRequirement(startRequirements)
//				: startRequirements;

		if (prerequisites != null) q.add("prerequisites", prerequisites);
		if (effectiveStartRequirements != null) q.add("requirements", effectiveStartRequirements);

		JsonArray objArr = new JsonArray(); for (JsonObject o : objectives) objArr.add(o);
		q.add("objectives", objArr);
		JsonArray rewArr = new JsonArray(); for (JsonObject r : rewards) rewArr.add(r);
		q.add("rewards", rewArr);
		return q;
	}

	// ---- Objective helpers ----

	private static JsonObject objKill(String entity, int count) {
		return objKill(entity, count, "NATURAL", "ANY_MATCHING");
	}

	private static JsonObject objQuestKill(String entity, int count) {
		return objKill(entity, count, "QUEST", "QUEST_SPAWNED_ONLY");
	}

	private static JsonObject objKill(String entity, int count, String spawnMode, String countMode) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "KILL");
		o.addProperty("entity", entity); o.addProperty("count", count);
		o.addProperty("spawn", spawnMode);
		o.addProperty("count_mode", countMode);
		return o;
	}

	private static JsonObject objStructure(String structureId) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "STRUCTURE"); o.addProperty("structure", structureId);
		return o;
	}

	private static JsonObject objBiome(String biomeId) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "BIOME"); o.addProperty("biome", biomeId);
		return o;
	}

	private static JsonObject objItem(String itemId, int count) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "ITEM");
		o.addProperty("item", itemId); o.addProperty("count", count);
		return o;
	}

	private static JsonObject objTalkTo(String npcId) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "TALK_TO"); o.addProperty("npcId", npcId);
		return o;
	}

	private static JsonObject objDragonSummon(String dragonId, String ballSetId) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "DRAGON_SUMMON");
		o.addProperty("dragon", dragonId);
		o.addProperty("ball_set", ballSetId);
		return o;
	}

	private static JsonObject objSkill(String skill, int level) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "SKILL");
		o.addProperty("skill", skill);
		o.addProperty("level", level);
		return o;
	}

	// ---- Reward helpers ----

	private static JsonObject rewTPS(int amount) {
		JsonObject r = new JsonObject(); r.addProperty("type", "TPS"); r.addProperty("amount", amount); return r;
	}

	private static JsonObject rewItem(String itemId, int count) {
		JsonObject r = new JsonObject(); r.addProperty("type", "ITEM"); r.addProperty("item", itemId); r.addProperty("count", count); return r;
	}

	private static JsonObject rewSkill(String skill, int level) {
		JsonObject r = new JsonObject(); r.addProperty("type", "SKILL"); r.addProperty("skill", skill); r.addProperty("level", level); return r;
	}

	/** A COMMAND reward — runs {@code command} at permission level 4 ({@code %player%} → player name). */
	private static JsonObject rewCommand(String command, String translationKey) {
		JsonObject r = new JsonObject(); r.addProperty("type", "COMMAND"); r.addProperty("command", command);
		if (translationKey != null) r.addProperty("translationKey", translationKey);
		return r;
	}

	private static JsonObject onlyOn(JsonObject reward, String... difficulties) {
		JsonArray arr = new JsonArray();
		for (String difficulty : difficulties) arr.add(difficulty);
		reward.add("difficulty", arr);
		return reward;
	}

	// ---- Prerequisite helpers ----

	private static JsonObject prereqs(String op, JsonObject... conditions) {
		JsonObject p = new JsonObject(); p.addProperty("operator", op);
		JsonArray arr = new JsonArray(); for (JsonObject c : conditions) arr.add(c);
		p.add("conditions", arr); return p;
	}

	private static JsonObject requirements(String op, JsonObject... conditions) {
		return prereqs(op, conditions);
	}

	private static JsonObject condSaga(String sagaId, int questId) {
		JsonObject c = new JsonObject(); c.addProperty("type", "SAGA_QUEST"); c.addProperty("sagaId", sagaId); c.addProperty("questId", questId); return c;
	}

	private static JsonObject condQuest(String questId) {
		JsonObject c = new JsonObject(); c.addProperty("type", "QUEST"); c.addProperty("questId", questId); return c;
	}

	private static JsonObject condLevel(int minLevel) {
		JsonObject c = new JsonObject(); c.addProperty("type", "LEVEL"); c.addProperty("minLevel", minLevel); return c;
	}

	private static JsonObject condSkill(String skill, int minLevel) {
		JsonObject c = new JsonObject(); c.addProperty("type", "SKILL"); c.addProperty("skill", skill); c.addProperty("minLevel", minLevel); return c;
	}

	private static JsonObject condAlignmentMin(int minAlignment) {
		JsonObject c = new JsonObject(); c.addProperty("type", "ALIGNMENT"); c.addProperty("min", minAlignment); return c;
	}

	private static JsonObject condStat(String stat, int minValue) {
		JsonObject c = new JsonObject(); c.addProperty("type", "STAT"); c.addProperty("stat", stat); c.addProperty("minValue", minValue); return c;
	}

	private static JsonObject condBiome(String biomeId) {
		JsonObject c = new JsonObject(); c.addProperty("type", "BIOME"); c.addProperty("biome", biomeId); return c;
	}

	private static JsonObject condStructure(String structureId) {
		JsonObject c = new JsonObject(); c.addProperty("type", "STRUCTURE"); c.addProperty("structure", structureId); return c;
	}

	private static JsonObject condDimension(String dimensionId) {
		JsonObject c = new JsonObject(); c.addProperty("type", "DIMENSION"); c.addProperty("dimension", dimensionId); return c;
	}

	private static JsonObject condGameTimeMinutes(long minutes) {
		JsonObject c = new JsonObject(); c.addProperty("type", "TIME"); c.addProperty("mode", "GAME_TIME"); c.addProperty("ticks", minutes * 20L * 60L); return c;
	}

	private static JsonObject condRealTimeMinutes(long minutes) {
		JsonObject c = new JsonObject(); c.addProperty("type", "TIME"); c.addProperty("mode", "REAL_TIME"); c.addProperty("milliseconds", minutes * 60L * 1000L); return c;
	}

	private static boolean requiresGoodPath(String questGiver, String turnIn) {
		return GOOD_PATH_NPCS.contains(normalizeNpcId(questGiver)) || GOOD_PATH_NPCS.contains(normalizeNpcId(turnIn));
	}

	private static String normalizeNpcId(String npcId) {
		if (npcId == null || npcId.isBlank()) return "";
		String normalized = npcId.trim().toLowerCase();
		if (normalized.contains(":")) {
			normalized = normalized.substring(normalized.indexOf(':') + 1);
		}
		return normalized;
	}

	private static JsonObject withAlignmentRequirement(JsonObject startRequirements) {
		JsonObject requirements = startRequirements != null ? startRequirements : requirements("AND");
		JsonArray conditions = requirements.getAsJsonArray("conditions");
		conditions.add(condAlignmentMin(0));
		return requirements;
	}

	// ========================================================================================
	// Training Side-Quests
	// ========================================================================================

	private static void createTrainingCategory(Path baseDir) {
		Path dir = baseDir.resolve("training");

		writeQuestFile(dir, "roshi_basic_training.json", sidequest(
				"roshi_basic_training", "dmz.sidequest.roshi_basic.name", "dmz.sidequest.roshi_basic.desc",
				"training", false, "roshi", "roshi", null,
				new JsonObject[]{
						objKill("minecraft:zombie", 10),
						objTalkTo("roshi")
				},
				new JsonObject[]{ rewTPS(600) }));

		writeQuestFile(dir, "endurance_training.json", sidequest(
				"endurance_training", "dmz.sidequest.endurance.name", "dmz.sidequest.endurance.desc",
				"training", false, "roshi", "roshi",
				prereqs("AND", condQuest("roshi_basic_training")),
				new JsonObject[]{
						objKill("minecraft:skeleton", 15),
						objKill("minecraft:spider", 10),
						objTalkTo("roshi")
				},
				new JsonObject[]{ rewTPS(1000) }));

		writeQuestFile(dir, "weighted_training.json", sidequest(
				"weighted_training", "dmz.sidequest.weighted.name", "dmz.sidequest.weighted.desc",
				"training", false, "roshi", "roshi",
				prereqs("AND", condQuest("endurance_training"), condSaga("saiyan_saga", 2)),
				new JsonObject[]{
						objKill("minecraft:iron_golem", 3),
						objItem("minecraft:iron_ingot", 32),
						objTalkTo("roshi")
				},
				new JsonObject[]{ rewTPS(2000), rewItem("minecraft:golden_apple", 5) }));

		writeQuestFile(dir, "gravity_chamber.json", sidequest(
				"gravity_chamber", "dmz.sidequest.gravity.name", "dmz.sidequest.gravity.desc",
				"training", true, "goku", "goku",
				prereqs("AND", condQuest("weighted_training"), condLevel(10)),
				new JsonObject[]{
						objQuestKill("minecraft:wither_skeleton", 5),
						objQuestKill("minecraft:blaze", 10),
						objItem("minecraft:blaze_rod", 10),
						objTalkTo("goku")
				},
				new JsonObject[]{ rewTPS(6000) }));
		writeQuestFile(dir, "specialized_training.json", sidequest(
				"specialized_training", "dmz.sidequest.specializedtr.name", "dmz.sidequest.specializedtr.desc",
				"training", true, "bulma", "bulma",
				prereqs("AND", condQuest("gravity_chamber"), condLevel(15)),
				new JsonObject[]{
						objItem("minecraft:diamond", 20),
						objItem("minecraft:obsidian", 20)
				},
				new JsonObject[]{ rewTPS(9000), rewItem("minecraft:enderpearl", 16)}
		));

		// --- Saiyan Saga: Training ---

		writeQuestFile(dir, "krillin_sparring.json", sidequest(
				"krillin_sparring", "dmz.sidequest.krillin_sparring.name", "dmz.sidequest.krillin_sparring.desc",
				"training", false, "krillin", "krillin",
				prereqs("AND", condSaga("saiyan_saga", 1)),
				new JsonObject[]{
						objKill("minecraft:zombie", 10),
						objKill("minecraft:skeleton", 5),
						objTalkTo("krillin")
				},
				new JsonObject[]{ rewTPS(800) }));

		writeQuestFile(dir, "tien_mountain_training.json", sidequest(
				"tien_mountain_training", "dmz.sidequest.tien_mountain.name", "dmz.sidequest.tien_mountain.desc",
				"training", false, "krillin", "krillin",
				prereqs("AND", condSaga("saiyan_saga", 4)),
				requirements("AND", condBiome("#minecraft:is_mountain")),
				new JsonObject[]{
						objKill("minecraft:iron_golem", 5),
						objTalkTo("krillin")
				},
				new JsonObject[]{ rewTPS(1400) }));

		writeQuestFile(dir, "piccolo_wilderness_survival.json", sidequest(
				"piccolo_wilderness_survival", "dmz.sidequest.piccolo_survival.name", "dmz.sidequest.piccolo_survival.desc",
				"training", true, "piccolo", "piccolo",
				prereqs("AND", condSaga("saiyan_saga", 4)),
				requirements("AND", condBiome("minecraft:forest")),
				new JsonObject[]{
						objKill("minecraft:zombie", 20),
						objKill("minecraft:skeleton", 10),
						objKill("minecraft:creeper", 5)
				},
				new JsonObject[]{ rewTPS(3300) }));

		writeQuestFile(dir, "gohan_survival.json", sidequest(
				"gohan_survival", "dmz.sidequest.gohan_survival.name", "dmz.sidequest.gohan_survival.desc",
				"training", false, "piccolo", "gohan",
				prereqs("AND", condSaga("saiyan_saga", 2)),
				new JsonObject[]{
						objItem("minecraft:cooked_beef", 16),
						objItem("minecraft:iron_sword", 1),
						objTalkTo("gohan")
				},
				new JsonObject[]{ rewTPS(1000) }));

		// --- Android Saga: Training ---

		writeQuestFile(dir, "vegeta_pride_training.json", sidequest(
				"vegeta_pride_training", "dmz.sidequest.vegeta_pride.name", "dmz.sidequest.vegeta_pride.desc",
				"training", false, "vegeta", "vegeta",
				prereqs("AND", condSaga("android_saga", 12)),
				requirements("AND", condStructure("dragonminez:timechamber")),
				new JsonObject[]{
						objKill("minecraft:wither_skeleton", 10),
						objKill("minecraft:blaze", 5),
						objTalkTo("vegeta")
				},
				new JsonObject[]{ rewTPS(60000) }));

		writeQuestFile(dir, "gohan_time_chamber_training.json", sidequest(
				"gohan_time_chamber_training", "dmz.sidequest.gohan_timechamber.name", "dmz.sidequest.gohan_timechamber.desc",
				"training", false, "gohan", "gohan",
				prereqs("AND", condSaga("android_saga", 11)),
				requirements("AND", condStructure("dragonminez:timechamber")),
				new JsonObject[]{
						objKill("minecraft:phantom", 20),
						objItem("minecraft:golden_apple", 8),
						objTalkTo("gohan")
				},
				new JsonObject[]{ rewTPS(72000) }));

		// --- Buu Saga: Training ---

		writeQuestFile(dir, "kaiosama_buu_warning.json", sidequest(
				"kaiosama_buu_warning", "dmz.sidequest.kaiosama_buu.name", "dmz.sidequest.kaiosama_buu.desc",
				"training", false, "kingkai", "kingkai",
				prereqs("AND", condSaga("buu_saga", 1)),
				new JsonObject[]{
						objTalkTo("kingkai")
				},
				new JsonObject[]{ rewTPS(72000) }));

		writeQuestFile(dir, "old_kai_ritual.json", sidequest(
				"old_kai_ritual", "dmz.sidequest.old_kai_ritual.name", "dmz.sidequest.old_kai_ritual.desc",
				"training", false, "kingkai", "kingkai",
				prereqs("AND", condSaga("buu_saga", 23)),
				requirements("AND", condBiome("dragonminez:sacredkai_plains"), condSkill("ultimate", 1)),
				new JsonObject[]{
						objItem("dragonminez:z_sword", 1),
						objTalkTo("oldkai")
				},
				new JsonObject[]{ rewTPS(70000)}));
	}

	// ========================================================================================
	// Exploration Side-Quests
	// ========================================================================================

	private static void createExplorationCategory(Path baseDir) {
		Path dir = baseDir.resolve("exploration");

		writeQuestFile(dir, "world_explorer.json", sidequest(
				"world_explorer", "dmz.sidequest.explorer.name", "dmz.sidequest.explorer.desc",
				"exploration", true, null, null, null,
				new JsonObject[]{
						objBiome("minecraft:plains"),
						objBiome("minecraft:desert"),
						objBiome("minecraft:forest")
				},
				new JsonObject[]{ rewTPS(800) }));

		writeQuestFile(dir, "earth_landmarks.json", sidequest(
				"earth_landmarks", "dmz.sidequest.earth_landmarks.name", "dmz.sidequest.earth_landmarks.desc",
				"exploration", true, null, null,
				prereqs("AND", condQuest("world_explorer")),
				new JsonObject[]{
						objStructure("dragonminez:roshi_house"),
						objStructure("dragonminez:goku_house"),
						objStructure("dragonminez:kamilookout")
				},
				new JsonObject[]{ rewTPS(1800) }));

		writeQuestFile(dir, "namek_explorer.json", sidequest(
				"namek_explorer", "dmz.sidequest.namek_explorer.name", "dmz.sidequest.namek_explorer.desc",
				"exploration", true, null, null,
				prereqs("AND", condSaga("saiyan_saga", 8)),
				new JsonObject[]{
						objBiome("dragonminez:ajissa_plains"),
						objStructure("dragonminez:village_ajissa")
				},
				new JsonObject[]{ rewTPS(1600) }));

		writeQuestFile(dir, "sacred_lands.json", sidequest(
				"sacred_lands", "dmz.sidequest.sacred_lands.name", "dmz.sidequest.sacred_lands.desc",
				"exploration", false, null, null,
				prereqs("AND", condQuest("namek_explorer")),
				new JsonObject[]{
						objStructure("dragonminez:village_sacred")
				},
				new JsonObject[]{ rewTPS(3600) }));

		// --- Frieza Saga: Exploration ---

		writeQuestFile(dir, "krillin_namek_scout.json", sidequest(
				"krillin_namek_scout", "dmz.sidequest.krillin_scout.name", "dmz.sidequest.krillin_scout.desc",
				"exploration", false, "krillin", "krillin",
				prereqs("AND", condSaga("frieza_saga", 2)),
				new JsonObject[]{
						objStructure("dragonminez:village_ajissa"),
						objTalkTo("krillin")
				},
				new JsonObject[]{ rewTPS(12000) }));

		writeQuestFile(dir, "namek_farewell.json", sidequest(
				"namek_farewell", "dmz.sidequest.namek_farewell.name", "dmz.sidequest.namek_farewell.desc",
				"exploration", false, "guru", "guru",
				prereqs("AND", condSaga("frieza_saga", 15)),
				new JsonObject[]{
						objStructure("dragonminez:village_sacred"),
						objTalkTo("guru")
				},
				new JsonObject[]{ rewTPS(18000) }));

		// --- Android Saga: Exploration ---

		writeQuestFile(dir, "trunks_warning.json", sidequest(
				"trunks_warning", "dmz.sidequest.trunks_warning.name", "dmz.sidequest.trunks_warning.desc",
				"exploration", false, "trunks", "trunks",
				prereqs("AND", condSaga("android_saga", 3)),
				new JsonObject[]{
						objStructure("dragonminez:gero_lab"),
						objTalkTo("trunks")
				},
				new JsonObject[]{ rewTPS(40000) }));

		writeQuestFile(dir, "piccolo_kami_fusion.json", sidequest(
				"piccolo_kami_fusion", "dmz.sidequest.piccolo_kami.name", "dmz.sidequest.piccolo_kami.desc",
				"exploration", false, "piccolo", "piccolo",
				prereqs("AND", condSaga("android_saga", 10)),
				new JsonObject[]{
						objStructure("dragonminez:kamilookout"),
						objTalkTo("piccolo")
				},
				new JsonObject[]{ rewTPS(52000) }));
	}

	// ========================================================================================
	// Combat Side-Quests
	// ========================================================================================

	private static void createCombatCategory(Path baseDir) {
		Path dir = baseDir.resolve("combat");

		writeQuestFile(dir, "monster_hunter.json", sidequest(
				"monster_hunter", "dmz.sidequest.monster_hunter.name", "dmz.sidequest.monster_hunter.desc",
				"combat", true, null, null, null,
				new JsonObject[]{
						objKill("minecraft:zombie", 20),
						objKill("minecraft:skeleton", 20),
						objKill("minecraft:creeper", 10)
				},
				new JsonObject[]{ rewTPS(1200) }));

		writeQuestFile(dir, "nether_warrior.json", sidequest(
				"nether_warrior", "dmz.sidequest.nether_warrior.name", "dmz.sidequest.nether_warrior.desc",
				"combat", false, null, null,
				prereqs("AND", condQuest("monster_hunter"), condLevel(5)),
				requirements("AND", condDimension("minecraft:the_nether")),
				new JsonObject[]{
						objKill("minecraft:blaze", 10),
						objKill("minecraft:wither_skeleton", 10)
				},
				new JsonObject[]{ rewTPS(4500), rewItem("minecraft:diamond", 3) }));

		writeQuestFile(dir, "dragon_ball_hunter.json", sidequest(
				"dragon_ball_hunter", "dmz.sidequest.dball_hunter.name", "dmz.sidequest.dball_hunter.desc",
				"combat", false, null, null,
				prereqs("AND", condSaga("saiyan_saga", 4)),
				new JsonObject[]{
						objStructure("minecraft:pillager_outpost"),
						objKill("minecraft:pillager", 15)
				},
				new JsonObject[]{ rewTPS(2000) }));

		// --- Saiyan Saga: Combat ---

		writeQuestFile(dir, "yamcha_desert_bandit.json", sidequest(
				"yamcha_desert_bandit", "dmz.sidequest.yamcha_bandit.name", "dmz.sidequest.yamcha_bandit.desc",
				"combat", false, "yamcha", "yamcha",
				prereqs("AND", condSaga("saiyan_saga", 2)),
				requirements("AND", condBiome("minecraft:desert")),
				new JsonObject[]{
						objKill("dragonminez:bandit", 10),
						objTalkTo("yamcha")
				},
				new JsonObject[]{ rewTPS(1200) }));

		// --- Frieza Saga: Combat ---

		writeQuestFile(dir, "namek_elder_blessing.json", sidequest(
				"namek_elder_blessing", "dmz.sidequest.elder_blessing.name", "dmz.sidequest.elder_blessing.desc",
				"combat", false, "guru", "guru",
				prereqs("AND", condSaga("frieza_saga", 2)),
				new JsonObject[]{
						objKill("#dragonminez:frieza_soldiers", 10),
						objTalkTo("guru")
				},
				new JsonObject[]{ rewTPS(9000) }));

		writeQuestFile(dir, "ginyu_force_warm_up.json", sidequest(
				"ginyu_force_warm_up", "dmz.sidequest.ginyu_warmup.name", "dmz.sidequest.ginyu_warmup.desc",
				"combat", true, "goku", "goku",
				prereqs("AND", condSaga("frieza_saga", 6)),
				requirements("AND", condBiome("dragonminez:ajissa_plains")),
				new JsonObject[]{
						objKill("#dragonminez:frieza_soldiers", 16)
				},
				new JsonObject[]{ rewTPS(21000) }));

		// --- Android Saga: Combat ---

		writeQuestFile(dir, "krillin_android_patrol.json", sidequest(
				"krillin_android_patrol", "dmz.sidequest.krillin_patrol.name", "dmz.sidequest.krillin_patrol.desc",
				"combat", false, "krillin", "krillin",
				prereqs("AND", condSaga("android_saga", 5)),
				requirements("AND", condBiome("dragonminez:rocky")),
				new JsonObject[]{
						objKill("dragonminez:red_ribbon_soldier", 15),
						objKill("dragonminez:robot1", 3),
						objTalkTo("krillin")
				},
				new JsonObject[]{ rewTPS(48000) }));

		writeQuestFile(dir, "tien_cell_search.json", sidequest(
				"tien_cell_search", "dmz.sidequest.tien_cell.name", "dmz.sidequest.tien_cell.desc",
				"combat", true, "krillin", "krillin",
				prereqs("AND", condSaga("android_saga", 10)),
				requirements("AND", condBiome("minecraft:plains")),
				new JsonObject[]{
						objKill("minecraft:zombie", 30),
						objKill("minecraft:spider", 15)
				},
				new JsonObject[]{ rewTPS(64000) }));

		writeQuestFile(dir, "videl_city_defense.json", sidequest(
				"videl_city_defense", "dmz.sidequest.videl_defense.name", "dmz.sidequest.videl_defense.desc",
				"combat", false, "gohan", "gohan",
				prereqs("AND", condSaga("android_saga", 14)),
				new JsonObject[]{
						objQuestKill("dragonminez:saga_cell_jr", 10),
						objTalkTo("gohan")
				},
				new JsonObject[]{ rewTPS(80000) }));
	}

	// ========================================================================================
	// Story Side-Quests
	// ========================================================================================

	private static void createStoryCategory(Path baseDir) {
		Path dir = baseDir.resolve("story");

		// --- Frieza Saga: Story ---

		writeQuestFile(dir, "piccolo_arrival.json", sidequest(
				"piccolo_arrival", "dmz.sidequest.piccolo_arrival.name", "dmz.sidequest.piccolo_arrival.desc",
				"story", false, "piccolo", "piccolo",
				prereqs("AND", condSaga("frieza_saga", 8)),
				new JsonObject[]{
						objKill("dragonminez:saga_friezasoldier01", 5),
						objKill("dragonminez:saga_friezasoldier02", 5),
						objTalkTo("piccolo")
				},
				new JsonObject[]{ rewTPS(15000) }));

		writeQuestFile(dir, "goku_healing_pod.json", sidequest(
				"goku_healing_pod", "dmz.sidequest.goku_healing.name", "dmz.sidequest.goku_healing.desc",
				"story", false, "goku", "goku",
				prereqs("AND", condSaga("frieza_saga", 11)),
				requirements("AND", condStructure("dragonminez:elder_guru")),
				new JsonObject[]{
						objKill("dragonminez:saga_friezasoldier01", 20),
						objTalkTo("goku")
				},
				new JsonObject[]{ rewTPS(24000) }));

		// --- Android Saga: Story ---

		writeQuestFile(dir, "chi_chi_gohan_worry.json", sidequest(
				"chi_chi_gohan_worry", "dmz.sidequest.chichi_worry.name", "dmz.sidequest.chichi_worry.desc",
				"story", false, "goku", "goku",
				prereqs("AND", condSaga("android_saga", 14)),
				new JsonObject[]{
						objItem("minecraft:iron_sword", 1),
						objItem("minecraft:cooked_beef", 16),
						objTalkTo("goku")
				},
				new JsonObject[]{ rewTPS(15000), rewItem("minecraft:golden_apple", 5) }));

		writeQuestFile(dir, "goku_final_farewell.json", sidequest(
				"goku_final_farewell", "dmz.sidequest.goku_farewell.name", "dmz.sidequest.goku_farewell.desc",
				"story", false, "goku", "goku",
				prereqs("AND", condSaga("android_saga", 15)),
				new JsonObject[]{
						objStructure("dragonminez:kamilookout"),
						objTalkTo("goku")
				},
				new JsonObject[]{ rewTPS(100000) }));

		// --- Future Saga: Story ---

		writeQuestFile(dir, "future_gohan_cure.json", sidequest(
				"future_gohan_cure", "dmz.sidequest.future_gohan_cure.name", "dmz.sidequest.future_gohan_cure.desc",
				"story", false, "trunks", "trunks",
				prereqs("AND", condSaga("future_saga", 3)),
				new JsonObject[]{
						objItem("minecraft:golden_apple", 8),
						objItem("dragonminez:senzu_bean", 1),
						objTalkTo("trunks")
				},
				new JsonObject[]{ rewTPS(112000) }));
	}

	// ========================================================================================
	// Collection Side-Quests
	// ========================================================================================

	private static void createCollectionCategory(Path baseDir) {
		Path dir = baseDir.resolve("collection");

		// --- Saiyan Saga: Collection ---

		writeQuestFile(dir, "bulma_radar_parts.json", sidequest(
				"bulma_radar_parts", "dmz.sidequest.bulma_radar.name", "dmz.sidequest.bulma_radar.desc",
				"collection", false, "bulma", "bulma",
				prereqs("AND", condSaga("saiyan_saga", 3)),
				new JsonObject[]{
						objItem("minecraft:redstone", 16),
						objItem("minecraft:copper_ingot", 16),
						objItem("minecraft:gold_ingot", 8),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(1600), rewItem("dragonminez:dball_radar", 1) }));

		writeQuestFile(dir, "collect_dragon_balls.json", sidequest(
				"collect_dragon_balls", "dmz.sidequest.collect_dballs.name", "dmz.sidequest.collect_dballs.desc",
				"collection", false, "bulma", "bulma",
				prereqs("AND", condSaga("saiyan_saga", 5), condQuest("bulma_radar_parts")),
				new JsonObject[]{
						objDragonSummon("shenron", "earth"),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(6600) }));

		writeQuestFile(dir, "chi_chi_provisions.json", sidequest(
				"chi_chi_provisions", "dmz.sidequest.chichi_provisions.name", "dmz.sidequest.chichi_provisions.desc",
				"collection", true, "goku", "goku",
				prereqs("AND", condSaga("saiyan_saga", 5)),
				new JsonObject[]{
						objItem("minecraft:cooked_beef", 32),
						objItem("minecraft:bread", 32),
						objItem("minecraft:golden_carrot", 8)
				},
				new JsonObject[]{ rewTPS(800), rewItem("minecraft:golden_apple", 5) }));

		// --- Frieza Saga: Collection ---

		writeQuestFile(dir, "bulma_namek_research.json", sidequest(
				"bulma_namek_research", "dmz.sidequest.bulma_namek.name", "dmz.sidequest.bulma_namek.desc",
				"collection", false, "bulma", "bulma",
				prereqs("AND", condSaga("frieza_saga", 2)),
				new JsonObject[]{
						objItem("dragonminez:kikono_shard", 16),
						objItem("dragonminez:kikono_cloth", 8),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(10500), rewItem("dragonminez:namekdball_radar", 1) }));

		writeQuestFile(dir, "merchant_alien_artifacts.json", sidequest(
				"merchant_alien_artifacts", "dmz.sidequest.merchant_artifacts.name", "dmz.sidequest.merchant_artifacts.desc",
				"collection", false, "bulma", "bulma",
				prereqs("AND", condSaga("frieza_saga", 4)),
				new JsonObject[]{
						objItem("dragonminez:kikono_shard", 16),
						objItem("minecraft:diamond", 8),
						objTalkTo("merchant_01")
				},
				new JsonObject[]{ rewTPS(13500), rewItem("minecraft:diamond_sword", 1) }));

		writeQuestFile(dir, "gohan_dragon_balls_namek.json", sidequest(
				"gohan_dragon_balls_namek", "dmz.sidequest.gohan_namek_db.name", "dmz.sidequest.gohan_namek_db.desc",
				"collection", false, "guru", "guru",
				prereqs("AND", condSaga("frieza_saga", 3), condQuest("bulma_namek_research")),
				new JsonObject[]{
						objDragonSummon("porunga", "namek"),
						objTalkTo("guru")
				},
				new JsonObject[]{ rewTPS(36000) }));

		// --- Android Saga: Collection ---

		writeQuestFile(dir, "bulma_gero_blueprints.json", sidequest(
				"bulma_gero_blueprints", "dmz.sidequest.bulma_gero.name", "dmz.sidequest.bulma_gero.desc",
				"collection", false, "bulma", "bulma",
				prereqs("AND", condSaga("android_saga", 6)),
				new JsonObject[]{
						objStructure("dragonminez:gero_lab"),
						objItem("minecraft:redstone", 32),
						objItem("dragonminez:kikono_shard", 8),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(56000) }));

		// --- Buu Saga: Collection ---

		writeQuestFile(dir, "buu_saga_dragon_balls.json", sidequest(
				"buu_saga_dragon_balls", "dmz.sidequest.buu_dragon_balls.name", "dmz.sidequest.buu_dragon_balls.desc",
				"collection", false, "bulma", "bulma",
				prereqs("AND", condSaga("buu_saga", 18)),
				new JsonObject[]{
						objDragonSummon("shenron", "earth"),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(200000), rewItem("dragonminez:senzu_bean", 3) }));
	}

	// ========================================================================================
	// Bulma Tech Side-Quests ("Bulma sidequests" — Capsule Corp R&D line)
	// ========================================================================================
	//
	// Bulma "hacks/upgrades the game": radar, scouter, spacepod, and a Gete tech tree.
	// Each quest hands the player a concrete upgrade. Features gate on quest completion via
	// PlayerQuestData.isQuestCompleted(<id>).
	//
	// Spacepod: by default the Otherworld is unreachable by space pod; completing
	// bulma_otherworld_drive retunes the pod so it can warp there. The Otherworld destination's
	// unlock rule (DMZSpacePodDestinationProvider) checks this quest's completion.

	private static void createBulmaTechCategory(Path baseDir) {
		Path dir = baseDir.resolve("tech");

		// --- Dragon Radar R&D ---
		// Amplifier: unlocks an extra long-range tier on every Dragon Radar
		// (DragonRadarItem checks isQuestCompleted("bulma_radar_amplifier")).
		writeQuestFile(dir, "bulma_radar_amplifier.json", sidequest(
				"bulma_radar_amplifier", "dmz.sidequest.bulma_radar_amplifier.name", "dmz.sidequest.bulma_radar_amplifier.desc",
				"tech", false, "bulma", "bulma",
				prereqs("AND", condQuest("bulma_radar_parts")),
				new JsonObject[]{
						objItem("minecraft:amethyst_shard", 16),
						objItem("minecraft:redstone", 24),
						objItem("minecraft:gold_ingot", 8),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(4500) }));

		// Fusion: hand over an Earth + a Namek radar and Bulma fuses them into the Bi-Dimensional Radar
		// (item reward "fused_dball_radar", works in both dimensions).
		writeQuestFile(dir, "bulma_radar_fusion.json", sidequest(
				"bulma_radar_fusion", "dmz.sidequest.bulma_radar_fusion.name", "dmz.sidequest.bulma_radar_fusion.desc",
				"tech", false, "bulma", "bulma",
				prereqs("AND", condQuest("bulma_radar_amplifier"), condQuest("bulma_namek_research")),
				new JsonObject[]{
						objItem("dragonminez:dball_radar", 1),
						objItem("dragonminez:namekdball_radar", 1),
						objItem("minecraft:diamond", 8),
						objItem("minecraft:redstone_block", 3),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(18000), rewItem("dragonminez:fused_dball_radar", 1) }));

		// Proximity HUD: enables the "nearest Dragon Ball" distance readout on the radar overlay
		// (RadarRenderEvent checks isQuestCompleted("bulma_proximity_hud")).
		writeQuestFile(dir, "bulma_proximity_hud.json", sidequest(
				"bulma_proximity_hud", "dmz.sidequest.bulma_proximity_hud.name", "dmz.sidequest.bulma_proximity_hud.desc",
				"tech", false, "bulma", "bulma",
				prereqs("AND", condQuest("bulma_radar_fusion")),
				new JsonObject[]{
						objItem("minecraft:ender_eye", 4),
						objItem("minecraft:diamond", 4),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(9000) }));

		// --- Scouter Intelligence ---
		// Each tier re-reveals enemy intel that is redacted by default from the scouter HUD and the
		// quest "enemy preview" card (ScouterHUD / QuestEnemyPreview check these quests via QuestUnlocks).
		// Calibration: unlocks the Battle Power readout.
		writeQuestFile(dir, "bulma_scouter_calibration.json", sidequest(
				"bulma_scouter_calibration", "dmz.sidequest.bulma_scouter_calibration.name", "dmz.sidequest.bulma_scouter_calibration.desc",
				"tech", false, "bulma", "bulma",
				prereqs("AND", condSaga("frieza_saga", 2)),
				new JsonObject[]{
						objItem("minecraft:redstone", 16),
						objItem("minecraft:iron_ingot", 8),
						objItem("minecraft:glass", 8),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(9000) }));

		// Bio-Scan: unlocks the target classification tag (player vs. hostile) on the scouter.
		writeQuestFile(dir, "bulma_scouter_bioscan.json", sidequest(
				"bulma_scouter_bioscan", "dmz.sidequest.bulma_scouter_bioscan.name", "dmz.sidequest.bulma_scouter_bioscan.desc",
				"tech", false, "bulma", "bulma",
				prereqs("AND", condQuest("bulma_scouter_calibration")),
				new JsonObject[]{
						objKill("dragonminez:saga_friezasoldier01", 12),
						objItem("minecraft:amethyst_shard", 8),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(15000) }));

		// Threat Database: unlocks the enemy's attack/threat breakdown in the quest preview card.
		writeQuestFile(dir, "bulma_scouter_threat_db.json", sidequest(
				"bulma_scouter_threat_db", "dmz.sidequest.bulma_scouter_threat_db.name", "dmz.sidequest.bulma_scouter_threat_db.desc",
				"tech", false, "bulma", "bulma",
				prereqs("AND", condQuest("bulma_scouter_bioscan")),
				new JsonObject[]{
						objKill("dragonminez:saga_friezasoldier02", 15),
						objItem("minecraft:diamond", 4),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(24000) }));

		// --- Spacepod — Otherworld Drive ---
		writeQuestFile(dir, "bulma_otherworld_drive.json", sidequest(
				"bulma_otherworld_drive", "dmz.sidequest.bulma_otherworld_drive.name", "dmz.sidequest.bulma_otherworld_drive.desc",
				"tech", false, "bulma", "bulma",
				prereqs("AND", condSaga("saiyan_saga", 5), condQuest("bulma_radar_parts")),
				new JsonObject[]{
						objItem("minecraft:ender_pearl", 8),
						objItem("minecraft:obsidian", 12),
						objItem("minecraft:redstone_block", 4),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(7500) }));

		// --- Gete Tech Tree ---
		// Discovery: bring Bulma the mysterious Gete metal so she can start researching it.
		writeQuestFile(dir, "bulma_gete_discovery.json", sidequest(
				"bulma_gete_discovery", "dmz.sidequest.bulma_gete_discovery.name", "dmz.sidequest.bulma_gete_discovery.desc",
				"tech", false, "bulma", "bulma",
				prereqs("AND", condQuest("bulma_gero_blueprints")),
				new JsonObject[]{
						objItem("dragonminez:gete_scrap", 3),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(24000), rewItem("dragonminez:gete_ingot", 2) }));

		// Gete armor: Bulma works out a post-netherite armor and hands over the crafting pattern.
		writeQuestFile(dir, "bulma_gete_armor.json", sidequest(
				"bulma_gete_armor", "dmz.sidequest.bulma_gete_armor.name", "dmz.sidequest.bulma_gete_armor.desc",
				"tech", false, "bulma", "bulma",
				prereqs("AND", condQuest("bulma_gete_discovery")),
				new JsonObject[]{
						objItem("dragonminez:gete_ingot", 8),
						objItem("dragonminez:kikono_cloth", 4),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(48000), rewItem("dragonminez:pattern_gete", 1) }));

		// Gete enhancements: Bulma's special enchantments, delivered as an enchanted book (COMMAND reward).
		writeQuestFile(dir, "bulma_gete_enhancements.json", sidequest(
				"bulma_gete_enhancements", "dmz.sidequest.bulma_gete_enhancements.name", "dmz.sidequest.bulma_gete_enhancements.desc",
				"tech", false, "bulma", "bulma",
				prereqs("AND", condQuest("bulma_gete_armor")),
				new JsonObject[]{
						objItem("dragonminez:gete_ingot", 6),
						objItem("minecraft:lapis_block", 4),
						objTalkTo("bulma")
				},
				new JsonObject[]{
						rewTPS(60000),
						rewCommand(
								"give %player% minecraft:enchanted_book{StoredEnchantments:[{id:\"dragonminez:ki_conductivity\",lvl:2},{id:\"dragonminez:gravity_forged\",lvl:2}]} 1",
								"dmz.sidequest.reward.gete_enchant_book")
				}));

		// Capsule tech: Bulma builds premium Gete-tech stat capsules (craft base capsule + Gete ingot).
		writeQuestFile(dir, "bulma_capsule_tech.json", sidequest(
				"bulma_capsule_tech", "dmz.sidequest.bulma_capsule_tech.name", "dmz.sidequest.bulma_capsule_tech.desc",
				"tech", false, "bulma", "bulma",
				prereqs("AND", condQuest("bulma_gete_discovery")),
				new JsonObject[]{
						objItem("dragonminez:gete_ingot", 4),
						objItem("minecraft:diamond", 4),
						objTalkTo("bulma")
				},
				new JsonObject[]{
						rewTPS(40000),
						rewItem("dragonminez:gete_red_capsule", 1),
						rewItem("dragonminez:gete_orange_capsule", 1)
				}));

		// Retrofit kit: Bulma teaches you to upgrade netherite gear into Gete armor at a smithing table.
		writeQuestFile(dir, "bulma_gete_retrofit.json", sidequest(
				"bulma_gete_retrofit", "dmz.sidequest.bulma_gete_retrofit.name", "dmz.sidequest.bulma_gete_retrofit.desc",
				"tech", false, "bulma", "bulma",
				prereqs("AND", condQuest("bulma_gete_armor")),
				new JsonObject[]{
						objItem("dragonminez:gete_ingot", 6),
						objItem("minecraft:netherite_ingot", 1),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(52000), rewItem("dragonminez:gete_smithing_template", 1) }));

		// Gravity Room Mk.II/Mk.III: training inside the Time Chamber earns 1.5x / 2x TP (gate in TPGainEvents).
		writeQuestFile(dir, "bulma_gravity_mk2.json", sidequest(
				"bulma_gravity_mk2", "dmz.sidequest.bulma_gravity_mk2.name", "dmz.sidequest.bulma_gravity_mk2.desc",
				"tech", false, "bulma", "bulma",
				prereqs("AND", condQuest("bulma_gete_discovery"), condSaga("android_saga", 11)),
				new JsonObject[]{
						objStructure("dragonminez:timechamber"),
						objItem("dragonminez:gete_ingot", 4),
						objItem("minecraft:piston", 8),
						objItem("minecraft:heavy_weighted_pressure_plate", 4),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(40000) }));

		writeQuestFile(dir, "bulma_gravity_mk3.json", sidequest(
				"bulma_gravity_mk3", "dmz.sidequest.bulma_gravity_mk3.name", "dmz.sidequest.bulma_gravity_mk3.desc",
				"tech", false, "bulma", "bulma",
				prereqs("AND", condQuest("bulma_gravity_mk2"), condSaga("buu_saga", 1)),
				new JsonObject[]{
						objItem("dragonminez:gete_block", 2),
						objItem("minecraft:netherite_ingot", 2),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(72000) }));

		// Ki Accumulator: hand over the parts and Bulma builds energy batteries (consumable ki restore).
		writeQuestFile(dir, "bulma_ki_battery.json", sidequest(
				"bulma_ki_battery", "dmz.sidequest.bulma_ki_battery.name", "dmz.sidequest.bulma_ki_battery.desc",
				"tech", false, "bulma", "bulma",
				prereqs("AND", condQuest("bulma_gete_enhancements"), condSaga("buu_saga", 1)),
				new JsonObject[]{
						objItem("dragonminez:gete_ingot", 2),
						objItem("minecraft:lapis_lazuli", 32),
						objItem("minecraft:redstone_block", 2),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(36000), rewItem("dragonminez:ki_battery", 3) }));

		// Anti-Ki Cloak: a stealth curio that hides your Battle Power from enemy scouters.
		writeQuestFile(dir, "bulma_anti_ki_cloak.json", sidequest(
				"bulma_anti_ki_cloak", "dmz.sidequest.bulma_anti_ki_cloak.name", "dmz.sidequest.bulma_anti_ki_cloak.desc",
				"tech", false, "bulma", "bulma",
				prereqs("AND", condQuest("bulma_scouter_threat_db"), condSaga("android_saga", 14)),
				new JsonObject[]{
						objKill("dragonminez:saga_cell_jr", 5),
						objItem("dragonminez:gete_ingot", 3),
						objItem("minecraft:observer", 4),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(44000), rewItem("dragonminez:anti_ki_cloak", 1) }));

		// Time-Chamber Link: Bulma adds the Hyperbolic Time Chamber as a space-pod destination.
		writeQuestFile(dir, "bulma_time_chamber_link.json", sidequest(
				"bulma_time_chamber_link", "dmz.sidequest.bulma_time_chamber_link.name", "dmz.sidequest.bulma_time_chamber_link.desc",
				"tech", false, "bulma", "bulma",
				prereqs("AND", condQuest("bulma_otherworld_drive"), condSaga("android_saga", 11)),
				new JsonObject[]{
						objStructure("dragonminez:timechamber"),
						objItem("dragonminez:gete_ingot", 3),
						objItem("minecraft:clock", 4),
						objItem("minecraft:ender_chest", 1),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(48000) }));
	}

	// ========================================================================================
	// Bulma Errands (R&D fetch/kill/talk quests)
	// ========================================================================================

	private static void createBulmaErrandsCategory(Path baseDir) {
		Path dir = baseDir.resolve("errands");

		// --- Saiyan Saga ---
		writeQuestFile(dir, "bulma_capsule_development.json", sidequest(
				"bulma_capsule_development", "dmz.sidequest.bulma_capsule_development.name", "dmz.sidequest.bulma_capsule_development.desc",
				"collection", true, "bulma", "bulma", prereqs("AND", condSaga("saiyan_saga", 1)),
				new JsonObject[]{ objItem("minecraft:diamond", 4), objItem("minecraft:redstone", 8), objTalkTo("bulma") },
				new JsonObject[]{ rewTPS(1200) }));

		writeQuestFile(dir, "bulma_rare_mineral_survey.json", sidequest(
				"bulma_rare_mineral_survey", "dmz.sidequest.bulma_rare_mineral_survey.name", "dmz.sidequest.bulma_rare_mineral_survey.desc",
				"collection", true, "bulma", "bulma", prereqs("AND", condSaga("saiyan_saga", 3)),
				new JsonObject[]{ objItem("minecraft:amethyst_shard", 8), objItem("minecraft:copper_ingot", 12), objTalkTo("bulma") },
				new JsonObject[]{ rewTPS(1600) }));

		writeQuestFile(dir, "bulma_weapon_analysis.json", sidequest(
				"bulma_weapon_analysis", "dmz.sidequest.bulma_weapon_analysis.name", "dmz.sidequest.bulma_weapon_analysis.desc",
				"collection", true, "bulma", "bulma", prereqs("AND", condQuest("bulma_radar_parts")),
				new JsonObject[]{ objItem("minecraft:iron_sword", 1), objItem("minecraft:redstone", 12), objItem("minecraft:copper_ingot", 8), objTalkTo("bulma") },
				new JsonObject[]{ rewTPS(1800) }));

		writeQuestFile(dir, "bulma_flight_stabilizer.json", sidequest(
				"bulma_flight_stabilizer", "dmz.sidequest.bulma_flight_stabilizer.name", "dmz.sidequest.bulma_flight_stabilizer.desc",
				"collection", true, "bulma", "bulma", prereqs("AND", condSaga("saiyan_saga", 4)),
				new JsonObject[]{ objItem("minecraft:feather", 16), objItem("minecraft:redstone", 12), objItem("minecraft:gold_ingot", 4), objTalkTo("bulma") },
				new JsonObject[]{ rewTPS(3600) }));

		writeQuestFile(dir, "bulma_saiyan_biology_sample.json", sidequest(
				"bulma_saiyan_biology_sample", "dmz.sidequest.bulma_saiyan_biology_sample.name", "dmz.sidequest.bulma_saiyan_biology_sample.desc",
				"combat", false, "bulma", "bulma", prereqs("AND", condSaga("saiyan_saga", 6)),
				new JsonObject[]{ objQuestKill("dragonminez:saga_raditz", 1), objItem("minecraft:diamond", 3), objTalkTo("bulma") },
				new JsonObject[]{ rewTPS(7500) }));

		writeQuestFile(dir, "bulma_energy_storage_battery.json", sidequest(
				"bulma_energy_storage_battery", "dmz.sidequest.bulma_energy_storage_battery.name", "dmz.sidequest.bulma_energy_storage_battery.desc",
				"collection", true, "bulma", "bulma", prereqs("AND", condQuest("bulma_capsule_development")),
				new JsonObject[]{ objItem("minecraft:lapis_lazuli", 32), objItem("minecraft:redstone_block", 3), objItem("minecraft:diamond", 4), objTalkTo("bulma") },
				new JsonObject[]{ rewTPS(21000) }));

		// --- Frieza Saga ---
		writeQuestFile(dir, "bulma_communication_relay.json", sidequest(
				"bulma_communication_relay", "dmz.sidequest.bulma_communication_relay.name", "dmz.sidequest.bulma_communication_relay.desc",
				"collection", true, "bulma", "bulma", prereqs("AND", condSaga("frieza_saga", 1)),
				new JsonObject[]{ objItem("minecraft:redstone", 16), objItem("minecraft:copper_block", 4), objItem("minecraft:amethyst_shard", 6), objTalkTo("bulma") },
				new JsonObject[]{ rewTPS(6000) }));

		writeQuestFile(dir, "bulma_frieza_armor_study.json", sidequest(
				"bulma_frieza_armor_study", "dmz.sidequest.bulma_frieza_armor_study.name", "dmz.sidequest.bulma_frieza_armor_study.desc",
				"combat", false, "bulma", "bulma", prereqs("AND", condSaga("frieza_saga", 3)),
				new JsonObject[]{ objKill("dragonminez:saga_friezasoldier01", 8), objItem("minecraft:iron_ingot", 16), objTalkTo("bulma") },
				new JsonObject[]{ rewTPS(12000) }));

		writeQuestFile(dir, "bulma_frieza_force_intelligence.json", sidequest(
				"bulma_frieza_force_intelligence", "dmz.sidequest.bulma_frieza_force_intelligence.name", "dmz.sidequest.bulma_frieza_force_intelligence.desc",
				"combat", false, "bulma", "bulma", prereqs("AND", condSaga("frieza_saga", 4)),
				new JsonObject[]{ objKill("dragonminez:saga_friezasoldier02", 10), objItem("minecraft:diamond", 6), objTalkTo("bulma") },
				new JsonObject[]{ rewTPS(15000) }));

		writeQuestFile(dir, "bulma_namekian_tech_integration.json", sidequest(
				"bulma_namekian_tech_integration", "dmz.sidequest.bulma_namekian_tech_integration.name", "dmz.sidequest.bulma_namekian_tech_integration.desc",
				"collection", true, "bulma", "bulma", prereqs("AND", condSaga("frieza_saga", 5)),
				new JsonObject[]{ objItem("dragonminez:kikono_shard", 12), objItem("minecraft:copper_ingot", 16), objItem("minecraft:redstone_block", 2), objTalkTo("bulma") },
				new JsonObject[]{ rewTPS(18000) }));

		writeQuestFile(dir, "bulma_senzu_bean_substitute.json", sidequest(
				"bulma_senzu_bean_substitute", "dmz.sidequest.bulma_senzu_bean_substitute.name", "dmz.sidequest.bulma_senzu_bean_substitute.desc",
				"collection", true, "bulma", "bulma", prereqs("AND", condSaga("frieza_saga", 8)),
				new JsonObject[]{ objItem("minecraft:golden_carrot", 12), objItem("minecraft:glow_berries", 8), objItem("dragonminez:kikono_cloth", 2), objTalkTo("bulma") },
				new JsonObject[]{ rewTPS(9000) }));

		writeQuestFile(dir, "bulma_ki_energy_collector.json", sidequest(
				"bulma_ki_energy_collector", "dmz.sidequest.bulma_ki_energy_collector.name", "dmz.sidequest.bulma_ki_energy_collector.desc",
				"collection", true, "bulma", "bulma", prereqs("AND", condQuest("bulma_scouter_calibration")),
				new JsonObject[]{ objItem("minecraft:amethyst_shard", 12), objItem("minecraft:lapis_lazuli", 16), objTalkTo("bulma") },
				new JsonObject[]{ rewTPS(15000), rewItem("minecraft:golden_apple", 3) }));

		// --- Android / Cell Saga ---
		writeQuestFile(dir, "bulma_android_parts_delivery.json", sidequest(
				"bulma_android_parts_delivery", "dmz.sidequest.bulma_android_parts_delivery.name", "dmz.sidequest.bulma_android_parts_delivery.desc",
				"collection", true, "bulma", "bulma", prereqs("AND", condSaga("android_saga", 3)),
				new JsonObject[]{ objItem("minecraft:redstone", 24), objItem("minecraft:copper_ingot", 12), objItem("dragonminez:kikono_shard", 4), objTalkTo("bulma") },
				new JsonObject[]{ rewTPS(16500) }));

		writeQuestFile(dir, "bulma_power_suppression_unit.json", sidequest(
				"bulma_power_suppression_unit", "dmz.sidequest.bulma_power_suppression_unit.name", "dmz.sidequest.bulma_power_suppression_unit.desc",
				"collection", true, "bulma", "bulma", prereqs("AND", condSaga("android_saga", 4)),
				new JsonObject[]{ objItem("minecraft:redstone", 24), objItem("minecraft:glass", 12), objItem("minecraft:diamond", 4), objTalkTo("bulma") },
				new JsonObject[]{ rewTPS(24000) }));

		writeQuestFile(dir, "bulma_android_tech_reverse_engineer.json", sidequest(
				"bulma_android_tech_reverse_engineer", "dmz.sidequest.bulma_android_tech_reverse_engineer.name", "dmz.sidequest.bulma_android_tech_reverse_engineer.desc",
				"collection", true, "bulma", "bulma", prereqs("AND", condSaga("android_saga", 5)),
				new JsonObject[]{ objItem("minecraft:redstone", 16), objItem("dragonminez:kikono_shard", 6), objTalkTo("bulma") },
				new JsonObject[]{ rewTPS(40000) }));

		writeQuestFile(dir, "bulma_gravity_chamber_parts.json", sidequest(
				"bulma_gravity_chamber_parts", "dmz.sidequest.bulma_gravity_chamber_parts.name", "dmz.sidequest.bulma_gravity_chamber_parts.desc",
				"collection", true, "bulma", "bulma", prereqs("AND", condSaga("android_saga", 6)),
				new JsonObject[]{ objItem("minecraft:obsidian", 20), objItem("minecraft:redstone_block", 4), objItem("minecraft:diamond", 6), objTalkTo("bulma") },
				new JsonObject[]{ rewTPS(48000) }));

		writeQuestFile(dir, "bulma_power_level_detector.json", sidequest(
				"bulma_power_level_detector", "dmz.sidequest.bulma_power_level_detector.name", "dmz.sidequest.bulma_power_level_detector.desc",
				"collection", true, "bulma", "bulma", prereqs("AND", condQuest("bulma_scouter_bioscan")),
				new JsonObject[]{ objItem("minecraft:redstone", 20), objItem("minecraft:glass", 16), objItem("minecraft:amethyst_shard", 8), objTalkTo("bulma") },
				new JsonObject[]{ rewTPS(36000) }));

		writeQuestFile(dir, "bulma_time_chamber_diagnostics.json", sidequest(
				"bulma_time_chamber_diagnostics", "dmz.sidequest.bulma_time_chamber_diagnostics.name", "dmz.sidequest.bulma_time_chamber_diagnostics.desc",
				"exploration", false, "bulma", "bulma", prereqs("AND", condSaga("android_saga", 10)),
				new JsonObject[]{ objStructure("dragonminez:timechamber"), objItem("minecraft:redstone", 20), objItem("minecraft:diamond", 8), objTalkTo("bulma") },
				new JsonObject[]{ rewTPS(60000) }));

		// --- Buu Saga ---
		writeQuestFile(dir, "bulma_buu_energy_analysis.json", sidequest(
				"bulma_buu_energy_analysis", "dmz.sidequest.bulma_buu_energy_analysis.name", "dmz.sidequest.bulma_buu_energy_analysis.desc",
				"combat", false, "bulma", "bulma", prereqs("AND", condSaga("buu_saga", 5)),
				new JsonObject[]{ objKill("dragonminez:saga_buufat", 5), objItem("minecraft:lapis_block", 3), objTalkTo("bulma") },
				new JsonObject[]{ rewTPS(160000) }));

		writeQuestFile(dir, "bulma_emergency_teleport_device.json", sidequest(
				"bulma_emergency_teleport_device", "dmz.sidequest.bulma_emergency_teleport_device.name", "dmz.sidequest.bulma_emergency_teleport_device.desc",
				"collection", true, "bulma", "bulma", prereqs("AND", condSaga("buu_saga", 8)),
				new JsonObject[]{ objItem("minecraft:ender_pearl", 12), objItem("minecraft:obsidian", 8), objItem("minecraft:redstone_block", 3), objTalkTo("bulma") },
				new JsonObject[]{ rewTPS(140000) }));
	}

	// ========================================================================================
	// ===== Movies Saga Side-Quests =====
	// ========================================================================================
	//
	// Gated on Movies-saga story progress (sagaId "movies_saga", 40 story quests). The Movies
	// saga itself unlocks after buu_saga 35. Sidequests cover the movie villains — Garlic Jr.,
	// Dr. Wheelo, Turles, Lord Slug, Cooler, the Big Gete Star, the cold androids, Bojack's crew,
	// and the Otherworld chaos (Janemba / Pikkon rematch).

	private static void createMoviesCategory(Path baseDir) {
		Path dir = baseDir.resolve("movies");

		// --- Garlic Jr. arc ---

		writeQuestFile(dir, "dende_lookout_vigil.json", sidequest(
				"dende_lookout_vigil", "dmz.sidequest.dende_lookout_vigil.name", "dmz.sidequest.dende_lookout_vigil.desc",
				"story", false, "dende", "dende",
				prereqs("AND", condSaga("movies_saga", 1)),
				requirements("AND", condStructure("dragonminez:kamilookout")),
				new JsonObject[]{
						objStructure("dragonminez:kamilookout"),
						objTalkTo("dende")
				},
				new JsonObject[]{ rewTPS(60000) }));

		writeQuestFile(dir, "garlic_jr_spice_boys.json", sidequest(
				"garlic_jr_spice_boys", "dmz.sidequest.garlic_jr_spice_boys.name", "dmz.sidequest.garlic_jr_spice_boys.desc",
				"combat", false, "popo", "popo",
				prereqs("AND", condSaga("movies_saga", 3)),
				requirements("AND", condBiome("dragonminez:rocky")),
				new JsonObject[]{
						objQuestKill("dragonminez:saga_garlick_jr", 3),
						objTalkTo("popo")
				},
				new JsonObject[]{ rewTPS(72000) }));

		// --- Dr. Wheelo arc ---

		writeQuestFile(dir, "wheelo_frozen_rescue.json", sidequest(
				"wheelo_frozen_rescue", "dmz.sidequest.wheelo_frozen_rescue.name", "dmz.sidequest.wheelo_frozen_rescue.desc",
				"collection", true, "bulma", "bulma",
				prereqs("AND", condSaga("movies_saga", 6)),
				requirements("AND", condBiome("minecraft:snowy_plains")),
				new JsonObject[]{
						objItem("dragonminez:gete_scrap", 4),
						objItem("minecraft:redstone", 24),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(88000) }));

		// --- Turles arc ---

		writeQuestFile(dir, "turles_tree_sapling.json", sidequest(
				"turles_tree_sapling", "dmz.sidequest.turles_tree_sapling.name", "dmz.sidequest.turles_tree_sapling.desc",
				"collection", true, "piccolo", "piccolo",
				prereqs("AND", condSaga("movies_saga", 10)),
				new JsonObject[]{
						objQuestKill("dragonminez:saga_turles", 1),
						objItem("minecraft:oak_sapling", 12),
						objItem("minecraft:apple", 16),
						objTalkTo("piccolo")
				},
				new JsonObject[]{ rewTPS(112000) }));

		// --- Lord Slug arc ---

		writeQuestFile(dir, "slug_aftermath.json", sidequest(
				"slug_aftermath", "dmz.sidequest.slug_aftermath.name", "dmz.sidequest.slug_aftermath.desc",
				"combat", false, "guru", "guru",
				prereqs("AND", condSaga("movies_saga", 13)),
				requirements("AND", condBiome("minecraft:plains")),
				new JsonObject[]{
						objKill("dragonminez:saga_slug_soldier", 20),
						objTalkTo("guru")
				},
				new JsonObject[]{ rewTPS(128000) }));

		// --- Cooler arc ---

		writeQuestFile(dir, "cooler_squadron_mopup.json", sidequest(
				"cooler_squadron_mopup", "dmz.sidequest.cooler_squadron_mopup.name", "dmz.sidequest.cooler_squadron_mopup.desc",
				"combat", true, "krillin", "krillin",
				prereqs("AND", condSaga("movies_saga", 16)),
				new JsonObject[]{
						objKill("dragonminez:saga_salza", 4),
						objKill("dragonminez:saga_dore", 4),
						objKill("dragonminez:saga_neiz", 4)
				},
				new JsonObject[]{ rewTPS(152000) }));

		// --- Big Gete Star arc ---

		writeQuestFile(dir, "big_gete_salvage.json", sidequest(
				"big_gete_salvage", "dmz.sidequest.big_gete_salvage.name", "dmz.sidequest.big_gete_salvage.desc",
				"collection", true, "bulma", "bulma",
				prereqs("AND", condSaga("movies_saga", 19)),
				new JsonObject[]{
						objQuestKill("dragonminez:saga_gete_robot", 8),
						objItem("dragonminez:gete_scrap", 8),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(180000), rewItem("dragonminez:gete_ingot", 4) }));

		// --- Cold androids arc (Androids 13/14/15) ---

		writeQuestFile(dir, "android_cold_parts.json", sidequest(
				"android_cold_parts", "dmz.sidequest.android_cold_parts.name", "dmz.sidequest.android_cold_parts.desc",
				"collection", true, "bulma", "bulma",
				prereqs("AND", condSaga("movies_saga", 22)),
				requirements("AND", condBiome("minecraft:snowy_plains")),
				new JsonObject[]{
						objQuestKill("dragonminez:saga_a14", 1),
						objQuestKill("dragonminez:saga_a15", 1),
						objItem("dragonminez:gete_scrap", 6),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(208000), rewItem("dragonminez:gete_ingot", 3) }));

		// --- Bojack arc ---

		writeQuestFile(dir, "bojack_tournament_cleanup.json", sidequest(
				"bojack_tournament_cleanup", "dmz.sidequest.bojack_tournament_cleanup.name", "dmz.sidequest.bojack_tournament_cleanup.desc",
				"combat", true, "gohan", "gohan",
				prereqs("AND", condSaga("movies_saga", 29)),
				new JsonObject[]{
						objQuestKill("dragonminez:saga_zangya", 1),
						objQuestKill("dragonminez:saga_bido", 1),
						objQuestKill("dragonminez:saga_bujin", 1),
						objQuestKill("dragonminez:saga_gokua", 1),
						objTalkTo("gohan")
				},
				new JsonObject[]{ rewTPS(260000) }));

		// --- Otherworld arc (Pikkon / Janemba) ---

		writeQuestFile(dir, "otherworld_pikkon_rematch.json", sidequest(
				"otherworld_pikkon_rematch", "dmz.sidequest.otherworld_pikkon_rematch.name", "dmz.sidequest.otherworld_pikkon_rematch.desc",
				"combat", false, "kingkai", "kingkai",
				prereqs("AND", condSaga("movies_saga", 37)),
				requirements("AND", condDimension("dragonminez:otherworld")),
				new JsonObject[]{
						objQuestKill("dragonminez:saga_paikuhan", 1),
						objTalkTo("kingkai")
				},
				new JsonObject[]{ rewTPS(360000), rewItem("dragonminez:senzu_bean", 3) }));
	}
}


