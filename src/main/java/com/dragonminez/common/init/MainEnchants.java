package com.dragonminez.common.init;

import com.dragonminez.Reference;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MainEnchants {
	public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, Reference.MOD_ID);

	public static final RegistryObject<Enchantment> VITALITY_RECOVERY = ENCHANTMENTS.register("vitality_recovery",
			() -> new RecoveryEnchantment(Enchantment.Rarity.UNCOMMON, EquipmentSlot.values(), "vitality"));

	public static final RegistryObject<Enchantment> RESISTANCE_RECOVERY = ENCHANTMENTS.register("resistance_recovery",
			() -> new RecoveryEnchantment(Enchantment.Rarity.UNCOMMON, EquipmentSlot.values(), "resistance"));

	public static final RegistryObject<Enchantment> ENERGY_RECOVERY = ENCHANTMENTS.register("energy_recovery",
			() -> new RecoveryEnchantment(Enchantment.Rarity.UNCOMMON, EquipmentSlot.values(), "energy"));

	public static class RecoveryEnchantment extends Enchantment {
		private final String type;

		protected RecoveryEnchantment(Rarity rarity, EquipmentSlot[] slots, String type) {
			super(rarity, EnchantmentCategory.ARMOR, slots);
			this.type = type;
		}

		@Override
		public int getMaxLevel() {
			return 4;
		}

		@Override
		protected boolean checkCompatibility(Enchantment other) {
			if (other instanceof RecoveryEnchantment) return false;
			return super.checkCompatibility(other);
		}
	}

	public static void register(IEventBus eventBus) { ENCHANTMENTS.register(eventBus); }
}