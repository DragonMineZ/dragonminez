package com.dragonminez.common.init;

import com.dragonminez.Reference;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Optional;

public final class MainPotions {

	public static final DeferredRegister<Potion> POTION_REGISTER =
			DeferredRegister.create(BuiltInRegistries.POTION, Reference.MOD_ID);

	public static final int CURATIVE_COLOR = 0x9DFF00;

	public static final DeferredHolder<Potion, Potion> CURATIVE = POTION_REGISTER.register("curative",
			() -> new Potion("curative", new MobEffectInstance(MobEffects.REGENERATION, 100, 4)));

	public static ItemStack createCurativeFlask() {
		ItemStack stack = new ItemStack(Items.POTION);
		stack.set(DataComponents.POTION_CONTENTS,
				new PotionContents(Optional.of(CURATIVE), Optional.of(CURATIVE_COLOR), List.of()));
		return stack;
	}

	public static void register(IEventBus bus) {
		POTION_REGISTER.register(bus);
	}
}
