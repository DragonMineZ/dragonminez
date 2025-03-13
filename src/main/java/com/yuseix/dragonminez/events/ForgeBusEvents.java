package com.yuseix.dragonminez.events;

import com.mojang.logging.LogUtils;
import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.commands.*;
import com.yuseix.dragonminez.config.DMZGeneralConfig;
import com.yuseix.dragonminez.init.MainBlocks;
import com.yuseix.dragonminez.init.MainEntity;
import com.yuseix.dragonminez.init.MainParticles;
import com.yuseix.dragonminez.init.entity.custom.PorungaEntity;
import com.yuseix.dragonminez.init.entity.custom.ShenlongEntity;
import com.yuseix.dragonminez.network.ModMessages;
import com.yuseix.dragonminez.network.S2C.SyncDragonBallsS2C;
import com.yuseix.dragonminez.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.stats.DMZStatsProvider;
import com.yuseix.dragonminez.stats.storymode.DMZQuestProvider;
import com.yuseix.dragonminez.stats.storymode.DMZStoryCapability;
import com.yuseix.dragonminez.utils.PlayerInventoryManager;
import com.yuseix.dragonminez.world.DragonBallGenProvider;
import com.yuseix.dragonminez.world.NamekDragonBallGenProvider;
import com.yuseix.dragonminez.world.StructuresCapability;
import com.yuseix.dragonminez.world.StructuresProvider;
import com.yuseix.dragonminez.worldgen.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
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
@Mod.EventBusSubscriber(modid = DragonMineZ.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeBusEvents {
	private static final List<BlockPos> dragonBallPositions = new ArrayList<>();
	private static final List<BlockPos> namekDragonBallPositions = new ArrayList<>();
	private static boolean spawnedDB4 = false, spawnedNamekDB4 = false, firstSpawnDB, firstSpawnNDB;

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final List<String> ALLOWED_USERNAMES = Arrays.asList(
			// Staff
			"Dev",
			"ImYuseix",
			"ezShokkoh",
			"MrBrunoh",
			"Toji71",
			"Umino10",
			"InmortalPx",
			"LecuTheAnimator",
			// Testers
			"Ducco123",
			"Rev_zy", //Mazu
			"grillo78",
			"TheWildBoss",
			"EsePibe01",
			"Pokimons123",
			// Patreon
			"Baby_Poop12311", // Cyanea capillata
			"SpaceCarp",
			"prolazorbema10", // Dssccat
			"iLalox",
			"Robberto10",
			"Athrizel"
	);

	// Recordar comentar esto antes de Buildear una versión Pública.
	// y Descomentar para el buildeo de versiones de Testing.
	@SubscribeEvent
	public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		Player player = event.getEntity();

		String username = player.getGameProfile().getName();

		//if (!ALLOWED_USERNAMES.contains(username)) {
		//	LOGGER.error("The user {} is not allowed to play the mod's beta. The game session will now be terminated.", username);
		//	throw new IllegalStateException("DMZ: Username not allowed to start gameplay!");
		//}
		String dimension = "overworld";
		if (player.level().dimension() == ModDimensions.NAMEK_DIM_LEVEL_KEY) {
			dimension = "namek";
		}

		if (event.getEntity() instanceof ServerPlayer serverPlayer) {
			sendDragonBallData(serverPlayer, dimension);
		}

		// Desactivar al iniciar sesión para evitar bugs de que el aura no haga sonido, el turbo no aumente velocidad, etc, etc, etc.
		DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(stats -> {
			if (stats.getBoolean("transform")) stats.setBoolean("transform", false);
			if (stats.getBoolean("turbo")) stats.setBoolean("turbo", false);
			if (stats.getBoolean("aura")) stats.setBoolean("aura", false);
			if (stats.getBoolean("porungarevive")) stats.setBoolean("porungarevive", false);
			if (stats.getBoolean("shenronrevive")) stats.setBoolean("shenronrevive", false);
			if (stats.getBoolean("descend")) stats.setBoolean("descend", false);
		});
	}

	@SubscribeEvent
	public void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			sendDragonBallData(player, "both");
			ServerLevel level = player.serverLevel();

			// Desactivar al cambiar de dimensión para evitar bugs de que el aura no haga sonido, el turbo no aumente velocidad, etc, etc, etc.
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(stats -> {
				if (stats.getBoolean("transform")) stats.setBoolean("transform", false);
				if (stats.getBoolean("turbo")) stats.setBoolean("turbo", false);
				if (stats.getBoolean("aura")) stats.setBoolean("aura", false);
				if (stats.getBoolean("porungarevive")) stats.setBoolean("porungarevive", false);
				if (stats.getBoolean("shenronrevive")) stats.setBoolean("shenronrevive", false);
				if (stats.getBoolean("descend")) stats.setBoolean("descend", false);

				if (DMZGeneralConfig.SAVE_INVENTORY.get()) {
					PlayerInventoryManager inventoryManager = PlayerInventoryManager.get(level);
					if (event.getFrom() == ModDimensions.OTHERWORLD_DIM_LEVEL_KEY) {
						ListTag mainInventory = inventoryManager.getMainInventory(player.getUUID());
						player.getInventory().save(mainInventory);
						inventoryManager.setMainInventory(player.getUUID(), mainInventory);
						player.getInventory().load(mainInventory);
					}
					player.inventoryMenu.broadcastChanges();
				}
			});
		}
	}

	private static void sendDragonBallData(ServerPlayer player, String dimension) {
		ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
		ServerLevel namek = player.server.getLevel(ModDimensions.NAMEK_DIM_LEVEL_KEY);

		List<BlockPos> earthDragonBalls = new ArrayList<>();
		List<BlockPos> namekDragonBalls = new ArrayList<>();

		if (overworld != null) {
			if (dimension.equals("overworld") || dimension.equals("both")) {
				overworld.getCapability(DragonBallGenProvider.CAPABILITY).ifPresent(cap -> {
					cap.loadFromSavedData(overworld);
					earthDragonBalls.addAll(cap.dragonBallPositions);
				});
			}
		}

		if (namek != null) {
			if (dimension.equals("namek") || dimension.equals("both")) {
				namek.getCapability(NamekDragonBallGenProvider.CAPABILITY).ifPresent(cap -> {
					cap.loadFromSavedData(namek);
					namekDragonBalls.addAll(cap.namekDragonBallPositions);
				});
			}
		}

		ModMessages.sendToPlayer(new SyncDragonBallsS2C(earthDragonBalls, namekDragonBalls), player);
	}

	@SubscribeEvent
	public void onServerStarting(ServerStartingEvent event) {
		ServerLevel serverOverworld = event.getServer().getLevel(Level.OVERWORLD);
		ServerLevel serverNamek = event.getServer().getLevel(ModDimensions.NAMEK_DIM_LEVEL_KEY);
		if (!DMZGeneralConfig.SHOULD_DBALL_SPAWN.get()) return;

		if (serverOverworld == null) return;
		if (!serverOverworld.isClientSide()) {
			LazyOptional<StructuresCapability> capability = serverOverworld.getCapability(StructuresProvider.CAPABILITY);
			serverOverworld.getCapability(DragonBallGenProvider.CAPABILITY).ifPresent(dragonBallsCapability -> {
				dragonBallsCapability.loadFromSavedData(serverNamek);

				serverOverworld.getServer().tell(new TickTask(serverOverworld.getServer().getTickCount() + 40, () -> {
					if (!dragonBallsCapability.hasDragonBalls() && !firstSpawnDB) {
						spawnDragonBall(serverOverworld, MainBlocks.DBALL1_BLOCK.get().defaultBlockState(), 1);
						spawnDragonBall(serverOverworld, MainBlocks.DBALL2_BLOCK.get().defaultBlockState(), 2);
						spawnDragonBall(serverOverworld, MainBlocks.DBALL3_BLOCK.get().defaultBlockState(), 3);
						// La primer vez que se generen las DragonBalls, guarda la posición de la Esfera de 4 Estrellas que está dentro de la casa de Goku
						capability.ifPresent(cap -> {
							if (cap.getHasGokuHouse() && !spawnedDB4) {
								BlockPos db4pos = cap.getDB4Position();
								dragonBallPositions.add(db4pos);
								LOGGER.info("[FirstSpawn] Dragon Ball [4] spawned at {} (Goku's House)", db4pos);
								spawnedDB4 = true;
							} else {
								spawnDragonBall(serverOverworld, MainBlocks.DBALL4_BLOCK.get().defaultBlockState(), 4);
							}
						});
						spawnDragonBall(serverOverworld, MainBlocks.DBALL5_BLOCK.get().defaultBlockState(), 5);
						spawnDragonBall(serverOverworld, MainBlocks.DBALL6_BLOCK.get().defaultBlockState(), 6);
						spawnDragonBall(serverOverworld, MainBlocks.DBALL7_BLOCK.get().defaultBlockState(), 7);

						dragonBallsCapability.setDragonBallPositions(dragonBallPositions);
						dragonBallsCapability.setHasDragonBalls(true);
						dragonBallsCapability.saveToSavedData(serverOverworld);
						firstSpawnDB = true;
					}
				}));
			});
		}

		if (serverNamek == null) return;
		if (!serverNamek.isClientSide()) {
			LazyOptional<StructuresCapability> capability = serverNamek.getCapability(StructuresProvider.CAPABILITY);
			serverNamek.getCapability(NamekDragonBallGenProvider.CAPABILITY).ifPresent(namekDragonBallsCapability -> {
				namekDragonBallsCapability.loadFromSavedData(serverNamek);

				// Verifica si ya se han generado las Dragon Balls
				serverNamek.getServer().tell(new TickTask(serverNamek.getServer().getTickCount() + 40, () -> {
					if (!namekDragonBallsCapability.hasNamekDragonBalls() && !firstSpawnNDB) {
						spawnNamekDragonBall(serverNamek, MainBlocks.DBALL1_NAMEK_BLOCK.get().defaultBlockState(), 1);
						spawnNamekDragonBall(serverNamek, MainBlocks.DBALL2_NAMEK_BLOCK.get().defaultBlockState(), 2);
						spawnNamekDragonBall(serverNamek, MainBlocks.DBALL3_NAMEK_BLOCK.get().defaultBlockState(), 3);
						capability.ifPresent(cap -> {
							if (cap.getHasElderGuru() && !spawnedNamekDB4) {
								BlockPos namekDB4pos = cap.getNamekDB4Position();
								namekDragonBallPositions.add(namekDB4pos);
								LOGGER.info("[FirstSpawn] Namekian Dragon Ball [4] spawned at {} (Elder Guru's House)", namekDB4pos);
								spawnedNamekDB4 = true;
							} else {
								spawnNamekDragonBall(serverNamek, MainBlocks.DBALL4_NAMEK_BLOCK.get().defaultBlockState(), 4);
							}
						});
						spawnNamekDragonBall(serverNamek, MainBlocks.DBALL5_NAMEK_BLOCK.get().defaultBlockState(), 5);
						spawnNamekDragonBall(serverNamek, MainBlocks.DBALL6_NAMEK_BLOCK.get().defaultBlockState(), 6);
						spawnNamekDragonBall(serverNamek, MainBlocks.DBALL7_NAMEK_BLOCK.get().defaultBlockState(), 7);

						// Indica que las Dragon Balls de Namek han sido generadas
						namekDragonBallsCapability.setNamekDragonBallPositions(namekDragonBallPositions);
						namekDragonBallsCapability.setHasNamekDragonBalls(true);
						namekDragonBallsCapability.saveToSavedData(serverNamek);
						firstSpawnNDB = true;
					}
				}));
			});
		}
	}

	@SubscribeEvent
	public void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof Player player) {
			final DMZStatsProvider provider = new DMZStatsProvider(player);
			final DMZQuestProvider provider_quests = new DMZQuestProvider();

			event.addCapability(DMZStatsProvider.ID, provider);
			event.addCapability(DMZQuestProvider.ID, provider_quests);
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
		new ReviveCommand(event.getDispatcher());
		new ZPointsCommand(event.getDispatcher());
		new StatsCommand(event.getDispatcher());
		new ResetCharacterCommand(event.getDispatcher());
		new AlignmentCommand(event.getDispatcher());
		new PermaEffectsCommand(event.getDispatcher());
		new TempEffectsCommand(event.getDispatcher());
		new SkillsCommand(event.getDispatcher());
		new SuperFormCommand(event.getDispatcher());
		new StoryModeCommand(event.getDispatcher());
		new LocationsCommand(event.getDispatcher());

		ConfigCommand.register(event.getDispatcher());
	}

	@SubscribeEvent
	public void onWorldLoad(LevelEvent.Load event) {
		if (event.getLevel() instanceof ServerLevel serverLevel && !serverLevel.isClientSide()) {
			if (serverLevel.dimension() == Level.OVERWORLD) {
				LazyOptional<StructuresCapability> capability = serverLevel.getCapability(StructuresProvider.CAPABILITY);
				capability.ifPresent(cap -> {
					if (DMZGeneralConfig.SHOULD_KAMILOOKOUT_SPAWN.get()) cap.generateKamisamaStructure(serverLevel);
					if (DMZGeneralConfig.SHOULD_GOKUHOUSE_SPAWN.get()) cap.generateGokuHouseStructure(serverLevel);
					if (DMZGeneralConfig.SHOULD_KAMEHOUSE_SPAWN.get()) cap.generateRoshiHouseStructure(serverLevel);
				});

				serverLevel.getCapability(DragonBallGenProvider.CAPABILITY).ifPresent(cap -> cap.loadFromSavedData(serverLevel));
			}
			if (serverLevel.dimension() == ModDimensions.NAMEK_DIM_LEVEL_KEY) {
				LazyOptional<StructuresCapability> capability = serverLevel.getCapability(StructuresProvider.CAPABILITY);
				capability.ifPresent(cap -> {
					if (DMZGeneralConfig.SHOULD_ELDERGURU_SPAWN.get()) cap.generateElderGuru(serverLevel);
				});

				serverLevel.getCapability(NamekDragonBallGenProvider.CAPABILITY).ifPresent(cap -> cap.loadFromSavedData(serverLevel));
			}
			if (serverLevel.dimension() == ModDimensions.TIME_CHAMBER_DIM_LEVEL_KEY) {
				LazyOptional<StructuresCapability> capability = serverLevel.getCapability(StructuresProvider.CAPABILITY);
				capability.ifPresent(cap -> {
					cap.generateHabTiempoStructure(serverLevel);
				});
			}
			if (serverLevel.dimension() == ModDimensions.NAMEK_DIM_LEVEL_KEY) {
				LazyOptional<StructuresCapability> capability = serverLevel.getCapability(StructuresProvider.CAPABILITY);
				capability.ifPresent(cap -> {
					serverLevel.getServer().tell(new TickTask(serverLevel.getServer().getTickCount() + 200, () -> {
						if (DMZGeneralConfig.SHOULD_ELDERGURU_SPAWN.get()) cap.generateElderGuru(serverLevel);
					}));
				});

				serverLevel.getCapability(NamekDragonBallGenProvider.CAPABILITY).ifPresent(cap -> cap.loadFromSavedData(serverLevel));
			}
			if (serverLevel.dimension() == ModDimensions.OTHERWORLD_DIM_LEVEL_KEY) {
				LazyOptional<StructuresCapability> capability = serverLevel.getCapability(StructuresProvider.CAPABILITY);
				capability.ifPresent(cap -> {
					if (DMZGeneralConfig.OTHERWORLD_ENABLED.get()) cap.generatePalacioEnma(serverLevel);
				});
			}
		}
	}

	@SubscribeEvent
	public void onPlayerDeath(LivingDeathEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		if (!DMZGeneralConfig.OTHERWORLD_ENABLED.get()) return;

		ServerLevel level = player.serverLevel();
		if (level.dimension() != ModDimensions.OTHERWORLD_DIM_LEVEL_KEY) {
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(stats -> {
				if (stats.getBoolean("dmzuser")) {
					stats.setBoolean("alive", false);
					if (stats.getBoolean("transform")) stats.setBoolean("transform", false);
					if (stats.getBoolean("turbo")) stats.setBoolean("turbo", false);
					if (stats.getBoolean("aura")) stats.setBoolean("aura", false);
					if (stats.getBoolean("porungarevive")) stats.setBoolean("porungarevive", false);
					if (stats.getBoolean("shenronrevive")) stats.setBoolean("shenronrevive", false);
					if (stats.getBoolean("descend")) stats.setBoolean("descend", false);
				}
				if (DMZGeneralConfig.SAVE_INVENTORY.get()) {
					PlayerInventoryManager inventoryManager = PlayerInventoryManager.get(level);

					// Guardar el inventario actual en el almacenamiento principal
					ListTag mainInventory = new ListTag();
					player.getInventory().save(mainInventory);
					inventoryManager.setMainInventory(player.getUUID(), mainInventory);

					// Limpiar el inventario del jugador
					player.getInventory().clearContent();

					// Asignar el inventario del Otherworld
					ListTag otherworldInventory = inventoryManager.getOtherworldInventory(player.getUUID());
					player.getInventory().load(otherworldInventory);

					player.inventoryMenu.broadcastChanges();
				}
			});
		}
	}

	@SubscribeEvent
	public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (!DMZGeneralConfig.OTHERWORLD_ENABLED.get()) return;
		if (event.getEntity() instanceof ServerPlayer player) {
			ServerLevel otherWorld = player.server.getLevel(ModDimensions.OTHERWORLD_DIM_LEVEL_KEY);
			ServerLevel level = player.serverLevel();

			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(stats -> {
				if (stats.getBoolean("dmzuser")) {
					BlockPos spawnPos = new BlockPos(0, 41, -101); // Ajusta la posición de spawn
					player.setRespawnPosition(otherWorld.dimension(), spawnPos, 0.0F, true, false);
					player.teleportTo(otherWorld, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 0, 0);

					if (stats.getBoolean("transform")) stats.setBoolean("transform", false);
					if (stats.getBoolean("turbo")) stats.setBoolean("turbo", false);
					if (stats.getBoolean("aura")) stats.setBoolean("aura", false);
					if (stats.getBoolean("porungarevive")) stats.setBoolean("porungarevive", false);
					if (stats.getBoolean("shenronrevive")) stats.setBoolean("shenronrevive", false);
					if (stats.getBoolean("descend")) stats.setBoolean("descend", false);
				}
			});
		}
	}

	@SubscribeEvent
	public static void onChatMessage(ServerChatEvent event) {
		ServerPlayer player = event.getPlayer();
		String message = event.getMessage().getString();

		DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(stats -> {
			if (stats.getBoolean("porungarevive")) { // Verifica si el jugador está seleccionando nombres
				List<String> porungaNames = Arrays.stream(message.split(","))
						.map(String::trim)
						.limit(3) // Máximo 3 nombres
						.toList();

				player.level().getEntities(player, player.getBoundingBox().inflate(50), entity ->
						entity.getType() == MainEntity.PORUNGA.get()).forEach(entity -> {
					if (entity instanceof PorungaEntity porunga) {
						porunga.setDeseos(0);
					}
				});

				reviveOthers(player, porungaNames, "Porunga");
				stats.setBoolean("porungarevive", false);
				event.setCanceled(true);
			} else if (stats.getBoolean("shenronrevive")) {
				List<String> names = Arrays.stream(message.split(","))
						.map(String::trim)
						.limit(3) // Máximo 3 nombres
						.toList();

				player.level().getEntities(player, player.getBoundingBox().inflate(50), entity ->
						entity.getType() == MainEntity.SHENLONG.get()).forEach(entity -> {
					if (entity instanceof ShenlongEntity shenlong) {
						shenlong.setDeseos(0);
					}
				});

				reviveOthers(player, names, "Shenron");
				stats.setBoolean("shenronrevive", false);
				event.setCanceled(true);
			}
		});

	}

	@SubscribeEvent
	public static void onPlayerHit(LivingHurtEvent event) {
		if (event.getSource().getEntity() instanceof Player && (event.getEntity().level() instanceof ServerLevel serverLevel)) {
			serverLevel.sendParticles(MainParticles.HIT_ATTACK_PARTICLE.get(),
					event.getEntity().getX(), event.getEntity().getY() + 1, event.getEntity().getZ(),
					1, 0.2, 0.2, 0.2, 0.01
			);
		}
	}

	private static void reviveOthers(ServerPlayer player, List<String> playerNames, String dragon) {
		MinecraftServer server = player.getServer();
		if (server != null) {
			for (String name : playerNames) {
				ServerPlayer target = server.getPlayerList().getPlayerByName(name);
				if (target != null) {
					DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, target).ifPresent(stats -> {
						if (!stats.getBoolean("alive")) {
							stats.setBoolean("alive", true);
							stats.setIntValue("babaalivetimer", 0);
							stats.setIntValue("babacooldown", 0);

							if (dragon.equals("Shenron")) {
								target.sendSystemMessage(Component.translatable("lines.shenron.revive.revived", player.getName().getString()));
								player.sendSystemMessage(Component.translatable("lines.shenron.revive.success", name));
							} else {
								target.sendSystemMessage(Component.translatable("lines.porunga.revive.revived", player.getName().getString()));
								player.sendSystemMessage(Component.translatable("lines.porunga.revive.success", name));
							}
						} else {
							if (dragon.equals("Shenron")) {
								player.sendSystemMessage(Component.translatable("lines.shenron.revive.fail", name));
							} else {
								player.sendSystemMessage(Component.translatable("lines.porunga.revive.fail", name));
							}
						}
					});
				} else {
					if (dragon.equals("Shenron")) {
						player.sendSystemMessage(Component.translatable("lines.shenron.revive.fail", name));
					} else {
						player.sendSystemMessage(Component.translatable("lines.porunga.revive.fail", name));
					}
				}
			}
		}
	}

	private void spawnDragonBall(ServerLevel serverWorld, BlockState dragonBall, int dBallNum) {
		//Spawn the dragon balls
		BlockPos spawnPos = serverWorld.getSharedSpawnPos();
		Random random = new Random();
		int range = DMZGeneralConfig.DBALL_SPAWN_RANGE.get();

		BlockPos posicionValida = new BlockPos(0, 0, 0); // Posición válida inicializada a 0, 0, 0

		while (posicionValida.equals(new BlockPos(0, 0, 0))) {
			// Generar posición aleatoria dentro de un rango de Xk bloques desde el spawn
			int x = spawnPos.getX() + random.nextInt(range * 2) - range;
			int z = spawnPos.getZ() + random.nextInt(range * 2) - range;

			serverWorld.getChunk(x >> 4, z >> 4); // Cargar el chunk

			// Obtener la altura del terreno en esa posición
			int y = serverWorld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos posiblePos = new BlockPos(x, y, z);

			BlockState belowBlockState = serverWorld.getBlockState(posiblePos.below()); // Bloque debajo de la posición
			BlockState belowBelowBlockState = serverWorld.getBlockState(posiblePos.below().below()); // Bloque debajo del bloque anterior

			// Validar que la posición no esté en agua ni aire
			if (!belowBlockState.isAir() && !(belowBlockState.getBlock() == Blocks.WATER) &&
					!belowBelowBlockState.isAir() && !(belowBelowBlockState.getBlock() == Blocks.WATER)) {
				posicionValida = posiblePos; // Si es válida, asignamos la posición
			}
		}

		// Place a Dragon Ball block at the generated position
		serverWorld.setBlock(posicionValida, dragonBall, 2);
		LOGGER.info("[FirstSpawn] Dragon Ball [{}] spawned at {}", dBallNum, posicionValida);

		dragonBallPositions.add(posicionValida);
	}

	private void spawnNamekDragonBall(ServerLevel serverWorld, BlockState namekDragonBall, int dBallNum) {
		//Spawn the dragon balls
		BlockPos spawnPos = serverWorld.getSharedSpawnPos();
		Random random = new Random();
		int range = DMZGeneralConfig.DBALL_SPAWN_RANGE.get();

		BlockPos posicionValida = new BlockPos(0, 0, 0); // Posición válida inicializada a 0, 0, 0

		while (posicionValida.equals(new BlockPos(0, 0, 0))) {
			// Generar posición aleatoria dentro de un rango de Xk bloques desde el spawn
			int x = spawnPos.getX() + random.nextInt(range * 2) - range;
			int z = spawnPos.getZ() + random.nextInt(range * 2) - range;

			serverWorld.getChunk(x >> 4, z >> 4); // Cargar el chunk

			// Obtener la altura del terreno en esa posición
			int y = serverWorld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos posiblePos = new BlockPos(x, y, z);

			BlockState belowBlockState = serverWorld.getBlockState(posiblePos.below()); // Bloque debajo de la posición
			BlockState belowBelowBlockState = serverWorld.getBlockState(posiblePos.below().below()); // Bloque debajo del bloque anterior

			// Validar que la posición no esté en agua ni aire
			if (!belowBlockState.isAir() && !(belowBlockState.getBlock() == Blocks.WATER) && !(belowBelowBlockState.getBlock() == MainBlocks.NAMEK_WATER_LIQUID.get()) &&
					!belowBelowBlockState.isAir() && !(belowBelowBlockState.getBlock() == Blocks.WATER) && !(belowBelowBlockState.getBlock() == MainBlocks.NAMEK_WATER_LIQUID.get())) {
				posicionValida = posiblePos; // Si es válida, asignamos la posición
			}
		}

		// Place a Dragon Ball block at the generated position
		serverWorld.setBlock(posicionValida, namekDragonBall, 2);
		LOGGER.info("[FirstSpawn] Namekian Dragon Ball [{}] spawned at {}", dBallNum, posicionValida);

		namekDragonBallPositions.add(posicionValida);
	}

}
