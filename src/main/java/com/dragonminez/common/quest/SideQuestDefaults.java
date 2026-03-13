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

/**
 * Generates the default side-quest JSON files in the unified schema.
 * <p>
 * Side-quests use the same schema as saga quests — only the {@code "type"} field
 * is set to {@code "SIDEQUEST"} and there is no {@code "chain"} block.
 * <p>
 * Called by {@link QuestRegistry} during side-quest loading.
 *
 * @since 2.1
 */
final class SideQuestDefaults {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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
		if (Files.exists(file)) return;
		try {
			Files.createDirectories(dir);
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
				prerequisites, objectives, rewards, null, null);
	}

	private static JsonObject sidequest(String id, String title, String desc, String category,
										boolean parallelObjectives,
										String questGiver, String turnIn,
										JsonObject prerequisites,
										JsonObject[] objectives, JsonObject[] rewards,
										String branchGroup, String branchPath) {
		JsonObject q = new JsonObject();
		q.addProperty("id", id);
		q.addProperty("title", title);
		q.addProperty("description", desc);
		q.addProperty("type", "SIDEQUEST");
		q.addProperty("category", category);
		q.addProperty("parallel_objectives", parallelObjectives);
		if (questGiver != null) q.addProperty("quest_giver", questGiver);
		else q.add("quest_giver", JsonNull.INSTANCE);
		if (turnIn != null) q.addProperty("turn_in", turnIn);
		else q.add("turn_in", JsonNull.INSTANCE);

		if (branchGroup != null && branchPath != null) {
			JsonObject branch = new JsonObject();
			branch.addProperty("group", branchGroup);
			branch.addProperty("path", branchPath);
			q.add("branch", branch);
		}

		if (prerequisites != null) q.add("prerequisites", prerequisites);

		JsonArray objArr = new JsonArray(); for (JsonObject o : objectives) objArr.add(o);
		q.add("objectives", objArr);
		JsonArray rewArr = new JsonArray(); for (JsonObject r : rewards) rewArr.add(r);
		q.add("rewards", rewArr);
		return q;
	}

	// ---- Objective helpers ----

	private static JsonObject objKill(String desc, String entity, int count) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "KILL"); o.addProperty("description", desc);
		o.addProperty("entity", entity); o.addProperty("count", count);
		return o;
	}

	private static JsonObject objStructure(String desc, String structureId) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "STRUCTURE"); o.addProperty("description", desc); o.addProperty("structure", structureId);
		return o;
	}

	private static JsonObject objBiome(String desc, String biomeId) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "BIOME"); o.addProperty("description", desc); o.addProperty("biome", biomeId);
		return o;
	}

	private static JsonObject objItem(String desc, String itemId, int count) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "ITEM"); o.addProperty("description", desc);
		o.addProperty("item", itemId); o.addProperty("count", count);
		return o;
	}

	private static JsonObject objTalkTo(String desc, String npcId) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "TALK_TO"); o.addProperty("description", desc); o.addProperty("npcId", npcId);
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

	private static JsonObject condSaga(String sagaId, int questId) {
		JsonObject c = new JsonObject(); c.addProperty("type", "SAGA_QUEST"); c.addProperty("sagaId", sagaId); c.addProperty("questId", questId); return c;
	}

	private static JsonObject condQuest(String questId) {
		JsonObject c = new JsonObject(); c.addProperty("type", "QUEST"); c.addProperty("questId", questId); return c;
	}

	private static JsonObject condLevel(int minLevel) {
		JsonObject c = new JsonObject(); c.addProperty("type", "LEVEL"); c.addProperty("minLevel", minLevel); return c;
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
						objStructure("dmz.sidequest.roshi_basic.obj1", "dragonminez:roshi_house"),
						objKill("dmz.sidequest.roshi_basic.obj2", "minecraft:zombie", 10),
						objTalkTo("dmz.sidequest.roshi_basic.obj3", "roshi")
				},
				new JsonObject[]{ rewTPS(300) }));

		writeQuestFile(dir, "endurance_training.json", sidequest(
				"endurance_training", "dmz.sidequest.endurance.name", "dmz.sidequest.endurance.desc",
				"training", false, "roshi", "roshi",
				prereqs("AND", condQuest("roshi_basic_training")),
				new JsonObject[]{
						objKill("dmz.sidequest.endurance.obj1", "minecraft:skeleton", 15),
						objKill("dmz.sidequest.endurance.obj2", "minecraft:spider", 10),
						objTalkTo("dmz.sidequest.endurance.obj3", "roshi")
				},
				new JsonObject[]{ rewTPS(500) }));

		writeQuestFile(dir, "weighted_training.json", sidequest(
				"weighted_training", "dmz.sidequest.weighted.name", "dmz.sidequest.weighted.desc",
				"training", false, "roshi", "roshi",
				prereqs("AND", condQuest("endurance_training"), condSaga("saiyan_saga", 2)),
				new JsonObject[]{
						objKill("dmz.sidequest.weighted.obj1", "minecraft:iron_golem", 3),
						objItem("dmz.sidequest.weighted.obj2", "minecraft:iron_ingot", 32),
						objTalkTo("dmz.sidequest.weighted.obj3", "roshi")
				},
				new JsonObject[]{ rewTPS(1000), rewItem("minecraft:golden_apple", 5) }));

		writeQuestFile(dir, "gravity_chamber.json", sidequest(
				"gravity_chamber", "dmz.sidequest.gravity.name", "dmz.sidequest.gravity.desc",
				"training", true, "goku", "goku",
				prereqs("AND", condQuest("weighted_training"), condLevel(10)),
				new JsonObject[]{
						objKill("dmz.sidequest.gravity.obj1", "minecraft:wither_skeleton", 5),
						objKill("dmz.sidequest.gravity.obj2", "minecraft:blaze", 10),
						objItem("dmz.sidequest.gravity.obj3", "minecraft:blaze_rod", 10),
						objTalkTo("dmz.sidequest.gravity.obj4", "goku")
				},
				new JsonObject[]{ rewTPS(2000) }));

		// --- Saiyan Saga: Training ---

		writeQuestFile(dir, "krillin_sparring.json", sidequest(
				"krillin_sparring", "dmz.sidequest.krillin_sparring.name", "dmz.sidequest.krillin_sparring.desc",
				"training", false, "krillin", "krillin",
				prereqs("AND", condSaga("saiyan_saga", 1)),
				new JsonObject[]{
						objKill("dmz.sidequest.krillin_sparring.obj1", "minecraft:zombie", 10),
						objKill("dmz.sidequest.krillin_sparring.obj2", "minecraft:skeleton", 5),
						objTalkTo("dmz.sidequest.krillin_sparring.obj3", "krillin")
				},
				new JsonObject[]{ rewTPS(400) }));

		writeQuestFile(dir, "tien_mountain_training.json", sidequest(
				"tien_mountain_training", "dmz.sidequest.tien_mountain.name", "dmz.sidequest.tien_mountain.desc",
				"training", false, "tien", "tien",
				prereqs("AND", condSaga("saiyan_saga", 4)),
				new JsonObject[]{
						objBiome("dmz.sidequest.tien_mountain.obj1", "#minecraft:is_mountain"),
						objKill("dmz.sidequest.tien_mountain.obj2", "minecraft:iron_golem", 5),
						objTalkTo("dmz.sidequest.tien_mountain.obj3", "tien")
				},
				new JsonObject[]{ rewTPS(700) },
				"saiyan_training_focus",
				"tien_route"));

		writeQuestFile(dir, "piccolo_wilderness_survival.json", sidequest(
				"piccolo_wilderness_survival", "dmz.sidequest.piccolo_survival.name", "dmz.sidequest.piccolo_survival.desc",
				"training", true, "piccolo", "piccolo",
				prereqs("AND", condSaga("saiyan_saga", 4)),
				new JsonObject[]{
						objBiome("dmz.sidequest.piccolo_survival.obj1", "minecraft:forest"),
						objKill("dmz.sidequest.piccolo_survival.obj2", "minecraft:zombie", 20),
						objKill("dmz.sidequest.piccolo_survival.obj3", "minecraft:skeleton", 10),
						objKill("dmz.sidequest.piccolo_survival.obj4", "minecraft:creeper", 5)
				},
				new JsonObject[]{ rewTPS(900) },
				"saiyan_training_focus",
				"piccolo_route"));

		writeQuestFile(dir, "gohan_survival.json", sidequest(
				"gohan_survival", "dmz.sidequest.gohan_survival.name", "dmz.sidequest.gohan_survival.desc",
				"training", false, "piccolo", "gohan",
				prereqs("AND", condSaga("saiyan_saga", 2)),
				new JsonObject[]{
						objItem("dmz.sidequest.gohan_survival.obj1", "minecraft:cooked_beef", 16),
						objItem("dmz.sidequest.gohan_survival.obj2", "minecraft:iron_sword", 1),
						objTalkTo("dmz.sidequest.gohan_survival.obj3", "gohan")
				},
				new JsonObject[]{ rewTPS(500) }));

		// --- Android Saga: Training ---

		writeQuestFile(dir, "vegeta_pride_training.json", sidequest(
				"vegeta_pride_training", "dmz.sidequest.vegeta_pride.name", "dmz.sidequest.vegeta_pride.desc",
				"training", false, "vegeta", "vegeta",
				prereqs("AND", condSaga("android_saga", 3)),
				new JsonObject[]{
						objStructure("dmz.sidequest.vegeta_pride.obj1", "dragonminez:timechamber"),
						objKill("dmz.sidequest.vegeta_pride.obj2", "minecraft:wither_skeleton", 10),
						objKill("dmz.sidequest.vegeta_pride.obj3", "minecraft:blaze", 5),
						objTalkTo("dmz.sidequest.vegeta_pride.obj4", "vegeta")
				},
				new JsonObject[]{ rewTPS(15000) }));

		writeQuestFile(dir, "gohan_time_chamber_training.json", sidequest(
				"gohan_time_chamber_training", "dmz.sidequest.gohan_timechamber.name", "dmz.sidequest.gohan_timechamber.desc",
				"training", false, "gohan", "gohan",
				prereqs("AND", condSaga("android_saga", 11)),
				new JsonObject[]{
						objStructure("dmz.sidequest.gohan_timechamber.obj1", "dragonminez:timechamber"),
						objKill("dmz.sidequest.gohan_timechamber.obj2", "minecraft:phantom", 20),
						objItem("dmz.sidequest.gohan_timechamber.obj3", "minecraft:golden_apple", 8),
						objTalkTo("dmz.sidequest.gohan_timechamber.obj4", "gohan")
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
						objBiome("dmz.sidequest.explorer.obj1", "minecraft:plains"),
						objBiome("dmz.sidequest.explorer.obj2", "minecraft:desert"),
						objBiome("dmz.sidequest.explorer.obj3", "minecraft:forest")
				},
				new JsonObject[]{ rewTPS(400) }));

		writeQuestFile(dir, "namek_explorer.json", sidequest(
				"namek_explorer", "dmz.sidequest.namek_explorer.name", "dmz.sidequest.namek_explorer.desc",
				"exploration", true, null, null,
				prereqs("AND", condSaga("saiyan_saga", 9)),
				new JsonObject[]{
						objBiome("dmz.sidequest.namek_explorer.obj1", "dragonminez:ajissa_plains"),
						objStructure("dmz.sidequest.namek_explorer.obj2", "dragonminez:village_ajissa")
				},
				new JsonObject[]{ rewTPS(800) }));

		writeQuestFile(dir, "sacred_lands.json", sidequest(
				"sacred_lands", "dmz.sidequest.sacred_lands.name", "dmz.sidequest.sacred_lands.desc",
				"exploration", false, null, null,
				prereqs("AND", condQuest("namek_explorer")),
				new JsonObject[]{
						objStructure("dmz.sidequest.sacred_lands.obj1", "dragonminez:village_sacred")
				},
				new JsonObject[]{ rewTPS(1200) }));

		// --- Frieza Saga: Exploration ---

		writeQuestFile(dir, "krillin_namek_scout.json", sidequest(
				"krillin_namek_scout", "dmz.sidequest.krillin_scout.name", "dmz.sidequest.krillin_scout.desc",
				"exploration", false, "krillin", "krillin",
				prereqs("AND", condSaga("frieza_saga", 2)),
				new JsonObject[]{
						objBiome("dmz.sidequest.krillin_scout.obj1", "dragonminez:ajissa_plains"),
						objStructure("dmz.sidequest.krillin_scout.obj2", "dragonminez:village_ajissa"),
						objKill("dmz.sidequest.krillin_scout.obj3", "dragonminez:saga_friezasoldier01", 5)
				},
				new JsonObject[]{ rewTPS(4000) }));

		writeQuestFile(dir, "namek_farewell.json", sidequest(
				"namek_farewell", "dmz.sidequest.namek_farewell.name", "dmz.sidequest.namek_farewell.desc",
				"exploration", false, "namek_elder", "namek_elder",
				prereqs("AND", condSaga("frieza_saga", 15)),
				new JsonObject[]{
						objBiome("dmz.sidequest.namek_farewell.obj1", "dragonminez:sacred_land"),
						objStructure("dmz.sidequest.namek_farewell.obj2", "dragonminez:village_sacred"),
						objTalkTo("dmz.sidequest.namek_farewell.obj3", "namek_elder")
				},
				new JsonObject[]{ rewTPS(6000) }));

		// --- Android Saga: Exploration ---

		writeQuestFile(dir, "trunks_warning.json", sidequest(
				"trunks_warning", "dmz.sidequest.trunks_warning.name", "dmz.sidequest.trunks_warning.desc",
				"exploration", false, "trunks", "trunks",
				prereqs("AND", condSaga("android_saga", 1)),
				new JsonObject[]{
						objStructure("dmz.sidequest.trunks_warning.obj1", "dragonminez:gero_lab"),
						objTalkTo("dmz.sidequest.trunks_warning.obj2", "trunks")
				},
				new JsonObject[]{ rewTPS(10000) }));

		writeQuestFile(dir, "piccolo_kami_fusion.json", sidequest(
				"piccolo_kami_fusion", "dmz.sidequest.piccolo_kami.name", "dmz.sidequest.piccolo_kami.desc",
				"exploration", false, "piccolo", "piccolo",
				prereqs("AND", condSaga("android_saga", 9)),
				new JsonObject[]{
						objStructure("dmz.sidequest.piccolo_kami.obj1", "dragonminez:kamilookout"),
						objTalkTo("dmz.sidequest.piccolo_kami.obj2", "piccolo")
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
						objKill("dmz.sidequest.monster_hunter.obj1", "minecraft:zombie", 20),
						objKill("dmz.sidequest.monster_hunter.obj2", "minecraft:skeleton", 20),
						objKill("dmz.sidequest.monster_hunter.obj3", "minecraft:creeper", 10)
				},
				new JsonObject[]{ rewTPS(600) }));

		writeQuestFile(dir, "nether_warrior.json", sidequest(
				"nether_warrior", "dmz.sidequest.nether_warrior.name", "dmz.sidequest.nether_warrior.desc",
				"combat", false, null, null,
				prereqs("AND", condQuest("monster_hunter"), condLevel(5)),
				new JsonObject[]{
						objBiome("dmz.sidequest.nether_warrior.obj1", "minecraft:nether_wastes"),
						objKill("dmz.sidequest.nether_warrior.obj2", "minecraft:blaze", 10),
						objKill("dmz.sidequest.nether_warrior.obj3", "minecraft:wither_skeleton", 10)
				},
				new JsonObject[]{ rewTPS(1500), rewItem("minecraft:diamond", 3) }));

		writeQuestFile(dir, "dragon_ball_hunter.json", sidequest(
				"dragon_ball_hunter", "dmz.sidequest.dball_hunter.name", "dmz.sidequest.dball_hunter.desc",
				"combat", false, null, null,
				prereqs("AND", condSaga("saiyan_saga", 4)),
				new JsonObject[]{
						objKill("dmz.sidequest.dball_hunter.obj1", "minecraft:pillager", 15),
						objStructure("dmz.sidequest.dball_hunter.obj2", "minecraft:pillager_outpost")
				},
				new JsonObject[]{ rewTPS(1000) }));

		// --- Saiyan Saga: Combat ---

		writeQuestFile(dir, "yamcha_desert_bandit.json", sidequest(
				"yamcha_desert_bandit", "dmz.sidequest.yamcha_bandit.name", "dmz.sidequest.yamcha_bandit.desc",
				"combat", false, "yamcha", "yamcha",
				prereqs("AND", condSaga("saiyan_saga", 2)),
				new JsonObject[]{
						objBiome("dmz.sidequest.yamcha_bandit.obj1", "minecraft:desert"),
						objKill("dmz.sidequest.yamcha_bandit.obj2", "dragonminez:bandit", 10),
						objTalkTo("dmz.sidequest.yamcha_bandit.obj3", "yamcha")
				},
				new JsonObject[]{ rewTPS(600) }));

		writeQuestFile(dir, "farmer_red_ribbon_threat.json", sidequest(
				"farmer_red_ribbon_threat", "dmz.sidequest.farmer_rr.name", "dmz.sidequest.farmer_rr.desc",
				"combat", false, "farmer_01", "farmer_01",
				prereqs("AND", condSaga("saiyan_saga", 1)),
				new JsonObject[]{
						objKill("dmz.sidequest.farmer_rr.obj1", "dragonminez:red_ribbon_soldier", 10),
						objKill("dmz.sidequest.farmer_rr.obj2", "dragonminez:robot1", 2),
						objTalkTo("dmz.sidequest.farmer_rr.obj3", "farmer_01")
				},
				new JsonObject[]{ rewTPS(600), rewItem("minecraft:golden_apple", 3) }));

		// --- Frieza Saga: Combat ---

		writeQuestFile(dir, "namek_elder_blessing.json", sidequest(
				"namek_elder_blessing", "dmz.sidequest.elder_blessing.name", "dmz.sidequest.elder_blessing.desc",
				"combat", false, "namek_elder", "namek_elder",
				prereqs("AND", condSaga("frieza_saga", 2)),
				new JsonObject[]{
						objKill("dmz.sidequest.elder_blessing.obj1", "dragonminez:saga_friezasoldier01", 10),
						objTalkTo("dmz.sidequest.elder_blessing.obj2", "namek_elder")
				},
				new JsonObject[]{ rewTPS(3000) }));

		writeQuestFile(dir, "ginyu_force_warm_up.json", sidequest(
				"ginyu_force_warm_up", "dmz.sidequest.ginyu_warmup.name", "dmz.sidequest.ginyu_warmup.desc",
				"combat", true, "goku", "goku",
				prereqs("AND", condSaga("frieza_saga", 6)),
				new JsonObject[]{
						objBiome("dmz.sidequest.ginyu_warmup.obj1", "dragonminez:ajissa_plains"),
						objKill("dmz.sidequest.ginyu_warmup.obj2", "dragonminez:saga_friezasoldier01", 8),
						objKill("dmz.sidequest.ginyu_warmup.obj3", "dragonminez:saga_friezasoldier02", 8)
				},
				new JsonObject[]{ rewTPS(7000) }));

		// --- Android Saga: Combat ---

		writeQuestFile(dir, "krillin_android_patrol.json", sidequest(
				"krillin_android_patrol", "dmz.sidequest.krillin_patrol.name", "dmz.sidequest.krillin_patrol.desc",
				"combat", false, "krillin", "krillin",
				prereqs("AND", condSaga("android_saga", 5)),
				new JsonObject[]{
						objKill("dmz.sidequest.krillin_patrol.obj1", "dragonminez:red_ribbon_soldier", 15),
						objKill("dmz.sidequest.krillin_patrol.obj2", "dragonminez:robot1", 3),
						objBiome("dmz.sidequest.krillin_patrol.obj3", "dragonminez:rocky")
				},
				new JsonObject[]{ rewTPS(12000) }));

		writeQuestFile(dir, "tien_cell_search.json", sidequest(
				"tien_cell_search", "dmz.sidequest.tien_cell.name", "dmz.sidequest.tien_cell.desc",
				"combat", true, "tien", "tien",
				prereqs("AND", condSaga("android_saga", 8)),
				new JsonObject[]{
						objBiome("dmz.sidequest.tien_cell.obj1", "minecraft:plains"),
						objKill("dmz.sidequest.tien_cell.obj2", "minecraft:zombie", 30),
						objKill("dmz.sidequest.tien_cell.obj3", "minecraft:spider", 15)
				},
				new JsonObject[]{ rewTPS(16000) }));

		writeQuestFile(dir, "videl_city_defense.json", sidequest(
				"videl_city_defense", "dmz.sidequest.videl_defense.name", "dmz.sidequest.videl_defense.desc",
				"combat", false, "videl", "videl",
				prereqs("AND", condSaga("android_saga", 17)),
				new JsonObject[]{
						objKill("dmz.sidequest.videl_defense.obj1", "dragonminez:saga_cell_jr", 10),
						objTalkTo("dmz.sidequest.videl_defense.obj2", "videl")
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
						objBiome("dmz.sidequest.chiaotzu_rescue.obj1", "minecraft:nether_wastes"),
						objItem("dmz.sidequest.chiaotzu_rescue.obj2", "minecraft:ender_pearl", 4),
						objTalkTo("dmz.sidequest.chiaotzu_rescue.obj3", "chiaotzu")
				},
				new JsonObject[]{ rewTPS(1000) }));

		writeQuestFile(dir, "scholar_saiyan_lore.json", sidequest(
				"scholar_saiyan_lore", "dmz.sidequest.scholar_lore.name", "dmz.sidequest.scholar_lore.desc",
				"story", false, "scholar_01", "scholar_01",
				prereqs("AND", condSaga("saiyan_saga", 7)),
				new JsonObject[]{
						objStructure("dmz.sidequest.scholar_lore.obj1", "dragonminez:goku_house"),
						objBiome("dmz.sidequest.scholar_lore.obj2", "dragonminez:rocky"),
						objTalkTo("dmz.sidequest.scholar_lore.obj3", "scholar_01")
				},
				new JsonObject[]{ rewTPS(600) }));

		// --- Frieza Saga: Story ---

		writeQuestFile(dir, "piccolo_arrival.json", sidequest(
				"piccolo_arrival", "dmz.sidequest.piccolo_arrival.name", "dmz.sidequest.piccolo_arrival.desc",
				"story", false, "piccolo", "piccolo",
				prereqs("AND", condSaga("frieza_saga", 8)),
				new JsonObject[]{
						objKill("dmz.sidequest.piccolo_arrival.obj1", "dragonminez:saga_friezasoldier01", 5),
						objKill("dmz.sidequest.piccolo_arrival.obj2", "dragonminez:saga_friezasoldier02", 5),
						objTalkTo("dmz.sidequest.piccolo_arrival.obj3", "piccolo")
				},
				new JsonObject[]{ rewTPS(5000) }));

		writeQuestFile(dir, "goku_healing_pod.json", sidequest(
				"goku_healing_pod", "dmz.sidequest.goku_healing.name", "dmz.sidequest.goku_healing.desc",
				"story", false, "goku", "goku",
				prereqs("AND", condSaga("frieza_saga", 11)),
				new JsonObject[]{
						objKill("dmz.sidequest.goku_healing.obj1", "dragonminez:saga_friezasoldier01", 20),
						objStructure("dmz.sidequest.goku_healing.obj2", "dragonminez:elder_guru"),
						objTalkTo("dmz.sidequest.goku_healing.obj3", "goku")
				},
				new JsonObject[]{ rewTPS(8000) }));

		// --- Android Saga: Story ---

		writeQuestFile(dir, "chi_chi_gohan_worry.json", sidequest(
				"chi_chi_gohan_worry", "dmz.sidequest.chichi_worry.name", "dmz.sidequest.chichi_worry.desc",
				"story", false, "chi_chi", "chi_chi",
				prereqs("AND", condSaga("android_saga", 16)),
				new JsonObject[]{
						objItem("dmz.sidequest.chichi_worry.obj1", "minecraft:iron_sword", 1),
						objItem("dmz.sidequest.chichi_worry.obj2", "minecraft:cooked_beef", 16),
						objTalkTo("dmz.sidequest.chichi_worry.obj3", "chi_chi")
				},
				new JsonObject[]{ rewTPS(5000), rewItem("minecraft:golden_apple", 5) }));

		writeQuestFile(dir, "goku_final_farewell.json", sidequest(
				"goku_final_farewell", "dmz.sidequest.goku_farewell.name", "dmz.sidequest.goku_farewell.desc",
				"story", false, "goku", "goku",
				prereqs("AND", condSaga("android_saga", 18)),
				new JsonObject[]{
						objStructure("dmz.sidequest.goku_farewell.obj1", "dragonminez:kamilookout"),
						objTalkTo("dmz.sidequest.goku_farewell.obj2", "goku")
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
						objItem("dmz.sidequest.bulma_radar.obj1", "minecraft:redstone", 16),
						objItem("dmz.sidequest.bulma_radar.obj2", "minecraft:copper_ingot", 16),
						objItem("dmz.sidequest.bulma_radar.obj3", "minecraft:gold_ingot", 8),
						objTalkTo("dmz.sidequest.bulma_radar.obj4", "bulma")
				},
				new JsonObject[]{ rewTPS(800), rewItem("dragonminez:dball_radar", 1) }));

		writeQuestFile(dir, "chi_chi_provisions.json", sidequest(
				"chi_chi_provisions", "dmz.sidequest.chichi_provisions.name", "dmz.sidequest.chichi_provisions.desc",
				"collection", true, "chi_chi", "chi_chi",
				prereqs("AND", condSaga("saiyan_saga", 6)),
				new JsonObject[]{
						objItem("dmz.sidequest.chichi_provisions.obj1", "minecraft:cooked_beef", 32),
						objItem("dmz.sidequest.chichi_provisions.obj2", "minecraft:bread", 32),
						objItem("dmz.sidequest.chichi_provisions.obj3", "minecraft:golden_carrot", 8)
				},
				new JsonObject[]{ rewTPS(400), rewItem("minecraft:golden_apple", 5) }));

		// --- Frieza Saga: Collection ---

		writeQuestFile(dir, "bulma_namek_research.json", sidequest(
				"bulma_namek_research", "dmz.sidequest.bulma_namek.name", "dmz.sidequest.bulma_namek.desc",
				"collection", false, "bulma", "bulma",
				prereqs("AND", condSaga("frieza_saga", 2)),
				new JsonObject[]{
						objItem("dmz.sidequest.bulma_namek.obj1", "dragonminez:kikono_shard", 16),
						objItem("dmz.sidequest.bulma_namek.obj2", "dragonminez:kikono_cloth", 8),
						objTalkTo("dmz.sidequest.bulma_namek.obj3", "bulma")
				},
				new JsonObject[]{ rewTPS(3500) }));

		writeQuestFile(dir, "merchant_alien_artifacts.json", sidequest(
				"merchant_alien_artifacts", "dmz.sidequest.merchant_artifacts.name", "dmz.sidequest.merchant_artifacts.desc",
				"collection", false, "merchant_01", "merchant_01",
				prereqs("AND", condSaga("frieza_saga", 4)),
				new JsonObject[]{
						objItem("dmz.sidequest.merchant_artifacts.obj1", "dragonminez:kikono_shard", 16),
						objItem("dmz.sidequest.merchant_artifacts.obj2", "minecraft:diamond", 8),
						objTalkTo("dmz.sidequest.merchant_artifacts.obj3", "merchant_01")
				},
				new JsonObject[]{ rewTPS(4500), rewItem("minecraft:diamond_sword", 1) }));

		writeQuestFile(dir, "gohan_dragon_balls_namek.json", sidequest(
				"gohan_dragon_balls_namek", "dmz.sidequest.gohan_namek_db.name", "dmz.sidequest.gohan_namek_db.desc",
				"collection", false, "gohan", "gohan",
				prereqs("AND", condSaga("frieza_saga", 3)),
				new JsonObject[]{
						objStructure("dmz.sidequest.gohan_namek_db.obj1", "dragonminez:elder_guru"),
						objItem("dmz.sidequest.gohan_namek_db.obj2", "dragonminez:kikono_shard", 32),
						objTalkTo("dmz.sidequest.gohan_namek_db.obj3", "gohan")
				},
				new JsonObject[]{ rewTPS(6000) }));

		// --- Android Saga: Collection ---

		writeQuestFile(dir, "bulma_gero_blueprints.json", sidequest(
				"bulma_gero_blueprints", "dmz.sidequest.bulma_gero.name", "dmz.sidequest.bulma_gero.desc",
				"collection", false, "bulma", "bulma",
				prereqs("AND", condSaga("android_saga", 6)),
				new JsonObject[]{
						objStructure("dmz.sidequest.bulma_gero.obj1", "dragonminez:gero_lab"),
						objItem("dmz.sidequest.bulma_gero.obj2", "minecraft:redstone", 32),
						objItem("dmz.sidequest.bulma_gero.obj3", "dragonminez:kikono_shard", 8),
						objTalkTo("dmz.sidequest.bulma_gero.obj4", "bulma")
				},
				new JsonObject[]{ rewTPS(14000) }));
	}
}

