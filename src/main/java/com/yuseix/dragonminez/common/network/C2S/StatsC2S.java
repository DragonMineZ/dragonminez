package com.yuseix.dragonminez.common.network.C2S;

import com.yuseix.dragonminez.common.config.DMZGeneralConfig;
import com.yuseix.dragonminez.common.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.common.stats.DMZStatsProvider;
import com.yuseix.dragonminez.client.config.DMZClientConfig;
import com.yuseix.dragonminez.common.util.DMZDatos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StatsC2S {

    private int id;
    private int cantidad;


    public StatsC2S(int id, int cantidad) {
        this.id = id;
        this.cantidad = cantidad;
    }

    public StatsC2S(FriendlyByteBuf buf) {
        id = buf.readInt();
        cantidad = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeInt(cantidad);

    }

    public static void handle(StatsC2S packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {

            DMZDatos dmzdatos = new DMZDatos();

            ServerPlayer player = ctx.get().getSender();

            if (player != null) {

                DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {
                    int maxStats = DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get();
                    int incrementoStats = packet.cantidad;

                    switch (packet.id) {
                        case 0:
                            incrementoStats = Math.min(packet.cantidad, maxStats - playerstats.getStat("STR"));
                            playerstats.addStat("STR", incrementoStats);
                            break;
                        case 1:
                            incrementoStats = Math.min(packet.cantidad, maxStats - playerstats.getStat("DEF"));
                            playerstats.addStat("DEF", incrementoStats);
                            break;
                        case 2:
                            incrementoStats = Math.min(packet.cantidad, maxStats - playerstats.getStat("CON"));
                            playerstats.addStat("CON", incrementoStats);

                            //playerstats.setIntValue("curstam", dmzdatos.calcStamina(playerstats));
                            player.refreshDimensions();
                            break;
                        case 3:
                            incrementoStats = Math.min(packet.cantidad, maxStats - playerstats.getStat("PWR"));
                            playerstats.addStat("PWR", incrementoStats);
                            break;
                        case 4:
                            incrementoStats = Math.min(packet.cantidad, maxStats - playerstats.getStat("ENE"));
                            playerstats.addStat("ENE", incrementoStats);
                            break;
                        default:
                            //System.out.println("Algo salio mal !");
                            break;
                    }

                });
            }

        });
        context.setPacketHandled(true);
    }
}

