package com.yuseix.dragonminez.events.characters;

import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.client.hud.spaceship.SaiyanSpacePodOverlay;
import com.yuseix.dragonminez.config.DMZGeneralConfig;
import com.yuseix.dragonminez.config.races.DMZColdDemonConfig;
import com.yuseix.dragonminez.events.ClientEvents;
import com.yuseix.dragonminez.events.StoryEvents;
import com.yuseix.dragonminez.init.MainFluids;
import com.yuseix.dragonminez.init.MainSounds;
import com.yuseix.dragonminez.init.entity.custom.NaveSaiyanEntity;
import com.yuseix.dragonminez.init.entity.custom.namek.NamekianEntity;
import com.yuseix.dragonminez.init.entity.custom.namek.SoldierEntity;
import com.yuseix.dragonminez.network.C2S.SpacePodC2S;
import com.yuseix.dragonminez.network.ModMessages;
import com.yuseix.dragonminez.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.stats.DMZStatsProvider;
import com.yuseix.dragonminez.stats.storymode.DMZQuest;
import com.yuseix.dragonminez.stats.storymode.DMZStoryCapability;
import com.yuseix.dragonminez.utils.Keys;
import com.yuseix.dragonminez.world.*;
import com.yuseix.dragonminez.worldgen.dimension.ModDimensions;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Mod.EventBusSubscriber(modid = DragonMineZ.MOD_ID)
public class EntityEvents {

	@SubscribeEvent
	public static void mobDeath(LivingDeathEvent event) {
		// Verificar si la entidad muerta es un aldeano, namekianentity,etc
		if (event.getEntity() instanceof NamekianEntity || event.getEntity() instanceof Villager || event.getEntity() instanceof Player) {

			// Verificar si el atacante es un jugador
			if (event.getSource().getEntity() instanceof Player) {
				Player player = (Player) event.getSource().getEntity();


				DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
					boolean isDmzUser = cap.getBoolean("dmzuser");

					if (isDmzUser) cap.removeIntValue("alignment", 5); //Remover puntos te hace maligno
				});

				player.displayClientMessage(Component.translatable("lines.alignment.evil"), true);

			}
		}
		//Aca es para ganar puntos de bondad xxx claro pe tilin
		if (event.getEntity() instanceof SoldierEntity || event.getEntity() instanceof Monster) {

			if (event.getSource().getEntity() instanceof Player) {
				Player player = (Player) event.getSource().getEntity();

				DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
					boolean isDmzUser = cap.getBoolean("dmzuser");

					if (isDmzUser) cap.addIntValue("alignment", 2); //Agregar puntos te hace bueno
				});

			}
		}

		//Pa ganar tps cuando mates algo claro pes papeto
		if (esEnemigo(event.getEntity())) {
			if (event.getSource().getEntity() instanceof Player) {
				Player player = (Player) event.getSource().getEntity();
				var vidaTps = (int) (event.getEntity().getMaxHealth() * DMZGeneralConfig.PERKILL_ZPOINTS_GAIN.get());
				var calculoTps = (int) Math.round((10 + vidaTps) * DMZGeneralConfig.MULTIPLIER_ZPOINTS_GAIN.get());

				// multiplicar si está en la hab del tiempo pes
				if (player.level().dimension().equals(ModDimensions.TIME_CHAMBER_DIM_LEVEL_KEY)) {
					calculoTps *= DMZGeneralConfig.MULTIPLIER_ZPOINTS_HTC.get();
				}

				int finalCalculoTps = calculoTps;

				DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
					boolean isDmzUser = cap.getBoolean("dmzuser");
					int finalTps = Math.round(finalCalculoTps);

					if (cap.getIntValue("race") == 4)  finalTps *= DMZColdDemonConfig.TP_MULTIPLER_PASSIVE.get();

					if (isDmzUser) cap.addIntValue("tps", finalTps);
				});
			}
		}

		// Eliminar la marca majin
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
				cap.removePermanentEffect("majin");
			});

			LivingEntity killer = null;
			if (event.getSource().getEntity() instanceof LivingEntity) killer = (LivingEntity) event.getSource().getEntity();

			String deathMessage = getCustomDeathMessage(killer);
			if (deathMessage != null) {
				String playerName = player.getDisplayName().getString();
				String killerName = getEntityName(killer);
				player.getCommandSenderWorld().players().forEach(p -> p.sendSystemMessage(Component.translatable(deathMessage, playerName, killerName)));
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public static void onEntityHit(LivingHurtEvent event) {

		if (event.getSource().getEntity() instanceof Player) {
			Player player = (Player) event.getSource().getEntity();

			double baseTps = DMZGeneralConfig.PERHIT_ZPOINTS_GAIN.get() * DMZGeneralConfig.MULTIPLIER_ZPOINTS_GAIN.get();

			// multiplicar si está en la hab del tiempo pes
			if (player.level().dimension().equals(ModDimensions.TIME_CHAMBER_DIM_LEVEL_KEY)) {
				baseTps *= DMZGeneralConfig.MULTIPLIER_ZPOINTS_HTC.get();
			}

			double finalBaseTps = baseTps;

			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
				boolean isDmzUser = cap.getBoolean("dmzuser");
				int finalTps = (int) Math.round(finalBaseTps);
				if (cap.getIntValue("race") == 4) finalTps = (int) (finalTps * DMZColdDemonConfig.TP_MULTIPLER_PASSIVE.get());
				if (isDmzUser) cap.addIntValue("tps", finalTps);
			});
		}
	}

	private static boolean esEnemigo(Entity entity) {
		List<Class<?>> listaEnemigos = List.of(
				Monster.class,
				Animal.class,
				Player.class,
				NamekianEntity.class,
				SoldierEntity.class,
				FlyingMob.class,
				Mob.class
		);
		return listaEnemigos.stream().anyMatch(clase -> clase.isInstance(entity));
	}

	private static final double HEAL_PERCENTAGE = 0.08; // 8% por segundo
	private static final int HEAL_TICKS = 3 * 20; // 3 segundos

	private static final Map<Player, Long> lastHealingTime = new WeakHashMap<>();

	@SubscribeEvent
	public static void onLivingTick(TickEvent.PlayerTickEvent event) {
		Player player = event.player;

		if (player.level().isClientSide || event.phase != TickEvent.Phase.END) return;

		ServerPlayer serverPlayer = (ServerPlayer) player;
		BlockPos playerPos = serverPlayer.blockPosition();
		ServerLevel level = (ServerLevel) player.level();

		if (player.level().dimension().equals(ServerLevel.OVERWORLD)) {
			level.getCapability(StructuresProvider.CAPABILITY).ifPresent(structures -> {
				if (structures.getHasTorreKami()) {
					BlockPos posKami = structures.getTorreKarinPosition(); // La torre de Karin está más abajo, asi que es más factible xd
					if (playerPos.distSqr(posKami) < 30000) grantAdvancement(serverPlayer, "kamilookout");
				}

				if (structures.getHasGokuHouse()) {
					BlockPos posGoku = structures.getGokuHousePosition();
					if (playerPos.distSqr(posGoku) < 10000) grantAdvancement(serverPlayer, "gokuhouse");
				}
				if (structures.getHasRoshiHouse()) {
					BlockPos posRoshi = structures.getRoshiHousePosition();
					if (playerPos.distSqr(posRoshi) < 10000) {
						grantAdvancement(serverPlayer, "roshihouse");
						player.getCapability(DMZStoryCapability.INSTANCE).ifPresent(cap -> {
							DMZQuest quest = cap.getAvailableQuest();
							if (quest.getId().equals("saiyQuest1")) {
								cap.setQuestCompletion(quest.getId(), true, player);
								StoryEvents.onQuestComplete(player, quest.getId());
							}
						});
					}
				}
			});
		} else if (player.level().dimension().equals(ModDimensions.OTHERWORLD_DIM_LEVEL_KEY)) {
			level.getCapability(StructuresProvider.CAPABILITY).ifPresent(structures -> {
				if (structures.getHasKaioPlanet()) {
					BlockPos posKaio = structures.getKaioPlanetPosition();
					if (playerPos.distSqr(posKaio) < 30000) grantAdvancement(serverPlayer, "kaiosama");
				}
			});
		} else if (player.level().dimension().equals(ModDimensions.NAMEK_DIM_LEVEL_KEY)) {
			level.getCapability(StructuresProvider.CAPABILITY).ifPresent(structures -> {
				if (structures.getHasElderGuru()) {
					BlockPos posNamek = structures.getElderGuruPosition();
					if (playerPos.distSqr(posNamek) < 100000) grantAdvancement(serverPlayer, "patriarca");
				}
			});
		}

		FluidState fluidState = player.level().getFluidState(player.blockPosition());

		if (fluidState.isEmpty()) {
			return;
		}

		if (fluidState.is(MainFluids.SOURCE_HEALING.get()) || fluidState.is(MainFluids.FLOWING_HEALING.get())) {
			long currentTime = player.level().getGameTime(); // Tiempo actual en ticks
			long lastHealTime = lastHealingTime.getOrDefault(player, 0L);

			if (currentTime - lastHealTime >= HEAL_TICKS) {
				funcLiqCurativo(player);
				lastHealingTime.put(player, currentTime); // Actualizar el último tiempo de curación
			}
		} else if (fluidState.is(MainFluids.SOURCE_NAMEK.get()) || fluidState.is(MainFluids.FLOWING_NAMEK.get())) {
			funcAguaNamek(player);
		}
	}

	private static void grantAdvancement(ServerPlayer player, String advancementPath) {
		Advancement advancementToGive = player.getServer().getAdvancements().getAdvancement(new ResourceLocation(DragonMineZ.MOD_ID, advancementPath));
		if (advancementToGive != null) {
			AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancementToGive);
			if (!progress.isDone()) {
				for (String criteria : progress.getRemainingCriteria()) {
					player.getAdvancements().award(advancementToGive, criteria);
				}
			}
		}
	}

	private static String getCustomDeathMessage(LivingEntity killer) {
		if (killer != null) {
			return switch (killer.getType().toString()) {
				case "entity.dragonminez.redribbon_soldier" -> "dmz.deathmessage.redribbon";
				case "entity.dragonminez.namek_warrior01", "entity.dragonminez.namek_warrior02" -> "dmz.deathmessage.namekwarrior";
				case "entity.dragonminez.soldier01", "entity.dragonminez.soldier02", "entity.dragonminez.soldier03" -> "dmz.deathmessage.soldier";
				case "entity.dragonminez.saga_raditz" -> "dmz.deathmessage.raditz";
				case "entity.dragonminez.saibaman", "entity.dragonminez.kaiwareman", "entity.dragonminez.kyukonman",
					 "entity.dragonminez.copyman", "entity.dragonminez.tennenman", "entity.dragonminez.jinkouman"  -> "dmz.deathmessage.saibaman";
				case "entity.dragonminez.saga_nappa" -> "dmz.deathmessage.nappa";
				case "entity.dragonminez.saga_vegeta" -> "dmz.deathmessage.vegeta";
				case "entity.dragonminez.saga_vegetaozaru" -> "dmz.deathmessage.vegetaozaru";
				default -> null;
			};
		} else return null;

	}

	private static String getEntityName(LivingEntity killer) {
		if (killer != null) {
			return switch (killer.getType().toString()) {
				case "entity.dragonminez.saibaman" -> "Saibaman";
				case "entity.dragonminez.kaiwareman" -> "Kaiwareman";
				case "entity.dragonminez.kyukonman" -> "Kyukonman";
				case "entity.dragonminez.copyman" -> "Copyman";
				case "entity.dragonminez.tennenman" -> "Tennenman";
				case "entity.dragonminez.jinkouman" -> "Jinkouman";
				default -> null;
			};
		} else return null;
	}

	private static void funcLiqCurativo(Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, serverPlayer).ifPresent(stats -> {
				int maxHp = stats.getIntValue("maxhealth");
				int healHp = (int) (maxHp * HEAL_PERCENTAGE);
				int maxKi = stats.getIntValue("maxenergy");
				int healKi = (int) (maxKi * HEAL_PERCENTAGE);
				boolean isDmzUser = stats.getBoolean("dmzuser");

				if (isDmzUser) {
					serverPlayer.heal(healHp);
					stats.addIntValue("curenergy", healKi);
				}

			});
		}

		// Apagar el fuego
		if (player.isOnFire()) {
			player.clearFire();
		}
	}

	private static void funcAguaNamek(Player player) {
		if (player.isOnFire()) {
			player.clearFire();
		}
	}
}




