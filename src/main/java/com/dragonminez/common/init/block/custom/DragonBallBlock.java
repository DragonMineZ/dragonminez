package com.dragonminez.common.init.block.custom;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.block.entity.DragonBallBlockEntity;
import com.dragonminez.common.init.entities.dragon.DragonWishEntity;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DragonBallBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	@Getter
    private final DragonBallType ballType;
	private final String dragonName;
	@Getter
	private final String ballName;
	private final Integer wishAmount;
	private final String dimensionKey;
	private final Integer ballAmount;

	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	private static VoxelShape calculateShape(Direction to, VoxelShape shape) {
		VoxelShape[] buffer = new VoxelShape[]{shape, Shapes.empty()};
		int times = (to.get2DDataValue() - Direction.NORTH.get2DDataValue() + 4) % 4;
		for (int i = 0; i < times; i++) {
			buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = Shapes.or(buffer[1],
					Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
			buffer[0] = buffer[1];
			buffer[1] = Shapes.empty();
		}
		return buffer[0];
	}

	public DragonBallBlock(Properties properties, DragonBallType ballType, String dragonName, Integer wishAmount, String ballName, String dimensionKey, Integer ballAmount, Double[] shapesBase) {
		super(properties);
		this.ballType = ballType;
		this.dragonName = dragonName;
		this.wishAmount = wishAmount;
		this.ballName = ballName;
		this.dimensionKey = dimensionKey;
		this.ballAmount = ballAmount;

		VoxelShape voxelShape = box(shapesBase[0], shapesBase[1], shapesBase[2], shapesBase[3], shapesBase[4], shapesBase[5]);
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			SHAPES.put(direction, calculateShape(direction, voxelShape));
		}
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new DragonBallBlockEntity(blockPos, blockState, ballType, ballName);
	}

	@Override
	public RenderShape getRenderShape(BlockState pState) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
		Direction direction = pState.getValue(FACING);
		return SHAPES.get(direction);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext pContext) {
		return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockState rotate(BlockState pState, Rotation pRotation) {
		return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState pState, Mirror pMirror) {
		return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
		pBuilder.add(FACING);
	}

	@Override
	public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
		if (pLevel.isClientSide) return InteractionResult.SUCCESS;
		if (areAllDragonBallsNearby(pLevel, pPos) && (pLevel.dimension().location().toString().equals(dimensionKey))) {
			removeAllDragonBalls(pLevel, pPos);
			spawnDragon((ServerLevel) pLevel, pPos, pPlayer);
			return InteractionResult.CONSUME;
		}
		return InteractionResult.PASS;
	}

	private void spawnDragon(ServerLevel serverLevel, BlockPos pPos, Player pPlayer) {
		long currentTime = serverLevel.getDayTime();
		serverLevel.setDayTime(16000);

		if (serverLevel.dimension().location().toString().equalsIgnoreCase(dimensionKey)) {
			DragonWishEntity dragonWishEntity = new DragonWishEntity(MainEntities.DRAGON_ENTITIES.get(dragonName).get(), serverLevel, dragonName, wishAmount);
			dragonWishEntity.setOwnerName(pPlayer.getName().getString());
			dragonWishEntity.setInvokingTime(currentTime);
			dragonWishEntity.setGrantedWish(false);
			dragonWishEntity.moveTo(pPos.getX() + 0.5, pPos.getY(), pPos.getZ() + 0.5, 0.0F, 0.0F);
			serverLevel.addFreshEntity(dragonWishEntity);
		}
		serverLevel.playSound(null, pPos, MainSounds.SHENRON.get(), SoundSource.AMBIENT, 1.0F, 1.0F);
	}

	private boolean areAllDragonBallsNearby(Level world, BlockPos pos) {
		Set<DragonBallType> foundBalls = new HashSet<>();
		for (BlockPos nearbyPos : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
			Block block = world.getBlockState(nearbyPos).getBlock();
			if (block instanceof DragonBallBlock dragonBall
					&& dragonBall.dimensionKey.equalsIgnoreCase(this.dimensionKey)
					&& dragonBall.dragonName.equalsIgnoreCase(this.dragonName)) {
				foundBalls.add(dragonBall.getBallType());
				if (foundBalls.size() == this.ballAmount) return true;
			}
		}
		return false;
	}

	private void removeAllDragonBalls(Level world, BlockPos pos) {
		Set<DragonBallType> removedBalls = new HashSet<>();
		for (BlockPos nearbyPos : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
			Block block = world.getBlockState(nearbyPos).getBlock();
			if (block instanceof DragonBallBlock dragonBall
					&& dragonBall.dimensionKey.equalsIgnoreCase(this.dimensionKey)
					&& dragonBall.dragonName.equalsIgnoreCase(this.dragonName)) {
				if (!removedBalls.contains(dragonBall.getBallType())) {
					world.removeBlock(nearbyPos, false);
					removedBalls.add(dragonBall.getBallType());
				}
				if (removedBalls.size() == 7) break;
			}
		}
	}

	@Override
	public boolean addLandingEffects(BlockState state1, ServerLevel level, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles) {
		return true;
	}

	@Override
	public boolean addRunningEffects(BlockState state, Level level, BlockPos pos, Entity entity) {
		return true;
	}
}

