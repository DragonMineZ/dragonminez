package com.dragonminez.common.combat.util;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.combat.weapon.WeaponAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class SoundHelper {
    private static final Random rng = new Random();

    public static void playSound(ServerLevel world, Entity entity, WeaponAttributes.Sound sound) {
        playSound(world, entity, null, sound);
    }

    public static void playSound(ServerLevel world, Entity entity, @Nullable Player except, WeaponAttributes.Sound sound) {
        SoundEvent soundEvent = resolveSoundEvent(sound);
        if (soundEvent == null) return;

        try {
            world.playSound(except, entity.getX(), entity.getY(), entity.getZ(), soundEvent, SoundSource.PLAYERS, sound.volume(), computePitch(sound));
        } catch (Exception e) {
            LogUtil.error(Env.CLIENT, "Failed to play sound: " + sound.id());
            e.printStackTrace();
        }
    }

    @Nullable
    public static SoundEvent resolveSoundEvent(WeaponAttributes.Sound sound) {
        if (sound == null || sound.id() == null || sound.id().isBlank()) return null;

        ResourceLocation soundLoc = ResourceLocation.tryParse(sound.id());
        if (soundLoc == null) {
            soundLoc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, sound.id());
        }
        return ForgeRegistries.SOUND_EVENTS.getValue(soundLoc);
    }

    public static float computePitch(WeaponAttributes.Sound sound) {
        return (sound.randomness() > 0)
                ? rng.nextFloat(sound.pitch() - sound.randomness(), sound.pitch() + sound.randomness())
                : sound.pitch();
    }
}