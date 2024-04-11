package com.yuseix.dragonminez.init;

import com.yuseix.dragonminez.DragonMineZ;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public interface MainSounds {

    DeferredRegister<SoundEvent> SOUND_EVENTS_REGISTER =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, DragonMineZ.MOD_ID);

    RegistryObject<SoundEvent> GOLPE1 = registerSoundEvent("punch1");
    RegistryObject<SoundEvent> GOLPE2 = registerSoundEvent("punch2");
    RegistryObject<SoundEvent> GOLPE3 = registerSoundEvent("punch3");
    RegistryObject<SoundEvent> GOLPE4 = registerSoundEvent("punch4");
    RegistryObject<SoundEvent> GOLPE5 = registerSoundEvent("punch5");
    RegistryObject<SoundEvent> GOLPE6 = registerSoundEvent("punch6");
    RegistryObject<SoundEvent> CRITICO1 = registerSoundEvent("critic_punch1");
    RegistryObject<SoundEvent> CRITICO2 = registerSoundEvent("critic_punch2");

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        ResourceLocation id = new ResourceLocation(DragonMineZ.MOD_ID, name);
        return SOUND_EVENTS_REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
