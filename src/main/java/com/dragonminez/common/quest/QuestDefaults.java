package com.dragonminez.common.quest;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.google.gson.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class QuestDefaults {

	private static final String PARTY_SCALING_KEY = "party_scaling";

	private static Path dmzBase;

	private QuestDefaults() {}

	static void createDefaultQuestFiles(Path questsDir) {
		if (!ConfigManager.getServerConfig().getGameplay().getStoryModeEnabled()) return;
		if (!ConfigManager.getServerConfig().getGameplay().getCreateDefaultSagas()) return;

		dmzBase = questsDir.getParent();

		createClassicSagaQuests(questsDir);
		createSaiyanSagaQuests(questsDir);
		createFriezaSagaQuests(questsDir);
		createAndroidSagaQuests(questsDir);
		createFutureSagaQuests(questsDir);
		createBuuSagaQuests(questsDir);
		createMoviesSagaQuests(questsDir);
		createGTSagaQuests(questsDir);
		createDaimaSagaQuests(questsDir);
	}

	private static void writeQuest(Path dir, String filename, JsonObject quest) {
		try {
			Files.createDirectories(dir);
			QuestUpgrader.upgradeOrWrite(dmzBase, dir.resolve(filename), quest);
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Failed to create default quest file: {}", filename, e);
		}
	}

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

	private static JsonObject objKill(String entity, int count, double hp, double melee, double ki, int textureVariant) {
		JsonObject o = objKill(entity, count, hp, melee, ki);
		o.addProperty("TextureVariant", textureVariant);
		return o;
	}

	private static JsonObject objSpar(String entity, double hp, double melee, double ki) {
		JsonObject o = objKill(entity, 1, hp, melee, ki);
		o.addProperty("type", "SPAR");
		return o;
	}

	private static JsonObject objWaves(String entity, int waves, int mobsPerWave, double hp, double melee, double ki) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "SURVIVE_WAVES");
		o.addProperty("entity", entity);
		o.addProperty("waves", waves);
		o.addProperty("mobs_per_wave", mobsPerWave);
		o.addProperty("wave_delay_seconds", 8);
		o.addProperty("health", hp);
		o.addProperty("meleeDamage", melee);
		o.addProperty("kiDamage", ki);
		return o;
	}

	/** Keeps a quest-spawned enemy in the form it spawned in, for fights that reuse a transforming entity. */
	private static JsonObject noTransform(JsonObject killObjective) {
		killObjective.addProperty("canTransform", false);
		return killObjective;
	}

	/**
	 * Authors the form a quest enemy transforms into instead of leaving it to the entity transform
	 * defaults (1.5x health and damage). Every stage after the first reads these same values, so a
	 * multi-stage fight keeps the saga's intended power curve even if a server retunes those defaults.
	 */
	private static JsonObject transformStats(JsonObject killObjective, double hp, double melee, double ki) {
		killObjective.addProperty("TransformHealth", hp);
		killObjective.addProperty("TransformMeleeDamage", melee);
		killObjective.addProperty("TransformKiDamage", ki);
		return killObjective;
	}

	private static JsonObject objItem(String itemId, int count) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "ITEM");
		o.addProperty("item", itemId);
		o.addProperty("count", count);
		return o;
	}

	private static JsonObject objDeliver(String itemId, int count, String npcId) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "DELIVER");
		o.addProperty("item", itemId);
		o.addProperty("count", count);
		o.addProperty("npcId", npcId);
		return o;
	}

	private static JsonObject objTalkTo(String targetNpcId) {
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

	private static JsonObject objDragonSummon(String dragonId, String ballSetId) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "DRAGON_SUMMON");
		o.addProperty("dragon", dragonId);
		o.addProperty("ball_set", ballSetId);
		return o;
	}

	private static JsonObject rewAttributes(int amount) {
		JsonObject r = new JsonObject();
		r.addProperty("type", "ATTRIBUTES");
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

	private static JsonObject condAny(JsonObject... conditions) {
		return prereqs("OR", conditions);
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

	private static JsonObject dimensionReq(String dimensionId, JsonObject... extraConditions) {
		JsonObject[] conditions = new JsonObject[extraConditions.length + 1];
		conditions[0] = condDimension(dimensionId);
		System.arraycopy(extraConditions, 0, conditions, 1, extraConditions.length);
		return requirements("AND", conditions);
	}

	private static JsonObject earthReq(JsonObject... extraConditions) {
		return dimensionReq("minecraft:overworld", extraConditions);
	}

	private static JsonObject namekReq(JsonObject... extraConditions) {
		return dimensionReq("dragonminez:namek", extraConditions);
	}

	private static JsonObject sacredKaiReq(JsonObject... extraConditions) {
		return dimensionReq("dragonminez:sacredkaiplanet", extraConditions);
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
	// Classic Saga Quests (folder: saga_classic)
	// ========================================================================================

	private static final int KID_GOKU_TURTLE_GI = 0;
	private static final int KID_GOKU_BLUE_GI = 1;
	private static final int KID_GOKU_KING_PICCOLO = 2;
	private static final int KID_KRILLIN_ORIN = 0;
	private static final int KID_KRILLIN_TURTLE_GI = 1;

	private static JsonObject atVillage() {
		return condAny(
				condStructure("minecraft:village_plains"),
				condStructure("minecraft:village_desert"),
				condStructure("minecraft:village_savanna"),
				condStructure("minecraft:village_snowy"),
				condStructure("minecraft:village_taiga"));
	}

	private static JsonObject nearWater() {
		return condAny(condBiome("#minecraft:is_river"), condBiome("#minecraft:is_beach"), condBiome("#minecraft:is_ocean"));
	}

	private static JsonObject onIsland() {
		return condAny(condStructure("dragonminez:roshi_house"), condBiome("#minecraft:is_beach"));
	}

	private static JsonObject inCave() {
		return condAny(condBiome("minecraft:dripstone_caves"), condBiome("minecraft:lush_caves"));
	}

	private static JsonObject inSnow() {
		return condBiome("#forge:is_snowy");
	}

	private static JsonObject atTournament() {
		return condBiome("#dragonminez:is_plains");
	}

	private static JsonObject atRedRibbonBase() {
		return condStructure("dragonminez:rrtower");
	}

	private static JsonObject atBabaPalace() {
		return condStructure("dragonminez:baba_palace");
	}

	private static void createClassicSagaQuests(Path questsDir) {
		writeSaga(questsDir.resolve("saga_classic"), "classic_saga", "saga_classic", null,
				// --- Pilaf Saga ---
				step("classic", 1, "01_giant_fish.json",
						earthReq(nearWater()),
						new JsonObject[]{ objKill("dragonminez:giant_fish", 1, 100, 4, 0) },
						rewAttributes(5), rewItem("dragonminez:giant_fish_cooked", 2)),
				step("classic", 2, "02_oolong_transformed.json",
						earthReq(atVillage()),
						new JsonObject[]{ objKill("dragonminez:saga_oolong_transformed", 1, 160, 6, 0) },
						rewAttributes(6)),
				step("classic", 3, "03_oolong.json",
						earthReq(atVillage()),
						new JsonObject[]{ objKill("dragonminez:saga_oolong", 1, 130, 5, 0) },
						rewAttributes(6)),
				step("classic", 4, "04_bandit_yamcha.json",
						earthReq(condBiome("minecraft:desert")),
						new JsonObject[]{ objKill("dragonminez:saga_teen_yamcha", 1, 260, 10, 8) },
						rewAttributes(7)),
				step("classic", 5, "05_oozaru_goku.json",
						earthReq(condBiome("minecraft:desert")),
						new JsonObject[]{ objKill("dragonminez:saga_ozaru", 1, 480, 16, 0) },
						rewAttributes(8)),

				// --- 21st World Martial Arts Tournament ---
				step("classic", 6, "06_master_roshi.json",
						earthReq(),
						new JsonObject[]{ objStructure("dragonminez:roshi_house"), objTalkTo("roshi") },
						rewAttributes(2)),
				step("classic", 7, "07_milk_delivery.json",
						earthReq(),
						new JsonObject[]{ objDeliver("minecraft:milk_bucket", 3, "roshi") },
						rewAttributes(2)),
				step("classic", 8, "08_kame_house_sparring.json",
						earthReq(onIsland()),
						new JsonObject[]{
								objKill("dragonminez:saga_kid_goku", 1, 340, 10, 8, KID_GOKU_BLUE_GI),
								objKill("dragonminez:saga_kid_krillin", 1, 310, 9, 6, KID_KRILLIN_ORIN)
						}, rewAttributes(8)),
				step("classic", 9, "09_tournament_giran.json",
						earthReq(atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_giran", 1, 440, 19, 5) },
						rewAttributes(9)),
				step("classic", 10, "10_tournament_nam.json",
						earthReq(atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_nam", 1, 480, 22, 8) },
						rewAttributes(9)),
				step("classic", 11, "11_jackie_chun.json",
						earthReq(atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_jackie_chun", 1, 600, 28, 25) },
						rewAttributes(10)),
				step("classic", 12, "12_jackie_chun_full_power.json",
						earthReq(atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_jackie_chun_fp", 1, 800, 34, 30) },
						rewAttributes(10)),

				// --- Red Ribbon Army Saga ---
				step("classic", 13, "13_colonel_silver.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{ objKill("dragonminez:saga_colonel_silver", 1, 560, 26, 10) },
						rewAttributes(10)),
				step("classic", 14, "14_sergeant_metallic.json",
						earthReq(inSnow()),
						new JsonObject[]{ objKill("dragonminez:saga_sergeant_metallic", 1, 740, 36, 0) },
						rewAttributes(10)),
				step("classic", 15, "15_ninja_murasaki.json",
						earthReq(inSnow()),
						new JsonObject[]{ objKill("dragonminez:saga_ninja_murasaki", 1, 640, 31, 12) },
						rewAttributes(10)),
				step("classic", 16, "16_android_8.json",
						earthReq(inSnow()),
						new JsonObject[]{ objKill("dragonminez:saga_a8", 1, 850, 40, 0) },
						rewAttributes(11)),
				step("classic", 17, "17_general_blue.json",
						earthReq(inCave()),
						new JsonObject[]{ objKill("dragonminez:saga_general_blue", 1, 820, 38, 20) },
						rewAttributes(11)),
				step("classic", 18, "18_tao_pai_pai_strikes.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{ objKill("dragonminez:saga_kid_goku", 1, 700, 34, 30, KID_GOKU_TURTLE_GI) },
						rewAttributes(11)),
				step("classic", 19, "19_sacred_water.json",
						earthReq(),
						new JsonObject[]{ objStructure("dragonminez:kamilookout"), objDeliver("minecraft:water_bucket", 1, "karin") },
						rewAttributes(2), rewItem("dragonminez:sacred_water", 1), rewItem("dragonminez:red_capsule", 1), rewItem("dragonminez:green_capsule", 1),
						rewItem("dragonminez:blue_capsule", 1)),
				step("classic", 20, "20_tao_pai_pai_rematch.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{ objKill("dragonminez:saga_tao_pai_pai", 1, 950, 48, 36) },
						rewAttributes(12)),
				step("classic", 21, "21_general_red.json",
						earthReq(atRedRibbonBase()),
						new JsonObject[]{ objKill("dragonminez:saga_general_red", 1, 400, 14, 0) },
						rewAttributes(11)),
				step("classic", 22, "22_officer_black_robot.json",
						earthReq(atRedRibbonBase()),
						new JsonObject[]{ objKill("dragonminez:saga_general_black_robot", 1, 1100, 54, 45) },
						rewAttributes(13)),

				// --- Fortuneteller Baba ---
				step("classic", 23, "23_fortuneteller_baba.json",
						earthReq(),
						new JsonObject[]{ objStructure("dragonminez:baba_palace"), objTalkTo("baba_earth") },
						rewAttributes(2)),
				step("classic", 24, "24_baba_dracula.json",
						earthReq(atBabaPalace()),
						new JsonObject[]{ objKill("dragonminez:saga_dracula", 1, 720, 35, 0) },
						rewAttributes(11)),
				step("classic", 25, "25_baba_invisible_man.json",
						earthReq(atBabaPalace()),
						new JsonObject[]{ objKill("dragonminez:saga_invisible_man", 1, 740, 36, 0) },
						rewAttributes(11)),
				step("classic", 26, "26_baba_mummy.json",
						earthReq(atBabaPalace()),
						new JsonObject[]{ objKill("dragonminez:saga_mummy", 1, 900, 45, 0) },
						rewAttributes(12)),
				step("classic", 27, "27_baba_akkuman.json",
						earthReq(atBabaPalace()),
						new JsonObject[]{ objKill("dragonminez:saga_akkuman", 1, 1000, 50, 40) },
						rewAttributes(13)),
				step("classic", 28, "28_baba_masked_warrior.json",
						earthReq(atBabaPalace()),
						new JsonObject[]{ objKill("dragonminez:saga_masked_warrior", 1, 1080, 56, 45) },
						rewAttributes(13)),
				step("classic", 29, "29_baba_kid_goku.json",
						earthReq(atBabaPalace()),
						new JsonObject[]{ objKill("dragonminez:saga_kid_goku", 1, 1150, 60, 50, KID_GOKU_TURTLE_GI) },
						rewAttributes(14)),

				// --- 22nd World Martial Arts Tournament ---
				step("classic", 30, "30_heavy_shell_training.json",
						earthReq(),
						new JsonObject[]{ objDeliver("minecraft:scute", 1, "roshi") },
						rewAttributes(2)),
				step("classic", 31, "31_22nd_tournament.json",
						earthReq(),
						new JsonObject[]{ objBiome("#dragonminez:is_plains") },
						rewAttributes(2)),
				step("classic", 32, "32_tournament_yamcha.json",
						earthReq(atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_young_yamcha", 1, 1100, 58, 50) },
						rewAttributes(13)),
				step("classic", 33, "33_tournament_chiaotzu.json",
						earthReq(atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_chaoz", 1, 960, 46, 60) },
						rewAttributes(13)),
				step("classic", 34, "34_tournament_krillin.json",
						earthReq(atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_kid_krillin", 1, 1150, 62, 55, KID_KRILLIN_TURTLE_GI) },
						rewAttributes(14)),
				step("classic", 35, "35_tournament_tien.json",
						earthReq(atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_young_tien", 1, 1300, 72, 65) },
						rewAttributes(14)),
				step("classic", 36, "36_tournament_goku.json",
						earthReq(atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_kid_goku", 1, 1350, 75, 70, KID_GOKU_TURTLE_GI) },
						rewAttributes(15)),

				// --- King Piccolo Saga ---
				step("classic", 37, "37_krillin.json",
						earthReq(),
						new JsonObject[]{ objKill("dragonminez:saga_kid_krillin", 1, 1200, 64, 55, KID_KRILLIN_TURTLE_GI) },
						rewAttributes(14)),
				step("classic", 38, "38_yajirobe.json",
						earthReq(),
						new JsonObject[]{ objKill("dragonminez:saga_yajirobe", 1, 1250, 70, 10) },
						rewAttributes(14)),
				step("classic", 39, "39_tambourine.json",
						earthReq(),
						new JsonObject[]{ objKill("dragonminez:saga_tambourine", 1, 1300, 72, 60) },
						rewAttributes(15)),
				step("classic", 40, "40_king_piccolo_old.json",
						earthReq(),
						new JsonObject[]{ objKill("dragonminez:saga_piccolo_daimao_old", 1, 1400, 76, 70) },
						rewAttributes(15)),
				step("classic", 41, "41_goku_strikes_back.json",
						earthReq(),
						new JsonObject[]{ objKill("dragonminez:saga_kid_goku", 1, 1400, 78, 75, KID_GOKU_KING_PICCOLO) },
						rewAttributes(15)),
				step("classic", 42, "42_pilaf_gang_robots.json",
						earthReq(),
						new JsonObject[]{
								objKill("dragonminez:saga_pilaf_robot", 1, 700, 25, 12),
								objKill("dragonminez:saga_shu_robot", 1, 720, 26, 12),
								objKill("dragonminez:saga_mai_robot", 1, 850, 29, 16)
						}, rewAttributes(15)),
				step("classic", 43, "43_fused_pilaf_robot.json",
						earthReq(),
						new JsonObject[]{ objKill("dragonminez:saga_pilaf_robot_fused", 1, 1500, 80, 40) },
						rewAttributes(16)),
				step("classic", 44, "44_drum.json",
						earthReq(),
						new JsonObject[]{ objKill("dragonminez:saga_drum", 1, 1500, 82, 30) },
						rewAttributes(16)),
				step("classic", 45, "45_king_piccolo_young.json",
						earthReq(),
						new JsonObject[]{ objKill("dragonminez:saga_piccolo_daimao_young", 1, 1600, 88, 90) },
						rewAttributes(18), rewItem("dragonminez:senzu_bean", 2)),

				// --- 23rd World Martial Arts Tournament ---
				step("classic", 46, "46_lookout_training.json",
						earthReq(),
						new JsonObject[]{ objStructure("dragonminez:kamilookout"), objDeliver("minecraft:emerald", 5, "popo") },
						rewAttributes(4)),
				step("classic", 47, "47_cyborg_tao_pai_pai.json",
						earthReq(atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_tao_pai_pai_cyborg", 1, 1450, 78, 80) },
						rewAttributes(17)),
				step("classic", 48, "48_tournament_chichi.json",
						earthReq(atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_chichi", 1, 1400, 74, 20) },
						rewAttributes(17)),
				step("classic", 49, "49_tournament_tien.json",
						earthReq(atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_tien_t23", 1, 1600, 86, 90) },
						rewAttributes(18)),
				step("classic", 50, "50_tournament_piccolo.json",
						earthReq(atTournament()),
						new JsonObject[]{ noTransform(objKill("dragonminez:saga_majunia", 1, 1650, 90, 130)) },
						rewAttributes(19)),
				step("classic", 51, "51_giant_piccolo.json",
						earthReq(atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_majunia_giant", 1, 1800, 95, 140) },
						rewAttributes(20)),
				step("classic", 52, "52_tournament_finale.json",
						earthReq(),
						new JsonObject[]{ objStructure("dragonminez:goku_house"), objTalkTo("goku") },
						rewAttributes(2), rewItem("dragonminez:senzu_bean", 3))
		);
	}

	// ========================================================================================
	// Saiyan Saga Quests (folder: saga_saiyan)
	// ========================================================================================

	private static void createSaiyanSagaQuests(Path questsDir) {
		JsonObject prevClassic = prevQuest("classic_saga", 52);
		writeSaga(questsDir.resolve("saga_saiyan"), "saiyan_saga", "saga_saiyan", prevClassic,
				step("saiyan", 1, "01_defeat_raditz.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_raditz", 1, 1800, 90, 140)
						},
						rewAttributes(20)),
				step("saiyan", 2, "02_piccolo_takes_gohan.json",
						earthReq(),
						new JsonObject[]{
								objStructure("dragonminez:piccolo_house"),
								objTalkTo("piccolo")
						},
						rewAttributes(2)),
				step("saiyan", 3, "03_survive_wilderness_training.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:dino1", 1, 2200, 40, 0)
						},
						rewAttributes(20), rewItem("dragonminez:cooked_dino_meat", 8)),
				step("saiyan", 4, "04_gohan_oozaru.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_ozaru", 1, 3200, 95, 0)
						},
						rewAttributes(25)),
				step("saiyan", 5, "05_spar_with_gohan.json",
						earthReq(),
						new JsonObject[]{
								objKill("dragonminez:saga_kid_gohan", 1, 2000, 80, 130)
						},
						rewAttributes(25)),
				step("saiyan", 6, "06_lookout_provisions.json",
						earthReq(),
						new JsonObject[]{
								objStructure("dragonminez:kamilookout"),
								objDeliver("dragonminez:cooked_dino_meat", 6, "popo")
						},
						rewAttributes(3)),
				step("saiyan", 7, "07_lookout_sparring.json",
						earthReq(condStructure("dragonminez:kamilookout")),
						new JsonObject[]{
								objKill("dragonminez:saga_krillin", 1, 1700, 36, 57),
								objKill("dragonminez:saga_yamcha", 1, 1700, 39, 52),
								objKill("dragonminez:saga_tien_early", 1, 1900, 45, 63)
						},
						rewAttributes(25)),
				step("saiyan", 8, "08_kill_the_saibamans.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("#dragonminez:saibamen", 6, 1800, 20, 27)
						},
						rewAttributes(25)),
				step("saiyan", 9, "09_hold_against_nappa.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_nappa", 1, 2800, 120, 160)
						}, rewAttributes(25)),
				step("saiyan", 10, "10_senzu_from_korin.json",
						earthReq(),
						new JsonObject[]{
								objStructure("dragonminez:kamilookout"),
								objTalkTo("karin")
						},
						rewAttributes(2), rewItem("dragonminez:senzu_bean", 3)),
				step("saiyan", 11, "11_face_vegeta.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_vegeta", 1, 3800, 150, 200)
						},
						rewAttributes(25)),
				step("saiyan", 12, "12_defeat_oozaru_vegeta.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_ozaruvegeta", 1, 7500, 250, 250)
						},
						rewAttributes(30)),
				step("saiyan", 13, "13_kame_house_recovery.json",
						earthReq(),
						new JsonObject[]{
								objStructure("dragonminez:roshi_house"),
								objDeliver("minecraft:golden_carrot", 12, "krillin")
						},
						rewAttributes(3)),
				step("saiyan", 14, "14_repair_the_saiyan_pod.json",
						earthReq(),
						new JsonObject[]{
								objStructure("dragonminez:vegeta_pod"),
								objDeliver("minecraft:iron_block", 8, "bulma"),
								objDeliver("minecraft:redstone_block", 4, "bulma"),
								objDeliver("minecraft:diamond", 4, "bulma")
						},
						rewAttributes(13)),
				step("saiyan", 15, "15_prepare_for_namek.json",
						earthReq(condRealTimeMinutes(5)),
						new JsonObject[]{
								objTalkTo("bulma")
						},
						rewAttributes(2), rewItem("dragonminez:saiyan_ship", 1)),
				step("saiyan", 16, "16_head_to_namek.json",
						earthReq(),
						new JsonObject[]{
								objDimension("dragonminez:namek")
						},
						rewAttributes(2))
		);
	}

	// ========================================================================================
	// Frieza Saga Quests (folder: saga_frieza)
	// ========================================================================================

	private static void createFriezaSagaQuests(Path questsDir) {
		JsonObject prevSaiyan = prevQuest("saiyan_saga", 16);
		writeSaga(questsDir.resolve("saga_frieza"), "frieza_saga", "saga_frieza", prevSaiyan,
				step("frieza", 1, "01_secure_namek_landing.json",
						namekReq(condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("#dragonminez:frieza_soldiers", 8, 4500, 23, 25)
						},
						rewAttributes(30)),
				step("frieza", 17, "01b_scout_the_ruined_village.json",
						namekReq(),
						new JsonObject[]{
								objStructure("dragonminez:namek_ruins")
						},
						rewAttributes(2)),
				step("frieza", 2, "02_defeat_cui.json",
						namekReq(condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cui", 1, 5500, 180, 200)
						},
						rewAttributes(35)),
				step("frieza", 3, "03_defend_the_namekians.json",
						namekReq(condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("#dragonminez:frieza_soldiers", 16, 5000, 14, 16)
						},
						rewAttributes(35)),
				step("frieza", 4, "04_defeat_dodoria.json",
						namekReq(condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_dodoria", 1, 7000, 220, 250)
						},
						rewAttributes(35)),
				step("frieza", 5, "05_defeat_zarbon.json",
						namekReq(condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								transformStats(objKill("dragonminez:saga_zarbon", 1, 6800, 250, 280), 10200, 290, 320)
						},
						rewAttributes(40)),
				step("frieza", 18, "05b_journey_to_guru.json",
						namekReq(),
						new JsonObject[]{
								objStructure("dragonminez:elder_guru"),
								objTalkTo("guru")
						},
						rewAttributes(3)),
				step("frieza", 6, "06_the_saiyan_prince.json",
						namekReq(condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_vegeta_namek", 1, 9000, 260, 290)
						},
						rewAttributes(40)),
				step("frieza", 19, "06b_raid_friezas_ship.json",
						namekReq(),
						new JsonObject[]{
								objStructure("dragonminez:frieza_ship"),
								objKill("#dragonminez:frieza_soldiers", 10, 6000, 18, 18)
						},
						rewAttributes(40)),
				step("frieza", 7, "07_defeat_guldo.json",
						namekReq(condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_guldo", 1, 6000, 180, 180)
						},
						rewAttributes(40)),
				step("frieza", 8, "08_defeat_recoome.json",
						namekReq(condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_recoome", 1, 10000, 300, 280)
						},
						rewAttributes(45)),
				step("frieza", 9, "09_defeat_burter_and_jeice.json",
						namekReq(condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_burter", 1, 10000, 200, 175),
								objKill("dragonminez:saga_jeice", 1, 10000, 200, 175)
						},
						rewAttributes(45)),
				step("frieza", 10, "10_defeat_ginyu.json",
						namekReq(condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_ginyu", 1, 14000, 400, 350)
						},
						rewAttributes(45)),
				step("frieza", 11, "11_defeat_ginyu_goku.json",
						namekReq(condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_ginyu_goku", 1, 9000, 260, 220)
						},
						rewAttributes(45)),
				step("frieza", 20, "11b_guard_the_medical_machine.json",
						namekReq(condStructure("dragonminez:frieza_ship")),
						new JsonObject[]{
								objWaves("dragonminez:saga_friezasoldier01", 3, 4, 6500, 65, 55)
						},
						rewAttributes(45)),
				step("frieza", 21, "11c_spar_with_nail.json",
						namekReq(condStructure("dragonminez:elder_guru")),
						new JsonObject[]{
								objSpar("dragonminez:saga_nail", 15000, 380, 340)
						},
						rewAttributes(50)),
				step("frieza", 22, "11d_summon_porunga.json",
						namekReq(),
						new JsonObject[]{
								objDragonSummon("porunga", "namek")
						},
						rewAttributes(25)),
				step("frieza", 12, "12_defeat_frieza_first.json",
						namekReq(condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_frieza_first", 1, 20000, 500, 450)
						},
						rewAttributes(50)),
				step("frieza", 13, "13_defeat_frieza_third.json",
						namekReq(condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								transformStats(objKill("dragonminez:saga_frieza_second", 1, 24000, 770, 680), 36000, 885, 780)
						},
						rewAttributes(55)),
				step("frieza", 14, "14_defeat_frieza_base.json",
						namekReq(condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_frieza_base", 1, 40000, 1180, 1025)
						},
						rewAttributes(55)),
				step("frieza", 15, "15_defeat_frieza_full_power.json",
						namekReq(condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_frieza_fp", 1, 50000, 1800, 1550)
						},
						rewAttributes(60)),
				step("frieza", 16, "16_escape_namek_before_collapse.json",
						namekReq(),
						new JsonObject[]{
								objDimension("minecraft:overworld")
						},
						rewAttributes(5))
		);
	}

	// ========================================================================================
	// Android Saga Quests (folder: saga_android)
	// ========================================================================================

	private static void createAndroidSagaQuests(Path questsDir) {
		JsonObject prevFrieza = prevQuest("frieza_saga", 16);

		writeSaga(questsDir.resolve("saga_android"), "android_saga", "saga_android", prevFrieza,
				step("android", 1, "01_defeat_mecha_frieza.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_mecha_frieza", 1, 58500, 2250, 2070)
						},
						rewAttributes(60)),
				step("android", 2, "02_defeat_king_cold.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_king_cold", 1, 36000, 1260, 1170)
						},
						rewAttributes(60)),
				step("android", 16, "02b_the_time_machine.json",
						earthReq(),
						new JsonObject[]{
								objStructure("dragonminez:trunks_ship")
						},
						rewAttributes(5)),
				step("android", 3, "03_warning_from_the_future.json",
						earthReq(),
						new JsonObject[]{
								objTalkTo("trunks")
						},
						rewAttributes(4)),
				step("android", 4, "04_three_year_training.json",
						earthReq(condRealTimeMinutes(15)),
						new JsonObject[]{
								objKill("dragonminez:shadow_dummy", 16, 5670, 175, 158)
						},
						rewAttributes(60)),
				step("android", 17, "04b_heart_virus_medicine.json",
						earthReq(),
						new JsonObject[]{
								objStructure("dragonminez:goku_house"),
								objDeliver("minecraft:glistering_melon_slice", 8, "goku")
						},
						rewAttributes(6)),
				step("android", 5, "05_defeat_a19.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_a19", 1, 72000, 2790, 2520)
						},
						rewAttributes(65)),
				step("android", 6, "06_defeat_drgero.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_drgero", 1, 63000, 2430, 2160)
						},
						rewAttributes(65)),
				step("android", 18, "06b_geros_blueprints.json",
						earthReq(),
						new JsonObject[]{
								objStructure("dragonminez:gero_lab"),
								objItem("minecraft:paper", 16),
								objTalkTo("bulma")
						},
						rewAttributes(7)),
				step("android", 7, "07_track_android_signal.json",
						earthReq(),
						new JsonObject[]{
								objBiome("#minecraft:is_mountain")
						},
						rewAttributes(5)),
				step("android", 8, "08_defeat_a18.json",
						earthReq(condBiome("#minecraft:is_mountain")),
						new JsonObject[]{
								objKill("dragonminez:saga_a18", 1, 99000, 3870, 3420)
						},
						rewAttributes(65)),
				step("android", 9, "09_defeat_a17.json",
						earthReq(condBiome("#minecraft:is_mountain")),
						new JsonObject[]{
								objKill("dragonminez:saga_a17", 1, 112500, 4680, 4950)
						},
						rewAttributes(65)),
				step("android", 19, "09b_android_16.json",
						earthReq(condBiome("#minecraft:is_mountain")),
						new JsonObject[]{
								objSpar("dragonminez:saga_a16", 104000, 4300, 3900)
						},
						rewAttributes(65)),
				step("android", 10, "10_defeat_cell_imperfect.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_imperfect", 1, 99000, 3960, 3600)
						},
						rewAttributes(70)),
				step("android", 11, "11_defeat_cell_semiperfect.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_semiperfect", 1, 120000, 4640, 4240)
						},
						rewAttributes(70)),
				step("android", 20, "11b_cell_games_announced.json",
						earthReq(),
						new JsonObject[]{
								objStructure("dragonminez:cell_arena")
						},
						rewAttributes(6)),
				step("android", 12, "12_beyond_super_saiyan.json",
						earthReq(condRealTimeMinutes(10)),
						new JsonObject[]{
								objKill("dragonminez:shadow_dummy", 20, 5540, 302, 282)
						},
						rewAttributes(75)),
				step("android", 21, "12b_a_new_guardian.json",
						earthReq(),
						new JsonObject[]{
								objStructure("dragonminez:kamilookout"),
								objTalkTo("dende")
						},
						rewAttributes(6)),
				step("android", 22, "12c_father_and_son_spar.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objSpar("dragonminez:saga_goku_mid_ssj", 140000, 5600, 5100)
						},
						rewAttributes(75)),
				step("android", 13, "13_defeat_cell_perfect.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_perfect", 1, 156240, 6260, 5750)
						},
						rewAttributes(75)),
				step("android", 14, "14_defeat_cell_jrs.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_jr", 7, 108360, 1200, 1080)
						},
						rewAttributes(75)),
				step("android", 15, "15_defeat_cell_superperfect.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_superperfect", 1, 206640, 8420, 7590)
						},
						rewAttributes(80))
		);
	}

	// ========================================================================================
	// Future Saga Quests (folder: saga_future)
	// ========================================================================================

	private static void createFutureSagaQuests(Path questsDir) {
		JsonObject prevAndroid = prevQuest("android_saga", 15);

		writeSaga(questsDir.resolve("saga_future"), "future_saga", "saga_future", prevAndroid,
				step("future", 1, "01_talk_to_future_trunks.json",
						earthReq(),
						new JsonObject[]{
								objTalkTo("trunks")
						},
						rewAttributes(4)),
				step("future", 2, "02_train_with_trunks_and_gohan.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								transformStats(objKill("dragonminez:saga_ftrunks_base", 1, 65520, 4050, 3670), 98280, 4650, 4220),
								noTransform(objKill("dragonminez:saga_fgohan_base", 1, 141120, 4370, 3950))
						},
						rewAttributes(65)),
				step("future", 3, "03_androids_ruined_plains.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_a17", 1, 156240, 4280, 3880),
								objKill("dragonminez:saga_a18", 1, 151200, 4140, 3720)
						},
						rewAttributes(70)),
				step("future", 4, "04_face_future_gohan.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_fgohan_ssj", 1, 171360, 6910, 6360)
						},
						rewAttributes(75)),
				step("future", 5, "05_androids_in_the_mountains.json",
						earthReq(condBiome("#minecraft:is_mountain")),
						new JsonObject[]{
								objKill("dragonminez:saga_a18", 1, 171360, 3880, 3530),
								objKill("dragonminez:saga_a17", 1, 178920, 4110, 3730)
						},
						rewAttributes(80)),
				step("future", 6, "06_imperfect_cell_of_the_future.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_imperfect", 1, 191520, 7990, 7220)
						},
						rewAttributes(85)),
				step("future", 7, "07_future_restored.json",
						earthReq(),
						new JsonObject[]{
								objTalkTo("trunks")
						},
						rewAttributes(5))
		);
	}

	// ========================================================================================
	// Buu Saga Quests (folder: saga_buu)
	// ========================================================================================

	private static void createBuuSagaQuests(Path questsDir) {
		JsonObject prevAndroid = prevQuest("android_saga", 15);

		writeSaga(questsDir.resolve("saga_buu"), "buu_saga", "saga_buu", prevAndroid,
				step("buu", 1, "01_train_with_goten_and_gohan.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								transformStats(objKill("dragonminez:saga_goten", 1, 78750, 4680, 4310), 117900, 5380, 4950),
								transformStats(objKill("dragonminez:saga_gohan_end_base", 1, 71100, 5850, 5300), 106650, 6730, 6090)
						},
						rewAttributes(90)),
				step("buu", 36, "01b_flying_lessons.json",
						earthReq(),
						new JsonObject[]{
								objSkill("fly", 1),
								objTalkTo("gohan")
						},
						rewAttributes(14)),
				step("buu", 2, "02_assemble_gravity_device_parts.json",
						earthReq(),
						new JsonObject[]{
								objItem("dragonminez:kikono_station", 1),
								objItem("dragonminez:fuel_generator", 1),
								objItem("dragonminez:energy_cable", 8)
						},
						rewAttributes(11)),
				step("buu", 3, "03_train_with_trunks_and_vegeta.json",
						earthReq(condRealTimeMinutes(10)),
						new JsonObject[]{
								transformStats(objKill("dragonminez:saga_kid_trunks", 1, 85050, 4560, 4180), 127800, 5250, 4800),
								transformStats(objKill("dragonminez:saga_vegeta_end_base", 1, 80100, 5970, 5470), 120150, 6860, 6290)
						},
						rewAttributes(95)),
				step("buu", 4, "04_enter_the_world_tournament.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objTalkTo("piccolo")
						},
						rewAttributes(6)),
				step("buu", 37, "04b_tournament_preliminaries.json",
						earthReq(atTournament()),
						new JsonObject[]{
								objSpar("dragonminez:saga_videl", 120000, 6500, 5850)
						},
						rewAttributes(60)),
				step("buu", 5, "05_tournament_goten.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								transformStats(objKill("dragonminez:saga_goten", 1, 88200, 7290, 6580), 132300, 8390, 7570)
						},
						rewAttributes(75)),
				step("buu", 6, "06_tournament_trunks.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								transformStats(objKill("dragonminez:saga_kid_trunks", 1, 94500, 7830, 7040), 141750, 9000, 8090)
						},
						rewAttributes(75)),
				step("buu", 7, "07_tournament_krillin.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_krillin", 1, 195300, 8100, 7340)
						},
						rewAttributes(75)),
				step("buu", 8, "08_tournament_shin.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_shin", 1, 207900, 8640, 7800)
						},
						rewAttributes(80)),
				step("buu", 9, "09_tournament_spopovich.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_spopovitch", 1, 214200, 8910, 7960)
						},
						rewAttributes(80)),
				step("buu", 10, "10_find_babidi_ship.json",
						earthReq(),
						new JsonObject[]{
								objStructure("dragonminez:babidi")
						},
						rewAttributes(8)),
				step("buu", 11, "11_babidi_level_pui_pui.json",
						earthReq(condStructure("dragonminez:babidi")),
						new JsonObject[]{
								objKill("dragonminez:saga_puipui", 1, 226800, 9450, 8570)
						},
						rewAttributes(85)),
				step("buu", 12, "12_babidi_level_yakon.json",
						earthReq(condStructure("dragonminez:babidi")),
						new JsonObject[]{
								objKill("dragonminez:saga_yakon", 1, 245700, 10260, 9330)
						},
						rewAttributes(85)),
				step("buu", 13, "13_babidi_level_dabura.json",
						earthReq(condStructure("dragonminez:babidi")),
						new JsonObject[]{
								objKill("dragonminez:saga_dabura", 1, 277200, 11610, 10560)
						},
						rewAttributes(90)),
				step("buu", 14, "14_fat_buu_awakes.json",
						earthReq(condStructure("dragonminez:babidi")),
						new JsonObject[]{
								objKill("dragonminez:saga_buufat", 1, 308700, 12960, 11630)
						},
						rewAttributes(95)),
				step("buu", 15, "15_goku_and_vegeta_clash.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{
								noTransform(objKill("dragonminez:saga_goku_end_ssj2", 1, 289800, 7130, 6470)),
								objKill("dragonminez:saga_vegeta_majin", 1, 302400, 7450, 6740)
						},
						rewAttributes(100)),
				step("buu", 16, "16_second_fat_buu_battle.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_buufat", 1, 346500, 14580, 13160)
						},
						rewAttributes(100)),
				step("buu", 38, "16b_evacuate_the_city.json",
						earthReq(),
						new JsonObject[]{
								objDeliver("minecraft:bread", 32, "bulma"),
								objDeliver("minecraft:cooked_beef", 24, "bulma")
						},
						rewAttributes(15)),
				step("buu", 17, "17_stop_babidi.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_babidi", 1, 220500, 9180, 8260)
						},
						rewAttributes(80)),
				step("buu", 18, "18_goku_super_saiyan_three.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_goku_end_ssj3", 1, 393750, 16470, 14990)
						},
						rewAttributes(110)),
				step("buu", 19, "19_beach_training_with_gotenks.json",
						earthReq(condBiome("#minecraft:is_beach")),
						new JsonObject[]{
								noTransform(objKill("dragonminez:saga_gotenks", 1, 283500, 11880, 10710))
						},
						rewAttributes(95)),
				step("buu", 39, "19b_fusion_dance_practice.json",
						earthReq(condBiome("#minecraft:is_beach")),
						new JsonObject[]{
								objSpar("dragonminez:saga_gotenks_ssj", 320000, 13300, 12000)
						},
						rewAttributes(85)),
				step("buu", 20, "20_evil_buu_at_buus_house.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_evilbuu", 1, 362250, 15120, 13770)
						},
						rewAttributes(110)),
				step("buu", 21, "21_krillin_and_android_18.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_krillin", 1, 245700, 8260, 7520),
								objKill("dragonminez:saga_a18", 1, 283500, 9560, 8620)
						},
						rewAttributes(100)),
				step("buu", 22, "22_super_buu_in_the_time_chamber.json",
						dimensionReq("dragonminez:time_chamber"),
						new JsonObject[]{
								objKill("dragonminez:saga_superbuu", 1, 425250, 17820, 16070)
						},
						rewAttributes(110)),
				step("buu", 23, "23_gotenks_rocky_wasteland.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_gotenks_ssj3", 1, 425250, 17820, 16070)
						},
						rewAttributes(110)),
				step("buu", 24, "24_sacred_world_and_z_sword.json",
						sacredKaiReq(condSkill("potentialunlock", 10)),
						new JsonObject[]{
								objItem("dragonminez:z_sword", 1),
								objSkill("ultimate", 1)
						},
						rewAttributes(25)),
				step("buu", 40, "24b_ultimate_gohan.json",
						sacredKaiReq(),
						new JsonObject[]{
								objSpar("dragonminez:saga_gohan_end_ultimate", 440000, 18400, 16600)
						},
						rewAttributes(95)),
				step("buu", 25, "25_super_buu_returns.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_superbuu", 1, 456750, 19170, 17440)
						},
						rewAttributes(120)),
				step("buu", 26, "26_super_buu_gotenks.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_superbuu_gotenks", 1, 504000, 21060, 19130)
						},
						rewAttributes(120)),
				step("buu", 27, "27_super_buu_gohan.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_superbuu_gohan", 1, 551250, 23220, 21110)
						},
						rewAttributes(130)),
				step("buu", 28, "28_face_vegetto.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{
								transformStats(objKill("dragonminez:saga_goku_end_ssj2", 1, 189000, 12290, 11090), 283500, 14130, 12750),
								objKill("dragonminez:saga_vegeta_end_ssj2", 1, 378000, 12290, 11090)
						},
						rewAttributes(120)),
				step("buu", 41, "28b_vegetto_unleashed.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objSpar("dragonminez:saga_vegetto_ssj", 520000, 21800, 19700)
						},
						rewAttributes(110)),
				step("buu", 29, "29_return_to_the_sacred_world.json",
						sacredKaiReq(),
						new JsonObject[]{
								objDimension("dragonminez:sacredkaiplanet")
						},
						rewAttributes(10)),
				step("buu", 30, "30_kid_buu.json",
						sacredKaiReq(),
						new JsonObject[]{
								objKill("dragonminez:saga_kidbuu", 1, 582750, 24570, 22190)
						},
						rewAttributes(130)),
				step("buu", 42, "30b_spirit_bomb_plan.json",
						sacredKaiReq(),
						new JsonObject[]{
								objStructure("dragonminez:oldkai_pillar"),
								objTalkTo("oldkai")
						},
						rewAttributes(10)),
				step("buu", 31, "31_goku_ssj3_final_stand.json",
						sacredKaiReq(),
						new JsonObject[]{
								objKill("dragonminez:saga_goku_end_ssj3", 1, 488250, 20520, 18670)
						},
						rewAttributes(120)),
				step("buu", 32, "32_vegeta_ssj2_final_stand.json",
						sacredKaiReq(),
						new JsonObject[]{
								objKill("dragonminez:saga_vegeta_end_ssj2", 1, 456750, 19170, 17440)
						},
						rewAttributes(120)),
				step("buu", 33, "33_satan_and_majin_buu.json",
						sacredKaiReq(),
						new JsonObject[]{
								objKill("dragonminez:saga_buufat", 1, 393750, 16470, 14990)
						},
						rewAttributes(110)),
				step("buu", 34, "34_destroy_kid_buu.json",
						sacredKaiReq(),
						new JsonObject[]{
								objKill("dragonminez:saga_kidbuu", 1, 693000, 29160, 26320)
						},
						rewAttributes(140)),
				step("buu", 35, "35_return_to_earth.json",
						earthReq(),
						new JsonObject[]{
								objDimension("minecraft:overworld")
						},
						rewAttributes(11))
		);
	}

	// ========================================================================================
	// Movies Saga Quests (folder: saga_movies)
	// ========================================================================================

	private static void createMoviesSagaQuests(Path questsDir) {
		JsonObject prevSaiyan = prevQuest("saiyan_saga", 1);

		writeSaga(questsDir.resolve("saga_movies"), "movies_saga", "saga_movies", prevSaiyan,
				step("movies", 1, "01_kamis_lookout_warning.json",
						earthReq(),
						new JsonObject[]{
								objStructure("dragonminez:kamilookout"),
								objTalkTo("dende")
						},
						rewAttributes(2)),
				step("movies", 2, "02_garlic_jr_in_the_wasteland.json",
						earthReq(condBiome("dragonminez:rocky"), condSaga("saiyan_saga", 11)),
						new JsonObject[]{
								objKill("dragonminez:saga_garlick_jr", 1, 4500, 240, 320)
						},
						rewAttributes(25)),
				step("movies", 3, "03_garlic_jr_transformed.json",
						earthReq(condBiome("dragonminez:rocky"), condSaga("saiyan_saga", 12)),
						new JsonObject[]{
								objKill("dragonminez:saga_garlick_jr_transformed", 1, 8500, 290, 290)
						},
						rewAttributes(25)),
				step("movies", 4, "04_frozen_biome_signal.json",
						earthReq(condBiome("minecraft:snowy_plains"), condSaga("saiyan_saga", 12)),
						new JsonObject[]{
								objBiome("minecraft:snowy_plains")
						},
						rewAttributes(2)),
				step("movies", 5, "05_wheelo_controlled_allies.json",
						earthReq(condBiome("minecraft:snowy_plains"), condSaga("saiyan_saga", 12)),
						new JsonObject[]{
								objKill("dragonminez:saga_kid_gohan", 1, 4500, 205, 285),
								objKill("dragonminez:saga_krillin", 1, 3000, 120, 160)
						},
						rewAttributes(25)),
				step("movies", 6, "06_dr_wheelo.json",
						earthReq(condBiome("minecraft:snowy_plains"), condSaga("saiyan_saga", 12)),
						new JsonObject[]{
								objKill("dragonminez:saga_dr_wheelo", 1, 9750, 325, 325)
						},
						rewAttributes(30)),
				step("movies", 7, "07_tree_of_might_wasteland.json",
						earthReq(condBiome("dragonminez:rocky"), condSaga("saiyan_saga", 12)),
						new JsonObject[]{
								objBiome("dragonminez:rocky")
						},
						rewAttributes(2)),
				step("movies", 8, "08_turles_goku.json",
						earthReq(condBiome("dragonminez:rocky"), condSaga("saiyan_saga", 12)),
						new JsonObject[]{
								transformStats(objKill("dragonminez:saga_goku_mid_base", 1, 3600, 250, 300), 5400, 290, 340)
						},
						rewAttributes(30)),
				step("movies", 9, "09_turles_oozaru_gohan.json",
						earthReq(condBiome("dragonminez:rocky"), condSaga("saiyan_saga", 12)),
						new JsonObject[]{
								objKill("dragonminez:saga_ozaru", 1, 8500, 330, 330)
						},
						rewAttributes(30)),
				step("movies", 10, "10_turles.json",
						earthReq(condBiome("dragonminez:rocky"), condSaga("saiyan_saga", 12)),
						new JsonObject[]{
								objKill("dragonminez:saga_turles", 1, 10900, 360, 360)
						},
						rewAttributes(35)),
				step("movies", 11, "11_slug_soldiers.json",
						earthReq(condBiome("#dragonminez:is_plains"), condSaga("saiyan_saga", 12)),
						new JsonObject[]{
								objKill("dragonminez:saga_slug_soldier", 8, 2000, 42, 63)
						},
						rewAttributes(35)),
				step("movies", 12, "12_slug.json",
						earthReq(condBiome("#dragonminez:is_plains"), condSaga("saiyan_saga", 12)),
						new JsonObject[]{
								objKill("dragonminez:saga_slug", 1, 10000, 340, 340)
						},
						rewAttributes(35)),
				step("movies", 13, "13_giant_slug.json",
						earthReq(condBiome("#dragonminez:is_plains"), condSaga("saiyan_saga", 12)),
						new JsonObject[]{
								objKill("dragonminez:saga_slug_giant", 1, 12000, 400, 400)
						},
						rewAttributes(40)),
				step("movies", 14, "14_cooler_armored_squadron.json",
						earthReq(condBiome("dragonminez:rocky"), condSaga("frieza_saga", 5)),
						new JsonObject[]{
								objKill("dragonminez:saga_neiz", 1, 7000, 138, 155),
								objKill("dragonminez:saga_salza", 1, 8000, 155, 172),
								objKill("dragonminez:saga_dore", 1, 9000, 172, 206)
						},
						rewAttributes(40)),
				step("movies", 15, "15_cooler.json",
						earthReq(condBiome("dragonminez:rocky"), condSaga("frieza_saga", 14)),
						new JsonObject[]{
								objKill("dragonminez:saga_cooler", 1, 64000, 2400, 2080)
						},
						rewAttributes(60)),
				step("movies", 16, "16_cooler_fifth_form.json",
						earthReq(condBiome("dragonminez:rocky"), condSaga("frieza_saga", 15)),
						new JsonObject[]{
								objKill("dragonminez:saga_cooler_5ta", 1, 80000, 2880, 2410)
						},
						rewAttributes(65)),
				step("movies", 17, "17_big_gete_star.json",
						namekReq(condBiome("dragonminez:ajissa_plains"), condSaga("frieza_saga", 16)),
						new JsonObject[]{
								objKill("dragonminez:saga_gete_robot", 10, 3094, 200, 214)
						},
						rewAttributes(65)),
				step("movies", 18, "18_metal_cooler.json",
						namekReq(condBiome("dragonminez:ajissa_plains"), condSaga("frieza_saga", 16)),
						new JsonObject[]{
								objKill("dragonminez:saga_metal_cooler", 1, 66000, 2400, 2080)
						},
						rewAttributes(65)),
				step("movies", 19, "19_metal_cooler_core.json",
						namekReq(condBiome("dragonminez:ajissa_plains"), condSaga("frieza_saga", 16)),
						new JsonObject[]{
								objKill("dragonminez:saga_metal_cooler_core", 1, 80000, 2880, 2410)
						},
						rewAttributes(70)),
				step("movies", 20, "20_androids_in_the_ice.json",
						earthReq(condBiome("minecraft:snowy_plains"), condSaga("android_saga", 6)),
						new JsonObject[]{
								objKill("dragonminez:saga_a14", 1, 138600, 2380, 2150),
								objKill("dragonminez:saga_a15", 1, 116550, 2090, 1890)
						},
						rewAttributes(70)),
				step("movies", 21, "21_android_13.json",
						earthReq(condBiome("minecraft:snowy_plains"), condSaga("android_saga", 11)),
						new JsonObject[]{
								objKill("dragonminez:saga_a13", 1, 192000, 7420, 6860)
						},
						rewAttributes(85)),
				step("movies", 22, "22_super_android_13.json",
						earthReq(condBiome("minecraft:snowy_plains"), condSaga("android_saga", 13)),
						new JsonObject[]{
								objKill("dragonminez:saga_super_a13", 1, 249840, 10020, 9200)
						},
						rewAttributes(90)),
				step("movies", 23, "23_broly_base.json",
						earthReq(condBiome("dragonminez:rocky"), condSaga("android_saga", 15)),
						new JsonObject[]{
								transformStats(objKill("dragonminez:saga_broly_base", 1, 199870, 10020, 9200), 299810, 11530, 10580)
						},
						rewAttributes(90)),
				step("movies", 24, "24_paragus.json",
						earthReq(condBiome("dragonminez:rocky"), condSaga("android_saga", 15)),
						new JsonObject[]{
								objKill("dragonminez:saga_paragus", 1, 124990, 5010, 4510)
						},
						rewAttributes(90)),
				step("movies", 25, "25_legendary_broly.json",
						earthReq(condBiome("dragonminez:rocky"), condSaga("android_saga", 15)),
						new JsonObject[]{
								objKill("dragonminez:saga_broly_lssj", 1, 330620, 13480, 12140)
						},
						rewAttributes(95)),
				step("movies", 26, "26_bojack_allies.json",
						earthReq(condBiome("#dragonminez:is_plains"), condSaga("android_saga", 15)),
						new JsonObject[]{
								objKill("dragonminez:saga_bujin", 1, 74590, 2690, 2420),
								objKill("dragonminez:saga_bido", 1, 120960, 4280, 3810),
								objKill("dragonminez:saga_zangya", 1, 88700, 3060, 2770)
						},
						rewAttributes(95)),
				step("movies", 27, "27_gokua.json",
						earthReq(condBiome("#dragonminez:is_plains"), condSaga("android_saga", 15)),
						new JsonObject[]{
								objKill("dragonminez:saga_gokua", 1, 124990, 5010, 4510)
						},
						rewAttributes(95)),
				step("movies", 28, "28_bojack.json",
						earthReq(condBiome("#dragonminez:is_plains"), condSaga("android_saga", 15)),
						new JsonObject[]{
								objKill("dragonminez:saga_bojack", 1, 249980, 10020, 9200)
						},
						rewAttributes(95)),
				step("movies", 29, "29_full_power_bojack.json",
						earthReq(condBiome("#dragonminez:is_plains"), condSaga("android_saga", 15)),
						new JsonObject[]{
								objKill("dragonminez:saga_bojack_fp", 1, 330620, 13480, 12140)
						},
						rewAttributes(100)),
				step("movies", 30, "30_broly_second_coming.json",
						earthReq(condBiome("minecraft:snowy_plains"), condSaga("buu_saga", 22)),
						new JsonObject[]{
								objKill("dragonminez:saga_broly_ssj", 1, 680400, 28510, 25700)
						},
						rewAttributes(140), rewSkill("legendaryforms", 1)),
				step("movies", 31, "31_goten_and_trunks.json",
						earthReq(condBiome("minecraft:snowy_plains"), condSaga("buu_saga", 22)),
						new JsonObject[]{
								transformStats(objKill("dragonminez:saga_goten", 1, 126000, 10370, 9550), 189000, 11930, 10980),
								transformStats(objKill("dragonminez:saga_kid_trunks", 1, 136080, 11230, 10280), 204120, 12920, 11830)
						},
						rewAttributes(140)),
				step("movies", 32, "32_legendary_broly_second_coming.json",
						earthReq(condBiome("minecraft:snowy_plains"), condSaga("buu_saga", 30)),
						new JsonObject[]{
								objKill("dragonminez:saga_broly_lssj", 1, 932400, 39310, 35500)
						},
						rewAttributes(150)),
				step("movies", 33, "33_bio_broly.json",
						earthReq(condBiome("minecraft:swamp"), condSaga("buu_saga", 35)),
						new JsonObject[]{
								objKill("dragonminez:saga_bio_broly", 1, 724500, 30240, 27540)
						},
						rewAttributes(150)),
				step("movies", 34, "34_giant_bio_broly.json",
						earthReq(condBiome("minecraft:swamp"), condSaga("buu_saga", 35)),
						new JsonObject[]{
								objKill("dragonminez:saga_bio_broly_giant", 1, 1108800, 46660, 42110)
						},
						rewAttributes(160), rewSkill("legendaryforms", 2)),
				step("movies", 35, "35_otherworld_tournament.json",
						dimensionReq("dragonminez:otherworld", condSaga("buu_saga", 34)),
						new JsonObject[]{
								objKill("dragonminez:saga_paikuhan", 1, 724500, 30240, 27540)
						},
						rewAttributes(160)),
				step("movies", 36, "36_janemba.json",
						dimensionReq("dragonminez:otherworld", condSaga("buu_saga", 34)),
						new JsonObject[]{
								objKill("dragonminez:saga_janemba_fat", 1, 1008000, 42120, 38250)
						},
						rewAttributes(160)),
				step("movies", 37, "37_super_janemba.json",
						dimensionReq("dragonminez:otherworld", condSaga("buu_saga", 34)),
						new JsonObject[]{
								objKill("dragonminez:saga_super_janemba", 1, 1102500, 46440, 42230)
						},
						rewAttributes(170), rewItem("dragonminez:dimensional_sword", 1)),
				step("movies", 38, "38_hildegarn_half.json",
						earthReq(condBiome("#dragonminez:is_plains"), condAny(condSaga("gt_saga", 30), condSaga("daima_saga", 11))),
						new JsonObject[]{
								objKill("dragonminez:saga_hirudegarn_incomplete2", 1, 2215700, 93100, 84000)
						},
						rewAttributes(210)),
				step("movies", 39, "39_hildegarn_complete.json",
						earthReq(condBiome("#dragonminez:is_plains"), condAny(condSaga("gt_saga", 30), condSaga("daima_saga", 11))),
						new JsonObject[]{
								objKill("dragonminez:saga_hirudegarn", 1, 2341000, 98400, 88800)
						},
						rewAttributes(210)),
				step("movies", 40, "40_super_hildegarn.json",
						earthReq(condBiome("#dragonminez:is_plains"), condAny(condSaga("gt_saga", 30), condSaga("daima_saga", 11))),
						new JsonObject[]{
								objKill("dragonminez:saga_super_hirudegarn", 1, 2787000, 117100, 105700)
						},
						rewAttributes(230), rewSkill("legendaryforms", 3))
		);
	}

	// ========================================================================================
	// GT Saga Quests (folder: saga_gt)
	// ========================================================================================

	private static JsonObject otherworldReq(JsonObject... extraConditions) {
		return dimensionReq("dragonminez:otherworld", extraConditions);
	}

	/** Vegeta's blue GT suit (Black Star and Baby arcs) and the maroon-sleeved one he wears afterwards. */
	private static final int VEGETA_GT_EARLY = 0;
	private static final int VEGETA_GT_LATE = 1;

	private static void createGTSagaQuests(Path questsDir) {
		JsonObject prevBuu = prevQuest("buu_saga", 35);

		writeSaga(questsDir.resolve("saga_gt"), "gt_saga", "saga_gt", prevBuu,
				step("gt", 1, "01_goku_vs_uub.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{ objKill("dragonminez:saga_uub", 1, 645000, 27110, 24530) },
						rewAttributes(130)),
				step("gt", 2, "02_parts_for_bulma.json",
						earthReq(),
						new JsonObject[]{
								objDeliver("dragonminez:radar_piece", 4, "bulma"),
								objDeliver("minecraft:redstone_block", 8, "bulma"),
								objDeliver("minecraft:iron_block", 8, "bulma"),
								objDeliver("dragonminez:saiyan_ship", 1, "bulma")
						},
						rewAttributes(55), rewItem("dragonminez:dball_radar", 1)),
				step("gt", 3, "03_ledgic.json",
						namekReq(),
						new JsonObject[]{ objKill("dragonminez:saga_ledgic", 1, 833500, 35020, 31690) },
						rewAttributes(150)),
				step("gt", 4, "04_para_para_brothers.json",
						namekReq(),
						new JsonObject[]{
								objKill("dragonminez:saga_bon_para", 1, 384500, 14100, 12750),
								objKill("dragonminez:saga_don_para", 1, 384500, 14100, 12750),
								objKill("dragonminez:saga_son_para", 1, 384500, 14100, 12750)
						},
						rewAttributes(150)),
				step("gt", 5, "05_luud.json",
						namekReq(),
						new JsonObject[]{ objKill("dragonminez:saga_luud", 1, 1006000, 42280, 38260) },
						rewAttributes(160)),
				step("gt", 6, "06_general_rilldo.json",
						namekReq(),
						new JsonObject[]{ objKill("dragonminez:saga_rilldo", 1, 851500, 35770, 32370) },
						rewAttributes(150)),
				step("gt", 7, "07_meta_rilldo.json",
						namekReq(),
						new JsonObject[]{ objKill("dragonminez:saga_metal_rilldo", 1, 963500, 40480, 36630) },
						rewAttributes(160)),
				step("gt", 8, "08_hyper_meta_rilldo.json",
						namekReq(),
						new JsonObject[]{ objKill("dragonminez:saga_hyper_rilldo", 1, 1135500, 47720, 43180) },
						rewAttributes(170)),
				step("gt", 9, "09_return_to_earth.json",
						namekReq(),
						new JsonObject[]{ objDimension("minecraft:overworld") },
						rewAttributes(14)),

				// --- Baby ---
				step("gt", 10, "10_possessed_goten.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{ objKill("dragonminez:saga_goten_gt_baby", 1, 927000, 38950, 35250) },
						rewAttributes(160)),
				step("gt", 11, "11_possessed_gohan.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{ objKill("dragonminez:saga_gohan_gt_baby", 1, 998000, 41930, 37950) },
						rewAttributes(160)),
				step("gt", 12, "12_piccolo.json",
						earthReq(condBiome("#minecraft:is_mountain")),
						new JsonObject[]{ objKill("dragonminez:saga_piccolo_kami", 1, 969500, 40740, 36870) },
						rewAttributes(160)),
				step("gt", 13, "13_vegeta_gt.json",
						earthReq(condBiome("dragonminez:rocky")),
						// Base -> SSJ -> SSJ2. Split so the three stages still add up to the step's original budget.
						new JsonObject[]{ transformStats(objKill("dragonminez:saga_vegeta_gt", 1, 325000, 37540, 33970, VEGETA_GT_EARLY), 487500, 43170, 39070) },
						rewAttributes(150)),
				step("gt", 14, "14_super_baby_and_his_army.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{
								transformStats(objKill("dragonminez:saga_baby_vegeta", 1, 487000, 40920, 37030), 730500, 47060, 42580),
								objKill("dragonminez:saga_gohan_gt_baby", 1, 433000, 4600, 4100),
								objKill("dragonminez:saga_goten_gt_baby", 1, 433000, 4600, 4100),
								objKill("dragonminez:saga_trunks_gt_baby", 1, 433000, 4600, 4100)
						},
						rewAttributes(190)),
				step("gt", 15, "15_uub_steps_in.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{ objKill("dragonminez:saga_uub", 1, 1050000, 44130, 39930) },
						rewAttributes(170)),
				step("gt", 16, "16_super_baby_vegeta_2.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{ noTransform(objKill("dragonminez:saga_super_baby_vegeta2", 1, 1251000, 52560, 47560)) },
						rewAttributes(180)),
				step("gt", 17, "17_majuub.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_majuub", 1, 1161000, 48790, 44150) },
						rewAttributes(170)),
				step("gt", 18, "18_old_kai.json",
						sacredKaiReq(),
						new JsonObject[]{
								objStructure("dragonminez:oldkai_pillar"),
								objTalkTo("oldkai")
						},
						rewAttributes(15)),
				step("gt", 19, "19_goku_regains_his_tail.json",
						sacredKaiReq(),
						new JsonObject[]{ transformStats(objKill("dragonminez:saga_goku_gt", 1, 304000, 35110, 31770), 456000, 40380, 36540) },
						rewAttributes(150)),
				step("gt", 20, "20_golden_great_ape_baby.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{ transformStats(objKill("dragonminez:saga_super_baby_vegeta2", 1, 552000, 46380, 41980), 828000, 53340, 48280) },
						rewAttributes(170)),
				step("gt", 21, "21_baby.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_baby", 1, 1449000, 60880, 55100) },
						rewAttributes(200), rewItem("dragonminez:senzu_bean", 3)),

				// --- Super 17 ---
				step("gt", 22, "22_trunks_falls.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{ transformStats(objKill("dragonminez:saga_trunks_gt", 1, 482000, 40500, 36650), 723000, 46580, 42150) },
						rewAttributes(160)),
				step("gt", 23, "23_frieza_and_cell_in_hell.json",
						otherworldReq(),
						new JsonObject[]{
								objKill("dragonminez:saga_frieza_base", 1, 786500, 33050, 29910),
								objKill("dragonminez:saga_cell_perfect", 1, 786500, 33050, 29910)
						},
						rewAttributes(180)),
				step("gt", 24, "24_saibamen_breakout.json",
						otherworldReq(),
						new JsonObject[]{ objKill("#dragonminez:saibamen", 8, 200500, 8420, 7620) },
						rewAttributes(170)),
				step("gt", 25, "25_hells_escapees.json",
						otherworldReq(),
						new JsonObject[]{
								objKill("dragonminez:saga_general_blue", 1, 409000, 14000, 12660),
								objKill("dragonminez:saga_hyper_rilldo", 1, 477000, 16300, 14770),
								objKill("dragonminez:saga_puipui", 1, 409000, 14000, 12660),
								objKill("dragonminez:saga_yakon", 1, 409000, 14000, 12660),
								objKill("dragonminez:saga_nappa", 1, 409000, 14000, 12660)
						},
						rewAttributes(190)),
				step("gt", 26, "26_the_z_fighters_fall.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								noTransform(objKill("dragonminez:saga_gohan_gt", 1, 391500, 12050, 10900)),
								noTransform(objKill("dragonminez:saga_goten_gt", 1, 391500, 12050, 10900)),
								noTransform(objKill("dragonminez:saga_trunks_gt", 1, 391500, 12050, 10900)),
								objKill("dragonminez:saga_majuub", 1, 391500, 12050, 10900),
								objKill("dragonminez:saga_pan", 1, 391500, 12050, 10900),
								noTransform(objKill("dragonminez:saga_vegeta_gt", 1, 391500, 12050, 10900, VEGETA_GT_LATE))
						},
						rewAttributes(200)),
				step("gt", 27, "27_super_17.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_super_17", 1, 1720500, 72280, 65410) },
						rewAttributes(210)),
				step("gt", 28, "28_goku_super_saiyan_4.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_goku_gt_ssj4", 1, 1616500, 67930, 61470) },
						rewAttributes(210)),
				step("gt", 29, "29_android_18_and_goku.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_a18_gt", 1, 823500, 34590, 31310),
								noTransform(objKill("dragonminez:saga_goku_gt", 1, 823500, 34590, 31310))
						},
						rewAttributes(180)),
				step("gt", 30, "30_super_17_rematch.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_super_17", 1, 1994000, 83780, 75820) },
						rewAttributes(230), rewItem("dragonminez:senzu_bean", 3)),

				// --- Shadow Dragons ---
				step("gt", 31, "31_the_cracked_dragon_balls.json",
						earthReq(),
						new JsonObject[]{ objDragonSummon("shenron", "earth") },
						rewAttributes(120)),
				step("gt", 32, "32_liang_xing_long.json",
						earthReq(condBiome("minecraft:swamp")),
						new JsonObject[]{ objKill("dragonminez:saga_liang_xing_long", 1, 1571000, 66010, 59740) },
						rewAttributes(200)),
				step("gt", 33, "33_wu_xing_long.json",
						earthReq(condBiome("#minecraft:is_mountain")),
						new JsonObject[]{ transformStats(objKill("dragonminez:saga_wu_xing_long", 1, 839750, 70560, 63860), 1259500, 81140, 73440) },
						rewAttributes(210)),
				step("gt", 34, "34_liu_xing_long.json",
						earthReq(condBiome("#minecraft:is_beach")),
						new JsonObject[]{ transformStats(objKill("dragonminez:saga_liu_xing_long", 1, 854750, 71820, 64990), 1282000, 82590, 74740) },
						rewAttributes(210)),
				step("gt", 35, "35_qi_xing_long.json",
						earthReq(condBiome("#minecraft:is_forest")),
						new JsonObject[]{ transformStats(objKill("dragonminez:saga_qi_xing_long", 1, 911250, 76570, 69290), 1367000, 88060, 79680) },
						rewAttributes(220)),
				step("gt", 36, "36_neo_shenron.json",
						earthReq(condBiome("minecraft:desert")),
						new JsonObject[]{ transformStats(objKill("dragonminez:saga_neo_shenron", 1, 975000, 81920, 74140), 1462500, 94210, 85260) },
						rewAttributes(230)),
				step("gt", 37, "37_eis_shenron.json",
						earthReq(condBiome("#forge:is_snowy")),
						new JsonObject[]{ objKill("dragonminez:saga_eis_shenron", 1, 1995000, 83820, 75850) },
						rewAttributes(230)),
				step("gt", 38, "38_syn_shenron.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_syn_shenron", 1, 2129000, 89460, 80960) },
						rewAttributes(240)),
				step("gt", 39, "39_omega_shenron.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_omega_shenron", 1, 2372000, 99660, 90180) },
						rewAttributes(250)),
				step("gt", 40, "40_goku_super_saiyan_4_returns.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_goku_gt_ssj4", 1, 2051500, 86200, 78010) },
						rewAttributes(230)),
				step("gt", 41, "41_friends_at_full_power.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_trunks_gt_ssj", 1, 667000, 22550, 20430),
								objKill("dragonminez:saga_gohan_gt_ssj", 1, 667000, 22550, 20430),
								objKill("dragonminez:saga_goten_gt_ssj", 1, 667000, 22550, 20430),
								objKill("dragonminez:saga_pan", 1, 667000, 22550, 20430),
								objKill("dragonminez:saga_majuub", 1, 667000, 22550, 20430)
						},
						rewAttributes(230)),
				step("gt", 42, "42_two_super_saiyan_4s.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_goku_gt_ssj4", 1, 1168500, 49100, 44440),
								objKill("dragonminez:saga_vegeta_gt_ssj4", 1, 1168500, 49100, 44440)
						},
						rewAttributes(240)),
				step("gt", 43, "43_omega_shenron_strikes_back.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_omega_shenron", 1, 2686000, 112870, 102140) },
						rewAttributes(270)),
				step("gt", 44, "44_gogeta_super_saiyan_4.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_gogeta_ssj4", 1, 2845000, 119540, 108180) },
						rewAttributes(270)),
				step("gt", 45, "45_lend_me_your_energy.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								noTransform(objKill("dragonminez:saga_trunks_gt", 1, 578000, 22140, 20030)),
								noTransform(objKill("dragonminez:saga_gohan_gt", 1, 578000, 22140, 20030)),
								noTransform(objKill("dragonminez:saga_goten_gt", 1, 578000, 22140, 20030)),
								objKill("dragonminez:saga_pan", 1, 578000, 22140, 20030),
								objKill("dragonminez:saga_majuub", 1, 578000, 22140, 20030),
								noTransform(objKill("dragonminez:saga_vegeta_gt", 1, 578000, 22140, 20030, VEGETA_GT_LATE))
						},
						rewAttributes(240)),
				step("gt", 46, "46_omega_shenron_final.json",
						earthReq(condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_omega_shenron", 1, 3161500, 132830, 120210) },
						rewAttributes(290), rewItem("dragonminez:senzu_bean", 5))
		);
	}

	private static final String THIRD_DEMON_WORLD = "dragonminez:third_demon_world";
	private static final String SECOND_DEMON_WORLD = "dragonminez:second_demon_world";
	private static final String FIRST_DEMON_WORLD = "dragonminez:first_demon_world";

	private static JsonObject demonRealmReq(JsonObject... extraConditions) {
		return dimensionReq("dragonminez:demon_realm", extraConditions);
	}

	private static void createDaimaSagaQuests(Path questsDir) {
		JsonObject prevBuu = prevQuest("buu_saga", 35);

		writeSaga(questsDir.resolve("saga_daima"), "daima_saga", "saga_daima", prevBuu,
				step("daima", 1, "01_vegeta_training.json",
						earthReq(condBiome("#dragonminez:is_plains")),
						new JsonObject[]{ noTransform(objSpar("dragonminez:saga_vegeta_daima", 645000, 27110, 24530)) },
						rewAttributes(130)),
				step("daima", 2, "02_gomahs_wish.json",
						earthReq(),
						new JsonObject[]{
								objStructure("dragonminez:goku_house"),
								objTalkTo("goku")
						},
						rewAttributes(10)),
				step("daima", 3, "03_to_the_demon_realm.json",
						earthReq(),
						new JsonObject[]{
								objDeliver("minecraft:ender_pearl", 16, "bulma"),
								objDeliver("minecraft:crying_obsidian", 8, "bulma"),
								objDeliver("minecraft:redstone_block", 8, "bulma"),
								objDeliver("minecraft:amethyst_shard", 16, "bulma")
						},
						rewAttributes(55)),
				step("daima", 4, "04_the_third_demon_world.json",
						null,
						new JsonObject[]{ objBiome(THIRD_DEMON_WORLD) },
						rewAttributes(10)),
				step("daima", 5, "05_demon_bandits.json",
						demonRealmReq(condBiome(THIRD_DEMON_WORLD)),
						new JsonObject[]{ objKill("dragonminez:bandit", 10, 120000, 4460, 4035) },
						rewAttributes(150)),
				step("daima", 6, "06_glorios_test.json",
						demonRealmReq(condBiome(THIRD_DEMON_WORLD)),
						new JsonObject[]{ objSpar("dragonminez:saga_glorio", 1060000, 44550, 40310) },
						rewAttributes(170)),
				step("daima", 7, "07_tamagami_number_3.json",
						demonRealmReq(condBiome(THIRD_DEMON_WORLD)),
						new JsonObject[]{ transformStats(objKill("dragonminez:saga_tamagami_3", 3, 390000, 49200, 44520), 390000, 56580, 51200) },
						rewAttributes(180)),
				step("daima", 8, "08_a_new_majin_is_born.json",
						demonRealmReq(condBiome(THIRD_DEMON_WORLD)),
						new JsonObject[]{ objKill("dragonminez:saga_majin_kuu", 1, 1293000, 54350, 49170) },
						rewAttributes(180)),
				step("daima", 9, "09_the_second_demon_world.json",
						demonRealmReq(),
						new JsonObject[]{ objBiome(SECOND_DEMON_WORLD) },
						rewAttributes(14)),
				step("daima", 10, "10_tamagami_number_2.json",
						demonRealmReq(condBiome(SECOND_DEMON_WORLD)),
						new JsonObject[]{ transformStats(objKill("dragonminez:saga_tamagami_2", 1, 1577500, 66300, 59990), 1577500, 76250, 68990) },
						rewAttributes(200)),
				step("daima", 11, "11_majin_duu.json",
						demonRealmReq(condBiome(SECOND_DEMON_WORLD)),
						new JsonObject[]{ objKill("dragonminez:saga_majin_duu", 1, 1742000, 73220, 66250) },
						rewAttributes(210), rewItem("dragonminez:senzu_bean", 3)),
				step("daima", 12, "12_the_first_demon_world.json",
						demonRealmReq(),
						new JsonObject[]{ objBiome(FIRST_DEMON_WORLD) },
						rewAttributes(17)),
				step("daima", 13, "13_tamagami_number_1.json",
						demonRealmReq(condBiome(FIRST_DEMON_WORLD)),
						new JsonObject[]{ transformStats(objKill("dragonminez:saga_tamagami_1", 1, 2125000, 89310, 80810), 2125000, 102710, 92930) },
						rewAttributes(240)),
				step("daima", 14, "14_ultra_vegeta_1.json",
						demonRealmReq(condBiome(FIRST_DEMON_WORLD)),
						new JsonObject[]{ transformStats(objKill("dragonminez:saga_vegeta_mini", 1, 426500, 98640, 89260), 640000, 113440, 102650) },
						rewAttributes(250)),
				step("daima", 15, "15_the_great_assault.json",
						demonRealmReq(condBiome(FIRST_DEMON_WORLD)),
						new JsonObject[]{
								objWaves("dragonminez:saga_gomah_soldier_1", 2, 5, 216000, 9080, 8210),
								objWaves("dragonminez:saga_gomah_soldier_2", 1, 5, 216000, 9080, 8210)
						},
						rewAttributes(250)),
				step("daima", 16, "16_king_gomah.json",
						demonRealmReq(condBiome(FIRST_DEMON_WORLD)),
						new JsonObject[]{ noTransform(objKill("dragonminez:saga_gomah_mini", 1, 2863000, 120330, 108880)) },
						rewAttributes(270)),
				step("daima", 17, "17_the_demon_eye.json",
						demonRealmReq(condBiome(FIRST_DEMON_WORLD)),
						new JsonObject[]{ objKill("dragonminez:saga_gomah_third_eye", 1, 3161500, 132830, 120210) },
						rewAttributes(290), rewItem("dragonminez:senzu_bean", 5))
		);
	}
}
