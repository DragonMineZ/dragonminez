package com.dragonminez.common.init.block.custom;

import com.dragonminez.server.world.dimension.HTCDimension;
import com.dragonminez.server.world.structure.helper.DMZStructures;
import com.dragonminez.server.world.structure.helper.StructureLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.util.ITeleporter;

import java.util.Collections;
import java.util.function.Function;

public class TimeChamberPortalBlock extends Block {
	public TimeChamberPortalBlock() {
		super(BlockBehaviour.Properties.copy(Blocks.QUARTZ_BLOCK).noParticlesOnBreak().strength(-1.0F, 3600000.0F).noLootTable());
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos blockpos, Player pPlayer, InteractionHand hand, BlockHitResult hit) {
		if (pPlayer.canChangeDimensions()) {
			if (pPlayer.level() instanceof ServerLevel) {
				boolean onHTC = pPlayer.level().dimension() == HTCDimension.HTC_KEY;
				ServerLevel targetWorld = ((ServerLevel) pPlayer.level()).getServer().getLevel(onHTC ? Level.OVERWORLD : HTCDimension.HTC_KEY);

				if (targetWorld != null && !pPlayer.isPassenger()) {
					pPlayer.changeDimension(targetWorld, new ITeleporter() {
						@Override
						public Entity placeEntity(Entity entity, ServerLevel current, ServerLevel destination, float yaw, Function<Boolean, Entity> repositionEntity) {
							return repositionEntity.apply(false);
						}
					});

					int rotX, rotY = 0; ResourceKey<Level> dim; BlockPos pos, structurePos;
					if (onHTC) {
						dim = Level.OVERWORLD;
						structurePos = StructureLocator.locateStructure(targetWorld, DMZStructures.KAMILOOKOUT, BlockPos.ZERO);
						pos = structurePos.offset(45, 125, 75);
						rotX = 180;
					} else {
						dim = HTCDimension.HTC_KEY;
						structurePos = StructureLocator.locateStructure(targetWorld, DMZStructures.TIMECHAMBER, BlockPos.ZERO);
						pos = structurePos.offset(62, 4, 66);
						rotX = 90;
					}
					ServerLevel targetLevel = pPlayer.getServer().getLevel(dim);
					pPlayer.teleportTo(targetLevel, pos.getX(), pos.getY(), pos.getZ(), Collections.emptySet(), rotX, rotY);

				}
			}

			return InteractionResult.SUCCESS;
		} else {
			return InteractionResult.CONSUME;
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
