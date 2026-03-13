package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.CompressionUtil;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.quest.Saga;
import com.dragonminez.common.quest.SagaManager;
import com.dragonminez.common.util.QuestObjectiveTypeAdapter;
import com.dragonminez.common.util.QuestRewardTypeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Supplier;

public class SyncSagasS2C {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(QuestObjective.class, new QuestObjectiveTypeAdapter())
            .registerTypeAdapter(QuestReward.class, new QuestRewardTypeAdapter())
            .create();

    private final byte[] compressedSagas;

    public SyncSagasS2C(Map<String, Saga> sagas) {
        this.compressedSagas = CompressionUtil.compress(GSON.toJson(sagas));
    }

    public SyncSagasS2C(FriendlyByteBuf buf) {
        this.compressedSagas = buf.readByteArray();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByteArray(compressedSagas);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                String decompressedJson = CompressionUtil.decompress(compressedSagas);
                Type mapType = new TypeToken<Map<String, Saga>>(){}.getType();
                Map<String, Saga> sagas = GSON.fromJson(decompressedJson, mapType);
                if (sagas != null) SagaManager.applySyncedSagas(sagas);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}