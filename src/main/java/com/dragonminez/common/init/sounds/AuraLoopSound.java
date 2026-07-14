package com.dragonminez.common.init.sounds;

import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public class AuraLoopSound extends AbstractTickableSoundInstance {
    private final Player player;

    public AuraLoopSound(Player player) {
        super(MainSounds.KI_CHARGE_LOOP.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());

        this.player = player;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.5F;
        this.pitch = 1.0F;

        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
    }

    @Override
    public boolean canStartSilent() {
        // Permite que el loop se registre aunque el volumen sea 0 en ese instante
        // (categoria muteada, atenuacion momentanea). Asi el motor no lo descarta y
        // vuelve a oirse solo cuando el volumen regresa, sin recrearlo cada tick.
        return true;
    }

    @Override
    public void tick() {
        if (this.player.isRemoved()) {
            this.stop();
            return;
        }

        var stats = StatsProvider.get(StatsCapability.INSTANCE, this.player).orElse(null);

        if (stats == null || !(stats.getStatus().isAuraActive() || stats.getStatus().isPermanentAura())) {
            this.stop();
            return;
        }

        this.x = this.player.getX();
        this.y = this.player.getY();
        this.z = this.player.getZ();

        // this.pitch = 1.0f + (stats.getCharacter().getPowerLevel() / 10000f);
    }
}