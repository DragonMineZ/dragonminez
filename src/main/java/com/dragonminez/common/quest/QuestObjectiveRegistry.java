package com.dragonminez.common.quest;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Extension point for addon quest objective types. Register during mod init:
 *
 * <pre>{@code
 * QuestObjectiveRegistry.register("MY_TYPE",
 *     json -> new MyObjective(json.get("value").getAsString()),          // parse from quest JSON
 *     (objective, out) -> out.addProperty("value", ((MyObjective) objective).getValue()), // client sync
 *     objective -> Component.literal("Do the thing"));                    // HUD/journal text
 * }</pre>
 *
 * Objective subclasses use the {@code QuestObjective(String customType, int required)} constructor
 * so their JSON {@code "type"} round-trips through parsing, client sync, and display. Progress
 * routing stays event-driven: addons listen to their own game events and advance progress via
 * {@code PlayerQuestData.setObjectiveProgress} like the built-ins do in QuestEvents.
 */
public final class QuestObjectiveRegistry {

	@FunctionalInterface
	public interface ObjectiveFactory {
		QuestObjective fromJson(JsonObject json);
	}

	private record Entry(ObjectiveFactory factory,
						 BiConsumer<QuestObjective, JsonObject> syncWriter,
						 Function<QuestObjective, Component> describer) {
	}

	private static final Map<String, Entry> ENTRIES = new ConcurrentHashMap<>();

	private QuestObjectiveRegistry() {
	}

	public static void register(String typeKey, ObjectiveFactory factory,
								BiConsumer<QuestObjective, JsonObject> syncWriter,
								Function<QuestObjective, Component> describer) {
		if (typeKey == null || typeKey.isBlank() || factory == null) return;
		ENTRIES.put(normalize(typeKey), new Entry(factory, syncWriter, describer));
	}

	public static boolean isRegistered(String typeKey) {
		return typeKey != null && ENTRIES.containsKey(normalize(typeKey));
	}

	/** Parses a registered objective; returns null (and logs) for unknown keys or factory errors. */
	public static QuestObjective parse(String typeKey, JsonObject json) {
		Entry entry = typeKey != null ? ENTRIES.get(normalize(typeKey)) : null;
		if (entry == null) return null;
		try {
			return entry.factory().fromJson(json);
		} catch (Exception e) {
			LogUtil.error(Env.COMMON, "Registered objective type '{}' failed to parse: {}", typeKey, e.toString());
			return null;
		}
	}

	/** Writes the objective's custom fields for client sync. Returns false when unhandled. */
	public static boolean writeSync(QuestObjective objective, JsonObject out) {
		Entry entry = ENTRIES.get(normalize(objective.getTypeKey()));
		if (entry == null || entry.syncWriter() == null) return false;
		entry.syncWriter().accept(objective, out);
		return true;
	}

	/** Display text for a registered objective, or null when none is registered. */
	public static Component describe(QuestObjective objective) {
		Entry entry = ENTRIES.get(normalize(objective.getTypeKey()));
		return entry != null && entry.describer() != null ? entry.describer().apply(objective) : null;
	}

	private static String normalize(String key) {
		return key.trim().toUpperCase(Locale.ROOT);
	}
}
