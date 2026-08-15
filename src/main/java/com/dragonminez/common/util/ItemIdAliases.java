package com.dragonminez.common.util;

/**
 * Stable aliases for item IDs that were written to persistent wish files by older releases.
 */
public final class ItemIdAliases {
    public static final String LEGACY_SENZU_ID = "dragonminez:senzu";
    public static final String SENZU_BEAN_ID = "dragonminez:senzu_bean";

    private ItemIdAliases() {
    }

    public static String normalize(String itemId) {
        return LEGACY_SENZU_ID.equals(itemId) ? SENZU_BEAN_ID : itemId;
    }
}
