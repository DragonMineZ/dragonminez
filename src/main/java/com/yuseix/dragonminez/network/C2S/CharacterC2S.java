package com.yuseix.dragonminez.network.C2S;

import com.yuseix.dragonminez.config.DMCAttrConfig;
import com.yuseix.dragonminez.events.ModEvents;
import com.yuseix.dragonminez.stats.PlayerStatsAttrProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CharacterC2S {

    private String tipo;
    private int cantidad;


    public CharacterC2S(String tipo, int cantidad) {
        this.tipo = tipo;
        this.cantidad = cantidad;
    }

    public CharacterC2S(FriendlyByteBuf buf) {
        tipo = buf.readUtf();
        cantidad = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(tipo);
        buf.writeInt(cantidad);

    }

    public static void handle(CharacterC2S packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {

            ServerPlayer player = ctx.get().getSender();

            if (player != null) {
                PlayerStatsAttrProvider.getCap(ModEvents.INSTANCE, player).ifPresent(playerstats -> {

                    switch (packet.tipo) {
                        case "BodyType":
                            playerstats.setBodytype(packet.cantidad);
                            break;
                        default:
                            System.out.println("Algo salio mal !");
                            break;
                    }

                });
            }

        });
        context.setPacketHandled(true);
    }
}
