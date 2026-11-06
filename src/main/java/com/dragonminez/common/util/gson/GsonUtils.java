package com.dragonminez.common.util.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;

/**
 * The shared Gson configurations for wishes, quest rewards and item DTOs.
 *
 * <p>Two instances, differing only in formatting:
 * <ul>
 *   <li>{@link #GSON} pretty-prints, for files on disk and datagen output.</li>
 *   <li>{@link #NETWORK} is compact, for sync packets, which are size-bounded by
 *       {@code FriendlyByteBuf.writeUtf}.</li>
 * </ul>
 */
public final class GsonUtils {

	public static final Gson GSON = builder().setPrettyPrinting().create();

	public static final Gson NETWORK = builder().create();

	private static GsonBuilder builder() {
		return new GsonBuilder()
				.registerTypeAdapter(ResourceLocation.class, new ResourceLocationTypeAdapter())
				.registerTypeAdapterFactory(new WishTypeAdapterFactory())
				.registerTypeAdapterFactory(new GenericItemTypeAdapterFactory());
	}

	private GsonUtils() {
	}
}
