package com.yuseix.dragonminez.common.network.C2S;

import com.yuseix.dragonminez.common.stats.DMZStatsAttributes;
import com.yuseix.dragonminez.common.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.common.stats.DMZStatsProvider;
import com.yuseix.dragonminez.common.util.DMZDatos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DendeC2S {

    private int option;

    public DendeC2S(int option) {
        this.option = option;
    }
    public DendeC2S(FriendlyByteBuf buf) {
        option = buf.readInt();

    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(option);

    }
    public static void handle(DendeC2S packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {

            ServerPlayer player = ctx.get().getSender();

            if (player != null) {
                DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {
                    if(packet.option == 1){
                        resetPlayerStats(player, playerstats);
                    } else if(packet.option == 2) {
                        healPlayer(player, playerstats);
                    }
                });
            }
        });
        context.setPacketHandled(true);
    }

    public static void resetPlayerStats(ServerPlayer player, DMZStatsAttributes playerstats) {
        player.displayClientMessage(Component.translatable("lines.dende.reset.success"), true);

        player.setHealth(20);
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);
        player.setHealth(20);


        playerstats.setBoolean("dmzuser", false);
        //Luego cambiar cuando decidamos las stats
        playerstats.setStat("STR", 5);
        playerstats.setStat("DEF", 5);
        playerstats.setStat("CON", 5);
        playerstats.setStat("PWR", 5);
        playerstats.setStat("ENE", 5);
        playerstats.setIntValue("tps", 0);
        playerstats.setIntValue("curstam", 5);
        playerstats.removeAllSkills();
        playerstats.setStringValue("form", "base");
        playerstats.setStringValue("groupform", "");
        playerstats.setBoolean("turbo", false);
        playerstats.setBoolean("aura", false);
        playerstats.setBoolean("kaioplanet", false);
        playerstats.setIntValue("babaalivetimer", 0);
        playerstats.setIntValue("babacooldown", 0);
        playerstats.setIntValue("zenkaitimer", 0);
        playerstats.setIntValue("zenkaicount", 0);
        playerstats.setStringValue("form", "base");
        playerstats.setIntValue("release", 0);
        playerstats.removeTemporalEffect("mightfruit");
        playerstats.removePermanentEffect("majin");
        playerstats.removeFormSkill("super_form");
        playerstats.setIntValue("curenergy", 0);

        // NOTA: Lo de la vida se hace dos veces, pq a veces se buguea la primera vez xd

        player.setHealth(20);
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);
        player.setHealth(20);
    }

    private static void healPlayer(ServerPlayer player, DMZStatsAttributes playerstats) {
        DMZDatos dmzdatos = new DMZDatos();

        player.displayClientMessage(Component.translatable("lines.dende.heal.success"), true);

        double vidaTotal = dmzdatos.calcConstitution(playerstats);
        int energiaMax = dmzdatos.calcEnergy(playerstats);
        int staminaMax = dmzdatos.calcStamina(playerstats);

        player.heal((float) vidaTotal);
        playerstats.setIntValue("curstam", staminaMax);
        playerstats.setIntValue("curenergy", energiaMax);

        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(15.0F);
    }
}
