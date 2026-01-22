package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.init.effects.DMZEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MainEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Reference.MOD_ID);

    public static final RegistryObject<MobEffect> STAGGER = EFFECTS.register("stagger", DMZEffect::new);
	public static final RegistryObject<MobEffect> STUN = EFFECTS.register("stun", () ->
			new DMZEffect().addAttributeModifier(Attributes.MOVEMENT_SPEED, "7107DE5E-7CE8-4030-940E-514C1F160890",
					-1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL));
	public static final RegistryObject<MobEffect> DASH_CD = EFFECTS.register("dash_cd", DMZEffect::new);
	public static final RegistryObject<MobEffect> DOUBLEDASH_CD = EFFECTS.register("doubledash_cd", DMZEffect::new);

    public static void register(IEventBus eventBus) { EFFECTS.register(eventBus); }
}