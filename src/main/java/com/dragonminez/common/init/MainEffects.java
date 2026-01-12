package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.init.effects.StaggerEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MainEffects {

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Reference.MOD_ID);

    public static final RegistryObject<MobEffect> STAGGER = EFFECTS.register("stagger", StaggerEffect::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }



}
