package com.yuseix.dragonminez.init;

import com.yuseix.dragonminez.DragonMineZ;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MainSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS_REGISTER =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, DragonMineZ.MOD_ID);

    public static final RegistryObject<SoundEvent> GOLPE1 = registerSoundEvent("punch1");
    public static final RegistryObject<SoundEvent> GOLPE2 = registerSoundEvent("punch2");
    public static final RegistryObject<SoundEvent> GOLPE3 = registerSoundEvent("punch3");
    public static final RegistryObject<SoundEvent> GOLPE4 = registerSoundEvent("punch4");
    public static final RegistryObject<SoundEvent> GOLPE5 = registerSoundEvent("punch5");
    public static final RegistryObject<SoundEvent> GOLPE6 = registerSoundEvent("punch6");
    public static final RegistryObject<SoundEvent> CRITICO1 = registerSoundEvent("critic_punch1");
    public static final RegistryObject<SoundEvent> CRITICO2 = registerSoundEvent("critic_punch2");

    public static final RegistryObject<SoundEvent> RADAR_SCAN = registerSoundEvent("radar_scan");
    public static final RegistryObject<SoundEvent> DRAGONRADAR = registerSoundEvent("dragonradar");
    public static final RegistryObject<SoundEvent> NUBE = registerSoundEvent("nube");
    public static final RegistryObject<SoundEvent> SENZU_BEAN = registerSoundEvent("senzu");
    public static final RegistryObject<SoundEvent> DRAGONBALLS = registerSoundEvent("dragonballssound");
    public static final RegistryObject<SoundEvent> SHENRON = registerSoundEvent("shenron");

    public static final RegistryObject<SoundEvent> FROG1 = registerSoundEvent("frogsound1");
    public static final RegistryObject<SoundEvent> FROG2 = registerSoundEvent("frogsound2");
    public static final RegistryObject<SoundEvent> FROG3 = registerSoundEvent("frogsound3");
    public static final RegistryObject<SoundEvent> FROG_LAUGH = registerSoundEvent("froglaugh");

    public static final RegistryObject<SoundEvent> MENU_MUSIC = registerSoundEvent("menu_music");
  
  public static final RegistryObject<SoundEvent> NAVE_ATERRIZANDO = registerSoundEvent("landing_ship");

    public static final RegistryObject<SoundEvent> AURA_START = registerSoundEvent("aura_start");
    public static final RegistryObject<SoundEvent> KI_CHARGE_LOOP = registerSoundEvent("ki_charge_loop");
    public static final RegistryObject<SoundEvent> TURBO_LOOP = registerSoundEvent("turbo_loop");

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        ResourceLocation id = new ResourceLocation(DragonMineZ.MOD_ID, name);

        return SOUND_EVENTS_REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus busEvent) {
        SOUND_EVENTS_REGISTER.register(busEvent);
    }
}
