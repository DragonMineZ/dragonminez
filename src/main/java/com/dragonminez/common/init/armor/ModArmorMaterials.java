package com.dragonminez.common.init.armor;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainItems;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

/**
 * 1.21 armor materials are registry entries ({@link Holder}&lt;{@link ArmorMaterial}&gt;), not an enum interface.
 */
public final class ModArmorMaterials {
	public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
			DeferredRegister.create(Registries.ARMOR_MATERIAL, Reference.MOD_ID);

	public static final DeferredHolder<ArmorMaterial, ArmorMaterial> BASIC = ARMOR_MATERIALS.register("basic", () -> {
		EnumMap<ArmorItem.Type, Integer> defense = Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
			map.put(ArmorItem.Type.BOOTS, 1);
			map.put(ArmorItem.Type.LEGGINGS, 2);
			map.put(ArmorItem.Type.CHESTPLATE, 3);
			map.put(ArmorItem.Type.HELMET, 1);
			map.put(ArmorItem.Type.BODY, 3);
		});
		return new ArmorMaterial(
				defense,
				10,
				SoundEvents.ARMOR_EQUIP_IRON,
				() -> Ingredient.of(Items.IRON_INGOT),
				List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "basic"))),
				0.0F,
				0.0F
		);
	});

	public static final DeferredHolder<ArmorMaterial, ArmorMaterial> KIKONO = ARMOR_MATERIALS.register("kikono", () -> {
		EnumMap<ArmorItem.Type, Integer> defense = Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
			map.put(ArmorItem.Type.BOOTS, 16);
			map.put(ArmorItem.Type.LEGGINGS, 26);
			map.put(ArmorItem.Type.CHESTPLATE, 35);
			map.put(ArmorItem.Type.HELMET, 2);
			map.put(ArmorItem.Type.BODY, 35);
		});
		return new ArmorMaterial(
				defense,
				25,
				SoundEvents.ARMOR_EQUIP_NETHERITE,
				() -> Ingredient.of(MainItems.KIKONO_SHARD.get()),
				List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "kikono"))),
				5.0F,
				0.1F
		);
	});

	private ModArmorMaterials() {}
}
