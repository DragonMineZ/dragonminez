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
		o.addProperty("targetNpc", targetNpcId);
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

	private static JsonObject condSaga(String sagaId, int questId) {
		JsonObject c = new JsonObject();
		c.addProperty("type", "SAGA_QUEST");
		c.addProperty("sagaId", sagaId);
		c.addProperty("questId", questId);
		return c;
	}

	private record QuestStep(int id, String filename, String title, String desc, JsonObject[] objectives, JsonObject[] rewards) {
	}

	private static QuestStep step(String sagaKey, int id, String filename, JsonObject[] objectives, JsonObject... rewards) {
		return new QuestStep(
				id,
				filename,
				"dmz.quest." + sagaKey + id + ".name",
				"dmz.quest." + sagaKey + id + ".desc",
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
					sagaQuest(step.id(), step.title(), step.desc(), category, sagaId, step.id(), null, prereq, step.objectives(), step.rewards())
			);
		}
	}

	// ========================================================================================
	// Saiyan Saga Quests (folder: saga_saiyan)
	// ========================================================================================

	private static void createSaiyanSagaQuests(Path questsDir) {
		Path dir = questsDir.resolve("saga_saiyan");
		String s = "saiyan_saga", c = "saga_saiyan";

		JsonObject[] namekDballObjs = new JsonObject[7];
		for (int i = 0; i < 7; i++)
			namekDballObjs[i] = objItem("dmz.quest.saiyan26.obj" + (i + 1), "dragonminez:dball" + (i + 1), 1);

		writeLinearSaga(dir, s, c, null,
				step("saiyan", 1, "01_find_roshi.json", new JsonObject[]{objStructure("dmz.quest.saiyan1.obj1", "dragonminez:roshi_house")}, rewTPS(500)),
				step("saiyan", 2, "02_train_with_krillin.json", new JsonObject[]{objBiome("dmz.quest.saiyan2.obj1", "minecraft:plains"), objKill("dmz.quest.saiyan2.obj2", "dragonminez:shadow_dummy", 4, 220, 12, 20)}, rewTPS(800)),
				step("saiyan", 3, "03_detect_raditz.json", new JsonObject[]{objBiome("dmz.quest.saiyan3.obj1", "minecraft:plains"), objKill("dmz.quest.saiyan3.obj2", "dragonminez:saga_raditz", 1, 350, 22, 45)}, rewTPS(1200)),
				step("saiyan", 4, "04_defeat_raditz.json", new JsonObject[]{objBiome("dmz.quest.saiyan4.obj1", "minecraft:plains"), objKill("dmz.quest.saiyan4.obj2", "dragonminez:saga_raditz", 1, 500, 32, 65)}, rewTPS(1500)),
				step("saiyan", 5, "05_get_radar.json", new JsonObject[]{objItem("dmz.quest.saiyan5.obj1", "dragonminez:dball_radar", 1)}, rewTPS(1800)),
				step("saiyan", 7, "07_scout_saiyan_landing.json", new JsonObject[]{objBiome("dmz.quest.saiyan7.obj1", "minecraft:plains"), objKill("dmz.quest.saiyan7.obj2", "dragonminez:saga_saibaman1", 3, 340, 22, 40)}, rewTPS(2500)),
				step("saiyan", 8, "08_defeat_saibamen_wave1.json", new JsonObject[]{objBiome("dmz.quest.saiyan8.obj1", "minecraft:plains"), objKill("dmz.quest.saiyan8.obj2", "dragonminez:saga_saibaman1", 6, 420, 28, 55)}, rewTPS(2800)),
				step("saiyan", 9, "09_defeat_saibamen_wave2.json", new JsonObject[]{objBiome("dmz.quest.saiyan9.obj1", "minecraft:plains"), objKill("dmz.quest.saiyan9.obj2", "dragonminez:saga_saibaman2", 6, 460, 30, 60)}, rewTPS(3200)),
				step("saiyan", 10, "10_hold_against_nappa.json", new JsonObject[]{objBiome("dmz.quest.saiyan10.obj1", "minecraft:plains"), objKill("dmz.quest.saiyan10.obj2", "dragonminez:saga_nappa", 1, 800, 50, 110)}, rewTPS(3600)),
				step("saiyan", 11, "11_break_nappa_guard.json", new JsonObject[]{objBiome("dmz.quest.saiyan11.obj1", "dragonminez:rocky"), objKill("dmz.quest.saiyan11.obj2", "dragonminez:saga_nappa", 1, 950, 60, 130)}, rewTPS(4000)),
				step("saiyan", 12, "12_face_vegeta.json", new JsonObject[]{objBiome("dmz.quest.saiyan12.obj1", "dragonminez:rocky"), objKill("dmz.quest.saiyan12.obj2", "dragonminez:saga_vegeta", 1, 1200, 70, 150)}, rewTPS(4400)),
				step("saiyan", 13, "13_survive_ozaru_assault.json", new JsonObject[]{objBiome("dmz.quest.saiyan13.obj1", "dragonminez:rocky"), objKill("dmz.quest.saiyan13.obj2", "dragonminez:saga_ozaruvegeta", 1, 2500, 140, 200)}, rewTPS(4700)),
				step("saiyan", 14, "14_counterattack_ozaru.json", new JsonObject[]{objBiome("dmz.quest.saiyan14.obj1", "dragonminez:rocky"), objKill("dmz.quest.saiyan14.obj2", "dragonminez:saga_ozaruvegeta", 1, 3000, 170, 240)}, rewTPS(5000)),
				step("saiyan", 15, "15_last_stand_vegeta.json", new JsonObject[]{objBiome("dmz.quest.saiyan15.obj1", "dragonminez:rocky"), objKill("dmz.quest.saiyan15.obj2", "dragonminez:saga_vegeta", 1, 1350, 84, 170)}, rewTPS(5400)),
				step("saiyan", 16, "16_secure_battlefield.json", new JsonObject[]{objBiome("dmz.quest.saiyan16.obj1", "minecraft:plains"), objKill("dmz.quest.saiyan16.obj2", "dragonminez:saga_saibaman3", 8, 500, 32, 70)}, rewTPS(5800)),
				step("saiyan", 17, "17_recover_at_kame_house.json", new JsonObject[]{objStructure("dmz.quest.saiyan17.obj1", "dragonminez:roshi_house")}, rewTPS(6200)),
				step("saiyan", 18, "18_prepare_saiyan_ship.json", new JsonObject[]{objItem("dmz.quest.saiyan18.obj1", "dragonminez:dball_radar", 1)}, rewTPS(6500)),
				step("saiyan", 19, "19_launch_for_namek.json", new JsonObject[]{objBiome("dmz.quest.saiyan19.obj1", "dragonminez:rocky")}, rewTPS(7000), rewItem("dragonminez:saiyan_ship", 1)),
				step("saiyan", 20, "20_land_on_namek.json", new JsonObject[]{objBiome("dmz.quest.saiyan20.obj1", "dragonminez:ajissa_plains")}, rewTPS(7400)),
				step("saiyan", 21, "21_clear_namek_patrols.json", new JsonObject[]{objBiome("dmz.quest.saiyan21.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.saiyan21.obj2", "dragonminez:saga_friezasoldier01", 8, 450, 30, 70)}, rewTPS(7800)),
				step("saiyan", 22, "22_push_back_elite_patrols.json", new JsonObject[]{objBiome("dmz.quest.saiyan22.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.saiyan22.obj2", "dragonminez:saga_friezasoldier02", 8, 520, 35, 80)}, rewTPS(8200)),
				step("saiyan", 23, "23_secure_namek_village.json", new JsonObject[]{objStructure("dmz.quest.saiyan23.obj1", "dragonminez:village_ajissa")}, rewTPS(8600)),
				step("saiyan", 24, "24_break_soldier_reinforcements.json", new JsonObject[]{objBiome("dmz.quest.saiyan24.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.saiyan24.obj2", "dragonminez:saga_friezasoldier03", 10, 560, 40, 90)}, rewTPS(9000)),
				step("saiyan", 25, "25_survive_vegeta_namek_clash.json", new JsonObject[]{objBiome("dmz.quest.saiyan25.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.saiyan25.obj2", "dragonminez:saga_vegeta_namek", 1, 1700, 95, 210)}, rewTPS(9400)),
				step("saiyan", 26, "26_collect_namek_dragon_balls.json", namekDballObjs, rewTPS(9800)),
				step("saiyan", 27, "27_hold_until_ginyu_arrives.json", new JsonObject[]{objBiome("dmz.quest.saiyan27.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.saiyan27.obj2", "dragonminez:saga_friezasoldier01", 12, 620, 45, 100)}, rewTPS(10200)),
				step("saiyan", 28, "28_ready_for_frieza_command.json", new JsonObject[]{objBiome("dmz.quest.saiyan28.obj1", "dragonminez:ajissa_plains")}, rewTPS(11000))
		);
	}

	// ========================================================================================
	// Frieza Saga Quests (folder: saga_frieza)
	// ========================================================================================

	private static void createFriezaSagaQuests(Path questsDir) {
		Path dir = questsDir.resolve("saga_frieza");
		String s = "frieza_saga", c = "saga_frieza";
		JsonObject prevSaiyan = prevQuest("saiyan_saga", 28);

		writeLinearSaga(dir, s, c, prevSaiyan,
				step("frieza", 1, "01_secure_namek_landing.json", new JsonObject[]{objBiome("dmz.quest.frieza1.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza1.obj2", "dragonminez:saga_friezasoldier01", 10, 700, 45, 90)}, rewTPS(6000)),
				step("frieza", 2, "02_defeat_cui.json", new JsonObject[]{objBiome("dmz.quest.frieza2.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza2.obj2", "dragonminez:saga_cui", 1, 1200, 70, 150)}, rewTPS(6800)),
				step("frieza", 3, "03_find_village.json", new JsonObject[]{objStructure("dmz.quest.frieza3.obj1", "dragonminez:village_ajissa")}, rewTPS(7200)),
				step("frieza", 4, "04_defend_namekians.json", new JsonObject[]{objBiome("dmz.quest.frieza4.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza4.obj2", "dragonminez:saga_friezasoldier02", 10, 850, 55, 110), objKill("dmz.quest.frieza4.obj3", "dragonminez:saga_friezasoldier03", 6, 950, 62, 120)}, rewTPS(7600)),
				step("frieza", 5, "05_defeat_dodoria.json", new JsonObject[]{objBiome("dmz.quest.frieza5.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza5.obj2", "dragonminez:saga_dodoria", 1, 1450, 85, 185)}, rewTPS(8200)),
				step("frieza", 6, "06_defeat_zarbon.json", new JsonObject[]{objBiome("dmz.quest.frieza6.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza6.obj2", "dragonminez:saga_zarbon", 1, 1500, 85, 190)}, rewTPS(8600)),
				step("frieza", 7, "07_survive_zarbon_transformed.json", new JsonObject[]{objBiome("dmz.quest.frieza7.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza7.obj2", "dragonminez:saga_zarbont1", 1, 1700, 95, 220)}, rewTPS(9200)),
				step("frieza", 8, "08_clash_with_vegeta_namek.json", new JsonObject[]{objBiome("dmz.quest.frieza8.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza8.obj2", "dragonminez:saga_vegeta_namek", 1, 1800, 100, 230)}, rewTPS(9800)),
				step("frieza", 9, "09_defeat_guldo.json", new JsonObject[]{objBiome("dmz.quest.frieza9.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza9.obj2", "dragonminez:saga_guldo", 1, 900, 55, 100)}, rewTPS(10400)),
				step("frieza", 10, "10_defeat_recoome.json", new JsonObject[]{objBiome("dmz.quest.frieza10.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza10.obj2", "dragonminez:saga_recoome", 1, 2200, 120, 200)}, rewTPS(11000)),
				step("frieza", 11, "11_defeat_burter.json", new JsonObject[]{objBiome("dmz.quest.frieza11.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza11.obj2", "dragonminez:saga_burter", 1, 2200, 120, 200)}, rewTPS(11600)),
				step("frieza", 12, "12_defeat_jeice.json", new JsonObject[]{objBiome("dmz.quest.frieza12.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza12.obj2", "dragonminez:saga_jeice", 1, 2200, 120, 200)}, rewTPS(12200)),
				step("frieza", 13, "13_defeat_ginyu.json", new JsonObject[]{objBiome("dmz.quest.frieza13.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza13.obj2", "dragonminez:saga_ginyu", 1, 3200, 170, 280)}, rewTPS(12800)),
				step("frieza", 14, "14_defeat_ginyu_goku.json", new JsonObject[]{objBiome("dmz.quest.frieza14.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza14.obj2", "dragonminez:saga_ginyu_goku", 1, 1800, 95, 155)}, rewTPS(13400)),
				step("frieza", 15, "15_repel_frigid_reinforcements.json", new JsonObject[]{objBiome("dmz.quest.frieza15.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza15.obj2", "dragonminez:saga_friezasoldier03", 14, 1200, 75, 145)}, rewTPS(14000)),
				step("frieza", 16, "16_defeat_frieza_first.json", new JsonObject[]{objBiome("dmz.quest.frieza16.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza16.obj2", "dragonminez:saga_frieza_first", 1, 4200, 210, 360)}, rewTPS(15000)),
				step("frieza", 17, "17_defeat_frieza_second.json", new JsonObject[]{objBiome("dmz.quest.frieza17.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza17.obj2", "dragonminez:saga_frieza_second", 1, 6200, 300, 520)}, rewTPS(16000)),
				step("frieza", 18, "18_defeat_frieza_third.json", new JsonObject[]{objBiome("dmz.quest.frieza18.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza18.obj2", "dragonminez:saga_frieza_third", 1, 8500, 420, 690)}, rewTPS(17000)),
				step("frieza", 19, "19_defeat_frieza_base.json", new JsonObject[]{objBiome("dmz.quest.frieza19.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza19.obj2", "dragonminez:saga_frieza_base", 1, 12500, 580, 940)}, rewTPS(18000)),
				step("frieza", 20, "20_withstand_frieza_barrage.json", new JsonObject[]{objBiome("dmz.quest.frieza20.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza20.obj2", "dragonminez:saga_frieza_base", 1, 14000, 650, 1050)}, rewTPS(19000)),
				step("frieza", 21, "21_defeat_frieza_full_power.json", new JsonObject[]{objBiome("dmz.quest.frieza21.obj1", "dragonminez:ajissa_plains"), objKill("dmz.quest.frieza21.obj2", "dragonminez:saga_frieza_fp", 1, 17000, 800, 1260)}, rewTPS(20000)),
				step("frieza", 22, "22_escape_namek_collapse.json", new JsonObject[]{objBiome("dmz.quest.frieza22.obj1", "dragonminez:ajissa_plains")}, rewTPS(9000)),
				step("frieza", 23, "23_return_to_earth.json", new JsonObject[]{objBiome("dmz.quest.frieza23.obj1", "dragonminez:rocky")}, rewTPS(9500)),
				step("frieza", 24, "24_intercept_mecha_frieza.json", new JsonObject[]{objBiome("dmz.quest.frieza24.obj1", "dragonminez:rocky"), objKill("dmz.quest.frieza24.obj2", "dragonminez:saga_mecha_frieza", 1, 18000, 840, 1360)}, rewTPS(20500)),
				step("frieza", 25, "25_defeat_king_cold.json", new JsonObject[]{objBiome("dmz.quest.frieza25.obj1", "dragonminez:rocky"), objKill("dmz.quest.frieza25.obj2", "dragonminez:saga_king_cold", 1, 9000, 440, 700)}, rewTPS(21000)),
				step("frieza", 26, "26_meet_future_ally.json", new JsonObject[]{objStructure("dmz.quest.frieza26.obj1", "dragonminez:goku_house")}, rewTPS(10000)),
				step("frieza", 27, "27_defend_earth_from_remnants.json", new JsonObject[]{objBiome("dmz.quest.frieza27.obj1", "dragonminez:rocky"), objKill("dmz.quest.frieza27.obj2", "dragonminez:saga_friezasoldier01", 15, 1300, 80, 150)}, rewTPS(21600)),
				step("frieza", 28, "28_enter_timechamber_training.json", new JsonObject[]{objStructure("dmz.quest.frieza28.obj1", "dragonminez:timechamber")}, rewTPS(11000)),
				step("frieza", 29, "29_three_year_preparation.json", new JsonObject[]{objBiome("dmz.quest.frieza29.obj1", "minecraft:plains"), objKill("dmz.quest.frieza29.obj2", "dragonminez:shadow_dummy", 14, 1500, 95, 170)}, rewTPS(22000)),
				step("frieza", 30, "30_androids_approach.json", new JsonObject[]{objBiome("dmz.quest.frieza30.obj1", "dragonminez:rocky")}, rewTPS(11500)),
				step("frieza", 31, "31_report_to_kami_lookout.json", new JsonObject[]{objStructure("dmz.quest.frieza31.obj1", "dragonminez:kamilookout")}, rewTPS(12000))
		);
	}

	// ========================================================================================
	// Android Saga Quests (folder: saga_android)
	// ========================================================================================

	private static void createAndroidSagaQuests(Path questsDir) {
		Path dir = questsDir.resolve("saga_android");
		String s = "android_saga", c = "saga_android";
		JsonObject prevFrieza = prevQuest("frieza_saga", 31);

		writeLinearSaga(dir, s, c, prevFrieza,
				step("android", 1, "01_defeat_soldiers.json", new JsonObject[]{objBiome("dmz.quest.android1.obj1", "dragonminez:rocky"), objKill("dmz.quest.android1.obj2", "dragonminez:saga_friezasoldier01", 15, 260, 18, 24)}, rewTPS(18000)),
				step("android", 2, "02_defeat_mecha_frieza.json", new JsonObject[]{objBiome("dmz.quest.android2.obj1", "dragonminez:rocky"), objKill("dmz.quest.android2.obj2", "dragonminez:saga_mecha_frieza", 1, 17000, 800, 1300)}, rewTPS(19000)),
				step("android", 3, "03_defeat_king_cold.json", new JsonObject[]{objBiome("dmz.quest.android3.obj1", "dragonminez:rocky"), objKill("dmz.quest.android3.obj2", "dragonminez:saga_king_cold", 1, 9000, 430, 700)}, rewTPS(19500)),
				step("android", 4, "04_defeat_goku_yardrat.json", new JsonObject[]{objBiome("dmz.quest.android4.obj1", "dragonminez:rocky"), objKill("dmz.quest.android4.obj2", "dragonminez:saga_goku_yardrat", 1, 20000, 900, 1500)}, rewTPS(20000)),
				step("android", 5, "05_find_goku_house.json", new JsonObject[]{objStructure("dmz.quest.android5.obj1", "dragonminez:goku_house")}, rewTPS(9000)),
				step("android", 6, "06_three_year_training.json", new JsonObject[]{objBiome("dmz.quest.android6.obj1", "minecraft:plains"), objKill("dmz.quest.android6.obj2", "dragonminez:shadow_dummy", 16, 1800, 110, 190)}, rewTPS(20500)),
				step("android", 7, "07_defeat_a19.json", new JsonObject[]{objBiome("dmz.quest.android7.obj1", "dragonminez:rocky"), objKill("dmz.quest.android7.obj2", "dragonminez:saga_a19", 1, 22000, 1000, 1600)}, rewTPS(21000)),
				step("android", 8, "08_defeat_drgero.json", new JsonObject[]{objBiome("dmz.quest.android8.obj1", "dragonminez:rocky"), objKill("dmz.quest.android8.obj2", "dragonminez:saga_drgero", 1, 18500, 880, 1400)}, rewTPS(22000)),
				step("android", 9, "09_track_android_signal.json", new JsonObject[]{objBiome("dmz.quest.android9.obj1", "#minecraft:is_mountain")}, rewTPS(9500)),
				step("android", 10, "10_defeat_a17.json", new JsonObject[]{objBiome("dmz.quest.android10.obj1", "#minecraft:is_mountain"), objKill("dmz.quest.android10.obj2", "dragonminez:saga_a17", 1, 30000, 1400, 2200)}, rewTPS(23000)),
				step("android", 11, "11_defeat_a18.json", new JsonObject[]{objBiome("dmz.quest.android11.obj1", "#minecraft:is_mountain"), objKill("dmz.quest.android11.obj2", "dragonminez:saga_a18", 1, 30000, 1400, 2200)}, rewTPS(23500)),
				step("android", 12, "12_defeat_a16.json", new JsonObject[]{objBiome("dmz.quest.android12.obj1", "minecraft:plains"), objKill("dmz.quest.android12.obj2", "dragonminez:saga_a16", 1, 32000, 1500, 2400)}, rewTPS(24000)),
				step("android", 13, "13_investigate_new_threat.json", new JsonObject[]{objBiome("dmz.quest.android13.obj1", "minecraft:plains"), objKill("dmz.quest.android13.obj2", "dragonminez:saga_cell_imperfect", 1, 28000, 1300, 2000)}, rewTPS(24500)),
				step("android", 14, "14_defeat_piccolo_kami.json", new JsonObject[]{objBiome("dmz.quest.android14.obj1", "minecraft:plains"), objKill("dmz.quest.android14.obj2", "dragonminez:saga_piccolo_kami", 1, 35000, 1600, 2600)}, rewTPS(25000)),
				step("android", 15, "15_find_timechamber.json", new JsonObject[]{objStructure("dmz.quest.android15.obj1", "dragonminez:timechamber")}, rewTPS(10000)),
				step("android", 16, "16_defend_city_from_cell.json", new JsonObject[]{objBiome("dmz.quest.android16.obj1", "minecraft:plains"), objKill("dmz.quest.android16.obj2", "dragonminez:saga_cell_imperfect", 2, 31000, 1450, 2300)}, rewTPS(25500)),
				step("android", 17, "17_stop_cell_absorption.json", new JsonObject[]{objBiome("dmz.quest.android17.obj1", "minecraft:plains"), objKill("dmz.quest.android17.obj2", "dragonminez:saga_cell_imperfect", 1, 36000, 1650, 2600), objKill("dmz.quest.android17.obj3", "dragonminez:saga_a18", 1, 32000, 1500, 2350)}, rewTPS(26500)),
				step("android", 18, "18_defeat_cell_semiperfect.json", new JsonObject[]{objBiome("dmz.quest.android18.obj1", "minecraft:plains"), objKill("dmz.quest.android18.obj2", "dragonminez:saga_cell_semiperfect", 1, 42000, 1900, 3100)}, rewTPS(27500)),
				step("android", 19, "19_clash_super_vegeta.json", new JsonObject[]{objBiome("dmz.quest.android19.obj1", "minecraft:plains"), objKill("dmz.quest.android19.obj2", "dragonminez:saga_super_vegeta", 1, 45000, 2100, 3400)}, rewTPS(28500)),
				step("android", 20, "20_clash_trunks_ssj.json", new JsonObject[]{objBiome("dmz.quest.android20.obj1", "minecraft:plains"), objKill("dmz.quest.android20.obj2", "dragonminez:saga_trunks_ssj", 1, 43000, 2000, 3250)}, rewTPS(29500)),
				step("android", 21, "21_defeat_cell_perfect.json", new JsonObject[]{objBiome("dmz.quest.android21.obj1", "minecraft:plains"), objKill("dmz.quest.android21.obj2", "dragonminez:saga_cell_perfect", 1, 62000, 2900, 4700)}, rewTPS(30500)),
				step("android", 22, "22_prepare_cell_games.json", new JsonObject[]{objStructure("dmz.quest.android22.obj1", "dragonminez:kamilookout")}, rewTPS(11000)),
				step("android", 23, "23_defeat_gohan_ssj.json", new JsonObject[]{objBiome("dmz.quest.android23.obj1", "minecraft:plains"), objKill("dmz.quest.android23.obj2", "dragonminez:saga_gohan_ssj", 1, 56000, 2550, 4100)}, rewTPS(31500)),
				step("android", 24, "24_defeat_cell_jrs_wave1.json", new JsonObject[]{objBiome("dmz.quest.android24.obj1", "minecraft:plains"), objKill("dmz.quest.android24.obj2", "dragonminez:saga_cell_jr", 6, 43000, 1980, 3850)}, rewTPS(32000)),
				step("android", 25, "25_defeat_cell_jrs_wave2.json", new JsonObject[]{objBiome("dmz.quest.android25.obj1", "minecraft:plains"), objKill("dmz.quest.android25.obj2", "dragonminez:saga_cell_jr", 8, 45000, 2050, 3950)}, rewTPS(33000)),
				step("android", 26, "26_defeat_cell_superperfect.json", new JsonObject[]{objBiome("dmz.quest.android26.obj1", "minecraft:plains"), objKill("dmz.quest.android26.obj2", "dragonminez:saga_cell_superperfect", 1, 82000, 3900, 6200)}, rewTPS(35000)),
				step("android", 27, "27_secure_earth_after_cell.json", new JsonObject[]{objBiome("dmz.quest.android27.obj1", "minecraft:plains"), objKill("dmz.quest.android27.obj2", "dragonminez:saga_cell_jr", 10, 46000, 2150, 4100)}, rewTPS(36000)),
				step("android", 28, "28_post_cell_training.json", new JsonObject[]{objBiome("dmz.quest.android28.obj1", "minecraft:plains"), objKill("dmz.quest.android28.obj2", "dragonminez:shadow_dummy", 20, 2200, 140, 230)}, rewTPS(36500)),
				step("android", 29, "29_return_to_kami_lookout.json", new JsonObject[]{objStructure("dmz.quest.android29.obj1", "dragonminez:kamilookout")}, rewTPS(11500)),
				step("android", 30, "30_world_peace_patrol.json", new JsonObject[]{objBiome("dmz.quest.android30.obj1", "dragonminez:rocky"), objKill("dmz.quest.android30.obj2", "dragonminez:saga_friezasoldier01", 18, 1600, 100, 180)}, rewTPS(37000)),
				step("android", 31, "31_hidden_cell_return.json", new JsonObject[]{objBiome("dmz.quest.android31.obj1", "minecraft:plains"), objKill("dmz.quest.android31.obj2", "dragonminez:saga_cell_perfect", 1, 68000, 3200, 5000)}, rewTPS(37500)),
				step("android", 32, "32_finish_cell_remnant.json", new JsonObject[]{objBiome("dmz.quest.android32.obj1", "minecraft:plains"), objKill("dmz.quest.android32.obj2", "dragonminez:saga_cell_superperfect", 1, 86000, 4100, 6500)}, rewTPS(38000)),
				step("android", 33, "33_pass_torch_to_next_generation.json", new JsonObject[]{objStructure("dmz.quest.android33.obj1", "dragonminez:goku_house")}, rewTPS(12000)),
				step("android", 34, "34_ready_for_buu_era.json", new JsonObject[]{objStructure("dmz.quest.android34.obj1", "dragonminez:kamilookout")}, rewTPS(12500))
		);
	}

	// ========================================================================================
	// Buu Saga Quests (folder: saga_buu)
	// ========================================================================================

	private static void createBuuSagaQuests(Path questsDir) {
		Path dir = questsDir.resolve("saga_buu");
		String s = "buu_saga", c = "saga_buu";
		JsonObject prevAndroid = prevQuest("android_saga", 34);

		writeLinearSaga(dir, s, c, prevAndroid,
				step("buu", 1, "01_world_tournament_arrival.json", new JsonObject[]{objBiome("dmz.quest.buu1.obj1", "minecraft:plains")}, rewTPS(12000)),
				step("buu", 2, "02_preliminary_ring_match.json", new JsonObject[]{objBiome("dmz.quest.buu2.obj1", "minecraft:plains"), objKill("dmz.quest.buu2.obj2", "dragonminez:shadow_dummy", 6, 2200, 140, 220)}, rewTPS(13000)),
				step("buu", 3, "03_search_for_supreme_kai.json", new JsonObject[]{objStructure("dmz.quest.buu3.obj1", "dragonminez:kamilookout")}, rewTPS(13500)),
				step("buu", 4, "04_track_babidi_scouts.json", new JsonObject[]{objBiome("dmz.quest.buu4.obj1", "dragonminez:rocky"), objKill("dmz.quest.buu4.obj2", "dragonminez:saga_morosoldier", 10, 2600, 160, 260)}, rewTPS(14500)),
				step("buu", 5, "05_stop_energy_harvesters.json", new JsonObject[]{objBiome("dmz.quest.buu5.obj1", "dragonminez:rocky"), objKill("dmz.quest.buu5.obj2", "dragonminez:saga_morosoldier", 12, 3000, 180, 290)}, rewTPS(15500)),
				step("buu", 6, "06_secure_tournament_city.json", new JsonObject[]{objBiome("dmz.quest.buu6.obj1", "minecraft:plains"), objKill("dmz.quest.buu6.obj2", "dragonminez:saga_cell_jr", 6, 46000, 2200, 4200)}, rewTPS(16500)),
				step("buu", 7, "07_enter_babidi_ship.json", new JsonObject[]{objBiome("dmz.quest.buu7.obj1", "dragonminez:rocky")}, rewTPS(10000)),
				step("buu", 8, "08_ship_floor_one.json", new JsonObject[]{objBiome("dmz.quest.buu8.obj1", "dragonminez:rocky"), objKill("dmz.quest.buu8.obj2", "dragonminez:saga_morosoldier", 14, 3400, 200, 320)}, rewTPS(17000)),
				step("buu", 9, "09_ship_floor_two.json", new JsonObject[]{objBiome("dmz.quest.buu9.obj1", "dragonminez:rocky"), objKill("dmz.quest.buu9.obj2", "dragonminez:saga_friezasoldier03", 14, 1800, 110, 200)}, rewTPS(17500)),
				step("buu", 10, "10_ship_floor_three.json", new JsonObject[]{objBiome("dmz.quest.buu10.obj1", "dragonminez:rocky"), objKill("dmz.quest.buu10.obj2", "dragonminez:saga_cell_imperfect", 2, 40000, 1850, 2900)}, rewTPS(18000)),
				step("buu", 11, "11_defeat_dabura_guard.json", new JsonObject[]{objBiome("dmz.quest.buu11.obj1", "dragonminez:rocky"), objKill("dmz.quest.buu11.obj2", "dragonminez:saga_cell_perfect", 1, 70000, 3300, 5300)}, rewTPS(19000)),
				step("buu", 12, "12_interrupt_revival_ritual.json", new JsonObject[]{objBiome("dmz.quest.buu12.obj1", "dragonminez:rocky"), objKill("dmz.quest.buu12.obj2", "dragonminez:saga_morosoldier", 16, 3600, 220, 350)}, rewTPS(19500)),
				step("buu", 13, "13_survive_majin_ambush.json", new JsonObject[]{objBiome("dmz.quest.buu13.obj1", "minecraft:plains"), objKill("dmz.quest.buu13.obj2", "dragonminez:saga_super_vegeta", 1, 50000, 2350, 3800)}, rewTPS(20500)),
				step("buu", 14, "14_break_majin_control.json", new JsonObject[]{objBiome("dmz.quest.buu14.obj1", "minecraft:plains"), objKill("dmz.quest.buu14.obj2", "dragonminez:saga_super_vegeta", 1, 54000, 2500, 4000)}, rewTPS(21000)),
				step("buu", 15, "15_delay_buu_hatching.json", new JsonObject[]{objBiome("dmz.quest.buu15.obj1", "minecraft:plains"), objKill("dmz.quest.buu15.obj2", "dragonminez:saga_cell_jr", 10, 50000, 2300, 4400)}, rewTPS(22000)),
				step("buu", 16, "16_first_clash_with_buu.json", new JsonObject[]{objBiome("dmz.quest.buu16.obj1", "minecraft:plains"), objKill("dmz.quest.buu16.obj2", "dragonminez:saga_cell_superperfect", 1, 90000, 4300, 6800)}, rewTPS(23000)),
				step("buu", 17, "17_evacuate_civilians.json", new JsonObject[]{objBiome("dmz.quest.buu17.obj1", "minecraft:plains")}, rewTPS(12000)),
				step("buu", 18, "18_regroup_at_lookout.json", new JsonObject[]{objStructure("dmz.quest.buu18.obj1", "dragonminez:kamilookout")}, rewTPS(12500)),
				step("buu", 19, "19_unlock_fusion_training.json", new JsonObject[]{objStructure("dmz.quest.buu19.obj1", "dragonminez:timechamber")}, rewTPS(13000)),
				step("buu", 20, "20_fusion_trial_one.json", new JsonObject[]{objBiome("dmz.quest.buu20.obj1", "minecraft:plains"), objKill("dmz.quest.buu20.obj2", "dragonminez:shadow_dummy", 12, 2600, 170, 280)}, rewTPS(23500)),
				step("buu", 21, "21_fusion_trial_two.json", new JsonObject[]{objBiome("dmz.quest.buu21.obj1", "minecraft:plains"), objKill("dmz.quest.buu21.obj2", "dragonminez:shadow_dummy", 16, 3000, 190, 320)}, rewTPS(24000)),
				step("buu", 22, "22_stall_super_buu.json", new JsonObject[]{objBiome("dmz.quest.buu22.obj1", "minecraft:plains"), objKill("dmz.quest.buu22.obj2", "dragonminez:saga_cell_superperfect", 1, 98000, 4700, 7400)}, rewTPS(25000)),
				step("buu", 23, "23_rescue_the_lookout.json", new JsonObject[]{objStructure("dmz.quest.buu23.obj1", "dragonminez:kamilookout"), objKill("dmz.quest.buu23.obj2", "dragonminez:saga_cell_jr", 12, 52000, 2450, 4600)}, rewTPS(25500)),
				step("buu", 24, "24_timechamber_last_stand.json", new JsonObject[]{objStructure("dmz.quest.buu24.obj1", "dragonminez:timechamber"), objKill("dmz.quest.buu24.obj2", "dragonminez:saga_cell_superperfect", 1, 104000, 5000, 7900)}, rewTPS(26000)),
				step("buu", 25, "25_free_absorbed_allies.json", new JsonObject[]{objBiome("dmz.quest.buu25.obj1", "minecraft:plains"), objKill("dmz.quest.buu25.obj2", "dragonminez:saga_cell_perfect", 2, 74000, 3500, 5600)}, rewTPS(27000)),
				step("buu", 26, "26_chase_buu_across_earth.json", new JsonObject[]{objBiome("dmz.quest.buu26.obj1", "dragonminez:rocky"), objKill("dmz.quest.buu26.obj2", "dragonminez:saga_morosoldier", 18, 4200, 250, 390)}, rewTPS(27500)),
				step("buu", 27, "27_prepare_spirit_bomb.json", new JsonObject[]{objBiome("dmz.quest.buu27.obj1", "minecraft:plains")}, rewTPS(13500)),
				step("buu", 28, "28_hold_off_minions.json", new JsonObject[]{objBiome("dmz.quest.buu28.obj1", "minecraft:plains"), objKill("dmz.quest.buu28.obj2", "dragonminez:saga_morosoldier", 20, 4600, 280, 430)}, rewTPS(28000)),
				step("buu", 29, "29_force_buu_separation.json", new JsonObject[]{objBiome("dmz.quest.buu29.obj1", "minecraft:plains"), objKill("dmz.quest.buu29.obj2", "dragonminez:saga_cell_superperfect", 1, 108000, 5200, 8200)}, rewTPS(29000)),
				step("buu", 30, "30_track_kid_buu_signal.json", new JsonObject[]{objBiome("dmz.quest.buu30.obj1", "dragonminez:rocky")}, rewTPS(14000)),
				step("buu", 31, "31_kid_buu_assault_phase_one.json", new JsonObject[]{objBiome("dmz.quest.buu31.obj1", "dragonminez:rocky"), objKill("dmz.quest.buu31.obj2", "dragonminez:saga_frieza_fp", 1, 20000, 960, 1500)}, rewTPS(30000)),
				step("buu", 32, "32_kid_buu_assault_phase_two.json", new JsonObject[]{objBiome("dmz.quest.buu32.obj1", "dragonminez:rocky"), objKill("dmz.quest.buu32.obj2", "dragonminez:saga_frieza_fp", 1, 22000, 1050, 1650)}, rewTPS(30500)),
				step("buu", 33, "33_kid_buu_assault_phase_three.json", new JsonObject[]{objBiome("dmz.quest.buu33.obj1", "dragonminez:rocky"), objKill("dmz.quest.buu33.obj2", "dragonminez:saga_frieza_fp", 1, 24000, 1120, 1780)}, rewTPS(31000)),
				step("buu", 34, "34_spirit_bomb_charge.json", new JsonObject[]{objBiome("dmz.quest.buu34.obj1", "minecraft:plains"), objKill("dmz.quest.buu34.obj2", "dragonminez:saga_morosoldier", 24, 5000, 300, 470)}, rewTPS(31500)),
				step("buu", 35, "35_last_line_of_defense.json", new JsonObject[]{objBiome("dmz.quest.buu35.obj1", "minecraft:plains"), objKill("dmz.quest.buu35.obj2", "dragonminez:saga_cell_jr", 16, 54000, 2550, 4800)}, rewTPS(32000)),
				step("buu", 36, "36_final_kid_buu_duel.json", new JsonObject[]{objBiome("dmz.quest.buu36.obj1", "dragonminez:rocky"), objKill("dmz.quest.buu36.obj2", "dragonminez:saga_frieza_fp", 1, 26000, 1200, 1900)}, rewTPS(33000)),
				step("buu", 37, "37_restore_earth.json", new JsonObject[]{objBiome("dmz.quest.buu37.obj1", "minecraft:plains")}, rewTPS(14500)),
				step("buu", 38, "38_return_to_tournament_grounds.json", new JsonObject[]{objBiome("dmz.quest.buu38.obj1", "minecraft:plains")}, rewTPS(15000)),
				step("buu", 39, "39_world_protector_oath.json", new JsonObject[]{objStructure("dmz.quest.buu39.obj1", "dragonminez:kamilookout")}, rewTPS(15500)),
				step("buu", 40, "40_end_of_buu_saga.json", new JsonObject[]{objStructure("dmz.quest.buu40.obj1", "dragonminez:roshi_house")}, rewTPS(18000))
		);
	}

	private static void createMoviesSagaQuests(Path questsDir) {
		Path dir = questsDir.resolve("saga_movies");
		String s = "movies_saga", c = "saga_movies";
		JsonObject prevAndroid = prereqs("AND", condSaga("buu_saga", 40));
	}
}
