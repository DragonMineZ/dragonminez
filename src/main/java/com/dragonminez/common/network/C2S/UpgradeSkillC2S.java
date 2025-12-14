package com.dragonminez.common.network.C2S;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpgradeSkillC2S {
    private final String skillName;

    public UpgradeSkillC2S(String skillName) {
        this.skillName = skillName;
    }

    public UpgradeSkillC2S(FriendlyByteBuf buf) {
        this.skillName = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.skillName);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                    // TODO: Implementar la lógica de upgrade de skills
                    // Por ahora solo es un placeholder
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

