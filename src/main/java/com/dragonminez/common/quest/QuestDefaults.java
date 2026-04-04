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
 * Files use the same schema for all quest types — only the {@code "type"} field differs.
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
 *   "chain": { "saga": "saiyan_saga", "order": 1, "next": null },
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

	private QuestDefaults() {
	} // utility class

	static void createDefaultQuestFiles(Path questsDir) {
		if (!ConfigManager.getServerConfig().getGameplay().getStoryModeEnabled()) return;
		if (!ConfigManager.getServerConfig().getGameplay().getCreateDefaultSagas()) return;

		createSaiyanSagaQuests(questsDir);
		createFriezaSagaQuests(questsDir);
		createAndroidSagaQuests(questsDir);
		createBuuSagaQuests(questsDir);
	}

	// ---- Helpers ----

	private static void writeQuest(Path dir, String filename, JsonObject quest) {
		Path file = dir.resolve(filename);
		if (Files.exists(file)) return;
		try {
			Files.createDirectories(dir);
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
										String chainSaga, int chainOrder, String chainNext,
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
		q.add("quest_giver", JsonNull.INSTANCE);
		q.add("turn_in", JsonNull.INSTANCE);

		JsonObject chain = new JsonObject();
		chain.addProperty("saga", chainSaga);
		chain.addProperty("order", chainOrder);
		if (chainNext != null) chain.addProperty("next", chainNext);
		else chain.add("next", JsonNull.INSTANCE);
		q.add("chain", chain);

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

	private static JsonObject objStructure(String desc, String structureId) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "STRUCTURE");
		o.addProperty("description", desc);
		o.addProperty("structure", structureId);
		return o;
	}

	private static JsonObject objBiome(String desc, String biomeId) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "BIOME");
		o.addProperty("description", desc);
		o.addProperty("biome", biomeId);
		return o;
	}

	private static JsonObject objDimension(String desc, String dimensionId) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "DIMENSION");
		o.addProperty("description", desc);
		o.addProperty("dimension", dimensionId);
		return o;
	}

	private static JsonObject objKill(String desc, String entity, int count, double hp, double melee, double ki) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "KILL");
		o.addProperty("description", desc);
		o.addProperty("entity", entity);
		o.addProperty("count", count);
		o.addProperty("health", hp);
		o.addProperty("meleeDamage", melee);
		o.addProperty("kiDamage", ki);
		return o;
	}

	private static JsonObject objItem(String desc, String itemId, int count) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "ITEM");
		o.addProperty("description", desc);
		o.addProperty("item", itemId);
		o.addProperty("count", count);
		return o;
	}

	private static JsonObject objTalkTo(String desc, String targetNpcId) { //Maybe change for entity (from targetNpcId) later?
		JsonObject o = new JsonObject();
		o.addProperty("type", "TALK_TO");
		o.addProperty("description", desc);
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
		c.addProperty("minutes", minutes);
		return c;
	}

	private static JsonObject condRealTimeMinutes(long minutes) {
		JsonObject c = new JsonObject();
		c.addProperty("type", "TIME");
		c.addProperty("mode", "REAL_TIME");
		c.addProperty("minutes", minutes);
		return c;
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
		Path dir = questsDir.resolve("saga_saiyan");
		String s = "saiyan_saga", c = "saga_saiyan";

		writeLinearSaga(dir, s, c, null,
				step("saiyan", 1, "01_find_roshi.json", requirements("AND", condLevel(1)), new JsonObject[]{objStructure("dmz.quest.saiyan1.obj1","dragonminez:roshi_house")}, rewTPS(500), rewItem("minecraft:bread", 3)),
				step("saiyan", 2, "02_train_with_goku.json", requirements("AND", condStructure("dragonminez:roshi_house"), condLevel(3)), new JsonObject[]{objKill("dmz.quest.saiyan2.obj1","dragonminez:saga_goku",1, 200, 10, 0)}, rewTPS(1000), rewItem("minecraft:iron_sword", 1)),
				step("saiyan", 3, "03_defend_gohan.json", requirements("AND", condBiome("minecraft:plains")), new JsonObject[]{objKill("dmz.quest.saiyan3.obj1", "dragonminez:saga_raditz", 1, 500, 32, 65)}, rewTPS(500), rewItem("dragonminez:broken_scouter", 1)),
				step("saiyan", 4, "04_train_with_gohan.json", requirements("AND", condBiome("minecraft:plains"), condLevel(10)), new JsonObject[]{objKill("dmz.quest.saiyan4.obj1", "dragonminez:dino1", 1, 1000, 10, 0)}, rewTPS(800), rewItem("dragonminez:cooked_dino_meat", 8)),
				step("saiyan", 5, "05_craft_dragon_radar.json", requirements("AND", condGameTimeMinutes(52800), condDimension("minecraft:overworld")), new JsonObject[]{objItem("dmz.quest.saiyan5.obj1", "dragonminez:dball_radar", 1), objTalkTo("dmz.quest.saiyan5.obj2", "dragonminez:bulma_1")}, rewTPS(1500)),
				step("saiyan", 6, "06_kill_the_saibamans.json", requirements("AND", condLevel(15), condBiome("minecraft:plains")), new JsonObject[]{objKill("dmz.quest.saiyan6.obj1", "dragonminez:saga_saibaman", 6, 400, 25, 50)}, rewTPS(2500)),
				step("saiyan", 7, "07_hold_against_nappa.json", requirements("AND", condBiome("minecraft:plains")), new JsonObject[]{objKill("dmz.quest.saiyan7.obj1", "dragonminez:saga_nappa", 1, 750, 45, 100)}, rewTPS(3000)),
				step("saiyan", 8, "08_face_vegeta", requirements("AND", condBiome("dragonminez:rocky")), new JsonObject[]{objKill("dmz.quest.saiyan8.obj1", "dragonnminez:saga_vegeta", 1, 1200, 70, 150)}, rewTPS(3500)),
				step("saiyan", 9, "09_kill_oozaru_vegeta", requirements("AND", condBiome("dragonminez:rocky")), new JsonObject[]{objKill("dmz.quest.saiyan9.obj1", "dragonminez:saga_vegeta_ozaru", 1, 2500, 140, 200)}, rewTPS(4000)),
				step("saiyan", 10, "10_feast_with_others", requirements("AND", condStructure("dragonminez:roshi_house")), new JsonObject[]{objStructure("dmz.quest.saiyan10.obj1", "dragonminez:roshi_house")}, rewTPS(500), rewItem("dragonminez:senzu_bean", 6)),
				step("saiyan", 11, "11_fix_the_saiyan_ship", requirements("AND", condRealTimeMinutes(5)), new JsonObject[]{objTalkTo("dmz.quest.saiyan11.obj1", "dragonminez:bulma_1")}, rewItem("dragonminez:saiyan_ship", 1)),
				step("saiyan", 12, "12_head_to_namek", requirements("AND", condLevel(100)), new JsonObject[]{objDimension("dmz.quest.saiyan12.obj1", "dragonminez:namek")})
		);
	}

	// ========================================================================================
	// Frieza Saga Quests (folder: saga_frieza)
	// ========================================================================================

	private static void createFriezaSagaQuests(Path questsDir) {
		Path dir = questsDir.resolve("saga_frieza");
		String s = "frieza_saga", c = "saga_frieza";
		JsonObject prevSaiyan = prevQuest("saiyan_saga", 28);

		JsonObject[] namekDballObjs = new JsonObject[7];
		for (int i = 0; i < 7; i++)
			namekDballObjs[i] = objItem("dmz.quest.saiyan26.obj" + (i + 1), "dragonminez:dball" + (i + 1), 1);

//		writeLinearSaga(dir, s, c, prevSaiyan,
//
//		);
	}

	// ========================================================================================
	// Android Saga Quests (folder: saga_android)
	// ========================================================================================

	private static void createAndroidSagaQuests(Path questsDir) {
		Path dir = questsDir.resolve("saga_android");
		String s = "android_saga", c = "saga_android";
		JsonObject prevFrieza = prevQuest("frieza_saga", 31);

//		writeLinearSaga(dir, s, c, prevFrieza,
//
//		);
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
		JsonObject prevAndroid = prereqs("AND", condSaga("buu_saga", 40));
	}
}
