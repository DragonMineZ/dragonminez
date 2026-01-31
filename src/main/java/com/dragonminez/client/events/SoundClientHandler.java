package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.common.init.sounds.AuraLoopSound;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class SoundClientHandler {

    // Mapa para rastrear los sonidos activos por jugador (UUID)
    private static final Map<UUID, AuraLoopSound> ACTIVE_AURA_SOUNDS = new HashMap<>();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            ACTIVE_AURA_SOUNDS.clear();
            return;
        }

        for (Player player : mc.level.players()) {
            updatePlayerAuraSound(player, mc);
        }

        ACTIVE_AURA_SOUNDS.entrySet().removeIf(entry -> entry.getValue().isStopped());
    }

    private static void updatePlayerAuraSound(Player player, Minecraft mc) {
        UUID playerId = player.getUUID();

        var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        boolean hasAura = (stats != null && stats.getStatus().isAuraActive());

        boolean isPlaying = ACTIVE_AURA_SOUNDS.containsKey(playerId) && !ACTIVE_AURA_SOUNDS.get(playerId).isStopped();

        if (hasAura) {
            if (!isPlaying) {
                AuraLoopSound sound = new AuraLoopSound(player);
                mc.getSoundManager().play(sound);
                ACTIVE_AURA_SOUNDS.put(playerId, sound);
            }
        }
    }
}