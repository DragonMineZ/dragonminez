package com.dragonminez.common.util.gson;

import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.objectives.*;
import com.google.gson.*;

import java.lang.reflect.Type;

public class QuestObjectiveTypeAdapter implements JsonSerializer<QuestObjective>, JsonDeserializer<QuestObjective> {

    @Override
    public QuestObjective deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String type = jsonObject.get("type").getAsString();

        return switch (type.toUpperCase()) {
            case "ITEM" -> context.deserialize(jsonObject, ItemObjective.class);
            case "KILL" -> context.deserialize(jsonObject, KillObjective.class);
            case "INTERACT" -> context.deserialize(jsonObject, InteractObjective.class);
            case "STRUCTURE" -> context.deserialize(jsonObject, StructureObjective.class);
            case "BIOME" -> context.deserialize(jsonObject, BiomeObjective.class);
            case "COORDS" -> context.deserialize(jsonObject, CoordsObjective.class);
            case "TALK_TO" -> context.deserialize(jsonObject, TalkToObjective.class);
            case "SKILL" -> context.deserialize(jsonObject, SkillObjective.class);
            default -> throw new JsonParseException("Unknown objective type: " + type);
        };
    }

    @Override
    public JsonElement serialize(QuestObjective src, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(src, src.getClass());
    }
}
