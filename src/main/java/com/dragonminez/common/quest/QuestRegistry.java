package com.dragonminez.common.quest;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Unified registry for all quest types — saga quests, side-quests, dailies, and events.
 * <p>
 * This is the <b>sole quest loader</b>. It loads:
 * <ul>
 *   <li><b>Saga manifest files</b> from {@code dragonminez/sagas/} — each saga is its own JSON with embedded quests</li>
 *   <li><b>Side-quest files</b> from {@code dragonminez/sidequests/} — each quest is its own JSON</li>
 *   <li><b>Individual quest files</b> from {@code dragonminez/quests/} — new unified format (future expansion)</li>
 * </ul>
 * <p>
 * On first run, default saga and side-quest files are generated automatically via
 * {@link SagaDefaults} and {@link SideQuestDefaults}.
 *
 * <h3>Folder Structure</h3>
 * <pre>
 * worldfolder/
 *   dragonminez/
 *     sagas/                           ← Saga manifest files (reference quest folders)
 *       saiyan_saga.json               →  {"id":"saiyan_saga", "questFolder":"saga_saiyan", ...}
 *       frieza_saga.json
 *       android_saga.json
 *     quests/                          ← Quest files organized by saga folder
 *       saga_saiyan/
 *         01_find_roshi.json           →  {"id":1, "title":"...", "objectives":[...], "rewards":[...]}
 *         02_defeat_raditz.json
 *         ...
 *       saga_frieza/
 *         01_defeat_cui.json
 *         ...
 *     sidequests/                      ← Side-quest JSON files
 *       training/
 *         roshi_basic_training.json
 *       exploration/
 *         world_explorer.json
 *       combat/
 *         monster_hunter.json
 * </pre>
 *
 * @since 2.1
 */
public class QuestRegistry extends SimplePreparableReloadListener<Map<String, Quest>> {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String SAGA_FOLDER = "dragonminez" + File.separator + "sagas";
    private static final String SIDEQUEST_FOLDER = "dragonminez" + File.separator + "sidequests";
    private static final String QUESTS_FOLDER = "dragonminez" + File.separator + "quests";

    // ========================================================================================
    // Server-Side State
    // ========================================================================================

    /** All loaded quests, keyed by their effective string ID. */
    private static final Map<String, Quest> LOADED_QUESTS = new LinkedHashMap<>();

    /** All loaded saga manifests, keyed by saga ID. */
    private static final Map<String, Saga> LOADED_SAGAS = new LinkedHashMap<>();

    // ========================================================================================
    // Client-Side State (synced from server)
    // ========================================================================================

    private static final Map<String, Quest> CLIENT_QUESTS = new LinkedHashMap<>();
    private static final Map<String, Saga> CLIENT_SAGAS = new LinkedHashMap<>();

    // ========================================================================================
    // Performance Indexes
    // ========================================================================================

    /** ObjectiveType → list of quest IDs containing that objective type. */
    private static final Map<QuestObjective.ObjectiveType, List<String>> OBJECTIVE_INDEX = new EnumMap<>(QuestObjective.ObjectiveType.class);

    /** NPC ID → list of quest IDs where that NPC is the quest giver. */
    private static final Map<String, List<String>> QUEST_GIVER_INDEX = new HashMap<>();

    /** NPC ID → list of quest IDs where that NPC is the turn-in target. */
    private static final Map<String, List<String>> TURN_IN_INDEX = new HashMap<>();

    /** Cached world folder path for resolving quest folders during saga loading. */
    private static Path cachedWorldFolder = null;

    // ========================================================================================
    // Initialization
    // ========================================================================================

    /** Called during mod setup to ensure the class is loaded. */
    public static void init() {
        // No-op — ensures class is loaded
    }

    // ========================================================================================
    // Loading — Primary Entry Point
    // ========================================================================================

    /**
     * Loads all quests from all sources. This is the sole loading method.
     * <p>
     * Loading order:
     * <ol>
     *   <li>Generate default quest files in {@code dragonminez/quests/} if missing</li>
     *   <li>Load saga manifest files from {@code dragonminez/sagas/} — each saga references a quest folder</li>
     *   <li>Load side-quest JSON files from {@code dragonminez/sidequests/}</li>
     *   <li>Build performance indexes</li>
     * </ol>
     */
    public static void loadAll(@Nullable MinecraftServer server) {
        LOADED_QUESTS.clear();
        LOADED_SAGAS.clear();
        OBJECTIVE_INDEX.clear();
        QUEST_GIVER_INDEX.clear();
        TURN_IN_INDEX.clear();

        if (server == null) {
            LogUtil.warn(Env.COMMON, "QuestRegistry: cannot load — server is null");
            return;
        }

        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            LogUtil.warn(Env.COMMON, "QuestRegistry: cannot load — overworld is null");
            return;
        }

        Path worldFolder = overworld.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
        cachedWorldFolder = worldFolder;

        // --- Step 1: Generate default quest files (before sagas, so sagas can reference them) ---
        Path questsDir = worldFolder.resolve(QUESTS_FOLDER);
        try {
            if (!Files.exists(questsDir)) Files.createDirectories(questsDir);
            QuestDefaults.createDefaultQuestFiles(questsDir);
        } catch (IOException e) {
            LogUtil.error(Env.COMMON, "Failed to create quests directory", e);
        }

        // --- Step 2: Load sagas (reads quest files from quests/ subfolders or embedded quests) ---
        if (ConfigManager.getServerConfig().getGameplay().getStoryModeEnabled()) {
            loadSagaFiles(worldFolder.resolve(SAGA_FOLDER));
        }

        // --- Step 3: Load side-quests ---
        if (ConfigManager.getServerConfig().getGameplay().getSideQuestsEnabled()) {
            loadSideQuestFiles(worldFolder.resolve(SIDEQUEST_FOLDER));
        }


        // --- Step 4: Build indexes ---
        buildIndexes();

        int sagaQuestCount = 0;
        for (Saga saga : LOADED_SAGAS.values()) {
            sagaQuestCount += saga.getQuests().size();
        }
        int sideCount = (int) LOADED_QUESTS.values().stream().filter(Quest::isSideQuest).count();
        LogUtil.info(Env.COMMON, "QuestRegistry: loaded {} quest(s) ({} saga quests across {} sagas, {} sidequests)",
                LOADED_QUESTS.size(), sagaQuestCount, LOADED_SAGAS.size(), sideCount);
    }

    // ========================================================================================
    // Saga Loading
    // ========================================================================================

    /**
     * Loads saga JSON files from the given directory. Generates defaults if configured.
     */
    private static void loadSagaFiles(Path sagaDir) {
        try {
            if (!Files.exists(sagaDir)) {
                Files.createDirectories(sagaDir);
            }
            SagaDefaults.createDefaultSagaFiles(sagaDir);

            try (var stream = Files.walk(sagaDir)) {
                stream.filter(path -> path.toString().endsWith(".json"))
                        .forEach(QuestRegistry::loadSingleSagaFile);
            }
        } catch (IOException e) {
            LogUtil.error(Env.COMMON, "Failed to load saga files from {}", sagaDir, e);
        }
    }

    private static void loadSingleSagaFile(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            Saga saga = parseSagaFromJson(root);
            LOADED_SAGAS.put(saga.getId(), saga);

            // Index each saga quest by composite key
            for (Quest quest : saga.getQuests()) {
                String effectiveId = saga.getId() + ":" + quest.getId();
                LOADED_QUESTS.put(effectiveId, quest);
            }

            LogUtil.info(Env.COMMON, "Loaded saga: {} ({} quests)", saga.getName(), saga.getQuests().size());
        } catch (Exception e) {
            LogUtil.error(Env.COMMON, "Failed to load saga file: {}", file.getFileName(), e);
        }
    }

    /**
     * Parses a Saga from a JSON object. Used by the loader and the network sync packet.
     * <p>
     * Supports two formats:
     * <ul>
     *   <li><b>New format:</b> {@code "questFolder": "saga_saiyan"} — loads quest files from
     *       {@code dragonminez/quests/saga_saiyan/}, sorted by filename (e.g. {@code 01_find_roshi.json}).</li>
     *   <li><b>Legacy format:</b> {@code "quests": [...]} — quests embedded directly in the saga file.</li>
     * </ul>
     */
    public static Saga parseSagaFromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        String name = json.get("name").getAsString();

        Saga.SagaRequirements requirements = null;
        if (json.has("requirements")) {
            JsonObject reqJson = json.getAsJsonObject("requirements");
            String prevSaga = reqJson.has("previousSaga") ? reqJson.get("previousSaga").getAsString() : "";
            requirements = new Saga.SagaRequirements(prevSaga);
        }

        List<Quest> quests = new ArrayList<>();

        // New format: questFolder references a subfolder in dragonminez/quests/
        if (json.has("questFolder") && cachedWorldFolder != null) {
            String folderName = json.get("questFolder").getAsString();
            Path questFolder = cachedWorldFolder.resolve(QUESTS_FOLDER).resolve(folderName);
            if (Files.exists(questFolder)) {
                quests = loadQuestsFromFolder(questFolder);
            } else {
                LogUtil.warn(Env.COMMON, "Saga '{}' references questFolder '{}' but it doesn't exist", id, folderName);
            }
        }
        // Legacy format: quests embedded in the saga JSON
        else if (json.has("quests")) {
            JsonArray questsArray = json.getAsJsonArray("quests");
            for (JsonElement questElement : questsArray) {
                Quest quest = QuestParser.parseSagaQuest(questElement.getAsJsonObject());
                quests.add(quest);
            }
        }

        return new Saga(id, name, quests, requirements);
    }

    /**
     * Loads quest files from a folder, sorted by filename. Each file is parsed as a saga quest
     * (numeric ID format). Files are sorted alphabetically so numeric prefixes control ordering
     * (e.g. {@code 01_find_roshi.json} loads before {@code 02_defeat_raditz.json}).
     */
    private static List<Quest> loadQuestsFromFolder(Path folder) {
        List<Quest> quests = new ArrayList<>();
        try (var stream = Files.list(folder)) {
            List<Path> files = stream
                    .filter(p -> p.toString().endsWith(".json"))
                    .sorted()
                    .toList();

            for (Path file : files) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonObject root = GSON.fromJson(reader, JsonObject.class);
                    Quest quest = QuestParser.parseQuest(root);
                    if (quest != null) quests.add(quest);
                } catch (Exception e) {
                    LogUtil.error(Env.COMMON, "Failed to load quest file: {}", file.getFileName(), e);
                }
            }
        } catch (IOException e) {
            LogUtil.error(Env.COMMON, "Failed to list quest files in folder: {}", folder, e);
        }
        return quests;
    }

    // ========================================================================================
    // Side-Quest Loading
    // ========================================================================================

    /**
     * Loads side-quest JSON files from the given directory. Generates defaults if configured.
     */
    private static void loadSideQuestFiles(Path sideQuestDir) {
        try {
            if (!Files.exists(sideQuestDir)) {
                Files.createDirectories(sideQuestDir);
            }
            SideQuestDefaults.createDefaultSideQuestFiles(sideQuestDir);

            try (var stream = Files.walk(sideQuestDir)) {
                stream.filter(path -> path.toString().endsWith(".json"))
                        .forEach(QuestRegistry::loadSingleSideQuestFile);
            }
        } catch (IOException e) {
            LogUtil.error(Env.COMMON, "Failed to load side-quest files from {}", sideQuestDir, e);
        }
    }

    private static void loadSingleSideQuestFile(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            // Use unified parser — supports both old camelCase and new snake_case field names
            Quest quest = QuestParser.parseQuest(root);
            if (quest != null) {
                String effectiveId = quest.getStringId() != null ? quest.getStringId() : quest.getEffectiveId();
                LOADED_QUESTS.put(effectiveId, quest);
            }
        } catch (Exception e) {
            LogUtil.error(Env.COMMON, "Failed to load side-quest file: {}", file.getFileName(), e);
        }
    }


    // ========================================================================================
    // Index Building
    // ========================================================================================

    private static void buildIndexes() {
        for (Map.Entry<String, Quest> entry : LOADED_QUESTS.entrySet()) {
            String questId = entry.getKey();
            Quest quest = entry.getValue();

            // Objective type index
            for (QuestObjective objective : quest.getObjectives()) {
                OBJECTIVE_INDEX.computeIfAbsent(objective.getType(), k -> new ArrayList<>()).add(questId);
            }

            // Quest giver index
            if (quest.getQuestGiver() != null && !quest.getQuestGiver().isEmpty()) {
                QUEST_GIVER_INDEX.computeIfAbsent(quest.getQuestGiver(), k -> new ArrayList<>()).add(questId);
            }

            // Turn-in index
            if (quest.getTurnIn() != null && !quest.getTurnIn().isEmpty()) {
                TURN_IN_INDEX.computeIfAbsent(quest.getTurnIn(), k -> new ArrayList<>()).add(questId);
            }
        }
    }

    // ========================================================================================
    // Server-Side Lookups
    // ========================================================================================

    /** Returns a quest by its string ID, or {@code null} if not found. */
    @Nullable
    public static Quest getQuest(String questId) {
        return LOADED_QUESTS.get(questId);
    }

    /** Returns an unmodifiable view of all loaded quests. */
    public static Map<String, Quest> getAllQuests() {
        return Collections.unmodifiableMap(LOADED_QUESTS);
    }

    /** Returns a saga by its saga ID, or {@code null} if not found. */
    @Nullable
    public static Saga getSaga(String sagaId) {
        return LOADED_SAGAS.get(sagaId);
    }

    /** Returns an unmodifiable view of all loaded sagas. */
    public static Map<String, Saga> getAllSagas() {
        return Collections.unmodifiableMap(LOADED_SAGAS);
    }

    /** Returns all quest IDs that contain the given objective type. */
    public static List<String> getQuestIdsByObjectiveType(QuestObjective.ObjectiveType type) {
        return OBJECTIVE_INDEX.getOrDefault(type, Collections.emptyList());
    }

    /** Returns all quest IDs where the given NPC is the quest giver. */
    public static List<String> getQuestIdsByGiver(String npcId) {
        return QUEST_GIVER_INDEX.getOrDefault(npcId, Collections.emptyList());
    }

    /** Returns all quest IDs where the given NPC is the turn-in target. */
    public static List<String> getQuestIdsByTurnIn(String npcId) {
        return TURN_IN_INDEX.getOrDefault(npcId, Collections.emptyList());
    }

    /** Returns all quests of a given type. */
    public static List<Quest> getQuestsByType(Quest.QuestType type) {
        List<Quest> result = new ArrayList<>();
        for (Quest quest : LOADED_QUESTS.values()) {
            if (quest.getType() == type) result.add(quest);
        }
        return result;
    }

    /** Returns all quests belonging to a given category. */
    public static List<Quest> getQuestsByCategory(String category) {
        List<Quest> result = new ArrayList<>();
        for (Quest quest : LOADED_QUESTS.values()) {
            if (category.equalsIgnoreCase(quest.getCategory())) result.add(quest);
        }
        return result;
    }

    // ========================================================================================
    // Client-Side Lookups
    // ========================================================================================

    @Nullable
    public static Quest getClientQuest(String questId) {
        return CLIENT_QUESTS.get(questId);
    }

    public static Map<String, Quest> getClientQuests() {
        return Collections.unmodifiableMap(CLIENT_QUESTS);
    }

    @Nullable
    public static Saga getClientSaga(String sagaId) {
        return CLIENT_SAGAS.get(sagaId);
    }

    public static Map<String, Saga> getClientSagas() {
        return Collections.unmodifiableMap(CLIENT_SAGAS);
    }

    // ========================================================================================
    // Client Sync
    // ========================================================================================

    public static void applySyncedQuests(Map<String, Quest> quests) {
        CLIENT_QUESTS.clear();
        CLIENT_QUESTS.putAll(quests);
        LogUtil.info(Env.CLIENT, "QuestRegistry: synced {} quest(s) from server", quests.size());
    }

    public static void applySyncedSagas(Map<String, Saga> sagas) {
        CLIENT_SAGAS.clear();
        CLIENT_SAGAS.putAll(sagas);
        LogUtil.info(Env.CLIENT, "QuestRegistry: synced {} saga(s) from server", sagas.size());
    }

    // ========================================================================================
    // Resource Reload Listener
    // ========================================================================================

    @Override
    protected @NotNull Map<String, Quest> prepare(@NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
        return new HashMap<>(LOADED_QUESTS);
    }

    @Override
    protected void apply(@NotNull Map<String, Quest> pObject, @NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
        LOADED_QUESTS.clear();
        LOADED_QUESTS.putAll(pObject);
        buildIndexes();
    }
}

