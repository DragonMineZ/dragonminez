package com.yuseix.dragonminez.common.network.C2S;

import com.yuseix.dragonminez.common.config.old.DMZGeneralConfig;
import com.yuseix.dragonminez.common.init.MainEntity;
import com.yuseix.dragonminez.common.init.MainItems;
import com.yuseix.dragonminez.common.init.entity.custom.ShenlongEntity;
import com.yuseix.dragonminez.common.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.common.stats.DMZStatsProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ShenlongC2S {
    private final int option;

    public ShenlongC2S(int option) {
        this.option = option;
    }

    public ShenlongC2S(FriendlyByteBuf buf) {
        option = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(option);
    }

    public static void handle(ShenlongC2S packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player != null) {
                switch (packet.option) {
                    case 1 -> player.getInventory().add(new ItemStack(MainItems.CAPSULA_ROJA.get(), DMZGeneralConfig.CAPSULE_SHENRON_WISH.get()));
                    case 2 -> player.getInventory().add(new ItemStack(MainItems.CAPSULA_MORADA.get(), DMZGeneralConfig.CAPSULE_SHENRON_WISH.get()));
                    case 3 -> player.getInventory().add(new ItemStack(MainItems.CAPSULA_VERDE.get(), DMZGeneralConfig.CAPSULE_SHENRON_WISH.get()));
                    case 4 -> player.getInventory().add(new ItemStack(MainItems.CAPSULA_AZUL.get(), DMZGeneralConfig.CAPSULE_SHENRON_WISH.get()));
                    case 5 -> player.getInventory().add(new ItemStack(MainItems.CAPSULA_ANARANJADA.get(), DMZGeneralConfig.CAPSULE_SHENRON_WISH.get()));
                    case 6 -> player.getInventory().add(new ItemStack(MainItems.SENZU_BEAN.get(), DMZGeneralConfig.SENZU_SHENRON_WISH.get()));
                    case 7 -> player.getInventory().add(new ItemStack(MainItems.T2_RADAR_CPU.get()));
                }

                if (packet.option == 8) {
                    DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
                        if (cap.getBoolean("alive")) {
                            player.displayClientMessage(Component.literal("lines.shenron.new_wish").withStyle(ChatFormatting.RED), true);
                        } else {
                            cap.setBoolean("alive", true);
                            cap.setIntValue("babacooldown", 0);
                            cap.setIntValue("babaalivetimer", 0);

                            player.level().getEntities(player, player.getBoundingBox().inflate(50), entity ->
                                    entity.getType() == MainEntity.SHENLONG.get()).forEach(entity -> {
                                if (entity instanceof ShenlongEntity shenlong) {
                                    shenlong.setDeseos(0);
                                }
                            });
                        }
                        });
                } else {
                    player.level().getEntities(player, player.getBoundingBox().inflate(50), entity ->
                            entity.getType() == MainEntity.SHENLONG.get()).forEach(entity -> {
                        if (entity instanceof ShenlongEntity shenlong) {
                            shenlong.setDeseos(0);
                        }
                    });
                }
            }
        });
        context.setPacketHandled(true);
    }
}
