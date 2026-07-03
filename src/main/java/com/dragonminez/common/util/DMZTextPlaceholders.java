package com.dragonminez.common.util;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * Central %placeholder% resolver for quest, dialogue, and NPC text. Side-agnostic: pass the
 * player whose data should fill the placeholders (the client player on render paths, the
 * target ServerPlayer on server-composed text).
 *
 * Addons can contribute their own placeholders during mod init:
 * {@code DMZTextPlaceholders.register("mykey", (player, data) -> ...)} resolves %mykey%.
 * Resolvers must tolerate a null StatsData (capability not attached yet).
 */
public final class DMZTextPlaceholders {

	private static final Map<String, BiFunction<Player, StatsData, String>> PLACEHOLDERS = new ConcurrentHashMap<>();

	static {
		register("player", (player, data) -> player.getName().getString());
		register("race", (player, data) -> data != null ? data.getCharacter().getRaceName() : "");
		register("class", (player, data) -> data != null ? safe(data.getCharacter().getCharacterClass()) : "");
		register("level", (player, data) -> data != null ? String.valueOf(data.getLevel()) : "");
		register("alignment", (player, data) -> data != null ? String.valueOf(data.getResources().getAlignment()) : "");
		register("deaths", (player, data) -> data != null ? String.valueOf(data.getStatus().getDeathCount()) : "");
		register("form", (player, data) -> {
			if (data == null) return "";
			String form = data.getCharacter().getActiveForm();
			return form == null || form.isEmpty() ? "base" : form;
		});
		register("dimension", (player, data) -> player.level().dimension().location().getPath());
	}

	private DMZTextPlaceholders() {
	}

	public static void register(String key, BiFunction<Player, StatsData, String> resolver) {
		if (key == null || key.isBlank() || resolver == null) return;
		PLACEHOLDERS.put(key.toLowerCase(Locale.ROOT), resolver);
	}

	public static String apply(String text, Player player) {
		if (text == null || player == null || text.indexOf('%') < 0) return text;
		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).resolve().orElse(null);
		String result = text;
		for (Map.Entry<String, BiFunction<Player, StatsData, String>> entry : PLACEHOLDERS.entrySet()) {
			String token = "%" + entry.getKey() + "%";
			if (!result.contains(token)) continue;
			String value;
			try {
				value = entry.getValue().apply(player, data);
			} catch (Exception ignored) {
				value = "";
			}
			result = result.replace(token, value != null ? value : "");
		}
		return result;
	}

	/**
	 * Applies placeholders to the component's visible text. Translatable components are
	 * flattened to a styled literal only when a placeholder is actually present, so normal
	 * text keeps its full component structure.
	 */
	public static Component apply(Component component, Player player) {
		if (component == null || player == null) return component;
		String visible = component.getString();
		if (visible.indexOf('%') < 0) return component;
		String replaced = apply(visible, player);
		if (replaced.equals(visible)) return component;
		return Component.literal(replaced).withStyle(component.getStyle());
	}

	private static String safe(String value) {
		return value != null ? value : "";
	}
}
