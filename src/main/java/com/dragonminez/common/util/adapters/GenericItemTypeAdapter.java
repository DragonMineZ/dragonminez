package com.dragonminez.common.util.adapters;

import com.dragonminez.common.util.types.items.*;
import com.google.gson.*;

import java.lang.reflect.Type;

public class GenericItemTypeAdapter implements JsonSerializer<GenericItemDTO>, JsonDeserializer<GenericItemDTO> {
	private static final String TYPE = "itemType";

	@Override
	public JsonElement serialize(GenericItemDTO src, Type typeOfSrc, JsonSerializationContext context) {
		return new GsonBuilder().setPrettyPrinting().create().toJsonTree(src, src.getClass());
	}

	@Override
	public GenericItemDTO deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonObject jsonObject = json.getAsJsonObject();
		String type = jsonObject.get(TYPE).getAsString();

		Class<? extends GenericItemDTO> target = classForType(type);
		if (target == null) {
			throw new JsonParseException("Unknown item type: " + type);
		}
		return new GsonBuilder().create().fromJson(json, target);
	}

	public static Class<? extends GenericItemDTO> classForType(String type) {
		if (type == null) return null;
		return switch (type) {
			case "enchanted_book" -> EnchantedBookDTO.class;
			case "enchanted_item" -> EnchantedItemDTO.class;
			case "generic_item" -> GenericItemDTO.class;
			case "lingering_potion" -> LingeringPotionDTO.class;
			case "potion" -> PotionDTO.class;
			case "splash_potion" -> SplashPotionDTO.class;
			case "tipped_arrow" -> TippedArrowDTO.class;
			case "trimmed_armor" -> TrimmedArmorDTO.class;
			default -> null;
		};
	}
}
