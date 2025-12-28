package com.dragonminez.common.util;

import com.dragonminez.common.wish.CommandWish;
import com.dragonminez.common.wish.ItemWish;
import com.dragonminez.common.wish.TPSWish;
import com.dragonminez.common.wish.Wish;
import com.google.gson.*;

import java.lang.reflect.Type;

public class WishTypeAdapter implements JsonSerializer<Wish>, JsonDeserializer<Wish> {

    private static final String TYPE = "type";

    @Override
    public JsonElement serialize(Wish src, Type typeOfSrc, JsonSerializationContext context) {
        return src.toJson();
    }

    @Override
    public Wish deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String type = jsonObject.get(TYPE).getAsString();

        String name = jsonObject.get("name").getAsString();
        String description = jsonObject.get("description").getAsString();

        switch (type) {
            case "item":
                String itemId = jsonObject.get("item_id").getAsString();
                int count = jsonObject.get("count").getAsInt();
                return new ItemWish(name, description, itemId, count);
            case "command":
                JsonArray commandsArray = jsonObject.getAsJsonArray("commands");
                String[] commands = new String[commandsArray.size()];
                for (int i = 0; i < commandsArray.size(); i++) {
                    commands[i] = commandsArray.get(i).getAsString();
                }
                return new CommandWish(name, description, commands);
            case "tps":
                int amount = jsonObject.get("amount").getAsInt();
                return new TPSWish(name, description, amount);
            default:
                throw new JsonParseException("Unknown wish type: " + type);
        }
    }
}
