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

        if (mc.isPaused()) return;

        for (Player player : mc.level.players()) {
            updatePlayerAuraSound(player, mc);
        }

        ACTIVE_AURA_SOUNDS.entrySet().removeIf(entry ->
                entry.getValue().isStopped() || !mc.getSoundManager().isActive(entry.getValue()));

        if (mc.level.getGameTime() % 200 == 0) { // Cada 10 segundos
            LIGHTNING_TIMERS.keySet().removeIf(uuid -> mc.level.getPlayerByUUID(uuid) == null);
        }
    }

    private static void updatePlayerAuraSound(Player player, Minecraft mc) {
        UUID playerId = player.getUUID();

        var statsCap = StatsProvider.get(StatsCapability.INSTANCE, player);
        var stats = statsCap.orElse(null);

        if (stats == null) return;

        var character = stats.getCharacter();

        boolean hasAura = stats.getStatus().isAuraActive() || stats.getStatus().isPermanentAura();

        AuraLoopSound existing = ACTIVE_AURA_SOUNDS.get(playerId);
        // isStopped() solo cubre las paradas que hacemos nosotros; isActive() detecta
        // ademas cuando el motor de sonido descarta el loop por su cuenta (falta de
        // canales, buffer underrun, limite de sonidos) para poder recrearlo.
        boolean isPlaying = existing != null && !existing.isStopped()
                && mc.getSoundManager().isActive(existing);

        if (hasAura) {
            if (!isPlaying) {
                AuraLoopSound sound = new AuraLoopSound(player);
                mc.getSoundManager().play(sound);
                ACTIVE_AURA_SOUNDS.put(playerId, sound);
            }
        }

        boolean hasLightnings = false;

        if (character.hasActiveStackForm() && character.getActiveStackFormData() != null) {
            if (character.getActiveStackFormData().getHasLightnings()) {
                hasLightnings = true;
            }
        }
        else if (character.hasActiveForm() && character.getActiveFormData() != null) {
            if (character.getActiveFormData().getHasLightnings()) {
                hasLightnings = true;
            }
        }

        if (hasLightnings) {
            long currentTime = mc.level.getGameTime();
            long nextPlayTime = LIGHTNING_TIMERS.getOrDefault(playerId, 0L);

            if (currentTime >= nextPlayTime) {
                float volume = 0.3F;
                float pitch = 0.9F + player.getRandom().nextFloat() * 0.2F;

//                mc.level.playSound(null, player.getX(), player.getY(), player.getZ(),
//                        MainSounds.KI_SPARKS.get(),
//                        SoundSource.PLAYERS,
//                        volume,
//                        pitch);

                mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                        MainSounds.KI_SPARKS.get(),
                        SoundSource.PLAYERS,
                        volume,
                        pitch,
                        false);

                LIGHTNING_TIMERS.put(playerId, currentTime + 60L);
            }
        } else {
            LIGHTNING_TIMERS.remove(playerId);
        }
    }
}