package com.yuseix.dragonminez.common.init.blocks.custom.dballs;

import com.google.common.collect.ImmutableMap;
import com.yuseix.dragonminez.common.config.old.DMZGeneralConfig;
import com.yuseix.dragonminez.common.init.MainEntity;
import com.yuseix.dragonminez.common.init.MainSounds;
import com.yuseix.dragonminez.common.init.blocks.entity.Dball6NamekBlockEntity;
import com.yuseix.dragonminez.common.init.entity.custom.PorungaEntity;
import com.yuseix.dragonminez.server.worldgen.dimension.ModDimensions;
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

public class Dball6NamekBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public Dball6NamekBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {

        switch (((Direction) pState.getValue(FACING)).getAxis()) {
            case X:
                // Ajusta las coordenadas X para centrarlo en el eje X
                return Dball6NamekBlock.box((16 - 12) / 2.0, 0.0, (16 - 12) / 2.0, (16 + 13) / 2.0, 12.0, (16 + 14) / 2.0);
            case Z:
            default:
                // Ajusta las coordenadas Z para centrarlo en el eje Z
                return Dball6NamekBlock.box((16 - 8) / 2.0, 0.0, (16 - 8) / 2.0, (16 + 8) / 2.0, 7.0, (16 + 8) / 2.0);
        }

    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new Dball6NamekBlockEntity(blockPos, blockState);
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
        if (pLevel.dimension() == ModDimensions.NAMEK_DIM_LEVEL_KEY) {
            if (areAllDballBlocksNearby(pLevel, pPos) && DMZGeneralConfig.SHOULD_DRAGON_SPAWN.get()) {
                // Elimina los bloques
                removeAllDballBlocks(pLevel, pPos);

                if (!pLevel.isClientSide) {
                    ServerLevel serverLevel = (ServerLevel) pLevel;
                    long currentTime = pLevel.getDayTime();
                    serverLevel.setDayTime(16000);

                    PorungaEntity dragonEntity = new PorungaEntity(MainEntity.PORUNGA.get(),pLevel);
                    dragonEntity.setOwnerName(pPlayer.getName().getString());
                    dragonEntity.setInvokingTime(currentTime);
                    dragonEntity.setDeseos(3);
                    dragonEntity.moveTo(pPos.getX() + 0.5, pPos.getY(), pPos.getZ() + 0.5, 0.0F, 0.0F);
                    pLevel.addFreshEntity(dragonEntity);
                    pLevel.playSound(null, pPos, MainSounds.SHENRON.get(), SoundSource.AMBIENT, 1.0F, 1.0F);
                }

                return InteractionResult.SUCCESS;
            }
        }

        // Si no están todos los bloques o no estamos en Namek, no hace nada
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

            if (block instanceof Dball1NamekBlock) {
                foundDball1 = true;
            } else if (block instanceof Dball2NamekBlock) {
                foundDball2 = true;
            } else if (block instanceof Dball3NamekBlock) {
                foundDball3 = true;
            } else if (block instanceof Dball4NamekBlock) {
                foundDball4 = true;
            } else if (block instanceof Dball5NamekBlock) {
                foundDball5 = true;
            } else if (block instanceof Dball6NamekBlock) {
                foundDball6 = true;
            } else if (block instanceof Dball7NamekBlock) {
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

            if (block instanceof Dball1NamekBlock || block instanceof Dball2NamekBlock ||
                    block instanceof Dball3NamekBlock || block instanceof Dball4NamekBlock ||
                    block instanceof Dball5NamekBlock || block instanceof Dball6NamekBlock ||
                    block instanceof Dball7NamekBlock) {
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