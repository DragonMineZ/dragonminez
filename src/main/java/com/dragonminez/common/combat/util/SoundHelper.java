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
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Random;

public class SoundHelper {
    private static final Random rng = new Random();

    public static void playSound(ServerLevel world, Entity entity, WeaponAttributes.Sound sound) {
        if (sound == null || sound.id() == null || sound.id().isBlank()) return;

        try {
            float pitch = (sound.randomness() > 0) ? rng.nextFloat(sound.pitch() - sound.randomness(), sound.pitch() + sound.randomness()) : sound.pitch();
            ResourceLocation soundLoc = ResourceLocation.tryParse(sound.id());
            if (soundLoc == null) {
                soundLoc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, sound.id());
            }
            SoundEvent soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(soundLoc);
            if (soundEvent != null) world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), soundEvent, SoundSource.PLAYERS, sound.volume(), pitch);
        } catch (Exception e) {
            LogUtil.error(Env.CLIENT, "Failed to play sound: " + sound.id());
            e.printStackTrace();
        }
    }
}