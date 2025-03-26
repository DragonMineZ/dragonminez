package com.yuseix.dragonminez.common.network.C2S;

import com.yuseix.dragonminez.common.config.old.DMZGeneralConfig;
import com.yuseix.dragonminez.common.init.MainItems;
import com.yuseix.dragonminez.common.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.common.stats.DMZStatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class KarinC2S {

    private int option;

    public KarinC2S(int option) {
        this.option = option;
    }
    public KarinC2S(FriendlyByteBuf buf) {
        option = buf.readInt();

    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(option);

    }
    public static void handle(KarinC2S packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {

            ServerPlayer player = ctx.get().getSender();

            if (player != null) {
                DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
                    Item nubeItem;
                    if (cap.getIntValue("alignment") < 60) nubeItem = MainItems.NUBE_NEGRA_ITEM.get();
                    else nubeItem = MainItems.NUBE_ITEM.get();

                    if(packet.option == 1){
                        player.getInventory().add(new ItemStack(nubeItem));
                    }else if(packet.option == 2){
                        player.getInventory().add(new ItemStack(MainItems.SENZU_BEAN.get(), DMZGeneralConfig.SENZU_GIVE.get()));
                    } else if (packet.option == 3) {
                        //cap.setIntValue("senzutimer", DMZGeneralConfig.SENZU_DAILY_COOLDOWN.get());
                        cap.setIntValue("senzutimer", DMZGeneralConfig.SENZU_DAILY_COOLDOWN.get() * 20);
                    }
                });


            }

        });
        context.setPacketHandled(true);
    }
}
