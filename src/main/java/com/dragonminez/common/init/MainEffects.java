package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.init.effects.DMZEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class MainEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, Reference.MOD_ID);

	// Real Effects | Harmful
	public static final DeferredHolder<MobEffect, MobEffect> MAJIN = EFFECTS.register("majin", () -> new DMZEffect(false));
    public static final DeferredHolder<MobEffect, MobEffect> STAGGER = EFFECTS.register("stagger", () -> new DMZEffect(false)
			.addAttributeModifier(Attributes.MOVEMENT_SPEED, com.dragonminez.common.util.AttributeMods.id(java.util.UUID.fromString("19421075-15D9-4372-A2C7-57BC617E0906")), -0.25F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
			.addAttributeModifier(Attributes.ATTACK_SPEED, com.dragonminez.common.util.AttributeMods.id(java.util.UUID.fromString("55FCED67-E92A-486E-9800-B47F202C4386")), -0.25F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	public static final DeferredHolder<MobEffect, MobEffect> STUN = EFFECTS.register("stun", () ->
			new DMZEffect(false).addAttributeModifier(Attributes.MOVEMENT_SPEED, com.dragonminez.common.util.AttributeMods.id(java.util.UUID.fromString("7107DE5E-7CE8-4030-940E-514C1F160890")), -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	public static final DeferredHolder<MobEffect, MobEffect> KI_SLOW = EFFECTS.register("ki_slow", () ->
			new DMZEffect(false).addAttributeModifier(Attributes.MOVEMENT_SPEED, com.dragonminez.common.util.AttributeMods.id(java.util.UUID.fromString("E21BB9D6-BF78-4D05-83DE-497DA4664A43")), -0.9D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

	// Placeholders, info for the player | Neutral
	public static final DeferredHolder<MobEffect, MobEffect> DASH_CD = EFFECTS.register("dash_cd", () -> new DMZEffect());
	public static final DeferredHolder<MobEffect, MobEffect> DOUBLEDASH_CD = EFFECTS.register("doubledash_cd", () -> new DMZEffect());
	public static final DeferredHolder<MobEffect, MobEffect> TELEPORT_CD = EFFECTS.register("teleport_cd", () -> new DMZEffect());
	public static final DeferredHolder<MobEffect, MobEffect> FUSED = EFFECTS.register("fused", () -> new DMZEffect());
	public static final DeferredHolder<MobEffect, MobEffect> FUSION_CD = EFFECTS.register("fusion_cd", () -> new DMZEffect());
	public static final DeferredHolder<MobEffect, MobEffect> SAIYAN_PASSIVE = EFFECTS.register("saiyan_passive", () -> new DMZEffect());
	public static final DeferredHolder<MobEffect, MobEffect> BIOANDROID_PASSIVE = EFFECTS.register("bioandroid_passive", () -> new DMZEffect());
	public static final DeferredHolder<MobEffect, MobEffect> MAJIN_REVIVE = EFFECTS.register("majin_revive", () -> new DMZEffect());
	public static final DeferredHolder<MobEffect, MobEffect> KI_BLAST_CD = EFFECTS.register("ki_blast_cd", () -> new DMZEffect());
	public static final DeferredHolder<MobEffect, MobEffect> POISE_CD = EFFECTS.register("poise_cd", () -> new DMZEffect());

	// Status Effects | Beneficial
	public static final DeferredHolder<MobEffect, MobEffect> KICHARGE = EFFECTS.register("kicharge", () -> new DMZEffect(true));
	public static final DeferredHolder<MobEffect, MobEffect> TRANSFORM = EFFECTS.register("transform", () -> new DMZEffect(true));
	public static final DeferredHolder<MobEffect, MobEffect> TRANSFORMED = EFFECTS.register("transformed", () -> new DMZEffect(true));
	public static final DeferredHolder<MobEffect, MobEffect> STACK_TRANSFORM = EFFECTS.register("stack_transform", () -> new DMZEffect(true));
	public static final DeferredHolder<MobEffect, MobEffect> STACK_TRANSFORMED = EFFECTS.register("stack_transformed", () -> new DMZEffect(true));
	public static final DeferredHolder<MobEffect, MobEffect> FLY = EFFECTS.register("fly", () -> new DMZEffect(true));

	// Bonus Effects | Beneficial
	public static final DeferredHolder<MobEffect, MobEffect> MIGHTFRUIT = EFFECTS.register("mightfruit", () -> new DMZEffect(true));
    public static final DeferredHolder<MobEffect, MobEffect> CANDY = EFFECTS.register("candy", () -> new DMZEffect(true));
	public static final DeferredHolder<MobEffect, MobEffect> KI_REGEN = EFFECTS.register("ki_regen", () -> new DMZEffect(MobEffectCategory.BENEFICIAL, 0x3C7DFF));
	public static final DeferredHolder<MobEffect, MobEffect> STAMINA_REGEN = EFFECTS.register("stamina_regen", () -> new DMZEffect(MobEffectCategory.BENEFICIAL, 0x45B35E));
	public static final DeferredHolder<MobEffect, MobEffect> TP_GAIN = EFFECTS.register("tp_gain", () -> new DMZEffect(MobEffectCategory.BENEFICIAL, 0xF29B38));
	public static final DeferredHolder<MobEffect, MobEffect> MASTERY_GAIN = EFFECTS.register("mastery_gain", () -> new DMZEffect(MobEffectCategory.BENEFICIAL, 0x9B59D0));
	public static final DeferredHolder<MobEffect, MobEffect> MUTANT = EFFECTS.register("mutant", () -> new DMZEffect(MobEffectCategory.BENEFICIAL, 0xB14CE0));

    public static void register(IEventBus eventBus) { EFFECTS.register(eventBus); }
}