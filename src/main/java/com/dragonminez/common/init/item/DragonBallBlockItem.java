package com.dragonminez.common.init.item;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public class DragonBallBlockItem extends BlockItem {
	public DragonBallBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public boolean canBeHurtBy(DamageSource source) {
		return false;
	}
}
