package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.entities.ShadowDummyEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class TPGainEvents {

	private static final Map<UUID, Vec3> lastPositions = new HashMap<>();
	private static final Map<UUID, Double> accumulatedDistance = new HashMap<>();

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onTPGain(DMZEvent.TPGainEvent event) {
		if (!(event.getPlayer() instanceof ServerPlayer player)) return;
		int baseTP = event.getTpGain();
		if (baseTP <= 0) return;
		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (data == null) return;
		int finalTP = data.calculateTPGain(baseTP);

		if (data.getStatus().isFused() && data.getStatus().isFusionLeader()) shareWithFusionPartner(player, data, finalTP);
		event.setTpGain(finalTP);
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (!data.getStatus().isHasCreatedCharacter()) return;

				int passiveTp = ConfigManager.getServerConfig().getGameplay().getPassiveTpGain();
				if (passiveTp > 0 && player.tickCount % 100 == 0) {
					data.getResources().addTrainingPoints(passiveTp);
				}

				int tpTravel = ConfigManager.getServerConfig().getGameplay().getTpPer20BlocksTraveled();
				if (tpTravel > 0) {
					UUID uuid = player.getUUID();
					Vec3 currentPos = player.position();
					Vec3 lastPos = lastPositions.getOrDefault(uuid, currentPos);
					double dist = currentPos.distanceTo(lastPos);

					if (dist > 0 && dist < 10.0) {
						double accum = accumulatedDistance.getOrDefault(uuid, 0.0) + dist;
						if (accum >= 20.0) {
							int times = (int) (accum / 20.0);
							data.getResources().addTrainingPoints(tpTravel * times);
							accum %= 20.0;
						}
						accumulatedDistance.put(uuid, accum);
					}
					lastPositions.put(uuid, currentPos);
				}
			});
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		lastPositions.remove(event.getEntity().getUUID());
		accumulatedDistance.remove(event.getEntity().getUUID());
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onBlockBreak(BlockEvent.BreakEvent event) {
		if (event.getPlayer() instanceof ServerPlayer player && !event.isCanceled()) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (data.getStatus().isHasCreatedCharacter()) {
					int baseTp = ConfigManager.getServerConfig().getGameplay().getTpPerBlockMined();
					if (baseTp > 0) {
						int xp = event.getExpToDrop();
						int bonus = (int) Math.round(baseTp * 0.25 * xp);
						data.getResources().addTrainingPoints(baseTp + bonus);
					}
				}
			});
		}
	}

	@SubscribeEvent
	public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (data.getStatus().isHasCreatedCharacter()) {
					int baseTp = ConfigManager.getServerConfig().getGameplay().getTpPerItemCrafted();
					if (baseTp > 0) {
						ItemStack stack = event.getCrafting();
						int amount = stack.getCount();

						int rarityMult = stack.getRarity().ordinal() + 1;
						int tierMult = 1;

						if (stack.getItem() instanceof TieredItem tiered) tierMult = Math.max(1, (int) (tiered.getTier().getSpeed() / 2));
						else if (stack.getItem() instanceof ArmorItem armor) tierMult = Math.max(1, armor.getDefense() / 4);


						int finalMult = Math.max(rarityMult, tierMult);
						data.getResources().addTrainingPoints(baseTp * amount * finalMult);
					}
				}
			});
		}
	}

	private static boolean dropTps(Entity entity) {
		List<Class<?>> enemyList = List.of(
				Monster.class,
				Animal.class,
				Player.class,
				FlyingMob.class,
				Mob.class
		);
		return enemyList.stream().anyMatch(clase -> clase.isInstance(entity));
	}

	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event.getEntity().level().isClientSide) return;
		if (!(event.getSource().getEntity() instanceof Player attacker)) return;

		StatsProvider.get(StatsCapability.INSTANCE, attacker).ifPresent(data -> {
			if (dropTps(event.getEntity())) {
				int tpsHealth;
				if (event.getEntity() instanceof ShadowDummyEntity)
					tpsHealth = (int) Math.round(event.getEntity().getMaxHealth() * ConfigManager.getServerConfig().getGameplay().getTpHealthRatio() * 0.5);
				else
					tpsHealth = (int) Math.round(event.getEntity().getMaxHealth() * ConfigManager.getServerConfig().getGameplay().getTpHealthRatio());
				data.getResources().addTrainingPoints(ConfigManager.getServerConfig().getGameplay().getTpPerHit() + tpsHealth);
			}
		});
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onEntityHit(LivingHurtEvent event) {
		if (event.getEntity().level().isClientSide) return;

		if (event.getSource().getEntity() instanceof Player attacker) {
			StatsProvider.get(StatsCapability.INSTANCE, attacker).ifPresent(attackerData -> {
				if (attackerData.getStatus().isHasCreatedCharacter()) {
					if (event.getAmount() >= 1) {
						int baseTps = ConfigManager.getServerConfig().getGameplay().getTpPerHit();
						attackerData.getResources().addTrainingPoints(baseTps);
					}
				}
			});
		}
	}

	private static void shareWithFusionPartner(ServerPlayer leader, StatsData leaderData, int totalTP) {
		UUID partnerUUID = leaderData.getStatus().getFusionPartnerUUID();
		if (partnerUUID == null) return;

		ServerPlayer partner = leader.getServer().getPlayerList().getPlayer(partnerUUID);
		if (partner == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, partner).ifPresent(pData -> {
			pData.getResources().addTrainingPoints(totalTP / 2);
			NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(partner), partner);
		});
	}
}