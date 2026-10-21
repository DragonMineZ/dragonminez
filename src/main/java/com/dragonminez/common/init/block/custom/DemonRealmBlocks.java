package com.dragonminez.common.init.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.SandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public final class DemonRealmBlocks {
	public static final BooleanProperty NATURAL = BooleanProperty.create("natural");

	private DemonRealmBlocks() {}

	public static BlockState natural(Block block) {
		return block.defaultBlockState().setValue(NATURAL, true);
	}

	public static class SkylitBlock extends Block {
		public SkylitBlock(Properties properties) {
			super(properties);
			this.registerDefaultState(this.stateDefinition.any().setValue(NATURAL, false));
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			builder.add(NATURAL);
		}

		@Override
		public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
			return state.getValue(NATURAL) ? 0 : super.getLightBlock(state, level, pos);
		}

		@Override
		public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
			return state.getValue(NATURAL) || super.propagatesSkylightDown(state, level, pos);
		}
	}

	public static class SkylitSoilBlock extends SkylitBlock {
		public SkylitSoilBlock(Properties properties) {
			super(properties);
		}

		@Override
		public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context, ToolAction toolAction, boolean simulate) {
			if (toolAction == ToolActions.HOE_TILL && context.getItemInHand().canPerformAction(toolAction)
					&& context.getClickedFace() != Direction.DOWN
					&& context.getLevel().getBlockState(context.getClickedPos().above()).isAir()) {
				return Blocks.FARMLAND.defaultBlockState();
			}
			return super.getToolModifiedState(state, context, toolAction, simulate);
		}
	}

	public static class SkylitSandBlock extends SandBlock {
		public SkylitSandBlock(int dustColor, Properties properties) {
			super(dustColor, properties);
			this.registerDefaultState(this.stateDefinition.any().setValue(NATURAL, false));
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			builder.add(NATURAL);
		}

		@Override
		public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
			return state.getValue(NATURAL) ? 0 : super.getLightBlock(state, level, pos);
		}

		@Override
		public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
			return state.getValue(NATURAL) || super.propagatesSkylightDown(state, level, pos);
		}
	}

	public static class DarkSeaCloudBlock extends Block {
		public DarkSeaCloudBlock(Properties properties) {
			super(properties);
		}

		@Override
		public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
			if (level.isClientSide || !(entity instanceof LivingEntity living) || !pos.equals(entity.blockPosition())) return;
			living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false));
			living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
			living.hurt(level.damageSources().magic(), Math.max(4.0F, living.getMaxHealth() * 0.25F));
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

	public static class GasVentBlock extends Block {
		private static final DustParticleOptions GAS = new DustParticleOptions(new Vector3f(0.78F, 0.6F, 0.82F), 2.2F);

		public GasVentBlock(Properties properties) {
			super(properties);
		}

		@Override
		public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
			return 0;
		}

		@Override
		public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
			return true;
		}

		@Override
		public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
			if (!level.getBlockState(pos.above()).isAir()) return;
			double x = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.3D;
			double y = pos.getY() + 1.02D;
			double z = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.3D;
			if (random.nextInt(2) == 0) {
				level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, x, y, z, 0.0D, 0.06D, 0.0D);
			}
			level.addParticle(GAS, x, y, z, 0.0D, 0.12D, 0.0D);
			if (random.nextInt(8) == 0) {
				level.addParticle(ParticleTypes.WHITE_ASH, x, y + 0.4D, z, 0.0D, 0.0D, 0.0D);
			}
		}
	}

	public static class MakaiShrubBlock extends BushBlock {
		private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);

		public MakaiShrubBlock(Properties properties) {
			super(properties);
		}

		@Override
		protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
			return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND);
		}

		@Override
		public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
			return SHAPE;
		}
	}

	public static class LightShieldBlock extends HalfTransparentBlock {
		private static final DustParticleOptions SPARK = new DustParticleOptions(new Vector3f(1.0F, 0.62F, 0.2F), 1.4F);

		public LightShieldBlock(Properties properties) {
			super(properties);
		}

		@Override
		public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
			if (random.nextInt(18) != 0) return;
			double x = pos.getX() + random.nextDouble();
			double z = pos.getZ() + random.nextDouble();
			double drift = random.nextBoolean() ? 0.05D : -0.05D;
			level.addParticle(SPARK, x, pos.getY() + 0.5D, z, 0.0D, drift, 0.0D);
		}

		@Override
		public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
			return Shapes.empty();
		}

		@Override
		public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
			return 1.0F;
		}

		@Override
		public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
			return true;
		}
	}
}
