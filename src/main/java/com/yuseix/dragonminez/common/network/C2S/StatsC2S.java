package com.yuseix.dragonminez.common.network.C2S;

import com.yuseix.dragonminez.common.config.DMZGeneralConfig;
import com.yuseix.dragonminez.common.events.characters.StatsEvents;
import com.yuseix.dragonminez.common.network.ModMessages;
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
    private int multi;


    public StatsC2S(int id, int cantidad, int multi) {
        this.id = id;
        this.cantidad = cantidad;
        this.multi = multi;
    }

    public StatsC2S(FriendlyByteBuf buf) {
        id = buf.readInt();
        cantidad = buf.readInt();
        multi = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeInt(cantidad);
        buf.writeInt(multi);
    }

    public static void handle(StatsC2S packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {

            ServerPlayer player = ctx.get().getSender();

            DMZDatos datos = new DMZDatos();

            if (player != null) {

                DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {
                    int maxStats = DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get();
                    int incrementoStats = packet.cantidad;
                    var tps = playerstats.getIntValue("tps"); var str = playerstats.getStat("STR"); var def = playerstats.getStat("DEF");
                    var con = playerstats.getStat("CON"); var kipower = playerstats.getStat("PWR"); var energy = playerstats.getStat("ENE");
                    int nivel = (str + def + con + kipower + energy) / 5;

                    int baseCost = (int) Math.round((nivel * DMZClientConfig.getMultiplierZPoints())
                            * DMZClientConfig.getMultiplierZPoints() * 1.5);


                    int actualTps = playerstats.getIntValue("tps");
                    int actualStr = playerstats.getStat("STR"); int actualDef = playerstats.getStat("DEF"); int actualCon = playerstats.getStat("CON");
                    int actualPwr = playerstats.getStat("PWR"); int actualEne = playerstats.getStat("ENE");

                    int actualUpgrade = 0;
                    int actualCost = 0;


                    switch (packet.id) {
                        case 0:
                            actualUpgrade = StatsEvents.calcularNivelesAumentar(nivel, packet.multi, actualStr, actualTps, baseCost, maxStats);
                            actualCost = StatsEvents.calcularCostoRecursivo(nivel, actualUpgrade, baseCost, maxStats);

                            incrementoStats = Math.min(actualUpgrade, maxStats - playerstats.getStat("STR"));
                            if (actualTps >= actualCost && actualStr < maxStats) {
                                playerstats.addStat("STR", incrementoStats);
                                playerstats.removeIntValue("tps", actualCost);
                            }
                            break;
                        case 1:
                            actualUpgrade = StatsEvents.calcularNivelesAumentar(nivel, packet.multi, actualDef, actualTps, baseCost, maxStats);
                            actualCost = StatsEvents.calcularCostoRecursivo(nivel, actualUpgrade, baseCost, maxStats);

                            incrementoStats = Math.min(actualUpgrade, maxStats - actualDef);
                            if (actualTps >= actualCost && actualDef < maxStats) {
                                playerstats.addStat("DEF", incrementoStats);
                                playerstats.removeIntValue("tps", actualCost);
                            }

                            break;
                        case 2:
                            actualUpgrade = StatsEvents.calcularNivelesAumentar(nivel, packet.multi, actualCon, actualTps, baseCost, maxStats);
                            actualCost = StatsEvents.calcularCostoRecursivo(nivel, actualUpgrade, baseCost, maxStats);

                            incrementoStats = Math.min(actualUpgrade, maxStats - actualCon);
                            if (actualTps >= actualCost && actualCon < maxStats) {
                                playerstats.addStat("CON", incrementoStats);
                                playerstats.removeIntValue("tps", actualCost);
                            }

                            //playerstats.setIntValue("curstam", dmzdatos.calcStamina(playerstats));
                            player.refreshDimensions();
                            break;
                        case 3:
                            actualUpgrade = StatsEvents.calcularNivelesAumentar(nivel, packet.multi, actualPwr, actualTps, baseCost, maxStats);
                            actualCost = StatsEvents.calcularCostoRecursivo(nivel, actualUpgrade, baseCost, maxStats);

                            incrementoStats = Math.min(actualUpgrade, maxStats - actualPwr);
                            if (actualTps >= actualCost && actualPwr < maxStats) {
                                playerstats.addStat("PWR", incrementoStats);
                                playerstats.removeIntValue("tps", actualCost);
                            }
                            break;
                        case 4:
                            actualUpgrade = StatsEvents.calcularNivelesAumentar(nivel, packet.multi, actualEne, actualTps, baseCost, maxStats);
                            actualCost = StatsEvents.calcularCostoRecursivo(nivel, actualUpgrade, baseCost, maxStats);

                            incrementoStats = Math.min(actualUpgrade, maxStats - actualEne);
                            if (actualTps >= actualCost && actualEne < maxStats) {
                                playerstats.addStat("ENE", incrementoStats);
                                playerstats.removeIntValue("tps", actualCost);
                            }

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

