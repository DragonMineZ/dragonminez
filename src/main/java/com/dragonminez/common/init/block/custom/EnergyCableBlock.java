package com.dragonminez.common.init.block.custom;

import com.dragonminez.common.init.MainBlockEntities;
import com.dragonminez.common.init.block.entity.EnergyCableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.EnumMap;

public class EnergyCableBlock extends BaseEntityBlock {
	public static final BooleanProperty NORTH = BooleanProperty.create("north");
	public static final BooleanProperty EAST = BooleanProperty.create("east");
	public static final BooleanProperty SOUTH = BooleanProperty.create("south");
	public static final BooleanProperty WEST = BooleanProperty.create("west");
	public static final BooleanProperty UP = BooleanProperty.create("up");
	public static final BooleanProperty DOWN = BooleanProperty.create("down");

	public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = new EnumMap<>(Direction.class);
	static {
		PROPERTY_BY_DIRECTION.put(Direction.NORTH, NORTH);
		PROPERTY_BY_DIRECTION.put(Direction.EAST, EAST);
		PROPERTY_BY_DIRECTION.put(Direction.SOUTH, SOUTH);
		PROPERTY_BY_DIRECTION.put(Direction.WEST, WEST);
		PROPERTY_BY_DIRECTION.put(Direction.UP, UP);
		PROPERTY_BY_DIRECTION.put(Direction.DOWN, DOWN);
	}

	private static final VoxelShape SHAPE_CENTER = Block.box(5, 5, 5, 11, 11, 11);
	private static final VoxelShape SHAPE_NORTH = Block.box(5, 5, 0, 11, 11, 5);
	private static final VoxelShape SHAPE_SOUTH = Block.box(5, 5, 11, 11, 11, 16);
	private static final VoxelShape SHAPE_EAST = Block.box(11, 5, 5, 16, 11, 11);
	private static final VoxelShape SHAPE_WEST = Block.box(0, 5, 5, 5, 11, 11);
	private static final VoxelShape SHAPE_UP = Block.box(5, 11, 5, 11, 16, 11);
	private static final VoxelShape SHAPE_DOWN = Block.box(5, 0, 5, 11, 5, 11);

	public EnergyCableBlock(Properties pProperties) {
		super(pProperties);
		this.registerDefaultState(this.stateDefinition.any()
				.setValue(NORTH, false).setValue(EAST, false)
				.setValue(SOUTH, false).setValue(WEST, false)
				.setValue(UP, false).setValue(DOWN, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
		pBuilder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
	}

	@Override
	public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
		VoxelShape shape = SHAPE_CENTER;

		if (pState.getValue(NORTH)) shape = Shapes.or(shape, SHAPE_NORTH);
		if (pState.getValue(SOUTH)) shape = Shapes.or(shape, SHAPE_SOUTH);
		if (pState.getValue(EAST)) shape = Shapes.or(shape, SHAPE_EAST);
		if (pState.getValue(WEST)) shape = Shapes.or(shape, SHAPE_WEST);
		if (pState.getValue(UP)) shape = Shapes.or(shape, SHAPE_UP);
		if (pState.getValue(DOWN)) shape = Shapes.or(shape, SHAPE_DOWN);

		return shape;
	}


	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext pContext) {
		return makeConnections(pContext.getLevel(), pContext.getClickedPos());
	}

	@Override
	public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos) {
		if (pLevel instanceof Level level) {
			return makeConnections(level, pCurrentPos);
		}
		return super.updateShape(pState, pDirection, pNeighborState, pLevel, pCurrentPos, pNeighborPos);
	}

	public BlockState makeConnections(Level level, BlockPos pos) {
		BlockState state = this.defaultBlockState();
		for (Direction dir : Direction.values()) {
			state = state.setValue(PROPERTY_BY_DIRECTION.get(dir), canConnectTo(level, pos.relative(dir), dir.getOpposite()));
		}
		return state;
	}

	private boolean canConnectTo(Level level, BlockPos pos, Direction side) {
		BlockState state = level.getBlockState(pos);
		BlockEntity be = level.getBlockEntity(pos);

		if (state.getBlock() instanceof EnergyCableBlock) {
			return true;
		}

		if (be != null) {
			var key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
			if (key != null && key.getNamespace().equals(com.dragonminez.Reference.MOD_ID)) {
				return be.getCapability(ForgeCapabilities.ENERGY, side).isPresent();
			}
		}

		return false;
	}

	@Override
	public RenderShape getRenderShape(BlockState pState) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
		return new EnergyCableBlockEntity(pPos, pState);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
		if (pLevel.isClientSide()) return null;
		return createTickerHelper(pBlockEntityType, MainBlockEntities.ENERGY_CABLE_BE.get(),
				(pLevel1, pPos, pState1, pBlockEntity) -> pBlockEntity.tick(pLevel1, pPos, pState1));
	}
}