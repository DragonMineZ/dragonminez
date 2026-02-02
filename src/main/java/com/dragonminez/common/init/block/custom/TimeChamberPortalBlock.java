package com.dragonminez.common.init.block.custom;

import com.dragonminez.common.init.block.entity.TimeChamberPortalBlockEntity;
import com.dragonminez.server.world.dimension.HTCDimension;
import com.dragonminez.server.world.structure.helper.DMZStructures;
import com.dragonminez.server.world.structure.helper.StructureLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.util.ITeleporter;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.function.Function;

public class TimeChamberPortalBlock extends BaseEntityBlock {

	public TimeChamberPortalBlock() {
		super(BlockBehaviour.Properties.copy(Blocks.QUARTZ_BLOCK).noParticlesOnBreak().strength(-1.0F, 3600000.0F));
	}

	@Override
	public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
		if (pHand == InteractionHand.MAIN_HAND) {
			if (!pLevel.isClientSide) {
				BlockEntity be = pLevel.getBlockEntity(pPos);
				if (!(be instanceof TimeChamberPortalBlockEntity tile)) return InteractionResult.FAIL;

				boolean onHTC = pLevel.dimension().equals(HTCDimension.HTC_KEY);
				ResourceKey<Level> targetDimKey = onHTC ? Level.OVERWORLD : HTCDimension.HTC_KEY;
				ServerLevel targetLevel = pPlayer.getServer().getLevel(targetDimKey);

				if (targetLevel != null && !pPlayer.isPassenger()) {
					BlockPos targetPos;
					float rotX = onHTC ? 180 : 90;

					if (tile.hasCachedTarget()) {
						targetPos = tile.getCachedTarget();
						if (!targetLevel.getBlockState(targetPos.below()).is(this)) {
							tile.setCachedTarget(null);
							targetPos = findTargetAndCache(targetLevel, onHTC, pPlayer, tile, this);
						}
					} else targetPos = findTargetAndCache(targetLevel, onHTC, pPlayer, tile, this);

					if (targetPos != null) {
						teleportPlayer(pPlayer, targetLevel, targetPos, rotX);
					}
				}
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.CONSUME;
	}

	private BlockPos findTargetAndCache(ServerLevel targetLevel, boolean onHTC, Player player, TimeChamberPortalBlockEntity tile, Block targetBlock) {
		ResourceKey<Structure> targetStructureKey = onHTC ? DMZStructures.KAMILOOKOUT : DMZStructures.TIMECHAMBER;
		BlockPos searchCenter = onHTC ? BlockPos.ZERO : player.blockPosition();

		BlockPos structurePos = StructureLocator.locateStructure(targetLevel, targetStructureKey, searchCenter);
		BlockPos finalPos = null;

		if (structurePos != null) {
			finalPos = findPortalInStructureMeta(targetLevel, structurePos, targetStructureKey, targetBlock);
			if (finalPos == null) finalPos = findPortalByAreaScan(targetLevel, structurePos, onHTC, targetBlock);
		}

		if (finalPos != null) tile.setCachedTarget(finalPos);

		return finalPos;
	}

	private BlockPos findPortalInStructureMeta(ServerLevel level, BlockPos structureCenter, ResourceKey<Structure> structureKey, Block targetBlock) {
		var structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
		var structureHolder = structureRegistry.getHolder(structureKey).orElse(null);
		if (structureHolder == null) return null;

		level.getChunk(structureCenter.getX() >> 4, structureCenter.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS);
		StructureStart start = level.structureManager().getStructureAt(structureCenter, structureHolder.value());

		if (start != null && start.isValid()) {
			var boundingBox = start.getBoundingBox();
			for (BlockPos pos : BlockPos.betweenClosed(boundingBox.minX(), boundingBox.minY(), boundingBox.minZ(), boundingBox.maxX(), boundingBox.maxY(), boundingBox.maxZ())) {
				if (level.getBlockState(pos).is(targetBlock)) return pos.above();
			}
		}
		return null;
	}

	private BlockPos findPortalByAreaScan(ServerLevel level, BlockPos center, boolean onHTC, Block targetBlock) {
		int offX, offY, offZ;

		if (onHTC) {
			offX = 45; offY = 125; offZ = 75;
		} else {
			offX = 62; offY = 4; offZ = 66;
		}

		BlockPos[] candidates = new BlockPos[4];
		candidates[0] = center.offset(offX, offY, offZ);
		candidates[1] = center.offset(-offZ, offY, offX);
		candidates[2] = center.offset(-offX, offY, -offZ);
		candidates[3] = center.offset(offZ, offY, -offX);

		int searchRadius = 24;
		int verticalRadius = onHTC ? 15 : 5;

		for (BlockPos p : candidates) {
			level.getChunk(p);

			for (BlockPos checkPos : BlockPos.betweenClosed(p.getX() - searchRadius, p.getY() - verticalRadius, p.getZ() - searchRadius, p.getX() + searchRadius, p.getY() + verticalRadius, p.getZ() + searchRadius)) {
				if (level.getBlockState(checkPos).is(targetBlock)) return checkPos.above();
			}
		}

		return null;
	}

	private void teleportPlayer(Player player, ServerLevel targetLevel, BlockPos targetPos, float rotX) {
		player.changeDimension(targetLevel, new ITeleporter() {
			@Override
			public Entity placeEntity(Entity entity, ServerLevel current, ServerLevel destination, float yaw, Function<Boolean, Entity> repositionEntity) {
				return repositionEntity.apply(false);
			}
		});
		player.teleportTo(targetLevel, targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, Collections.emptySet(), rotX, 0);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
		return new TimeChamberPortalBlockEntity(pPos, pState);
	}

	@Override
	public RenderShape getRenderShape(BlockState pState) {
		return RenderShape.MODEL;
	}
}