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

	/** Keeps a quest-spawned enemy in the form it spawned in, for fights that reuse a transforming entity. */
	private static JsonObject noTransform(JsonObject killObjective) {
		killObjective.addProperty("canTransform", false);
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
						earthReq(1, nearWater()),
						new JsonObject[]{ objKill("dragonminez:giant_fish", 1, 100, 4, 0) },
						rewTPS(400), rewItem("dragonminez:giant_fish_cooked", 2)),
				step("classic", 2, "02_oolong_transformed.json",
						earthReq(2, atVillage()),
						new JsonObject[]{ objKill("dragonminez:saga_oolong_transformed", 1, 160, 6, 0) },
						rewTPS(500)),
				step("classic", 3, "03_oolong.json",
						earthReq(3, atVillage()),
						new JsonObject[]{ objKill("dragonminez:saga_oolong", 1, 130, 5, 0) },
						rewTPS(550)),
				step("classic", 4, "04_bandit_yamcha.json",
						earthReq(4, condBiome("minecraft:desert")),
						new JsonObject[]{ objKill("dragonminez:saga_teen_yamcha", 1, 260, 10, 8) },
						rewTPS(700)),
				step("classic", 5, "05_oozaru_goku.json",
						earthReq(5, condBiome("minecraft:desert")),
						new JsonObject[]{ objKill("dragonminez:saga_ozaru", 1, 480, 16, 0) },
						rewTPS(1000)),

				// --- 21st World Martial Arts Tournament ---
				step("classic", 6, "06_master_roshi.json",
						earthReq(6),
						new JsonObject[]{ objStructure("dragonminez:roshi_house"), objTalkTo("roshi") },
						rewTPS(400)),
				step("classic", 7, "07_milk_delivery.json",
						earthReq(7),
						new JsonObject[]{ objDeliver("minecraft:milk_bucket", 3, "roshi") },
						rewTPS(900)),
				step("classic", 8, "08_kame_house_sparring.json",
						earthReq(8, onIsland()),
						new JsonObject[]{
								objKill("dragonminez:saga_kid_goku", 1, 340, 13, 10, KID_GOKU_BLUE_GI),
								objKill("dragonminez:saga_kid_krillin", 1, 310, 12, 8, KID_KRILLIN_ORIN)
						}, rewTPS(1100)),
				step("classic", 9, "09_tournament_giran.json",
						earthReq(9, atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_giran", 1, 440, 19, 5) },
						rewTPS(1200)),
				step("classic", 10, "10_tournament_nam.json",
						earthReq(10, atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_nam", 1, 480, 22, 8) },
						rewTPS(1300)),
				step("classic", 11, "11_jackie_chun.json",
						earthReq(11, atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_jackie_chun", 1, 600, 28, 25) },
						rewTPS(1500)),
				step("classic", 12, "12_jackie_chun_full_power.json",
						earthReq(12, atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_ozaru", 1, 800, 34, 0) },
						rewTPS(1700)),

				// --- Red Ribbon Army Saga ---
				step("classic", 13, "13_colonel_silver.json",
						earthReq(12, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{ objKill("dragonminez:saga_colonel_silver", 1, 560, 26, 10) },
						rewTPS(1500)),
				step("classic", 14, "14_sergeant_metallic.json",
						earthReq(13, inSnow()),
						new JsonObject[]{ objKill("dragonminez:saga_sergeant_metallic", 1, 740, 36, 0) },
						rewTPS(1650)),
				step("classic", 15, "15_ninja_murasaki.json",
						earthReq(13, inSnow()),
						new JsonObject[]{ objKill("dragonminez:saga_ninja_murasaki", 1, 640, 31, 12) },
						rewTPS(1650)),
				step("classic", 16, "16_android_8.json",
						earthReq(14, inSnow()),
						new JsonObject[]{ objKill("dragonminez:saga_a8", 1, 850, 40, 0) },
						rewTPS(1800)),
				step("classic", 17, "17_general_blue.json",
						earthReq(15, inCave()),
						new JsonObject[]{ objKill("dragonminez:saga_general_blue", 1, 820, 38, 20) },
						rewTPS(1900)),
				step("classic", 18, "18_tao_pai_pai_strikes.json",
						earthReq(16, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{ objKill("dragonminez:saga_kid_goku", 1, 700, 34, 30, KID_GOKU_TURTLE_GI) },
						rewTPS(2000)),
				step("classic", 19, "19_sacred_water.json",
						earthReq(16),
						new JsonObject[]{ objStructure("dragonminez:kamilookout"), objDeliver("minecraft:water_bucket", 1, "karin") },
						rewTPS(1500), rewItem("dragonminez:red_capsule", 1), rewItem("dragonminez:green_capsule", 1),
						rewItem("dragonminez:blue_capsule", 1)),
				step("classic", 20, "20_tao_pai_pai_rematch.json",
						earthReq(17, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{ objKill("dragonminez:saga_tao_pai_pai", 1, 950, 48, 36) },
						rewTPS(2300)),
				step("classic", 21, "21_general_red.json",
						earthReq(18, atRedRibbonBase()),
						new JsonObject[]{ objKill("dragonminez:saga_general_red", 1, 400, 14, 0) },
						rewTPS(2000)),
				step("classic", 22, "22_officer_black_robot.json",
						earthReq(19, atRedRibbonBase()),
						new JsonObject[]{ objKill("dragonminez:saga_general_black_robot", 1, 1100, 54, 45) },
						rewTPS(2600)),

				// --- Fortuneteller Baba ---
				step("classic", 23, "23_fortuneteller_baba.json",
						earthReq(19),
						new JsonObject[]{ objStructure("dragonminez:baba_palace"), objTalkTo("baba_earth") },
						rewTPS(800)),
				step("classic", 24, "24_baba_dracula.json",
						earthReq(20, atBabaPalace()),
						new JsonObject[]{ objKill("dragonminez:saga_dracula", 1, 720, 35, 0) },
						rewTPS(2000)),
				step("classic", 25, "25_baba_invisible_man.json",
						earthReq(20, atBabaPalace()),
						new JsonObject[]{ objKill("dragonminez:saga_invisible_man", 1, 740, 36, 0) },
						rewTPS(2100)),
				step("classic", 26, "26_baba_mummy.json",
						earthReq(21, atBabaPalace()),
						new JsonObject[]{ objKill("dragonminez:saga_mummy", 1, 900, 45, 0) },
						rewTPS(2300)),
				step("classic", 27, "27_baba_akkuman.json",
						earthReq(21, atBabaPalace()),
						new JsonObject[]{ objKill("dragonminez:saga_akkuman", 1, 1000, 50, 40) },
						rewTPS(2500)),
				step("classic", 28, "28_baba_masked_warrior.json",
						earthReq(22, atBabaPalace()),
						new JsonObject[]{ objKill("dragonminez:saga_masked_warrior", 1, 1080, 56, 45) },
						rewTPS(2800)),
				step("classic", 29, "29_baba_kid_goku.json",
						earthReq(22, atBabaPalace()),
						new JsonObject[]{ objKill("dragonminez:saga_kid_goku", 1, 1150, 60, 50, KID_GOKU_TURTLE_GI) },
						rewTPS(3000)),

				// --- 22nd World Martial Arts Tournament ---
				step("classic", 30, "30_heavy_shell_training.json",
						earthReq(23),
						new JsonObject[]{ objDeliver("minecraft:scute", 1, "roshi") },
						rewTPS(2200)),
				step("classic", 31, "31_22nd_tournament.json",
						earthReq(23),
						new JsonObject[]{ objBiome("#dragonminez:is_plains") },
						rewTPS(600)),
				step("classic", 32, "32_tournament_yamcha.json",
						earthReq(24, atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_young_yamcha", 1, 1100, 58, 50) },
						rewTPS(2800)),
				step("classic", 33, "33_tournament_chiaotzu.json",
						earthReq(24, atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_chaoz", 1, 960, 46, 60) },
						rewTPS(2800)),
				step("classic", 34, "34_tournament_krillin.json",
						earthReq(25, atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_kid_krillin", 1, 1150, 62, 55, KID_KRILLIN_TURTLE_GI) },
						rewTPS(3000)),
				step("classic", 35, "35_tournament_tien.json",
						earthReq(25, atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_young_tien", 1, 1300, 72, 65) },
						rewTPS(3300)),
				step("classic", 36, "36_tournament_goku.json",
						earthReq(26, atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_kid_goku", 1, 1350, 75, 70, KID_GOKU_TURTLE_GI) },
						rewTPS(3600)),

				// --- King Piccolo Saga ---
				step("classic", 37, "37_krillin.json",
						earthReq(26),
						new JsonObject[]{ objKill("dragonminez:saga_kid_krillin", 1, 1200, 64, 55, KID_KRILLIN_TURTLE_GI) },
						rewTPS(3000)),
				step("classic", 38, "38_yajirobe.json",
						earthReq(26),
						new JsonObject[]{ objKill("dragonminez:saga_yajirobe", 1, 1250, 70, 10) },
						rewTPS(3100)),
				step("classic", 39, "39_tambourine.json",
						earthReq(27),
						new JsonObject[]{ objKill("dragonminez:saga_tambourine", 1, 1300, 72, 60) },
						rewTPS(3400)),
				step("classic", 40, "40_king_piccolo_old.json",
						earthReq(27),
						new JsonObject[]{ objKill("dragonminez:saga_piccolo_daimao_old", 1, 1400, 76, 70) },
						rewTPS(3800)),
				step("classic", 41, "41_goku_strikes_back.json",
						earthReq(27),
						new JsonObject[]{ objKill("dragonminez:saga_kid_goku", 1, 1400, 78, 75, KID_GOKU_KING_PICCOLO) },
						rewTPS(3800)),
				step("classic", 42, "42_pilaf_gang_robots.json",
						earthReq(28),
						new JsonObject[]{
								objKill("dragonminez:saga_pilaf_robot", 1, 700, 38, 18),
								objKill("dragonminez:saga_shu_robot", 1, 720, 40, 18),
								objKill("dragonminez:saga_mai_robot", 1, 850, 45, 24)
						}, rewTPS(3500)),
				step("classic", 43, "43_fused_pilaf_robot.json",
						earthReq(28),
						new JsonObject[]{ objKill("dragonminez:saga_pilaf_robot_fused", 1, 1500, 80, 40) },
						rewTPS(4000)),
				step("classic", 44, "44_drum.json",
						earthReq(28),
						new JsonObject[]{ objKill("dragonminez:saga_drum", 1, 1500, 82, 30) },
						rewTPS(4200)),
				step("classic", 45, "45_king_piccolo_young.json",
						earthReq(29),
						new JsonObject[]{ objKill("dragonminez:saga_piccolo_daimao_young", 1, 1600, 88, 90) },
						rewTPS(5000), rewItem("dragonminez:senzu_bean", 2)),

				// --- 23rd World Martial Arts Tournament ---
				step("classic", 46, "46_lookout_training.json",
						earthReq(29),
						new JsonObject[]{ objStructure("dragonminez:kamilookout"), objDeliver("minecraft:emerald", 5, "popo") },
						rewTPS(2500)),
				step("classic", 47, "47_cyborg_tao_pai_pai.json",
						earthReq(29, atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_tao_pai_pai_cyborg", 1, 1450, 78, 80) },
						rewTPS(4600)),
				step("classic", 48, "48_tournament_chichi.json",
						earthReq(30, atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_chichi", 1, 1400, 74, 20) },
						rewTPS(4600)),
				step("classic", 49, "49_tournament_tien.json",
						earthReq(30, atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_tien_t23", 1, 1600, 86, 90) },
						rewTPS(5200)),
				step("classic", 50, "50_tournament_piccolo.json",
						earthReq(30, atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_majunia", 1, 1650, 90, 130) },
						rewTPS(6000)),
				step("classic", 51, "51_giant_piccolo.json",
						earthReq(30, atTournament()),
						new JsonObject[]{ objKill("dragonminez:saga_majunia_giant", 1, 1800, 95, 140) },
						rewTPS(7000)),
				step("classic", 52, "52_tournament_finale.json",
						earthReq(30),
						new JsonObject[]{ objStructure("dragonminez:goku_house"), objTalkTo("goku") },
						rewTPS(3000), rewItem("dragonminez:senzu_bean", 3))
		);
	}

	// ========================================================================================
	// Saiyan Saga Quests (folder: saga_saiyan)
	// ========================================================================================

	private static void createSaiyanSagaQuests(Path questsDir) {
		JsonObject prevClassic = prevQuest("classic_saga", 52);
		writeSaga(questsDir.resolve("saga_saiyan"), "saiyan_saga", "saga_saiyan", prevClassic,
				step("saiyan", 1, "01_defeat_raditz.json",
						earthReq(31, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_raditz", 1, 1800, 90, 140)
						},
						rewTPS(7500), rewItem("dragonminez:broken_scouter", 1)),
				step("saiyan", 2, "02_piccolo_takes_gohan.json",
						earthReq(34),
						new JsonObject[]{
								objStructure("dragonminez:piccolo_house"),
								objTalkTo("piccolo")
						},
						rewTPS(5000)),
				step("saiyan", 3, "03_survive_wilderness_training.json",
						earthReq(38, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:dino1", 1, 2200, 40, 0)
						},
						rewTPS(8000), rewItem("dragonminez:cooked_dino_meat", 8)),
				step("saiyan", 4, "04_gohan_oozaru.json",
						earthReq(40, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_ozaru", 1, 3200, 95, 0)
						},
						rewTPS(8200)),
				step("saiyan", 5, "05_spar_with_gohan.json",
						earthReq(42),
						new JsonObject[]{
								objKill("dragonminez:saga_kid_gohan", 1, 2000, 80, 130)
						},
						rewTPS(8300)),
				step("saiyan", 6, "06_lookout_provisions.json",
						earthReq(43),
						new JsonObject[]{
								objStructure("dragonminez:kamilookout"),
								objDeliver("dragonminez:cooked_dino_meat", 6, "popo")
						},
						rewTPS(4500)),
				step("saiyan", 7, "07_lookout_sparring.json",
						earthReq(44, condStructure("dragonminez:kamilookout")),
						new JsonObject[]{
								objKill("dragonminez:saga_krillin", 1, 1700, 70, 110),
								objKill("dragonminez:saga_yamcha", 1, 1700, 75, 100),
								objKill("dragonminez:saga_tien_early", 1, 1900, 85, 120)
						},
						rewTPS(8400)),
				step("saiyan", 8, "08_kill_the_saibamans.json",
						earthReq(45, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("#dragonminez:saibamen", 6, 1800, 80, 120)
						},
						rewTPS(8500)),
				step("saiyan", 9, "09_hold_against_nappa.json",
						earthReq(52, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_nappa", 1, 2800, 120, 160)
						}, rewTPS(9500)),
				step("saiyan", 10, "10_senzu_from_korin.json",
						earthReq(56),
						new JsonObject[]{
								objStructure("dragonminez:kamilookout"),
								objTalkTo("karin")
						},
						rewTPS(5000), rewItem("dragonminez:senzu_bean", 3)),
				step("saiyan", 11, "11_face_vegeta.json",
						earthReq(60, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_vegeta", 1, 3800, 150, 200)
						},
						rewTPS(11000)),
				step("saiyan", 12, "12_defeat_oozaru_vegeta.json",
						earthReq(70, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_ozaruvegeta", 1, 7500, 250, 250)
						},
						rewTPS(13000)),
				step("saiyan", 13, "13_kame_house_recovery.json",
						earthReq(80),
						new JsonObject[]{
								objStructure("dragonminez:roshi_house"),
								objDeliver("minecraft:golden_carrot", 12, "krillin")
						},
						rewTPS(7000)),
				step("saiyan", 14, "14_repair_the_saiyan_pod.json",
						earthReq(90),
						new JsonObject[]{
								objStructure("dragonminez:vegeta_pod"),
								objDeliver("minecraft:iron_block", 8, "bulma"),
								objDeliver("minecraft:redstone_block", 4, "bulma"),
								objDeliver("minecraft:diamond", 4, "bulma")
						},
						rewTPS(9000)),
				step("saiyan", 15, "15_prepare_for_namek.json",
						earthReq(100, condRealTimeMinutes(5)),
						new JsonObject[]{
								objTalkTo("bulma")
						},
						rewTPS(6000), rewItem("dragonminez:saiyan_ship", 1)),
				step("saiyan", 16, "16_head_to_namek.json",
						earthReq(130),
						new JsonObject[]{
								objDimension("dragonminez:namek")
						},
						rewTPS(6000))
		);
	}

	// ========================================================================================
	// Frieza Saga Quests (folder: saga_frieza)
	// ========================================================================================

	private static void createFriezaSagaQuests(Path questsDir) {
		JsonObject prevSaiyan = prevQuest("saiyan_saga", 16);
		writeSaga(questsDir.resolve("saga_frieza"), "frieza_saga", "saga_frieza", prevSaiyan,
				step("frieza", 1, "01_secure_namek_landing.json",
						namekReq(130, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("#dragonminez:frieza_soldiers", 8, 4500, 150, 150)
						},
						rewTPS(16000)),
				step("frieza", 2, "02_defeat_cui.json",
						namekReq(142, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cui", 1, 5500, 180, 200)
						},
						rewTPS(18000)),
				step("frieza", 3, "03_defend_the_namekians.json",
						namekReq(154, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("#dragonminez:frieza_soldiers", 16, 5000, 160, 160)
						},
						rewTPS(20000)),
				step("frieza", 4, "04_defeat_dodoria.json",
						namekReq(168, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_dodoria", 1, 7000, 220, 250)
						},
						rewTPS(22000)),
				step("frieza", 5, "05_defeat_zarbon.json",
						namekReq(178, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_zarbon", 1, 8500, 250, 280)
						},
						rewTPS(24000)),
				step("frieza", 6, "06_the_saiyan_prince.json",
						namekReq(190, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_vegeta_namek", 1, 9000, 260, 290)
						},
						rewTPS(26000)),
				step("frieza", 7, "07_defeat_guldo.json",
						namekReq(202, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_guldo", 1, 6000, 180, 180)
						},
						rewTPS(27000)),
				step("frieza", 8, "08_defeat_recoome.json",
						namekReq(214, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_recoome", 1, 10000, 300, 280)
						},
						rewTPS(29000)),
				step("frieza", 9, "09_defeat_burter_and_jeice.json",
						namekReq(226, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_burter", 1, 10000, 300, 280),
								objKill("dragonminez:saga_jeice", 1, 10000, 300, 280)
						},
						rewTPS(32000)),
				step("frieza", 10, "10_defeat_ginyu.json",
						namekReq(238, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_ginyu", 1, 14000, 400, 350)
						},
						rewTPS(34000)),
				step("frieza", 11, "11_defeat_ginyu_goku.json",
						namekReq(250, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_ginyu_goku", 1, 9000, 260, 220)
						},
						rewTPS(36000)),
				step("frieza", 12, "12_defeat_frieza_first.json",
						namekReq(285, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_frieza_first", 1, 20000, 500, 450)
						},
						rewTPS(42000)),
				step("frieza", 13, "13_defeat_frieza_third.json",
						namekReq(310, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_frieza_second", 1, 30000, 950, 850)
						},
						rewTPS(45000)),
				step("frieza", 14, "14_defeat_frieza_base.json",
						namekReq(345, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_frieza_base", 1, 40000, 1500, 1300)
						},
						rewTPS(48000)),
				step("frieza", 15, "15_defeat_frieza_full_power.json",
						namekReq(380, condBiome("dragonminez:ajissa_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_frieza_fp", 1, 50000, 1800, 1550)
						},
						rewTPS(53000)),
				step("frieza", 16, "16_escape_namek_before_collapse.json",
						namekReq(390),
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
						earthReq(470, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_mecha_frieza", 1, 65000, 2500, 2300)
						},
						rewTPS(56000)),
				step("android", 2, "02_defeat_king_cold.json",
						earthReq(500, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_king_cold", 1, 40000, 1400, 1300)
						},
						rewTPS(58000)),
				step("android", 3, "03_warning_from_the_future.json",
						earthReq(530),
						new JsonObject[]{
								objTalkTo("trunks")
						},
						rewTPS(16800)),
				step("android", 4, "04_three_year_training.json",
						earthReq(590, condRealTimeMinutes(15)),
						new JsonObject[]{
								objKill("dragonminez:shadow_dummy", 16, 6300, 330, 323)
						},
						rewTPS(58800)),
				step("android", 5, "05_defeat_a19.json",
						earthReq(650, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_a19", 1, 80000, 3100, 2800)
						},
						rewTPS(63000)),
				step("android", 6, "06_defeat_drgero.json",
						earthReq(690, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_drgero", 1, 70000, 2700, 2400)
						},
						rewTPS(65000)),
				step("android", 7, "07_track_android_signal.json",
						earthReq(730),
						new JsonObject[]{
								objBiome("#minecraft:is_mountain")
						},
						rewTPS(9800)),
				step("android", 8, "08_defeat_a18.json",
						earthReq(770, condBiome("#minecraft:is_mountain")),
						new JsonObject[]{
								objKill("dragonminez:saga_a18", 1, 110000, 4300, 3800)
						},
						rewTPS(68000)),
				step("android", 9, "09_defeat_a17.json",
						earthReq(810, condBiome("#minecraft:is_mountain")),
						new JsonObject[]{
								objKill("dragonminez:saga_a17", 1, 125000, 5200, 5500)
						},
						rewTPS(70000)),
				step("android", 10, "10_defeat_cell_imperfect.json",
						earthReq(1010, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_imperfect", 1, 110000, 4400, 4000)
						},
						rewTPS(78000)),
				step("android", 11, "11_defeat_cell_semiperfect.json",
						earthReq(1090, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_semiperfect", 1, 150000, 5800, 5300)
						},
						rewTPS(82000)),
				step("android", 12, "12_beyond_super_saiyan.json",
						earthReq(1130, condRealTimeMinutes(10)),
						new JsonObject[]{
								objKill("dragonminez:shadow_dummy", 20, 7700, 420, 391)
						},
						rewTPS(86240)),
				step("android", 13, "13_defeat_cell_perfect.json", // MERGE POINT: Todo vuelve a los stats originales desde aquí.
						earthReq(1210, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_perfect", 1, 217000, 8700, 7990)
						},
						rewTPS(88480)),
				step("android", 14, "14_defeat_cell_jrs.json",
						earthReq(1330, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_jr", 7, 150500, 5940, 6545)
						},
						rewTPS(92960)),
				step("android", 15, "15_defeat_cell_superperfect.json",
						earthReq(1410, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_superperfect", 1, 287000, 11700, 10540)
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
						earthReq(1410),
						new JsonObject[]{
								objTalkTo("trunks")
						},
						rewTPS(25600)),
				step("future", 2, "02_train_with_trunks_and_gohan.json",
						earthReq(1430, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_ftrunks_base", 1, 182000, 7500, 6800),
								objKill("dragonminez:saga_fgohan_base", 1, 196000, 8100, 7310)
						},
						rewTPS(70400)),
				step("future", 3, "03_androids_ruined_plains.json",
						earthReq(1470, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_a17", 1, 217000, 9000, 8160),
								objKill("dragonminez:saga_a18", 1, 210000, 8700, 7820)
						},
						rewTPS(83200)),
				step("future", 4, "04_face_future_gohan.json",
						earthReq(1510, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_fgohan_ssj", 1, 238000, 9600, 8840)
						},
						rewTPS(89600)),
				step("future", 5, "05_androids_in_the_mountains.json",
						earthReq(1550, condBiome("#minecraft:is_mountain")),
						new JsonObject[]{
								objKill("dragonminez:saga_a18", 1, 238000, 9900, 9010),
								objKill("dragonminez:saga_a17", 1, 248500, 10500, 9520)
						},
						rewTPS(102400)),
				step("future", 6, "06_imperfect_cell_of_the_future.json",
						earthReq(1590, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_cell_imperfect", 1, 266000, 11100, 10030)
						},
						rewTPS(115200)),
				step("future", 7, "07_future_restored.json",
						earthReq(1610),
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
						earthReq(1450, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_goten", 1, 175000, 7200, 6630),
								objKill("dragonminez:saga_gohan_end_base", 1, 217000, 9000, 8160)
						},
						rewTPS(129600)),
				step("buu", 2, "02_assemble_gravity_device_parts.json",
						earthReq(1490),
						new JsonObject[]{
								objItem("dragonminez:kikono_station", 1),
								objItem("dragonminez:fuel_generator", 1),
								objItem("dragonminez:energy_cable", 8)
						},
						rewTPS(79200)),
				step("buu", 3, "03_train_with_trunks_and_vegeta.json",
						earthReq(1530, condRealTimeMinutes(10)),
						new JsonObject[]{
								objKill("dragonminez:saga_kid_trunks", 1, 189000, 7800, 7140),
								objKill("dragonminez:saga_vegeta_end_base", 1, 245000, 10200, 9350)
						},
						rewTPS(151200)),
				step("buu", 4, "04_enter_the_world_tournament.json",
						earthReq(1570, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objTalkTo("piccolo")
						},
						rewTPS(43200)),
				step("buu", 5, "05_tournament_goten.json",
						earthReq(1590, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_goten", 1, 196000, 8100, 7310)
						},
						rewTPS(86400)),
				step("buu", 6, "06_tournament_trunks.json",
						earthReq(1610, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_kid_trunks", 1, 210000, 8700, 7820)
						},
						rewTPS(93600)),
				step("buu", 7, "07_tournament_krillin.json",
						earthReq(1630, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_krillin", 1, 217000, 9000, 8160)
						},
						rewTPS(93600)),
				step("buu", 8, "08_tournament_shin.json",
						earthReq(1650, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_shin", 1, 231000, 9600, 8670)
						},
						rewTPS(100800)),
				step("buu", 9, "09_tournament_spopovich.json",
						earthReq(1670, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_spopovitch", 1, 238000, 9900, 8840)
						},
						rewTPS(108000)),
				step("buu", 10, "10_find_babidi_ship.json",
						earthReq(1690),
						new JsonObject[]{
								objStructure("dragonminez:babidi")
						},
						rewTPS(50400)),
				step("buu", 11, "11_babidi_level_pui_pui.json",
						earthReq(1710, condStructure("dragonminez:babidi")),
						new JsonObject[]{
								objKill("dragonminez:saga_puipui", 1, 252000, 10500, 9520)
						},
						rewTPS(115200)),
				step("buu", 12, "12_babidi_level_yakon.json",
						earthReq(1730, condStructure("dragonminez:babidi")),
						new JsonObject[]{
								objKill("dragonminez:saga_yakon", 1, 273000, 11400, 10370)
						},
						rewTPS(122400)),
				step("buu", 13, "13_babidi_level_dabura.json",
						earthReq(1770, condStructure("dragonminez:babidi")),
						new JsonObject[]{
								objKill("dragonminez:saga_dabura", 1, 308000, 12900, 11730)
						},
						rewTPS(136800)),
				step("buu", 14, "14_fat_buu_awakes.json",
						earthReq(1810, condStructure("dragonminez:babidi")),
						new JsonObject[]{
								objKill("dragonminez:saga_buufat", 1, 343000, 14400, 12920)
						},
						rewTPS(151200)),
				step("buu", 15, "15_goku_and_vegeta_clash.json",
						earthReq(1850, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_goku_end_ssj2", 1, 322000, 13500, 12240),
								objKill("dragonminez:saga_vegeta_majin", 1, 336000, 14100, 12750)
						},
						rewTPS(158400)),
				step("buu", 16, "16_second_fat_buu_battle.json",
						earthReq(1890, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_buufat", 1, 385000, 16200, 14620)
						},
						rewTPS(165600)),
				step("buu", 17, "17_stop_babidi.json",
						earthReq(1910, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_babidi", 1, 245000, 10200, 9180)
						},
						rewTPS(108000)),
				step("buu", 18, "18_goku_super_saiyan_three.json",
						earthReq(1950, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_goku_end_ssj3", 1, 437500, 18300, 16660)
						},
						rewTPS(187200)),
				step("buu", 19, "19_beach_training_with_gotenks.json",
						earthReq(1970, condBiome("#minecraft:is_beach")),
						new JsonObject[]{
								objKill("dragonminez:saga_gotenks", 1, 315000, 13200, 11900)
						},
						rewTPS(151200)),
				step("buu", 20, "20_evil_buu_at_buus_house.json",
						earthReq(1990, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_evilbuu", 1, 402500, 16800, 15300)
						},
						rewTPS(187200)),
				step("buu", 21, "21_krillin_and_android_18.json",
						earthReq(2010, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_krillin", 1, 273000, 11400, 10370),
								objKill("dragonminez:saga_a18", 1, 315000, 13200, 11900)
						},
						rewTPS(158400)),
				step("buu", 22, "22_super_buu_in_the_time_chamber.json",
						dimensionReq("dragonminez:time_chamber", 2050),
						new JsonObject[]{
								objKill("dragonminez:saga_superbuu", 1, 472500, 19800, 17850)
						},
						rewTPS(208800)),
				step("buu", 23, "23_gotenks_rocky_wasteland.json",
						earthReq(2070, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_gotenks_ssj3", 1, 472500, 19800, 17850)
						},
						rewTPS(201600)),
				step("buu", 24, "24_sacred_world_and_z_sword.json",
						sacredKaiReq(2090, condSkill("potentialunlock", 10)),
						new JsonObject[]{
								objItem("dragonminez:z_sword", 1),
								objSkill("ultimate", 1)
						},
						rewTPS(187200)),
				step("buu", 25, "25_super_buu_returns.json",
						earthReq(2110, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_superbuu", 1, 507500, 21300, 19380)
						},
						rewTPS(216000)),
				step("buu", 26, "26_super_buu_gotenks.json",
						earthReq(2130, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_superbuu_gotenks", 1, 560000, 23400, 21250)
						},
						rewTPS(237600)),
				step("buu", 27, "27_super_buu_gohan.json",
						earthReq(2170, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_superbuu_gohan", 1, 612500, 25800, 23460)
						},
						rewTPS(259200)),
				step("buu", 28, "28_face_vegetto.json",
						earthReq(2210, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_goku_end_ssj2", 1, 420000, 17700, 15980),
								objKill("dragonminez:saga_vegeta_end_ssj2", 1, 420000, 17700, 15980)
						},
						rewTPS(244800)),
				step("buu", 29, "29_return_to_the_sacred_world.json",
						sacredKaiReq(2230),
						new JsonObject[]{
								objDimension("dragonminez:sacredkaiplanet")
						},
						rewTPS(79200)),
				step("buu", 30, "30_kid_buu.json",
						sacredKaiReq(2270),
						new JsonObject[]{
								objKill("dragonminez:saga_kidbuu", 1, 647500, 27300, 24650)
						},
						rewTPS(273600)),
				step("buu", 31, "31_goku_ssj3_final_stand.json",
						sacredKaiReq(2290),
						new JsonObject[]{
								objKill("dragonminez:saga_goku_end_ssj3", 1, 542500, 22800, 20740)
						},
						rewTPS(223200)),
				step("buu", 32, "32_vegeta_ssj2_final_stand.json",
						sacredKaiReq(2310),
						new JsonObject[]{
								objKill("dragonminez:saga_vegeta_end_ssj2", 1, 507500, 21300, 19380)
						},
						rewTPS(216000)),
				step("buu", 33, "33_satan_and_majin_buu.json",
						sacredKaiReq(2330),
						new JsonObject[]{
								objKill("dragonminez:saga_buufat", 1, 437500, 18300, 16660)
						},
						rewTPS(187200)),
				step("buu", 34, "34_destroy_kid_buu.json",
						sacredKaiReq(2350),
						new JsonObject[]{
								objKill("dragonminez:saga_kidbuu", 1, 770000, 32400, 29240)
						},
						rewTPS(324000)),
				step("buu", 35, "35_return_to_earth.json",
						earthReq(2350),
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
						earthReq(55),
						new JsonObject[]{
								objStructure("dragonminez:kamilookout"),
								objTalkTo("dende")
						},
						rewTPS(1500)),
				step("movies", 2, "02_garlic_jr_in_the_wasteland.json",
						earthReq(60, condBiome("dragonminez:rocky"), condSaga("saiyan_saga", 11)),
						new JsonObject[]{
								objKill("dragonminez:saga_garlick_jr", 1, 4500, 250, 350)
						},
						rewTPS(9000)),
				step("movies", 3, "03_garlic_jr_transformed.json",
						earthReq(75, condBiome("dragonminez:rocky"), condSaga("saiyan_saga", 12)),
						new JsonObject[]{
								objKill("dragonminez:saga_garlick_jr_transformed", 1, 8500, 400, 400)
						},
						rewTPS(12000)),
				step("movies", 4, "04_frozen_biome_signal.json",
						earthReq(78, condBiome("minecraft:snowy_plains"), condSaga("saiyan_saga", 12)),
						new JsonObject[]{
								objBiome("minecraft:snowy_plains")
						},
						rewTPS(10600)),
				step("movies", 5, "05_wheelo_controlled_allies.json",
						earthReq(80, condBiome("minecraft:snowy_plains"), condSaga("saiyan_saga", 12)),
						new JsonObject[]{
								objKill("dragonminez:saga_kid_gohan", 1, 4500, 250, 350),
								objKill("dragonminez:saga_krillin", 1, 3000, 150, 200)
						},
						rewTPS(10900)),
				step("movies", 6, "06_dr_wheelo.json",
						earthReq(90, condBiome("minecraft:snowy_plains"), condSaga("saiyan_saga", 12)),
						new JsonObject[]{
								objKill("dragonminez:saga_dr_wheelo", 1, 10000, 450, 450)
						},
						rewTPS(15000)),
				step("movies", 7, "07_tree_of_might_wasteland.json",
						earthReq(95, condBiome("dragonminez:rocky"), condSaga("saiyan_saga", 12)),
						new JsonObject[]{
								objBiome("dragonminez:rocky")
						},
						rewTPS(13400)),
				step("movies", 8, "08_turles_goku.json",
						earthReq(100, condBiome("dragonminez:rocky"), condSaga("saiyan_saga", 12)),
						new JsonObject[]{
								objKill("dragonminez:saga_goku_mid_base", 1, 4500, 250, 350)
						},
						rewTPS(14400)),
				step("movies", 9, "09_turles_oozaru_gohan.json",
						earthReq(110, condBiome("dragonminez:rocky"), condSaga("saiyan_saga", 12)),
						new JsonObject[]{
								objKill("dragonminez:saga_ozaru", 1, 8500, 400, 400)
						},
						rewTPS(15600)),
				step("movies", 10, "10_turles.json",
						earthReq(120, condBiome("dragonminez:rocky"), condSaga("saiyan_saga", 12)),
						new JsonObject[]{
								objKill("dragonminez:saga_turles", 1, 12000, 500, 500)
						},
						rewTPS(19000)),
				step("movies", 11, "11_slug_soldiers.json",
						earthReq(125, condBiome("#dragonminez:is_plains"), condSaga("saiyan_saga", 12)),
						new JsonObject[]{
								objKill("dragonminez:saga_slug_soldier", 8, 2000, 100, 150)
						},
						rewTPS(18100)),
				step("movies", 12, "12_slug.json",
						earthReq(130, condBiome("#dragonminez:is_plains"), condSaga("saiyan_saga", 12)),
						new JsonObject[]{
								objKill("dragonminez:saga_slug", 1, 14000, 600, 600)
						},
						rewTPS(21000)),
				step("movies", 13, "13_giant_slug.json",
						earthReq(140, condBiome("#dragonminez:is_plains"), condSaga("saiyan_saga", 12)),
						new JsonObject[]{
								objKill("dragonminez:saga_slug_giant", 1, 20000, 800, 800)
						},
						rewTPS(23000)),
				step("movies", 14, "14_cooler_armored_squadron.json",
						earthReq(180, condBiome("dragonminez:rocky"), condSaga("frieza_saga", 5)),
						new JsonObject[]{
								objKill("dragonminez:saga_neiz", 1, 7000, 400, 450),
								objKill("dragonminez:saga_salza", 1, 8000, 450, 500),
								objKill("dragonminez:saga_dore", 1, 9000, 500, 600)
						},
						rewTPS(24400)),
				step("movies", 15, "15_cooler.json",
						earthReq(350, condBiome("dragonminez:rocky"), condSaga("frieza_saga", 14)),
						new JsonObject[]{
								objKill("dragonminez:saga_cooler", 1, 65000, 3000, 2600)
						},
						rewTPS(60000)),
				step("movies", 16, "16_cooler_fifth_form.json",
						earthReq(390, condBiome("dragonminez:rocky"), condSaga("frieza_saga", 15)),
						new JsonObject[]{
								objKill("dragonminez:saga_cooler_5ta", 1, 78000, 3700, 3100)
						},
						rewTPS(67000)),
				step("movies", 17, "17_big_gete_star.json",
						namekReq(400, condBiome("dragonminez:ajissa_plains"), condSaga("frieza_saga", 16)),
						new JsonObject[]{
								objKill("dragonminez:saga_gete_robot", 10, 3094, 200, 214)
						},
						rewTPS(66900)),
				step("movies", 18, "18_metal_cooler.json",
						namekReq(410, condBiome("dragonminez:ajissa_plains"), condSaga("frieza_saga", 16)),
						new JsonObject[]{
								objKill("dragonminez:saga_metal_cooler", 1, 65000, 3000, 2600)
						},
						rewTPS(70000)),
				step("movies", 19, "19_metal_cooler_core.json",
						namekReq(430, condBiome("dragonminez:ajissa_plains"), condSaga("frieza_saga", 16)),
						new JsonObject[]{
								objKill("dragonminez:saga_metal_cooler_core", 1, 78000, 3700, 3100)
						},
						rewTPS(74000)),
				step("movies", 20, "20_androids_in_the_ice.json", // MERGE POINT: Todo vuelve a los stats originales desde aquí.
						earthReq(730, condBiome("minecraft:snowy_plains"), condSaga("android_saga", 6)),
						new JsonObject[]{
								objKill("dragonminez:saga_a14", 1, 154000, 6000, 5440),
								objKill("dragonminez:saga_a15", 1, 129500, 5280, 4760)
						},
						rewTPS(81200)),
				step("movies", 21, "21_android_13.json",
						earthReq(1110, condBiome("minecraft:snowy_plains"), condSaga("android_saga", 11)),
						new JsonObject[]{
								objKill("dragonminez:saga_a13", 1, 294000, 11400, 10540)
						},
						rewTPS(110000)),
				step("movies", 22, "22_super_android_13.json",
						earthReq(1230, condBiome("minecraft:snowy_plains"), condSaga("android_saga", 13)),
						new JsonObject[]{
								objKill("dragonminez:saga_super_a13", 1, 434000, 17400, 15980)
						},
						rewTPS(131200)),
				step("movies", 23, "23_broly_base.json",
						earthReq(1490, condBiome("dragonminez:rocky"), condSaga("android_saga", 15)),
						new JsonObject[]{
								objKill("dragonminez:saga_broly_base", 1, 434000, 17400, 15980)
						},
						rewTPS(132500)),
				step("movies", 24, "24_paragus.json",
						earthReq(1495, condBiome("dragonminez:rocky"), condSaga("android_saga", 15)),
						new JsonObject[]{
								objKill("dragonminez:saga_paragus", 1, 217000, 8700, 7820)
						},
						rewTPS(133800)),
				step("movies", 25, "25_legendary_broly.json",
						earthReq(1580, condBiome("dragonminez:rocky"), condSaga("android_saga", 15)),
						new JsonObject[]{
								objKill("dragonminez:saga_broly_lssj", 1, 574000, 23400, 21080)
						},
						rewTPS(143800), rewSkill("legendaryforms", 1)),
				step("movies", 26, "26_bojack_allies.json",
						earthReq(1590, condBiome("#dragonminez:is_plains"), condSaga("android_saga", 15)),
						new JsonObject[]{
								objKill("dragonminez:saga_bujin", 1, 129500, 5280, 4760),
								objKill("dragonminez:saga_bido", 1, 210000, 8400, 7480),
								objKill("dragonminez:saga_zangya", 1, 154000, 6000, 5440)
						},
						rewTPS(145000)),
				step("movies", 27, "27_gokua.json",
						earthReq(1600, condBiome("#dragonminez:is_plains"), condSaga("android_saga", 15)),
						new JsonObject[]{
								objKill("dragonminez:saga_gokua", 1, 217000, 8700, 7820)
						},
						rewTPS(147500)),
				step("movies", 28, "28_bojack.json",
						earthReq(1630, condBiome("#dragonminez:is_plains"), condSaga("android_saga", 15)),
						new JsonObject[]{
								objKill("dragonminez:saga_bojack", 1, 434000, 17400, 15980)
						},
						rewTPS(151200)),
				step("movies", 29, "29_full_power_bojack.json",
						earthReq(1680, condBiome("#dragonminez:is_plains"), condSaga("android_saga", 15)),
						new JsonObject[]{
								objKill("dragonminez:saga_bojack_fp", 1, 574000, 23400, 21080)
						},
						rewTPS(158800)),
				step("movies", 30, "30_broly_second_coming.json",
						earthReq(2050, condBiome("minecraft:snowy_plains"), condSaga("buu_saga", 22)),
						new JsonObject[]{
								objKill("dragonminez:saga_broly_ssj", 1, 945000, 39600, 35700)
						},
						rewTPS(306200)),
				step("movies", 31, "31_goten_and_trunks.json",
						earthReq(2060, condBiome("minecraft:snowy_plains"), condSaga("buu_saga", 22)),
						new JsonObject[]{
								objKill("dragonminez:saga_goten", 1, 350000, 14400, 13260),
								objKill("dragonminez:saga_kid_trunks", 1, 378000, 15600, 14280)
						},
						rewTPS(312500)),
				step("movies", 32, "32_legendary_broly_second_coming.json",
						earthReq(2270, condBiome("minecraft:snowy_plains"), condSaga("buu_saga", 30)),
						new JsonObject[]{
								objKill("dragonminez:saga_broly_lssj", 1, 1295000, 54600, 49300)
						},
						rewTPS(375000)),
				step("movies", 33, "33_bio_broly.json",
						earthReq(2280, condBiome("minecraft:swamp"), condSaga("buu_saga", 30)),
						new JsonObject[]{
								objKill("dragonminez:saga_bio_broly", 1, 805000, 33600, 30600)
						},
						rewTPS(381200)),
				step("movies", 34, "34_giant_bio_broly.json",
						earthReq(2380, condBiome("minecraft:swamp"), condSaga("buu_saga", 34)),
						new JsonObject[]{
								objKill("dragonminez:saga_bio_broly_giant", 1, 1120000, 46800, 42500)
						},
						rewTPS(406200), rewSkill("legendaryforms", 2)),
				step("movies", 35, "35_otherworld_tournament.json",
						dimensionReq("dragonminez:otherworld", 2390, condSaga("buu_saga", 34)),
						new JsonObject[]{
								objKill("dragonminez:saga_paikuhan", 1, 805000, 33600, 30600)
						},
						rewTPS(412500)),
				step("movies", 36, "36_janemba.json",
						dimensionReq("dragonminez:otherworld", 2410, condSaga("buu_saga", 34)),
						new JsonObject[]{
								objKill("dragonminez:saga_janemba_fat", 1, 1120000, 46800, 42500)
						},
						rewTPS(425000)),
				step("movies", 37, "37_super_janemba.json",
						dimensionReq("dragonminez:otherworld", 2430, condSaga("buu_saga", 34)),
						new JsonObject[]{
								objKill("dragonminez:saga_super_janemba", 1, 1225000, 51600, 46920)
						},
						rewTPS(437500), rewItem("dragonminez:dimensional_sword", 1)),
				step("movies", 38, "38_hildegarn_half.json",
						earthReq(2440, condBiome("#dragonminez:is_plains"), condSaga("buu_saga", 34)),
						new JsonObject[]{
								objKill("dragonminez:saga_hirudegarn_incomplete2", 1, 1225000, 51600, 46920)
						},
						rewTPS(443800)),
				step("movies", 39, "39_hildegarn_complete.json",
						earthReq(2460, condBiome("#dragonminez:is_plains"), condSaga("buu_saga", 34)),
						new JsonObject[]{
								objKill("dragonminez:saga_hirudegarn", 1, 1295000, 54600, 49300)
						},
						rewTPS(456200)),
				step("movies", 40, "40_super_hildegarn.json",
						earthReq(2490, condBiome("#dragonminez:is_plains"), condSaga("buu_saga", 34)),
						new JsonObject[]{
								objKill("dragonminez:saga_super_hirudegarn", 1, 1540000, 64800, 58480)
						},
						rewTPS(475000), rewSkill("legendaryforms", 3))
		);
	}

	// ========================================================================================
	// GT Saga Quests (folder: saga_gt)
	// ========================================================================================

	private static JsonObject otherworldReq(int minLevel, JsonObject... extraConditions) {
		return dimensionReq("dragonminez:otherworld", minLevel, extraConditions);
	}

	private static void createGTSagaQuests(Path questsDir) {
		JsonObject prevBuu = prevQuest("buu_saga", 35);

		writeSaga(questsDir.resolve("saga_gt"), "gt_saga", "saga_gt", prevBuu,
				step("gt", 1, "01_goku_vs_uub.json",
						earthReq(2400, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{ objKill("dragonminez:saga_uub", 1, 645000, 27110, 24530) },
						rewTPS(271000)),
				step("gt", 2, "02_parts_for_bulma.json",
						earthReq(2410),
						new JsonObject[]{
								objDeliver("dragonminez:radar_piece", 4, "bulma"),
								objDeliver("minecraft:redstone_block", 8, "bulma"),
								objDeliver("minecraft:iron_block", 8, "bulma"),
								objDeliver("dragonminez:saiyan_ship", 1, "bulma")
						},
						rewTPS(97600), rewItem("dragonminez:dball_radar", 1)),
				step("gt", 3, "03_ledgic.json",
						namekReq(2440),
						new JsonObject[]{ objKill("dragonminez:saga_ledgic", 1, 833500, 35020, 31690) },
						rewTPS(350100)),
				step("gt", 4, "04_para_para_brothers.json",
						namekReq(2470),
						new JsonObject[]{
								objKill("dragonminez:saga_bon_para", 1, 384500, 16150, 14610),
								objKill("dragonminez:saga_don_para", 1, 384500, 16150, 14610),
								objKill("dragonminez:saga_son_para", 1, 384500, 16150, 14610)
						},
						rewTPS(345900)),
				step("gt", 5, "05_luud.json",
						namekReq(2500),
						new JsonObject[]{ objKill("dragonminez:saga_luud", 1, 1006000, 42280, 38260) },
						rewTPS(422600)),
				step("gt", 6, "06_general_rilldo.json",
						namekReq(2530),
						new JsonObject[]{ objKill("dragonminez:saga_rilldo", 1, 851500, 35770, 32370) },
						rewTPS(357600)),
				step("gt", 7, "07_meta_rilldo.json",
						namekReq(2560),
						new JsonObject[]{ objKill("dragonminez:saga_metal_rilldo", 1, 963500, 40480, 36630) },
						rewTPS(404600)),
				step("gt", 8, "08_hyper_meta_rilldo.json",
						namekReq(2600),
						new JsonObject[]{ objKill("dragonminez:saga_hyper_rilldo", 1, 1135500, 47720, 43180) },
						rewTPS(477000)),
				step("gt", 9, "09_return_to_earth.json",
						namekReq(2600),
						new JsonObject[]{ objDimension("minecraft:overworld") },
						rewTPS(113600)),

				// --- Baby ---
				step("gt", 10, "10_possessed_goten.json",
						earthReq(2640, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{ objKill("dragonminez:saga_goten_gt_baby", 1, 927000, 38950, 35250) },
						rewTPS(389300)),
				step("gt", 11, "11_possessed_gohan.json",
						earthReq(2670, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{ objKill("dragonminez:saga_gohan_gt_baby", 1, 998000, 41930, 37950) },
						rewTPS(419200)),
				step("gt", 12, "12_piccolo.json",
						earthReq(2700, condBiome("#minecraft:is_mountain")),
						new JsonObject[]{ objKill("dragonminez:saga_piccolo_kami", 1, 969500, 40740, 36870) },
						rewTPS(407200)),
				step("gt", 13, "13_vegeta_gt.json",
						earthReq(2740, condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_vegeta_gt", 1, 893500, 37540, 33970) },
						rewTPS(375200)),
				step("gt", 14, "14_super_baby_and_his_army.json",
						earthReq(2780, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_baby_vegeta", 1, 974000, 40920, 37030),
								objKill("dragonminez:saga_gohan_gt_baby", 1, 433000, 18180, 16460),
								objKill("dragonminez:saga_goten_gt_baby", 1, 433000, 18180, 16460),
								objKill("dragonminez:saga_trunks_gt_baby", 1, 433000, 18180, 16460)
						},
						rewTPS(600000)),
				step("gt", 15, "15_uub_steps_in.json",
						earthReq(2810, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{ objKill("dragonminez:saga_uub", 1, 1050000, 44130, 39930) },
						rewTPS(441100)),
				step("gt", 16, "16_super_baby_vegeta_2.json",
						earthReq(2850, condBiome("dragonminez:rocky")),
						new JsonObject[]{ noTransform(objKill("dragonminez:saga_super_baby_vegeta2", 1, 1251000, 52560, 47560)) },
						rewTPS(525400)),
				step("gt", 17, "17_majuub.json",
						earthReq(2880, condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_majuub", 1, 1161000, 48790, 44150) },
						rewTPS(487700)),
				step("gt", 18, "18_old_kai.json",
						sacredKaiReq(2890),
						new JsonObject[]{
								objStructure("dragonminez:oldkai_pillar"),
								objTalkTo("oldkai")
						},
						rewTPS(140300)),
				step("gt", 19, "19_goku_regains_his_tail.json",
						sacredKaiReq(2920),
						new JsonObject[]{ objKill("dragonminez:saga_goku_gt", 1, 835500, 35110, 31770) },
						rewTPS(350900)),
				step("gt", 20, "20_golden_great_ape_baby.json",
						earthReq(2960, condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_super_baby_vegeta2", 1, 1104000, 46380, 41980) },
						rewTPS(463700)),
				step("gt", 21, "21_baby.json",
						earthReq(3000, condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_baby", 1, 1449000, 60880, 55100) },
						rewTPS(608600), rewItem("dragonminez:senzu_bean", 3)),

				// --- Super 17 ---
				step("gt", 22, "22_trunks_falls.json",
						earthReq(3030, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{ objKill("dragonminez:saga_trunks_gt", 1, 964000, 40500, 36650) },
						rewTPS(404900)),
				step("gt", 23, "23_frieza_and_cell_in_hell.json",
						otherworldReq(3060),
						new JsonObject[]{
								objKill("dragonminez:saga_frieza_base", 1, 786500, 33050, 29910),
								objKill("dragonminez:saga_cell_perfect", 1, 786500, 33050, 29910)
						},
						rewTPS(520000)),
				step("gt", 24, "24_saibamen_breakout.json",
						otherworldReq(3090),
						new JsonObject[]{ objKill("#dragonminez:saibamen", 8, 200500, 8420, 7620) },
						rewTPS(481200)),
				step("gt", 25, "25_hells_escapees.json",
						otherworldReq(3120),
						new JsonObject[]{
								objKill("dragonminez:saga_general_blue", 1, 409000, 17180, 15550),
								objKill("dragonminez:saga_hyper_rilldo", 1, 477000, 20040, 18140),
								objKill("dragonminez:saga_puipui", 1, 409000, 17180, 15550),
								objKill("dragonminez:saga_yakon", 1, 409000, 17180, 15550),
								objKill("dragonminez:saga_nappa", 1, 409000, 17180, 15550)
						},
						rewTPS(560000)),
				step("gt", 26, "26_the_z_fighters_fall.json",
						earthReq(3160, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								noTransform(objKill("dragonminez:saga_gohan_gt", 1, 391500, 16450, 14880)),
								noTransform(objKill("dragonminez:saga_goten_gt", 1, 391500, 16450, 14880)),
								noTransform(objKill("dragonminez:saga_trunks_gt", 1, 391500, 16450, 14880)),
								objKill("dragonminez:saga_majuub", 1, 391500, 16450, 14880),
								objKill("dragonminez:saga_pan", 1, 391500, 16450, 14880),
								noTransform(objKill("dragonminez:saga_vegeta_gt", 1, 391500, 16450, 14880))
						},
						rewTPS(620000)),
				step("gt", 27, "27_super_17.json",
						earthReq(3200, condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_super_17", 1, 1720500, 72280, 65410) },
						rewTPS(722500)),
				step("gt", 28, "28_goku_super_saiyan_4.json",
						earthReq(3240, condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_goku_gt_ssj4", 1, 1616500, 67930, 61470) },
						rewTPS(679000)),
				step("gt", 29, "29_android_18_and_goku.json",
						earthReq(3270, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								objKill("dragonminez:saga_a18_gt", 1, 823500, 34590, 31310),
								noTransform(objKill("dragonminez:saga_goku_gt", 1, 823500, 34590, 31310))
						},
						rewTPS(520000)),
				step("gt", 30, "30_super_17_rematch.json",
						earthReq(3310, condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_super_17", 1, 1994000, 83780, 75820) },
						rewTPS(837500), rewItem("dragonminez:senzu_bean", 3)),

				// --- Shadow Dragons ---
				step("gt", 31, "31_the_cracked_dragon_balls.json",
						earthReq(3320),
						new JsonObject[]{ objDragonSummon("shenron", "earth") },
						rewTPS(185200)),
				step("gt", 32, "32_liang_xing_long.json",
						earthReq(3350, condBiome("minecraft:swamp")),
						new JsonObject[]{ objKill("dragonminez:saga_liang_xing_long", 1, 1571000, 66010, 59740) },
						rewTPS(659900)),
				step("gt", 33, "33_wu_xing_long.json",
						earthReq(3380, condBiome("#minecraft:is_mountain")),
						new JsonObject[]{ objKill("dragonminez:saga_wu_xing_long", 1, 1679500, 70560, 63860) },
						rewTPS(705300)),
				step("gt", 34, "34_liu_xing_long.json",
						earthReq(3410, condBiome("#minecraft:is_beach")),
						new JsonObject[]{ objKill("dragonminez:saga_liu_xing_long", 1, 1709500, 71820, 64990) },
						rewTPS(717900)),
				step("gt", 35, "35_qi_xing_long.json",
						earthReq(3440, condBiome("#minecraft:is_forest")),
						new JsonObject[]{ objKill("dragonminez:saga_qi_xing_long", 1, 1822500, 76570, 69290) },
						rewTPS(765400)),
				step("gt", 36, "36_neo_shenron.json",
						earthReq(3480, condBiome("minecraft:desert")),
						new JsonObject[]{ objKill("dragonminez:saga_neo_shenron", 1, 1950000, 81920, 74140) },
						rewTPS(818900)),
				step("gt", 37, "37_eis_shenron.json",
						earthReq(3520, condBiome("#forge:is_snowy")),
						new JsonObject[]{ objKill("dragonminez:saga_eis_shenron", 1, 1995000, 83820, 75850) },
						rewTPS(837800)),
				step("gt", 38, "38_syn_shenron.json",
						earthReq(3560, condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_syn_shenron", 1, 2129000, 89460, 80960) },
						rewTPS(894200)),
				step("gt", 39, "39_omega_shenron.json",
						earthReq(3610, condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_omega_shenron", 1, 2372000, 99660, 90180) },
						rewTPS(996200)),
				step("gt", 40, "40_goku_super_saiyan_4_returns.json",
						earthReq(3650, condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_goku_gt_ssj4", 1, 2051500, 86200, 78010) },
						rewTPS(861700)),
				step("gt", 41, "41_friends_at_full_power.json",
						earthReq(3690, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_trunks_gt_ssj", 1, 667000, 28030, 25370),
								objKill("dragonminez:saga_gohan_gt_ssj", 1, 667000, 28030, 25370),
								objKill("dragonminez:saga_goten_gt_ssj", 1, 667000, 28030, 25370),
								objKill("dragonminez:saga_pan", 1, 667000, 28030, 25370),
								objKill("dragonminez:saga_majuub", 1, 667000, 28030, 25370)
						},
						rewTPS(880000)),
				step("gt", 42, "42_two_super_saiyan_4s.json",
						earthReq(3730, condBiome("dragonminez:rocky")),
						new JsonObject[]{
								objKill("dragonminez:saga_goku_gt_ssj4", 1, 1168500, 49100, 44440),
								objKill("dragonminez:saga_vegeta_gt_ssj4", 1, 1168500, 49100, 44440)
						},
						rewTPS(900000)),
				step("gt", 43, "43_omega_shenron_strikes_back.json",
						earthReq(3770, condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_omega_shenron", 1, 2686000, 112870, 102140) },
						rewTPS(1128200)),
				step("gt", 44, "44_gogeta_super_saiyan_4.json",
						earthReq(3810, condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_gogeta_ssj4", 1, 2845000, 119540, 108180) },
						rewTPS(1195000)),
				step("gt", 45, "45_lend_me_your_energy.json",
						earthReq(3840, condBiome("#dragonminez:is_plains")),
						new JsonObject[]{
								noTransform(objKill("dragonminez:saga_trunks_gt", 1, 578000, 24290, 21980)),
								noTransform(objKill("dragonminez:saga_gohan_gt", 1, 578000, 24290, 21980)),
								noTransform(objKill("dragonminez:saga_goten_gt", 1, 578000, 24290, 21980)),
								objKill("dragonminez:saga_pan", 1, 578000, 24290, 21980),
								objKill("dragonminez:saga_majuub", 1, 578000, 24290, 21980),
								noTransform(objKill("dragonminez:saga_vegeta_gt", 1, 578000, 24290, 21980))
						},
						rewTPS(930000)),
				step("gt", 46, "46_omega_shenron_final.json",
						earthReq(3880, condBiome("dragonminez:rocky")),
						new JsonObject[]{ objKill("dragonminez:saga_omega_shenron", 1, 3161500, 132830, 120210) },
						rewTPS(1327800), rewItem("dragonminez:senzu_bean", 5))
		);
	}
}