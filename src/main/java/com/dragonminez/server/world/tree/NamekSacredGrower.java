package com.dragonminez.server.world.tree;

import com.dragonminez.server.world.feature.NamekConfiguredFeatures;
import net.minecraft.world.level.block.grower.TreeGrower;

import java.util.Optional;

/**
 * Namek Sacred sapling grower (TreeGrower is final in 1.21 — use composition).
 */
public final class NamekSacredGrower {
	public static final TreeGrower INSTANCE = new TreeGrower(
			"namek_sacred",
			Optional.empty(),
			Optional.of(NamekConfiguredFeatures.SACRED_TREE),
			Optional.empty()
	);

	private NamekSacredGrower() {}
}
