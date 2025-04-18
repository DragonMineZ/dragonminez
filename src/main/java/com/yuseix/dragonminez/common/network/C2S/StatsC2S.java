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
    private int cost;


    public StatsC2S(int id, int cantidad, int cost) {
        this.id = id;
        this.cantidad = cantidad;
        this.cost = cost;
    }

    public StatsC2S(FriendlyByteBuf buf) {
        id = buf.readInt();
        cantidad = buf.readInt();
        cost = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeInt(cantidad);
        buf.writeInt(cost);
    }

    public static void handle(StatsC2S packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {

            ServerPlayer player = ctx.get().getSender();

            if (player != null) {

                DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {
                    int maxStats = DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get();
                    int incrementoStats = packet.cantidad;

                    switch (packet.id) {
                        case 0:
                            incrementoStats = Math.min(packet.cantidad, maxStats - playerstats.getStat("STR"));
                            playerstats.addStat("STR", incrementoStats);
                            playerstats.removeIntValue("tps", packet.cost);
                            break;
                        case 1:
                            incrementoStats = Math.min(packet.cantidad, maxStats - playerstats.getStat("DEF"));
                            playerstats.addStat("DEF", incrementoStats);
                            playerstats.removeIntValue("tps", packet.cost);
                            break;
                        case 2:
                            incrementoStats = Math.min(packet.cantidad, maxStats - playerstats.getStat("CON"));
                            playerstats.addStat("CON", incrementoStats);
                            playerstats.removeIntValue("tps", packet.cost);

                            //playerstats.setIntValue("curstam", dmzdatos.calcStamina(playerstats));
                            player.refreshDimensions();
                            break;
                        case 3:
                            incrementoStats = Math.min(packet.cantidad, maxStats - playerstats.getStat("PWR"));
                            playerstats.addStat("PWR", incrementoStats);
                            playerstats.removeIntValue("tps", packet.cost);

                            break;
                        case 4:
                            incrementoStats = Math.min(packet.cantidad, maxStats - playerstats.getStat("ENE"));
                            playerstats.addStat("ENE", incrementoStats);
                            playerstats.removeIntValue("tps", packet.cost);
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

