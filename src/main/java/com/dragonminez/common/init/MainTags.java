package com.dragonminez.common.init;

import com.dragonminez.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;

public class MainTags {

	public static class Structures {
		/** Structures whose blocks are protected from ki-attack griefing unless the
		 *  {@code allowKiGriefingMasterStructures} gamerule is enabled. */
		public static final TagKey<Structure> KI_GRIEFING_PROTECTED = create("ki_griefing_protected");

		private static TagKey<Structure> create(String name) {
			return TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
		}
	}

	public static class EntityTypes {
		public static final TagKey<EntityType<?>> FRIEZA_SOLDIERS = create("frieza_soldiers");
		public static final TagKey<EntityType<?>> SAIBAMEN = create("saibamen");
		public static final TagKey<EntityType<?>> RED_RIBBON_ROBOTS = create("red_ribbon_robots");

		private static TagKey<EntityType<?>> create(String name) {
			return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
		}
	}

	public static class Biomes {
		public static final TagKey<Biome> IS_NAMEK = create("is_namekworld"), IS_SACREDLAND = create("is_sacredland"), IS_HTC = create("is_htc"),
		IS_OTHERWORLD = create("is_otherworld"), HAS_DINOSAURS = create("has_dinosaurs"), HAS_SABERTOOTH = create("has_sabertooth"), HAS_ROBOTS = create("has_robots"),
		HAS_SAIBAMANS = create("has_saibamans"),
		IS_ROCKYBIOME = create("is_rockybiome"), IS_SACREDKAI = create("is_sacredkai"), IS_LAND = create("is_land"),
		IS_MOUNTAINLIKE = create("is_mountainlike"), IS_PLAINSLIKE = create("is_plainslike"), IS_DESERTLIKE = create("is_desertlike");

		private static TagKey<Biome> create(String name) {
			return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
		}
	}

	public static class Blocks {
		public static final TagKey<Block> NAMEK_ALOG = create("namek_alog"), NAMEK_SLOG = create("namek_slog"), NAMEKDEEPSLATE_REPLACEABLES = create("namek_deepslate_ore_replaceables"),
		NAMEKSTONE_REPLACEABLES = create("namek_stone_ore_replaceables"), NEEDS_GETE_TOOL = create("needs_gete_tool");

		private static TagKey<Block> create(String name) {
		    return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
		}
	}

	public static class Items {
		public static final TagKey<Item> NAMEK_ALOG = create("namek_alog"), NAMEK_SLOG = create("namek_slog"),
		WEIGHTED_ITEMS = create("weighted_items");

		private static TagKey<Item> create(String name) {
		    return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
		}
	}
}
