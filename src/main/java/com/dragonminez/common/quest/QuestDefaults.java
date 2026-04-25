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
 * Generates default individual quest JSON files in the unified schema.
 * <p>
 * Files use the same schema for all quest types â€” only the {@code "type"} field differs.
 * Filenames use a numeric prefix for ordering (e.g. {@code 01_find_roshi.json}).
 * <p>
 * <b>Unified quest schema:</b>
 * <pre>
 * {
 *   "id": 1,
 *   "title": "translation.key",
 *   "description": "translation.key",
 *   "type": "SAGA",
 *   "category": "saga_saiyan",
 *   "parallel_objectives": false,
 *   "quest_giver": null,
 *   "turn_in": null,
 *   "prerequisites": { ... },
 *   "requirements": { ... },
 *   "objectives": [ ... ],
 *   "rewards": [ ... ]
 * }
 * </pre>
 *
 * @since 2.1
 */
final class QuestDefaults {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String PARTY_SCALING_KEY = "party_scaling";

	private QuestDefaults() {
	} // utility class

	static void createDefaultQuestFiles(Path questsDir) {
		if (!ConfigManager.getServerConfig().getGameplay().getStoryModeEnabled()) return;
		if (!ConfigManager.getServerConfig().getGameplay().getCreateDefaultSagas()) return;

		createSaiyanSagaQuests(questsDir);
		createFriezaSagaQuests(questsDir);
		createAndroidSagaQuests(questsDir);
		createBuuSagaQuests(questsDir);
		// createMoviesSagaQuests(questsDir);
	}

	// ---- Helpers ----

	private static void writeQuest(Path dir, String filename, JsonObject quest) {
		Path file = dir.resolve(filename);
		try {
			Files.createDirectories(dir);
			if (Files.exists(file)) {
				return;
			}
			try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				GSON.toJson(quest, w);
			}
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Failed to create default quest file: {}", filename, e);
		}
	}

	/**
	 * Builds a saga quest in the schema.
	 */
	private static JsonObject sagaQuest(int id, String title, String desc, String category,
										String unusedChainSaga, int unusedChainOrder, String unusedChainNext,
										JsonObject prerequisites,
										JsonObject startRequirements,
										JsonObject[] objectives, JsonObject[] rewards) {
		JsonObject q = new JsonObject();
		q.addProperty("id", id);
		q.addProperty("title", title);
		q.addProperty("description", desc);
		q.addProperty("type", "SAGA");
		q.addProperty("category", category);
		q.addProperty("parallel_objectives", false);
		q.addProperty(PARTY_SCALING_KEY, true);
		q.addProperty("secret", false);
		q.addProperty("claim_mode", "TREE_OR_NPC");
		q.add("quest_giver", JsonNull.INSTANCE);
		q.add("turn_in", JsonNull.INSTANCE);

		if (prerequisites != null) q.add("prerequisites", prerequisites);
		if (startRequirements != null) q.add("requirements", startRequirements);

		JsonArray objArr = new JsonArray();
		for (JsonObject o : objectives) objArr.add(o);
		q.add("objectives", objArr);
		JsonArray rewArr = new JsonArray();
		for (JsonObject r : rewards) rewArr.add(r);
		q.add("rewards", rewArr);
		return q;
	}

	// ---- Objective helpers ----

	private static JsonObject objStructure(String structureId) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "STRUCTURE");
		o.addProperty("structure", structureId);
		return o;
	}

	private static JsonObject objBiome(String biomeId) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "BIOME");
		o.addProperty("biome", biomeId);
		return o;
	}

	private static JsonObject objDimension(String dimensionId) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "DIMENSION");
		o.addProperty("dimension", dimensionId);
		return o;
	}

	private static JsonObject objKill(String entity, int count, double hp, double melee, double ki) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "KILL");
		o.addProperty("entity", entity);
		o.addProperty("count", count);
		o.addProperty("health", hp);
		o.addProperty("meleeDamage", melee);
		o.addProperty("kiDamage", ki);
		o.addProperty("spawn", "QUEST");
		o.addProperty("count_mode", "QUEST_SPAWNED_ONLY");
		return o;
	}

	private static JsonObject objItem(String itemId, int count) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "ITEM");
		o.addProperty("item", itemId);
		o.addProperty("count", count);
		return o;
	}

	private static JsonObject objTalkTo(String targetNpcId) { //Maybe change for entity (from targetNpcId) later?
		JsonObject o = new JsonObject();
		o.addProperty("type", "TALK_TO");
		o.addProperty("npcId", targetNpcId);
		return o;
	}

	// ---- Reward helpers ----

	private static JsonObject rewTPS(int amount) {
		JsonObject r = new JsonObject();
		r.addProperty("type", "TPS");
		r.addProperty("amount", amount);
		return r;
	}

	private static JsonObject rewItem(String itemId, int count) {
		JsonObject r = new JsonObject();
		r.addProperty("type", "ITEM");
		r.addProperty("item", itemId);
		r.addProperty("count", count);
		return r;
	}

	// ---- Prerequisite helpers ----

	private static JsonObject prereqs(String op, JsonObject... conditions) {
		JsonObject p = new JsonObject();
		p.addProperty("operator", op);
		JsonArray arr = new JsonArray();
		for (JsonObject c : conditions) arr.add(c);
		p.add("conditions", arr);
		return p;
	}

	private static JsonObject requirements(String op, JsonObject... conditions) {
		return prereqs(op, conditions);
	}

	private static JsonObject condSaga(String sagaId, int questId) {
		JsonObject c = new JsonObject();
		c.addProperty("type", "SAGA_QUEST");
		c.addProperty("sagaId", sagaId);
		c.addProperty("questId", questId);
		return c;
	}

	private static JsonObject condQuest(String questId) {
		JsonObject c = new JsonObject();
		c.addProperty("type", "QUEST");
		c.addProperty("questId", questId);
		return c;
	}

	private static JsonObject condLevel(int minLevel) {
		JsonObject c = new JsonObject();
		c.addProperty("type", "LEVEL");
		c.addProperty("minLevel", minLevel);
		return c;
	}

	private static JsonObject condStat(String stat, int minValue) {
		JsonObject c = new JsonObject();
		c.addProperty("type", "STAT");
		c.addProperty("stat", stat);
		c.addProperty("minValue", minValue);
		return c;
	}

	private static JsonObject condBiome(String biomeId) {
		JsonObject c = new JsonObject();
		c.addProperty("type", "BIOME");
		c.addProperty("biome", biomeId);
		return c;
	}

	private static JsonObject condStructure(String structureId) {
		JsonObject c = new JsonObject();
		c.addProperty("type", "STRUCTURE");
		c.addProperty("structure", structureId);
		return c;
	}

	private static JsonObject condDimension(String dimensionId) {
		JsonObject c = new JsonObject();
		c.addProperty("type", "DIMENSION");
		c.addProperty("dimension", dimensionId);
		return c;
	}

	private static JsonObject condGameTimeMinutes(long minutes) {
		JsonObject c = new JsonObject();
		c.addProperty("type", "TIME");
		c.addProperty("mode", "GAME_TIME");
		c.addProperty("ticks", minutes * 20L * 60L);
		return c;
	}

	private static JsonObject condRealTimeMinutes(long minutes) {
		JsonObject c = new JsonObject();
		c.addProperty("type", "TIME");
		c.addProperty("mode", "REAL_TIME");
		c.addProperty("milliseconds", minutes * 60L * 1000L);
		return c;
	}

	private static JsonObject dimensionReq(String dimensionId, int minLevel, JsonObject... extraConditions) {
		JsonObject[] conditions = new JsonObject[extraConditions.length + 2];
		conditions[0] = condLevel(minLevel);
		conditions[1] = condDimension(dimensionId);
		System.arraycopy(extraConditions, 0, conditions, 2, extraConditions.length);
		return requirements("AND", conditions);
	}

	private static JsonObject earthReq(int minLevel, JsonObject... extraConditions) {
		return dimensionReq("minecraft:overworld", minLevel, extraConditions);
	}

	private static JsonObject namekReq(int minLevel, JsonObject... extraConditions) {
		return dimensionReq("dragonminez:namek", minLevel, extraConditions);
	}

	private record QuestStep(int id, String filename, String title, String desc, JsonObject startRequirements,
							 JsonObject[] objectives, JsonObject[] rewards) {
	}

	private static QuestStep step(String sagaKey, int id, String filename, JsonObject startRequirements,
								  JsonObject[] objectives, JsonObject... rewards) {
		return new QuestStep(
				id,
				filename,
				"dmz.quest." + sagaKey + id + ".name",
				"dmz.quest." + sagaKey + id + ".desc",
				startRequirements,
				objectives,
				rewards
		);
	}

	private static JsonObject prevQuest(String sagaId, int questId) {
		return prereqs("AND", condSaga(sagaId, questId));
	}

	private static void writeLinearSaga(Path dir, String sagaId, String category, JsonObject firstPrereq, QuestStep... steps) {
		for (int i = 0; i < steps.length; i++) {
			QuestStep step = steps[i];
			JsonObject prereq = i == 0 ? firstPrereq : prevQuest(sagaId, steps[i - 1].id());
			writeQuest(
					dir,
					step.filename(),
					sagaQuest(step.id(), step.title(), step.desc(), category, sagaId, step.id(), null,
							prereq, step.startRequirements(), step.objectives(), step.rewards())
			);
		}
	}

	// ========================================================================================
	// Saiyan Saga Quests (folder: saga_saiyan)
	// ========================================================================================

	private static void createSaiyanSagaQuests(Path questsDir) {
		writeLinearSaga(questsDir.resolve("saga_saiyan"), "saiyan_saga", "saga_saiyan", null,
				step("saiyan", 1, "01_find_roshi.json",
						requirements("AND", condLevel(1)),
						new JsonObject[]{
								objStructure("dragonminez:roshi_house")
						},
						rewTPS(500), rewItem("minecraft:bread", 3)),
				step("saiyan", 2, "02_train_with_krillin.json",
						requirements("AND", condStructure("dragonminez:roshi_house"), condLevel(3)),
						new JsonObject[]{
								objKill("dragonminez:saga_krillin",1, 200, 10, 0)
						},
						rewTPS(1000), rewItem("minecraft:iron_sword", 1)),
				step("saiyan", 3, "03_defend_gohan.json",
						requirements("AND", condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_raditz", 1, 500, 32, 65)
						},
						rewTPS(500), rewItem("dragonminez:broken_scouter", 1)),
				step("saiyan", 4, "04_train_with_gohan.json",
						requirements("AND", condBiome("minecraft:plains"), condLevel(10)),
						new JsonObject[]{
								objKill("dragonminez:dino1", 1, 1000, 10, 0)
						},
						rewTPS(800), rewItem("dragonminez:cooked_dino_meat", 8)),
				step("saiyan", 5, "05_craft_dragon_radar.json",
						requirements("AND", condGameTimeMinutes(44), condDimension("minecraft:overworld")),
						new JsonObject[]{
								objItem("dragonminez:dball_radar", 1),
								objTalkTo("bulma")
						},
						rewTPS(1500)),
				step("saiyan", 6, "06_kill_the_saibamans.json",
						requirements("AND", condLevel(15), condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_saibaman1", 6, 400, 25, 50)
						},
						rewTPS(2500)),
				step("saiyan", 7, "07_hold_against_nappa.json",
						requirements("AND", condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_nappa", 1, 750, 45, 100)
						}, rewTPS(3000)),
				step("saiyan", 8, "08_face_vegeta.json",
						requirements("AND", condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_vegeta", 1, 1200, 70, 150)
						},
						rewTPS(3500)),
				step("saiyan", 9, "09_kill_oozaru_vegeta.json",
						requirements("AND", condBiome("dragonminez:rocky")),
						new JsonObject[]{objKill("dragonminez:saga_ozaruvegeta", 1, 2500, 140, 200)
						},
						rewTPS(4000)),
				step("saiyan", 10, "10_feast_with_others.json",
						requirements("AND", condStructure("dragonminez:roshi_house")),
						new JsonObject[]{
								objStructure("dragonminez:roshi_house")
						},
						rewTPS(500), rewItem("dragonminez:senzu_bean", 6)),
				step("saiyan", 11, "11_fix_the_saiyan_ship.json",
						requirements("AND", condRealTimeMinutes(5)),
						new JsonObject[]{
								objTalkTo("bulma")
						},
						rewItem("dragonminez:saiyan_ship", 1)),
				step("saiyan", 12, "12_head_to_namek.json",
						requirements("AND", condLevel(100)),
						new JsonObject[]{
								objDimension("dragonminez:namek")
						},
						rewTPS(1000))
		);
	}

	// ========================================================================================
	// Frieza Saga Quests (folder: saga_frieza)
	// ========================================================================================

	private static void createFriezaSagaQuests(Path questsDir) {
		JsonObject prevSaiyan = prevQuest("saiyan_saga", 12);
		writeLinearSaga(questsDir.resolve("saga_frieza"), "frieza_saga", "saga_frieza", prevSaiyan,
				step("frieza", 1, "01_secure_namek_landing.json",
						namekReq(100, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objBiome("dragonminez:ajissa_plains"),
								objKill("dragonminez:saga_friezasoldier01", 8, 700, 45, 90)
						},
						rewTPS(6000)),
				step("frieza", 2, "02_defeat_cui.json",
						namekReq(112, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objBiome("dragonminez:ajissa_plains"),
								objKill("dragonminez:saga_cui", 1, 1200, 70, 150)
						},
						rewTPS(6800)),
				step("frieza", 3, "03_find_namekian_village.json",
						namekReq(120, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objStructure("dragonminez:village_ajissa")
						},
						rewTPS(7200)),
				step("frieza", 4, "04_defend_the_namekians.json",
						namekReq(128, condStructure("dragonminez:village_ajissa")),
						new JsonObject[]{
								objKill("dragonminez:saga_friezasoldier02", 10, 850, 55, 110),
								objKill("dragonminez:saga_friezasoldier03", 6, 950, 62, 120)
						},
						rewTPS(7800)),
				step("frieza", 6, "06_defeat_dodoria.json",
						namekReq(138, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objBiome("dragonminez:ajissa_plains"),
								objKill("dragonminez:saga_dodoria", 1, 1450, 85, 185)
						},
						rewTPS(8600)),
				step("frieza", 7, "07_defeat_zarbon.json",
						namekReq(148, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objBiome("dragonminez:ajissa_plains"),
								objKill("dragonminez:saga_zarbont1", 1, 1700, 95, 220)
						},
						rewTPS(9400)),
				step("frieza", 9, "09_the_saiyan_prince.json",
						namekReq(160, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objBiome("dragonminez:ajissa_plains"),
								objKill("dragonminez:saga_vegeta_namek", 1, 1800, 100, 230)
						},
						rewTPS(10200)),
				step("frieza", 10, "10_defeat_guldo.json",
						namekReq(172, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objBiome("dragonminez:ajissa_plains"),
								objKill("dragonminez:saga_guldo", 1, 900, 55, 100)
						},
						rewTPS(10800)),
				step("frieza", 11, "11_defeat_recoome.json",
						namekReq(184, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objBiome("dragonminez:ajissa_plains"),
								objKill("dragonminez:saga_recoome", 1, 2200, 120, 200)
						},
						rewTPS(11600)),
				step("frieza", 12, "12_defeat_burter_and_jeice.json",
						namekReq(196, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objBiome("dragonminez:ajissa_plains"),
								objKill("dragonminez:saga_burter", 1, 2200, 120, 200),
								objKill("dragonminez:saga_jeice", 1, 2200, 120, 200)
						},
						rewTPS(12600)),
				step("frieza", 14, "14_defeat_ginyu.json",
						namekReq(208, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objBiome("dragonminez:ajissa_plains"),
								objKill("dragonminez:saga_ginyu", 1, 3200, 170, 280)
						},
						rewTPS(13600)),
				step("frieza", 15, "15_defeat_ginyu_goku.json",
						namekReq(220, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objBiome("dragonminez:ajissa_plains"),
								objKill("dragonminez:saga_ginyu_goku", 1, 1800, 95, 155)
						},
						rewTPS(14400)),
				step("frieza", 16, "16_repel_frieza_forces.json",
						namekReq(235, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objBiome("dragonminez:ajissa_plains"),
								objKill("dragonminez:saga_friezasoldier03", 14, 1200, 75, 145)
						},
						rewTPS(15400)),
				step("frieza", 17, "17_defeat_frieza_first.json",
						namekReq(255, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objBiome("dragonminez:ajissa_plains"),
								objKill("dragonminez:saga_frieza_first", 1, 4200, 210, 360)
						},
						rewTPS(16600)),
				step("frieza", 18, "18_defeat_frieza_second_third.json",
						namekReq(280, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objBiome("dragonminez:ajissa_plains"),
								objKill("dragonminez:saga_frieza_second", 1, 8500, 420, 690)
						},
						rewTPS(17800)),
				step("frieza", 20, "20_defeat_frieza_base.json",
						namekReq(315, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objBiome("dragonminez:ajissa_plains"),
								objKill("dragonminez:saga_frieza_base", 1, 14000, 650, 1050)
						},
						rewTPS(19400)),
				step("frieza", 21, "21_defeat_frieza_full_power.json",
						namekReq(350, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objBiome("dragonminez:ajissa_plains"),
								objKill("dragonminez:saga_frieza_fp", 1, 17000, 800, 1260)
						},
						rewTPS(21000)),
				step("frieza", 22, "22_escape_namek_before_collapse.json",
						namekReq(360, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objBiome("dragonminez:ajissa_plains"),
								objDimension("minecraft:overworld")
						},
						rewTPS(12000))
		);
	}

	// ========================================================================================
	// Android Saga Quests (folder: saga_android)
	// ========================================================================================

	private static void createAndroidSagaQuests(Path questsDir) {
		JsonObject prevFrieza = prevQuest("frieza_saga", 22);

		writeLinearSaga(questsDir.resolve("saga_android"), "android_saga", "saga_android", prevFrieza,
				step("android", 1, "01_defeat_soldiers.json",
						earthReq(400, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objBiome("dragonminez:rocky"),
								objKill("dragonminez:saga_friezasoldier01", 15, 260, 18, 24)
						},
						rewTPS(18000)),
				step("android", 2, "02_defeat_mecha_frieza.json",
						earthReq(440, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objBiome("dragonminez:rocky"),
								objKill("dragonminez:saga_mecha_frieza", 1, 17000, 800, 1300)
						},
						rewTPS(19000)),
				step("android", 3, "03_defeat_king_cold.json",
						earthReq(470, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objBiome("dragonminez:rocky"),
								objKill("dragonminez:saga_king_cold", 1, 9000, 430, 700)
						},
						rewTPS(19800)),
				step("android", 4, "04_test_future_trunks.json",
						earthReq(500, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objBiome("dragonminez:rocky"),
								objKill("dragonminez:saga_ftrunks_ssj", 1, 21000, 980, 1550)
						},
						rewTPS(20600)),
				step("android", 5, "05_find_goku_house.json",
						earthReq(520, condStructure("dragonminez:goku_house")),
						new JsonObject[]{
								objStructure("dragonminez:goku_house")
						},
						rewTPS(9000)),
				step("android", 6, "06_three_year_training.json",
						earthReq(560, condRealTimeMinutes(10)),
						new JsonObject[]{
								objBiome("minecraft:plains"),
								objKill("dragonminez:shadow_dummy", 16, 1800, 110, 190)
						},
						rewTPS(21000)),
				step("android", 7, "07_defeat_a19.json",
						earthReq(620, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objBiome("dragonminez:rocky"),
								objKill("dragonminez:saga_a19", 1, 22000, 1000, 1600)
						},
						rewTPS(21800)),
				step("android", 8, "08_defeat_drgero.json",
						earthReq(660, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objBiome("dragonminez:rocky"),
								objKill("dragonminez:saga_drgero", 1, 18500, 880, 1400)
						},
						rewTPS(22600)),
				step("android", 9, "09_track_android_signal.json",
						earthReq(700, condBiome("#minecraft:is_mountain")),
						new JsonObject[]{
								objBiome("#minecraft:is_mountain")
						},
						rewTPS(9500)),
				step("android", 10, "10_defeat_a17.json",
						earthReq(740, condBiome("#minecraft:is_mountain")),
						new JsonObject[]{
								objBiome("#minecraft:is_mountain"),
								objKill("dragonminez:saga_a17", 1, 30000, 1400, 2200)
						},
						rewTPS(23600)),
				step("android", 11, "11_defeat_a18.json",
						earthReq(780, condBiome("#minecraft:is_mountain")),
						new JsonObject[]{
								objBiome("#minecraft:is_mountain"),
								objKill("dragonminez:saga_a18", 1, 30000, 1400, 2200)
						},
						rewTPS(24400)),
				step("android", 12, "12_defeat_a16.json",
						earthReq(820, condBiome("minecraft:plains")),
						new JsonObject[]{
								objBiome("minecraft:plains"),
								objKill("dragonminez:saga_a16", 1, 32000, 1500, 2400)
						},
						rewTPS(25200)),
				step("android", 13, "13_investigate_new_threat.json",
						earthReq(860, condBiome("minecraft:plains")),
						new JsonObject[]{
								objBiome("minecraft:plains"),
								objKill("dragonminez:saga_cell_imperfect", 1, 28000, 1300, 2000)
						},
						rewTPS(26000)),
				step("android", 14, "14_defeat_piccolo_kami.json",
						earthReq(900, condBiome("minecraft:plains")),
						new JsonObject[]{
								objBiome("minecraft:plains"),
								objKill("dragonminez:saga_piccolo_kami", 1, 35000, 1600, 2600)
						},
						rewTPS(26800)),
				step("android", 15, "15_find_timechamber.json",
						earthReq(940, condStructure("dragonminez:kamilookout")),
						new JsonObject[]{
								objStructure("dragonminez:timechamber")
						},
						rewTPS(10000)),
				step("android", 16, "16_defend_city_from_cell.json",
						earthReq(980, condBiome("minecraft:plains")),
						new JsonObject[]{
								objBiome("minecraft:plains"),
								objKill("dragonminez:saga_cell_imperfect", 2, 31000, 1450, 2300)
						},
						rewTPS(27600)),
				step("android", 17, "17_stop_cell_absorption.json",
						earthReq(1020, condBiome("minecraft:plains")),
						new JsonObject[]{
								objBiome("minecraft:plains"),
								objKill("dragonminez:saga_cell_imperfect", 1, 36000, 1650, 2600),
								objKill("dragonminez:saga_a18", 1, 32000, 1500, 2350)
						},
						rewTPS(28400)),
				step("android", 18, "18_defeat_cell_semiperfect.json",
						earthReq(1060, condBiome("minecraft:plains")),
						new JsonObject[]{
								objBiome("minecraft:plains"),
								objKill("dragonminez:saga_cell_semiperfect", 1, 42000, 1900, 3100)
						},
						rewTPS(29200)),
				step("android", 19, "19_clash_super_vegeta.json",
						earthReq(1100, condBiome("minecraft:plains")),
						new JsonObject[]{
								objBiome("minecraft:plains"),
								objKill("dragonminez:saga_vegeta_mid_ssg2", 1, 45000, 2100, 3400)
						},
						rewTPS(30000)),
				step("android", 20, "20_clash_trunks_ssj.json",
						earthReq(1140, condBiome("minecraft:plains")),
						new JsonObject[]{
								objBiome("minecraft:plains"),
								objKill("dragonminez:saga_ftrunks_ssj", 1, 43000, 2000, 3250)
						},
						rewTPS(30800)),
				step("android", 21, "21_defeat_cell_perfect.json",
						earthReq(1180, condBiome("minecraft:plains")),
						new JsonObject[]{
								objBiome("minecraft:plains"),
								objKill("dragonminez:saga_cell_perfect", 1, 62000, 2900, 4700)
						},
						rewTPS(31600)),
				step("android", 22, "22_prepare_cell_games.json",
						earthReq(1220, condStructure("dragonminez:kamilookout"), condRealTimeMinutes(6)),
						new JsonObject[]{
								objStructure("dragonminez:kamilookout")
						},
						rewTPS(11000)),
				step("android", 23, "23_help_gohan_ssj.json",
						earthReq(1260, condBiome("minecraft:plains")),
						new JsonObject[]{
								objBiome("minecraft:plains"),
								objKill("dragonminez:saga_gohan_mid_ssj", 1, 56000, 2550, 4100)
						},
						rewTPS(32400)),
				step("android", 24, "24_defeat_cell_jrs_wave1.json",
						earthReq(1300, condBiome("minecraft:plains")),
						new JsonObject[]{
								objBiome("minecraft:plains"),
								objKill("dragonminez:saga_cell_jr", 6, 43000, 1980, 3850)
						},
						rewTPS(33200)),
				step("android", 25, "25_defeat_cell_jrs_wave2.json",
						earthReq(1340, condBiome("minecraft:plains")),
						new JsonObject[]{
								objBiome("minecraft:plains"),
								objKill("dragonminez:saga_cell_jr", 8, 45000, 2050, 3950)
						},
						rewTPS(34000)),
				step("android", 26, "26_defeat_cell_superperfect.json",
						earthReq(1380, condBiome("minecraft:plains")),
						new JsonObject[]{
								objBiome("minecraft:plains"),
								objKill("dragonminez:saga_cell_superperfect", 1, 82000, 3900, 6200)
						},
						rewTPS(35200)),
				step("android", 27, "27_secure_earth_after_cell.json",
						earthReq(1420, condBiome("minecraft:plains")),
						new JsonObject[]{
								objBiome("minecraft:plains"),
								objKill("dragonminez:saga_cell_jr", 10, 46000, 2150, 4100)
						},
						rewTPS(36000)),
				step("android", 28, "28_post_cell_training.json",
						earthReq(1460, condRealTimeMinutes(5)),
						new JsonObject[]{
								objBiome("minecraft:plains"),
								objKill("dragonminez:shadow_dummy", 20, 2200, 140, 230)
						},
						rewTPS(36600)),
				step("android", 29, "29_return_to_kami_lookout.json",
						earthReq(1480, condStructure("dragonminez:kamilookout")),
						new JsonObject[]{
								objStructure("dragonminez:kamilookout")
						},
						rewTPS(11500)),
				step("android", 30, "30_world_peace_patrol.json",
						earthReq(1500, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objBiome("dragonminez:rocky"),
								objKill("dragonminez:saga_friezasoldier01", 18, 1600, 100, 180)
						},
						rewTPS(37200)),
				step("android", 31, "31_hidden_cell_return.json",
						earthReq(1540, condBiome("minecraft:plains")),
						new JsonObject[]{
								objBiome("minecraft:plains"),
								objKill("dragonminez:saga_cell_perfect", 1, 68000, 3200, 5000)
						},
						rewTPS(37800)),
				step("android", 32, "32_finish_cell_remnant.json",
						earthReq(1580, condBiome("minecraft:plains")),
						new JsonObject[]{
								objBiome("minecraft:plains"),
								objKill("dragonminez:saga_cell_superperfect", 1, 86000, 4100, 6500)
						},
						rewTPS(38400)),
				step("android", 33, "33_pass_torch_to_next_generation.json",
						earthReq(1600, condStructure("dragonminez:goku_house")),
						new JsonObject[]{
								objStructure("dragonminez:goku_house")
						},
						rewTPS(12000)),
				step("android", 34, "34_ready_for_buu_era.json",
						earthReq(1620, condStructure("dragonminez:kamilookout")),
						new JsonObject[]{
								objStructure("dragonminez:kamilookout")
						},
						rewTPS(12500))
		);
	}

	// ========================================================================================
	// Buu Saga Quests (folder: saga_buu)
	// ========================================================================================

	private static void createBuuSagaQuests(Path questsDir) {
		Path dir = questsDir.resolve("saga_buu");
		String s = "buu_saga", c = "saga_buu";
		JsonObject prevAndroid = prevQuest("android_saga", 34);

//		writeLinearSaga(dir, s, c, prevAndroid,
//
//		);
	}

	private static void createMoviesSagaQuests(Path questsDir) {
		// Planned for a future update once the core sagas are locked in.
		// Path dir = questsDir.resolve("saga_movies");
		// String s = "movies_saga", c = "saga_movies";
		// JsonObject prevBuu = prevQuest("buu_saga", 40);
		//
		// writeLinearSaga(dir, s, c, prevBuu,
		// 		step("movies", 1, "01_dead_zone_returns.json", earthReq(1650, condBiome("dragonminez:rocky")), new JsonObject[]{}, rewTPS(14000)),
		// 		step("movies", 2, "02_worlds_strongest.json", earthReq(1700, condBiome("minecraft:snowy_plains")), new JsonObject[]{}, rewTPS(14800)),
		// 		step("movies", 3, "03_tree_of_might.json", earthReq(1750, condBiome("minecraft:plains")), new JsonObject[]{}, rewTPS(15600))
		// );
	}
}

