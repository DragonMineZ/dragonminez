package com.dragonminez.common.network.C2S;

import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class SokidanControlC2S {

    public SokidanControlC2S() {
    }

    public SokidanControlC2S(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.level() != null) {
                double searchRadius = 64.0;
                AABB searchBox = player.getBoundingBox().inflate(searchRadius);

                List<KiBlastEntity> blasts = player.level().getEntitiesOfClass(KiBlastEntity.class, searchBox);

                for (KiBlastEntity blast : blasts) {
                    if (blast.isControllable() && blast.getOwner() != null && blast.getOwner().getUUID().equals(player.getUUID())) {
                        blast.toggleSokidanControl();
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
