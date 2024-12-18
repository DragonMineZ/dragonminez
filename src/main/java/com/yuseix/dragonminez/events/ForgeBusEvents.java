package com.yuseix.dragonminez.events;

import com.mojang.logging.LogUtils;
import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.commands.*;
import com.yuseix.dragonminez.init.MainBlocks;
import com.yuseix.dragonminez.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.stats.DMZStatsProvider;
import com.yuseix.dragonminez.storyline.player.PlayerStorylineProvider;
import com.yuseix.dragonminez.world.*;
import com.yuseix.dragonminez.worldgen.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.command.ConfigCommand;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

//Anteriormente llamado ForgeListener ya que los eventos forman parte del bus de MinecraftForge
//ACTUALMENTE LOS ModEvents son eventos que se ejecutan en el bus de Forge **(DIFERENTE al IModBusEvent)**
//Si una clase extiende "Event" se considera un evento del bus de Forge y TIENE que estar acá.
//O también si es parte del paquete "net.minecraftforge.eventbus.api"
public class ForgeBusEvents {

	public static final Capability<DMZStatsCapabilities> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {
	});


	private static final List<BlockPos> dragonBallPositions = new ArrayList<>();
	private static final List<BlockPos> namekDragonBallPositions = new ArrayList<>();

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final List<String> ALLOWED_USERNAMES = Arrays.asList(
			// Staff
			"Dev",
			"Yuseix",
			"ezShokkoh",
			"MrBrunoh",
			"Toji71",
			// Testers
			"ThiagoHanagaki",
			"Gabrielololo",
			"InYourHeart_",
			"Im_Lu_",
			"kavu_",
			"andysito_",
			"Ducco123",
			"Rev_zy", //Mazu
			"grillo78",
			"TheWildBoss",
			"EsePibe01",
			"Pokimons123",
			"bbysixty",
			"Onashi",
			// Patreon
			"Baby_Poop12311" // Cyanea capillata
	);

	// Recordar comentar esto antes de Buildear una versión Pública.
	// y Descomentar para el buildeo de versiones de Testing.
//    @SubscribeEvent
//    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
//        Player player = event.getEntity();
//
//        String username = player.getGameProfile().getName();
//
//        if (!ALLOWED_USERNAMES.contains(username)) {
//            LOGGER.error("The user {} is not allowed to play the mod. The game session will now be terminated.", username);
//            throw new IllegalStateException("DMZ: Username not allowed to start gameplay!");
//        }
//    }


	@SubscribeEvent
	public void onServerStarting(ServerStartingEvent event) {
		ServerLevel serverWorld = event.getServer().getLevel(Level.OVERWORLD);
		if (serverWorld == null) {
			return;
		}
		if (!serverWorld.isClientSide()) {
			serverWorld.getCapability(DragonBallGenProvider.CAPABILITY).ifPresent(dragonBallsCapability -> {
				boolean bhasDragonBalls = dragonBallsCapability.hasDragonBalls();

				if (!bhasDragonBalls) {
					spawnDragonBall(serverWorld, MainBlocks.DBALL1_BLOCK.get().defaultBlockState());
					spawnDragonBall(serverWorld, MainBlocks.DBALL2_BLOCK.get().defaultBlockState());
					spawnDragonBall(serverWorld, MainBlocks.DBALL3_BLOCK.get().defaultBlockState());
					spawnDragonBall(serverWorld, MainBlocks.DBALL4_BLOCK.get().defaultBlockState());
					spawnDragonBall(serverWorld, MainBlocks.DBALL5_BLOCK.get().defaultBlockState());
					spawnDragonBall(serverWorld, MainBlocks.DBALL6_BLOCK.get().defaultBlockState());
					spawnDragonBall(serverWorld, MainBlocks.DBALL7_BLOCK.get().defaultBlockState());

					dragonBallsCapability.setDragonBallPositions(dragonBallPositions);
					dragonBallsCapability.setHasDragonBalls(true);
				}
			});
		}
	}

	@SubscribeEvent
	public void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof Player player) {
			if (event.getObject().getCapability(INSTANCE).isPresent()) {
				return;
			}
			//System.out.println("Añadiendo capability");
			final DMZStatsProvider provider = new DMZStatsProvider(player);
			final PlayerStorylineProvider storylineprovider = new PlayerStorylineProvider();

			event.addCapability(DMZStatsProvider.ID, provider);
			event.addCapability(PlayerStorylineProvider.ID, storylineprovider);

		}
	}

	@SubscribeEvent
	public void onAttachCapabilitiesWorld(AttachCapabilitiesEvent<Level> event) {
		if (event.getObject() instanceof ServerLevel) {
			if (!event.getObject().getCapability(DragonBallGenProvider.CAPABILITY).isPresent())
				event.addCapability(new ResourceLocation(DragonMineZ.MOD_ID, "dragon_balls"), new DragonBallGenProvider());

			if (!event.getObject().getCapability(NamekDragonBallGenProvider.CAPABILITY).isPresent())
				event.addCapability(new ResourceLocation(DragonMineZ.MOD_ID, "namek_dragon_balls"), new NamekDragonBallGenProvider());

			if (!event.getObject().getCapability(StructuresProvider.CAPABILITY).isPresent())
				event.addCapability(new ResourceLocation(DragonMineZ.MOD_ID, "structures"), new StructuresProvider());


		}
	}

	@SubscribeEvent
	public void onCommandsRegister(RegisterCommandsEvent event) {
		new ZPointsCommand(event.getDispatcher());
		new StatsCommand(event.getDispatcher());
		new ResetCharacterCommand(event.getDispatcher());
		new AlignmentCommand(event.getDispatcher());
		new LocationsCommand(event.getDispatcher());
		new DMZPermaEffectsCommand(event.getDispatcher());
		new DMZTempEffectsCommand(event.getDispatcher());
		new StorylineCommand(event.getDispatcher());

		ConfigCommand.register(event.getDispatcher());
	}

	@SubscribeEvent
	public void onWorldLoad(LevelEvent.Load event) {
		if (event.getLevel() instanceof ServerLevel serverLevel && !serverLevel.isClientSide()) {
			if (serverLevel.dimension() == Level.OVERWORLD) { //Dimension de overworld
				// Obtener la capability
				LazyOptional<StructuresCapability> capability = serverLevel.getCapability(StructuresProvider.CAPABILITY);

				// Ejecutar la generación si la Torre aún no ha sido generada
				capability.ifPresent(torreCap -> {
					torreCap.generateKamisamaStructure(serverLevel);
				});
			}
			if (serverLevel.dimension() == ModDimensions.TIME_CHAMBER_DIM_LEVEL_KEY) { //Dimension Habitación del Tiempo
				LazyOptional<StructuresCapability> capability = serverLevel.getCapability(StructuresProvider.CAPABILITY);
				capability.ifPresent(cap -> {
					cap.generateHabTiempoStructure(serverLevel);
				});
			}
			if (serverLevel.dimension() == ModDimensions.NAMEK_DIM_LEVEL_KEY) {
				LazyOptional<NamekDragonBallsCapability> namekDragonBallsCapability = serverLevel.getCapability(NamekDragonBallGenProvider.CAPABILITY);
				namekDragonBallsCapability.ifPresent(namekDragonBalls -> {
					// Verifica si ya se han generado las Dragon Balls
					if (!namekDragonBalls.hasNamekDragonBalls()) {
						spawnNamekDragonBall(serverLevel, MainBlocks.DBALL1_NAMEK_BLOCK.get().defaultBlockState());
						spawnNamekDragonBall(serverLevel, MainBlocks.DBALL2_NAMEK_BLOCK.get().defaultBlockState());
						spawnNamekDragonBall(serverLevel, MainBlocks.DBALL3_NAMEK_BLOCK.get().defaultBlockState());
						spawnNamekDragonBall(serverLevel, MainBlocks.DBALL4_NAMEK_BLOCK.get().defaultBlockState()); // Quitar una vez se meta al Gran Patriarca xd
						spawnNamekDragonBall(serverLevel, MainBlocks.DBALL5_NAMEK_BLOCK.get().defaultBlockState());
						spawnNamekDragonBall(serverLevel, MainBlocks.DBALL6_NAMEK_BLOCK.get().defaultBlockState());
						spawnNamekDragonBall(serverLevel, MainBlocks.DBALL7_NAMEK_BLOCK.get().defaultBlockState());

						// Indica que las Dragon Balls de Namek han sido generadas
						namekDragonBalls.setNamekDragonBallPositions(namekDragonBallPositions);
						namekDragonBalls.setHasNamekDragonBalls(true);
					}
				});
			}
		}
	}


	private void spawnDragonBall(ServerLevel serverWorld, BlockState dragonBall) {

		//Spawn the dragon balls
		BlockPos spawnPos = serverWorld.getSharedSpawnPos();
		Random random = new Random();

		// Generate a random position within a 5k block radius from the spawn
		int x = spawnPos.getX() + random.nextInt(10000) - 5000;
		int z = spawnPos.getZ() + random.nextInt(10000) - 5000;

		serverWorld.getChunk(x >> 4, z >> 4); // Load the chunk (if not already loaded)


		int y = serverWorld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

		BlockPos pos = new BlockPos(x, y, z);

		// Place a Dragon Ball block at the generated position
		serverWorld.setBlock(pos, dragonBall, 2);
		System.out.println("Dragon Ball spawned at " + pos);

		dragonBallPositions.add(pos);
	}

	private void spawnNamekDragonBall(ServerLevel serverWorld, BlockState namekDragonBall) {
		//Spawn the dragon balls
		BlockPos spawnPos = serverWorld.getSharedSpawnPos();
		Random random = new Random();

		// Generate a random position within a 5k block radius from the spawn
		int x = spawnPos.getX() + random.nextInt(10000) - 5000;
		int z = spawnPos.getZ() + random.nextInt(10000) - 5000;

		serverWorld.getChunk(x >> 4, z >> 4); // Load the chunk (if not already loaded)

		int y = serverWorld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

		BlockPos pos = new BlockPos(x, y, z);

		// Place a Dragon Ball block at the generated position
		serverWorld.setBlock(pos, namekDragonBall, 2);
		System.out.println("Namekian Dragon Ball spawned at " + pos);

		namekDragonBallPositions.add(pos);
	}
}
