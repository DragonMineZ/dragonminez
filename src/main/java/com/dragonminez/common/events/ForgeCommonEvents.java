package com.dragonminez.common.events;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.alignment.NpcAlignmentRules;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.combat.logic.weapon.WeaponRegistry;
import com.dragonminez.common.combat.util.Player_DMZ;
import com.dragonminez.common.combat.weapon.WeaponAttributes;
import com.dragonminez.common.compat.WorldGuardCompat;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.init.CapsuleCorpMapTrade;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.init.MainEnchants;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainVillagers;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.armor.DbzArmorTextured;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.init.entities.PunchMachineEntity;
import com.dragonminez.common.init.entities.ki.KiBarrierEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.AppearanceSyncS2C;
import com.dragonminez.common.network.S2C.SyncWeaponRegistryS2C;
import com.dragonminez.common.spacepod.SpacePodDestinationRegistry;
import com.dragonminez.common.network.S2C.SyncWishesS2C;
import com.dragonminez.common.dragonball.DragonDefinitionReloadListener;
import com.dragonminez.common.wish.DragonWishRegistry;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.BetaWhitelist;
import com.dragonminez.common.wish.WishManager;
import com.dragonminez.server.DMZServer;
import com.dragonminez.server.commands.DMZPermissions;
import com.dragonminez.server.commands.LocateCommand;
import com.dragonminez.server.events.DragonBallsHandler;
import com.dragonminez.server.storage.StorageManager;
import com.dragonminez.server.util.FusionLogic;
import com.dragonminez.server.world.data.DragonBallSavedData;
import com.dragonminez.server.world.dimension.*;
import com.dragonminez.server.world.npc.NPCPlacementManager;
import com.dragonminez.server.world.structure.helper.DMZStructures;
import com.dragonminez.server.world.structure.helper.StructureLocator;
import com.mojang.brigadier.ParseResults;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.*;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.*;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeCommonEvents {

	@SubscribeEvent
	public static void onVillagerTrades(VillagerTradesEvent event) {
		if (!event.getType().equals(MainVillagers.CAPSULE_CORP_ASSISTANT.get())) {
			return;
		}

		Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

		// Nivel 1 (Novato)
		trades.get(1).add(mapTrade(DMZStructures.GOKU_HOUSE, "dragonminez.goku_house",
				new ItemStack(MainItems.DINO_MEAT_COOKED.get(), 10)));
		trades.get(1).add(mapTrade(DMZStructures.ROSHI_HOUSE, "dragonminez.roshi_house",
				new ItemStack(Items.COD, 8)));

		// Nivel 2 (Aprendiz)
		trades.get(2).add(mapTrade(DMZStructures.PICCOLO_HOUSE, "dragonminez.piccolo_house",
				new ItemStack(Items.WATER_BUCKET, 1)));
		trades.get(2).add(mapTrade(DMZStructures.YAMCHA_HOUSE, "dragonminez.yamcha_house",
				new ItemStack(Items.BONE, 1)));

		// Nivel 3 (Oficial)
		trades.get(3).add(mapTrade(DMZStructures.KAMILOOKOUT, "dragonminez.kamilookout",
				new ItemStack(Items.SPRUCE_SAPLING, 1)));
		trades.get(3).add(mapTrade(DMZStructures.CELL_ARENA, "dragonminez.cell_arena",
				new ItemStack(MainItems.T1_RADAR_CHIP.get(), 1)));

		// Nivel 4 (Experto)
		trades.get(4).add(mapTrade(DMZStructures.GERO_LAB, "dragonminez.gero_lab",
				new ItemStack(Items.IRON_INGOT, 16)));
		trades.get(4).add(mapTrade(DMZStructures.TRUNKS_SHIP, "dragonminez.trunks_ship",
				new ItemStack(Items.IRON_SWORD, 1)));

		// Nivel 5 (Maestro)
		trades.get(5).add(mapTrade(DMZStructures.VEGETA_POD, "dragonminez.vegeta_pod",
				new ItemStack(MainItems.RED_SCOUTER.get(), 1)));
		trades.get(5).add(mapTrade(DMZStructures.BABIDI, "dragonminez.babidi",
				new ItemStack(Items.ENDER_PEARL, 4)));
	}

	private static final int MAP_XP = 40;

	private static final int MAP_MAX_USES = 8;

	private static ItemStack emptyMap() {
		return new ItemStack(Items.MAP, 1);
	}

	private static VillagerTrades.ItemListing mapTrade(ResourceKey<Structure> destination, String displayName,
			ItemStack cost) {
		return new CapsuleCorpMapTrade(emptyMap(), cost, destination, displayName,
				MapDecoration.Type.RED_X, MAP_MAX_USES, MAP_XP);
	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		String version = Reference.VERSION;
		if (version.contains("-beta") || version.contains("-alpha")) {
			Player player = event.getEntity();
			String username = player.getGameProfile().getName();

			if (!BetaWhitelist.isAllowed(username)) {
				LogUtil.error(Env.SERVER, "User {} tried to join but is not in the beta whitelist.", username);
				if (player instanceof ServerPlayer serverPlayer) {
					serverPlayer.connection.disconnect(Component.literal("""
							§c[DragonMine Z]
							
							§7You are not allowed to play this Beta/Alpha version.
							§fAre you a §cPatreon§f? Use the §cVerify here§f button to connect Discord and request access automatically.
							
							§7If you have been recently whitelisted, restart Minecraft to apply the changes!
							§7Your Minecraft nickname is: §f""" + username));
				} else {
					throw new IllegalStateException("DMZ: User not allowed.");
				}
			}
		}

		if (event.getEntity() instanceof ServerPlayer player) {
			NetworkHandler.sendToPlayer(new SyncWishesS2C(WishManager.getAllWishes()), player);
			DragonBallsHandler.syncRadar(player.serverLevel());

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (ConfigManager.getCombatConfig().getKillPlayersOnCombatLogout()) {
					if (data.getCooldowns().hasCooldown(Cooldowns.COMBAT)) player.kill();
				}
				endFusionIfNeeded(player);
			});

			Map<String, WeaponAttributes> stringMap = new java.util.HashMap<>();
			WeaponRegistry.registrations.forEach((k, v) -> stringMap.put(k.toString(), v));

			String jsonRegistry = new com.google.gson.Gson().toJson(stringMap);
			NetworkHandler.sendToPlayer(new SyncWeaponRegistryS2C(jsonRegistry), player);
			SpacePodDestinationRegistry.syncToPlayer(player);
		}
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (ConfigManager.getCombatConfig().getKillPlayersOnCombatLogout()) {
					if (data.getCooldowns().hasCooldown(Cooldowns.COMBAT)) player.kill();
				}
				endFusionIfNeeded(player);
			});
		}
	}

	@SubscribeEvent
	public static void onPlayerDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			DragonBallsHandler.syncRadar(player.serverLevel());
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				endFusionIfNeeded(player);

				if (ConfigManager.getServerConfig().getWorldGen().getOtherworldActive()) {
					if (data.getStatus().isAlive())
						data.getCooldowns().addCooldown(Cooldowns.REVIVE_BABA, ConfigManager.getServerConfig().getGameplay().getReviveCooldownSeconds() * 20);
					if (data.getStatus().isHasCreatedCharacter()) data.getStatus().setAlive(false);
					if (!data.getStatus().isInKaioPlanet()) data.getStatus().setInKaioPlanet(true);
				}

				if (data.getSkills().hasSkill("kaioken") && data.getSkills().isSkillActive("kaioken"))
					data.getSkills().setSkillActive("kaioken", false);
				data.getCooldowns().removeCooldown(Cooldowns.COMBAT);
				data.getCharacter().clearActiveForm(player);
				data.getCharacter().clearActiveStackForm(player);
				data.getEffects().removeAllEffects();
				data.getSecondaryStatEffects().clear();
				player.refreshDimensions();
			});
		}
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (!data.getStatus().isAlive()) {
					ServerLevel otherworld = player.getServer().getLevel(OtherworldDimension.OTHERWORLD_KEY);
					player.teleportTo(otherworld, 0, 41, 10, 0, 0);
				}
				player.refreshDimensions();
			});
		}
	}

	@SubscribeEvent
	public static void onGamemodeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
		if (event.getEntity() instanceof ServerPlayer player && event.getNewGameMode() != GameType.SPECTATOR) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> endFusionIfNeeded(player));
		}
	}

	@SubscribeEvent
	public static void onBlockBreak(BlockEvent.BreakEvent event) {
		if (event.getPlayer() instanceof ServerPlayer player) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (data.getStatus().isBlocking()) event.setCanceled(true);
			});
		}
	}

	public static double getCriticalChance(Player player) {
		double chance = player.getAttributeValue(MainAttributes.CRIT_CHANCE.get());
		int chanceLevel = player.getMainHandItem().getEnchantmentLevel(MainEnchants.CRIT_CHANCE.get());
		if (chanceLevel > 0) chance += (chanceLevel * 0.05D);
		DMZEvent.CritChanceEvent event = new DMZEvent.CritChanceEvent(player, chance);
		MinecraftForge.EVENT_BUS.post(event);
		return event.getChance();
	}

	public static double getCriticalDamage(Player player) {
		double multiplier = player.getAttributeValue(MainAttributes.CRIT_DAMAGE.get());
		int damageLevel = player.getMainHandItem().getEnchantmentLevel(MainEnchants.CRIT_DAMAGE.get());
		if (damageLevel > 0) multiplier += (damageLevel * 0.05D);
		return multiplier;
	}

	@SubscribeEvent
	public static void onPlayerAttack(AttackEntityEvent event) {
		Entity target = event.getTarget();
		Player attacker = event.getEntity();
		Level level = attacker.level();

		if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
			TargetHelper.Relation relation = TargetHelper.getRelation(attacker, target);
			if (!TargetHelper.canAttack(attacker, target, attacker.distanceTo(target) + 1.0D)) {
				event.setCanceled(true);
				return;
			}
			TargetHelper.onSuccessfulAttack(attacker, target, relation);

			double chance = getCriticalChance(attacker);
			boolean isCrit = ((Player_DMZ) attacker).rollAndGetCriticalStatus(chance);

			boolean isValidTarget = false;
			if (target instanceof ServerPlayer targetPlayer) {
				isValidTarget = StatsProvider.get(StatsCapability.INSTANCE, targetPlayer)
						.map(targetData -> !targetData.getStatus().isBlocking() && !targetPlayer.isCreative())
						.orElse(false);
			} else if (!(target instanceof MastersEntity) && !target.isInvulnerable() && !(target instanceof PunchMachineEntity)) {
				isValidTarget = true;
			}

			if (isValidTarget) {
				double x = target.getX();
				double y = target.getY() + (target.getBbHeight() * 0.65);
				double z = target.getZ();
				float[] rgb = ColorUtils.rgbIntToFloat(0xFFFFFF);

				if (isCrit) {
					//serverLevel.sendParticles(MainParticles.CRIT_PARTICLE.get(), x, y, z, 0, rgb[0], rgb[1], rgb[2], 1.0);}
				}
				else serverLevel.sendParticles(MainParticles.PUNCH_PARTICLE.get(), x, y, z, 0, rgb[0], rgb[1], rgb[2], 1.0);
			}

			if (isCrit) {
				RegistryObject<SoundEvent>[] sonidosCritico = new RegistryObject[]{
						MainSounds.CRITICO1, MainSounds.CRITICO2
				};
				int indiceRandom = level.random.nextInt(sonidosCritico.length);
				SoundEvent sonidoCritico = sonidosCritico[indiceRandom].get();
				attacker.playNotifySound(sonidoCritico, SoundSource.PLAYERS, 1.0F, 1.0F);
			} else if (attacker.getMainHandItem() == ItemStack.EMPTY || attacker.getMainHandItem().is(Items.AIR)) {
				RegistryObject<SoundEvent>[] sonidosGolpe = new RegistryObject[]{
						MainSounds.GOLPE1,
						MainSounds.GOLPE2,
						MainSounds.GOLPE3,
						MainSounds.GOLPE4,
						MainSounds.GOLPE5,
						MainSounds.GOLPE6
				};

				int indiceRandom = level.random.nextInt(sonidosGolpe.length);
				SoundEvent sonidoElegido = sonidosGolpe[indiceRandom].get();
				attacker.playNotifySound(sonidoElegido, SoundSource.PLAYERS, 1.0F, 1.0F);
			}
		}
	}

	@SubscribeEvent
	public static void onMeleeCriticalHit(CriticalHitEvent event) {
		Player player = event.getEntity();
		if (player == null || player.level().isClientSide) return;

		double chance = getCriticalChance(player);
		boolean isCrit = ((Player_DMZ) player).rollAndGetCriticalStatus(chance);

		if (isCrit) {
			event.setDamageModifier((float) getCriticalDamage(player));
			event.setResult(Event.Result.ALLOW);
		} else event.setResult(Event.Result.DENY);
	}

	@SubscribeEvent
	public static void onRangedCriticalHit(LivingHurtEvent event) {
		if (event.getSource().getDirectEntity() instanceof AbstractArrow && event.getSource().getEntity() instanceof Player player) {
			if (player.level().isClientSide) return;

			double chance = getCriticalChance(player);
			boolean isCrit = ((Player_DMZ) player).rollAndGetCriticalStatus(chance);

			if (isCrit) event.setAmount((float) (event.getAmount() * getCriticalDamage(player)));
		}
	}

	@SubscribeEvent
	public static void suppressVanillaCritSounds(PlayLevelSoundEvent.AtPosition event) {
		if (event.getSound() == SoundEvents.PLAYER_ATTACK_CRIT) event.setCanceled(true);
	}

	private static final UUID WEAPON_CRIT_CHANCE_UUID = UUID.fromString("c37e2055-7783-4559-b18a-1b3e6f5a0410");
	private static final UUID WEAPON_CRIT_DAMAGE_UUID = UUID.fromString("496f2714-a61f-41e2-aed5-265e7fa0e0fa");

	@SubscribeEvent
	public static void attachDynamicWeaponAttributes(ItemAttributeModifierEvent event) {
		if (event.getSlotType() == EquipmentSlot.MAINHAND) {
			ItemStack stack = event.getItemStack();

			WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);

			if (attributes != null) {
				double weaponCritChance = attributes.getSafeCritChance();
				double weaponCritDamage = attributes.getSafeCritDamage();

				if (weaponCritChance > 0.0D) {
					event.addModifier(MainAttributes.CRIT_CHANCE.get(), new AttributeModifier(
							WEAPON_CRIT_CHANCE_UUID,
							"Weapon innate crit chance",
							weaponCritChance,
							AttributeModifier.Operation.ADDITION
					));
				}

				if (weaponCritDamage > 0.0D) {
					event.addModifier(MainAttributes.CRIT_DAMAGE.get(), new AttributeModifier(
							WEAPON_CRIT_DAMAGE_UUID,
							"Weapon innate crit damage",
							weaponCritDamage,
							AttributeModifier.Operation.ADDITION
					));
				}
			}
		}
	}

	private static String fusionDisplayName(Player player) {
		return StatsProvider.get(StatsCapability.INSTANCE, player)
				.map(data -> data.getStatus().isFused() ? data.getStatus().getFusionName() : "")
				.filter(name -> name != null && !name.isEmpty())
				.orElse(null);
	}

	@SubscribeEvent
	public static void onPlayerNameFormat(PlayerEvent.NameFormat event) {
		String fusionName = fusionDisplayName(event.getEntity());
		if (fusionName != null) event.setDisplayname(Component.literal(fusionName));
	}

	@SubscribeEvent
	public static void onPlayerTabListNameFormat(PlayerEvent.TabListNameFormat event) {
		String fusionName = fusionDisplayName(event.getEntity());
		if (fusionName != null) event.setDisplayName(Component.literal(fusionName));
	}

	@SubscribeEvent
	public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) DragonBallsHandler.syncRadarForPlayer(player);
	}

	@SubscribeEvent
	public static void onServerAboutToStart(net.minecraftforge.event.server.ServerAboutToStartEvent event) {
		if (ConfigManager.getServerConfig().getWorldGen().getOtherworldActive()) {
			OtherworldRegionLoader.loadPreGeneratedRegions(event.getServer());
		}
	}

	@SubscribeEvent
	public static void onServerStarting(ServerStartingEvent event) {
		StorageManager.init();
		BetaWhitelist.reload();
		WishManager.loadWishes(event.getServer());
		DMZPermissions.init();
		QuestRegistry.loadAll(event.getServer());
		NpcAlignmentRules.load(event.getServer());
		NPCPlacementManager.load(event.getServer());

		WorldGuardCompat.init();

		if (ConfigManager.getServerConfig().getWorldGen().getGenerateDragonBalls()) {
			for (var definition : DragonBallDefinitions.getBallSets()) {
				ServerLevel targetLevel = event.getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, definition.getValidDimensions().iterator().next()));
				if (targetLevel == null) continue;
				DragonBallSavedData data = DragonBallSavedData.get(targetLevel);
				if (!data.isFirstSpawnComplete(definition.getId())) {
					DragonBallsHandler.scatterDragonBalls(targetLevel, definition.getId());
					LogUtil.info(Env.COMMON, "First DragonBalls Spawn setup for set: " + definition.getId());
				}
			}
		} else {
			LogUtil.info(Env.COMMON, "DragonBalls generation is disabled in the config.");
		}
	}

	@SubscribeEvent
	public static void onServerStarted(ServerStartedEvent event) {
		ServerLevel otherworld = event.getServer().getLevel(OtherworldDimension.OTHERWORLD_KEY);

		if (otherworld != null) {
			LogUtil.info(Env.SERVER, "ServerStartedEvent: Attempting to load Otherworld regions, double-checking dimension presence.");
			OtherworldRegionLoader.loadPreGeneratedRegions(event.getServer());
		}
		NPCPlacementManager.spawnForLoadedLevels(event.getServer());
	}

	@SubscribeEvent
	public static void onLevelLoad(LevelEvent.Load event) {
		if (event.getLevel() instanceof ServerLevel serverLevel) {
			try {
				var chunkSource = serverLevel.getChunkSource();
				com.dragonminez.server.world.structure.placement.StructureSpawnPlanner.prewarm(
						serverLevel.getSeed(), chunkSource.randomState(), chunkSource.getGeneratorState());
			} catch (Throwable ignored) {
			}

			if (serverLevel.dimension().equals(OtherworldDimension.OTHERWORLD_KEY)) {
				LogUtil.info(Env.SERVER, "LevelEvent.Load: Detected Otherworld dimension load, attempting to load regions.");
				OtherworldRegionLoader.loadPreGeneratedRegions(serverLevel.getServer());
			}
		}
	}

	@SubscribeEvent
	public void onServerStopping(ServerStoppingEvent event) {
		StorageManager.shutdown();
		com.dragonminez.server.world.structure.placement.StructureSpawnPlanner.reset();
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		DMZServer.registerCommands(event.getDispatcher());
	}

	@SubscribeEvent
	public static void onLocateCommand(CommandEvent event) {
		try {
			ParseResults<CommandSourceStack> parse = event.getParseResults();
			var nodes = parse.getContext().getNodes();
			if (nodes.size() < 3) return;
			if (!nodes.get(0).getNode().getName().equals("locate")) return;
			if (!nodes.get(1).getNode().getName().equals("structure")) return;

			String argument = nodes.get(2).getRange().get(parse.getReader().getString());
			if (argument.isEmpty() || argument.charAt(0) == '#') return;

			ResourceLocation id = ResourceLocation.tryParse(argument);
			if (id == null || !id.getNamespace().equals(Reference.MOD_ID)) return;

			CommandSourceStack source = parse.getContext().getSource();
			ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, id);

			if (!StructureLocator.usesCustomPlacement(source.getLevel(), key)) return;

			event.setCanceled(true);
			LocateCommand.locate(source, key);
		} catch (Exception ignored) {}
	}

	@SubscribeEvent
	public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
		Mob mob = event.getEntity();
		if (mob.getType().getCategory() == MobCategory.MONSTER) {
			List<MastersEntity> masters = mob.level().getEntitiesOfClass(MastersEntity.class,
					new AABB(mob.blockPosition()).inflate(80));

			if (!masters.isEmpty() && !mob.level().dimension().equals(HTCDimension.HTC_KEY)) {
				event.setSpawnCancelled(true);
				event.setResult(Event.Result.DENY);
			}
		}
	}

	@SubscribeEvent
	public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}

		if (event.getSlot() != EquipmentSlot.CHEST) {
			return;
		}

		ItemStack newStack = event.getTo();

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {

			boolean shouldBeArmored = false;

			if (!newStack.isEmpty() && newStack.getItem() instanceof ArmorItem) {
				boolean isVanilla = ForgeRegistries.ITEMS.getKey(newStack.getItem()).getNamespace().equals("minecraft");
				boolean isDbzArmor = newStack.getItem() instanceof DbzArmorTextured;

				if (!isVanilla && !isDbzArmor) {
					shouldBeArmored = true;
				}
			}
			if (stats.getCharacter().getArmored() != shouldBeArmored) {
				stats.getCharacter().setArmored(shouldBeArmored);
				NetworkHandler.sendToTrackingEntityAndSelf(new AppearanceSyncS2C(player), player);
			}
		});
	}

	@SubscribeEvent
	public static void onLivingAttack(LivingAttackEvent event) {
		LivingEntity victim = event.getEntity();
		if (isCharacterCreationProtected(victim)) {
			event.setCanceled(true);
			return;
		}
		if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
		if (!(victim instanceof Player) && !(victim instanceof DBSagasEntity)) return;

		AABB searchArea = victim.getBoundingBox().inflate(3.0D);
		List<KiBarrierEntity> barriers = victim.level().getEntitiesOfClass(KiBarrierEntity.class, searchArea);

		for (KiBarrierEntity barrier : barriers) {
			if (barrier.isActive() && barrier.protects(victim)) {
				event.setCanceled(true);
				barrier.absorbDamage(event.getAmount(), event.getSource().getEntity());
				return;
			}
		}
	}

	@SubscribeEvent
	public static void onLivingDamage(LivingDamageEvent event) {
		if (isCharacterCreationProtected(event.getEntity())) {
			event.setCanceled(true);
		}
	}

	private static boolean isCharacterCreationProtected(LivingEntity entity) {
		if (!ConfigManager.getServerConfig().getGameplay().getForceCharacterCreation()) return false;
		if (!(entity instanceof ServerPlayer player)) return false;
		final boolean[] protectedPlayer = {false};
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) protectedPlayer[0] = true;
		});
		return protectedPlayer[0];
	}

	public static void endFusionIfNeeded(ServerPlayer player) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (data.getStatus().isFused() || data.getStatus().getFusionPartnerUUID() != null) {
				UUID partnerUUID = data.getStatus().getFusionPartnerUUID();
				if (partnerUUID != null) {
					ServerPlayer partner = player.getServer().getPlayerList().getPlayer(partnerUUID);
					if (partner != null) {
						if (!partner.isDeadOrDying()) partner.kill();
						StatsProvider.get(StatsCapability.INSTANCE, partner).ifPresent(partnerData -> FusionLogic.endFusion(partner, partnerData, true));
					}
				}
				FusionLogic.endFusion(player, data, true);
			}
		});
	}

	@SubscribeEvent
	public static void onAddReloadListeners(AddReloadListenerEvent event) {
		event.addListener(SpacePodDestinationRegistry.INSTANCE);
		event.addListener(DragonDefinitionReloadListener.INSTANCE);
		event.addListener(DragonWishRegistry.INSTANCE);
		event.addListener(new SimplePreparableReloadListener<Void>() {
			@Override
			protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
				return null;
			}

			@Override
			protected void apply(Void dummy, ResourceManager resourceManager, ProfilerFiller profiler) {
				WeaponRegistry.loadAttributes(resourceManager);
			}
		});
	}

	@SubscribeEvent
	public static void onHandSwap(LivingSwapItemsEvent.Hands event) {
		if (event.getEntity() instanceof Player player) {
			var offHandStack = player.getOffhandItem();
			event.setItemSwappedToOffHand(player.getMainHandItem());
			event.setItemSwappedToMainHand(offHandStack);
		}
	}
}
