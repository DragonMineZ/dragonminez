package com.dragonminez.common.quest;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.google.gson.JsonObject;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Extension point for addon quest reward types, mirroring QuestObjectiveRegistry. Reward display
 * text comes from the reward's own {@code getDescription()}, so only parse + client-sync hooks
 * are needed. Subclasses use the {@code QuestReward(String customType)} constructor.
 */
public final class QuestRewardRegistry {

	@FunctionalInterface
	public interface RewardFactory {
		QuestReward fromJson(JsonObject json);
	}

	private record Entry(RewardFactory factory, BiConsumer<QuestReward, JsonObject> syncWriter) {
	}

	private static final Map<String, Entry> ENTRIES = new ConcurrentHashMap<>();

	private QuestRewardRegistry() {
	}

	public static void register(String typeKey, RewardFactory factory,
								BiConsumer<QuestReward, JsonObject> syncWriter) {
		if (typeKey == null || typeKey.isBlank() || factory == null) return;
		ENTRIES.put(normalize(typeKey), new Entry(factory, syncWriter));
	}

	public static boolean isRegistered(String typeKey) {
		return typeKey != null && ENTRIES.containsKey(normalize(typeKey));
	}

	public static QuestReward parse(String typeKey, JsonObject json) {
		Entry entry = typeKey != null ? ENTRIES.get(normalize(typeKey)) : null;
		if (entry == null) return null;
		try {
			return entry.factory().fromJson(json);
		} catch (Exception e) {
			LogUtil.error(Env.COMMON, "Registered reward type '{}' failed to parse: {}", typeKey, e.toString());
			return null;
		}
	}

	public static boolean writeSync(QuestReward reward, JsonObject out) {
		Entry entry = ENTRIES.get(normalize(reward.getTypeKey()));
		if (entry == null || entry.syncWriter() == null) return false;
		entry.syncWriter().accept(reward, out);
		return true;
	}

	private static String normalize(String key) {
		return key.trim().toUpperCase(Locale.ROOT);
	}
}
