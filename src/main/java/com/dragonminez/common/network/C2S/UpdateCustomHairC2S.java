package com.dragonminez.common.network.C2S;

import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateCustomHairC2S {
    private final CustomHair customHair;

    public UpdateCustomHairC2S(CustomHair customHair) {
        this.customHair = customHair;
    }

    public static void encode(UpdateCustomHairC2S msg, FriendlyByteBuf buf) {
        boolean hasHair = msg.customHair != null;
        buf.writeBoolean(hasHair);
        if (hasHair) {
            msg.customHair.writeToBuffer(buf);
        }
    }

    public static UpdateCustomHairC2S decode(FriendlyByteBuf buf) {
        boolean hasHair = buf.readBoolean();
        CustomHair hair = null;
        if (hasHair) {
            hair = CustomHair.readFromBuffer(buf);
        }
        return new UpdateCustomHairC2S(hair);
    }

    public static void handle(UpdateCustomHairC2S msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                if (msg.customHair != null) {
                    data.getCharacter().setCustomHair(msg.customHair);
                    data.getCharacter().setHairId(0);

                    NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
