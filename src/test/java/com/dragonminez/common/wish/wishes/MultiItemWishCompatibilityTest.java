package com.dragonminez.common.wish.wishes;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MultiItemWishCompatibilityTest {
    @Test
    void acceptsLegacyTupleKeysUsedByCustomWishFiles() {
        String json = "{\"type\":\"multi_wish\",\"name\":\"n\",\"description\":\"d\",\"items\":["
                + "{\"a\":\"minecraft:iron_ingot\",\"b\":64},"
                + "{\"itemId\":\"minecraft:gold_ingot\",\"count\":2}]}";

        MultiItemWish wish = MultiItemWish.fromJson(JsonParser.parseString(json).getAsJsonObject());

        assertEquals(2, JsonParser.parseString(wish.toJson()).getAsJsonObject().getAsJsonArray("items").size());
    }

    @Test
    void rejectsInvalidItemIdsDuringParsing() {
        String json = "{\"type\":\"multi_wish\",\"name\":\"n\",\"description\":\"d\",\"items\":["
                + "{\"itemId\":\"\",\"count\":1}]}";

        assertThrows(com.google.gson.JsonParseException.class,
                () -> MultiItemWish.fromJson(JsonParser.parseString(json).getAsJsonObject()));
    }
}
