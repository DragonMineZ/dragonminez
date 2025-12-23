package com.dragonminez.common.init;

import com.dragonminez.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

public class MainTags {
	public static class Biomes {
		public static final TagKey<Biome> IS_NAMEK = create("is_namekworld"), IS_SACREDLAND = create("is_sacredland"), IS_HTC = create("is_htc"),
		IS_OTHERWORLD = create("is_otherworld");

		private static TagKey<Biome> create(String name) {
			return TagKey.create(Registries.BIOME, new ResourceLocation(Reference.MOD_ID, name));
		}
	}

	public static class Blocks {
		public static final TagKey<Block> NAMEK_ALOG = create("namek_alog"), NAMEK_SLOG = create("namek_slog"), NAMEKDEEPSLATE_REPLACEABLES = create("namek_deepslate_ore_replaceables"),
		NAMEKSTONE_REPLACEABLES = create("namek_stone_ore_replaceables");

		private static TagKey<Block> create(String name) {
		    return TagKey.create(Registries.BLOCK, new ResourceLocation(Reference.MOD_ID, name));
		}
	}

	public static class Items {
		public static final TagKey<Item> NAMEK_ALOG = create("namek_alog"), NAMEK_SLOG = create("namek_slog");

		private static TagKey<Item> create(String name) {
		    return TagKey.create(Registries.ITEM, new ResourceLocation(Reference.MOD_ID, name));
		}
	}
}
