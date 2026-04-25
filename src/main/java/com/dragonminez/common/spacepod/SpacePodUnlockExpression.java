package com.dragonminez.common.spacepod;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public interface SpacePodUnlockExpression {

	boolean test(Player player);

	static SpacePodUnlockExpression fromJson(JsonElement element) {
		if (element == null || element.isJsonNull()) {
			throw new IllegalArgumentException("unlock_rules is required");
		}

		if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
			return new Primitive(element.getAsString());
		}

		if (!element.isJsonObject()) {
			throw new IllegalArgumentException("unlock_rules must be a string or an object");
		}

		JsonObject object = element.getAsJsonObject();
		if (object.has("and")) {
			return new And(parseArray(object.get("and"), "and"));
		}
		if (object.has("or")) {
			return new Or(parseArray(object.get("or"), "or"));
		}
		if (object.has("not")) {
			return new Not(fromJson(object.get("not")));
		}
		if (object.has("quest")) {
			return new QuestCompleted(getRequiredString(object, "quest"));
		}
		if (object.has("level")) {
			return new LevelAtLeast(object.get("level").getAsInt());
		}
		if (object.has("race")) {
			return new RaceMatches(getRequiredString(object, "race"));
		}
		if (object.has("tag")) {
			return new PlayerTag(getRequiredString(object, "tag"));
		}
		if (object.has("visited_dimension")) {
			String dimension = getRequiredString(object, "visited_dimension");
			if (ResourceLocation.tryParse(dimension) == null) {
				throw new IllegalArgumentException("unlock_rules.visited_dimension must be a valid resource location");
			}
			return new VisitedDimension(dimension);
		}
		if (object.has("stat")) {
			JsonElement statElement = object.get("stat");
			if (statElement == null || !statElement.isJsonObject()) {
				throw new IllegalArgumentException("unlock_rules.stat must be an object");
			}
			JsonObject statObject = statElement.getAsJsonObject();
			return new StatAtLeast(getRequiredString(statObject, "name"), statObject.get("min").getAsInt());
		}

		throw new IllegalArgumentException("unlock_rules object must contain one of: and, or, not, quest, level, race, tag, visited_dimension, stat");
	}

	private static List<SpacePodUnlockExpression> parseArray(JsonElement element, String key) {
		if (element == null || !element.isJsonArray()) {
			throw new IllegalArgumentException("unlock_rules." + key + " must be an array");
		}

		JsonArray array = element.getAsJsonArray();
		if (array.isEmpty()) {
			throw new IllegalArgumentException("unlock_rules." + key + " must not be empty");
		}

		List<SpacePodUnlockExpression> expressions = new ArrayList<>();
		for (JsonElement child : array) {
			expressions.add(fromJson(child));
		}
		return expressions;
	}

	private static String getRequiredString(JsonObject object, String key) {
		if (!object.has(key) || object.get(key).isJsonNull()) {
			throw new IllegalArgumentException("Missing required unlock_rules field '" + key + "'");
		}
		return object.get(key).getAsString();
	}

	private static StatsData getStatsData(Player player) {
		return StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
	}

	final class Primitive implements SpacePodUnlockExpression {
		private final Rule rule;

		private Primitive(String rawRule) {
			this.rule = Rule.valueOf(rawRule.toUpperCase(Locale.ROOT));
		}

		public Rule rule() {
			return rule;
		}

		@Override
		public boolean test(Player player) {
			return switch (rule) {
				case ALWAYS -> true;
				case NEVER -> false;
				case KAIO_UNLOCKED -> {
					final boolean[] unlocked = {false};
					StatsProvider.get(StatsCapability.INSTANCE, player)
							.ifPresent(cap -> unlocked[0] = cap.getStatus().isInKaioPlanet());
					yield unlocked[0];
				}
				case OTHERWORLD_ENABLED -> ConfigManager.getServerConfig().getWorldGen().getOtherworldActive();
			};
		}
	}

	final class QuestCompleted implements SpacePodUnlockExpression {
		private final String questId;

		public QuestCompleted(String questId) {
			this.questId = questId;
		}

		public String questId() {
			return questId;
		}

		@Override
		public boolean test(Player player) {
			StatsData data = getStatsData(player);
			return data != null && data.getPlayerQuestData().isQuestCompleted(questId);
		}
	}

	final class LevelAtLeast implements SpacePodUnlockExpression {
		private final int level;

		public LevelAtLeast(int level) {
			this.level = level;
		}

		public int level() {
			return level;
		}

		@Override
		public boolean test(Player player) {
			StatsData data = getStatsData(player);
			return data != null && data.getLevel() >= level;
		}
	}

	final class StatAtLeast implements SpacePodUnlockExpression {
		private final String statName;
		private final int min;

		public StatAtLeast(String statName, int min) {
			this.statName = statName.toUpperCase(Locale.ROOT);
			this.min = min;
		}

		public String statName() {
			return statName;
		}

		public int min() {
			return min;
		}

		@Override
		public boolean test(Player player) {
			StatsData data = getStatsData(player);
			return data != null && data.getCurrentStatValue(statName) >= min;
		}
	}

	final class RaceMatches implements SpacePodUnlockExpression {
		private final String race;

		public RaceMatches(String race) {
			this.race = race.toLowerCase(Locale.ROOT);
		}

		public String race() {
			return race;
		}

		@Override
		public boolean test(Player player) {
			StatsData data = getStatsData(player);
			return data != null && data.getCharacter().getRaceName().equalsIgnoreCase(race);
		}
	}

	final class PlayerTag implements SpacePodUnlockExpression {
		private final String tag;

		public PlayerTag(String tag) {
			this.tag = tag;
		}

		public String tag() {
			return tag;
		}

		@Override
		public boolean test(Player player) {
			return player.getTags().contains(tag);
		}
	}

	final class VisitedDimension implements SpacePodUnlockExpression {
		private final String dimensionId;

		public VisitedDimension(String dimensionId) {
			this.dimensionId = dimensionId;
		}

		public String dimensionId() {
			return dimensionId;
		}

		@Override
		public boolean test(Player player) {
			StatsData data = getStatsData(player);
			return data != null && data.getStatus().hasVisitedDimension(dimensionId);
		}
	}

	final class And implements SpacePodUnlockExpression {
		private final List<SpacePodUnlockExpression> children;

		private And(List<SpacePodUnlockExpression> children) {
			this.children = children;
		}

		public List<SpacePodUnlockExpression> children() {
			return children;
		}

		@Override
		public boolean test(Player player) {
			for (SpacePodUnlockExpression child : children) {
				if (!child.test(player)) {
					return false;
				}
			}
			return true;
		}
	}

	final class Or implements SpacePodUnlockExpression {
		private final List<SpacePodUnlockExpression> children;

		private Or(List<SpacePodUnlockExpression> children) {
			this.children = children;
		}

		public List<SpacePodUnlockExpression> children() {
			return children;
		}

		@Override
		public boolean test(Player player) {
			for (SpacePodUnlockExpression child : children) {
				if (child.test(player)) {
					return true;
				}
			}
			return false;
		}
	}

	final class Not implements SpacePodUnlockExpression {
		private final SpacePodUnlockExpression child;

		private Not(SpacePodUnlockExpression child) {
			this.child = child;
		}

		public SpacePodUnlockExpression child() {
			return child;
		}

		@Override
		public boolean test(Player player) {
			return !child.test(player);
		}
	}

	enum Rule {
		ALWAYS,
		NEVER,
		KAIO_UNLOCKED,
		OTHERWORLD_ENABLED
	}
}
