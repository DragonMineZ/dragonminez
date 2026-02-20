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

	// Real Effects | Harmful
    public static final RegistryObject<MobEffect> STAGGER = EFFECTS.register("stagger", () -> new DMZEffect(false));
	public static final RegistryObject<MobEffect> STUN = EFFECTS.register("stun", () ->
			new DMZEffect(false).addAttributeModifier(Attributes.MOVEMENT_SPEED, "7107DE5E-7CE8-4030-940E-514C1F160890",
					-1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL));

	// Placeholders, info for the player | Neutral
	public static final RegistryObject<MobEffect> DASH_CD = EFFECTS.register("dash_cd", DMZEffect::new);
	public static final RegistryObject<MobEffect> DOUBLEDASH_CD = EFFECTS.register("doubledash_cd", DMZEffect::new);
	public static final RegistryObject<MobEffect> FUSED = EFFECTS.register("fused", DMZEffect::new);
	public static final RegistryObject<MobEffect> COMBO_CD = EFFECTS.register("combo_cd", DMZEffect::new);
	public static final RegistryObject<MobEffect> SAIYAN_PASSIVE = EFFECTS.register("saiyan_passive", DMZEffect::new);
	public static final RegistryObject<MobEffect> BIOANDROID_PASSIVE = EFFECTS.register("bioandroid_passive", DMZEffect::new);
	public static final RegistryObject<MobEffect> MAJIN_REVIVE = EFFECTS.register("majin_revive", DMZEffect::new);

	// Status Effects | Beneficial
	public static final RegistryObject<MobEffect> KICHARGE = EFFECTS.register("kicharge", () -> new DMZEffect(true));
	public static final RegistryObject<MobEffect> TRANSFORM = EFFECTS.register("transform", () -> new DMZEffect(true));
	public static final RegistryObject<MobEffect> FLY = EFFECTS.register("fly", () -> new DMZEffect(true));

	// Bonus Effects | Beneficial
	public static final RegistryObject<MobEffect> MAJIN = EFFECTS.register("majin", () -> new DMZEffect(true));
	public static final RegistryObject<MobEffect> MIGHTFRUIT = EFFECTS.register("mightfruit", () -> new DMZEffect(true));

    public static void register(IEventBus eventBus) { EFFECTS.register(eventBus); }
}