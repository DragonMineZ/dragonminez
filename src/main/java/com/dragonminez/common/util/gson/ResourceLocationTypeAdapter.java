package com.dragonminez.common.util.gson;

import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;

/**
 * Reads and writes {@link ResourceLocation} as a plain {@code "namespace:path"} string.
 * Without it Gson reflects over the two private fields and emits
 * {@code {"namespace": "...", "path": "..."}}.
 *
 * <p>Also covers map <em>keys</em>, such as the enchantment map: Gson writes keys via
 * {@code toString()} and promotes the key name back to a string on read, so
 * {@code {"minecraft:sharpness": 3}} round-trips as-is.
 */
public final class ResourceLocationTypeAdapter extends TypeAdapter<ResourceLocation> {

	@Override
	public void write(JsonWriter out, ResourceLocation value) throws IOException {
		if (value == null) {
			out.nullValue();
			return;
		}
		out.value(value.toString());
	}

	@Override
	public ResourceLocation read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}

		String id = in.nextString();
		ResourceLocation location = ResourceLocation.tryParse(id);
		if (location == null) {
			throw new JsonSyntaxException("Invalid resource location: '" + id + "'");
		}
		return location;
	}
}
