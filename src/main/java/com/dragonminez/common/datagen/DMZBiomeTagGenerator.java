package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainTags;
import com.dragonminez.server.world.biome.HTCBiomes;
import com.dragonminez.server.world.biome.NamekBiomes;
import com.dragonminez.server.world.biome.OtherworldBiomes;
import com.dragonminez.server.world.biome.OverworldBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
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


	}
}
