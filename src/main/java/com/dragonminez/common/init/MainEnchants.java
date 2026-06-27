package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.weapons.WeaponItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MainEnchants {
	public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, Reference.MOD_ID);

	private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[] {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	public static final RegistryObject<Enchantment> VITALITY_RECOVERY = ENCHANTMENTS.register("vitality_recovery",
			() -> new RecoveryEnchantment(Enchantment.Rarity.UNCOMMON, ARMOR_SLOTS, "vitality"));

	public static final RegistryObject<Enchantment> RESISTANCE_RECOVERY = ENCHANTMENTS.register("resistance_recovery",
			() -> new RecoveryEnchantment(Enchantment.Rarity.UNCOMMON, ARMOR_SLOTS, "resistance"));

	public static final RegistryObject<Enchantment> ENERGY_RECOVERY = ENCHANTMENTS.register("energy_recovery",
			() -> new RecoveryEnchantment(Enchantment.Rarity.UNCOMMON, ARMOR_SLOTS, "energy"));

	public static final RegistryObject<Enchantment> DEFENSE_PENETRATION = ENCHANTMENTS.register("defense_penetration",
			() -> new WeaponPenetrationEnchantment(Enchantment.Rarity.RARE, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));

	public static final RegistryObject<Enchantment> HEALING_REDUCTION = ENCHANTMENTS.register("healing_reduction",
			() -> new HealingReductionEnchantment(Enchantment.Rarity.RARE, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));

	public static final RegistryObject<Enchantment> CRIT_CHANCE = ENCHANTMENTS.register("critical_chance",
			() -> new CriticalStatEnchantment(Enchantment.Rarity.UNCOMMON, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));

	public static final RegistryObject<Enchantment> CRIT_DAMAGE = ENCHANTMENTS.register("critical_damage",
			() -> new CriticalStatEnchantment(Enchantment.Rarity.UNCOMMON, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));

	// --- Bulma's Gete-tech "special" armor enchantments ---
	// Quest-only (not discoverable): granted by bulma_gete_enhancements as enchanted books.
	public static final RegistryObject<Enchantment> KI_CONDUCTIVITY = ENCHANTMENTS.register("ki_conductivity",
			() -> new GeteArmorEnchantment(Enchantment.Rarity.RARE, 4));

	public static final RegistryObject<Enchantment> GRAVITY_FORGED = ENCHANTMENTS.register("gravity_forged",
			() -> new GeteArmorEnchantment(Enchantment.Rarity.RARE, 4));


	public static class WeaponPenetrationEnchantment extends Enchantment {
		protected WeaponPenetrationEnchantment(Rarity rarity, EquipmentSlot... slots) {
			super(rarity, EnchantmentCategory.WEAPON, slots);
		}

		@Override
		public int getMaxLevel() {
			return 5;
		}

		@Override
		public boolean canEnchant(ItemStack stack) {
			return stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem || stack.getItem() instanceof WeaponItem;
		}
	}

	public static class HealingReductionEnchantment extends Enchantment {
		protected HealingReductionEnchantment(Rarity rarity, EquipmentSlot... slots) {
			super(rarity, EnchantmentCategory.WEAPON, slots);
		}

		@Override
		public int getMaxLevel() {
			return 4;
		}

		@Override
		public boolean canEnchant(ItemStack stack) {
			return stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem || stack.getItem() instanceof WeaponItem;
		}
	}

	public static class CriticalStatEnchantment extends Enchantment {
		public CriticalStatEnchantment(Rarity rarity, EquipmentSlot... slots) {
			super(rarity, EnchantmentCategory.WEAPON, slots);
		}

		@Override
		public int getMaxLevel() {
			return 5;
		}

		@Override
		public boolean canEnchant(ItemStack stack) {
			return stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem || stack.getItem() instanceof WeaponItem;
		}

		@Override
		protected boolean checkCompatibility(Enchantment other) {
			return super.checkCompatibility(other) && !(other instanceof CriticalStatEnchantment);
		}
	}

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

	public static class GeteArmorEnchantment extends Enchantment {
		private final int maxLevel;

		protected GeteArmorEnchantment(Rarity rarity, int maxLevel) {
			super(rarity, EnchantmentCategory.ARMOR, ARMOR_SLOTS);
			this.maxLevel = maxLevel;
		}

		@Override
		public int getMaxLevel() {
			return maxLevel;
		}

		@Override
		public boolean isDiscoverable() {
			return false;
		}

		@Override
		public boolean isTradeable() {
			return false;
		}
	}

	public static void register(IEventBus eventBus) { ENCHANTMENTS.register(eventBus); }
}