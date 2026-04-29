package com.dragonminez.common.init.block.custom;

import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonBallSetDefinition;
import com.dragonminez.common.dragonball.DragonDefinition;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.block.entity.DragonBallBlockEntity;
import com.dragonminez.server.events.DragonBallsHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DragonBallBlock extends BaseEntityBlock implements EntityBlock {
	private static final VoxelShape SHAPE = Shapes.box(0.25D, 0.0D, 0.25D, 0.75D, 0.5D, 0.75D);

	private final DragonBallType ballType;
	private final String ballSetId;

	public DragonBallBlock(BlockBehaviour.Properties properties, DragonBallType ballType, String ballSetId) {
		super(properties);
		this.ballType = ballType;
		this.ballSetId = ballSetId;
	}

	public DragonBallType getBallType() {
		return ballType;
	}

	public String getBallSetId() {
		return ballSetId;
	}

	public boolean isNamekian() {
		return "namek".equals(ballSetId);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return !level.isEmptyBlock(pos.below());
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DragonBallBlockEntity(pos, state, ballType, ballSetId);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}

		DragonBallSetDefinition ballSetDefinition = DragonBallDefinitions.getBallSet(ballSetId);
		if (ballSetDefinition == null || !ballSetDefinition.supportsDimension(level.dimension())) {
			return InteractionResult.PASS;
		}

		DragonDefinition dragonDefinition = DragonBallDefinitions.getDragonForSetAndDimension(ballSetId, level.dimension());
		if (dragonDefinition == null) {
			return InteractionResult.PASS;
		}

		if (areAllDragonBallsNearby(level, pos, ballSetDefinition)) {
			List<BlockPos> consumedPositions = removeAllDragonBalls(level, pos, ballSetDefinition);
			if (level instanceof ServerLevel serverLevel) {
				DragonBallsHandler.unregisterConsumedDragonBalls(serverLevel, consumedPositions, ballSetId);
				if (summonDragon(serverLevel, pos, player, dragonDefinition)) {
					MinecraftForge.EVENT_BUS.post(new DMZEvent.DragonSummonedEvent(
							player,
							serverLevel,
							pos,
							dragonDefinition,
							ballSetDefinition,
							consumedPositions
					));
					serverLevel.playSound(null, pos, MainSounds.SHENRON.get(), SoundSource.AMBIENT, 1.0F, 1.0F);
				}
			}
			return InteractionResult.CONSUME;
		}

		return InteractionResult.PASS;
	}

	private boolean summonDragon(ServerLevel serverLevel, BlockPos pos, Player player, DragonDefinition dragonDefinition) {
		EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.fromNamespaceAndPath("dragonminez", dragonDefinition.getId()));
		if (entityType == null) {
			return false;
		}

		var entity = entityType.create(serverLevel);
		if (!(entity instanceof com.dragonminez.common.init.entities.dragon.DragonWishEntity dragon)) {
			return false;
		}

		dragon.setDragonDefinitionId(dragonDefinition.getId());
		dragon.setOwnerName(player.getName().getString());
		dragon.setInvokingTime(serverLevel.getDayTime());
		dragon.setGrantedWish(false);
		dragon.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
		return serverLevel.addFreshEntity(dragon);
	}

	private boolean areAllDragonBallsNearby(Level level, BlockPos pos, DragonBallSetDefinition setDefinition) {
		Set<DragonBallType> foundBalls = new HashSet<>();
		int radius = setDefinition.getSummonRadius();
		for (BlockPos checkPos : BlockPos.betweenClosed(pos.offset(-radius, -radius, -radius), pos.offset(radius, radius, radius))) {
			Block block = level.getBlockState(checkPos).getBlock();
			if (block instanceof DragonBallBlock dragonBall && ballSetId.equals(dragonBall.getBallSetId())) {
				foundBalls.add(dragonBall.getBallType());
			}
		}
		return foundBalls.size() == 7;
	}

	private List<BlockPos> removeAllDragonBalls(Level level, BlockPos pos, DragonBallSetDefinition setDefinition) {
		Set<DragonBallType> removedBalls = new HashSet<>();
		List<BlockPos> removedPositions = new ArrayList<>();
		int radius = setDefinition.getSummonRadius();
		for (BlockPos checkPos : BlockPos.betweenClosed(pos.offset(-radius, -radius, -radius), pos.offset(radius, radius, radius))) {
			Block block = level.getBlockState(checkPos).getBlock();
			if (block instanceof DragonBallBlock dragonBall && ballSetId.equals(dragonBall.getBallSetId())) {
				if (!removedBalls.contains(dragonBall.getBallType())) {
					level.removeBlock(checkPos, false);
					removedBalls.add(dragonBall.getBallType());
					removedPositions.add(checkPos.immutable());
				}
			}
		}
		return removedPositions;
	}
}
