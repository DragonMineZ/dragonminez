package com.dragonminez.common.init.item.farming;

import com.dragonminez.common.init.MainBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Musgo de Namek: reactivo base de la agricultura (crafteo + alquimia). Además puede plantarse,
 * pero <b>solo sobre el bloque de hierba de Namek</b> ({@code namek_grass_block}); al madurar el
 * cultivo suelta más musgo, haciéndolo renovable en Namek.
 */
public class NamekMossItem extends Item {

	public NamekMossItem() {
		super(new Properties().stacksTo(64));
	}

	@Override
	public @NotNull InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos clicked = context.getClickedPos();
		BlockPos plantPos = clicked.above();

		if (level.getBlockState(clicked).is(MainBlocks.NAMEK_GRASS_BLOCK.get()) && level.isEmptyBlock(plantPos)) {
			if (!level.isClientSide) {
				level.setBlock(plantPos, MainBlocks.NAMEK_MOSS_CROP.get().defaultBlockState(), 3);
			}
			Player player = context.getPlayer();
			if (player == null || !player.getAbilities().instabuild) {
				context.getItemInHand().shrink(1);
			}
			return InteractionResult.sidedSuccess(level.isClientSide());
		}
		return InteractionResult.PASS;
	}
}
