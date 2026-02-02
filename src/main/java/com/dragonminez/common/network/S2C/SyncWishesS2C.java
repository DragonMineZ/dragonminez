package com.dragonminez.common.network.S2C;

import com.dragonminez.common.util.WishTypeAdapter;
import com.dragonminez.common.wish.Wish;
import com.dragonminez.common.wish.WishManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SyncWishesS2C {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Wish.class, new WishTypeAdapter())
            .create();

    private final Map<String, List<Wish>> wishes;

    public SyncWishesS2C(Map<String, List<Wish>> wishes) {
        this.wishes = wishes;
    }

    public SyncWishesS2C(FriendlyByteBuf buf) {
        this.wishes = new HashMap<>();
        int mapSize = buf.readInt();
        for (int i = 0; i < mapSize; i++) {
            String dragonName = buf.readUtf();
            int listSize = buf.readInt();
            List<Wish> wishList = new ArrayList<>();
            for (int j = 0; j < listSize; j++) {
                String wishJson = buf.readUtf(32767);
                Wish wish = GSON.fromJson(wishJson, Wish.class);
                if (wish != null) {
                    wishList.add(wish);
                }
            }
            this.wishes.put(dragonName, wishList);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(wishes.size());
        for (Map.Entry<String, List<Wish>> entry : wishes.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeInt(entry.getValue().size());
            for (Wish wish : entry.getValue()) {
                buf.writeUtf(GSON.toJson(wish, Wish.class));
            }
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> WishManager.applySyncedWishes(wishes));
        });
        ctx.get().setPacketHandled(true);
    }
}
