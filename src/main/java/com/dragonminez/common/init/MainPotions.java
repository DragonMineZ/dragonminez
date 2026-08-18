package com.dragonminez.common.init;

import com.dragonminez.Reference;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MainPotions {

	public static final DeferredRegister<Potion> POTION_REGISTER =
			DeferredRegister.create(ForgeRegistries.POTIONS, Reference.MOD_ID);

	/** Color verde limón del líquido curativo (se fuerza con CustomPotionColor). */
	public static final int CURATIVE_COLOR = 0x9DFF00;

	// Frasco con líquido curativo: poción de Regeneración V (5 s).
	public static final RegistryObject<Potion> CURATIVE = POTION_REGISTER.register("curative",
			() -> new Potion("curative", new MobEffectInstance(MobEffects.REGENERATION, 100, 4)));

	/**
	 * Crea el stack del frasco curativo con el color de líquido verde limón forzado.
	 * Fuente única de verdad: lo usan el brewing (salida) y la receta de cápsulas (ingrediente NBT-estricto),
	 * así que ambos comparten exactamente el mismo NBT y casan.
	 */
	public static ItemStack createCurativeFlask() {
		ItemStack stack = PotionUtils.setPotion(new ItemStack(Items.POTION), CURATIVE.get());
		stack.getOrCreateTag().putInt("CustomPotionColor", CURATIVE_COLOR);
		return stack;
	}

	public static void register(IEventBus bus) {
		POTION_REGISTER.register(bus);
	}
}
