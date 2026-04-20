package com.dragonminez.common.spacepod;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

		throw new IllegalArgumentException("unlock_rules object must contain one of: and, or, not");
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
