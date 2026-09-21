package com.dragonminez.common.init.block.custom;

import com.dragonminez.common.init.MainEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class OtherworldCloudBlock extends Block {
	// Multiplicador por tick de la velocidad de caída dentro de la nube (la telaraña equivale a ~0.05).
	private static final double FALL_DRAG = 0.85D;

	public OtherworldCloudBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		// entityInside se llama una vez por cada bloque que toca la hitbox: solo el bloque de los pies aplica el freno.
		if (!(entity instanceof LivingEntity living) || !pos.equals(entity.blockPosition())) return;
		if (living.isFallFlying() || living.hasEffect(MainEffects.FLY.get())) return;
		if (living instanceof Player player && player.getAbilities().flying) return;

		Vec3 motion = living.getDeltaMovement();
		if (motion.y < 0.0D) living.setDeltaMovement(motion.x, motion.y * FALL_DRAG, motion.z);
		living.resetFallDistance();
	}

	@Override
	public boolean skipRendering(BlockState state, BlockState adjacentState, Direction side) {
		return adjacentState.is(this) || super.skipRendering(state, adjacentState, side);
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
		return 0;
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return true;
	}
}
