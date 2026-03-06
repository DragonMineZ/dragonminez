package com.dragonminez.common.quest;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/**
 * Utility for resolving quest text fields that may be either translation keys or literal strings.
 * <p>
 * If the value looks like a translation key (contains {@code "."}), it is resolved through
 * Minecraft's I18n system. Otherwise, it is returned as literal text.
 * <p>
 * This allows default quests to use translation keys for multi-language support
 * (e.g. {@code "dmz.quest.saiyan1.name"}), while custom/server-defined quests can use
 * plain text without needing lang files (e.g. {@code "My Custom Quest"}).
 *
 * @since 2.0
 */
public class QuestTextResolver {

    /**
     * The prefix used by default quest translation keys.
     * If a value starts with this prefix, it is always treated as a translation key.
     */
    private static final String DMZ_KEY_PREFIX = "dmz.";

    /**
     * Returns the resolved display text for a quest field.
     * <p>
     * <b>Client-side only</b> — uses {@link I18n} which is not available on the server.
     *
     * @param value the raw string from the JSON file (translation key or literal)
     * @return the resolved display text
     */
    public static String resolve(String value) {
        if (value == null) return "";
        if (isTranslationKey(value)) {
            String translated = I18n.get(value);
            // If I18n returns the key itself, no translation exists — use the key as fallback
            return translated.equals(value) ? value : translated;
        }
        return value;
    }

    /**
     * Returns the value as a {@link Component}, suitable for server-side chat messages
     * and command feedback.
     * <p>
     * If the value is a translation key, returns {@link Component#translatable(String)}.
     * Otherwise, returns {@link Component#literal(String)}.
     *
     * @param value the raw string from the JSON file
     * @return the value as a Component
     */
    public static Component toComponent(String value) {
        if (value == null) return Component.empty();
        if (isTranslationKey(value)) {
            return Component.translatable(value);
        }
        return Component.literal(value);
    }

    /**
     * Determines whether a string looks like a translation key.
     * <p>
     * A value is considered a translation key if:
     * <ul>
     *   <li>It starts with {@code "dmz."} (default quest keys), or</li>
     *   <li>It starts with {@code "quest."} (standard quest key prefix), or</li>
     *   <li>It starts with {@code "saga."} (standard saga key prefix)</li>
     * </ul>
     *
     * @param value the string to check
     * @return {@code true} if the value appears to be a translation key
     */
    public static boolean isTranslationKey(String value) {
        if (value == null || value.isEmpty()) return false;
        return value.startsWith(DMZ_KEY_PREFIX)
                || value.startsWith("quest.")
                || value.startsWith("saga.");
    }
}

