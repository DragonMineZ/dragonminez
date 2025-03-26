package com.yuseix.dragonminez.common.init.blocks.custom.dballs;

import com.google.common.collect.ImmutableMap;
import com.yuseix.dragonminez.common.config.old.DMZGeneralConfig;
import com.yuseix.dragonminez.common.init.MainEntity;
import com.yuseix.dragonminez.common.init.MainSounds;
import com.yuseix.dragonminez.common.init.blocks.entity.Dball3BlockEntity;
import com.yuseix.dragonminez.common.init.entity.custom.ShenlongEntity;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class Dball3Block extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public Dball3Block(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {

        switch (((Direction) pState.getValue(FACING)).getAxis()) {
            case X:
                // Ajusta las coordenadas X para centrarlo en el eje X
                return Dball3Block.box((16 - 7.5) / 2.0, 0.0, (16 - 7.5) / 2.0, (16 + 7.5) / 2.0, 7.0, (16 + 7.5) / 2.0);
            case Z:
            default:
                // Ajusta las coordenadas Z para centrarlo en el eje Z
                return Dball3Block.box((16 - 8) / 2.0, 0.0, (16 - 8) / 2.0, (16 + 8) / 2.0, 7.0, (16 + 8) / 2.0);
        }

    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new Dball3BlockEntity(blockPos, blockState);
    }

    @Override
    protected ImmutableMap<BlockState, VoxelShape> getShapeForEachState(Function<BlockState, VoxelShape> pShapeGetter) {
        return super.getShapeForEachState(pShapeGetter);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
    /* FACING */

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
        if (pLevel.dimension() == Level.OVERWORLD) {
            if (areAllDballBlocksNearby(pLevel, pPos) && DMZGeneralConfig.SHOULD_DRAGON_SPAWN.get()) {
                // Elimina los bloques
                removeAllDballBlocks(pLevel, pPos);

                if (!pLevel.isClientSide) {
                    ServerLevel serverLevel = (ServerLevel) pLevel;
                    long currentTime = pLevel.getDayTime();
                    serverLevel.setDayTime(16000);

                    ShenlongEntity dragonEntity = new ShenlongEntity(MainEntity.SHENLONG.get(),pLevel);
                    dragonEntity.setOwnerName(pPlayer.getName().getString());
                    dragonEntity.setInvokingTime(currentTime);
                    dragonEntity.setDeseos(1);
                    dragonEntity.moveTo(pPos.getX() + 0.5, pPos.getY(), pPos.getZ() + 0.5, 0.0F, 0.0F);
                    pLevel.addFreshEntity(dragonEntity);
                    pLevel.playSound(null, pPos, MainSounds.SHENRON.get(), SoundSource.AMBIENT, 1.0F, 1.0F);
                }

                return InteractionResult.SUCCESS;
            }
        }

        // Si no están todos los bloques o no está en el Overworld, no hace nada
        return InteractionResult.PASS;
    }

    private boolean areAllDballBlocksNearby(Level world, BlockPos pos) {
        boolean foundDball1 = false;
        boolean foundDball2 = false;
        boolean foundDball3 = false;
        boolean foundDball4 = false;
        boolean foundDball5 = false;
        boolean foundDball6 = false;
        boolean foundDball7 = false;

        for (BlockPos nearbyPos : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
            Block block = world.getBlockState(nearbyPos).getBlock();

            if (block instanceof Dball1Block) {
                foundDball1 = true;
            } else if (block instanceof Dball2Block) {
                foundDball2 = true;
            } else if (block instanceof Dball3Block) {
                foundDball3 = true;
            } else if (block instanceof Dball4Block) {
                foundDball4 = true;
            } else if (block instanceof Dball5Block) {
                foundDball5 = true;
            } else if (block instanceof Dball6Block) {
                foundDball6 = true;
            } else if (block instanceof Dball7Block) {
                foundDball7 = true;
            }

            // Si todos los bloques fueron encontrados, no es necesario seguir buscando
            if (foundDball1 && foundDball2 && foundDball3 && foundDball4 && foundDball5 && foundDball6 && foundDball7) {
                return true;
            }
        }

        // Retorna true solo si todos los bloques fueron encontrados
        return foundDball1 && foundDball2 && foundDball3 && foundDball4 && foundDball5 && foundDball6 && foundDball7;
    }

    private void removeAllDballBlocks(Level world, BlockPos pos) {
        for (BlockPos nearbyPos : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
            Block block = world.getBlockState(nearbyPos).getBlock();

            if (block instanceof Dball1Block || block instanceof Dball2Block ||
                    block instanceof Dball3Block || block instanceof Dball4Block ||
                    block instanceof Dball5Block || block instanceof Dball6Block ||
                    block instanceof Dball7Block) {
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