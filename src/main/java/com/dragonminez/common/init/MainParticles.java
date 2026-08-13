package com.dragonminez.common.init;

import com.dragonminez.Reference;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class MainParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Reference.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> KI_FLASH =
            PARTICLE_TYPES.register("ki_flash", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> KI_SPLASH =
            PARTICLE_TYPES.register("ki_splash", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> KI_SPLASH_WAVE =
            PARTICLE_TYPES.register("ki_splash_wave", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> KI_TRAIL =
            PARTICLE_TYPES.register("ki_trail", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> KI_EXPLOSION_FLASH =
            PARTICLE_TYPES.register("ki_explosion_flash", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> KI_EXPLOSION_SPLASH =
            PARTICLE_TYPES.register("ki_explosion_splash", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> KI_EXPLOSION=
            PARTICLE_TYPES.register("ki_explosion", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> KI_SHEDDING =
            PARTICLE_TYPES.register("ki_shedding", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> KI_LIGHTNING =
            PARTICLE_TYPES.register("ki_lightning", () -> new SimpleParticleType(true));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> KINTON =
            PARTICLE_TYPES.register("kinton", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DUST =
            PARTICLE_TYPES.register("dust_particle", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ROCK =
            PARTICLE_TYPES.register("rock_particle", () -> new SimpleParticleType(true));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PUNCH_PARTICLE =
            PARTICLE_TYPES.register("punch_particle", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLOCK_PARTICLE =
            PARTICLE_TYPES.register("block_particle", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GUARD_BLOCK =
            PARTICLE_TYPES.register("guard_block_particle", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPARKS =
            PARTICLE_TYPES.register("sparks_particle", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AURA =
            PARTICLE_TYPES.register("aura_particle", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DIVINE =
            PARTICLE_TYPES.register("divine_particle", () -> new SimpleParticleType(true));


    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }

}
