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
 *   "objectives": [ ... ],
 *   "rewards": [ ... ]
 * }
 * </pre>
 *
 * @since 2.1
 */
final class QuestDefaults {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private QuestDefaults() {} // utility class

	static void createDefaultQuestFiles(Path questsDir) {
		if (!ConfigManager.getServerConfig().getGameplay().getStoryModeEnabled()) return;
		if (!ConfigManager.getServerConfig().getGameplay().getCreateDefaultSagas()) return;

		createSaiyanSagaQuests(questsDir);
		createFriezaSagaQuests(questsDir);
		createAndroidSagaQuests(questsDir);
	}

	// ---- Helpers ----

	private static void writeQuest(Path dir, String filename, JsonObject quest) {
		Path file = dir.resolve(filename);
		if (Files.exists(file)) return;
		try {
			Files.createDirectories(dir);
			try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) { GSON.toJson(quest, w); }
		} catch (IOException e) { LogUtil.error(Env.COMMON, "Failed to create default quest file: {}", filename, e); }
	}

	/** Builds a saga quest in the unified schema. */
	private static JsonObject sagaQuest(int id, String title, String desc, String category,
										String chainSaga, int chainOrder, String chainNext,
										JsonObject prerequisites,
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

		JsonArray objArr = new JsonArray(); for (JsonObject o : objectives) objArr.add(o);
		q.add("objectives", objArr);
		JsonArray rewArr = new JsonArray(); for (JsonObject r : rewards) rewArr.add(r);
		q.add("rewards", rewArr);
		return q;
	}

	// ---- Objective helpers ----

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

	private static JsonObject objKill(String desc, String entity, int count, double hp, double melee, double ki) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "KILL"); o.addProperty("description", desc);
		o.addProperty("entity", entity); o.addProperty("count", count);
		o.addProperty("health", hp); o.addProperty("meleeDamage", melee); o.addProperty("kiDamage", ki);
		return o;
	}

	private static JsonObject objItem(String desc, String itemId, int count) {
		JsonObject o = new JsonObject();
		o.addProperty("type", "ITEM"); o.addProperty("description", desc);
		o.addProperty("item", itemId); o.addProperty("count", count);
		return o;
	}

	private static JsonObject objTalkTo(String desc, String targetNpcId) { //Maybe change for entity (from targetNpcId) later?
		JsonObject o = new JsonObject();
		o.addProperty("type", "TALK_TO"); o.addProperty("description", desc); o.addProperty("targetNpc", targetNpcId);
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

	// ========================================================================================
	// Saiyan Saga Quests (folder: saga_saiyan)
	// ========================================================================================

	private static void createSaiyanSagaQuests(Path questsDir) {
		Path dir = questsDir.resolve("saga_saiyan");
		String s = "saiyan_saga", c = "saga_saiyan";

		JsonObject[] dballObjs = new JsonObject[7];
		for (int i = 0; i < 7; i++) dballObjs[i] = objItem("dmz.quest.saiyan4.obj" + (i + 1), "dragonminez:dball" + (i + 1), 1);

		JsonObject afterSaibamen = prereqs("AND", condSaga(s, 5));
		JsonObject afterFirstSplit = prereqs("OR", condSaga(s, 6), condSaga(s, 7));
		JsonObject afterSecondSplit = prereqs("OR", condSaga(s, 10), condSaga(s, 11));

		writeQuest(dir, "01_find_roshi.json", sagaQuest(1, "dmz.quest.saiyan1.name", "dmz.quest.saiyan1.desc", c, s, 1, null, null, new JsonObject[]{ objStructure("dmz.quest.saiyan1.obj1", "dragonminez:roshi_house") }, new JsonObject[]{ rewTPS(500) }));
		writeQuest(dir, "02_defeat_raditz.json", sagaQuest(2, "dmz.quest.saiyan2.name", "dmz.quest.saiyan2.desc", c, s, 2, null, null, new JsonObject[]{ objBiome("dmz.quest.saiyan2.obj1", "minecraft:plains"), objKill("dmz.quest.saiyan2.obj2", "dragonminez:saga_raditz", 1, 400, 25, 50) }, new JsonObject[]{ rewTPS(1000) }));
		writeQuest(dir, "03_get_radar.json", sagaQuest(3, "dmz.quest.saiyan3.name", "dmz.quest.saiyan3.desc", c, s, 3, null, null, new JsonObject[]{ objItem("dmz.quest.saiyan3.obj1", "dragonminez:dball_radar", 1) }, new JsonObject[]{ rewTPS(1500) }));
		writeQuest(dir, "04_collect_dragon_balls.json", sagaQuest(4, "dmz.quest.saiyan4.name", "dmz.quest.saiyan4.desc", c, s, 4, null, null, dballObjs, new JsonObject[]{ rewTPS(2000) }));
		writeQuest(dir, "05_defeat_saibamen.json", sagaQuest(5, "dmz.quest.saiyan5.name", "dmz.quest.saiyan5.desc", c, s, 5, null, null, new JsonObject[]{ objBiome("dmz.quest.saiyan5.obj1", "minecraft:plains"), objKill("dmz.quest.saiyan5.obj2", "dragonminez:saga_saibaman1", 6, 400, 25, 50) }, new JsonObject[]{ rewTPS(2500) }));

		// First branch split
		writeQuest(dir, "06_defeat_nappa.json", sagaQuest(6, "dmz.quest.saiyan6.name", "dmz.quest.saiyan6.desc", c, s, 6, null, afterSaibamen, new JsonObject[]{ objBiome("dmz.quest.saiyan6.obj1", "minecraft:plains"), objKill("dmz.quest.saiyan6.obj2", "dragonminez:saga_nappa", 1, 750, 45, 100) }, new JsonObject[]{ rewTPS(3000) }));
		writeQuest(dir, "07_defeat_vegeta.json", sagaQuest(7, "dmz.quest.saiyan7.name", "dmz.quest.saiyan7.desc", c, s, 7, null, afterSaibamen, new JsonObject[]{ objBiome("dmz.quest.saiyan7.obj1", "dragonminez:rocky"), objKill("dmz.quest.saiyan7.obj2", "dragonminez:saga_vegeta", 1, 1200, 70, 150) }, new JsonObject[]{ rewTPS(3500) }));

		// Route quests after first split
		writeQuest(dir, "08_route_push_forward.json", sagaQuest(8, "dmz.quest.saiyan8.name", "dmz.quest.saiyan8.desc", c, s, 8, null, afterFirstSplit, new JsonObject[]{ objBiome("dmz.quest.saiyan8.obj1", "dragonminez:rocky"), objKill("dmz.quest.saiyan8.obj2", "dragonminez:saga_ozaruvegeta", 1, 2500, 140, 200) }, new JsonObject[]{ rewTPS(4000) }));
		writeQuest(dir, "09_route_regroup.json", sagaQuest(9, "dmz.quest.saiyan9.name", "dmz.quest.saiyan9.desc", c, s, 9, null, afterFirstSplit, new JsonObject[]{ objBiome("dmz.quest.saiyan9.obj1", "minecraft:plains"), objKill("dmz.quest.saiyan9.obj2", "dragonminez:saga_nappa", 1, 900, 55, 120) }, new JsonObject[]{ rewTPS(4200) }));

		// Second branch split
		writeQuest(dir, "10_second_split_mercy.json", sagaQuest(10, "dmz.quest.saiyan10.name", "dmz.quest.saiyan10.desc", c, s, 10, null, prereqs("OR", condSaga(s, 8), condSaga(s, 9)), new JsonObject[]{ objBiome("dmz.quest.saiyan10.obj1", "dragonminez:rocky"), objKill("dmz.quest.saiyan10.obj2", "dragonminez:saga_vegeta", 1, 1300, 80, 160) }, new JsonObject[]{ rewTPS(4400) }));
		writeQuest(dir, "11_second_split_wrath.json", sagaQuest(11, "dmz.quest.saiyan11.name", "dmz.quest.saiyan11.desc", c, s, 11, null, prereqs("OR", condSaga(s, 8), condSaga(s, 9)), new JsonObject[]{ objBiome("dmz.quest.saiyan11.obj1", "dragonminez:rocky"), objKill("dmz.quest.saiyan11.obj2", "dragonminez:saga_ozaruvegeta", 1, 3000, 170, 240) }, new JsonObject[]{ rewTPS(4600) }));

		// Ending arc
		writeQuest(dir, "12_final_battle.json", sagaQuest(12, "dmz.quest.saiyan12.name", "dmz.quest.saiyan12.desc", c, s, 12, null, afterSecondSplit, new JsonObject[]{ objBiome("dmz.quest.saiyan12.obj1", "dragonminez:rocky"), objKill("dmz.quest.saiyan12.obj2", "dragonminez:saga_ozaruvegeta", 1, 3200, 180, 260) }, new JsonObject[]{ rewTPS(4800), rewItem("dragonminez:saiyan_ship", 1) }));
		writeQuest(dir, "13_travel_to_namek.json", sagaQuest(13, "dmz.quest.saiyan13.name", "dmz.quest.saiyan13.desc", c, s, 13, null, prereqs("AND", condSaga(s, 12)), new JsonObject[]{ objBiome("dmz.quest.saiyan13.obj1", "dragonminez:ajissa_plains") }, new JsonObject[]{ rewTPS(5000) }));
	}

	// ========================================================================================
	// Frieza Saga Quests (folder: saga_frieza)
	// ========================================================================================

	private static void createFriezaSagaQuests(Path questsDir) {
		Path dir = questsDir.resolve("saga_frieza");
		String s = "frieza_saga", c = "saga_frieza";
		JsonObject prevSaiyan = prereqs("AND", condSaga("saiyan_saga", 13));
		JsonObject afterGinyuForceStart = prereqs("AND", condSaga(s, 7));
		JsonObject afterGinyuForceSplit = prereqs("OR", condSaga(s, 8), condSaga(s, 9));
		JsonObject afterFriezaFormSplit = prereqs("OR", condSaga(s, 12), condSaga(s, 13));

		writeQuest(dir, "01_defeat_cui.json", sagaQuest(1, "dmz.quest.frieza1.name", "dmz.quest.frieza1.desc", c, s, 1, null, prevSaiyan, new JsonObject[]{ objBiome("dmz.quest.frieza1.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza1.obj2", "dragonminez:saga_cui", 1, 1200, 70, 150) }, new JsonObject[]{ rewTPS(5000) }));
		writeQuest(dir, "02_find_village.json", sagaQuest(2, "dmz.quest.frieza2.name", "dmz.quest.frieza2.desc", c, s, 2, null, null, new JsonObject[]{ objStructure("dmz.quest.frieza2.obj1", "dragonminez:village_ajissa") }, new JsonObject[]{ rewTPS(5500) }));
		writeQuest(dir, "03_defeat_dodoria.json", sagaQuest(3, "dmz.quest.frieza3.name", "dmz.quest.frieza3.desc", c, s, 3, null, null, new JsonObject[]{ objBiome("dmz.quest.frieza3.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza3.obj2", "dragonminez:saga_dodoria", 1, 1400, 80, 180) }, new JsonObject[]{ rewTPS(6000) }));
		writeQuest(dir, "04_defeat_zarbon.json", sagaQuest(4, "dmz.quest.frieza4.name", "dmz.quest.frieza4.desc", c, s, 4, null, null, new JsonObject[]{ objBiome("dmz.quest.frieza4.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza4.obj2", "dragonminez:saga_zarbont1", 1, 1500, 85, 200) }, new JsonObject[]{ rewTPS(6500) }));
		writeQuest(dir, "05_defeat_vegeta_namek.json", sagaQuest(5, "dmz.quest.frieza5.name", "dmz.quest.frieza5.desc", c, s, 5, null, null, new JsonObject[]{ objBiome("dmz.quest.frieza5.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza5.obj2", "dragonminez:saga_vegeta_namek", 1, 1600, 90, 200) }, new JsonObject[]{ rewTPS(7000) }));
		writeQuest(dir, "06_defeat_guldo.json", sagaQuest(6, "dmz.quest.frieza6.name", "dmz.quest.frieza6.desc", c, s, 6, null, null, new JsonObject[]{ objBiome("dmz.quest.frieza6.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza6.obj2", "dragonminez:saga_guldo", 1, 800, 50, 90) }, new JsonObject[]{ rewTPS(8000) }));
		writeQuest(dir, "07_defeat_recoome.json", sagaQuest(7, "dmz.quest.frieza7.name", "dmz.quest.frieza7.desc", c, s, 7, null, null, new JsonObject[]{ objBiome("dmz.quest.frieza7.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza7.obj2", "dragonminez:saga_recoome", 1, 2000, 110, 180) }, new JsonObject[]{ rewTPS(8500) }));
		writeQuest(dir, "08_defeat_burter.json", sagaQuest(8, "dmz.quest.frieza8.name", "dmz.quest.frieza8.desc", c, s, 8, null, afterGinyuForceStart, new JsonObject[]{ objBiome("dmz.quest.frieza8.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza8.obj2", "dragonminez:saga_burter", 1, 2000, 110, 180) }, new JsonObject[]{ rewTPS(9000) }));
		writeQuest(dir, "09_defeat_jeice.json", sagaQuest(9, "dmz.quest.frieza9.name", "dmz.quest.frieza9.desc", c, s, 9, null, afterGinyuForceStart, new JsonObject[]{ objBiome("dmz.quest.frieza9.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza9.obj2", "dragonminez:saga_jeice", 1, 2000, 110, 180) }, new JsonObject[]{ rewTPS(9500) }));
		writeQuest(dir, "10_defeat_ginyu.json", sagaQuest(10, "dmz.quest.frieza10.name", "dmz.quest.frieza10.desc", c, s, 10, null, afterGinyuForceSplit, new JsonObject[]{ objBiome("dmz.quest.frieza10.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza10.obj2", "dragonminez:saga_ginyu", 1, 3000, 160, 260) }, new JsonObject[]{ rewTPS(10000) }));
		writeQuest(dir, "11_defeat_ginyu_goku.json", sagaQuest(11, "dmz.quest.frieza11.name", "dmz.quest.frieza11.desc", c, s, 11, null, prereqs("AND", condSaga(s, 10)), new JsonObject[]{ objBiome("dmz.quest.frieza11.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza11.obj2", "dragonminez:saga_ginyu_goku", 1, 1500, 85, 140) }, new JsonObject[]{ rewTPS(11000) }));
		writeQuest(dir, "12_defeat_frieza_first.json", sagaQuest(12, "dmz.quest.frieza12.name", "dmz.quest.frieza12.desc", c, s, 12, null, prereqs("AND", condSaga(s, 11)), new JsonObject[]{ objBiome("dmz.quest.frieza12.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza12.obj2", "dragonminez:saga_frieza_first", 1, 4000, 200, 350) }, new JsonObject[]{ rewTPS(12000) }));
		writeQuest(dir, "13_defeat_frieza_third.json", sagaQuest(13, "dmz.quest.frieza13.name", "dmz.quest.frieza13.desc", c, s, 13, null, prereqs("AND", condSaga(s, 11)), new JsonObject[]{ objBiome("dmz.quest.frieza13.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza13.obj2", "dragonminez:saga_frieza_third", 1, 8000, 400, 650) }, new JsonObject[]{ rewTPS(13000) }));
		writeQuest(dir, "14_defeat_frieza_base.json", sagaQuest(14, "dmz.quest.frieza14.name", "dmz.quest.frieza14.desc", c, s, 14, null, afterFriezaFormSplit, new JsonObject[]{ objBiome("dmz.quest.frieza14.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza14.obj2", "dragonminez:saga_frieza_base", 1, 12000, 550, 900) }, new JsonObject[]{ rewTPS(14000) }));
		writeQuest(dir, "15_defeat_frieza_fp.json", sagaQuest(15, "dmz.quest.frieza15.name", "dmz.quest.frieza15.desc", c, s, 15, null, prereqs("AND", condSaga(s, 14)), new JsonObject[]{ objBiome("dmz.quest.frieza15.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza15.obj2", "dragonminez:saga_frieza_fp", 1, 16000, 750, 1200) }, new JsonObject[]{ rewTPS(15000) }));
		writeQuest(dir, "16_return_to_earth.json", sagaQuest(16, "dmz.quest.frieza16.name", "dmz.quest.frieza16.desc", c, s, 16, null, prereqs("AND", condSaga(s, 15)), new JsonObject[]{ objBiome("dmz.quest.frieza16.obj1", "dragonminez:rocky") }, new JsonObject[]{ rewTPS(5000) }));
	}

	// ========================================================================================
	// Android Saga Quests (folder: saga_android)
	// ========================================================================================

	private static void createAndroidSagaQuests(Path questsDir) {
		Path dir = questsDir.resolve("saga_android");
		String s = "android_saga", c = "saga_android";
		JsonObject prevFrieza = prereqs("AND", condSaga("frieza_saga", 16));
		JsonObject afterAndroidSplit = prereqs("OR", condSaga(s, 13), condSaga(s, 14));
		JsonObject afterAndroidFinishSplit = prereqs("OR", condSaga(s, 17), condSaga(s, 18));

		writeQuest(dir, "01_defeat_soldiers.json", sagaQuest(1, "dmz.quest.android1.name", "dmz.quest.android1.desc", c, s, 1, null, prevFrieza, new JsonObject[]{ objBiome("dmz.quest.android1.obj1", "dragonminez:rocky"), objKill("dmz.quest.android1.obj2", "dragonminez:saga_friezasoldier01", 15, 200, 15, 20) }, new JsonObject[]{ rewTPS(18000) }));
		writeQuest(dir, "02_defeat_mecha_frieza.json", sagaQuest(2, "dmz.quest.android2.name", "dmz.quest.android2.desc", c, s, 2, null, null, new JsonObject[]{ objBiome("dmz.quest.android2.obj1", "dragonminez:rocky"), objKill("dmz.quest.android2.obj2", "dragonminez:saga_mecha_frieza", 1, 17000, 800, 1300), objKill("dmz.quest.android2.obj3", "dragonminez:saga_king_cold", 1, 8000, 400, 650) }, new JsonObject[]{ rewTPS(19000) }));
		writeQuest(dir, "03_defeat_goku_yardrat.json", sagaQuest(3, "dmz.quest.android3.name", "dmz.quest.android3.desc", c, s, 3, null, null, new JsonObject[]{ objBiome("dmz.quest.android3.obj1", "dragonminez:rocky"), objKill("dmz.quest.android3.obj2", "dragonminez:saga_goku_yardrat", 1, 20000, 900, 1500) }, new JsonObject[]{ rewTPS(20000) }));
		writeQuest(dir, "04_find_goku_house.json", sagaQuest(4, "dmz.quest.android4.name", "dmz.quest.android4.desc", c, s, 4, null, null, new JsonObject[]{ objStructure("dmz.quest.android4.obj1", "dragonminez:goku_house") }, new JsonObject[]{ rewTPS(5000) }));
		writeQuest(dir, "05_defeat_a19.json", sagaQuest(5, "dmz.quest.android5.name", "dmz.quest.android5.desc", c, s, 5, null, null, new JsonObject[]{ objBiome("dmz.quest.android5.obj1", "dragonminez:rocky"), objKill("dmz.quest.android5.obj2", "dragonminez:saga_a19", 1, 22000, 1000, 1600) }, new JsonObject[]{ rewTPS(21000) }));
		writeQuest(dir, "06_defeat_drgero.json", sagaQuest(6, "dmz.quest.android6.name", "dmz.quest.android6.desc", c, s, 6, null, null, new JsonObject[]{ objBiome("dmz.quest.android6.obj1", "dragonminez:rocky"), objKill("dmz.quest.android6.obj2", "dragonminez:saga_drgero", 1, 18000, 850, 1350) }, new JsonObject[]{ rewTPS(22000) }));
		writeQuest(dir, "07_defeat_a17_a18.json", sagaQuest(7, "dmz.quest.android7.name", "dmz.quest.android7.desc", c, s, 7, null, null, new JsonObject[]{ objBiome("dmz.quest.android7.obj1", "#minecraft:is_mountain"), objKill("dmz.quest.android7.obj2", "dragonminez:saga_a17", 1, 30000, 1400, 2200), objKill("dmz.quest.android7.obj3", "dragonminez:saga_a18", 1, 30000, 1400, 2200) }, new JsonObject[]{ rewTPS(23000) }));
		writeQuest(dir, "08_defeat_cell_imperfect.json", sagaQuest(8, "dmz.quest.android8.name", "dmz.quest.android8.desc", c, s, 8, null, null, new JsonObject[]{ objBiome("dmz.quest.android8.obj1", "minecraft:plains"), objKill("dmz.quest.android8.obj2", "dragonminez:saga_cell_imperfect", 1, 28000, 1300, 2000) }, new JsonObject[]{ rewTPS(24000) }));
		writeQuest(dir, "09_defeat_piccolo_a17.json", sagaQuest(9, "dmz.quest.android9.name", "dmz.quest.android9.desc", c, s, 9, null, null, new JsonObject[]{ objBiome("dmz.quest.android9.obj1", "minecraft:plains"), objKill("dmz.quest.android9.obj2", "dragonminez:saga_piccolo_kami", 1, 35000, 1600, 2600), objKill("dmz.quest.android9.obj3", "dragonminez:saga_a17", 1, 30000, 1400, 2200) }, new JsonObject[]{ rewTPS(25000) }));
		writeQuest(dir, "10_defeat_a16.json", sagaQuest(10, "dmz.quest.android10.name", "dmz.quest.android10.desc", c, s, 10, null, null, new JsonObject[]{ objBiome("dmz.quest.android10.obj1", "minecraft:plains"), objKill("dmz.quest.android10.obj2", "dragonminez:saga_a16", 1, 32000, 1500, 2400) }, new JsonObject[]{ rewTPS(26000) }));
		writeQuest(dir, "11_find_timechamber.json", sagaQuest(11, "dmz.quest.android11.name", "dmz.quest.android11.desc", c, s, 11, null, null, new JsonObject[]{ objStructure("dmz.quest.android11.obj1", "dragonminez:timechamber") }, new JsonObject[]{ rewTPS(10000) }));
		writeQuest(dir, "12_defeat_cell_semi_a18.json", sagaQuest(12, "dmz.quest.android12.name", "dmz.quest.android12.desc", c, s, 12, null, null, new JsonObject[]{ objBiome("dmz.quest.android12.obj1", "minecraft:plains"), objKill("dmz.quest.android12.obj2", "dragonminez:saga_cell_semiperfect", 1, 40000, 1800, 3000), objKill("dmz.quest.android12.obj3", "dragonminez:saga_a18", 1, 30000, 1400, 2200) }, new JsonObject[]{ rewTPS(27000) }));
		writeQuest(dir, "13_defeat_super_vegeta.json", sagaQuest(13, "dmz.quest.android13.name", "dmz.quest.android13.desc", c, s, 13, null, prereqs("AND", condSaga(s, 12)), new JsonObject[]{ objBiome("dmz.quest.android13.obj1", "minecraft:plains"), objKill("dmz.quest.android13.obj2", "dragonminez:saga_super_vegeta", 1, 45000, 2100, 3400) }, new JsonObject[]{ rewTPS(28000) }));
		writeQuest(dir, "14_defeat_trunks_ssj.json", sagaQuest(14, "dmz.quest.android14.name", "dmz.quest.android14.desc", c, s, 14, null, prereqs("AND", condSaga(s, 12)), new JsonObject[]{ objBiome("dmz.quest.android14.obj1", "minecraft:plains"), objKill("dmz.quest.android14.obj2", "dragonminez:saga_trunks_ssj", 1, 42000, 1950, 3200) }, new JsonObject[]{ rewTPS(29000) }));
		writeQuest(dir, "15_defeat_cell_perfect.json", sagaQuest(15, "dmz.quest.android15.name", "dmz.quest.android15.desc", c, s, 15, null, afterAndroidSplit, new JsonObject[]{ objBiome("dmz.quest.android15.obj1", "minecraft:plains"), objKill("dmz.quest.android15.obj2", "dragonminez:saga_cell_perfect", 1, 60000, 2800, 4500) }, new JsonObject[]{ rewTPS(30000) }));
		writeQuest(dir, "16_defeat_gohan_ssj.json", sagaQuest(16, "dmz.quest.android16.name", "dmz.quest.android16.desc", c, s, 16, null, prereqs("AND", condSaga(s, 15)), new JsonObject[]{ objBiome("dmz.quest.android16.obj1", "minecraft:plains"), objKill("dmz.quest.android16.obj2", "dragonminez:saga_gohan_ssj", 1, 55000, 2500, 4000) }, new JsonObject[]{ rewTPS(31000) }));
		writeQuest(dir, "17_defeat_cell_jrs.json", sagaQuest(17, "dmz.quest.android17.name", "dmz.quest.android17.desc", c, s, 17, null, prereqs("AND", condSaga(s, 16)), new JsonObject[]{ objBiome("dmz.quest.android17.obj1", "minecraft:plains"), objKill("dmz.quest.android17.obj2", "dragonminez:saga_cell_jr", 6, 42500, 1950, 3800) }, new JsonObject[]{ rewTPS(32000) }));
		writeQuest(dir, "18_defeat_cell_super.json", sagaQuest(18, "dmz.quest.android18.name", "dmz.quest.android18.desc", c, s, 18, null, prereqs("AND", condSaga(s, 16)), new JsonObject[]{ objBiome("dmz.quest.android18.obj1", "minecraft:plains"), objKill("dmz.quest.android18.obj2", "dragonminez:saga_cell_superperfect", 1, 80000, 3800, 6000) }, new JsonObject[]{ rewTPS(35000) }));
		writeQuest(dir, "19_find_kamilookout.json", sagaQuest(19, "dmz.quest.android19.name", "dmz.quest.android19.desc", c, s, 19, null, afterAndroidFinishSplit, new JsonObject[]{ objStructure("dmz.quest.android19.obj1", "dragonminez:kamilookout") }, new JsonObject[]{ rewTPS(10000) }));
	}
}
