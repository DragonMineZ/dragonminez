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
                        if (data.getStatus().isChargingKi() != msg.value) data.getStatus().setChargingKi(msg.value);
                        break;
                    case "isDescending":
						if (data.getStatus().isDescending() != msg.value) data.getStatus().setDescending(msg.value);
                        break;
					case "isActionCharging":
						if (data.getStatus().isActionCharging() != msg.value) data.getStatus().setActionCharging(msg.value);
						break;
					case "isBlocking":
						if (data.getStatus().isBlocking() != msg.value) data.getStatus().setBlocking(msg.value);
						if (msg.value) data.getStatus().setLastBlockTime(System.currentTimeMillis());
						break;
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}

