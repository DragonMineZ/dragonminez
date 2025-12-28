package com.dragonminez.common.init;

import com.dragonminez.Reference;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MainParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Reference.MOD_ID);

    public static final RegistryObject<SimpleParticleType> KI_FLASH =
            PARTICLE_TYPES.register("ki_flash", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> KI_SPLASH =
            PARTICLE_TYPES.register("ki_splash", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> KI_TRAIL =
            PARTICLE_TYPES.register("ki_trail", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> KINTON =
            PARTICLE_TYPES.register("kinton", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> PUNCH_PARTICLE =
            PARTICLE_TYPES.register("punch_particle", () -> new SimpleParticleType(true));

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }

}
