package com.dragonminez.common.quest;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.google.gson.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates default individual quest JSON files in the unified schema.
 * <p>
 * Files use the same schema for all quest types & only the {@code "type"} field differs.
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

	private static final String PARTY_SCALING_KEY = "party_scaling";

	private static Path dmzBase;

	private QuestDefaults() {
	} // utility class

	static void createDefaultQuestFiles(Path questsDir) {
		if (!ConfigManager.getServerConfig().getGameplay().getStoryModeEnabled()) return;
		if (!ConfigManager.getServerConfig().getGameplay().getCreateDefaultSagas()) return;

		dmzBase = questsDir.getParent();

		createSaiyanSagaQuests(questsDir);
		createFriezaSagaQuests(questsDir);
		createAndroidSagaQuests(questsDir);
		createFutureSagaQuests(questsDir);
		createBuuSagaQuests(questsDir);
		createMoviesSagaQuests(questsDir);
	}

	// ---- Helpers ----

	private static void writeQuest(Path dir, String filename, JsonObject quest) {
		try {
			Files.createDirectories(dir);
			QuestUpgrader.upgradeOrWrite(dmzBase, dir.resolve(filename), quest);
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
		return objKill(entity, count, hp, melee, ki, 0);
	}

	private static JsonObject objKill(String entity, int count, double hp, double melee, double ki, int aiTier) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "KILL");
		o.addProperty("entity", entity);
		o.addProperty("count", count);
		o.addProperty("health", hp);
		o.addProperty("meleeDamage", melee);
		o.addProperty("kiDamage", ki);
		o.addProperty("spawn", "QUEST");
		o.addProperty("count_mode", "QUEST_SPAWNED_ONLY");
		if (aiTier > 0) o.addProperty("AITier", aiTier);
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

	private static JsonObject onlyOn(JsonObject reward, String... difficulties) {
		JsonArray arr = new JsonArray();
		for (String difficulty : difficulties) arr.add(difficulty);
		reward.add("difficulty", arr);
		return reward;
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
//		JsonObject[] withAlignment = new JsonObject[conditions.length + 1];
//		System.arraycopy(conditions, 0, withAlignment, 0, conditions.length);
//		withAlignment[conditions.length] = condAlignmentMin(0);
//		return prereqs(op, withAlignment);
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

	private static JsonObject sacredKaiReq(int minLevel, JsonObject... extraConditions) {
		return dimensionReq("dragonminez:sacredkaiplanet", minLevel, extraConditions);
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
								objKill("dragonminez:saga_raditz", 1, 450, 22, 39, 1)
						},
						rewTPS(2400), rewItem("dragonminez:broken_scouter", 1)),
				step("saiyan", 2, "02_survive_wilderness_training.json",
						earthReq(8, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:dino1", 1, 900, 12, 0)
						},
						rewTPS(3600), rewItem("dragonminez:cooked_dino_meat", 8)),
				step("saiyan", 3, "03_kill_the_saibamans.json",
						earthReq(15, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_saibaman1", 6, 520, 33, 46, 1)
						},
						rewTPS(5000)),
				step("saiyan", 4, "04_hold_against_nappa.json",
						earthReq(22, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_nappa", 1, 975, 59, 91, 2)
						}, rewTPS(6000)),
				step("saiyan", 5, "05_face_vegeta.json",
						earthReq(30, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_vegeta", 1, 1560, 91, 137, 2)
						},
						rewTPS(7000)),
				step("saiyan", 6, "06_defeat_oozaru_vegeta.json",
						earthReq(40, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_ozaruvegeta", 1, 3250, 182, 182, 3)
						},
						rewTPS(8000)),
				step("saiyan", 7, "07_prepare_for_namek.json",
						earthReq(70, condRealTimeMinutes(5)),
						new JsonObject[]{
								objTalkTo("bulma")
						},
						rewTPS(2000), rewItem("dragonminez:saiyan_ship", 1)),
				step("saiyan", 8, "08_head_to_namek.json",
						earthReq(100),
						new JsonObject[]{
								objDimension("dragonminez:namek")
						},
						rewTPS(2000))
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
								objKill("dragonminez:saga_friezasoldier01", 8, 1547, 100, 107, 1)
						},
						rewTPS(14400)),
				step("frieza", 2, "02_defeat_cui.json",
						namekReq(112, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cui", 1, 2652, 155, 179, 1)
						},
						rewTPS(16320)),
				step("frieza", 3, "03_defend_the_namekians.json",
						namekReq(124, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_friezasoldier02", 10, 1879, 122, 131, 1),
								objKill("dragonminez:saga_friezasoldier03", 6, 2100, 137, 143, 1)
						},
						rewTPS(18720)),
				step("frieza", 4, "04_defeat_dodoria.json",
						namekReq(138, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_dodoria", 1, 3205, 189, 221, 1)
						},
						rewTPS(20640)),
				step("frieza", 5, "05_defeat_zarbon.json",
						namekReq(148, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_zarbon", 1, 3757, 211, 262, 1)
						},
						rewTPS(22560)),
				step("frieza", 6, "06_the_saiyan_prince.json",
						namekReq(160, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_vegeta_namek", 1, 3978, 221, 274, 2)
						},
						rewTPS(24480)),
				step("frieza", 7, "07_defeat_guldo.json",
						namekReq(172, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_guldo", 1, 1989, 122, 119, 2)
						},
						rewTPS(25920)),
				step("frieza", 8, "08_defeat_recoome.json",
						namekReq(184, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_recoome", 1, 4862, 265, 238, 2)
						},
						rewTPS(27840)),
				step("frieza", 9, "09_defeat_burter_and_jeice.json",
						namekReq(196, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_burter", 1, 4862, 265, 238, 2),
								objKill("dragonminez:saga_jeice", 1, 4862, 265, 238, 2)
						},
						rewTPS(30240)),
				step("frieza", 10, "10_defeat_ginyu.json",
						namekReq(208, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_ginyu", 1, 7072, 376, 333, 2)
						},
						rewTPS(32640)),
				step("frieza", 11, "11_defeat_ginyu_goku.json",
						namekReq(220, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_ginyu_goku", 1, 3978, 211, 185, 2)
						},
						rewTPS(34560)),
				step("frieza", 12, "12_defeat_frieza_first.json",
						namekReq(255, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_frieza_first", 1, 9282, 464, 428, 3)
						},
						rewTPS(39840)),
				step("frieza", 13, "13_defeat_frieza_third.json",
						namekReq(280, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_frieza_second", 1, 18785, 928, 821, 3)
						},
						rewTPS(42720)),
				step("frieza", 14, "14_defeat_frieza_base.json",
						namekReq(315, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_frieza_base", 1, 30940, 1437, 1250, 3)
						},
						rewTPS(46560)),
				step("frieza", 15, "15_defeat_frieza_full_power.json",
						namekReq(350, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_frieza_fp", 1, 37570, 1768, 1499, 3)
						},
						rewTPS(50400)),
				step("frieza", 16, "16_escape_namek_before_collapse.json",
						namekReq(360),
						new JsonObject[]{
								objDimension("minecraft:overworld")
						},
						rewTPS(28800))
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
								objKill("dragonminez:saga_mecha_frieza", 1, 59500, 2400, 2210, 2)
						},
						rewTPS(53200)),
				step("android", 2, "02_defeat_king_cold.json",
						earthReq(470, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_king_cold", 1, 31500, 1290, 1190, 1)
						},
						rewTPS(55440)),
				step("android", 3, "03_warning_from_the_future.json",
						earthReq(500),
						new JsonObject[]{
								objTalkTo("trunks")
						},
						rewTPS(16800)),
				step("android", 4, "04_three_year_training.json",
						earthReq(560, condRealTimeMinutes(30)),
						new JsonObject[]{
								objKill("dragonminez:shadow_dummy", 16, 6300, 330, 323)
						},
						rewTPS(58800)),
				step("android", 5, "05_defeat_a19.json",
						earthReq(620, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_a19", 1, 77000, 3000, 2720, 2)
						},
						rewTPS(61040)),
				step("android", 6, "06_defeat_drgero.json",
						earthReq(660, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_drgero", 1, 64750, 2640, 2380, 2)
						},
						rewTPS(63280)),
				step("android", 7, "07_track_android_signal.json",
						earthReq(700),
						new JsonObject[]{
								objBiome("#minecraft:is_mountain")
						},
						rewTPS(9800)),
				step("android", 8, "08_defeat_a18.json",
						earthReq(740, condBiome("#minecraft:is_mountain")),
						new JsonObject[]{
								objKill("dragonminez:saga_a18", 1, 105000, 4200, 3740, 2)
						},
						rewTPS(66080)),
				step("android", 9, "09_defeat_a17.json",
						earthReq(780, condBiome("#minecraft:is_mountain")),
						new JsonObject[]{
								objKill("dragonminez:saga_a17", 1, 122500, 5100, 5440, 2)
						},
						rewTPS(68320)),
				step("android", 10, "10_defeat_cell_imperfect.json",
						earthReq(980, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_imperfect", 1, 108500, 4350, 3910, 2)
						},
						rewTPS(77280)),
				step("android", 11, "11_defeat_cell_semiperfect.json",
						earthReq(1060, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_semiperfect", 1, 147000, 5700, 5270, 2)
						},
						rewTPS(81760)),
				step("android", 12, "12_beyond_super_saiyan.json",
						earthReq(1100, condRealTimeMinutes(10)),
						new JsonObject[]{
								objKill("dragonminez:shadow_dummy", 20, 7700, 420, 391)
						},
						rewTPS(86240)),
				step("android", 13, "13_defeat_cell_perfect.json",
						earthReq(1180, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_perfect", 1, 217000, 8700, 7990, 3)
						},
						rewTPS(88480)),
				step("android", 14, "14_defeat_cell_jrs.json",
						earthReq(1300, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_jr", 7, 150500, 5940, 6545, 2)
						},
						rewTPS(92960)),
				step("android", 15, "15_defeat_cell_superperfect.json",
						earthReq(1380, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_superperfect", 1, 287000, 11700, 10540, 3)
						},
						rewTPS(98560))
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
						rewTPS(25600)),
				step("future", 2, "02_train_with_trunks_and_gohan.json",
						earthReq(1400, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_ftrunks_base", 1, 182000, 7500, 6800, 2),
								objKill("dragonminez:saga_fgohan_base", 1, 196000, 8100, 7310, 2)
						},
						rewTPS(70400)),
				step("future", 3, "03_androids_ruined_plains.json",
						earthReq(1440, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_a17", 1, 217000, 9000, 8160, 2),
								objKill("dragonminez:saga_a18", 1, 210000, 8700, 7820, 2)
						},
						rewTPS(83200)),
				step("future", 4, "04_face_future_gohan.json",
						earthReq(1480, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_fgohan_ssj", 1, 238000, 9600, 8840, 3)
						},
						rewTPS(89600)),
				step("future", 5, "05_androids_in_the_mountains.json",
						earthReq(1520, condBiome("#minecraft:is_mountain")),
						new JsonObject[]{
								objKill("dragonminez:saga_a18", 1, 238000, 9900, 9010, 3),
								objKill("dragonminez:saga_a17", 1, 248500, 10500, 9520, 3)
						},
						rewTPS(102400)),
				step("future", 6, "06_imperfect_cell_of_the_future.json",
						earthReq(1560, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_imperfect", 1, 266000, 11100, 10030, 3)
						},
						rewTPS(115200)),
				step("future", 7, "07_future_restored.json",
						earthReq(1580),
						new JsonObject[]{
								objTalkTo("trunks")
						},
						rewTPS(57600))
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
								objKill("dragonminez:saga_goten", 1, 175000, 7200, 6630, 2),
								objKill("dragonminez:saga_gohan_end_base", 1, 217000, 9000, 8160, 2)
						},
						rewTPS(129600)),
				step("buu", 2, "02_assemble_gravity_device_parts.json",
						earthReq(1460),
						new JsonObject[]{
								objItem("dragonminez:kikono_station", 1),
								objItem("dragonminez:fuel_generator", 1),
								objItem("dragonminez:energy_cable", 8)
						},
						rewTPS(79200)),
				step("buu", 3, "03_train_with_trunks_and_vegeta.json",
						earthReq(1500, condRealTimeMinutes(10)),
						new JsonObject[]{
								objKill("dragonminez:saga_kid_trunks", 1, 189000, 7800, 7140, 2),
								objKill("dragonminez:saga_vegeta_end_base", 1, 245000, 10200, 9350, 2)
						},
						rewTPS(151200)),
				step("buu", 4, "04_enter_the_world_tournament.json",
						earthReq(1540, condBiome("minecraft:plains")),
						new JsonObject[]{
								objTalkTo("piccolo")
						},
						rewTPS(43200)),
				step("buu", 5, "05_tournament_goten.json",
						earthReq(1560, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_goten", 1, 196000, 8100, 7310, 1)
						},
						rewTPS(86400)),
				step("buu", 6, "06_tournament_trunks.json",
						earthReq(1580, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_kid_trunks", 1, 210000, 8700, 7820, 1)
						},
						rewTPS(93600)),
				step("buu", 7, "07_tournament_krillin.json",
						earthReq(1600, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_krillin", 1, 217000, 9000, 8160, 1)
						},
						rewTPS(93600)),
				step("buu", 8, "08_tournament_shin.json",
						earthReq(1620, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_shin", 1, 231000, 9600, 8670, 2)
						},
						rewTPS(100800)),
				step("buu", 9, "09_tournament_spopovich.json",
						earthReq(1640, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_spopovitch", 1, 238000, 9900, 8840, 2)
						},
						rewTPS(108000)),
				step("buu", 10, "10_find_babidi_ship.json",
						earthReq(1660),
						new JsonObject[]{
								objStructure("dragonminez:babidi")
						},
						rewTPS(50400)),
				step("buu", 11, "11_babidi_level_pui_pui.json",
						earthReq(1680, condStructure("dragonminez:babidi")),
						new JsonObject[]{
								objKill("dragonminez:saga_puipui", 1, 252000, 10500, 9520, 1)
						},
						rewTPS(115200)),
				step("buu", 12, "12_babidi_level_yakon.json",
						earthReq(1700, condStructure("dragonminez:babidi")),
						new JsonObject[]{
								objKill("dragonminez:saga_yakon", 1, 273000, 11400, 10370, 2)
						},
						rewTPS(122400)),
				step("buu", 13, "13_babidi_level_dabura.json",
						earthReq(1740, condStructure("dragonminez:babidi")),
						new JsonObject[]{
								objKill("dragonminez:saga_dabura", 1, 308000, 12900, 11730, 2)
						},
						rewTPS(136800)),
				step("buu", 14, "14_fat_buu_awakes.json",
						earthReq(1780, condStructure("dragonminez:babidi")),
						new JsonObject[]{
								objKill("dragonminez:saga_buufat", 1, 343000, 14400, 12920, 3)
						},
						rewTPS(151200)),
				step("buu", 15, "15_goku_and_vegeta_clash.json",
						earthReq(1820, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_goku_end_ssj2", 1, 322000, 13500, 12240, 3),
								objKill("dragonminez:saga_vegeta_majin", 1, 336000, 14100, 12750, 3)
						},
						rewTPS(158400)),
				step("buu", 16, "16_second_fat_buu_battle.json",
						earthReq(1860, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_buufat", 1, 385000, 16200, 14620, 3)
						},
						rewTPS(165600)),
				step("buu", 17, "17_stop_babidi.json",
						earthReq(1880, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_babidi", 1, 245000, 10200, 9180, 1)
						},
						rewTPS(108000)),
				step("buu", 18, "18_goku_super_saiyan_three.json",
						earthReq(1920, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_goku_end_ssj3", 1, 437500, 18300, 16660, 3)
						},
						rewTPS(187200)),
				step("buu", 19, "19_beach_training_with_gotenks.json",
						earthReq(1940, condBiome("#minecraft:is_beach")),
						new JsonObject[]{
								objKill("dragonminez:saga_gotenks", 1, 315000, 13200, 11900, 2)
						},
						rewTPS(151200)),
				step("buu", 20, "20_evil_buu_at_buus_house.json",
						earthReq(1960, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_evilbuu", 1, 402500, 16800, 15300, 3)
						},
						rewTPS(187200)),
				step("buu", 21, "21_krillin_and_android_18.json",
						earthReq(1980, condBiome("minecraft:plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_krillin", 1, 273000, 11400, 10370, 1),
								objKill("dragonminez:saga_a18", 1, 315000, 13200, 11900, 2)
						},
						rewTPS(158400)),
				step("buu", 22, "22_super_buu_in_the_time_chamber.json",
						dimensionReq("dragonminez:time_chamber", 2020),
						new JsonObject[]{
								objKill("dragonminez:saga_superbuu", 1, 472500, 19800, 17850, 3)
						},
						rewTPS(208800)),
				step("buu", 23, "23_gotenks_rocky_wasteland.json",
						earthReq(2040, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_gotenks_ssj3", 1, 472500, 19800, 17850, 3)
						},
						rewTPS(201600)),
				step("buu", 24, "24_sacred_world_and_z_sword.json",
						sacredKaiReq(2060, condSkill("potentialunlock", 10)),
						new JsonObject[]{
								objItem("dragonminez:z_sword", 1),
								objSkill("ultimate", 1)
						},
						rewTPS(187200)),
				step("buu", 25, "25_super_buu_returns.json",
						earthReq(2080, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_superbuu", 1, 507500, 21300, 19380, 3)
						},
						rewTPS(216000)),
				step("buu", 26, "26_super_buu_gotenks.json",
						earthReq(2100, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_superbuu_gotenks", 1, 560000, 23400, 21250, 3)
						},
						rewTPS(237600)),
				step("buu", 27, "27_super_buu_gohan.json",
						earthReq(2140, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_superbuu_gohan", 1, 612500, 25800, 23460, 3)
						},
						rewTPS(259200)),
				step("buu", 28, "28_face_vegetto.json",
						earthReq(2180, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_goku_end_ssj2", 1, 420000, 17700, 15980, 3),
								objKill("dragonminez:saga_vegeta_end_ssj2", 1, 420000, 17700, 15980, 3)
						},
						rewTPS(244800)),
				step("buu", 29, "29_return_to_the_sacred_world.json",
						sacredKaiReq(2200),
						new JsonObject[]{
								objDimension("dragonminez:sacredkaiplanet")
						},
						rewTPS(79200)),
				step("buu", 30, "30_kid_buu.json",
						sacredKaiReq(2240),
						new JsonObject[]{
								objKill("dragonminez:saga_kidbuu", 1, 647500, 27300, 24650, 3)
						},
						rewTPS(273600)),
				step("buu", 31, "31_goku_ssj3_final_stand.json",
						sacredKaiReq(2260),
						new JsonObject[]{
								objKill("dragonminez:saga_goku_end_ssj3", 1, 542500, 22800, 20740, 3)
						},
						rewTPS(223200)),
				step("buu", 32, "32_vegeta_ssj2_final_stand.json",
						sacredKaiReq(2280),
						new JsonObject[]{
								objKill("dragonminez:saga_vegeta_end_ssj2", 1, 507500, 21300, 19380, 3)
						},
						rewTPS(216000)),
				step("buu", 33, "33_satan_and_majin_buu.json",
						sacredKaiReq(2300),
						new JsonObject[]{
								objKill("dragonminez:saga_buufat", 1, 437500, 18300, 16660, 2)
						},
						rewTPS(187200)),
				step("buu", 34, "34_destroy_kid_buu.json",
						sacredKaiReq(2320),
						new JsonObject[]{
								objKill("dragonminez:saga_kidbuu", 1, 770000, 32400, 29240, 3)
						},
						rewTPS(324000)),
				step("buu", 35, "35_return_to_earth.json",
						earthReq(2320),
						new JsonObject[]{
								objDimension("minecraft:overworld")
						},
						rewTPS(108000))
		);
	}

	// ========================================================================================
	// Movies Saga Quests (folder: saga_movies)
	// ========================================================================================

	private static void createMoviesSagaQuests(Path questsDir) {
		JsonObject prevSaiyan = prevQuest("saiyan_saga", 1);

		writeSaga(questsDir.resolve("saga_movies"), "movies_saga", "saga_movies", prevSaiyan,
				step("movies", 1, "01_kamis_lookout_warning.json",
						earthReq(25),
						new JsonObject[]{
								objStructure("dragonminez:kamilookout"),
								objTalkTo("dende")
						},
						rewTPS(500)),
				step("movies", 2, "02_garlic_jr_in_the_wasteland.json",
						earthReq(30, condBiome("dragonminez:rocky"), condSaga("saiyan_saga", 5)),
						new JsonObject[]{
								objKill("dragonminez:saga_garlick_jr", 1, 3120, 182, 273, 2)
						},
						rewTPS(7500)),
				step("movies", 3, "03_garlic_jr_transformed.json",
						earthReq(45, condBiome("dragonminez:rocky"), condSaga("saiyan_saga", 6)),
						new JsonObject[]{
								objKill("dragonminez:saga_garlick_jr_transformed", 1, 6500, 364, 364, 3)
						},
						rewTPS(10000)),
				step("movies", 4, "04_frozen_biome_signal.json",
						earthReq(48, condBiome("minecraft:snowy_plains"), condSaga("saiyan_saga", 6)),
						new JsonObject[]{
								objBiome("minecraft:snowy_plains")
						},
						rewTPS(10600)),
				step("movies", 5, "05_wheelo_controlled_allies.json",
						earthReq(50, condBiome("minecraft:snowy_plains"), condSaga("saiyan_saga", 6)),
						new JsonObject[]{
								objKill("dragonminez:saga_kid_gohan", 1, 3120, 182, 273, 1),
								objKill("dragonminez:saga_krillin", 1, 1950, 118, 182, 1)
						},
						rewTPS(10900)),
				step("movies", 6, "06_dr_wheelo.json",
						earthReq(60, condBiome("minecraft:snowy_plains"), condSaga("saiyan_saga", 6)),
						new JsonObject[]{
								objKill("dragonminez:saga_dr_wheelo", 1, 6500, 364, 364, 3)
						},
						rewTPS(12500)),
				step("movies", 7, "07_tree_of_might_wasteland.json",
						earthReq(65, condBiome("dragonminez:rocky"), condSaga("saiyan_saga", 6)),
						new JsonObject[]{
								objBiome("dragonminez:rocky")
						},
						rewTPS(13400)),
				step("movies", 8, "08_turles_goku.json",
						earthReq(70, condBiome("dragonminez:rocky"), condSaga("saiyan_saga", 6)),
						new JsonObject[]{
								objKill("dragonminez:saga_goku_mid_base", 1, 3120, 182, 273, 2)
						},
						rewTPS(14400)),
				step("movies", 9, "09_turles_oozaru_gohan.json",
						earthReq(80, condBiome("dragonminez:rocky"), condSaga("saiyan_saga", 6)),
						new JsonObject[]{
								objKill("dragonminez:saga_ozaru", 1, 6500, 364, 364, 3)
						},
						rewTPS(15600)),
				step("movies", 10, "10_turles.json",
						earthReq(90, condBiome("dragonminez:rocky"), condSaga("saiyan_saga", 6)),
						new JsonObject[]{
								objKill("dragonminez:saga_turles", 1, 6500, 364, 364, 3)
						},
						rewTPS(16900)),
				step("movies", 11, "11_slug_soldiers.json",
						earthReq(95, condBiome("minecraft:plains"), condSaga("saiyan_saga", 6)),
						new JsonObject[]{
								objKill("dragonminez:saga_slug_soldier", 8, 1040, 66, 91, 1)
						},
						rewTPS(18100)),
				step("movies", 12, "12_slug.json",
						earthReq(100, condBiome("minecraft:plains"), condSaga("saiyan_saga", 6)),
						new JsonObject[]{
								objKill("dragonminez:saga_slug", 1, 3120, 182, 273, 2)
						},
						rewTPS(18800)),
				step("movies", 13, "13_giant_slug.json",
						earthReq(110, condBiome("minecraft:plains"), condSaga("saiyan_saga", 6)),
						new JsonObject[]{
								objKill("dragonminez:saga_slug_giant", 1, 6500, 364, 364, 3)
						},
						rewTPS(20600)),
				step("movies", 14, "14_cooler_armored_squadron.json",
						earthReq(150, condBiome("dragonminez:rocky"), condSaga("frieza_saga", 5)),
						new JsonObject[]{
								objKill("dragonminez:saga_neiz", 1, 5304, 309, 357, 1),
								objKill("dragonminez:saga_salza", 1, 6409, 377, 441, 1),
								objKill("dragonminez:saga_dore", 1, 7514, 421, 524, 1)
						},
						rewTPS(24400)),
				step("movies", 15, "15_cooler.json",
						earthReq(320, condBiome("dragonminez:rocky"), condSaga("frieza_saga", 14)),
						new JsonObject[]{
								objKill("dragonminez:saga_cooler", 1, 61880, 2873, 2499, 2)
						},
						rewTPS(58800)),
				step("movies", 16, "16_cooler_fifth_form.json",
						earthReq(360, condBiome("dragonminez:rocky"), condSaga("frieza_saga", 15)),
						new JsonObject[]{
								objKill("dragonminez:saga_cooler_5ta", 1, 75140, 3536, 2999, 3)
						},
						rewTPS(65000)),
				step("movies", 17, "17_big_gete_star.json",
						namekReq(370, condBiome("dragonminez:ajissa_plains"), condSaga("frieza_saga", 16)),
						new JsonObject[]{
								objKill("dragonminez:saga_gete_robot", 10, 3094, 200, 214, 1)
						},
						rewTPS(66900)),
				step("movies", 18, "18_metal_cooler.json",
						namekReq(380, condBiome("dragonminez:ajissa_plains"), condSaga("frieza_saga", 16)),
						new JsonObject[]{
								objKill("dragonminez:saga_metal_cooler", 1, 61880, 2873, 2499, 3)
						},
						rewTPS(68800)),
				step("movies", 19, "19_metal_cooler_core.json",
						namekReq(400, condBiome("dragonminez:ajissa_plains"), condSaga("frieza_saga", 16)),
						new JsonObject[]{
								objKill("dragonminez:saga_metal_cooler_core", 1, 75140, 3536, 2999, 3)
						},
						rewTPS(72500)),
				step("movies", 20, "20_androids_in_the_ice.json",
						earthReq(700, condBiome("minecraft:snowy_plains"), condSaga("android_saga", 6)),
						new JsonObject[]{
								objKill("dragonminez:saga_a14", 1, 154000, 6000, 5440, 2),
								objKill("dragonminez:saga_a15", 1, 129500, 5280, 4760, 2)
						},
						rewTPS(81200)),
				step("movies", 21, "21_android_13.json",
						earthReq(1080, condBiome("minecraft:snowy_plains"), condSaga("android_saga", 11)),
						new JsonObject[]{
								objKill("dragonminez:saga_a13", 1, 294000, 11400, 10540, 2)
						},
						rewTPS(110000)),
				step("movies", 22, "22_super_android_13.json",
						earthReq(1200, condBiome("minecraft:snowy_plains"), condSaga("android_saga", 13)),
						new JsonObject[]{
								objKill("dragonminez:saga_super_a13", 1, 434000, 17400, 15980, 3)
						},
						rewTPS(131200)),
				step("movies", 23, "23_broly_base.json",
						earthReq(1460, condBiome("dragonminez:rocky"), condSaga("android_saga", 15)),
						new JsonObject[]{
								objKill("dragonminez:saga_broly_base", 1, 434000, 17400, 15980, 2)
						},
						rewTPS(132500)),
				step("movies", 24, "24_paragus.json",
						earthReq(1465, condBiome("dragonminez:rocky"), condSaga("android_saga", 15)),
						new JsonObject[]{
								objKill("dragonminez:saga_paragus", 1, 217000, 8700, 7820, 1)
						},
						rewTPS(133800)),
				step("movies", 25, "25_legendary_broly.json",
						earthReq(1550, condBiome("dragonminez:rocky"), condSaga("android_saga", 15)),
						new JsonObject[]{
								objKill("dragonminez:saga_broly_lssj", 1, 574000, 23400, 21080, 3)
						},
						rewTPS(143800), rewSkill("legendaryforms", 1)),
				step("movies", 26, "26_bojack_allies.json",
						earthReq(1560, condBiome("minecraft:plains"), condSaga("android_saga", 15)),
						new JsonObject[]{
								objKill("dragonminez:saga_bujin", 1, 129500, 5280, 4760, 1),
								objKill("dragonminez:saga_bido", 1, 210000, 8400, 7480, 1),
								objKill("dragonminez:saga_zangya", 1, 154000, 6000, 5440, 1)
						},
						rewTPS(145000)),
				step("movies", 27, "27_gokua.json",
						earthReq(1570, condBiome("minecraft:plains"), condSaga("android_saga", 15)),
						new JsonObject[]{
								objKill("dragonminez:saga_gokua", 1, 217000, 8700, 7820, 2)
						},
						rewTPS(147500)),
				step("movies", 28, "28_bojack.json",
						earthReq(1600, condBiome("minecraft:plains"), condSaga("android_saga", 15)),
						new JsonObject[]{
								objKill("dragonminez:saga_bojack", 1, 434000, 17400, 15980, 2)
						},
						rewTPS(151200)),
				step("movies", 29, "29_full_power_bojack.json",
						earthReq(1650, condBiome("minecraft:plains"), condSaga("android_saga", 15)),
						new JsonObject[]{
								objKill("dragonminez:saga_bojack_fp", 1, 574000, 23400, 21080, 3)
						},
						rewTPS(158800)),
				step("movies", 30, "30_broly_second_coming.json",
						earthReq(2020, condBiome("minecraft:snowy_plains"), condSaga("buu_saga", 22)),
						new JsonObject[]{
								objKill("dragonminez:saga_broly_ssj", 1, 945000, 39600, 35700, 2)
						},
						rewTPS(306200)),
				step("movies", 31, "31_goten_and_trunks.json",
						earthReq(2030, condBiome("minecraft:snowy_plains"), condSaga("buu_saga", 22)),
						new JsonObject[]{
								objKill("dragonminez:saga_goten", 1, 350000, 14400, 13260, 1),
								objKill("dragonminez:saga_kid_trunks", 1, 378000, 15600, 14280, 1)
						},
						rewTPS(312500)),
				step("movies", 32, "32_legendary_broly_second_coming.json",
						earthReq(2240, condBiome("minecraft:snowy_plains"), condSaga("buu_saga", 30)),
						new JsonObject[]{
								objKill("dragonminez:saga_broly_lssj", 1, 1295000, 54600, 49300, 3)
						},
						rewTPS(375000)),
				step("movies", 33, "33_bio_broly.json",
						earthReq(2250, condBiome("minecraft:swamp"), condSaga("buu_saga", 30)),
						new JsonObject[]{
								objKill("dragonminez:saga_bio_broly", 1, 805000, 33600, 30600, 2)
						},
						rewTPS(381200)),
				step("movies", 34, "34_giant_bio_broly.json",
						earthReq(2350, condBiome("minecraft:swamp"), condSaga("buu_saga", 34)),
						new JsonObject[]{
								objKill("dragonminez:saga_bio_broly_giant", 1, 1120000, 46800, 42500, 3)
						},
						rewTPS(406200), rewSkill("legendaryforms", 2)),
				step("movies", 35, "35_otherworld_tournament.json",
						dimensionReq("dragonminez:otherworld", 2360, condSaga("buu_saga", 34)),
						new JsonObject[]{
								objKill("dragonminez:saga_paikuhan", 1, 805000, 33600, 30600, 2)
						},
						rewTPS(412500)),
				step("movies", 36, "36_janemba.json",
						dimensionReq("dragonminez:otherworld", 2380, condSaga("buu_saga", 34)),
						new JsonObject[]{
								objKill("dragonminez:saga_janemba_fat", 1, 1120000, 46800, 42500, 2)
						},
						rewTPS(425000)),
				step("movies", 37, "37_super_janemba.json",
						dimensionReq("dragonminez:otherworld", 2400, condSaga("buu_saga", 34)),
						new JsonObject[]{
								objKill("dragonminez:saga_super_janemba", 1, 1225000, 51600, 46920, 3)
						},
						rewTPS(437500), rewItem("dragonminez:dimensional_sword", 1)),
				step("movies", 38, "38_hildegarn_half.json",
						earthReq(2410, condBiome("minecraft:plains"), condSaga("buu_saga", 34)),
						new JsonObject[]{
								objKill("dragonminez:saga_hirudegarn_incomplete2", 1, 1225000, 51600, 46920, 2)
						},
						rewTPS(443800)),
				step("movies", 39, "39_hildegarn_complete.json",
						earthReq(2430, condBiome("minecraft:plains"), condSaga("buu_saga", 34)),
						new JsonObject[]{
								objKill("dragonminez:saga_hirudegarn", 1, 1295000, 54600, 49300, 3)
						},
						rewTPS(456200)),
				step("movies", 40, "40_super_hildegarn.json",
						earthReq(2460, condBiome("minecraft:plains"), condSaga("buu_saga", 34)),
						new JsonObject[]{
								objKill("dragonminez:saga_super_hirudegarn", 1, 1540000, 64800, 58480, 3)
						},
						rewTPS(475000), rewSkill("legendaryforms", 3))
		);
	}
}

