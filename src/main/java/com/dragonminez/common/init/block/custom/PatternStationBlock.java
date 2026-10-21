package com.dragonminez.common.init.block.custom;

import com.dragonminez.common.init.block.entity.PatternStationBlockEntity;
import com.dragonminez.common.init.menu.menutypes.PatternStationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class PatternStationBlock extends BaseEntityBlock {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
	private static final Component CONTAINER_TITLE = Component.translatable("container.dragonminez.pattern_station");

	private static final Map<Direction, VoxelShape> LOWER_SHAPES = rotations(Shapes.or(
			Block.box(0, 0, 0, 16, 13, 16),
			Block.box(0, 13, 0, 16, 15, 16),
			Block.box(0, 15, 0, 8, 16, 16),
			Block.box(12, 15, 0, 14, 16, 2),
			Block.box(12, 15, 14, 14, 16, 16)));
	private static final Map<Direction, VoxelShape> UPPER_SHAPES = rotations(Shapes.or(
			Block.box(1, 2, 0, 5, 6, 16),
			Block.box(0, 0, 0, 6, 2, 2),
			Block.box(0, 0, 14, 6, 2, 16),
			Block.box(6, 0, 0, 10, 2.5, 2),
			Block.box(6, 0, 14, 10, 2.5, 16),
			Block.box(10, 0, 0, 14, 1.5, 2),
			Block.box(10, 0, 14, 14, 1.5, 16)));

	public PatternStationBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(HALF, DoubleBlockHalf.LOWER));
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, HALF);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return (state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER_SHAPES : UPPER_SHAPES).get(state.getValue(FACING));
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		Level level = context.getLevel();
		if (pos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(pos.above()).canBeReplaced(context)) {
			return this.defaultBlockState()
					.setValue(FACING, context.getHorizontalDirection().getOpposite())
					.setValue(HALF, DoubleBlockHalf.LOWER);
		}
		return null;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		DoubleBlockHalf half = state.getValue(HALF);
		if (direction.getAxis() == Direction.Axis.Y && half == DoubleBlockHalf.LOWER == (direction == Direction.UP)) {
			return neighborState.is(this) && neighborState.getValue(HALF) != half ? state : Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
	}

	@Override
	public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide && player.isCreative() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
			BlockPos lowerPos = pos.below();
			BlockState lowerState = level.getBlockState(lowerPos);
			if (lowerState.is(this) && lowerState.getValue(HALF) == DoubleBlockHalf.LOWER) {
				level.setBlock(lowerPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				level.levelEvent(player, LevelEvent.PARTICLES_DESTROY_BLOCK, lowerPos, Block.getId(lowerState));
			}
		}
		super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide) return InteractionResult.SUCCESS;
		player.openMenu(state.getMenuProvider(level, pos));
		return InteractionResult.CONSUME;
	}

	@Override
	public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
		BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
		return new SimpleMenuProvider((containerId, inventory, player) ->
				new PatternStationMenu(containerId, inventory, ContainerLevelAccess.create(level, lowerPos)), CONTAINER_TITLE);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new PatternStationBlockEntity(pos, state) : null;
	}

	private static Map<Direction, VoxelShape> rotations(VoxelShape eastShape) {
		Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
		Direction facing = Direction.EAST;
		VoxelShape shape = eastShape;
		for (int i = 0; i < 4; i++) {
			shapes.put(facing, shape);
			facing = facing.getClockWise();
			shape = rotateClockwise(shape);
		}
		return shapes;
	}

	private static VoxelShape rotateClockwise(VoxelShape shape) {
		VoxelShape[] rotated = {Shapes.empty()};
		shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
				rotated[0] = Shapes.or(rotated[0], Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
		return rotated[0];
	}
}
