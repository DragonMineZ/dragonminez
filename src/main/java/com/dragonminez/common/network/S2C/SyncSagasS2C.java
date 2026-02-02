package com.dragonminez.common.network.S2C;

import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.quest.Saga;
import com.dragonminez.common.quest.SagaManager;
import com.dragonminez.common.util.QuestObjectiveTypeAdapter;
import com.dragonminez.common.util.QuestRewardTypeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncSagasS2C {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(QuestObjective.class, new QuestObjectiveTypeAdapter())
            .registerTypeAdapter(QuestReward.class, new QuestRewardTypeAdapter())
            .create();

    private final Map<String, Saga> sagas;

    public SyncSagasS2C(Map<String, Saga> sagas) {
        this.sagas = sagas;
    }

    public SyncSagasS2C(FriendlyByteBuf buf) {
        this.sagas = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            String sagaJson = buf.readUtf(32767);
            Saga saga = GSON.fromJson(sagaJson, Saga.class);
            if (saga != null && saga.getId() != null) {
                this.sagas.put(saga.getId(), saga);
            }
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(sagas.size());
        sagas.values().forEach(saga -> {
            buf.writeUtf(GSON.toJson(saga));
        });
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SagaManager.applySyncedSagas(sagas));
        });
        ctx.get().setPacketHandled(true);
    }
}
