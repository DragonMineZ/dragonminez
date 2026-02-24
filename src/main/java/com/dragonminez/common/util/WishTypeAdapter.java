package com.dragonminez.common.util;

import com.dragonminez.common.wish.*;
import com.dragonminez.common.wish.wishtype.*;
import com.google.gson.*;

import java.lang.reflect.Type;

public class WishTypeAdapter implements JsonSerializer<Wish>, JsonDeserializer<Wish> {

    private static final String TYPE = "type";

    @Override
    public JsonElement serialize(Wish src, Type typeOfSrc, JsonSerializationContext context) {
        return new GsonBuilder().setPrettyPrinting().create().toJsonTree(src, src.getClass());
    }

    @Override
    public Wish deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String type = jsonObject.get(TYPE).getAsString();

        return switch (type) {
            case "item" -> new GsonBuilder().create().fromJson(json, ItemWish.class);
            case "command" -> new GsonBuilder().create().fromJson(json, CommandWish.class);
            case "tps" -> new GsonBuilder().create().fromJson(json, TPSWish.class);
            case "multi_wish" -> new GsonBuilder().create().fromJson(json, MultiItemWish.class);
            case "skill" -> new GsonBuilder().create().fromJson(json, SkillWish.class);
            case "passivereset" -> new GsonBuilder().create().fromJson(json, PassiveResetWish.class);
            case "restoresaiyantail" -> new GsonBuilder().create().fromJson(json, RestoreSaiyanTailWish.class);
            default -> throw new JsonParseException("Unknown wish type: " + type);
        };
    }
}
