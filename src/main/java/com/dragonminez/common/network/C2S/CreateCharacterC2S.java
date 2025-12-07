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

    private final String raceName;
    private final String className;
    private final String gender;

    public CreateCharacterC2S(String raceName, String className, String gender) {
        this.raceName = raceName;
        this.className = className;
        this.gender = gender;
    }

    public static void encode(CreateCharacterC2S msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.raceName);
        buf.writeUtf(msg.className);
        buf.writeUtf(msg.gender);
    }

    public static CreateCharacterC2S decode(FriendlyByteBuf buf) {
        return new CreateCharacterC2S(
                buf.readUtf(),
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
                    data.initializeWithRaceAndClass(msg.raceName, msg.className, msg.gender);

                    LogUtil.info(Env.COMMON, "Jugador {} creó personaje: Raza={}, Clase={}, Género={}",
                            player.getName().getString(), msg.raceName, msg.className, msg.gender);

                    NetworkHandler.sendToPlayer(new com.dragonminez.common.network.S2C.StatsSyncS2C(player), player);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}


