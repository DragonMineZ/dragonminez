package com.dragonminez.common.quest;

import com.dragonminez.common.quest.objectives.*;
import com.dragonminez.common.quest.rewards.CommandReward;
import com.dragonminez.common.quest.rewards.ItemReward;
import com.dragonminez.common.quest.rewards.TPSReward;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class QuestParser {

    public static Quest parseQuest(JsonObject json) {
        int id = json.get("id").getAsInt();
        String title = json.get("title").getAsString();
        String description = json.get("description").getAsString();

        List<QuestObjective> objectives = new ArrayList<>();
        if (json.has("objectives")) {
            JsonArray objArray = json.getAsJsonArray("objectives");
            for (JsonElement element : objArray) {
                QuestObjective obj = parseObjective(element.getAsJsonObject());
                if (obj != null) {
                    objectives.add(obj);
                }
            }
        }

        List<QuestReward> rewards = new ArrayList<>();
        if (json.has("rewards")) {
            JsonArray rewardArray = json.getAsJsonArray("rewards");
            for (JsonElement element : rewardArray) {
                QuestReward reward = parseReward(element.getAsJsonObject());
                if (reward != null) {
                    rewards.add(reward);
                }
            }
        }

        return new Quest(id, title, description, objectives, rewards);
    }

    private static QuestObjective parseObjective(JsonObject json) {
        String type = json.get("type").getAsString();
        String description = json.get("description").getAsString();

        switch (type.toUpperCase()) {
            case "ITEM":
                String itemId = json.get("item").getAsString();
                int count = json.get("count").getAsInt();
                Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(itemId));
                if (item != Items.AIR) {
                    return new ItemObjective(description, item, count);
                }
                break;

            case "KILL":
                String entityId = json.get("entity").getAsString();
                int killCount = json.get("count").getAsInt();
                EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(entityId));
                return new KillObjective(description, entityType, killCount);

            case "BIOME":
                String biomeId = json.get("biome").getAsString();
                return new BiomeObjective(description, biomeId);

            case "COORDS":
                int x = json.get("x").getAsInt();
                int y = json.get("y").getAsInt();
                int z = json.get("z").getAsInt();
                int radius = json.has("radius") ? json.get("radius").getAsInt() : 10;
                return new CoordsObjective(description, new BlockPos(x, y, z), radius);

            case "INTERACT":
                String interactEntity = json.has("entity") ? json.get("entity").getAsString() : null;
                String entityName = json.has("entityName") ? json.get("entityName").getAsString() : null;
                EntityType<?> interactType = interactEntity != null ?
                    BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(interactEntity)) : null;
                return new InteractObjective(description, interactType, entityName);

            case "STRUCTURE":
                String structureId = json.get("structure").getAsString();
                return new StructureObjective(description, structureId);
        }

        return null;
    }

    private static QuestReward parseReward(JsonObject json) {
        String type = json.get("type").getAsString();

        QuestReward.DifficultyType difficultyType = QuestReward.DifficultyType.ALL;
        if (type.toLowerCase().startsWith("hard:")) {
            difficultyType = QuestReward.DifficultyType.HARD;
            type = type.substring(5);
        } else if (type.toLowerCase().startsWith("normal:")) {
            difficultyType = QuestReward.DifficultyType.NORMAL;
            type = type.substring(7);
        }

        QuestReward reward = null;
        switch (type.toUpperCase()) {
            case "ITEM":
                String itemId = json.get("item").getAsString();
                int count = json.has("count") ? json.get("count").getAsInt() : 1;
                Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(itemId));
                if (item != Items.AIR) {
                    reward = new ItemReward(new ItemStack(item, count));
                }
                break;

            case "TPS":
                int amount = json.get("amount").getAsInt();
                reward = new TPSReward(amount);
                break;

            case "COMMAND":
                String command = json.get("command").getAsString();
                reward = new CommandReward(command);
                break;
        }

        if (reward != null) {
            reward.setDifficultyType(difficultyType);
        }

        return reward;
    }
}

