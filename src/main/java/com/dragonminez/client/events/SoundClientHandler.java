package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.sounds.AuraLoopSound;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
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

    private static final Map<UUID, AuraLoopSound> ACTIVE_AURA_SOUNDS = new HashMap<>();

    private static final Map<UUID, Long> LIGHTNING_TIMERS = new HashMap<>();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            ACTIVE_AURA_SOUNDS.clear();
            LIGHTNING_TIMERS.clear();
            return;
        }

        for (Player player : mc.level.players()) {
            updatePlayerAuraSound(player, mc);
        }

        ACTIVE_AURA_SOUNDS.entrySet().removeIf(entry -> entry.getValue().isStopped());

        if (mc.level.getGameTime() % 200 == 0) { // Cada 10 segundos
            LIGHTNING_TIMERS.keySet().removeIf(uuid -> mc.level.getPlayerByUUID(uuid) == null);
        }
    }

    private static void updatePlayerAuraSound(Player player, Minecraft mc) {
        UUID playerId = player.getUUID();

        var statsCap = StatsProvider.get(StatsCapability.INSTANCE, player);
        var stats = statsCap.orElse(null);

        if (stats == null) return;

        boolean hasAura = stats.getStatus().isAuraActive();
        boolean isPlaying = ACTIVE_AURA_SOUNDS.containsKey(playerId) && !ACTIVE_AURA_SOUNDS.get(playerId).isStopped();

        if (hasAura) {
            if (!isPlaying) {
                AuraLoopSound sound = new AuraLoopSound(player);
                mc.getSoundManager().play(sound);
                ACTIVE_AURA_SOUNDS.put(playerId, sound);
            }

            var character = stats.getCharacter();

            if (character.hasActiveForm() && character.getActiveFormData() != null) {
                if (character.getActiveFormData().hasLightnings()) {

                    long currentTime = System.currentTimeMillis();
                    long nextPlayTime = LIGHTNING_TIMERS.getOrDefault(playerId, 0L);

                    if (currentTime >= nextPlayTime) {
                        float volume = 0.3F;
                        float pitch = 0.9F + player.getRandom().nextFloat() * 0.2F;

                        mc.level.playSound(mc.player, player.getX(), player.getY(), player.getZ(),
                                MainSounds.KI_SPARKS.get(),
                                SoundSource.PLAYERS,
                                volume,
                                pitch);

                        LIGHTNING_TIMERS.put(playerId, currentTime + 3000L);
                    }
                }
            }
        } else {
            LIGHTNING_TIMERS.remove(playerId);
        }
    }
}