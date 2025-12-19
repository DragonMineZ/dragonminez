package com.dragonminez.common.network;

import com.dragonminez.client.gui.QuestsMenuScreen;
import com.dragonminez.client.gui.SkillsMenuScreen;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ClientPacketHandler {

    @OnlyIn(Dist.CLIENT)
    public static void handleStatsSyncPacket(int playerId, CompoundTag nbt) {
        var clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) return;

        var entity = clientLevel.getEntity(playerId);
        if (entity instanceof Player player) {
            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                data.load(nbt);
                player.refreshDimensions();
            });
        }
    }
}

