package com.dragonminez.common.quest;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.google.gson.*;

import java.io.IOException;
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

	private SagaDefaults() {} // utility class

	/**
	 * Creates default saga manifest files if enabled in config and the files don't exist yet.
	 */
	static void createDefaultSagaFiles(Path sagaDir) {
		if (!ConfigManager.getServerConfig().getGameplay().getStoryModeEnabled()) return;
		if (!ConfigManager.getServerConfig().getGameplay().getCreateDefaultSagas()) return;

		Path dmzBase = sagaDir.getParent(); // <world>/dragonminez

		writeSagaManifest(dmzBase, sagaDir, "saiyan_saga.json", "saiyan_saga", "dmz.saga.saiyan_saga", "", "saga_saiyan");
		writeSagaManifest(dmzBase, sagaDir, "frieza_saga.json", "frieza_saga", "dmz.saga.frieza_saga", "saiyan_saga", "saga_frieza");
		writeSagaManifest(dmzBase, sagaDir, "android_saga.json", "android_saga", "dmz.saga.android_saga", "frieza_saga", "saga_android");
		writeSagaManifest(dmzBase, sagaDir, "future_saga.json", "future_saga", "dmz.saga.future_saga", "android_saga", "saga_future");
		writeSagaManifest(dmzBase, sagaDir, "buu_saga.json", "buu_saga", "dmz.saga.buu_saga", "android_saga", "saga_buu");
		writeSagaManifest(dmzBase, sagaDir, "movies_saga.json", "movies_saga", "dmz.saga.movies_saga", "", "saga_movies");
	}

	/**
	 * Writes a single saga manifest JSON file on first run, or upgrades it in place if outdated.
	 */
	private static void writeSagaManifest(Path dmzBase, Path sagaDir, String filename, String sagaId, String name, String previousSaga, String questFolder) {
		try {
			Files.createDirectories(sagaDir);

			JsonObject root = new JsonObject();
			root.addProperty("id", sagaId);
			root.addProperty("name", name);

			JsonObject req = new JsonObject();
			req.addProperty("previousSaga", previousSaga);
			root.add("requirements", req);

			root.addProperty("questFolder", questFolder);

			QuestUpgrader.upgradeOrWrite(dmzBase, sagaDir.resolve(filename), root);
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Failed to create saga manifest: {}", filename, e);
		}
	}

}
