package com.dragonminez.common.init;

import com.dragonminez.Reference;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;

import java.util.Optional;

/**
 * Enchantment keys for DragonMineZ.
 * In Minecraft 1.21 Enchantment is a final record and is data-driven; subclass-based registration
 * is no longer possible. Call sites use {@link #level} helpers against datapack-registered entries.
 * JSON definitions can be added under data/dragonminez/enchantment/ in a follow-up.
 */
public final class MainEnchants {
	private MainEnchants() {}

	public static final ResourceKey<Enchantment> VITALITY_RECOVERY = key("vitality_recovery");
	public static final ResourceKey<Enchantment> RESISTANCE_RECOVERY = key("resistance_recovery");
	public static final ResourceKey<Enchantment> ENERGY_RECOVERY = key("energy_recovery");
	public static final ResourceKey<Enchantment> DEFENSE_PENETRATION = key("defense_penetration");
	public static final ResourceKey<Enchantment> HEALING_REDUCTION = key("healing_reduction");
	public static final ResourceKey<Enchantment> CRIT_CHANCE = key("critical_chance");
	public static final ResourceKey<Enchantment> CRIT_DAMAGE = key("critical_damage");
	public static final ResourceKey<Enchantment> KI_CONDUCTIVITY = key("ki_conductivity");
	public static final ResourceKey<Enchantment> GRAVITY_FORGED = key("gravity_forged");

	private static ResourceKey<Enchantment> key(String path) {
		return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, path));
	}

	public static Optional<Holder.Reference<Enchantment>> holder(Level level, ResourceKey<Enchantment> key) {
		return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key);
	}

	public static int level(ItemStack stack, Level level, ResourceKey<Enchantment> key) {
		return holder(level, key).map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack)).orElse(0);
	}

	public static int level(LivingEntity entity, ResourceKey<Enchantment> key) {
		return level(entity.getMainHandItem(), entity.level(), key);
	}

	/** Compatibility shim for old DeferredHolder.get() call sites during port. */
	public static ResourceKey<Enchantment> get(ResourceKey<Enchantment> key) {
		return key;
	}

	public static void register(IEventBus eventBus) {
		// Datapack registry — no deferred code registration on 1.21.
	}
}
