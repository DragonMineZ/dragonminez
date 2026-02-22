package com.dragonminez.common.util;

import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.quest.rewards.CommandReward;
import com.dragonminez.common.quest.rewards.ItemReward;
import com.dragonminez.common.quest.rewards.SkillReward;
import com.dragonminez.common.quest.rewards.TPSReward;
import com.google.gson.*;

import java.lang.reflect.Type;

public class QuestRewardTypeAdapter implements JsonSerializer<QuestReward>, JsonDeserializer<QuestReward> {

    @Override
    public QuestReward deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String type = jsonObject.get("type").getAsString();

        return switch (type.toUpperCase()) {
            case "ITEM" -> context.deserialize(jsonObject, ItemReward.class);
            case "COMMAND" -> context.deserialize(jsonObject, CommandReward.class);
            case "TPS" -> context.deserialize(jsonObject, TPSReward.class);
            case "SKILL" -> context.deserialize(jsonObject, SkillReward.class);
            default -> throw new JsonParseException("Unknown reward type: " + type);
        };
    }

    @Override
    public JsonElement serialize(QuestReward src, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(src, src.getClass());
    }
}
