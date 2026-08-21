package com.dragonminez.common.util.gson;

import com.dragonminez.common.util.types.items.PotionEffectDTO;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a potion's {@code mobEffects} in either supported shape, and always writes the list
 * form.
 *
 * <p>List form, which states duration and amplifier separately:
 * <pre>"mobEffects": [{"effect": "minecraft:speed", "duration": 600, "amplifier": 1}]</pre>
 *
 * <p>Legacy map form, kept so older files still load:
 * <pre>"mobEffects": {"minecraft:speed": 600}</pre>
 * The number is a duration — it was passed to {@code MobEffectInstance(MobEffect, int)} —
 * so it is read back as one, with a zero amplifier.
 */
public final class PotionEffectListTypeAdapter implements TypeAdapterFactory {

	@Override
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
		TypeAdapter<PotionEffectDTO> elementAdapter = gson.getAdapter(PotionEffectDTO.class);

		TypeAdapter<List<PotionEffectDTO>> adapter = new TypeAdapter<>() {

			@Override
			public void write(JsonWriter out, List<PotionEffectDTO> value) throws IOException {
				out.beginArray();
				for (PotionEffectDTO effect : value) {
					elementAdapter.write(out, effect);
				}
				out.endArray();
			}

			@Override
			public List<PotionEffectDTO> read(JsonReader in) throws IOException {
				List<PotionEffectDTO> effects = new ArrayList<>();

				if (in.peek() == JsonToken.BEGIN_OBJECT) {
					in.beginObject();
					while (in.hasNext()) {
						effects.add(PotionEffectDTO.fromLegacyEntry(in.nextName(), in.nextInt()));
					}
					in.endObject();
					return effects;
				}

				in.beginArray();
				while (in.hasNext()) {
					effects.add(elementAdapter.read(in));
				}
				in.endArray();
				return effects;
			}
		};

		@SuppressWarnings("unchecked")
		TypeAdapter<T> result = (TypeAdapter<T>) adapter.nullSafe();
		return result;
	}
}
