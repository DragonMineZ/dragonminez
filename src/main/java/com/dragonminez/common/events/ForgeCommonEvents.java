package com.dragonminez.common.events;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.SyncWishesS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.BetaWhitelist;
import com.dragonminez.common.wish.WishManager;
import com.dragonminez.server.commands.*;
import com.dragonminez.server.events.DragonBallsHandler;
import com.dragonminez.server.storage.StorageManager;
import com.dragonminez.server.world.data.DragonBallSavedData;
import com.dragonminez.server.world.dimension.NamekDimension;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

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
		}
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			DragonBallsHandler.syncRadar(player.serverLevel());
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				player.setHealth(data.getMaxHealth());
				data.getResources().setCurrentEnergy(data.getMaxEnergy());
				data.getResources().setCurrentStamina(data.getMaxStamina());
			});
		}
	}

	@SubscribeEvent
	public static void onPlayerDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			DragonBallsHandler.syncRadar(player.serverLevel());
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (ConfigManager.getServerConfig().getWorldGen().isOtherworldActive()) {
					data.getStatus().setAlive(false);
					if (!data.getStatus().isInKaioPlanet()) data.getStatus().isInKaioPlanet();
				}
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

            serverLevel.sendParticles(MainParticles.PUNCH_PARTICLE.get(), x, y, z, 0, rgb[0], rgb[1], rgb[2], 1.0);

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

            level.playSound(
                    null, attacker.getX(), attacker.getY(), attacker.getZ(), sonidoElegido,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1.0F,
                    1.0F                      
            );
        }
    }

	@SubscribeEvent
	public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			DragonBallsHandler.syncRadar(player.serverLevel());
		}
	}

	@SubscribeEvent
	public static void onServerStarting(ServerStartingEvent event) {
		StorageManager.init();
		BetaWhitelist.reload();
		WishManager.loadWishes(event.getServer());
		DMZPermissions.init();

		ServerLevel overworld = event.getServer().getLevel(Level.OVERWORLD);
		ServerLevel namek = event.getServer().getLevel(NamekDimension.NAMEK_KEY);

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
	public void onServerStopping(ServerStoppingEvent event) {
		StorageManager.shutdown();
	}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        StatsCommand.register(event.getDispatcher());
        PointsCommand.register(event.getDispatcher());
        SkillsCommand.register(event.getDispatcher());
        EffectsCommand.register(event.getDispatcher());
        PartyCommand.register(event.getDispatcher());
        BonusCommand.register(event.getDispatcher());
        LocateCommand.register(event.getDispatcher());
		StoryCommand.register(event.getDispatcher());
		LogUtil.info(Env.COMMON, "Commands registered successfully.");
    }

	@SubscribeEvent
	public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
		Mob mob = event.getEntity();
		if (mob.getType().getCategory() == MobCategory.MONSTER) {
			List<MastersEntity> masters = mob.level().getEntitiesOfClass(MastersEntity.class,
					new AABB(mob.blockPosition()).inflate(40));

			if (!masters.isEmpty()) {
				event.setSpawnCancelled(true);
				event.setResult(Event.Result.DENY);
			}
		}
	}
}
