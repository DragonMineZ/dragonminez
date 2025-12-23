package com.dragonminez.common.quest;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SagaManager extends SimplePreparableReloadListener<Map<String, Saga>> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Saga> LOADED_SAGAS = new HashMap<>();
    private static final String SAGA_FOLDER = "dragonminez" + File.separator + "sagas";

    public static void init() {
    }

    private static void createDefaultSagaFile(Path sagaDir) {
        Path defaultFile = sagaDir.resolve("saiyan_saga.json");
        if (!Files.exists(defaultFile)) {
            try {
                Files.createDirectories(sagaDir);
                try (Writer writer = Files.newBufferedWriter(defaultFile, StandardCharsets.UTF_8)) {
                    JsonObject root = new JsonObject();

                    root.addProperty("id", "saiyan_saga");
                    root.addProperty("name", "dmz.saga.saiyan_saga");

                    JsonObject requirements = new JsonObject();
                    requirements.addProperty("previousSaga", "");
                    requirements.addProperty("minLevel", 1);
                    requirements.addProperty("requiredRace", "");
                    root.add("requirements", requirements);

                    JsonArray quests = new JsonArray();

                    JsonObject quest1 = new JsonObject();
                    quest1.addProperty("id", 1);
                    quest1.addProperty("title", "dmz.quest.saiyan1.name");
                    quest1.addProperty("description", "dmz.quest.saiyan1.desc");

                    JsonArray objectives1 = new JsonArray();
                    JsonObject obj1_1 = new JsonObject();
                    obj1_1.addProperty("type", "STRUCTURE");
                    obj1_1.addProperty("description", "dmz.quest.saiyan1.obj1");
                    obj1_1.addProperty("structure", "dragonminez:kame_house");
                    objectives1.add(obj1_1);
                    quest1.add("objectives", objectives1);

                    JsonArray rewards1 = new JsonArray();
                    JsonObject reward1_1 = new JsonObject();
                    reward1_1.addProperty("type", "TPS");
                    reward1_1.addProperty("amount", 500);
                    rewards1.add(reward1_1);
                    quest1.add("rewards", rewards1);

                    quests.add(quest1);

                    JsonObject quest2 = new JsonObject();
                    quest2.addProperty("id", 2);
                    quest2.addProperty("title", "dmz.quest.saiyan2.name");
                    quest2.addProperty("description", "dmz.quest.saiyan2.desc");

                    JsonArray objectives2 = new JsonArray();
                    JsonObject obj2_1 = new JsonObject();
                    obj2_1.addProperty("type", "BIOME");
                    obj2_1.addProperty("description", "dmz.quest.saiyan2.obj1");
                    obj2_1.addProperty("biome", "plains");
                    objectives2.add(obj2_1);

                    JsonObject obj2_2 = new JsonObject();
                    obj2_2.addProperty("type", "KILL");
                    obj2_2.addProperty("description", "dmz.quest.saiyan2.obj2");
                    obj2_2.addProperty("entity", "dragonminez:raditz");
                    obj2_2.addProperty("count", 1);
                    objectives2.add(obj2_2);
                    quest2.add("objectives", objectives2);

                    JsonArray rewards2 = new JsonArray();
                    JsonObject reward2_1 = new JsonObject();
                    reward2_1.addProperty("type", "TPS");
                    reward2_1.addProperty("amount", 1000);
                    rewards2.add(reward2_1);
                    quest2.add("rewards", rewards2);

                    quests.add(quest2);

                    JsonObject quest3 = new JsonObject();
                    quest3.addProperty("id", 3);
                    quest3.addProperty("title", "dmz.quest.saiyan3.name");
                    quest3.addProperty("description", "dmz.quest.saiyan3.desc");

                    JsonArray objectives3 = new JsonArray();
                    JsonObject obj3_1 = new JsonObject();
                    obj3_1.addProperty("type", "BIOME");
                    obj3_1.addProperty("description", "dmz.quest.saiyan3.obj1");
                    obj3_1.addProperty("biome", "mountain");
                    objectives3.add(obj3_1);

                    JsonObject obj3_2 = new JsonObject();
                    obj3_2.addProperty("type", "KILL");
                    obj3_2.addProperty("description", "dmz.quest.saiyan3.obj2");
                    obj3_2.addProperty("entity", "dragonminez:saga_saibaman1");
                    obj3_2.addProperty("count", 6);
                    objectives3.add(obj3_2);
                    quest3.add("objectives", objectives3);

                    JsonArray rewards3 = new JsonArray();
                    JsonObject reward3_1 = new JsonObject();
                    reward3_1.addProperty("type", "TPS");
                    reward3_1.addProperty("amount", 1500);
                    rewards3.add(reward3_1);

                    JsonObject reward3_2 = new JsonObject();
                    reward3_2.addProperty("type", "COMMAND");
                    reward3_2.addProperty("command", "dmzpoints %player% add 500");
                    rewards3.add(reward3_2);
                    quest3.add("rewards", rewards3);

                    quests.add(quest3);

                    root.add("quests", quests);

                    GSON.toJson(root, writer);
                }
            } catch (IOException e) {
                LogUtil.error(Env.COMMON, "Failed to create default saga file", e);
            }
        }
    }

    public static void loadSagas(MinecraftServer server) {
        LOADED_SAGAS.clear();

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
                createDefaultSagaFile(sagaDir);
            }

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
            if (saga != null) {
                LOADED_SAGAS.put(saga.getId(), saga);
                LogUtil.debug(Env.COMMON, "Loaded saga: {}", saga.getName());
            }
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
            int minLevel = reqJson.has("minLevel") ? reqJson.get("minLevel").getAsInt() : 0;
            String race = reqJson.has("requiredRace") ? reqJson.get("requiredRace").getAsString() : "";
            requirements = new Saga.SagaRequirements(prevSaga, minLevel, race);
        }

        List<Quest> quests = new ArrayList<>();
        if (json.has("quests")) {
            JsonArray questsArray = json.getAsJsonArray("quests");
            for (JsonElement questElement : questsArray) {
                Quest quest = QuestParser.parseQuest(questElement.getAsJsonObject());
                if (quest != null) {
                    quests.add(quest);
                }
            }
        }

        return new Saga(id, name, quests, requirements);
    }

    public static Map<String, Saga> getAllSagas() {
        return new HashMap<>(LOADED_SAGAS);
    }

    public static Saga getSaga(String id) {
        return LOADED_SAGAS.get(id);
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

