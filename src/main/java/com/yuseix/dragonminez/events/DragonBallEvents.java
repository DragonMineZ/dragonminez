package com.yuseix.dragonminez.events;

import com.mojang.logging.LogUtils;
import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.config.DMZGeneralConfig;
import com.yuseix.dragonminez.init.MainBlocks;
import com.yuseix.dragonminez.network.ModMessages;
import com.yuseix.dragonminez.network.S2C.UpdateDragonRadarS2C;
import com.yuseix.dragonminez.network.S2C.UpdateNamekDragonRadarS2C;
import com.yuseix.dragonminez.utils.DebugUtils;
import com.yuseix.dragonminez.world.DragonBallGenProvider;
import com.yuseix.dragonminez.world.NamekDragonBallGenProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = DragonMineZ.MOD_ID)
public class DragonBallEvents {
	private static final Logger LOGGER = LogUtils.getLogger();

	@SubscribeEvent
	public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		if (!DMZGeneralConfig.SHOULD_DBALLEVENTS_ACTIVE.get()) return;

		Level level = player.level();
		BlockPos pos = event.getPos();
		BlockState blockState = event.getPlacedBlock();
		Block block = blockState.getBlock();

		// Verificar si el bloque colocado es una Dragon Ball
		if (!isDragonBallBlock(block)) return;

		if (level instanceof ServerLevel serverLevel) {
			if (level.getCapability(DragonBallGenProvider.CAPABILITY).isPresent() && isEarthDB(block)) {
				level.getCapability(DragonBallGenProvider.CAPABILITY).ifPresent(capability -> {
					// Buscar si ya había una esfera con el mismo tipo en otra ubicación
					capability.dragonBallPositions.stream()
							.filter(existingPos -> mismaDragonBall(level, existingPos, block))
							.findFirst()
							.ifPresent(existingPos -> {
								// Romper el bloque anterior y reemplazarlo con aire
								level.setBlock(existingPos, Blocks.AIR.defaultBlockState(), 3);
								LOGGER.info("[DBallEvents] Removed existing Dragon Ball at {}", existingPos);
								capability.dragonBallPositions.remove(existingPos);
							});

					// Actualizar la posición actual
					capability.dragonBallPositions.add(pos);
					LOGGER.info("[DBallEvents] Placed Dragon Ball at {}", pos);

					// Guardar datos y sincronizar con el cliente
					capability.saveToSavedData(serverLevel);
					ModMessages.sendToPlayer(new UpdateDragonRadarS2C(capability.dragonBallPositions), player);
				});
			} else if (level.getCapability(NamekDragonBallGenProvider.CAPABILITY).isPresent() && isNamekDB(block)) {
				level.getCapability(NamekDragonBallGenProvider.CAPABILITY).ifPresent(capability -> {
					// Buscar si ya había una esfera con el mismo tipo en otra ubicación
					capability.namekDragonBallPositions.stream()
							.filter(existingPos -> mismaDragonBall(level, existingPos, block))
							.findFirst()
							.ifPresent(existingPos -> {
								// Romper el bloque anterior y reemplazarlo con aire
								level.setBlock(existingPos, Blocks.AIR.defaultBlockState(), 3);
								LOGGER.info("[DBallEvents] Removed existing Namek Dragon Ball at {}", existingPos);
								capability.namekDragonBallPositions.remove(existingPos);
							});

					// Actualizar la posición actual
					capability.namekDragonBallPositions.add(pos);
					LOGGER.info("[DBallEvents] Placed Namekian Dragon Ball at {}", pos);

					// Guardar datos y sincronizar con el cliente
					capability.saveToSavedData(serverLevel);
					ModMessages.sendToPlayer(new UpdateNamekDragonRadarS2C(capability.namekDragonBallPositions), player);
				});
			}
		}
	}

	@SubscribeEvent
	public static void onBlockBreak(BlockEvent.BreakEvent event) {
		if (!(event.getPlayer() instanceof ServerPlayer player)) return;

		Level level = player.level();
		BlockPos pos = event.getPos();
		Block block = level.getBlockState(pos).getBlock();

		// Verificar si el bloque roto es una Dragon Ball
		if (!isDragonBallBlock(block)) return;

		if (level instanceof ServerLevel serverLevel) {
			if (level.getCapability(DragonBallGenProvider.CAPABILITY).isPresent() && isEarthDB(block)) {
				level.getCapability(DragonBallGenProvider.CAPABILITY).ifPresent(capability -> {
					// Eliminar la posición
					capability.dragonBallPositions.removeIf(p -> p.equals(pos));
					LOGGER.info("[DBallEvents] Breaked Dragon Ball at {}", pos);

					// Guardar datos y sincronizar con el cliente
					capability.saveToSavedData(serverLevel);
					ModMessages.sendToPlayer(new UpdateDragonRadarS2C(capability.dragonBallPositions), player);
				});
			} else if (level.getCapability(NamekDragonBallGenProvider.CAPABILITY).isPresent() && isNamekDB(block)) {
				level.getCapability(NamekDragonBallGenProvider.CAPABILITY).ifPresent(capability -> {
					// Eliminar la posición
					capability.namekDragonBallPositions.removeIf(p -> p.equals(pos));
					LOGGER.info("[DBallEvents] Breaked Namekian Dragon Ball at {}", pos);

					// Guardar datos y sincronizar con el cliente
					capability.saveToSavedData(serverLevel);
					ModMessages.sendToPlayer(new UpdateNamekDragonRadarS2C(capability.namekDragonBallPositions), player);
				});
			}
		}
	}

	private static boolean isDragonBallBlock(Block block) {
		return block == MainBlocks.DBALL1_BLOCK.get() || block == MainBlocks.DBALL2_BLOCK.get() || block == MainBlocks.DBALL3_BLOCK.get() || block == MainBlocks.DBALL4_BLOCK.get() || block == MainBlocks.DBALL5_BLOCK.get() || block == MainBlocks.DBALL6_BLOCK.get() || block == MainBlocks.DBALL7_BLOCK.get() ||
				block == MainBlocks.DBALL1_NAMEK_BLOCK.get() || block == MainBlocks.DBALL2_NAMEK_BLOCK.get() || block == MainBlocks.DBALL3_NAMEK_BLOCK.get() || block == MainBlocks.DBALL4_NAMEK_BLOCK.get() || block == MainBlocks.DBALL5_NAMEK_BLOCK.get() || block == MainBlocks.DBALL6_NAMEK_BLOCK.get() || block == MainBlocks.DBALL7_NAMEK_BLOCK.get();
	}

	private static boolean isEarthDB(Block block) {
		return block == MainBlocks.DBALL1_BLOCK.get() || block == MainBlocks.DBALL2_BLOCK.get() || block == MainBlocks.DBALL3_BLOCK.get() || block == MainBlocks.DBALL4_BLOCK.get() || block == MainBlocks.DBALL5_BLOCK.get() || block == MainBlocks.DBALL6_BLOCK.get() || block == MainBlocks.DBALL7_BLOCK.get();
	}

	private static boolean isNamekDB(Block block) {
		return block == MainBlocks.DBALL1_NAMEK_BLOCK.get() || block == MainBlocks.DBALL2_NAMEK_BLOCK.get() || block == MainBlocks.DBALL3_NAMEK_BLOCK.get() || block == MainBlocks.DBALL4_NAMEK_BLOCK.get() || block == MainBlocks.DBALL5_NAMEK_BLOCK.get() || block == MainBlocks.DBALL6_NAMEK_BLOCK.get() || block == MainBlocks.DBALL7_NAMEK_BLOCK.get();
	}

	private static boolean mismaDragonBall(Level level, BlockPos pos, Block block) {
		Block bloqueActual = level.getBlockState(pos).getBlock();
		return bloqueActual == block;
	}
}
