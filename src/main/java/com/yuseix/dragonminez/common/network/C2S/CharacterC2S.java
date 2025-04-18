package com.yuseix.dragonminez.common.network.C2S;

import com.yuseix.dragonminez.common.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.common.stats.DMZStatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
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
                DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {

                    switch (packet.tipo) {
                        case "Gender":
                            if (packet.cantidad == 0) {
                                playerstats.setStringValue("gender", "male");
                            } else {
                                playerstats.setStringValue("gender", "female");
                            }
                            break;
                        case "dmzClass":
                            if (packet.cantidad == 0) {
                                playerstats.setStringValue("class", "warrior");
                            } else {
                                playerstats.setStringValue("class", "spiritualist");
                            }
                            break;
                        case "dmzskiweapon":
                            if (packet.cantidad == 0) {
                                playerstats.setStringValue("kiweapon", "scythe");
                            } else if(packet.cantidad == 1){
                                playerstats.setStringValue("kiweapon","trident");
                            } else {
                                playerstats.setStringValue("kiweapon","sword");
                            }
                            break;
                        case "dmzAlignment":
                            playerstats.setIntValue("alignment", packet.cantidad);
                            break;
                        case "BodyType":
                            playerstats.setIntValue("bodytype", packet.cantidad);
                            break;
                        case "EyeType":
                            playerstats.setIntValue("eyestype", packet.cantidad);
                            break;
                        case "setRace":
                            playerstats.setIntValue("race", packet.cantidad);
                            playerstats.setIntValue("bodytype", 0);
                            playerstats.setIntValue("eyestype", 0);
                            break;
                        case "BodyColor1":
                            playerstats.setIntValue("bodycolor", packet.cantidad);
                            break;
                        case "BodyColor2":
                            playerstats.setIntValue("bodycolor2", packet.cantidad);
                            break;
                        case "BodyColor3":
                            playerstats.setIntValue("bodycolor3", packet.cantidad);
                            break;
                        case "eye1Color":
                            playerstats.setIntValue("eye1color", packet.cantidad);
                            break;
                        case "eye2Color":
                            playerstats.setIntValue("eye2color", packet.cantidad);
                            break;
                        case "hairColor":
                            playerstats.setIntValue("haircolor", packet.cantidad);
                            break;
                        case "auraColor":
                            playerstats.setIntValue("auracolor", packet.cantidad);
                            break;
                        case "hairID":
                            playerstats.setIntValue("hairid", packet.cantidad);
                            break;
                        case "isConfirm":
							playerstats.setBoolean("dmzuser", packet.cantidad != 0);
                            break;
                        case "isAuraOn":
							playerstats.setBoolean("aura", packet.cantidad != 0);
                            break;
                        case "isTurboOn":
                            playerstats.setBoolean("turbo", packet.cantidad != 0);
                            break;
                        case "isTransform":
                            playerstats.setBoolean("transform", packet.cantidad != 0);
                            break;
                        case "isDescendOn":
                            playerstats.setBoolean("descend", packet.cantidad != 0);
                            break;
                        case "zenkaiCount":
                            playerstats.setIntValue("zenkaicount", packet.cantidad);
                            break;
                        case "zenkaiCooldown":
                            playerstats.setIntValue("zenkaitimer", packet.cantidad * 20 * 60); // Ticks * Segundos * Minutos
                            break;
                        case "isCompactMenu":
                            playerstats.setBoolean("compactmenu", packet.cantidad != 0);
                            break;
                        case "setPorungaRevive":
                            playerstats.setBoolean("porungarevive", packet.cantidad != 0);
                            break;
                        case "setShenronRevive":
                            playerstats.setBoolean("shenronrevive", packet.cantidad != 0);
                            break;
                        case "isKaioPlanet":
                            playerstats.setBoolean("kaioplanet", packet.cantidad != 0);
                            break;
                        case "str":
                            playerstats.setStat("STR", packet.cantidad);
                            break;
                        case "def":
                            playerstats.setStat("DEF", packet.cantidad);
                            break;
                        case "con":
                            playerstats.setStat("CON", packet.cantidad);
                            break;
                        case "pwr":
                            playerstats.setStat("PWR", packet.cantidad);
                            break;
                        case "ene":
                            playerstats.setStat("ENE", packet.cantidad);
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
