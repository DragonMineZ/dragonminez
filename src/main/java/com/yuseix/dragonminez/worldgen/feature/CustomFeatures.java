package com.yuseix.dragonminez.worldgen.feature;

import com.yuseix.dragonminez.DragonMineZ;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CustomFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES_REGISTER
            = DeferredRegister.create(ForgeRegistries.FEATURES, DragonMineZ.MOD_ID);

    /*public static final RegistryObject<Feature<NoneFeatureConfiguration>> GETE_STAR
            = FEATURES_REGISTER.register("gete_star", () -> new GeteStarFeature(NoneFeatureConfiguration.CODEC));*/
}
