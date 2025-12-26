package com.dragonminez.common.util;

import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.objectives.*;
import com.google.gson.*;

import java.lang.reflect.Type;

public class QuestObjectiveTypeAdapter implements JsonSerializer<QuestObjective>, JsonDeserializer<QuestObjective> {

    @Override
    public QuestObjective deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String type = jsonObject.get("type").getAsString();
        
        switch (type.toUpperCase()) {
            case "ITEM":
                return context.deserialize(jsonObject, ItemObjective.class);
            case "KILL":
                return context.deserialize(jsonObject, KillObjective.class);
            case "INTERACT":
                return context.deserialize(jsonObject, InteractObjective.class);
            case "STRUCTURE":
                return context.deserialize(jsonObject, StructureObjective.class);
            case "BIOME":
                return context.deserialize(jsonObject, BiomeObjective.class);
            case "COORDS":
                return context.deserialize(jsonObject, CoordsObjective.class);
            default:
                throw new JsonParseException("Unknown objective type: " + type);
        }
    }

    @Override
    public JsonElement serialize(QuestObjective src, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(src, src.getClass());
    }
}
