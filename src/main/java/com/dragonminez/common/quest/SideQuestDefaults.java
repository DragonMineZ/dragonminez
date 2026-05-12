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
 * Side-quests use the same schema as saga quests â€” only the {@code "type"} field
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

		JsonObject effectiveStartRequirements = requiresGoodPath(questGiver, turnIn)
				? withAlignmentRequirement(startRequirements)
				: startRequirements;

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

	// ---- Reward helpers ----

	private static JsonObject rewTPS(int amount) {
		JsonObject r = new JsonObject(); r.addProperty("type", "TPS"); r.addProperty("amount", amount); return r;
	}

	private static JsonObject rewItem(String itemId, int count) {
		JsonObject r = new JsonObject(); r.addProperty("type", "ITEM"); r.addProperty("item", itemId); r.addProperty("count", count); return r;
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
		conditions.add(condAlignmentMin(41));
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
						objStructure("dragonminez:roshi_house"),
						objKill("minecraft:zombie", 10),
						objTalkTo("roshi")
				},
				new JsonObject[]{ rewTPS(300) }));

		writeQuestFile(dir, "endurance_training.json", sidequest(
				"endurance_training", "dmz.sidequest.endurance.name", "dmz.sidequest.endurance.desc",
				"training", false, "roshi", "roshi",
				prereqs("AND", condQuest("roshi_basic_training")),
				new JsonObject[]{
						objKill("minecraft:skeleton", 15),
						objKill("minecraft:spider", 10),
						objTalkTo("roshi")
				},
				new JsonObject[]{ rewTPS(500) }));

		writeQuestFile(dir, "weighted_training.json", sidequest(
				"weighted_training", "dmz.sidequest.weighted.name", "dmz.sidequest.weighted.desc",
				"training", false, "roshi", "roshi",
				prereqs("AND", condQuest("endurance_training"), condSaga("saiyan_saga", 2)),
				new JsonObject[]{
						objKill("minecraft:iron_golem", 3),
						objItem("minecraft:iron_ingot", 32),
						objTalkTo("roshi")
				},
				new JsonObject[]{ rewTPS(1000), rewItem("minecraft:golden_apple", 5) }));

		writeQuestFile(dir, "gravity_chamber.json", sidequest(
				"gravity_chamber", "dmz.sidequest.gravity.name", "dmz.sidequest.gravity.desc",
				"training", true, "goku", "goku",
				prereqs("AND", condQuest("weighted_training"), condLevel(10)),
				new JsonObject[]{
						objKill("minecraft:wither_skeleton", 5),
						objKill("minecraft:blaze", 10),
						objItem("minecraft:blaze_rod", 10),
						objTalkTo("goku")
				},
				new JsonObject[]{ rewTPS(2000) }));

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
				new JsonObject[]{ rewTPS(400) }));

		writeQuestFile(dir, "tien_mountain_training.json", sidequest(
				"tien_mountain_training", "dmz.sidequest.tien_mountain.name", "dmz.sidequest.tien_mountain.desc",
				"training", false, "tien", "tien",
				prereqs("AND", condSaga("saiyan_saga", 4)),
				requirements("AND", condBiome("#minecraft:is_mountain")),
				new JsonObject[]{
						objKill("minecraft:iron_golem", 5),
						objTalkTo("tien")
				},
				new JsonObject[]{ rewTPS(700) }));

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
				new JsonObject[]{ rewTPS(900) }));

		writeQuestFile(dir, "gohan_survival.json", sidequest(
				"gohan_survival", "dmz.sidequest.gohan_survival.name", "dmz.sidequest.gohan_survival.desc",
				"training", false, "piccolo", "gohan",
				prereqs("AND", condSaga("saiyan_saga", 2)),
				new JsonObject[]{
						objItem("minecraft:cooked_beef", 16),
						objItem("minecraft:iron_sword", 1),
						objTalkTo("gohan")
				},
				new JsonObject[]{ rewTPS(500) }));

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
				new JsonObject[]{ rewTPS(15000) }));

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
				new JsonObject[]{ rewTPS(18000) }));
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
				new JsonObject[]{ rewTPS(400) }));

		writeQuestFile(dir, "earth_landmarks.json", sidequest(
				"earth_landmarks", "dmz.sidequest.earth_landmarks.name", "dmz.sidequest.earth_landmarks.desc",
				"exploration", true, null, null,
				prereqs("AND", condQuest("world_explorer")),
				new JsonObject[]{
						objStructure("dragonminez:roshi_house"),
						objStructure("dragonminez:goku_house"),
						objStructure("dragonminez:kamilookout")
				},
				new JsonObject[]{ rewTPS(900) }));

		writeQuestFile(dir, "namek_explorer.json", sidequest(
				"namek_explorer", "dmz.sidequest.namek_explorer.name", "dmz.sidequest.namek_explorer.desc",
				"exploration", true, null, null,
				prereqs("AND", condSaga("saiyan_saga", 8)),
				new JsonObject[]{
						objBiome("dragonminez:ajissa_plains"),
						objStructure("dragonminez:village_ajissa")
				},
				new JsonObject[]{ rewTPS(800) }));

		writeQuestFile(dir, "sacred_lands.json", sidequest(
				"sacred_lands", "dmz.sidequest.sacred_lands.name", "dmz.sidequest.sacred_lands.desc",
				"exploration", false, null, null,
				prereqs("AND", condQuest("namek_explorer")),
				new JsonObject[]{
						objStructure("dragonminez:village_sacred")
				},
				new JsonObject[]{ rewTPS(1200) }));

		// --- Frieza Saga: Exploration ---

		writeQuestFile(dir, "krillin_namek_scout.json", sidequest(
				"krillin_namek_scout", "dmz.sidequest.krillin_scout.name", "dmz.sidequest.krillin_scout.desc",
				"exploration", false, "krillin", "krillin",
				prereqs("AND", condSaga("frieza_saga", 2)),
				new JsonObject[]{
						objStructure("dragonminez:village_ajissa"),
						objTalkTo("krillin")
				},
				new JsonObject[]{ rewTPS(4000) }));

		writeQuestFile(dir, "namek_farewell.json", sidequest(
				"namek_farewell", "dmz.sidequest.namek_farewell.name", "dmz.sidequest.namek_farewell.desc",
				"exploration", false, "namek_elder", "namek_elder",
				prereqs("AND", condSaga("frieza_saga", 15)),
				new JsonObject[]{
						objStructure("dragonminez:village_sacred"),
						objTalkTo("namek_elder")
				},
				new JsonObject[]{ rewTPS(6000) }));

		// --- Android Saga: Exploration ---

		writeQuestFile(dir, "trunks_warning.json", sidequest(
				"trunks_warning", "dmz.sidequest.trunks_warning.name", "dmz.sidequest.trunks_warning.desc",
				"exploration", false, "trunks", "trunks",
				prereqs("AND", condSaga("android_saga", 3)),
				new JsonObject[]{
						objStructure("dragonminez:gero_lab"),
						objTalkTo("trunks")
				},
				new JsonObject[]{ rewTPS(10000) }));

		writeQuestFile(dir, "piccolo_kami_fusion.json", sidequest(
				"piccolo_kami_fusion", "dmz.sidequest.piccolo_kami.name", "dmz.sidequest.piccolo_kami.desc",
				"exploration", false, "piccolo", "piccolo",
				prereqs("AND", condSaga("android_saga", 10)),
				new JsonObject[]{
						objStructure("dragonminez:kamilookout"),
						objTalkTo("piccolo")
				},
				new JsonObject[]{ rewTPS(13000) }));
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
				new JsonObject[]{ rewTPS(600) }));

		writeQuestFile(dir, "nether_warrior.json", sidequest(
				"nether_warrior", "dmz.sidequest.nether_warrior.name", "dmz.sidequest.nether_warrior.desc",
				"combat", false, null, null,
				prereqs("AND", condQuest("monster_hunter"), condLevel(5)),
				requirements("AND", condDimension("minecraft:the_nether")),
				new JsonObject[]{
						objKill("minecraft:blaze", 10),
						objKill("minecraft:wither_skeleton", 10)
				},
				new JsonObject[]{ rewTPS(1500), rewItem("minecraft:diamond", 3) }));

		writeQuestFile(dir, "dragon_ball_hunter.json", sidequest(
				"dragon_ball_hunter", "dmz.sidequest.dball_hunter.name", "dmz.sidequest.dball_hunter.desc",
				"combat", false, null, null,
				prereqs("AND", condSaga("saiyan_saga", 4)),
				new JsonObject[]{
						objStructure("minecraft:pillager_outpost"),
						objKill("minecraft:pillager", 15)
				},
				new JsonObject[]{ rewTPS(1000) }));

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
				new JsonObject[]{ rewTPS(600) }));

		writeQuestFile(dir, "farmer_red_ribbon_threat.json", sidequest(
				"farmer_red_ribbon_threat", "dmz.sidequest.farmer_rr.name", "dmz.sidequest.farmer_rr.desc",
				"combat", false, "farmer_01", "farmer_01",
				prereqs("AND", condSaga("saiyan_saga", 1)),
				new JsonObject[]{
						objKill("dragonminez:red_ribbon_soldier", 10),
						objKill("dragonminez:robot1", 2),
						objTalkTo("farmer_01")
				},
				new JsonObject[]{ rewTPS(600), rewItem("minecraft:golden_apple", 3) }));

		// --- Frieza Saga: Combat ---

		writeQuestFile(dir, "namek_elder_blessing.json", sidequest(
				"namek_elder_blessing", "dmz.sidequest.elder_blessing.name", "dmz.sidequest.elder_blessing.desc",
				"combat", false, "namek_elder", "namek_elder",
				prereqs("AND", condSaga("frieza_saga", 2)),
				new JsonObject[]{
						objKill("dragonminez:saga_friezasoldier01", 10),
						objTalkTo("namek_elder")
				},
				new JsonObject[]{ rewTPS(3000) }));

		writeQuestFile(dir, "ginyu_force_warm_up.json", sidequest(
				"ginyu_force_warm_up", "dmz.sidequest.ginyu_warmup.name", "dmz.sidequest.ginyu_warmup.desc",
				"combat", true, "goku", "goku",
				prereqs("AND", condSaga("frieza_saga", 6)),
				requirements("AND", condBiome("dragonminez:ajissa_plains")),
				new JsonObject[]{
						objKill("dragonminez:saga_friezasoldier01", 8),
						objKill("dragonminez:saga_friezasoldier02", 8)
				},
				new JsonObject[]{ rewTPS(7000) }));

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
				new JsonObject[]{ rewTPS(12000) }));

		writeQuestFile(dir, "tien_cell_search.json", sidequest(
				"tien_cell_search", "dmz.sidequest.tien_cell.name", "dmz.sidequest.tien_cell.desc",
				"combat", true, "tien", "tien",
				prereqs("AND", condSaga("android_saga", 10)),
				requirements("AND", condBiome("minecraft:plains")),
				new JsonObject[]{
						objKill("minecraft:zombie", 30),
						objKill("minecraft:spider", 15)
				},
				new JsonObject[]{ rewTPS(16000) }));

		writeQuestFile(dir, "videl_city_defense.json", sidequest(
				"videl_city_defense", "dmz.sidequest.videl_defense.name", "dmz.sidequest.videl_defense.desc",
				"combat", false, "videl", "videl",
				prereqs("AND", condSaga("android_saga", 14)),
				new JsonObject[]{
						objQuestKill("dragonminez:saga_cell_jr", 10),
						objTalkTo("videl")
				},
				new JsonObject[]{ rewTPS(20000) }));
	}

	// ========================================================================================
	// Story Side-Quests
	// ========================================================================================

	private static void createStoryCategory(Path baseDir) {
		Path dir = baseDir.resolve("story");

		// --- Saiyan Saga: Story ---

		writeQuestFile(dir, "chiaotzu_rescue.json", sidequest(
				"chiaotzu_rescue", "dmz.sidequest.chiaotzu_rescue.name", "dmz.sidequest.chiaotzu_rescue.desc",
				"story", false, "tien", "chiaotzu",
				prereqs("AND", condSaga("saiyan_saga", 5)),
				new JsonObject[]{
						objBiome("minecraft:nether_wastes"),
						objItem("minecraft:ender_pearl", 4),
						objTalkTo("chiaotzu")
				},
				new JsonObject[]{ rewTPS(1000) }));

		writeQuestFile(dir, "scholar_saiyan_lore.json", sidequest(
				"scholar_saiyan_lore", "dmz.sidequest.scholar_lore.name", "dmz.sidequest.scholar_lore.desc",
				"story", false, "scholar_01", "scholar_01",
				prereqs("AND", condSaga("saiyan_saga", 7)),
				new JsonObject[]{
						objStructure("dragonminez:goku_house"),
						objBiome("dragonminez:rocky"),
						objTalkTo("scholar_01")
				},
				new JsonObject[]{ rewTPS(600) }));

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
				new JsonObject[]{ rewTPS(5000) }));

		writeQuestFile(dir, "goku_healing_pod.json", sidequest(
				"goku_healing_pod", "dmz.sidequest.goku_healing.name", "dmz.sidequest.goku_healing.desc",
				"story", false, "goku", "goku",
				prereqs("AND", condSaga("frieza_saga", 11)),
				requirements("AND", condStructure("dragonminez:elder_guru")),
				new JsonObject[]{
						objKill("dragonminez:saga_friezasoldier01", 20),
						objTalkTo("goku")
				},
				new JsonObject[]{ rewTPS(8000) }));

		// --- Android Saga: Story ---

		writeQuestFile(dir, "chi_chi_gohan_worry.json", sidequest(
				"chi_chi_gohan_worry", "dmz.sidequest.chichi_worry.name", "dmz.sidequest.chichi_worry.desc",
				"story", false, "chi_chi", "chi_chi",
				prereqs("AND", condSaga("android_saga", 14)),
				new JsonObject[]{
						objItem("minecraft:iron_sword", 1),
						objItem("minecraft:cooked_beef", 16),
						objTalkTo("chi_chi")
				},
				new JsonObject[]{ rewTPS(5000), rewItem("minecraft:golden_apple", 5) }));

		writeQuestFile(dir, "goku_final_farewell.json", sidequest(
				"goku_final_farewell", "dmz.sidequest.goku_farewell.name", "dmz.sidequest.goku_farewell.desc",
				"story", false, "goku", "goku",
				prereqs("AND", condSaga("android_saga", 15)),
				new JsonObject[]{
						objStructure("dragonminez:kamilookout"),
						objTalkTo("goku")
				},
				new JsonObject[]{ rewTPS(25000) }));
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
				new JsonObject[]{ rewTPS(800), rewItem("dragonminez:dball_radar", 1) }));

		writeQuestFile(dir, "collect_dragon_balls.json", sidequest(
				"collect_dragon_balls", "dmz.sidequest.collect_dballs.name", "dmz.sidequest.collect_dballs.desc",
				"collection", false, "bulma", "bulma",
				prereqs("AND", condSaga("saiyan_saga", 5), condQuest("bulma_radar_parts")),
				new JsonObject[]{
						objDragonSummon("shenron", "earth"),
						objTalkTo("bulma")
				},
				new JsonObject[]{ rewTPS(2200) }));

		writeQuestFile(dir, "chi_chi_provisions.json", sidequest(
				"chi_chi_provisions", "dmz.sidequest.chichi_provisions.name", "dmz.sidequest.chichi_provisions.desc",
				"collection", true, "chi_chi", "chi_chi",
				prereqs("AND", condSaga("saiyan_saga", 5)),
				new JsonObject[]{
						objItem("minecraft:cooked_beef", 32),
						objItem("minecraft:bread", 32),
						objItem("minecraft:golden_carrot", 8)
				},
				new JsonObject[]{ rewTPS(400), rewItem("minecraft:golden_apple", 5) }));

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
				new JsonObject[]{ rewTPS(3500), rewItem("dragonminez:namekdball_radar", 1) }));

		writeQuestFile(dir, "merchant_alien_artifacts.json", sidequest(
				"merchant_alien_artifacts", "dmz.sidequest.merchant_artifacts.name", "dmz.sidequest.merchant_artifacts.desc",
				"collection", false, "merchant_01", "merchant_01",
				prereqs("AND", condSaga("frieza_saga", 4)),
				new JsonObject[]{
						objItem("dragonminez:kikono_shard", 16),
						objItem("minecraft:diamond", 8),
						objTalkTo("merchant_01")
				},
				new JsonObject[]{ rewTPS(4500), rewItem("minecraft:diamond_sword", 1) }));

		writeQuestFile(dir, "gohan_dragon_balls_namek.json", sidequest(
				"gohan_dragon_balls_namek", "dmz.sidequest.gohan_namek_db.name", "dmz.sidequest.gohan_namek_db.desc",
				"collection", false, "namek_elder", "namek_elder",
				prereqs("AND", condSaga("frieza_saga", 3), condQuest("bulma_namek_research")),
				new JsonObject[]{
						objDragonSummon("porunga", "namek"),
						objTalkTo("namek_elder")
				},
				new JsonObject[]{ rewTPS(9000) }));

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
				new JsonObject[]{ rewTPS(14000) }));
	}
}


