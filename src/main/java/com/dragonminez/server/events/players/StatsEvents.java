package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.GeneralServerConfig.FoodConfig;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainFluids;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ShadowDummyEntity;
import com.dragonminez.common.network.C2S.SummonPlayerShadowDummyC2S;
import com.dragonminez.common.init.entities.namek.NamekTraderEntity;
import com.dragonminez.common.init.entities.namek.NamekWarriorEntity;
import com.dragonminez.common.init.entities.redribbon.BanditEntity;
import com.dragonminez.common.init.entities.redribbon.RedRibbonSoldierEntity;
import com.dragonminez.common.init.entities.redribbon.RobotEntity;
import com.dragonminez.common.init.entities.sagas.SagaFriezaSoldier01Entity;
import com.dragonminez.common.init.entities.sagas.SagaFriezaSoldier02Entity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.AppearanceSyncS2C;
import com.dragonminez.common.passives.PassiveEventHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.SecondaryStatEffects;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.dragonminez.common.util.lists.SaiyanForms;
import com.dragonminez.server.events.DragonBallsHandler;
import com.dragonminez.server.util.FusionLogic;
import com.dragonminez.server.util.GravityLogic;
import com.dragonminez.server.util.MutantManager;
import com.dragonminez.server.util.PotionEffectHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import com.dragonminez.common.util.CuriosUtil;
import top.theillusivec4.curios.api.CuriosApi;
import com.dragonminez.common.init.item.WeightItem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StatsEvents {

	public static final UUID DMZ_HEALTH_MODIFIER_UUID = UUID.fromString("b065b873-f4c8-4a0f-aa8c-6e778cd410e0");
	public static final UUID FORM_SPEED_UUID = UUID.fromString("c8c07577-3365-4b1c-9917-26b237da6e08");
	public static final UUID FORM_REACH_UUID = UUID.fromString("d8d18684-4476-5c2d-ba28-37c348eb521f");
	public static final UUID FORM_ATTACK_SPEED_UUID = UUID.fromString("f2e0aaf0-a4ab-4921-a5b0-f34cf1c3533b");
	public static final UUID KI_WEAPON_ATTACK_SPEED_UUID = UUID.fromString("a3b1c5d7-9e2f-4a6b-8c1d-5f7e9a0b2c4d");

	public static final UUID WEIGHT_MOVEMENT_SPEED_MOD_UUID = UUID.fromString("6f663f73-199f-431a-8c35-132d75a31742");
	private static final UUID WEIGHT_ATTACK_SPEED_MOD_UUID = UUID.fromString("a1b2c3d4-e5f6-431a-8c35-132d75a31742");

	private static double getWeightStatMultiplier(int level, int weight) {
		if (weight <= 0) return 1.0;
		double x = (double) level;
		double w = (double) weight;
		double penalty;
		if (x <= 2 * w) {
			penalty = 10.5 * (1.0 - Math.sqrt(x / (4.0 * w)));
		} else {
			double f2w = 10.5 * (1.0 - Math.sqrt((2.0 * w) / (4.0 * w)));
			double innerDiv = (-w / (w + 10.0)) + 1.0;
			double top = f2w / innerDiv;
			double slope = -(top / (2.0 * (w + 10.0)));
			double intercept = top;
			double result = slope * x + intercept;
			penalty = Math.max(0, result);
		}
		double multiplier = 1.0 - (penalty / 10.5);
		return Math.max(0.001, multiplier);
	}

	private static void applyWeightAttributeModifier(Player player, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID uuid, String name, double amount) {
		var inst = player.getAttribute(attribute);
		if (inst != null) {
			AttributeModifier existing = inst.getModifier(uuid);
			if (amount == 0) {
				if (existing != null) inst.removeModifier(uuid);
			} else {
				if (existing == null || Math.abs(existing.getAmount() - amount) > 0.001) {
					if (existing != null) inst.removeModifier(uuid);
					inst.addTransientModifier(new AttributeModifier(uuid, name, amount, AttributeModifier.Operation.MULTIPLY_BASE));
				}
			}
		}
	}

	private static final Map<UUID, List<FoodRegenTask>> FOOD_REGEN_QUEUE = new ConcurrentHashMap<>();

	public static class FoodRegenTask {
		public int ticksPassed = 0;
		public final int totalSeconds;
		public final float hpPerSecond;
		public final float kiPerSecond;
		public final float staminaPerSecond;

		public FoodRegenTask(int durationSeconds, float totalHp, float totalKi, float totalStam) {
			this.totalSeconds = durationSeconds;
			this.hpPerSecond = totalHp / (float) durationSeconds;
			this.kiPerSecond = totalKi / (float) durationSeconds;
			this.staminaPerSecond = totalStam / (float) durationSeconds;
		}
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
		Player player = event.player;
		if (!(player instanceof ServerPlayer serverPlayer)) return;

		StatsProvider.get(StatsCapability.INSTANCE, serverPlayer).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;

			
			int[] totalWeight = {0};
			CuriosApi.getCuriosInventory(serverPlayer).ifPresent(inv -> {
				var handler = inv.getCurios().get("weights");
				if (handler != null) {
					for (int i = 0; i < handler.getSlots(); i++) {
						ItemStack stack = handler.getStacks().getStackInSlot(i);
						if (stack.getItem() instanceof WeightItem) {
							totalWeight[0] += WeightItem.getWeight(stack);
						} else if (!stack.isEmpty()) {
							totalWeight[0] += stack.getOrCreateTag().getInt("WeightValue");
						}
					}
				}
			});

			if (totalWeight[0] > 0) {
				double gravityMultiplier = GravityLogic.getGravityMultiplier(serverPlayer);
				int effectiveWeight = (int) (totalWeight[0] * gravityMultiplier);

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

				double multiplier = getWeightStatMultiplier((int) relativeLevel, effectiveWeight);

				if (multiplier < 1.0) {
					data.getBonusStats().addBonus("STR", "Weights", "*", multiplier);
					data.getBonusStats().addBonus("SKP", "Weights", "*", multiplier);
					data.getBonusStats().addBonus("PWR", "Weights", "*", multiplier);
					data.getBonusStats().addBonus("DEF", "Weights", "*", multiplier);
					data.getBonusStats().addBonus("STM", "Weights", "*", multiplier);
				} else {
					data.getBonusStats().removeBonus("STR", "Weights");
					data.getBonusStats().removeBonus("SKP", "Weights");
					data.getBonusStats().removeBonus("PWR", "Weights");
					data.getBonusStats().removeBonus("DEF", "Weights");
					data.getBonusStats().removeBonus("STM", "Weights");
				}

				double reductionModifier = multiplier - 1.0;
				applyWeightAttributeModifier(serverPlayer, Attributes.MOVEMENT_SPEED, WEIGHT_MOVEMENT_SPEED_MOD_UUID, "Weight Speed Penalty", reductionModifier);
				applyWeightAttributeModifier(serverPlayer, Attributes.ATTACK_SPEED, WEIGHT_ATTACK_SPEED_MOD_UUID, "Weight Attack Speed Penalty", reductionModifier);
			} else {
				data.getBonusStats().removeBonus("STR", "Weights");
				data.getBonusStats().removeBonus("SKP", "Weights");
				data.getBonusStats().removeBonus("PWR", "Weights");
				data.getBonusStats().removeBonus("DEF", "Weights");
				data.getBonusStats().removeBonus("STM", "Weights");
				applyWeightAttributeModifier(serverPlayer, Attributes.MOVEMENT_SPEED, WEIGHT_MOVEMENT_SPEED_MOD_UUID, "Weight Speed Penalty", 0);
				applyWeightAttributeModifier(serverPlayer, Attributes.ATTACK_SPEED, WEIGHT_ATTACK_SPEED_MOD_UUID, "Weight Attack Speed Penalty", 0);
			}

			applyHealthBonus(serverPlayer);

			UUID playerId = serverPlayer.getUUID();
			List<FoodRegenTask> tasks = FOOD_REGEN_QUEUE.get(playerId);

			if (tasks != null && !tasks.isEmpty()) {
				float totalHpPulse = 0f;
				float totalKiPulse = 0f;
				float totalStamPulse = 0f;

				Iterator<FoodRegenTask> iterator = tasks.iterator();
				while (iterator.hasNext()) {
					FoodRegenTask task = iterator.next();
					task.ticksPassed++;

					if (task.ticksPassed % 20 == 0) {
						totalHpPulse += task.hpPerSecond;
						totalKiPulse += task.kiPerSecond;
						totalStamPulse += task.staminaPerSecond;
					}

					if (task.ticksPassed >= task.totalSeconds * 20) iterator.remove();
				}

				if (totalHpPulse > 0) {
					totalHpPulse *= (float) Math.min(1.0, data.getSecondaryStatEffects().getMultiplier(SecondaryStatEffects.HP_REGEN));
					if (totalHpPulse > 0) {
						PassiveEventHandler.suppressHealingBonus = true;
						serverPlayer.heal(totalHpPulse);
						PassiveEventHandler.suppressHealingBonus = false;
					}
				}

				if (totalKiPulse > 0 || totalStamPulse > 0) {
					float maxEnergy = data.getMaxEnergy();
					float maxStamina = data.getMaxStamina();
					float currentEnergy = data.getResources().getCurrentEnergy();
					float currentStamina = data.getResources().getCurrentStamina();

					data.getResources().setCurrentEnergy(Math.min(maxEnergy, currentEnergy + totalKiPulse));
					data.getResources().setCurrentStamina(Math.min(maxStamina, currentStamina + totalStamPulse));
				}
			}

			if (data.getResources().getCurrentEnergy() > data.getMaxEnergy())
				data.getResources().setCurrentEnergy(data.getMaxEnergy());
			if (data.getResources().getCurrentStamina() > data.getMaxStamina())
				data.getResources().setCurrentStamina(data.getMaxStamina());
		});
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		FOOD_REGEN_QUEUE.remove(event.getEntity().getUUID());
		StatsProvider.get(StatsCapability.INSTANCE, event.getEntity()).ifPresent(data -> data.getSkills().setSkillActive("kisense", false));
	}

	public static void applyHealthBonus(ServerPlayer serverPlayer) {
		StatsProvider.get(StatsCapability.INSTANCE, serverPlayer).ifPresent(data -> {
			AttributeInstance maxHealthAttr = serverPlayer.getAttribute(Attributes.MAX_HEALTH);
			if (maxHealthAttr == null) return;

			float dmzHealthBonus = data.getHealthBonus();
			if (!Float.isFinite(dmzHealthBonus) || dmzHealthBonus < 0) dmzHealthBonus = 0f;

			AttributeModifier existingModifier = maxHealthAttr.getModifier(DMZ_HEALTH_MODIFIER_UUID);

			if (existingModifier == null || existingModifier.getAmount() != dmzHealthBonus) {
				maxHealthAttr.removeModifier(DMZ_HEALTH_MODIFIER_UUID);

				if (dmzHealthBonus > 0) {
					AttributeModifier healthModifier = new AttributeModifier(
							DMZ_HEALTH_MODIFIER_UUID,
							"DMZ Health Bonus",
							dmzHealthBonus,
							AttributeModifier.Operation.ADDITION
					);
					maxHealthAttr.addPermanentModifier(healthModifier);
				}

				if (serverPlayer.getHealth() > maxHealthAttr.getValue()) {
					serverPlayer.setHealth((float) maxHealthAttr.getValue());
				}
			}

			if (!data.hasInitializedHealth()) {
				serverPlayer.setHealth((float) maxHealthAttr.getValue());
				data.setInitializedHealth(true);
			}
		});
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			DragonBallsHandler.syncRadar(player.serverLevel());
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				applyHealthBonus(player);
				player.setHealth(player.getMaxHealth());
				data.getResources().setCurrentEnergy(data.getMaxEnergy());
				data.getResources().setCurrentStamina(data.getMaxStamina());
				data.getSkills().setSkillActive("kisense", false);
			});
		}
	}

	private static boolean addsAlignment(Entity entity) {
		return entity instanceof RedRibbonSoldierEntity || entity instanceof SagaFriezaSoldier01Entity || entity instanceof SagaFriezaSoldier02Entity
				|| entity instanceof RobotEntity || entity instanceof BanditEntity;
	}

	private static boolean removesAlignment(Entity entity) {
		return entity instanceof NamekWarriorEntity || entity instanceof Villager || entity instanceof NamekTraderEntity;
	}

	@SubscribeEvent
	public static void cookDropsOnKiKill(LivingDeathEvent event) {
		LivingEntity entity = event.getEntity();
		if (entity.level().isClientSide) return;
		if (entity.fireImmune()) return;

		DamageSource source = event.getSource();
		boolean isKiKill = MainDamageTypes.isKiblastDamage(source) || MainDamageTypes.isStrikeAttackDamage(source);
		if (!isKiKill) return;

		if (entity.getRemainingFireTicks() <= 0) entity.setRemainingFireTicks(1);
	}

	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event.getEntity().level().isClientSide) return;
		Player attacker = resolveAttackerPlayer(event.getSource().getEntity(), event.getSource().getDirectEntity());
		if (attacker == null) return;
		boolean[] addAlignment = new boolean[]{false};
		boolean[] removeAlignment = new boolean[]{false};

		if (event.getEntity() instanceof Player victim) {
			StatsProvider.get(StatsCapability.INSTANCE, victim).ifPresent(victimData -> {
				if (victimData.getResources().getAlignment() < 50 || !victimData.getStatus().isHasCreatedCharacter())
					addAlignment[0] = true;
				else removeAlignment[0] = true;
				if (victimData.getStatus().isHasCreatedCharacter()) {
					if (!ConfigManager.getServerConfig().getMutant().getKeepMutantOnDeath() && victimData.getEffects().hasEffect(MutantManager.EFFECT_NAME) && victim instanceof ServerPlayer mutantVictim) MutantManager.revoke(mutantVictim, victimData);
					victimData.getEffects().removeAllEffects();
					victimData.getSecondaryStatEffects().clear();
					victimData.getStatus().setChargingKi(false);
					victimData.getStatus().setActionCharging(false);
					victimData.getCharacter().setActiveForm(null, null);
					victimData.getCharacter().setActiveStackForm(null, null);
					victimData.getSkills().setSkillActive("kisense", false);
					FOOD_REGEN_QUEUE.remove(victim.getUUID());
				}
			});
		}

		if (removesAlignment(event.getEntity())) removeAlignment[0] = true;
		if (addsAlignment(event.getEntity())) addAlignment[0] = true;

		if (event.getEntity() instanceof ShadowDummyEntity dummyEntity && attacker instanceof ServerPlayer killer) {
			StatsProvider.get(StatsCapability.INSTANCE, killer).ifPresent(killerData ->
					killerData.getStatus().setShadowDummyKillCount(killerData.getStatus().getShadowDummyKillCount() + 1));
		}

		if (event.getEntity() instanceof ShadowDummyEntity dummyEntity && dummyEntity.getPersistentData().getBoolean(SummonPlayerShadowDummyC2S.TAG_PLAYER_SHADOW)) {
			SummonPlayerShadowDummyC2S.dismissByDummy(dummyEntity);
		}

		StatsProvider.get(StatsCapability.INSTANCE, attacker).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;

			if (removeAlignment[0]) {
				data.getResources().removeAlignment(5);
				removeAlignment[0] =  false;
			}

			if (addAlignment[0]) {
				data.getResources().addAlignment(2);
				addAlignment[0] = false;
			}

			grantTechniqueKillXp(data, event.getSource().getDirectEntity());
		});
	}

	private static Player resolveAttackerPlayer(Entity sourceEntity, Entity directEntity) {
		if (sourceEntity instanceof Player player) return player;
		if (directEntity instanceof AbstractKiProjectile projectile && projectile.getOwner() instanceof Player player) return player;
		return null;
	}

	private static void grantTechniqueKillXp(com.dragonminez.common.stats.StatsData data, Entity directEntity) {
		if (!(directEntity instanceof AbstractKiProjectile projectile)) return;

		String techniqueId = projectile.getTechniqueId();
		if (techniqueId == null || techniqueId.isEmpty()) return;

		TechniqueData techniqueData = data.getTechniques().getUnlockedTechniques().get(techniqueId);
		if (!(techniqueData instanceof KiAttackData kiAttackData)) return;

		int xpGain = kiAttackData.getXpGainPerKill();
		if (xpGain > 0) data.getTechniques().addExperienceToTechnique(techniqueId, xpGain);
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onEntityHit(LivingHurtEvent event) {
		if (event.getEntity().level().isClientSide) return;

		if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
			StatsProvider.get(StatsCapability.INSTANCE, attacker).ifPresent(attackerData -> {
				if (attackerData.getStatus().isHasCreatedCharacter()) {
					if (event.getAmount() >= 1 && !isMasteryBlacklisted(event.getEntity())) {
						double damageScale = masteryDamageScale(event.getEntity(), event.getAmount());

						if (attackerData.getCharacter().hasActiveForm()) {
							FormConfig.FormData activeForm = attackerData.getCharacter().getActiveFormData();
							if (activeForm != null && attackerData.getResources().getPowerRelease() >= 50) {
								String formGroup = attackerData.getCharacter().getActiveFormGroup();
								String formName = attackerData.getCharacter().getActiveForm();
								double bonus = 1.0 + (GravityLogic.getBonusGravity(attacker) * ConfigManager.getServerConfig().getGravity().getMasteryBonusPerGravity());
								attackerData.getCharacter().gainMastery(formGroup, formName, PotionEffectHelper.applyMasteryGainMultiplier(attacker, activeForm.getMasteryPerHitDealt() * bonus * damageScale));
							}
						}

						if (attackerData.getCharacter().hasActiveStackForm()) {
							FormConfig.FormData activeStackForm = attackerData.getCharacter().getActiveStackFormData();
							if (activeStackForm != null && attackerData.getResources().getPowerRelease() >= 50) {
								String stackFormGroup = attackerData.getCharacter().getActiveStackFormGroup();
								String stackForm = attackerData.getCharacter().getActiveStackForm();
								double bonus = 1.0 + (GravityLogic.getBonusGravity(attacker) * ConfigManager.getServerConfig().getGravity().getMasteryBonusPerGravity());
								attackerData.getCharacter().gainMastery(stackFormGroup, stackForm, PotionEffectHelper.applyMasteryGainMultiplier(attacker, activeStackForm.getMasteryPerHitDealt() * bonus * damageScale));
							}
						}
					}
				}
				NetworkHandler.sendToTrackingEntityAndSelf(new AppearanceSyncS2C(attacker), attacker);
			});
		}

		if (event.getEntity() instanceof ServerPlayer victim) {
			boolean fromEntity = event.getSource().getEntity() instanceof LivingEntity;
			StatsProvider.get(StatsCapability.INSTANCE, victim).ifPresent(victimData -> {
				if (fromEntity && victimData.getStatus().isHasCreatedCharacter()) {
					if (event.getAmount() >= 1 && !isMasteryBlacklisted(event.getSource().getEntity())) {
						double damageScale = masteryDamageScale(victim, event.getAmount());

						if (victimData.getCharacter().hasActiveForm()) {
							FormConfig.FormData activeForm = victimData.getCharacter().getActiveFormData();
							if (activeForm != null && victimData.getResources().getPowerRelease() >= 50) {
								String formGroup = victimData.getCharacter().getActiveFormGroup();
								String formName = victimData.getCharacter().getActiveForm();
								double bonus = 1.0 + (GravityLogic.getBonusGravity(victim) * ConfigManager.getServerConfig().getGravity().getMasteryBonusPerGravity());
								victimData.getCharacter().gainMastery(formGroup, formName, PotionEffectHelper.applyMasteryGainMultiplier(victim, activeForm.getMasteryPerHitReceived() * bonus * damageScale));
							}
						}

						if (victimData.getCharacter().hasActiveStackForm()) {
							FormConfig.FormData activeStackForm = victimData.getCharacter().getActiveStackFormData();
							if (activeStackForm != null && victimData.getResources().getPowerRelease() >= 50) {
								String stackFormGroup = victimData.getCharacter().getActiveStackFormGroup();
								String stackForm = victimData.getCharacter().getActiveStackForm();
								double bonus = 1.0 + (GravityLogic.getBonusGravity(victim) * ConfigManager.getServerConfig().getGravity().getMasteryBonusPerGravity());
								victimData.getCharacter().gainMastery(stackFormGroup, stackForm, PotionEffectHelper.applyMasteryGainMultiplier(victim, activeStackForm.getMasteryPerHitReceived() * bonus * damageScale));
							}
						}
					}
				}
				NetworkHandler.sendToTrackingEntityAndSelf(new AppearanceSyncS2C(victim), victim);
			});
		}
	}

	private static boolean isMasteryBlacklisted(Entity entity) {
		if (entity == null) return false;
		ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
		if (key == null) return false;
		return ConfigManager.getCombatConfig().getMasteryBlacklistEntities().contains(key.toString());
	}

	private static double masteryDamageScale(LivingEntity hitEntity, double damage) {
		double maxHp = hitEntity.getMaxHealth();
		if (maxHp <= 0.0 || damage <= 0.0) return 1.0;
		double pct = damage / maxHp;
		double t = Mth.clamp((pct - 0.20) / 0.60, 0.0, 1.0);
		return 1.0 + t * 3.0;
	}

	private static final double HEAL_PERCENTAGE = 0.08;
	private static final int HEAL_TICKS = 3 * 20;
	private static final Map<Player, Long> lastHealingTime = new WeakHashMap<>();

	@SubscribeEvent
	public static void onLivingTick(TickEvent.PlayerTickEvent event) {
		Player player = event.player;
		if (player.level().isClientSide || event.phase != TickEvent.Phase.END) return;
		FluidState fluidState = player.level().getFluidState(player.blockPosition());
		if (fluidState.isEmpty()) return;

		if (fluidState.is(MainFluids.SOURCE_HEALING.get()) || fluidState.is(MainFluids.FLOWING_HEALING.get())) {
			long currentTime = player.level().getGameTime();
			long lastHealTime = lastHealingTime.getOrDefault(player, 0L);

			if (currentTime - lastHealTime >= HEAL_TICKS) {
				funcHealingLiquid(player);
				lastHealingTime.put(player, currentTime);
			}
		} else if (fluidState.is(MainFluids.SOURCE_NAMEK.get()) || fluidState.is(MainFluids.FLOWING_NAMEK.get())) {
			funcNamekWater(player);
		}
	}

	private static void funcHealingLiquid(Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			StatsProvider.get(StatsCapability.INSTANCE, serverPlayer).ifPresent(data -> {
				float maxHp = player.getMaxHealth();
				float healHp = (float) (maxHp * HEAL_PERCENTAGE);
				float maxKi = data.getMaxEnergy();
				float healKi = (float) (maxKi * HEAL_PERCENTAGE);
				float maxStamina = data.getMaxStamina();
				float healStamina = (float) (maxStamina * HEAL_PERCENTAGE);
				boolean hasCreatedChar = data.getStatus().isHasCreatedCharacter();

				if (healHp > maxHp) healHp = maxHp;
				if (healKi > maxKi) healKi = maxKi;
				if (healStamina > maxStamina) healStamina = maxStamina;

				if (hasCreatedChar) {
					serverPlayer.setHealth(player.getHealth() + healHp);
					data.getResources().addEnergy(healKi);
					data.getResources().addStamina(healStamina);
				}

			});
		}
		if (player.isOnFire()) {
			player.clearFire();
		}
	}

	private static void funcNamekWater(Player player) {
		if (player.isOnFire()) {
			player.clearFire();
		}
	}

	@SubscribeEvent
	public static void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
		if (event.getLevel().isClientSide) return;

		Player player = event.getEntity();
		ItemStack stack = event.getItemStack();
		ResourceLocation itemKey = ForgeRegistries.ITEMS.getKey(stack.getItem());
		if (itemKey == null) return;
		String itemId = itemKey.toString();
		String namespace = itemKey.getNamespace();

		FoodConfig foodConfig = ConfigManager.getServerConfig().getGameplay().getFood();
		boolean isModBlacklisted = !foodConfig.getBlacklistedNamespaces().isEmpty() && foodConfig.getBlacklistedNamespaces().contains(namespace);
		boolean isItemBlacklisted = !foodConfig.getBlacklistedItems().isEmpty() && foodConfig.getBlacklistedItems().contains(itemId);

		if (!isModBlacklisted && !isItemBlacklisted) {
			player.startUsingItem(event.getHand());
			event.setCancellationResult(InteractionResult.CONSUME);
		}
	}

	@SubscribeEvent
	public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
		if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) return;

		ItemStack stack = event.getItem();
		ResourceLocation itemKey = ForgeRegistries.ITEMS.getKey(stack.getItem());
		if (itemKey == null) return;
		String itemId = itemKey.toString();
		String namespace = itemKey.getNamespace();

		FoodConfig foodConfig = ConfigManager.getServerConfig().getGameplay().getFood();

		boolean isModBlacklisted = !foodConfig.getBlacklistedNamespaces().isEmpty() && foodConfig.getBlacklistedNamespaces().contains(namespace);
		boolean isItemBlacklisted = !foodConfig.getBlacklistedItems().isEmpty() && foodConfig.getBlacklistedItems().contains(itemId);

		if (!isModBlacklisted && !isItemBlacklisted) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				boolean isSenzu = itemId.equals("dragonminez:senzu_bean");
				boolean isHeartMedicine = itemId.equals("dragonminez:heart_medicine");

				if ((isSenzu || isHeartMedicine) && player.getCooldowns().isOnCooldown(stack.getItem())) return;

				FoodProperties foodProperties = stack.getFoodProperties(player);
				if (foodProperties == null) return;

				int foodGain = foodProperties.getNutrition();
				float saturationGain = foodProperties.getSaturationModifier();

				float healthFoodRecoveryPercentage = foodGain >= foodConfig.getMinHungerPoints()
						? Math.min(foodGain, foodConfig.getMaxHungerPoints()) * foodConfig.getHealthPercentageRecoveredPerHungerPoint()
						: 0;
				float kiFoodRecoveryPercentage = foodGain >= foodConfig.getMinHungerPoints()
						? Math.min(foodGain, foodConfig.getMaxHungerPoints()) * foodConfig.getKiPercentageRecoveredPerHungerPoint()
						: 0;
				float staminaFoodRecoveryPercentage = foodGain >= foodConfig.getMinHungerPoints()
						? Math.min(foodGain, foodConfig.getMaxHungerPoints()) * foodConfig.getStaminaPercentageRecoveredPerHungerPoint()
						: 0;

				float healthSaturationRecoveryPercentage = saturationGain >= foodConfig.getMinSaturationPoints()
						? Math.min(saturationGain, foodConfig.getMaxSaturationPoints()) * foodConfig.getHealthPercentageRecoveredPerSaturationPoint()
						: 0;
				float kiSaturationRecoveryPercentage = saturationGain >= foodConfig.getMinSaturationPoints()
						? Math.min(saturationGain, foodConfig.getMaxSaturationPoints()) * foodConfig.getKiPercentageRecoveredPerSaturationPoint()
						: 0;
				float staminaSaturationRecoveryPercentage = saturationGain >= foodConfig.getMinSaturationPoints()
						? Math.min(saturationGain, foodConfig.getMaxSaturationPoints()) * foodConfig.getStaminaPercentageRecoveredPerSaturationPoint()
						: 0;

				float healthTotalRecoveryPercentage = healthFoodRecoveryPercentage + healthSaturationRecoveryPercentage;
				float kiTotalRecoveryPercentage = kiFoodRecoveryPercentage + kiSaturationRecoveryPercentage;
				float staminaTotalRecoveryPercentage = staminaFoodRecoveryPercentage + staminaSaturationRecoveryPercentage;

				float maxHealth = player.getMaxHealth();
				float maxEnergy = data.getMaxEnergy();
				float maxStamina = data.getMaxStamina();

				float healAmount = (maxHealth * healthTotalRecoveryPercentage);
				float energyAmount = (maxEnergy * kiTotalRecoveryPercentage);
				float staminaAmount = (maxStamina * staminaTotalRecoveryPercentage);

				if (isSenzu || isHeartMedicine) {
					PassiveEventHandler.suppressHealingBonus = true;
					player.heal(maxHealth - player.getHealth());
					PassiveEventHandler.suppressHealingBonus = false;
					data.getResources().setCurrentEnergy(maxEnergy);
					data.getResources().setCurrentStamina(maxStamina);

					int cooldownTicks = ConfigManager.getServerConfig().getGameplay().getSenzuCooldownTicks();
					player.getCooldowns().addCooldown(stack.getItem(), cooldownTicks);
				} else {
					int durationSeconds = 6;
					FOOD_REGEN_QUEUE.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(new FoodRegenTask(durationSeconds, healAmount, energyAmount, staminaAmount));
				}
			});
		}
	}

	@SubscribeEvent
	public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
		if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) return;

		ItemStack stack = event.getItem();
		ResourceLocation itemKey = ForgeRegistries.ITEMS.getKey(stack.getItem());
		if (itemKey == null) return;
		String itemId = itemKey.toString();

		if (itemId.equals("dragonminez:senzu_bean") || itemId.equals("dragonminez:heart_medicine")) {
			if (player.getCooldowns().isOnCooldown(stack.getItem()) || player.hasEffect(MainEffects.STUN.get()))
				event.setCanceled(true);
			else event.setDuration(1);
		}
	}

	@SubscribeEvent
	public static void onPlayerAttack(AttackEntityEvent event) {
		if (event.getEntity().level().isClientSide) return;
		if (event.getEntity().hasEffect(MainEffects.STUN.get())) event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onLivingAttack(LivingAttackEvent event) {
		if (event.getEntity().level().isClientSide) return;
		if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker.hasEffect(MainEffects.STUN.get()))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getLevel().isClientSide) return;
		if (event.getEntity() == null) return;
		if (event.getEntity().hasEffect(MainEffects.STUN.get())) event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
		if (event.getEntity().level().isClientSide) return;

		if (event.getEntity().hasEffect(MainEffects.STUN.get()))
			event.getEntity().setDeltaMovement(event.getEntity().getDeltaMovement().multiply(1, 0, 1));
	}

	@SubscribeEvent
	public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
		LivingEntity entity = event.getEntity();
		if (entity.level().isClientSide) return;

		if (entity.hasEffect(MainEffects.STUN.get())) {
			entity.setDeltaMovement(0, entity.getDeltaMovement().y, 0);
			entity.setJumping(false);
			entity.setSprinting(false);
			if (entity instanceof ServerPlayer serverPlayer) {
				if (serverPlayer.getAbilities().flying) {
					serverPlayer.getAbilities().flying = false;
					serverPlayer.onUpdateAbilities();
				}
				if (serverPlayer.isFallFlying()) {
					serverPlayer.stopFallFlying();
				}
			}

			if (entity.getPose() != Pose.CROUCHING) entity.setPose(Pose.CROUCHING);

			if (entity instanceof Mob mob) {
				mob.getNavigation().stop();
				mob.setTarget(null);
				mob.setAggressive(false);
			}
		}

		if (entity instanceof ServerPlayer serverPlayer) {
			StatsProvider.get(StatsCapability.INSTANCE, serverPlayer).ifPresent(data -> {
				if (!data.getStatus().isHasCreatedCharacter()) return;

				AttributeInstance speedAttr = serverPlayer.getAttribute(Attributes.MOVEMENT_SPEED);
				AttributeInstance attackSpeedAttr = serverPlayer.getAttribute(Attributes.ATTACK_SPEED);
				if (speedAttr != null) {
					double expectedBonus = 0.0;
					if (data.getCharacter().hasActiveForm()) {
						FormConfig.FormData activeForm = data.getCharacter().getActiveFormData();
						if (activeForm != null) {
							double multiplier = activeForm.getSpeedMultiplier();
							if (multiplier != 1.0) expectedBonus = multiplier - 1.0;
						}
					}

					AttributeModifier existingSpeed = speedAttr.getModifier(FORM_SPEED_UUID);
					double currentBonus = existingSpeed != null ? existingSpeed.getAmount() : 0.0;

					if (expectedBonus != currentBonus) {
						speedAttr.removeModifier(FORM_SPEED_UUID);
						if (expectedBonus > 0) {
							speedAttr.addTransientModifier(new AttributeModifier(FORM_SPEED_UUID, "Form Speed Bonus", expectedBonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
						}
					}
				}

				if (attackSpeedAttr != null) {
					// Ki weapons act as fake weapons: apply their configured attack speed as a flat
					// modifier (e.g. -2.4 -> base 4.0 becomes 1.6, like a real sword). Applied before
					// the form multiplier below so forms scale the adjusted Ki weapon speed.
					double expectedKi = 0.0;
					if (PlayerAttackHelper.isKiWeaponActive(serverPlayer)) {
						var kiCfg = ConfigManager.getCombatConfig().getKiWeaponConfig(data.getStatus().getKiWeaponType());
						if (kiCfg != null) expectedKi = kiCfg.getAttackSpeed();
					}
					AttributeModifier existingKi = attackSpeedAttr.getModifier(KI_WEAPON_ATTACK_SPEED_UUID);
					double currentKi = existingKi != null ? existingKi.getAmount() : 0.0;
					if (Math.abs(expectedKi - currentKi) > 1e-9) {
						attackSpeedAttr.removeModifier(KI_WEAPON_ATTACK_SPEED_UUID);
						if (expectedKi != 0.0) {
							attackSpeedAttr.addTransientModifier(new AttributeModifier(
									KI_WEAPON_ATTACK_SPEED_UUID,
									"Ki Weapon Attack Speed",
									expectedKi,
									AttributeModifier.Operation.ADDITION
							));
						}
					}
				}

				if (attackSpeedAttr != null) {
					double expectedMultiplier = 1.0;
					if (data.getCharacter().hasActiveForm()) {
						FormConfig.FormData activeForm = data.getCharacter().getActiveFormData();
						if (activeForm != null) expectedMultiplier *= activeForm.getAttackSpeed();
					}
					if (data.getCharacter().hasActiveStackForm()) {
						FormConfig.FormData activeStackForm = data.getCharacter().getActiveStackFormData();
						if (activeStackForm != null) expectedMultiplier *= activeStackForm.getAttackSpeed();
					}

					double base = attackSpeedAttr.getBaseValue();
					double afterAdditions = base + attackSpeedAttr.getModifiers(AttributeModifier.Operation.ADDITION).stream().mapToDouble(AttributeModifier::getAmount).sum();
					double intermediateSpeed = afterAdditions;
					for (AttributeModifier m : attackSpeedAttr.getModifiers(AttributeModifier.Operation.MULTIPLY_BASE)) intermediateSpeed += afterAdditions * m.getAmount();

					double expectedBonus;
					if (intermediateSpeed > 0.0) {
						double targetSpeed = Math.max(0.25, intermediateSpeed * expectedMultiplier);
						expectedBonus = targetSpeed / intermediateSpeed - 1.0;
					} else expectedBonus = 0.0;

					AttributeModifier existingAttackSpeed = attackSpeedAttr.getModifier(FORM_ATTACK_SPEED_UUID);
					double currentBonus = existingAttackSpeed != null ? existingAttackSpeed.getAmount() : 0.0;

					if (Math.abs(expectedBonus - currentBonus) > 1e-9) {
						attackSpeedAttr.removeModifier(FORM_ATTACK_SPEED_UUID);
						if (expectedBonus != 0.0) {
							attackSpeedAttr.addTransientModifier(new AttributeModifier(
									FORM_ATTACK_SPEED_UUID,
									"Form Attack Speed Bonus",
									expectedBonus,
									AttributeModifier.Operation.MULTIPLY_TOTAL
							));
						}
					}
				}

				AttributeInstance reachAttr = serverPlayer.getAttribute(ForgeMod.BLOCK_REACH.get());
				AttributeInstance entityReachAttr = serverPlayer.getAttribute(ForgeMod.ENTITY_REACH.get());

				Float[] scaling = data.getCharacter().getResolvedModelScaling();
				float currentScaleY = scaling[1];

				final float BASE_SCALE = 0.9375f;
				final float BASE_HEIGHT = 1.8F;
				final float BASE_REACH = 4.5F;
				float ratioY = currentScaleY / BASE_SCALE;

				double expectedReach = 0.0;
				if (ratioY > 1.01f) {
					float currentHeight = BASE_HEIGHT * ratioY;
					expectedReach = (currentHeight - BASE_HEIGHT) * (BASE_REACH / BASE_HEIGHT);
				}

				if (reachAttr != null) {
					AttributeModifier existingReach = reachAttr.getModifier(FORM_REACH_UUID);
					if ((existingReach != null ? existingReach.getAmount() : 0.0) != expectedReach) {
						reachAttr.removeModifier(FORM_REACH_UUID);
						if (expectedReach > 0) reachAttr.addTransientModifier(new AttributeModifier(FORM_REACH_UUID, "Form Reach Bonus", expectedReach, AttributeModifier.Operation.ADDITION));
					}
				}

				if (entityReachAttr != null) {
					AttributeModifier existingEntityReach = entityReachAttr.getModifier(FORM_REACH_UUID);
					if ((existingEntityReach != null ? existingEntityReach.getAmount() : 0.0) != expectedReach) {
						entityReachAttr.removeModifier(FORM_REACH_UUID);
						if (expectedReach > 0) entityReachAttr.addTransientModifier(new AttributeModifier(FORM_REACH_UUID, "Form Reach Bonus", expectedReach, AttributeModifier.Operation.ADDITION));
					}
				}
			});
		}
	}

	@SubscribeEvent
	public static void onFall(LivingFallEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;

		final int[] jumpLevel = {0};
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;
			if (!data.getSkills().hasSkill("jump") || !data.getSkills().isSkillActive("jump")) return;
			jumpLevel[0] = data.getSkills().getSkillLevel("jump");
		});

		if (jumpLevel[0] <= 0) return;

		float maxHeight = 1.25f + (jumpLevel[0] * 1.0f);
		float safeHeight = maxHeight + 1.0f;

		float fallDistance = event.getDistance();

		if (fallDistance <= safeHeight) {
			player.resetFallDistance();
			event.setCanceled(true);
		} else {
			float reducedDistance = fallDistance - safeHeight;
			event.setDistance(reducedDistance);
		}
	}

	@SubscribeEvent
	public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
		if (event.getLevel().isClientSide) return;
		if (!(event.getTarget() instanceof ServerPlayer target)) return;
		ServerPlayer source = (ServerPlayer) event.getEntity();
		if (!source.getMainHandItem().isEmpty()) return;

		StatsProvider.get(StatsCapability.INSTANCE, source).ifPresent(sData -> {
			StatsProvider.get(StatsCapability.INSTANCE, target).ifPresent(tData -> {

				if (!tData.getStatus().isBlocking()) return;

				boolean sHasRight = hasPothala(source, "right");
				boolean tHasLeft = hasPothala(target, "left");

				boolean sameColor = checkPothalaColorMatch(source, target);

				if (sHasRight && tHasLeft && sameColor) {
					FusionLogic.executePothala(source, target, sData, tData);
					event.setCanceled(true);
				}
			});
		});
	}

	private static ItemStack getHeadTechStack(ServerPlayer player) {
		return CuriosUtil.getFirstStackForItem(player, "head_tech", "pothala");
	}

	private static boolean hasPothala(ServerPlayer player, String side) {
		ItemStack headTech = getHeadTechStack(player);
		if (headTech.isEmpty()) return false;

		if (side.equals("left") && (headTech.getItem() == MainItems.POTHALA_LEFT.get() || headTech.getItem() == MainItems.GREEN_POTHALA_LEFT.get())) {
			return true;
		}
		return side.equals("right") && (headTech.getItem() == MainItems.POTHALA_RIGHT.get() || headTech.getItem() == MainItems.GREEN_POTHALA_RIGHT.get());
	}

	private static boolean checkPothalaColorMatch(ServerPlayer p1, ServerPlayer p2) {
		ItemStack p1Tech = getHeadTechStack(p1);
		ItemStack p2Tech = getHeadTechStack(p2);

		if (p1Tech.isEmpty() || p2Tech.isEmpty()) return false;

		boolean p1IsGreen = p1Tech.getItem().getDescriptionId().contains("green");
		boolean p2IsGreen = p2Tech.getItem().getDescriptionId().contains("green");

		return p1IsGreen == p2IsGreen;
	}

    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) return;

        StatsProvider.get(StatsCapability.INSTANCE, entity).ifPresent(data -> {
            var character = data.getCharacter();
            String currentForm = character.getActiveForm();
            String race = character.getRaceName().toLowerCase();

            String logicKey = character.getRenderLogicKey();

            Float[] resolved = character.getResolvedModelScaling();
            float configScaleX = resolved[0];
            float configScaleY = resolved[1];

            float scalingX = configScaleX;
            float scalingY = configScaleY;

            boolean isOozaru = logicKey.startsWith("oozaru") ||
                    (race.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU)));

            if (isOozaru) {
                float baseOozaruSize = 3.8f;

                float visualScaleX = Math.max(0.1f, configScaleX - 2.8f);
                float visualScaleY = Math.max(0.1f, configScaleY - 2.8f);

                scalingX = visualScaleX * baseOozaruSize;
                scalingY = visualScaleY * baseOozaruSize;
            } else {
                scalingX = configScaleX;
                scalingY = configScaleY;
            }

            Pose pose = event.getPose();

            if (pose == Pose.DYING || pose == Pose.SLEEPING) {
                event.setNewSize(EntityDimensions.fixed(0.2F, 0.2F));
                event.setNewEyeHeight(0.2F);
                return;
            }

            float rawWidth = 0.6F * scalingX;
            float rawHeight = 1.9F * scalingY;

            float finalWidth = Math.round(rawWidth * 10.0F) / 10.0F;
            float finalHeight = Math.round(rawHeight * 10.0F) / 10.0F;

            float poseHeightMultiplier = 1.0F;
            float eyeHeightMultiplier = 1.0F;

            if (pose == Pose.CROUCHING) {
                poseHeightMultiplier = 1.5F / 1.8F;
                eyeHeightMultiplier = 1.27F / 1.62F;
            } else if (pose == Pose.SWIMMING || pose == Pose.FALL_FLYING || pose == Pose.SPIN_ATTACK) {
                poseHeightMultiplier = 0.6F / 1.8F;
                eyeHeightMultiplier = 0.4F / 1.62F;
            }

            float heightConPose = finalHeight * poseHeightMultiplier;
            float alturaSegura = Math.round(heightConPose * 10.0F) / 10.0F;

            EntityDimensions newDims = EntityDimensions.fixed(finalWidth, alturaSegura);
            event.setNewSize(newDims);

            float rawEyeHeight = 1.7F * scalingY * eyeHeightMultiplier;
            float finalEyeHeight = Math.round(rawEyeHeight * 10.0F) / 10.0F;

            event.setNewEyeHeight(finalEyeHeight);
        });
    }
}