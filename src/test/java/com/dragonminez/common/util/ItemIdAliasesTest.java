package com.dragonminez.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemIdAliasesTest {
    @Test
    void migratesTheLegacySenzuItemId() {
        assertEquals(ItemIdAliases.SENZU_BEAN_ID, ItemIdAliases.normalize(ItemIdAliases.LEGACY_SENZU_ID));
    }

    @Test
    void leavesCurrentAndCustomItemIdsUntouched() {
        String currentId = ItemIdAliases.SENZU_BEAN_ID;
        String customId = "example:custom_item";

        assertEquals(currentId, ItemIdAliases.normalize(currentId));
        assertEquals(customId, ItemIdAliases.normalize(customId));
    }
}
