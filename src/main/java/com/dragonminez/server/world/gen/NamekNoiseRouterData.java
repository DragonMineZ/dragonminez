package com.dragonminez.server.world.gen;

import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class NamekNoiseRouterData extends NoiseRouterData {

	public static NoiseRouter createNamekRouter(HolderGetter<DensityFunction> density, HolderGetter<NormalNoise.NoiseParameters> noise) {
		return overworld(density, noise, false, false);
	}
}