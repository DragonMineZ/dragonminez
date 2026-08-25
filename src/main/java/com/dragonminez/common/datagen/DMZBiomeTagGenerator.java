package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainTags;
import com.dragonminez.server.world.biome.HTCBiomes;
import com.dragonminez.server.world.biome.NamekBiomes;
import com.dragonminez.server.world.biome.OtherworldBiomes;
import com.dragonminez.server.world.biome.OverworldBiomes;
import com.dragonminez.server.world.biome.SacredKaiBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class DMZBiomeTagGenerator extends BiomeTagsProvider {
	public DMZBiomeTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> pProvider, @Nullable ExistingFileHelper existingFileHelper) {
		super(output, pProvider, Reference.MOD_ID, existingFileHelper);
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {
		this.tag(MainTags.Biomes.IS_NAMEK)
				.replace(false)
				.add(NamekBiomes.AJISSA_PLAINS)
				.add(NamekBiomes.SACRED_LAND)
				.add(NamekBiomes.NAMEKIAN_RIVERS);

		this.tag(MainTags.Biomes.IS_SACREDLAND)
				.replace(false)
				.add(NamekBiomes.SACRED_LAND);

		this.tag(MainTags.Biomes.IS_HTC)
				.replace(false)
				.add(HTCBiomes.TIME_CHAMBER);

		this.tag(MainTags.Biomes.IS_OTHERWORLD)
				.replace(false)
				.add(OtherworldBiomes.OTHERWORLD);

		this.tag(MainTags.Biomes.HAS_DINOSAURS)
				.replace(false)
				.addTag(BiomeTags.HAS_VILLAGE_SAVANNA)
				.addTag(BiomeTags.IS_BADLANDS)
				.addTag(BiomeTags.IS_MOUNTAIN)
				.addTag(BiomeTags.IS_HILL)
				.add(OverworldBiomes.ROCKY);

		this.tag(MainTags.Biomes.HAS_SABERTOOTH)
				.replace(false)
				.addTag(BiomeTags.HAS_VILLAGE_SAVANNA)
				.addTag(BiomeTags.IS_SAVANNA)
				.addTag(BiomeTags.HAS_VILLAGE_PLAINS)
				.addTag(BiomeTags.IS_FOREST)
				.addTag(BiomeTags.HAS_WOODLAND_MANSION)
				.addTag(BiomeTags.IS_JUNGLE);

		this.tag(MainTags.Biomes.HAS_ROBOTS)
				.replace(false)
				.addTag(BiomeTags.HAS_VILLAGE_SAVANNA)
				.addTag(BiomeTags.HAS_VILLAGE_PLAINS)
				.addTag(BiomeTags.HAS_VILLAGE_SNOWY);

		this.tag(MainTags.Biomes.IS_ROCKYBIOME)
				.replace(false)
				.add(OverworldBiomes.ROCKY);

		this.tag(MainTags.Biomes.HAS_SAIBAMANS)
				.replace(false)
				.add(OverworldBiomes.ROCKY);

		this.tag(MainTags.Biomes.IS_SACREDKAI)
				.replace(false)
				.add(SacredKaiBiomes.SACREDKAI_PLAINS)
				.add(SacredKaiBiomes.SACREDKAI_HILLS)
				.add(SacredKaiBiomes.SACREDKAI_RIVERS);

		this.tag(MainTags.Biomes.IS_LAND)
				.replace(false)
				.addTag(BiomeTags.HAS_VILLAGE_PLAINS)
				.addTag(BiomeTags.HAS_VILLAGE_DESERT)
				.addTag(BiomeTags.HAS_VILLAGE_SAVANNA)
				.addTag(BiomeTags.HAS_VILLAGE_SNOWY)
				.addTag(BiomeTags.HAS_VILLAGE_TAIGA)
				.addTag(BiomeTags.IS_FOREST)
				.addTag(BiomeTags.IS_JUNGLE)
				.addTag(BiomeTags.IS_BADLANDS)
				.addTag(BiomeTags.IS_TAIGA)
				.addTag(BiomeTags.IS_HILL)
				.addTag(BiomeTags.IS_MOUNTAIN)
				.add(OverworldBiomes.ROCKY)
				.addOptionalTag(forge("is_plains"))
				.addOptionalTag(forge("is_desert"))
				.addOptionalTag(forge("is_sandy"))
				.addOptionalTag(forge("is_snowy"))
				.addOptionalTag(forge("is_swamp"))
				.addOptionalTag(forge("is_mountain"))
				.addOptionalTag(forge("is_plateau"))
				.addOptionalTag(forge("is_slope"))
				.addOptionalTag(forge("is_peak"))
				.addOptionalTag(forge("is_lush"))
				.addOptionalTag(forge("is_coniferous"));

		this.tag(MainTags.Biomes.IS_MOUNTAINLIKE)
				.replace(false)
				.addTag(BiomeTags.IS_MOUNTAIN)
				.addOptionalTag(forge("is_mountain"));

		this.tag(MainTags.Biomes.IS_PLAINSLIKE)
				.replace(false)
				.addTag(BiomeTags.HAS_VILLAGE_PLAINS)
				.addOptionalTag(forge("is_plains"));

		this.tag(MainTags.Biomes.IS_DESERTLIKE)
				.replace(false)
				.addTag(BiomeTags.HAS_VILLAGE_DESERT)
				.addOptionalTag(forge("is_desert"));
	}

	private static ResourceLocation forge(String path) {
		return ResourceLocation.fromNamespaceAndPath("forge", path);
	}
}
