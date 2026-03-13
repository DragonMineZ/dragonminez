package com.dragonminez.common.quest;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SagaManager extends SimplePreparableReloadListener<Map<String, Saga>> {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<String, Saga> LOADED_SAGAS = new HashMap<>();
	private static final Map<String, Saga> CLIENT_SAGAS = new HashMap<>();
	private static final String SAGA_FOLDER = "dragonminez" + File.separator + "sagas";

	public static void init() {
	}

	private static void createDefaultSagaFiles(Path sagaDir) {
		if (ConfigManager.getServerConfig().getGameplay().getStoryModeEnabled() && ConfigManager.getServerConfig().getGameplay().getCreateDefaultSagas()) {
			checkAndCreateSaga(sagaDir, "saiyan_saga.json", SagaManager::createSaiyanSagaFile);
			checkAndCreateSaga(sagaDir, "frieza_saga.json", SagaManager::createFriezaSagaFile);
			checkAndCreateSaga(sagaDir, "android_saga.json", SagaManager::createAndroidSagaFile);
		}
	}

	private static void checkAndCreateSaga(Path sagaDir, String fileName, java.util.function.Consumer<Path> generator) {
		Path file = sagaDir.resolve(fileName);
		if (!Files.exists(file) || isFileOutdated(file)) {
			generator.accept(sagaDir);
		}
	}

	private static boolean isFileOutdated(Path file) {
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray quests = root.getAsJsonArray("quests");
			if (quests == null || quests.isEmpty()) return true;

			for (JsonElement q : quests) {
				for (JsonElement o : q.getAsJsonObject().getAsJsonArray("objectives")) {
					JsonObject obj = o.getAsJsonObject();
					if ("KILL".equals(obj.get("type").getAsString())) if (!obj.has("health")) return true;
				}
			}
			return false;
		} catch (Exception e) {
			LogUtil.error(Env.SERVER, "Error parsing saga file " + file.getFileName() + " for version check, regenerating file. Error:", e);
			return true;
		}
	}

	private static void createSaiyanSagaFile(Path sagaDir) {
		Path defaultFile = sagaDir.resolve("saiyan_saga.json");
		try {
			Files.createDirectories(sagaDir);
			try (Writer writer = Files.newBufferedWriter(defaultFile, StandardCharsets.UTF_8)) {
				JsonObject root = new JsonObject();

				root.addProperty("id", "saiyan_saga");
				root.addProperty("name", "dmz.saga.saiyan_saga");

				JsonObject requirements = new JsonObject();
				requirements.addProperty("previousSaga", "");
				root.add("requirements", requirements);

				JsonArray quests = new JsonArray();

				quests.add(createQuest(1, "dmz.quest.saiyan1.name", "dmz.quest.saiyan1.desc",
						new JsonObject[][]{{createObjective("STRUCTURE", "dmz.quest.saiyan1.obj1", "dragonminez:roshi_house", null, 0, 0.0, 0.0, 0.0)}},
						new JsonObject[][]{{createReward("TPS", 500, null, 0, null)}}
				));

				quests.add(createQuest(2, "dmz.quest.saiyan2.name", "dmz.quest.saiyan2.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.saiyan2.obj1", "minecraft:plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.saiyan2.obj2", null, "dragonminez:saga_raditz", 1, 400.0, 25.0, 50.0)}
						},
						new JsonObject[][]{{createReward("TPS", 1000, null, 0, null)}}
				));

				quests.add(createQuest(3, "dmz.quest.saiyan3.name", "dmz.quest.saiyan3.desc",
						new JsonObject[][]{{createObjective("ITEM", "dmz.quest.saiyan3.obj1", "dragonminez:dball_radar", null, 1, 0.0, 0.0, 0.0)}},
						new JsonObject[][]{{createReward("TPS", 1500, null, 0, null)}}
				));

				JsonArray objectives4 = new JsonArray();
				for (int i = 1; i <= 7; i++)
					objectives4.add(createObjective("ITEM", "dmz.quest.saiyan4.obj" + i, "dragonminez:dball" + i, null, 1, 0.0, 0.0, 0.0));
				quests.add(createQuest(4, "dmz.quest.saiyan4.name", "dmz.quest.saiyan4.desc",
						new JsonObject[][]{{objectives4.get(0).getAsJsonObject(), objectives4.get(1).getAsJsonObject(), objectives4.get(2).getAsJsonObject(), objectives4.get(3).getAsJsonObject(), objectives4.get(4).getAsJsonObject(), objectives4.get(5).getAsJsonObject(), objectives4.get(6).getAsJsonObject()}},
						new JsonObject[][]{{createReward("TPS", 2000, null, 0, null)}}
				));

				quests.add(createQuest(5, "dmz.quest.saiyan5.name", "dmz.quest.saiyan5.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.saiyan5.obj1", "minecraft:plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.saiyan5.obj2", null, "dragonminez:saga_saibaman1", 6, 400.0, 25.0, 50.0)}
						},
						new JsonObject[][]{{createReward("TPS", 2500, null, 0, null)}}
				));

				quests.add(createQuest(6, "dmz.quest.saiyan6.name", "dmz.quest.saiyan6.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.saiyan6.obj1", "minecraft:plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.saiyan6.obj2", null, "dragonminez:saga_nappa", 1, 750.0, 45.0, 100.0)}
						},
						new JsonObject[][]{{createReward("TPS", 3000, null, 0, null)}}
				));

				quests.add(createQuest(7, "dmz.quest.saiyan7.name", "dmz.quest.saiyan7.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.saiyan7.obj1", "dragonminez:rocky", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.saiyan7.obj2", null, "dragonminez:saga_vegeta", 1, 1200.0, 70.0, 150.0)}
						},
						new JsonObject[][]{{createReward("TPS", 3500, null, 0, null)}}
				));

				quests.add(createQuest(8, "dmz.quest.saiyan8.name", "dmz.quest.saiyan8.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.saiyan8.obj1", "dragonminez:rocky", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.saiyan8.obj2", null, "dragonminez:saga_ozaruvegeta", 1, 2500.0, 140.0, 200.0)}
						},
						new JsonObject[][]{
								{createReward("TPS", 4000, null, 1, null)},
								{createReward("ITEM", 0, "dragonminez:saiyan_ship", 1, null)}
						}
				));

				quests.add(createQuest(9, "dmz.quest.saiyan9.name", "dmz.quest.saiyan9.desc",
						new JsonObject[][]{{createObjective("BIOME", "dmz.quest.saiyan9.obj1", "dragonminez:ajissa_plains", null, 0, 0.0, 0.0, 0.0)}},
						new JsonObject[][]{{createReward("TPS", 4500, null, 0, null)}}
				));

				root.add("quests", quests);
				GSON.toJson(root, writer);
			}
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Failed to create default saiyan saga file", e);
		}
	}

	private static void createFriezaSagaFile(Path sagaDir) {
		Path defaultFile = sagaDir.resolve("frieza_saga.json");
		try {
			Files.createDirectories(sagaDir);
			try (Writer writer = Files.newBufferedWriter(defaultFile, StandardCharsets.UTF_8)) {
				JsonObject root = new JsonObject();

				root.addProperty("id", "frieza_saga");
				root.addProperty("name", "dmz.saga.frieza_saga");

				JsonObject requirements = new JsonObject();
				requirements.addProperty("previousSaga", "saiyan_saga");
				root.add("requirements", requirements);

				JsonArray quests = new JsonArray();

				quests.add(createQuest(1, "dmz.quest.frieza1.name", "dmz.quest.frieza1.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.frieza1.obj1", "dragonminez:ajissa_plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.frieza1.obj2", null, "dragonminez:saga_cui", 1, 1200.0, 70.0, 150.0)}
						},
						new JsonObject[][]{{createReward("TPS", 5000, null, 0, null)}}
				));

				quests.add(createQuest(2, "dmz.quest.frieza2.name", "dmz.quest.frieza2.desc",
						new JsonObject[][]{{createObjective("STRUCTURE", "dmz.quest.frieza2.obj1", "dragonminez:village_ajissa", null, 0, 0.0, 0.0, 0.0)}},
						new JsonObject[][]{{createReward("TPS", 5500, null, 0, null)}}
				));

				quests.add(createQuest(3, "dmz.quest.frieza3.name", "dmz.quest.frieza3.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.frieza3.obj1", "dragonminez:ajissa_plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.frieza3.obj2", null, "dragonminez:saga_dodoria", 1, 1400.0, 80.0, 180.0)}
						},
						new JsonObject[][]{{createReward("TPS", 6000, null, 0, null)}}
				));

				quests.add(createQuest(4, "dmz.quest.frieza4.name", "dmz.quest.frieza4.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.frieza4.obj1", "dragonminez:ajissa_plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.frieza4.obj2", null, "dragonminez:saga_zarbont1", 1, 1500.0, 85.0, 200.0)}
						},
						new JsonObject[][]{{createReward("TPS", 6500, null, 0, null)}}
				));

				quests.add(createQuest(5, "dmz.quest.frieza5.name", "dmz.quest.frieza5.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.frieza5.obj1", "dragonminez:ajissa_plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.frieza5.obj2", null, "dragonminez:saga_vegeta_namek", 1, 1600.0, 90.0, 200.0)}
						},
						new JsonObject[][]{{createReward("TPS", 7000, null, 0, null)}}
				));


				quests.add(createQuest(6, "dmz.quest.frieza6.name", "dmz.quest.frieza6.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.frieza6.obj1", "dragonminez:ajissa_plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.frieza6.obj2", null, "dragonminez:saga_guldo", 1, 800.0, 50.0, 90.0)}
						},
						new JsonObject[][]{{createReward("TPS", 8000, null, 0, null)}}
				));

				quests.add(createQuest(7, "dmz.quest.frieza7.name", "dmz.quest.frieza7.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.frieza7.obj1", "dragonminez:ajissa_plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.frieza7.obj2", null, "dragonminez:saga_recoome", 1, 2000.0, 110.0, 180.0)}
						},
						new JsonObject[][]{{createReward("TPS", 8500, null, 0, null)}}
				));

				quests.add(createQuest(8, "dmz.quest.frieza8.name", "dmz.quest.frieza8.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.frieza8.obj1", "dragonminez:ajissa_plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.frieza8.obj2", null, "dragonminez:saga_burter", 1, 2000.0, 110.0, 180.0)}
						},
						new JsonObject[][]{{createReward("TPS", 9000, null, 0, null)}}
				));

				quests.add(createQuest(9, "dmz.quest.frieza9.name", "dmz.quest.frieza9.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.frieza9.obj1", "dragonminez:ajissa_plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.frieza9.obj2", null, "dragonminez:saga_jeice", 1, 2000.0, 110.0, 180.0)}
						},
						new JsonObject[][]{{createReward("TPS", 9500, null, 0, null)}}
				));

				quests.add(createQuest(10, "dmz.quest.frieza10.name", "dmz.quest.frieza10.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.frieza10.obj1", "dragonminez:ajissa_plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.frieza10.obj2", null, "dragonminez:saga_ginyu", 1, 3000.0, 160.0, 260.0)}
						},
						new JsonObject[][]{{createReward("TPS", 10000, null, 0, null)}}
				));

				quests.add(createQuest(11, "dmz.quest.frieza11.name", "dmz.quest.frieza11.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.frieza11.obj1", "dragonminez:ajissa_plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.frieza11.obj2", null, "dragonminez:saga_ginyu_goku", 1, 1500.0, 85.0, 140.0)}
						},
						new JsonObject[][]{{createReward("TPS", 11000, null, 0, null)}}
				));

				quests.add(createQuest(12, "dmz.quest.frieza12.name", "dmz.quest.frieza12.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.frieza12.obj1", "dragonminez:ajissa_plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.frieza12.obj2", null, "dragonminez:saga_frieza_first", 1, 4000.0, 200.0, 350.0)}
						},
						new JsonObject[][]{{createReward("TPS", 12000, null, 0, null)}}
				));

				quests.add(createQuest(13, "dmz.quest.frieza13.name", "dmz.quest.frieza13.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.frieza13.obj1", "dragonminez:ajissa_plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.frieza13.obj2", null, "dragonminez:saga_frieza_third", 1, 8000.0, 400.0, 650.0)}
						},
						new JsonObject[][]{{createReward("TPS", 13000, null, 0, null)}}
				));

				quests.add(createQuest(14, "dmz.quest.frieza14.name", "dmz.quest.frieza14.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.frieza14.obj1", "dragonminez:ajissa_plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.frieza14.obj2", null, "dragonminez:saga_frieza_base", 1, 12000.0, 550.0, 900.0)}
						},
						new JsonObject[][]{{createReward("TPS", 14000, null, 0, null)}}
				));

				quests.add(createQuest(15, "dmz.quest.frieza15.name", "dmz.quest.frieza15.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.frieza15.obj1", "dragonminez:ajissa_plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.frieza15.obj2", null, "dragonminez:saga_frieza_fp", 1, 16000.0, 750.0, 1200.0)}
						},
						new JsonObject[][]{{createReward("TPS", 15000, null, 0, null)}}
				));

				quests.add(createQuest(16, "dmz.quest.frieza16.name", "dmz.quest.frieza16.desc",
						new JsonObject[][]{{createObjective("BIOME", "dmz.quest.frieza16.obj1", "dragonminez:rocky", null, 0, 0.0, 0.0, 0.0)}},
						new JsonObject[][]{{createReward("TPS", 5000, null, 0, null)}}
				));

				root.add("quests", quests);
				GSON.toJson(root, writer);
			}
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Failed to create default frieza saga file", e);
		}
	}

	private static void createAndroidSagaFile(Path sagaDir) {
		Path defaultFile = sagaDir.resolve("android_saga.json");
		try {
			Files.createDirectories(sagaDir);
			try (Writer writer = Files.newBufferedWriter(defaultFile, StandardCharsets.UTF_8)) {
				JsonObject root = new JsonObject();

				root.addProperty("id", "android_saga");
				root.addProperty("name", "dmz.saga.android_saga");

				JsonObject requirements = new JsonObject();
				requirements.addProperty("previousSaga", "frieza_saga");
				root.add("requirements", requirements);

				JsonArray quests = new JsonArray();

				quests.add(createQuest(1, "dmz.quest.android1.name", "dmz.quest.android1.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.android1.obj1", "dragonminez:rocky", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.android1.obj2", null, "dragonminez:saga_friezasoldier01", 15, 200.0, 15.0, 20.0)}
						},
						new JsonObject[][]{{createReward("TPS", 18000, null, 0, null)}}
				));

				quests.add(createQuest(2, "dmz.quest.android2.name", "dmz.quest.android2.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.android2.obj1", "dragonminez:rocky", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.android2.obj2", null, "dragonminez:saga_mecha_frieza", 1, 17000.0, 800.0, 1300.0)},
								{createObjective("KILL", "dmz.quest.android2.obj3", null, "dragonminez:saga_king_cold", 1, 8000.0, 400.0, 650.0)}
						},
						new JsonObject[][]{{createReward("TPS", 19000, null, 0, null)}}
				));

				quests.add(createQuest(3, "dmz.quest.android3.name", "dmz.quest.android3.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.android3.obj1", "dragonminez:rocky", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.android3.obj2", null, "dragonminez:saga_goku_yardrat", 1, 20000.0, 900.0, 1500.0)}
						},
						new JsonObject[][]{{createReward("TPS", 20000, null, 0, null)}}
				));

				quests.add(createQuest(4, "dmz.quest.android4.name", "dmz.quest.android4.desc",
						new JsonObject[][]{{createObjective("STRUCTURE", "dmz.quest.android4.obj1", "dragonminez:goku_house", null, 0, 0.0, 0.0, 0.0)}},
						new JsonObject[][]{{createReward("TPS", 5000, null, 0, null)}}
				));

				quests.add(createQuest(5, "dmz.quest.android5.name", "dmz.quest.android5.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.android5.obj1", "dragonminez:rocky", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.android5.obj2", null, "dragonminez:saga_a19", 1, 22000.0, 1000.0, 1600.0)}
						},
						new JsonObject[][]{{createReward("TPS", 21000, null, 0, null)}}
				));

				quests.add(createQuest(6, "dmz.quest.android6.name", "dmz.quest.android6.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.android6.obj1", "dragonminez:rocky", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.android6.obj2", null, "dragonminez:saga_drgero", 1, 18000.0, 850.0, 1350.0)}
						},
						new JsonObject[][]{{createReward("TPS", 22000, null, 0, null)}}
				));

				quests.add(createQuest(7, "dmz.quest.android7.name", "dmz.quest.android7.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.android7.obj1", "#minecraft:is_mountain", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.android7.obj2", null, "dragonminez:saga_a17", 1, 30000.0, 1400.0, 2200.0)},
								{createObjective("KILL", "dmz.quest.android7.obj3", null, "dragonminez:saga_a18", 1, 30000.0, 1400.0, 2200.0)}
						},
						new JsonObject[][]{{createReward("TPS", 23000, null, 0, null)}}
				));

				quests.add(createQuest(8, "dmz.quest.android8.name", "dmz.quest.android8.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.android8.obj1", "minecraft:plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.android8.obj2", null, "dragonminez:saga_cell_imperfect", 1, 28000.0, 1300.0, 2000.0)}
						},
						new JsonObject[][]{{createReward("TPS", 24000, null, 0, null)}}
				));

				quests.add(createQuest(9, "dmz.quest.android9.name", "dmz.quest.android9.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.android9.obj1", "minecraft:plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.android9.obj2", null, "dragonminez:saga_piccolo_kami", 1, 35000.0, 1600.0, 2600.0)},
								{createObjective("KILL", "dmz.quest.android9.obj3", null, "dragonminez:saga_a17", 1, 30000.0, 1400.0, 2200.0)}
						},
						new JsonObject[][]{{createReward("TPS", 25000, null, 0, null)}}
				));

				quests.add(createQuest(10, "dmz.quest.android10.name", "dmz.quest.android10.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.android10.obj1", "minecraft:plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.android10.obj2", null, "dragonminez:saga_a16", 1, 32000.0, 1500.0, 2400.0)}
						},
						new JsonObject[][]{{createReward("TPS", 26000, null, 0, null)}}
				));

				quests.add(createQuest(11, "dmz.quest.android11.name", "dmz.quest.android11.desc",
						new JsonObject[][]{{createObjective("STRUCTURE", "dmz.quest.android11.obj1", "dragonminez:timechamber", null, 0, 0.0, 0.0, 0.0)}},
						new JsonObject[][]{{createReward("TPS", 10000, null, 0, null)}}
				));

				quests.add(createQuest(12, "dmz.quest.android12.name", "dmz.quest.android12.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.android12.obj1", "minecraft:plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.android12.obj2", null, "dragonminez:saga_cell_semiperfect", 1, 40000.0, 1800.0, 3000.0)},
								{createObjective("KILL", "dmz.quest.android12.obj3", null, "dragonminez:saga_a18", 1, 30000.0, 1400.0, 2200.0)}
						},
						new JsonObject[][]{{createReward("TPS", 27000, null, 0, null)}}
				));

				quests.add(createQuest(13, "dmz.quest.android13.name", "dmz.quest.android13.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.android13.obj1", "minecraft:plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.android13.obj2", null, "dragonminez:saga_super_vegeta", 1, 45000.0, 2100.0, 3400.0)}
						},
						new JsonObject[][]{{createReward("TPS", 28000, null, 0, null)}}
				));

				quests.add(createQuest(14, "dmz.quest.android14.name", "dmz.quest.android14.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.android14.obj1", "minecraft:plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.android14.obj2", null, "dragonminez:saga_trunks_ssj", 1, 42000.0, 1950.0, 3200.0)}
						},
						new JsonObject[][]{{createReward("TPS", 29000, null, 0, null)}}
				));

				quests.add(createQuest(15, "dmz.quest.android15.name", "dmz.quest.android15.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.android15.obj1", "minecraft:plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.android15.obj2", null, "dragonminez:saga_cell_perfect", 1, 60000.0, 2800.0, 4500.0)}
						},
						new JsonObject[][]{{createReward("TPS", 30000, null, 0, null)}}
				));

				quests.add(createQuest(16, "dmz.quest.android16.name", "dmz.quest.android16.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.android16.obj1", "minecraft:plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.android16.obj2", null, "dragonminez:saga_gohan_ssj", 1, 55000.0, 2500.0, 4000.0)}
						},
						new JsonObject[][]{{createReward("TPS", 31000, null, 0, null)}}
				));

				quests.add(createQuest(17, "dmz.quest.android17.name", "dmz.quest.android17.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.android17.obj1", "minecraft:plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.android17.obj2", null, "dragonminez:saga_cell_jr", 6, 42500.0, 1950.0, 3800.0)}
						},
						new JsonObject[][]{{createReward("TPS", 32000, null, 0, null)}}
				));

				quests.add(createQuest(18, "dmz.quest.android18.name", "dmz.quest.android18.desc",
						new JsonObject[][]{
								{createObjective("BIOME", "dmz.quest.android18.obj1", "minecraft:plains", null, 0, 0.0, 0.0, 0.0)},
								{createObjective("KILL", "dmz.quest.android18.obj2", null, "dragonminez:saga_cell_superperfect", 1, 80000.0, 3800.0, 6000.0)}
						},
						new JsonObject[][]{{createReward("TPS", 35000, null, 0, null)}}
				));

				quests.add(createQuest(19, "dmz.quest.android19.name", "dmz.quest.android19.desc",
						new JsonObject[][]{{createObjective("STRUCTURE", "dmz.quest.android19.obj1", "dragonminez:kamilookout", null, 0, 0.0, 0.0, 0.0)}},
						new JsonObject[][]{{createReward("TPS", 10000, null, 0, null)}}
				));

				root.add("quests", quests);
				GSON.toJson(root, writer);
			}
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Failed to create default android saga file", e);
		}
	}

	private static JsonObject createQuest(int id, String title, String description, JsonObject[][] objectives, JsonObject[][] rewards) {
		JsonObject quest = new JsonObject();
		quest.addProperty("id", id);
		quest.addProperty("title", title);
		quest.addProperty("description", description);

		JsonArray objectivesArray = new JsonArray();
		for (JsonObject[] objectiveGroup : objectives) {
			for (JsonObject objective : objectiveGroup) {
				objectivesArray.add(objective);
			}
		}
		quest.add("objectives", objectivesArray);

		JsonArray rewardsArray = new JsonArray();
		for (JsonObject[] rewardGroup : rewards) {
			for (JsonObject reward : rewardGroup) {
				rewardsArray.add(reward);
			}
		}
		quest.add("rewards", rewardsArray);

		return quest;
	}

	private static JsonObject createObjective(String type, String description, String target, String entity, int count, Double health, Double meleeDamage, Double kiDamage) {
		JsonObject obj = new JsonObject();
		obj.addProperty("type", type);
		obj.addProperty("description", description);
		if (target != null) {
			if (type.equals("STRUCTURE") || type.equals("BIOME") || type.equals("ITEM")) {
				obj.addProperty(type.toLowerCase(), target);
			}
		}
		if (entity != null) obj.addProperty("entity", entity);
		if (count > 0) obj.addProperty("count", count);

		if (type.equals("KILL") && health != null && health > 0) {
			obj.addProperty("health", health);
			obj.addProperty("meleeDamage", meleeDamage != null ? meleeDamage : 1.0);
			obj.addProperty("kiDamage", kiDamage != null ? kiDamage : 1.0);
		}
		return obj;
	}

	private static JsonObject createReward(String type, int amount, String item, int count, String command) {
		JsonObject reward = new JsonObject();
		reward.addProperty("type", type);
		switch (type) {
			case "TPS" -> reward.addProperty("amount", amount);
			case "ITEM" -> {
				reward.addProperty("item", item);
				reward.addProperty("count", count);
			}
			case "COMMAND" -> reward.addProperty("command", command);
		}
		return reward;
	}

	public static void loadSagas(MinecraftServer server) {
		LOADED_SAGAS.clear();
		if (!ConfigManager.getServerConfig().getGameplay().getStoryModeEnabled()) return;

		if (server == null) {
			LogUtil.warn(Env.COMMON, "Cannot load sagas: server is null");
			return;
		}

		ServerLevel overworld = server.getLevel(Level.OVERWORLD);
		if (overworld == null) {
			LogUtil.warn(Env.COMMON, "Cannot load sagas: overworld is null");
			return;
		}

		Path worldFolder = overworld.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
		Path sagaDir = worldFolder.resolve(SAGA_FOLDER);

		try {
			if (!Files.exists(sagaDir)) {
				Files.createDirectories(sagaDir);
			}
			createDefaultSagaFiles(sagaDir);

			try (var stream = Files.walk(sagaDir)) {
				stream.filter(path -> path.toString().endsWith(".json"))
						.forEach(SagaManager::loadSagaFile);
			}

			LogUtil.info(Env.COMMON, "Loaded {} saga(s) from world folder", LOADED_SAGAS.size());
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Failed to load sagas", e);
		}
	}

	private static void loadSagaFile(Path file) {
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			JsonObject root = GSON.fromJson(reader, JsonObject.class);
			Saga saga = parseSaga(root);
			LOADED_SAGAS.put(saga.getId(), saga);
			LogUtil.info(Env.COMMON, "Loaded saga: {}", saga.getName());
		} catch (Exception e) {
			LogUtil.error(Env.COMMON, "Failed to load saga file: {}", file, e);
		}
	}

	private static Saga parseSaga(JsonObject json) {
		String id = json.get("id").getAsString();
		String name = json.get("name").getAsString();

		Saga.SagaRequirements requirements = null;
		if (json.has("requirements")) {
			JsonObject reqJson = json.getAsJsonObject("requirements");
			String prevSaga = reqJson.has("previousSaga") ? reqJson.get("previousSaga").getAsString() : "";
			requirements = new Saga.SagaRequirements(prevSaga);
		}

		List<Quest> quests = new ArrayList<>();
		if (json.has("quests")) {
			JsonArray questsArray = json.getAsJsonArray("quests");
			for (JsonElement questElement : questsArray) {
				Quest quest = QuestParser.parseQuest(questElement.getAsJsonObject());
				quests.add(quest);
			}
		}

		return new Saga(id, name, quests, requirements);
	}

	public static Map<String, Saga> getAllSagas() {
		if (!ConfigManager.getServerConfig().getGameplay().getStoryModeEnabled()) return null;
		return new HashMap<>(LOADED_SAGAS);
	}

	public static Saga getSaga(String id) {
		return LOADED_SAGAS.get(id);
	}

	public static Map<String, Saga> getClientSagas() {
		if (!ConfigManager.getServerConfig().getGameplay().getStoryModeEnabled()) return null;
		return new HashMap<>(CLIENT_SAGAS);
	}

	public static Saga getClientSaga(String id) {
		return CLIENT_SAGAS.get(id);
	}

	public static void applySyncedSagas(Map<String, Saga> sagas) {
		CLIENT_SAGAS.clear();
		CLIENT_SAGAS.putAll(sagas);
		LogUtil.info(Env.CLIENT, "Loaded {} saga(s) from server", sagas.size());
	}

	@Override
	protected @NotNull Map<String, Saga> prepare(@NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
		return new HashMap<>(LOADED_SAGAS);
	}

	@Override
	protected void apply(@NotNull Map<String, Saga> pObject, @NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
		LOADED_SAGAS.clear();
		LOADED_SAGAS.putAll(pObject);
	}
}
