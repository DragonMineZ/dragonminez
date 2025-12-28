package com.dragonminez.common.init.block.custom;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.block.entity.DragonBallBlockEntity;
import com.dragonminez.common.init.entities.PorungaEntity;
import com.dragonminez.common.init.entities.ShenronEntity;
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
    private final DragonBallType ballType;
    private final boolean isNamekian;

	private static final Map<Direction, VoxelShape> EARTH_SHAPES = new EnumMap<>(Direction.class);
	private static final Map<Direction, VoxelShape> NAMEK_SHAPES = new EnumMap<>(Direction.class);

	static {
		VoxelShape earthBase = box(4.0D, 0.0D, 4.0D, 12.0D, 7.0D, 12.0D);
		VoxelShape namekBase = box(-4.0D, 0.0D, -4.0D, 20.0D, 21.0D, 20.0D);
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			EARTH_SHAPES.put(direction, calculateShape(direction, earthBase));
			NAMEK_SHAPES.put(direction, calculateShape(direction, namekBase));
		}
	}

	private static VoxelShape calculateShape(Direction to, VoxelShape shape) {
		VoxelShape[] buffer = new VoxelShape[]{ shape, Shapes.empty() };
		int times = (to.get2DDataValue() - Direction.NORTH.get2DDataValue() + 4) % 4;
		for (int i = 0; i < times; i++) {
			buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = Shapes.or(buffer[1],
					Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
			buffer[0] = buffer[1];
			buffer[1] = Shapes.empty();
		}
		return buffer[0];
	}
    public DragonBallBlock(Properties properties, DragonBallType ballType, boolean isNamekian) {
        super(properties);
        this.ballType = ballType;
        this.isNamekian = isNamekian;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public DragonBallType getBallType() {
        return ballType;
    }

    public boolean isNamekian() {
        return isNamekian;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new DragonBallBlockEntity(blockPos, blockState, ballType, isNamekian);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
	public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
		Direction direction = pState.getValue(FACING);
		return isNamekian ? NAMEK_SHAPES.get(direction) : EARTH_SHAPES.get(direction);
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
		if (areAllDragonBallsNearby(pLevel, pPos)) {
			removeAllDragonBalls(pLevel, pPos);
			spawnDragon((ServerLevel) pLevel, pPos, pPlayer);
			return InteractionResult.CONSUME;
		}
		return InteractionResult.PASS;
	}

	private void spawnDragon(ServerLevel serverLevel, BlockPos pPos, Player pPlayer) {
		long currentTime = serverLevel.getDayTime();
		serverLevel.setDayTime(16000);

		if (isNamekian) {
			PorungaEntity porunga = new PorungaEntity(MainEntities.PORUNGA.get(), serverLevel);
			porunga.setOwnerName(pPlayer.getName().getString());
			porunga.setInvokingTime(currentTime);
			porunga.setGrantedWish(false);
			porunga.moveTo(pPos.getX() + 0.5, pPos.getY(), pPos.getZ() + 0.5, 0.0F, 0.0F);
			serverLevel.addFreshEntity(porunga);
		} else {
			ShenronEntity Shenron = new ShenronEntity(MainEntities.Shenron.get(), serverLevel);
			Shenron.setOwnerName(pPlayer.getName().getString());
			Shenron.setInvokingTime(currentTime);
			Shenron.setGrantedWish(false);
			Shenron.moveTo(pPos.getX() + 0.5, pPos.getY(), pPos.getZ() + 0.5, 0.0F, 0.0F);
			serverLevel.addFreshEntity(Shenron);
		}
		serverLevel.playSound(null, pPos, MainSounds.SHENRON.get(), SoundSource.AMBIENT, 1.0F, 1.0F);
	}

	private boolean areAllDragonBallsNearby(Level world, BlockPos pos) {
		Set<DragonBallType> foundBalls = new HashSet<>();
		for (BlockPos nearbyPos : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
			Block block = world.getBlockState(nearbyPos).getBlock();
			if (block instanceof DragonBallBlock dragonBall && dragonBall.isNamekian() == this.isNamekian) {
				foundBalls.add(dragonBall.getBallType());
				if (foundBalls.size() == 7) return true;
			}
		}
		return false;
	}

	private void removeAllDragonBalls(Level world, BlockPos pos) {
		for (BlockPos nearbyPos : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
			Block block = world.getBlockState(nearbyPos).getBlock();
			if (block instanceof DragonBallBlock dragonBall && dragonBall.isNamekian() == this.isNamekian) {
				world.removeBlock(nearbyPos, false);
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

