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
		JsonObject[] withAlignment = new JsonObject[conditions.length + 1];
		System.arraycopy(conditions, 0, withAlignment, 0, conditions.length);
		withAlignment[conditions.length] = condAlignmentMin(41);
		return prereqs(op, withAlignment);
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

	private static JsonObject condAlignmentMin(int minAlignment) {
		JsonObject c = new JsonObject();
		c.addProperty("type", "ALIGNMENT");
		c.addProperty("min", minAlignment);
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
					sagaQuest(step.id(), step.title(), step.desc(), category,
							prereq, step.startRequirements(), step.objectives(), step.rewards())
			);
		}
	}

	// ========================================================================================
	// Saiyan Saga Quests (folder: saga_saiyan)
	// ========================================================================================

	private static void createSaiyanSagaQuests(Path questsDir) {
		writeLinearSaga(questsDir.resolve("saga_saiyan"), "saiyan_saga", "saga_saiyan", null,
				step("saiyan", 1, "01_defeat_raditz.json",
						earthReq(1, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_raditz", 1, 500, 32, 65)
						},
						rewTPS(1200), rewItem("dragonminez:broken_scouter", 1)),
				step("saiyan", 2, "02_survive_wilderness_training.json",
						earthReq(8, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:dino1", 1, 1000, 10, 0)
						},
						rewTPS(1800), rewItem("dragonminez:cooked_dino_meat", 8)),
				step("saiyan", 3, "03_kill_the_saibamans.json",
						earthReq(15, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_saibaman1", 6, 400, 25, 50)
						},
						rewTPS(2500)),
				step("saiyan", 4, "04_hold_against_nappa.json",
						earthReq(22, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_nappa", 1, 750, 45, 100)
						}, rewTPS(3000)),
				step("saiyan", 5, "05_face_vegeta.json",
						earthReq(30, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_vegeta", 1, 1200, 70, 150)
						},
						rewTPS(3500)),
				step("saiyan", 6, "06_defeat_oozaru_vegeta.json",
						earthReq(40, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_ozaruvegeta", 1, 2500, 140, 200)
						},
						rewTPS(4000)),
				step("saiyan", 7, "07_prepare_for_namek.json",
						earthReq(70, condRealTimeMinutes(5)),
						new JsonObject[]{
								objTalkTo("bulma")
						},
						rewTPS(1000), rewItem("dragonminez:saiyan_ship", 1)),
				step("saiyan", 8, "08_head_to_namek.json",
						earthReq(100),
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
		JsonObject prevSaiyan = prevQuest("saiyan_saga", 8);
		writeLinearSaga(questsDir.resolve("saga_frieza"), "frieza_saga", "saga_frieza", prevSaiyan,
				step("frieza", 1, "01_secure_namek_landing.json",
						namekReq(100, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_friezasoldier01", 8, 700, 45, 90)
						},
						rewTPS(6000)),
				step("frieza", 2, "02_defeat_cui.json",
						namekReq(112, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cui", 1, 1200, 70, 150)
						},
						rewTPS(6800)),
				step("frieza", 3, "03_defend_the_namekians.json",
						namekReq(124, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_friezasoldier02", 10, 850, 55, 110),
								objKill("dragonminez:saga_friezasoldier03", 6, 950, 62, 120)
						},
						rewTPS(7800)),
				step("frieza", 4, "04_defeat_dodoria.json",
						namekReq(138, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_dodoria", 1, 1450, 85, 185)
						},
						rewTPS(8600)),
				step("frieza", 5, "05_defeat_zarbon.json",
						namekReq(148, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_zarbont1", 1, 1700, 95, 220)
						},
						rewTPS(9400)),
				step("frieza", 6, "06_the_saiyan_prince.json",
						namekReq(160, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_vegeta_namek", 1, 1800, 100, 230)
						},
						rewTPS(10200)),
				step("frieza", 7, "07_defeat_guldo.json",
						namekReq(172, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_guldo", 1, 900, 55, 100)
						},
						rewTPS(10800)),
				step("frieza", 8, "08_defeat_recoome.json",
						namekReq(184, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_recoome", 1, 2200, 120, 200)
						},
						rewTPS(11600)),
				step("frieza", 9, "09_defeat_burter_and_jeice.json",
						namekReq(196, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_burter", 1, 2200, 120, 200),
								objKill("dragonminez:saga_jeice", 1, 2200, 120, 200)
						},
						rewTPS(12600)),
				step("frieza", 10, "10_defeat_ginyu.json",
						namekReq(208, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_ginyu", 1, 3200, 170, 280)
						},
						rewTPS(13600)),
				step("frieza", 11, "11_defeat_ginyu_goku.json",
						namekReq(220, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_ginyu_goku", 1, 1800, 95, 155)
						},
						rewTPS(14400)),
				step("frieza", 12, "12_defeat_frieza_first.json",
						namekReq(255, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_frieza_first", 1, 4200, 210, 360)
						},
						rewTPS(16600)),
				step("frieza", 13, "13_defeat_frieza_third.json",
						namekReq(280, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_frieza_third", 1, 8500, 420, 690)
						},
						rewTPS(17800)),
				step("frieza", 14, "14_defeat_frieza_base.json",
						namekReq(315, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_frieza_base", 1, 14000, 650, 1050)
						},
						rewTPS(19400)),
				step("frieza", 15, "15_defeat_frieza_full_power.json",
						namekReq(350, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_frieza_fp", 1, 17000, 800, 1260)
						},
						rewTPS(21000)),
				step("frieza", 16, "16_escape_namek_before_collapse.json",
						namekReq(360),
						new JsonObject[]{
								objDimension("minecraft:overworld")
						},
						rewTPS(12000))
		);
	}

	// ========================================================================================
	// Android Saga Quests (folder: saga_android)
	// ========================================================================================

	private static void createAndroidSagaQuests(Path questsDir) {
		JsonObject prevFrieza = prevQuest("frieza_saga", 16);

		writeLinearSaga(questsDir.resolve("saga_android"), "android_saga", "saga_android", prevFrieza,
				step("android", 1, "01_defeat_mecha_frieza.json",
						earthReq(440, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_mecha_frieza", 1, 17000, 800, 1300)
						},
						rewTPS(19000)),
				step("android", 2, "02_defeat_king_cold.json",
						earthReq(470, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_king_cold", 1, 9000, 430, 700)
						},
						rewTPS(19800)),
				step("android", 3, "03_warning_from_the_future.json",
						earthReq(500),
						new JsonObject[]{
								objTalkTo("trunks")
						},
						rewTPS(6000)),
				step("android", 4, "04_three_year_training.json",
						earthReq(560, condRealTimeMinutes(30)),
						new JsonObject[]{
								objKill("dragonminez:shadow_dummy", 16, 1800, 110, 190)
						},
						rewTPS(21000)),
				step("android", 5, "05_defeat_a19.json",
						earthReq(620, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_a19", 1, 22000, 1000, 1600)
						},
						rewTPS(21800)),
				step("android", 6, "06_defeat_drgero.json",
						earthReq(660, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_drgero", 1, 18500, 880, 1400)
						},
						rewTPS(22600)),
				step("android", 7, "07_track_android_signal.json",
						earthReq(700),
						new JsonObject[]{
								objBiome("#minecraft:is_mountain")
						},
						rewTPS(3500)),
				step("android", 8, "08_defeat_a18.json",
						earthReq(740, condBiome("#minecraft:is_mountain")),
						new JsonObject[]{
								objKill("dragonminez:saga_a18", 1, 30000, 1400, 2200)
						},
						rewTPS(23600)),
				step("android", 9, "09_defeat_a17.json",
						earthReq(780, condBiome("#minecraft:is_mountain")),
						new JsonObject[]{
								objKill("dragonminez:saga_a17", 1, 35000, 1700, 3200)
						},
						rewTPS(24400)),
				step("android", 10, "10_defeat_cell_imperfect.json",
						earthReq(980, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_imperfect", 1, 31000, 1450, 2300)
						},
						rewTPS(27600)),
				step("android", 11, "11_defeat_cell_semiperfect.json",
						earthReq(1060, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_semiperfect", 1, 42000, 1900, 3100)
						},
						rewTPS(29200)),
				step("android", 12, "12_beyond_super_saiyan.json",
						earthReq(1100, condRealTimeMinutes(10)),
						new JsonObject[]{
								objKill("dragonminez:shadow_dummy", 20, 2200, 140, 230)
						},
						rewTPS(30800)),
				step("android", 13, "13_defeat_cell_perfect.json",
						earthReq(1180, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_perfect", 1, 62000, 2900, 4700)
						},
						rewTPS(31600)),
				step("android", 14, "14_defeat_cell_jrs.json",
						earthReq(1300, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_jr", 7, 43000, 1980, 3850)
						},
						rewTPS(33200)),
				step("android", 15, "15_defeat_cell_superperfect.json",
						earthReq(1380, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_superperfect", 1, 82000, 3900, 6200)
						},
						rewTPS(35200))
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
		 Path dir = questsDir.resolve("saga_movies");
		 String s = "movies_saga", c = "saga_movies";
		 JsonObject prevBuu = prevQuest("buu_saga", 40);

//		 writeLinearSaga(dir, s, c, prevBuu,
//
//		 );
	}
}

