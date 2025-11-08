package com.dragonminez.common.network.C2S;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CreateCharacterC2S {

    private final int raceId;
    private final String className;
    private final String gender;

    public CreateCharacterC2S(int raceId, String className, String gender) {
        this.raceId = raceId;
        this.className = className;
        this.gender = gender;
    }

    public static void encode(CreateCharacterC2S msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.raceId);
        buf.writeUtf(msg.className);
        buf.writeUtf(msg.gender);
    }

    public static CreateCharacterC2S decode(FriendlyByteBuf buf) {
        return new CreateCharacterC2S(
                buf.readInt(),
                buf.readUtf(),
                buf.readUtf()
        );
    }

    public static void handle(CreateCharacterC2S msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                if (!data.getStatus().hasCreatedCharacter()) {
                    data.initializeWithRaceAndClass(msg.raceId, msg.className, msg.gender);

                    LogUtil.info(Env.COMMON, "Jugador {} creó personaje: Raza={}, Clase={}, Género={}",
                            player.getName().getString(), msg.raceId, msg.className, msg.gender);

                    NetworkHandler.sendToPlayer(new com.dragonminez.common.network.S2C.StatsSyncS2C(player), player);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}


