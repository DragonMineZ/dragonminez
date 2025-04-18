package com.yuseix.dragonminez.common.network.C2S;

import com.yuseix.dragonminez.common.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.common.stats.DMZStatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.function.Supplier;

public class SkillActivateC2S {

    private String tipo;
    private String id;
    private int value;
    private int cost;


    public SkillActivateC2S(String tipo, String id, int value, int cost) {
        this.tipo = tipo;
        this.id = id;
        this.value = value;
        this.cost = cost;
    }

    public SkillActivateC2S(FriendlyByteBuf buf) {
        tipo = buf.readUtf();
        id = buf.readUtf();
        value = buf.readInt();
        cost = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(tipo);
        buf.writeUtf(id);
        buf.writeInt(value);
        buf.writeInt(cost);
    }

    public static void handle(SkillActivateC2S packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {

            ServerPlayer player = ctx.get().getSender();

            if (player != null) {
                DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {

                    switch (packet.tipo.toLowerCase(Locale.ROOT)){
                        case "active":
                            if(packet.value == 0){
                                playerstats.setSkillActive(packet.id, false);
                            } else {
                                playerstats.setSkillActive(packet.id, true);
                            }
                            break;
                        case "remove":
                            playerstats.removeSkill(packet.id);
                            break;
                        case "setlevel":
                            playerstats.setSkillLvl(packet.id, packet.value);
                            playerstats.removeIntValue("tps", packet.cost);
                        default:
                            break;
                    }

                });
            }

        });
        context.setPacketHandled(true);
    }
}
