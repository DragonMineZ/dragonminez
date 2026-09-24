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
		public static final TagKey<Structure> KI_GRIEFING_PROTECTED = create("ki_griefing_protected");
        public static final TagKey<Structure> BUILD_PROTECTED = create("build_protected");

		private static TagKey<Structure> create(String name) {
			return TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
		}
	}

	public static class EntityTypes {
		public static final TagKey<EntityType<?>> FRIEZA_SOLDIERS = create("frieza_soldiers");
		public static final TagKey<EntityType<?>> SAIBAMEN = create("saibamen");
		public static final TagKey<EntityType<?>> RED_RIBBON_ROBOTS = create("red_ribbon_robots");
		public static final TagKey<EntityType<?>> NPC_SUPERSAIYAN = create("npc_supersaiyan");
		public static final TagKey<EntityType<?>> NPC_SUPERSAIYAN2 = create("npc_supersaiyan2");
		public static final TagKey<EntityType<?>> NPC_SUPERSAIYAN3 = create("npc_supersaiyan3");
		public static final TagKey<EntityType<?>> NPC_SUPERSAIYAN4 = create("npc_supersaiyan4");
		public static final TagKey<EntityType<?>> NPC_GINYUFORCE = create("npc_ginyuforce");
		public static final TagKey<EntityType<?>> NPC_CELLSAGA = create("npc_cellsaga");
		public static final TagKey<EntityType<?>> NPC_SAIYANSAGA = create("npc_saiyansaga");
		public static final TagKey<EntityType<?>> NPC_FRIEZASAGA = create("npc_friezasaga");
		public static final TagKey<EntityType<?>> NPC_FRIEZAFORMS = create("npc_friezaforms");
		public static final TagKey<EntityType<?>> NPC_ANDROIDS = create("npc_androids");
		public static final TagKey<EntityType<?>> NPC_BUUSAGA = create("npc_buusaga");
		public static final TagKey<EntityType<?>> NPC_MAJINBUU = create("npc_majinbuu");
		public static final TagKey<EntityType<?>> NPC_REDRIBBON = create("npc_redribbon");
		public static final TagKey<EntityType<?>> NPC_PILAFGANG = create("npc_pilafgang");
		public static final TagKey<EntityType<?>> NPC_KINGPICCOLO = create("npc_kingpiccolo");
		public static final TagKey<EntityType<?>> NPC_BABAFIGHTERS = create("npc_babafighters");
		public static final TagKey<EntityType<?>> NPC_WORLDTOURNAMENT = create("npc_worldtournament");
		public static final TagKey<EntityType<?>> NPC_ZFIGHTERS = create("npc_zfighters");
		public static final TagKey<EntityType<?>> NPC_COOLERFORCE = create("npc_coolerforce");
		public static final TagKey<EntityType<?>> NPC_BOJACKCREW = create("npc_bojackcrew");
		public static final TagKey<EntityType<?>> NPC_BROLY = create("npc_broly");
		public static final TagKey<EntityType<?>> NPC_MOVIEVILLAINS = create("npc_movievillains");
		public static final TagKey<EntityType<?>> NPC_GTVILLAINS = create("npc_gtvillains");
		public static final TagKey<EntityType<?>> NPC_SHADOWDRAGONS = create("npc_shadowdragons");
		public static final TagKey<EntityType<?>> NPC_FUSIONS = create("npc_fusions");
		public static final TagKey<EntityType<?>> NPC_GIANTS = create("npc_giants");
		public static final TagKey<EntityType<?>> NPC_FINALBOSSES = create("npc_finalbosses");

		private static TagKey<EntityType<?>> create(String name) {
			return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
		}
	}

	public static class Biomes {
		public static final TagKey<Biome> IS_NAMEK = create("is_namekworld"), IS_SACREDLAND = create("is_sacredland"), IS_HTC = create("is_htc"),
		IS_OTHERWORLD = create("is_otherworld"), HAS_DINOSAURS = create("has_dinosaurs"), HAS_SABERTOOTH = create("has_sabertooth"), HAS_ROBOTS = create("has_robots"),
		HAS_SAIBAMANS = create("has_saibamans"), HAS_GIANT_FISH = create("has_giant_fish"), HAS_GIANT_TURTLE = create("has_giant_turtle"),
		IS_ROCKYBIOME = create("is_rockybiome"), IS_SACREDKAI = create("is_sacredkai"), IS_LAND = create("is_land"),
		IS_MOUNTAINLIKE = create("is_mountainlike"), IS_PLAINSLIKE = create("is_plainslike"), IS_DESERTLIKE = create("is_desertlike"),
		IS_GEROLAB = create("is_gerolab"), IS_PLAINS = create("is_plains");

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
		WEIGHTED_ITEMS = create("weighted_items"),
		SENZU_BEANS = create("senzu_beans");

		private static TagKey<Item> create(String name) {
		    return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
		}
	}
}
