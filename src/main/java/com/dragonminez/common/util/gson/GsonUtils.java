package com.dragonminez.common.util.gson;

import com.dragonminez.common.util.types.items.GenericItemDTO;
import com.dragonminez.common.wish.Wish;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;

public final class GsonUtils {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ResourceLocation.class, new ResourceLocationTypeAdapter())
            .registerTypeAdapter(Wish.class, new WishTypeAdapter())
            .registerTypeAdapter(GenericItemDTO.class, new GenericItemTypeAdapter())
            .setPrettyPrinting()
            .create();

    private GsonUtils() {}
}