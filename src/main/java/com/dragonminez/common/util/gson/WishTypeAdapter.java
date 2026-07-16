package com.dragonminez.common.util.gson;

import com.dragonminez.common.wish.Wish;
import com.dragonminez.common.wish.wishes.*;
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

		Class<? extends Wish> target = classForType(type);
		if (target == null) {
			throw new JsonParseException("Unknown wish type: " + type);
		}
		return new GsonBuilder().create().fromJson(json, target);
	}

	public static Class<? extends Wish> classForType(String type) {
		if (type == null) return null;
		return switch (type) {
			case "item" -> ItemWish.class;
			case "command" -> CommandWish.class;
			case "tps" -> TPSWish.class;
			case "multi_wish" -> MultiItemWish.class;
			case "skill" -> SkillWish.class;
			case "passivereset" -> PassiveResetWish.class;
			case "recustomize" -> ReCustomizeWish.class;
			case "relocatestats" -> RelocateStatsWish.class;
			case "changedifficulty" -> ChangeDifficultyWish.class;
			case "resetstory" -> ResetStoryWish.class;
			case "item_list_wish"  -> ItemListWish.class;
			default -> null;
		};
	}
}
