package com.dragonminez.common.network.C2S;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateStatC2S {

	public enum StatAction {
		CHARGE_KI, DESCEND, ACTION_CHARGE, BLOCK
	}

	private final StatAction statusKey;
    private final boolean value;

    public UpdateStatC2S(StatAction statusKey, boolean value) {
        this.statusKey = statusKey;
        this.value = value;
    }

    public static void encode(UpdateStatC2S msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.statusKey);
        buf.writeBoolean(msg.value);
    }

    public static UpdateStatC2S decode(FriendlyByteBuf buf) {
        return new UpdateStatC2S(
                buf.readEnum(StatAction.class),
                buf.readBoolean()
        );
    }

    public static void handle(UpdateStatC2S msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                switch (msg.statusKey) {
					case CHARGE_KI:
                        if (data.getStatus().isChargingKi() != msg.value) data.getStatus().setChargingKi(msg.value);
                        break;
					case DESCEND:
						if (data.getStatus().isDescending() != msg.value) data.getStatus().setDescending(msg.value);
                        break;
					case ACTION_CHARGE:
						if (data.getStatus().isActionCharging() != msg.value) data.getStatus().setActionCharging(msg.value);
						break;
					case BLOCK:
						if (data.getStatus().isBlocking() != msg.value) data.getStatus().setBlocking(msg.value);
						if (msg.value) data.getStatus().setLastBlockTime(System.currentTimeMillis());
						break;
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}

