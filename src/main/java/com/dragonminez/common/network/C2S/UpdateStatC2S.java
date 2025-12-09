package com.dragonminez.common.network.C2S;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateStatC2S {

    private final String statusKey;
    private final boolean value;

    public UpdateStatC2S(String statusKey, boolean value) {
        this.statusKey = statusKey;
        this.value = value;
    }

    public static void encode(UpdateStatC2S msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.statusKey);
        buf.writeBoolean(msg.value);
    }

    public static UpdateStatC2S decode(FriendlyByteBuf buf) {
        return new UpdateStatC2S(
                buf.readUtf(),
                buf.readBoolean()
        );
    }

    public static void handle(UpdateStatC2S msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                switch (msg.statusKey) {
                    case "isChargingKi":
                        data.getStatus().setChargingKi(msg.value);
                        break;
                    case "isDescending":
                        data.getStatus().setDescending(msg.value);
                        break;
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}

