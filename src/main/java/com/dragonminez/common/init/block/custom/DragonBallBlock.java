package com.dragonminez.common.init.block.custom;

import com.dragonminez.common.init.block.entity.DragonBallBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class DragonBallBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final DragonBallType ballType;
    private final boolean isNamekian;

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
        return switch (pState.getValue(FACING).getAxis()) {
            case X -> box((16 - 7.5) / 2.0, 0.0, (16 - 7.5) / 2.0, (16 + 7.5) / 2.0, 7.0, (16 + 7.5) / 2.0);
            default -> box((16 - 8) / 2.0, 0.0, (16 - 8) / 2.0, (16 + 8) / 2.0, 7.0, (16 + 8) / 2.0);
        };
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
        if (pLevel.dimension() == Level.OVERWORLD && !isNamekian) {
            if (areAllDragonBallsNearby(pLevel, pPos)) {
                removeAllDragonBalls(pLevel, pPos);

                if (!pLevel.isClientSide) {
                    spawnShenlong(pLevel, pPos);
                }

                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    private void spawnShenlong(Level pLevel, BlockPos pPos) {
        ServerLevel serverLevel = (ServerLevel) pLevel;
        serverLevel.setDayTime(16000);
    }

    private boolean areAllDragonBallsNearby(Level world, BlockPos pos) {
        Set<DragonBallType> foundBalls = new HashSet<>();

        for (BlockPos nearbyPos : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
            Block block = world.getBlockState(nearbyPos).getBlock();

            if (block instanceof DragonBallBlock dragonBall && dragonBall.isNamekian() == this.isNamekian) {
                foundBalls.add(dragonBall.getBallType());

                if (foundBalls.size() == 7) {
                    return true;
                }
            }
        }

        return false;
    }

    private void removeAllDragonBalls(Level world, BlockPos pos) {
        for (BlockPos nearbyPos : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
            Block block = world.getBlockState(nearbyPos).getBlock();

            if (block instanceof DragonBallBlock dragonBall && dragonBall.isNamekian() == this.isNamekian) {
                world.destroyBlock(nearbyPos, false);
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

