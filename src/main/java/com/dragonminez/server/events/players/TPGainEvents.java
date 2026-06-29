package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.entities.ShadowDummyEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.C2S.SummonPlayerShadowDummyC2S;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.quest.QuestUnlocks;
import com.dragonminez.server.dynamicgrowth.DynamicGrowthService;
import com.dragonminez.server.world.dimension.HTCDimension;
import com.dragonminez.server.util.GravityLogic;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.extras.DynamicGrowthStat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class TPGainEvents {

	private static final Map<UUID, Vec3> lastPositions = new HashMap<>();
	private static final Map<UUID, Double> accumulatedDistance = new HashMap<>();

	private static final ThreadLocal<Boolean> IS_SHARING_TP = ThreadLocal.withInitial(() -> false);

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onTPGain(DMZEvent.TPGainEvent event) {
		if (!(event.getPlayer() instanceof ServerPlayer player)) return;
		int baseTP = event.getTpGain();
		if (baseTP <= 0) return;

		if (IS_SHARING_TP.get()) return;

		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (data == null) return;
		
		int modifiedBaseTp = applyWeights(player, data, baseTP);
		double difficultyMult = data.getPlayerQuestData().getDifficulty().tpMultiplier();
		int finalTP = (int) Math.max(0, Math.round(data.calculateTPGain(modifiedBaseTp) * difficultyMult));

		if (event.getShareWithParty()) {
			IS_SHARING_TP.set(true);
			try {
				if (data.getStatus().isFused() && data.getStatus().isFusionLeader()) shareWithFusionPartner(player, data, finalTP);
				shareWithParty(player, data, finalTP);
			} finally {
				IS_SHARING_TP.set(false);
			}
		}

		event.setTpGain(finalTP);
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (!data.getStatus().isHasCreatedCharacter()) return;

				int passiveTp = ConfigManager.getServerConfig().getGameplay().getPassiveTpGain();
				if (passiveTp > 0 && player.tickCount % 100 == 0) {
					data.getResources().addTrainingPoints(applyGravityRoom(player, passiveTp));
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

	/**
	 * Bulma's gravity-room upgrades: training inside the Hyperbolic Time Chamber
	 * earns more TP once the player has completed the gravity-room sidequests. Mk.III overrides Mk.II.
	 */
	private static int applyGravityRoom(Player player, int tp) {
		if (tp <= 0 || !player.level().dimension().equals(HTCDimension.HTC_KEY)) return tp;
		if (QuestUnlocks.isCompleted(player, QuestUnlocks.GRAVITY_MK3)) return (int) Math.round(tp * 2.0);
		if (QuestUnlocks.isCompleted(player, QuestUnlocks.GRAVITY_MK2)) return (int) Math.round(tp * 1.5);
		return tp;
	}

	private static int applyWeights(Player player, StatsData data, int tp) {
		if (tp <= 0) return tp;

		GeneralServerConfig.GravityConfig gravityConfig = ConfigManager.getServerConfig().getGravity();
		if (!gravityConfig.getTpEnabled()) return tp;

		int totalWeight = GravityLogic.getTotalWeight(player);
		if (totalWeight <= 0) return tp;

		double gravityMultiplier = GravityLogic.getGravityMultiplier(player);
		int effectiveWeight = (int) (totalWeight * gravityMultiplier);

		int currentBaseLevel = data.getLevel();
		int totalBaseStats = data.getStats().getTotalStats();
		int initialStats = totalBaseStats - (currentBaseLevel - 1) * 6;

		double boostedTotal = 0;
		boostedTotal += data.getStats().getStrength() * data.getTotalMultiplier("STR");
		boostedTotal += data.getStats().getStrikePower() * data.getTotalMultiplier("SKP");
		boostedTotal += data.getStats().getResistance() * data.getTotalMultiplier("RES");
		boostedTotal += data.getStats().getVitality() * data.getTotalMultiplier("VIT");
		boostedTotal += data.getStats().getKiPower() * data.getTotalMultiplier("PWR");
		boostedTotal += data.getStats().getEnergy() * data.getTotalMultiplier("ENE");

		double relativeLevel = ((boostedTotal - initialStats) / 6.0) + 1.0;

		double peak = gravityConfig.getTpPeakMultiplier();
		double width = gravityConfig.getTpCurveWidth();
		double exponent = -Math.pow((relativeLevel - 2 * effectiveWeight), 2) / (2 * Math.pow(width, 2));
		double multiplier = peak * Math.exp(exponent) + 1;

		int newTp = (int) (tp * multiplier);
		return newTp == 0 && multiplier > 0 ? 1 : newTp;
	}

	private static boolean isPlayerOwnedShadow(Entity entity) {
		return entity instanceof ShadowDummyEntity
				&& entity.getPersistentData().getBoolean(SummonPlayerShadowDummyC2S.TAG_PLAYER_SHADOW);
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
		if (isPlayerOwnedShadow(event.getEntity())) return;

		StatsProvider.get(StatsCapability.INSTANCE, attacker).ifPresent(data -> {
			if (dropTps(event.getEntity())) {
				int tpsHealth;
				if (event.getEntity() instanceof ShadowDummyEntity)
					tpsHealth = (int) Math.round(event.getEntity().getMaxHealth() * ConfigManager.getServerConfig().getGameplay().getTpHealthRatio() * 0.5);
				else
					tpsHealth = (int) Math.round(event.getEntity().getMaxHealth() * ConfigManager.getServerConfig().getGameplay().getTpHealthRatio());
				int killTp = applyDynamicGrowthCombatTpMult(ConfigManager.getServerConfig().getGameplay().getTpPerHit() + tpsHealth);
				int finalTp = applyGravityRoom(attacker, killTp);
				data.getResources().addTrainingPoints(finalTp);
			}
		});
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onEntityHit(LivingHurtEvent event) {
		if (event.getEntity().level().isClientSide) return;
		if (isPlayerOwnedShadow(event.getEntity())) return;

		if (event.getSource().getEntity() instanceof Player attacker) {
			StatsProvider.get(StatsCapability.INSTANCE, attacker).ifPresent(attackerData -> {
				if (attackerData.getStatus().isHasCreatedCharacter()) {
					if (event.getAmount() >= 1) {
						int baseTps = applyDynamicGrowthCombatTpMult(ConfigManager.getServerConfig().getGameplay().getTpPerHit());
						int finalTps = applyGravityRoom(attacker, baseTps);
						attackerData.getResources().addTrainingPoints(finalTps);
					}
				}
			});
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingHurtDynamicGrowth(LivingHurtEvent event) {
		if (event.isCanceled() || event.getEntity().level().isClientSide) return;
		if (!ConfigManager.getServerConfig().getDynamicGrowth().isEnabled()) return;

		LivingEntity target = event.getEntity();
		DamageSource source = event.getSource();
		Entity sourceEntity = source.getEntity();
		Entity directEntity = source.getDirectEntity();
		float damage = event.getAmount();
		if (damage <= 0.0F) return;

		if (isPlayerOwnedShadow(target) || isPlayerOwnedShadow(sourceEntity)) return;

		if (sourceEntity instanceof ServerPlayer attacker && !attacker.is(target)) {
			StatsProvider.get(StatsCapability.INSTANCE, attacker).ifPresent(attackerData -> {
				DynamicGrowthService.markCombat(attackerData);
				if (isKiDamage(source, directEntity)) {
					double damageXp = DynamicGrowthService.practiceDamageXp(attacker, target, damage);
					DynamicGrowthService.award(attacker, attackerData, DynamicGrowthStat.PWR, damageXp, target);
				} else if (isPlayerMeleeDamage(source, attacker)) {
					var pdata = attacker.getPersistentData();
					double growthDamage = pdata.contains("dmz_growth_melee_damage") ? pdata.getDouble("dmz_growth_melee_damage") : damage;
					boolean kiWeaponInUse = pdata.getBoolean("dmz_growth_ki_weapon");
					boolean kiInfuseActive = pdata.getBoolean("dmz_growth_ki_infuse");
					pdata.remove("dmz_growth_melee_damage");
					pdata.remove("dmz_growth_ki_weapon");
					pdata.remove("dmz_growth_ki_infuse");

					double growthXp = DynamicGrowthService.practiceDamageXp(attacker, target, (float) growthDamage);
					if (kiWeaponInUse) {
						DynamicGrowthService.award(attacker, attackerData, DynamicGrowthStat.PWR, growthXp, target);
					} else if (kiInfuseActive) {
						DynamicGrowthService.award(attacker, attackerData, DynamicGrowthStat.STR, growthXp, target);
						double pwrShare = ConfigManager.getServerConfig().getDynamicGrowth().getKiWeaponMeleePwrShare();
						DynamicGrowthService.award(attacker, attackerData, DynamicGrowthStat.PWR, growthXp * pwrShare, target);
					} else {
						DynamicGrowthService.award(attacker, attackerData, DynamicGrowthStat.STR, growthXp, target);
					}
				}
			});
		}

		if (target instanceof ServerPlayer victim && sourceEntity instanceof LivingEntity attacker && !sourceEntity.is(victim)) {
			StatsProvider.get(StatsCapability.INSTANCE, victim).ifPresent(victimData -> {
				DynamicGrowthService.markCombat(victimData);
				double vitalityXp = DynamicGrowthService.practiceDamageXp(victim, attacker, damage);
				DynamicGrowthService.award(victim, victimData, DynamicGrowthStat.VIT, vitalityXp, attacker);
			});
		}
	}

	private static boolean isPlayerMeleeDamage(DamageSource source, ServerPlayer attacker) {
		return "player".equals(source.getMsgId()) && (source.getDirectEntity() == null || source.getDirectEntity().is(attacker));
	}

	private static boolean isKiDamage(DamageSource source, Entity directEntity) {
		if (directEntity instanceof AbstractKiProjectile) return true;
		return source.getMsgId().toLowerCase(Locale.ROOT).contains("kiblast");
	}

	private static int applyDynamicGrowthCombatTpMult(int tp) {
		if (tp <= 0) return tp;
		var dynamicGrowth = ConfigManager.getServerConfig().getDynamicGrowth();
		if (!dynamicGrowth.isEnabled()) return tp;
		double mult = dynamicGrowth.getNaturalCombatTpMultiplier();
		if (mult == 1.0) return tp;
		return mult <= 0.0 ? 0 : Math.max(1, (int) Math.round(tp * mult));
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

	private static void shareWithParty(ServerPlayer earner, StatsData data, int tp) {
		if (!data.getPlayerQuestData().isInParty()) return;

		double shareRatio = ConfigManager.getServerConfig().getGameplay().getPartyTpShareRatio();
		if (shareRatio <= 0) return;

		int sharedTP = (int) (tp * shareRatio);
		if (sharedTP <= 0) return;

		for (ServerPlayer member : PartyManager.getAllPartyMembers(earner)) {
			if (member.getUUID().equals(earner.getUUID())) continue;

			StatsProvider.get(StatsCapability.INSTANCE, member).ifPresent(pData -> {
				if (pData.getStatus().isAlive()) {
					pData.getResources().addTrainingPoints(sharedTP);
					NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(member), member);
				}
			});
		}
	}
}