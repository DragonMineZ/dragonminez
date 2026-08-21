package com.dragonminez.common.util.gson;

import com.dragonminez.common.wish.Wish;
import com.dragonminez.common.wish.wishes.ChangeDifficultyWish;
import com.dragonminez.common.wish.wishes.CommandWish;
import com.dragonminez.common.wish.wishes.ItemListWish;
import com.dragonminez.common.wish.wishes.ItemWish;
import com.dragonminez.common.wish.wishes.MultiItemWish;
import com.dragonminez.common.wish.wishes.PassiveResetWish;
import com.dragonminez.common.wish.wishes.ReCustomizeWish;
import com.dragonminez.common.wish.wishes.RelocateStatsWish;
import com.dragonminez.common.wish.wishes.ResetStoryWish;
import com.dragonminez.common.wish.wishes.SkillWish;
import com.dragonminez.common.wish.wishes.TPSWish;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Polymorphic (de)serialisation for the {@link Wish} hierarchy, keyed on the
 * {@code "type"} discriminator.
 *
 * <p>Delegating through {@link Gson#getDelegateAdapter} keeps a wish's nested item DTOs on
 * the outer Gson, so they resolve to their own subtypes rather than to the base class.
 */
public final class WishTypeAdapterFactory implements TypeAdapterFactory {

	private static final String TYPE_FIELD = "type";

	@Override
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
		if (type.getRawType() != Wish.class) {
			return null;
		}

		final TypeAdapterFactory self = this;
		TypeAdapter<T> adapter = new TypeAdapter<>() {

			@Override
			public void write(JsonWriter out, T value) throws IOException {
				@SuppressWarnings("unchecked")
				TypeAdapter<Object> delegate =
						(TypeAdapter<Object>) gson.getDelegateAdapter(self, TypeToken.get(value.getClass()));
				gson.toJson(delegate.toJsonTree(value), out);
			}

			@Override
			public T read(JsonReader in) throws IOException {
				JsonElement element = JsonParser.parseReader(in);
				if (!element.isJsonObject()) {
					throw new JsonSyntaxException("Expected a wish object but found: " + element);
				}

				JsonObject object = element.getAsJsonObject();
				if (!object.has(TYPE_FIELD) || !object.get(TYPE_FIELD).isJsonPrimitive()) {
					throw new JsonSyntaxException("Wish is missing a 'type' field: " + object);
				}

				String wishType = object.get(TYPE_FIELD).getAsString();
				Class<? extends Wish> target = classForType(wishType);
				if (target == null) {
					throw new JsonSyntaxException("Unknown wish type: '" + wishType + "'");
				}

				@SuppressWarnings("unchecked")
				T result = (T) gson.getDelegateAdapter(self, TypeToken.get(target)).fromJsonTree(object);
				return result;
			}
		};

		return adapter.nullSafe();
	}

	public static Class<? extends Wish> classForType(String type) {
		if (type == null) {
			return null;
		}
		return switch (type) {
			case "item" -> ItemWish.class;
			case "command" -> CommandWish.class;
			case "tps" -> TPSWish.class;
			case "multi_wish" -> MultiItemWish.class;
			case "item_list_wish" -> ItemListWish.class;
			case "skill" -> SkillWish.class;
			case "passivereset" -> PassiveResetWish.class;
			case "recustomize" -> ReCustomizeWish.class;
			case "relocatestats" -> RelocateStatsWish.class;
			case "changedifficulty" -> ChangeDifficultyWish.class;
			case "resetstory" -> ResetStoryWish.class;
			default -> null;
		};
	}
}
