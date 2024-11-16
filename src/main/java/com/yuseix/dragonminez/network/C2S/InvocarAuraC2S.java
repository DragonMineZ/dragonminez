package com.yuseix.dragonminez.network.C2S;

import com.google.common.collect.Maps;
import com.yuseix.dragonminez.init.MainEntity;
import com.yuseix.dragonminez.init.entity.custom.fpcharacters.AuraEntity;
import com.yuseix.dragonminez.network.ModMessages;
import com.yuseix.dragonminez.network.S2C.InvocarAuraS2C;
import com.yuseix.dragonminez.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.stats.DMZStatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class InvocarAuraC2S {

    public static final Map<UUID, AuraEntity> playerAuraMap = Maps.newConcurrentMap();

    public InvocarAuraC2S() {
    }

    public InvocarAuraC2S(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public static void handle(InvocarAuraC2S packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
                    UUID playerId = player.getUUID();

                    if (cap.isAuraOn()) {
                        playerAuraMap.computeIfAbsent(playerId, id -> {
                            AuraEntity newAura = new AuraEntity(MainEntity.AURA.get(), player.level());
                            newAura.setOwnerUUID(player.getUUID());
                            newAura.setRaza(cap.getRace());
                            newAura.setTransformation(cap.getDmzState());
                            // Por qué se usa el color 16777045?
                            if (cap.getRace() == 1 || cap.getRace() == 3 && cap.getDmzState() != 1) {
                                newAura.setColorAura(16777045);
                            } else {
                                newAura.setColorAura(cap.getAuraColor());
                            }
                            player.level().addFreshEntity(newAura);
                            newAura.setPos(player.getX(), player.getY(), player.getZ());
                            return newAura;
                        });

                        float transparency = playerId.equals(player.getUUID()) && isInFirstPersonView(player) ? 0.05F : 0.15F;
                        ModMessages.sendToPlayer(new InvocarAuraS2C(playerId, transparency), player);
                    } else {
                        AuraEntity aura = playerAuraMap.remove(playerId);
                        if (aura != null) {
                            aura.remove(Entity.RemovalReason.DISCARDED);
                        }
                    }
                });
            }
        });
        context.setPacketHandled(true);
    }

    public static boolean isInFirstPersonView(Player player) {
        return Minecraft.getInstance().options.getCameraType().isFirstPerson();
    }
}
