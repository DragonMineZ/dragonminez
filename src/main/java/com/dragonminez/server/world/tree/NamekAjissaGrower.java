package com.dragonminez.server.world.tree;

import com.dragonminez.server.world.feature.NamekConfiguredFeatures;
import net.minecraft.world.level.block.grower.TreeGrower;

import java.util.Optional;

/**
 * Namek Ajissa sapling grower (TreeGrower is final in 1.21 — use composition).
 */
public final class NamekAjissaGrower {
	public static final TreeGrower INSTANCE = new TreeGrower(
			"namek_ajissa",
			Optional.empty(),
			Optional.of(NamekConfiguredFeatures.AJISSA_TREE),
			Optional.empty()
	);

	private NamekAjissaGrower() {}
}
