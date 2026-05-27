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
		createFutureSagaQuests(questsDir);
		createBuuSagaQuests(questsDir);
		createMoviesSagaQuests(questsDir);
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

	private static JsonObject objSkill(String skill, int level) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "SKILL");
		o.addProperty("skill", skill);
		o.addProperty("level", level);
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

	private static JsonObject rewSkill(String skill, int level) {
		JsonObject r = new JsonObject();
		r.addProperty("type", "SKILL");
		r.addProperty("skill", skill);
		r.addProperty("level", level);
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

	private static JsonObject condSkill(String skill, int minLevel) {
		JsonObject c = new JsonObject();
		c.addProperty("type", "SKILL");
		c.addProperty("skill", skill);
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

	private static void writeSaga(Path dir, String sagaId, String category, JsonObject firstPrereq, QuestStep... steps) {
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
		writeSaga(questsDir.resolve("saga_saiyan"), "saiyan_saga", "saga_saiyan", null,
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
		writeSaga(questsDir.resolve("saga_frieza"), "frieza_saga", "saga_frieza", prevSaiyan,
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
								objKill("dragonminez:saga_zarbon", 1, 1700, 95, 220)
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
								objKill("dragonminez:saga_frieza_second", 1, 8500, 420, 690)
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

		writeSaga(questsDir.resolve("saga_android"), "android_saga", "saga_android", prevFrieza,
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
	// Future Saga Quests (folder: saga_future)
	// ========================================================================================

	private static void createFutureSagaQuests(Path questsDir) {
		JsonObject prevAndroid = prevQuest("android_saga", 15);

		writeSaga(questsDir.resolve("saga_future"), "future_saga", "saga_future", prevAndroid,
				step("future", 1, "01_talk_to_future_trunks.json",
						earthReq(1380),
						new JsonObject[]{
								objTalkTo("trunks")
						},
						rewTPS(8000)),
				step("future", 2, "02_train_with_trunks_and_gohan.json",
						earthReq(1400, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_ftrunks_base", 1, 52000, 2500, 4000),
								objKill("dragonminez:saga_fgohan_base", 1, 56000, 2700, 4300)
						},
						rewTPS(22000)),
				step("future", 3, "03_androids_ruined_plains.json",
						earthReq(1440, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_a17", 1, 62000, 3000, 4800),
								objKill("dragonminez:saga_a18", 1, 60000, 2900, 4600)
						},
						rewTPS(26000)),
				step("future", 4, "04_face_future_gohan.json",
						earthReq(1480, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_fgohan_ssj", 1, 68000, 3200, 5200)
						},
						rewTPS(28000)),
				step("future", 5, "05_androids_in_the_mountains.json",
						earthReq(1520, condBiome("#minecraft:is_mountain")),
						new JsonObject[]{
								objKill("dragonminez:saga_a18", 1, 68000, 3300, 5300),
								objKill("dragonminez:saga_a17", 1, 71000, 3500, 5600)
						},
						rewTPS(32000)),
				step("future", 6, "06_imperfect_cell_of_the_future.json",
						earthReq(1560, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_imperfect", 1, 76000, 3700, 5900)
						},
						rewTPS(36000)),
				step("future", 7, "07_future_restored.json",
						earthReq(1580),
						new JsonObject[]{
								objTalkTo("trunks")
						},
						rewTPS(18000))
		);
	}

	// ========================================================================================
	// Buu Saga Quests (folder: saga_buu)
	// ========================================================================================

	private static void createBuuSagaQuests(Path questsDir) {
		JsonObject prevAndroid = prevQuest("android_saga", 15);

		writeSaga(questsDir.resolve("saga_buu"), "buu_saga", "saga_buu", prevAndroid,
				step("buu", 1, "01_train_with_goten_and_gohan.json",
						earthReq(1420, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_goten", 1, 50000, 2400, 3900),
								objKill("dragonminez:saga_gohan_end_base", 1, 62000, 3000, 4800)
						},
						rewTPS(36000)),
				step("buu", 2, "02_assemble_gravity_device_parts.json",
						earthReq(1460),
						new JsonObject[]{
								objItem("dragonminez:kikono_station", 1),
								objItem("dragonminez:fuel_generator", 1),
								objItem("dragonminez:energy_cable", 8)
						},
						rewTPS(22000)),
				step("buu", 3, "03_train_with_trunks_and_vegeta.json",
						earthReq(1500, condRealTimeMinutes(10)),
						new JsonObject[]{
								objKill("dragonminez:saga_kid_trunks", 1, 54000, 2600, 4200),
								objKill("dragonminez:saga_vegeta_end_base", 1, 70000, 3400, 5500)
						},
						rewTPS(42000)),
				step("buu", 4, "04_enter_the_world_tournament.json",
						earthReq(1540, condBiome("minecraft:plains")),
						new JsonObject[]{
								objTalkTo("shin")
						},
						rewTPS(12000)),
				step("buu", 5, "05_tournament_goten.json",
						earthReq(1560, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_goten", 1, 56000, 2700, 4300)
						},
						rewTPS(24000)),
				step("buu", 6, "06_tournament_trunks.json",
						earthReq(1580, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_kid_trunks", 1, 60000, 2900, 4600)
						},
						rewTPS(26000)),
				step("buu", 7, "07_tournament_krillin.json",
						earthReq(1600, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_krillin", 1, 62000, 3000, 4800)
						},
						rewTPS(26000)),
				step("buu", 8, "08_tournament_shin.json",
						earthReq(1620, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_shin", 1, 66000, 3200, 5100)
						},
						rewTPS(28000)),
				step("buu", 9, "09_tournament_spopovich.json",
						earthReq(1640, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_spopovitch", 1, 68000, 3300, 5200)
						},
						rewTPS(30000)),
				step("buu", 10, "10_find_babidi_ship.json",
						earthReq(1660, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objTalkTo("shin")
						},
						rewTPS(14000)),
				step("buu", 11, "11_babidi_level_pui_pui.json",
						earthReq(1680, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_puipui", 1, 72000, 3500, 5600)
						},
						rewTPS(32000)),
				step("buu", 12, "12_babidi_level_yakon.json",
						earthReq(1700, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_yakon", 1, 78000, 3800, 6100)
						},
						rewTPS(34000)),
				step("buu", 13, "13_babidi_level_dabura.json",
						earthReq(1740, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_dabura", 1, 88000, 4300, 6900)
						},
						rewTPS(38000)),
				step("buu", 14, "14_fat_buu_awakes.json",
						earthReq(1780, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_buufat", 1, 98000, 4800, 7600)
						},
						rewTPS(42000)),
				step("buu", 15, "15_goku_and_vegeta_clash.json",
						earthReq(1820, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_goku_end_ssj2", 1, 92000, 4500, 7200),
								objKill("dragonminez:saga_vegeta_majin", 1, 96000, 4700, 7500)
						},
						rewTPS(44000)),
				step("buu", 16, "16_second_fat_buu_battle.json",
						earthReq(1860, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_buufat", 1, 110000, 5400, 8600)
						},
						rewTPS(46000)),
				step("buu", 17, "17_stop_babidi.json",
						earthReq(1880, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_babidi", 1, 70000, 3400, 5400)
						},
						rewTPS(30000)),
				step("buu", 18, "18_goku_super_saiyan_three.json",
						earthReq(1920, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_goku_end_ssj3", 1, 125000, 6100, 9800)
						},
						rewTPS(52000)),
				step("buu", 19, "19_beach_training_with_gotenks.json",
						earthReq(1940, condBiome("#minecraft:is_beach")),
						new JsonObject[]{
								objKill("dragonminez:saga_gotenks", 1, 90000, 4400, 7000)
						},
						rewTPS(42000)),
				step("buu", 20, "20_evil_buu_at_buus_house.json",
						earthReq(1960, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_evilbuu", 1, 115000, 5600, 9000)
						},
						rewTPS(52000)),
				step("buu", 21, "21_krillin_and_android_18.json",
						earthReq(1980, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_krillin", 1, 78000, 3800, 6100),
								objKill("dragonminez:saga_a18", 1, 90000, 4400, 7000)
						},
						rewTPS(44000)),
				step("buu", 22, "22_super_buu_in_the_time_chamber.json",
						dimensionReq("dragonminez:time_chamber", 2020),
						new JsonObject[]{
								objKill("dragonminez:saga_superbuu", 1, 135000, 6600, 10500)
						},
						rewTPS(58000)),
				step("buu", 23, "23_gotenks_rocky_wasteland.json",
						earthReq(2040, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_gotenks_ssj3", 1, 135000, 6600, 10500)
						},
						rewTPS(56000)),
				step("buu", 24, "24_sacred_world_and_z_sword.json",
						namekReq(2060, condBiome("dragonminez:sacred_land"), condSkill("potentialunlock", 20)),
						new JsonObject[]{
								objItem("dragonminez:z_sword", 1),
								objSkill("potentialunlock", 30)
						},
						rewTPS(52000)),
				step("buu", 25, "25_super_buu_returns.json",
						earthReq(2080, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_superbuu", 1, 145000, 7100, 11400)
						},
						rewTPS(60000)),
				step("buu", 26, "26_super_buu_gotenks.json",
						earthReq(2100, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_superbuu_gotenks", 1, 160000, 7800, 12500)
						},
						rewTPS(66000)),
				step("buu", 27, "27_super_buu_gohan.json",
						earthReq(2140, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_superbuu_gohan", 1, 175000, 8600, 13800)
						},
						rewTPS(72000)),
				step("buu", 28, "28_face_vegetto.json",
						earthReq(2180, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_goku_end_ssj2", 1, 120000, 5900, 9400),
								objKill("dragonminez:saga_vegeta_end_ssj2", 1, 120000, 5900, 9400)
						},
						rewTPS(68000)),
				step("buu", 29, "29_return_to_the_sacred_world.json",
						namekReq(2200, condBiome("dragonminez:sacred_land")),
						new JsonObject[]{
								objStructure("dragonminez:village_sacred")
						},
						rewTPS(22000)),
				step("buu", 30, "30_kid_buu.json",
						namekReq(2240, condBiome("dragonminez:sacred_land")),
						new JsonObject[]{
								objKill("dragonminez:saga_kidbuu", 1, 185000, 9100, 14500)
						},
						rewTPS(76000)),
				step("buu", 31, "31_goku_ssj3_final_stand.json",
						namekReq(2260, condBiome("dragonminez:sacred_land")),
						new JsonObject[]{
								objKill("dragonminez:saga_goku_end_ssj3", 1, 155000, 7600, 12200)
						},
						rewTPS(62000)),
				step("buu", 32, "32_vegeta_ssj2_final_stand.json",
						namekReq(2280, condBiome("dragonminez:sacred_land")),
						new JsonObject[]{
								objKill("dragonminez:saga_vegeta_end_ssj2", 1, 145000, 7100, 11400)
						},
						rewTPS(60000)),
				step("buu", 33, "33_satan_and_majin_buu.json",
						namekReq(2300, condBiome("dragonminez:sacred_land")),
						new JsonObject[]{
								objKill("dragonminez:saga_buufat", 1, 125000, 6100, 9800)
						},
						rewTPS(52000)),
				step("buu", 34, "34_destroy_kid_buu.json",
						namekReq(2320, condBiome("dragonminez:sacred_land")),
						new JsonObject[]{
								objKill("dragonminez:saga_kidbuu", 1, 220000, 10800, 17200)
						},
						rewTPS(90000)),
				step("buu", 35, "35_return_to_earth.json",
						earthReq(2320),
						new JsonObject[]{
								objDimension("minecraft:overworld")
						},
						rewTPS(30000))
		);
	}

	// ========================================================================================
	// Movies Saga Quests (folder: saga_movies)
	// ========================================================================================

	private static void createMoviesSagaQuests(Path questsDir) {
		JsonObject prevBuu = prevQuest("buu_saga", 35);

		writeSaga(questsDir.resolve("saga_movies"), "movies_saga", "saga_movies", prevBuu,
				step("movies", 1, "01_kamis_lookout_warning.json",
						earthReq(2340),
						new JsonObject[]{
								objStructure("dragonminez:kamilookout"),
								objTalkTo("dende")
						},
						rewTPS(30000)),
				step("movies", 2, "02_garlic_jr_in_the_wasteland.json",
						earthReq(2340, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_garlic_jr", 1, 95000, 4700, 7600)
						},
						rewTPS(52000)),
				step("movies", 3, "03_garlic_jr_transformed.json",
						earthReq(2350, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_garlic_jr", 1, 130000, 6400, 10200)
						},
						rewTPS(62000)),
				step("movies", 4, "04_frozen_biome_signal.json",
						earthReq(2360, condBiome("minecraft:snowy_plains")),
						new JsonObject[]{
								objBiome("minecraft:snowy_plains")
						},
						rewTPS(26000)),
				step("movies", 5, "05_wheelo_controlled_allies.json",
						earthReq(2360, condBiome("minecraft:snowy_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_kid_gohan", 1, 70000, 3400, 5400),
								objKill("dragonminez:saga_krillin", 1, 68000, 3300, 5200)
						},
						rewTPS(58000)),
				step("movies", 6, "06_dr_wheelo.json",
						earthReq(2370, condBiome("minecraft:snowy_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_dr_wheelo", 1, 145000, 7100, 11400)
						},
						rewTPS(68000)),
				step("movies", 7, "07_tree_of_might_wasteland.json",
						earthReq(2380, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objBiome("dragonminez:rocky")
						},
						rewTPS(26000)),
				step("movies", 8, "08_turles_goku.json",
						earthReq(2380, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_goku_mid_base", 1, 105000, 5100, 8200)
						},
						rewTPS(56000)),
				step("movies", 9, "09_turles_oozaru_gohan.json",
						earthReq(2390, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_ozaru", 1, 150000, 7400, 11800)
						},
						rewTPS(70000)),
				step("movies", 10, "10_turles.json",
						earthReq(2400, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_turles", 1, 160000, 7900, 12600)
						},
						rewTPS(76000)),
				step("movies", 11, "11_slug_soldiers.json",
						earthReq(2410, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_slug_soldier", 8, 45000, 2200, 3600)
						},
						rewTPS(56000)),
				step("movies", 12, "12_slug.json",
						earthReq(2420, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_slug", 1, 150000, 7400, 11800)
						},
						rewTPS(72000)),
				step("movies", 13, "13_giant_slug.json",
						earthReq(2430, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_slug", 1, 190000, 9300, 14900)
						},
						rewTPS(84000)),
				step("movies", 14, "14_cooler_armored_squadron.json",
						earthReq(2440, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_neiz", 1, 90000, 4400, 7000),
								objKill("dragonminez:saga_salza", 1, 95000, 4700, 7500),
								objKill("dragonminez:saga_dore", 1, 105000, 5100, 8200)
						},
						rewTPS(78000)),
				step("movies", 15, "15_cooler.json",
						earthReq(2450, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_cooler", 1, 175000, 8600, 13800)
						},
						rewTPS(82000)),
				step("movies", 16, "16_cooler_fifth_form.json",
						earthReq(2460, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_cooler", 1, 230000, 11300, 18100)
						},
						rewTPS(94000)),
				step("movies", 17, "17_big_gete_star.json",
						namekReq(2470, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_gete_machine", 10, 60000, 2900, 4700)
						},
						rewTPS(70000)),
				step("movies", 18, "18_metal_cooler.json",
						namekReq(2480, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_metal_cooler", 1, 220000, 10800, 17200)
						},
						rewTPS(92000)),
				step("movies", 19, "19_metal_cooler_core.json",
						namekReq(2490, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_metal_cooler_core", 1, 260000, 12700, 20400)
						},
						rewTPS(104000)),
				step("movies", 20, "20_androids_in_the_ice.json",
						earthReq(2500, condBiome("minecraft:snowy_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_a14", 1, 135000, 6600, 10500),
								objKill("dragonminez:saga_a15", 1, 130000, 6400, 10200)
						},
						rewTPS(82000)),
				step("movies", 21, "21_android_13.json",
						earthReq(2510, condBiome("minecraft:snowy_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_a13", 1, 190000, 9300, 14900)
						},
						rewTPS(90000)),
				step("movies", 22, "22_super_android_13.json",
						earthReq(2520, condBiome("minecraft:snowy_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_a13", 1, 250000, 12200, 19600)
						},
						rewTPS(102000)),
				step("movies", 23, "23_broly_base.json",
						earthReq(2530, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_broly", 1, 220000, 10800, 17200)
						},
						rewTPS(94000)),
				step("movies", 24, "24_paragus.json",
						earthReq(2540, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_paragus", 1, 105000, 5100, 8200)
						},
						rewTPS(52000)),
				step("movies", 25, "25_legendary_broly.json",
						earthReq(2550, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_broly", 1, 310000, 15200, 24400)
						},
						rewTPS(118000)),
				step("movies", 26, "26_bojack_allies.json",
						earthReq(2560, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_bojack_ally", 4, 90000, 4400, 7000)
						},
						rewTPS(76000)),
				step("movies", 27, "27_gokua.json",
						earthReq(2570, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_gokua", 1, 165000, 8100, 13000)
						},
						rewTPS(78000)),
				step("movies", 28, "28_bojack.json",
						earthReq(2580, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_bojack", 1, 225000, 11000, 17600)
						},
						rewTPS(94000)),
				step("movies", 29, "29_full_power_bojack.json",
						earthReq(2590, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_bojack", 1, 285000, 14000, 22400)
						},
						rewTPS(112000)),
				step("movies", 30, "30_broly_second_coming.json",
						earthReq(2600, condBiome("minecraft:snowy_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_broly", 1, 260000, 12700, 20400)
						},
						rewTPS(104000)),
				step("movies", 31, "31_goten_and_trunks.json",
						earthReq(2610, condBiome("minecraft:snowy_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_goten", 1, 110000, 5400, 8600),
								objKill("dragonminez:saga_kid_trunks", 1, 115000, 5600, 9000)
						},
						rewTPS(76000)),
				step("movies", 32, "32_legendary_broly_second_coming.json",
						earthReq(2620, condBiome("minecraft:snowy_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_broly", 1, 340000, 16700, 26700)
						},
						rewTPS(124000)),
				step("movies", 33, "33_bio_broly.json",
						earthReq(2630, condBiome("minecraft:swamp")),
						new JsonObject[]{
								objKill("dragonminez:saga_bio_broly", 1, 240000, 11800, 18800)
						},
						rewTPS(96000)),
				step("movies", 34, "34_giant_bio_broly.json",
						earthReq(2640, condBiome("minecraft:swamp")),
						new JsonObject[]{
								objKill("dragonminez:saga_bio_broly", 1, 300000, 14700, 23500)
						},
						rewTPS(114000)),
				step("movies", 35, "35_otherworld_tournament.json",
						dimensionReq("dragonminez:otherworld", 2650),
						new JsonObject[]{
								objKill("dragonminez:saga_paikuhan", 1, 190000, 9300, 14900)
						},
						rewTPS(82000)),
				step("movies", 36, "36_janemba.json",
						dimensionReq("dragonminez:otherworld", 2660),
						new JsonObject[]{
								objKill("dragonminez:saga_janemba", 1, 270000, 13200, 21200)
						},
						rewTPS(104000)),
				step("movies", 37, "37_super_janemba.json",
						dimensionReq("dragonminez:otherworld", 2670),
						new JsonObject[]{
								objKill("dragonminez:saga_janemba", 1, 360000, 17600, 28200)
						},
						rewTPS(132000)),
				step("movies", 38, "38_hildegarn_half.json",
						earthReq(2680, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_hildegarn", 1, 280000, 13700, 22000)
						},
						rewTPS(108000)),
				step("movies", 39, "39_hildegarn_complete.json",
						earthReq(2690, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_hildegarn", 1, 350000, 17100, 27400)
						},
						rewTPS(128000)),
				step("movies", 40, "40_super_hildegarn.json",
						earthReq(2700, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_hildegarn", 1, 430000, 21100, 33700)
						},
						rewTPS(150000))
		);
	}
}

