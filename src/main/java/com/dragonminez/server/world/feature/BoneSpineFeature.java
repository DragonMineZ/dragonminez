package com.dragonminez.server.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class BoneSpineFeature extends Feature<NoneFeatureConfiguration> {

	public BoneSpineFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		RandomSource random = context.random();

		BlockPos ground = FeatureUtil.findHellSurface(level, context.origin());
		if (ground == null) return false;
		if (FeatureUtil.isInsideDmzStructure(level, ground)) return false;

		double angle = random.nextDouble() * Math.PI * 2.0D;
		double spread = 1.0D / Math.max(Math.abs(Math.cos(angle)), Math.abs(Math.sin(angle)));
		double stepX = Math.cos(angle) * spread;
		double stepZ = Math.sin(angle) * spread;

		int segments = 11 + random.nextInt(11);
		float arch = 5.0F + random.nextFloat() * 5.0F;
		Direction.Axis spineAxis = Math.abs(stepX) >= Math.abs(stepZ) ? Direction.Axis.X : Direction.Axis.Z;
		Direction.Axis ribAxis = spineAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;

		BlockPos previous = null;
		boolean placed = false;

		for (int i = 0; i < segments; i++) {
			float progress = (float) i / (float) (segments - 1);
			int x = ground.getX() + Mth.floor(stepX * i);
			int z = ground.getZ() + Mth.floor(stepZ * i);

			BlockPos column = FeatureUtil.findHellSurface(level, new BlockPos(x, ground.getY() + Mth.ceil(arch) + 10, z));
			if (column == null) continue;

			int lift = Mth.floor(Mth.sin(progress * (float) Math.PI) * arch);
			BlockPos vertebra = new BlockPos(x, column.getY() + 1 + lift, z);

			if (previous != null) {
				int gap = vertebra.getY() - previous.getY();
				int direction = gap >= 0 ? 1 : -1;
				for (int step = 1; step < Math.abs(gap); step++) {
					placed |= placeBone(level, previous.offset(0, step * direction, 0), Direction.Axis.Y);
				}
			}

			Direction.Axis axis = previous != null && Math.abs(vertebra.getY() - previous.getY()) >= 1 ? Direction.Axis.Y : spineAxis;
			placed |= placeBone(level, vertebra, axis);
			previous = vertebra;

			if (lift >= 3 && i % 2 == 0 && progress > 0.12F && progress < 0.88F) {
				int ribLength = 2 + Mth.floor(Mth.sin(progress * (float) Math.PI) * (2.0F + random.nextFloat() * 2.0F));
				placed |= placeRib(level, vertebra, ribAxis, ribLength, 1);
				placed |= placeRib(level, vertebra, ribAxis, ribLength, -1);
			}
		}

		if (placed && random.nextInt(3) != 0) {
			placeSkull(level, ground);
		}
		return placed;
	}

	private boolean placeRib(WorldGenLevel level, BlockPos vertebra, Direction.Axis axis, int length, int side) {
		boolean placed = false;
		BlockPos.MutableBlockPos cursor = vertebra.mutable();

		for (int i = 1; i <= length; i++) {
			int offset = i * side;
			int drop = i <= 1 ? 0 : i - 1;
			cursor.set(
					axis == Direction.Axis.X ? vertebra.getX() + offset : vertebra.getX(),
					vertebra.getY() - drop,
					axis == Direction.Axis.Z ? vertebra.getZ() + offset : vertebra.getZ()
			);
			placed |= placeBone(level, cursor, i >= length && length > 2 ? Direction.Axis.Y : axis);
		}
		return placed;
	}

	private void placeSkull(WorldGenLevel level, BlockPos ground) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		BlockPos center = ground.above(2);

		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					if (Math.abs(x) + Math.abs(y) + Math.abs(z) > 2) continue;
					cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
					placeBone(level, cursor, Direction.Axis.Y);
				}
			}
		}
	}

	private boolean placeBone(WorldGenLevel level, BlockPos pos, Direction.Axis axis) {
		if (pos.getY() <= level.getMinBuildHeight() + 1 || pos.getY() >= level.getMaxBuildHeight() - 1) return false;
		BlockState state = level.getBlockState(pos);
		if (!state.isAir() && !state.canBeReplaced() && !FeatureUtil.isHellGround(state)) return false;
		this.setBlock(level, pos, Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
		return true;
	}
}
