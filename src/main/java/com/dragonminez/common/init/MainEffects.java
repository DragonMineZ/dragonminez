package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.init.effects.DMZEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MainEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Reference.MOD_ID);

	// Real Effects | Harmful
	public static final RegistryObject<MobEffect> MAJIN = EFFECTS.register("majin", () -> new DMZEffect(false));
    public static final RegistryObject<MobEffect> STAGGER = EFFECTS.register("stagger", () -> new DMZEffect(false)
			.addAttributeModifier(Attributes.MOVEMENT_SPEED, "19421075-15D9-4372-A2C7-57BC617E0906", -0.25F, AttributeModifier.Operation.MULTIPLY_TOTAL)
			.addAttributeModifier(Attributes.ATTACK_SPEED, "55FCED67-E92A-486E-9800-B47F202C4386", -0.25F, AttributeModifier.Operation.MULTIPLY_TOTAL));
	public static final RegistryObject<MobEffect> STUN = EFFECTS.register("stun", () ->
			new DMZEffect(false).addAttributeModifier(Attributes.MOVEMENT_SPEED, "7107DE5E-7CE8-4030-940E-514C1F160890",
					-1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL));

	// Placeholders, info for the player | Neutral
	public static final RegistryObject<MobEffect> DASH_CD = EFFECTS.register("dash_cd", DMZEffect::new);
	public static final RegistryObject<MobEffect> DOUBLEDASH_CD = EFFECTS.register("doubledash_cd", DMZEffect::new);
	public static final RegistryObject<MobEffect> TELEPORT_CD = EFFECTS.register("teleport_cd", DMZEffect::new);
	public static final RegistryObject<MobEffect> FUSED = EFFECTS.register("fused", DMZEffect::new);
	public static final RegistryObject<MobEffect> SAIYAN_PASSIVE = EFFECTS.register("saiyan_passive", DMZEffect::new);
	public static final RegistryObject<MobEffect> BIOANDROID_PASSIVE = EFFECTS.register("bioandroid_passive", DMZEffect::new);
	public static final RegistryObject<MobEffect> MAJIN_REVIVE = EFFECTS.register("majin_revive", DMZEffect::new);
	public static final RegistryObject<MobEffect> KI_BLAST_CD = EFFECTS.register("ki_blast_cd", DMZEffect::new);
	public static final RegistryObject<MobEffect> POISE_CD = EFFECTS.register("poise_cd", DMZEffect::new);

	// Status Effects | Beneficial
	public static final RegistryObject<MobEffect> KICHARGE = EFFECTS.register("kicharge", () -> new DMZEffect(true));
	public static final RegistryObject<MobEffect> TRANSFORM = EFFECTS.register("transform", () -> new DMZEffect(true));
	public static final RegistryObject<MobEffect> TRANSFORMED = EFFECTS.register("transformed", () -> new DMZEffect(true));
	public static final RegistryObject<MobEffect> STACK_TRANSFORM = EFFECTS.register("stack_transform", () -> new DMZEffect(true));
	public static final RegistryObject<MobEffect> STACK_TRANSFORMED = EFFECTS.register("stack_transformed", () -> new DMZEffect(true));
	public static final RegistryObject<MobEffect> FLY = EFFECTS.register("fly", () -> new DMZEffect(true));

	// Bonus Effects | Beneficial
	public static final RegistryObject<MobEffect> MIGHTFRUIT = EFFECTS.register("mightfruit", () -> new DMZEffect(true));
    public static final RegistryObject<MobEffect> CANDY = EFFECTS.register("candy", () -> new DMZEffect(true));
	public static final RegistryObject<MobEffect> KI_REGEN = EFFECTS.register("ki_regen", () -> new DMZEffect(MobEffectCategory.BENEFICIAL, 0x3C7DFF));
	public static final RegistryObject<MobEffect> STAMINA_REGEN = EFFECTS.register("stamina_regen", () -> new DMZEffect(MobEffectCategory.BENEFICIAL, 0x45B35E));
	public static final RegistryObject<MobEffect> TP_GAIN = EFFECTS.register("tp_gain", () -> new DMZEffect(MobEffectCategory.BENEFICIAL, 0xF29B38));
	public static final RegistryObject<MobEffect> MASTERY_GAIN = EFFECTS.register("mastery_gain", () -> new DMZEffect(MobEffectCategory.BENEFICIAL, 0x9B59D0));
	public static final RegistryObject<MobEffect> MUTANT = EFFECTS.register("mutant", () -> new DMZEffect(MobEffectCategory.BENEFICIAL, 0xB14CE0));

    public static void register(IEventBus eventBus) { EFFECTS.register(eventBus); }
}