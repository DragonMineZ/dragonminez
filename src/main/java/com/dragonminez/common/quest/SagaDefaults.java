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
 * Generates the default saga JSON manifest files on first run.
 * <p>
 * Saga manifests are lightweight files that define saga metadata and reference
 * a quest folder (e.g. {@code "questFolder": "saga_saiyan"}). The actual quest
 * definitions live as individual files inside {@code dragonminez/quests/<questFolder>/}.
 * <p>
 * Called by {@link QuestRegistry} during saga loading.
 *
 * @since 2.1
 */
final class SagaDefaults {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private SagaDefaults() {} // utility class

	/**
	 * Creates default saga manifest files if enabled in config and the files don't exist yet.
	 */
	static void createDefaultSagaFiles(Path sagaDir) {
		if (!ConfigManager.getServerConfig().getGameplay().getStoryModeEnabled()) return;
		if (!ConfigManager.getServerConfig().getGameplay().getCreateDefaultSagas()) return;

		writeSagaManifest(sagaDir, "saiyan_saga.json", "saiyan_saga", "dmz.saga.saiyan_saga", "", "saga_saiyan");
		writeSagaManifest(sagaDir, "frieza_saga.json", "frieza_saga", "dmz.saga.frieza_saga", "saiyan_saga", "saga_frieza");
		writeSagaManifest(sagaDir, "android_saga.json", "android_saga", "dmz.saga.android_saga", "frieza_saga", "saga_android");
	}

	/**
	 * Writes a single saga manifest JSON file if it doesn't exist or is in the old embedded format.
	 */
	private static void writeSagaManifest(Path sagaDir, String filename, String sagaId, String name, String previousSaga, String questFolder) {
		Path file = sagaDir.resolve(filename);

		// If file exists and already uses questFolder format, skip
		if (Files.exists(file) && !isOldEmbeddedFormat(file)) return;

		try {
			Files.createDirectories(sagaDir);
			try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				JsonObject root = new JsonObject();
				root.addProperty("id", sagaId);
				root.addProperty("name", name);

				JsonObject req = new JsonObject();
				req.addProperty("previousSaga", previousSaga);
				root.add("requirements", req);

				root.addProperty("questFolder", questFolder);

				GSON.toJson(root, w);
			}
			LogUtil.info(Env.COMMON, "Created saga manifest: {}", filename);
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Failed to create saga manifest: {}", filename, e);
		}
	}

	/**
	 * Checks if an existing saga file uses the old embedded format (has "quests" array instead of "questFolder").
	 */
	private static boolean isOldEmbeddedFormat(Path file) {
		try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			return root.has("quests") && !root.has("questFolder");
		} catch (Exception e) {
			return false;
		}
	}
}
