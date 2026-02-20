package com.dragonminez.common.events;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.compat.WorldGuardCompat;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.armor.DbzArmorItem;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.init.entities.PunchMachineEntity;
import com.dragonminez.common.init.entities.ki.KiBarrierEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.SyncWishesS2C;
import com.dragonminez.common.quest.SagaManager;
import com.dragonminez.common.stats.Cooldowns;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.BetaWhitelist;
import com.dragonminez.common.util.ComboManager;
import com.dragonminez.common.wish.WishManager;
import com.dragonminez.server.DMZServer;
import com.dragonminez.server.commands.*;
import com.dragonminez.server.events.DragonBallsHandler;
import com.dragonminez.server.events.players.TickHandler;
import com.dragonminez.server.storage.StorageManager;
import com.dragonminez.server.util.FusionLogic;
import com.dragonminez.server.world.data.DragonBallSavedData;
import com.dragonminez.server.world.dimension.*;
import com.dragonminez.server.world.dimension.NamekDimension;
import com.dragonminez.server.world.dimension.OtherworldDimension;
import com.dragonminez.server.world.dimension.OtherworldRegionLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
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

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeCommonEvents {

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		String version = Reference.VERSION;
		if (version.contains("-beta") || version.contains("-alpha")) {
			Player player = event.getEntity();
			String username = player.getGameProfile().getName();

			if (!BetaWhitelist.isAllowed(username)) {
				LogUtil.error(Env.SERVER, "User {} tried to join but is not in the beta whitelist.", username);
				if (player instanceof ServerPlayer serverPlayer) {
					serverPlayer.connection.disconnect(Component.literal("§c[DragonMine Z]\n\n§7You are not allowed to play this Beta/Alpha version.\n§fPlease contact the developers if this is an error."));
				} else {
					throw new IllegalStateException("DMZ: User not allowed.");
				}
			}
		}

		if (event.getEntity() instanceof ServerPlayer player) {
			NetworkHandler.sendToPlayer(new SyncWishesS2C(WishManager.getAllWishes()), player);
			DragonBallsHandler.syncRadar(player.serverLevel());

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (ConfigManager.getServerConfig().getCombat().isKillPlayersOnCombatLogout()) {
					if (data.getCooldowns().hasCooldown(Cooldowns.COMBAT)) player.kill();
				}
				endFusionIfNeeded(player);
			});
		}
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (ConfigManager.getServerConfig().getCombat().isKillPlayersOnCombatLogout()) {
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

				if (ConfigManager.getServerConfig().getWorldGen().isOtherworldActive()) {
					if (data.getStatus().isAlive()) data.getCooldowns().addCooldown(Cooldowns.REVIVE_BABA, ConfigManager.getServerConfig().getGameplay().getReviveCooldownSeconds() * 20);
					if (data.getStatus().hasCreatedCharacter()) data.getStatus().setAlive(false);
					if (!data.getStatus().isInKaioPlanet()) data.getStatus().setInKaioPlanet(true);
					data.getEffects().removeAllEffects();
					data.getCooldowns().removeCooldown(Cooldowns.COMBAT);
					} else {
					data.getEffects().removeAllEffects();
				}

				if (data.getSkills().hasSkill("kaioken")) data.getSkills().setSkillActive("kaioken", false);
				data.getCooldowns().removeCooldown(Cooldowns.COMBAT);
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
			});
		}
	}

	@SubscribeEvent
	public static void onGamemodeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
		if (event.getEntity() instanceof ServerPlayer player && event.getNewGameMode() != GameType.SPECTATOR) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				endFusionIfNeeded(player);
			});
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

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        Entity target = event.getTarget();
        Player attacker = event.getEntity();
        Level level = attacker.level();

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            double x = target.getX();
            double y = target.getY() + (target.getBbHeight() * 0.65);
            double z = target.getZ();

            float[] rgb = ColorUtils.rgbIntToFloat(0xFFFFFF);

			if (target instanceof ServerPlayer targetPlayer) {
				StatsProvider.get(StatsCapability.INSTANCE, targetPlayer).ifPresent(targetData -> {
					if (!targetData.getStatus().isBlocking() && !((ServerPlayer) target).isCreative() && ComboManager.getCombo(attacker.getUUID()) != 4) {
						serverLevel.sendParticles(MainParticles.PUNCH_PARTICLE.get(), x, y, z, 0, rgb[0], rgb[1], rgb[2], 1.0);
					}
				});
			} else if (!(target instanceof MastersEntity) && !target.isInvulnerable() && !(target instanceof PunchMachineEntity)) {
				serverLevel.sendParticles(MainParticles.PUNCH_PARTICLE.get(), x, y, z, 0, rgb[0], rgb[1], rgb[2], 1.0);
			}

            RegistryObject<SoundEvent>[] sonidosGolpe = new RegistryObject[] {
                    MainSounds.GOLPE1,
                    MainSounds.GOLPE2,
                    MainSounds.GOLPE3,
                    MainSounds.GOLPE4,
                    MainSounds.GOLPE5,
                    MainSounds.GOLPE6
            };

            int indiceRandom = level.random.nextInt(sonidosGolpe.length);
            SoundEvent sonidoElegido = sonidosGolpe[indiceRandom].get();

			if (ComboManager.getCombo(attacker.getUUID()) != 4) {
				level.playSound(
						null, attacker.getX(), attacker.getY(), attacker.getZ(), sonidoElegido,
						SoundSource.PLAYERS,
						1.0F,
						1.0F
				);
			}
        }
    }

	@SubscribeEvent
	public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) DragonBallsHandler.syncRadar(player.serverLevel());
	}

	@SubscribeEvent
	public static void onServerStarting(ServerStartingEvent event) {
		StorageManager.init();
		BetaWhitelist.reload();
		WishManager.loadWishes(event.getServer());
		DMZPermissions.init();
		SagaManager.loadSagas(event.getServer());

		TickHandler.registerActionModeHandlers();
		TickHandler.registerStatusEffectHandlers();

		WorldGuardCompat.init();

		ServerLevel overworld = event.getServer().getLevel(Level.OVERWORLD);
		ServerLevel namek = event.getServer().getLevel(NamekDimension.NAMEK_KEY);
		ServerLevel otherworld = event.getServer().getLevel(OtherworldDimension.OTHERWORLD_KEY);

		if (otherworld != null) {
			LogUtil.info(Env.SERVER, "ServerStartingEvent: Attempting to load Otherworld regions.");
			OtherworldRegionLoader.loadPreGeneratedRegions(otherworld);
		}

		if (ConfigManager.getServerConfig().getWorldGen().isGenerateDragonBalls()) {
			if (overworld != null) {
				DragonBallSavedData data = DragonBallSavedData.get(overworld);
				if (!data.hasFirstSpawnHappened(false)) {
					DragonBallsHandler.scatterDragonBalls(overworld, false);
					data.setFirstSpawnHappened(false);
					LogUtil.info(Env.COMMON, "First DragonBalls Spawn setup for Earth.");
				}
			}

			if (namek != null) {
				DragonBallSavedData data = DragonBallSavedData.get(namek);
				if (!data.hasFirstSpawnHappened(true)) {
					DragonBallsHandler.scatterDragonBalls(namek, true);
					data.setFirstSpawnHappened(true);
					LogUtil.info(Env.COMMON, "First DragonBalls Spawn setup for Namek.");
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
			OtherworldRegionLoader.loadPreGeneratedRegions(otherworld);
		}
	}

	@SubscribeEvent
	public static void onLevelLoad(LevelEvent.Load event) {
		if (event.getLevel() instanceof ServerLevel serverLevel) {
			if (serverLevel.dimension().equals(OtherworldDimension.OTHERWORLD_KEY)) {
				LogUtil.info(Env.SERVER, "LevelEvent.Load: Asegurando regiones del Otherworld...");
				OtherworldRegionLoader.loadPreGeneratedRegions(serverLevel);
			}
		}
	}

	@SubscribeEvent
	public void onServerStopping(ServerStoppingEvent event) {
		StorageManager.shutdown();
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		DMZServer.registerCommands(event.getDispatcher());
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
                boolean isDbzArmor = newStack.getItem() instanceof DbzArmorItem;

                if (!isVanilla && !isDbzArmor) {
                    shouldBeArmored = true;
                }
            }
            if (stats.getCharacter().getArmored() != shouldBeArmored) {
                stats.getCharacter().setArmored(shouldBeArmored);
                NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
            }
        });
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();

        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }

        AABB searchArea = victim.getBoundingBox().inflate(3.0D);
        List<KiBarrierEntity> barriers = victim.level().getEntitiesOfClass(KiBarrierEntity.class, searchArea);

        for (KiBarrierEntity barrier : barriers) {
            if (barrier.getOwner() == victim) {


                event.setCanceled(true);

                victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                        MainSounds.BLOCK1.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

                return;
            }
        }
    }

	public static void endFusionIfNeeded(ServerPlayer player) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (data.getStatus().isFused() || data.getStatus().getFusionPartnerUUID() != null) {
				UUID partnerUUID = data.getStatus().getFusionPartnerUUID();
				if (partnerUUID != null) {
					ServerPlayer partner = player.getServer().getPlayerList().getPlayer(partnerUUID);
					if (partner != null) {
						if (!partner.isDeadOrDying()) partner.kill();
						StatsProvider.get(StatsCapability.INSTANCE, partner).ifPresent(partnerData -> {
							FusionLogic.endFusion(partner, partnerData, true);
						});
					}
				}
				FusionLogic.endFusion(player, data, true);
			}
		});
	}
}
