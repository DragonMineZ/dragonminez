package com.dragonminez.common.init.block.custom;

import com.dragonminez.common.init.MainBlockEntities;
import com.dragonminez.common.init.block.entity.KikonoStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class KikonoStationBlock extends BaseEntityBlock {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

	public KikonoStationBlock(Properties pProperties) {
		super(pProperties);
		this.registerDefaultState(this.stateDefinition.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(HALF, DoubleBlockHalf.LOWER));
	}
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
		pBuilder.add(FACING, HALF);
	}

	@Override
	public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
		return SHAPE;
	}

	@Override
	public RenderShape getRenderShape(BlockState pState) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext pContext) {
		BlockPos pos = pContext.getClickedPos();
		Level level = pContext.getLevel();

		if (pos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(pos.above()).canBeReplaced(pContext)) {
			return this.defaultBlockState()
					.setValue(FACING, pContext.getHorizontalDirection().getOpposite())
					.setValue(HALF, DoubleBlockHalf.LOWER);
		}
		return null;
	}

	@Override
	public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
		pLevel.setBlock(pPos.above(), pState.setValue(HALF, DoubleBlockHalf.UPPER), 3);
	}

	@Override
	public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
		DoubleBlockHalf half = pState.getValue(HALF);
		if (pFacing.getAxis() == Direction.Axis.Y && half == DoubleBlockHalf.LOWER == (pFacing == Direction.UP)) {
			return (pFacingState.is(this) && pFacingState.getValue(HALF) != half)
					? pState
					: Blocks.AIR.defaultBlockState();
		}
		if (half == DoubleBlockHalf.LOWER && pFacing == Direction.DOWN && !pState.canSurvive(pLevel, pCurrentPos)) {
			return Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
	}

	@Override
	public void playerWillDestroy(Level pLevel, BlockPos pPos, BlockState pState, Player pPlayer) {
		if (!pLevel.isClientSide) {
			if (pPlayer.isCreative()) {
				preventCreativeDropFromBottomPart(pLevel, pPos, pState, pPlayer);
			}
		}
		super.playerWillDestroy(pLevel, pPos, pState, pPlayer);
	}

	protected static void preventCreativeDropFromBottomPart(Level pLevel, BlockPos pPos, BlockState pState, Player pPlayer) {
		DoubleBlockHalf half = pState.getValue(HALF);
		if (half == DoubleBlockHalf.UPPER) {
			BlockPos belowPos = pPos.below();
			BlockState belowState = pLevel.getBlockState(belowPos);
			if (belowState.is(pState.getBlock()) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER) {
				pLevel.setBlock(belowPos, Blocks.AIR.defaultBlockState(), 35);
				pLevel.levelEvent(pPlayer, 2001, belowPos, Block.getId(belowState));
			}
		}
	}

	@Override
	public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
		if (pState.getBlock() != pNewState.getBlock()) {
			if (pState.getValue(HALF) == DoubleBlockHalf.LOWER) {
				BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
				if (blockEntity instanceof KikonoStationBlockEntity station) {
					station.drops();
				}
			}
		}
		super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
	}

	@Override
	public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
		if (!pLevel.isClientSide()) {
			BlockPos targetPos = pPos;

			if (pState.getValue(HALF) == DoubleBlockHalf.UPPER) {
				targetPos = pPos.below();
			}

			BlockEntity entity = pLevel.getBlockEntity(targetPos);
			if (entity instanceof KikonoStationBlockEntity station) {
				NetworkHooks.openScreen((ServerPlayer) pPlayer, station, targetPos);
			} else {
				throw new IllegalStateException("Container provider missing at " + targetPos);
			}
		}
		return InteractionResult.sidedSuccess(pLevel.isClientSide());
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
		if (pState.getValue(HALF) == DoubleBlockHalf.LOWER) {
			return new KikonoStationBlockEntity(pPos, pState);
		}
		return null;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
		if (pLevel.isClientSide() || pState.getValue(HALF) == DoubleBlockHalf.UPPER) return null;

		return createTickerHelper(pBlockEntityType, MainBlockEntities.KIKONO_STATION_BE.get(),
				(pLevel1, pPos, pState1, pBlockEntity) -> pBlockEntity.tick(pLevel1, pPos, pState1));
	}
}