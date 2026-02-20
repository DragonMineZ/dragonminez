package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.CompressionUtil;
import com.dragonminez.common.util.WishTypeAdapter;
import com.dragonminez.common.wish.Wish;
import com.dragonminez.common.wish.WishManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SyncWishesS2C {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Wish.class, new WishTypeAdapter())
            .create();

    private final byte[] compressedWishes;

    public SyncWishesS2C(Map<String, List<Wish>> wishes) {
        this.compressedWishes = CompressionUtil.compress(GSON.toJson(wishes));
    }

    public SyncWishesS2C(FriendlyByteBuf buf) {
        this.compressedWishes = buf.readByteArray();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByteArray(compressedWishes);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                String decompressedJson = CompressionUtil.decompress(compressedWishes);
                Type mapType = new TypeToken<Map<String, List<Wish>>>(){}.getType();
                Map<String, List<Wish>> wishes = GSON.fromJson(decompressedJson, mapType);
                if (wishes != null) WishManager.applySyncedWishes(wishes);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}