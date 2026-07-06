package com.dragonminez.server.events.players.combat;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.combat.util.Player_DMZ;
import com.dragonminez.common.combat.util.SoundHelper;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.*;
import com.dragonminez.common.init.entities.PunchMachineEntity;
import com.dragonminez.common.init.entities.ShadowDummyEntity;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.network.C2S.SummonPlayerShadowDummyC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TriggerImpactFrameS2C;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.dragonminez.common.stats.techniques.TechniqueDispatcher;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.character.SecondaryStatEffects;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.util.GravityLogic;
import com.dragonminez.server.world.dimension.OtherworldDimension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CombatEvent {
	private static final Map<String, Long> LAST_PLAYER_HIT_GUARD_MS = new HashMap<>();
	public static final String DMZ_LAST_ATTACKER_ID_TAG = "dmz_last_attacker_id";
	public static final String DMZ_LAST_HIT_TARGET_ID_TAG = "dmz_last_hit_target_id";
	public static final String DMZ_LAST_HIT_TARGET_TIME_TAG = "dmz_last_hit_target_time";

	private static final double HEALING_REDUCTION_CAP = 0.40;
	private static final int HEALING_REDUCTION_DURATION_TICKS = 120;

	private static void maybeForceCombatFly(Player player) {
		if (!ConfigManager.getCombatConfig().getCombatFlyAutoSwitchOnDamage()) return;
		if (!(player instanceof ServerPlayer serverPlayer)) return;

		StatsProvider.get(StatsCapability.INSTANCE, serverPlayer).ifPresent(data -> {
			if (!data.getSkills().isSkillActive("fly")) return;

			int lockTicks = ConfigManager.getCombatConfig().getCombatFlyLockSeconds() * 20;
			boolean switched = false;
			if (data.getStatus().getFlightMode() == Status.FLIGHT_SEARCH) {
				data.getStatus().setFlightMode(Status.FLIGHT_COMBAT);
				switched = true;
			}
			data.getCooldowns().setCooldown(Cooldowns.COMBAT_FLY_LOCK, lockTicks);

			if (switched) NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(serverPlayer), serverPlayer);
		});
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onLivingHurt(LivingHurtEvent event) {
		DamageSource source = event.getSource();
		final double[] currentDamage = {event.getAmount()};
		final boolean[] wasBlocked = {false};
		final boolean[] wasParry = {false};
		final boolean[] canceledByBlocking = {false};
		final double[] passiveDefensePen = {0.0};

		if (source.getEntity() instanceof LivingEntity livingAttacker && livingAttacker.hasEffect(MainEffects.STUN.get())) {
			event.setCanceled(true);
			return;
		}

		if (event.getEntity() instanceof Player victim) {
			StatsProvider.get(StatsCapability.INSTANCE, victim).ifPresent(victimData -> {
				if (victimData.getStatus().isKnockedDown()) {
					if (source.getEntity() instanceof Player attacker) {
						boolean isSamePartyPvp = PartyManager.areInSameParty(attacker, victim) && PartyManager.isPartyPvpEnabled(attacker);
						boolean isFriendlyFist = StatsProvider.get(StatsCapability.INSTANCE, attacker)
								.map(data -> data.getStatus().isFriendlyFistEnabled())
								.orElse(false);

						if (isSamePartyPvp || isFriendlyFist) event.setCanceled(true);
					}
				}
			});
			if (event.isCanceled()) return;
		}

		if (isSpecificKiAttack(source)) NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.7f, 0.1f, 2, true), event.getEntity());

		if (source.getEntity() instanceof Player attacker && source.getMsgId().equals("player")) {
			if (attacker.hasEffect(MainEffects.STUN.get()) || attacker.isBlocking()) {
				event.setCanceled(true);
				return;
			}

			LivingEntity livingTarget = event.getEntity();
			long now = System.currentTimeMillis();
			String hitKey = attacker.getUUID() + ":" + livingTarget.getUUID();
			long lastHit = LAST_PLAYER_HIT_GUARD_MS.getOrDefault(hitKey, 0L);
			if ((now - lastHit) <= 35L) {
				event.setCanceled(true);
				return;
			}
			LAST_PLAYER_HIT_GUARD_MS.put(hitKey, now);

			attacker.getPersistentData().putInt(DMZ_LAST_HIT_TARGET_ID_TAG, livingTarget.getId());
			attacker.getPersistentData().putLong(DMZ_LAST_HIT_TARGET_TIME_TAG, now);

			boolean isPunchMachine = event.getEntity() instanceof PunchMachineEntity;

			StatsProvider.get(StatsCapability.INSTANCE, attacker).ifPresent(attackerData -> {
				if (!attackerData.getStatus().isHasCreatedCharacter()) return;
				if (attackerData.getStatus().isBlocking()) {
					event.setCanceled(true);
					canceledByBlocking[0] = true;
					return;
				}

				double baseDamage = currentDamage[0];
				double dmzDamage = attackerData.getMeleeDamage();
				double staminaDamage = attackerData.getMeleeDamageNoMultipliers();

				var dmzPlayer = (Player_DMZ) attacker;
				var currentAttack = dmzPlayer.getCurrentAttack();

				if (currentAttack != null) {
					dmzDamage *= currentAttack.attack().damageMultiplier();
					staminaDamage *= currentAttack.attack().damageMultiplier();
					if (currentAttack.isOffHand()) {
						dmzDamage *= 0.9;
						staminaDamage *= 0.9;
					}
				}

				DMZEvent.DamageModifyEvent modifyEvent = new DMZEvent.DamageModifyEvent(attacker, livingTarget, dmzDamage, 0.0, DMZEvent.DamageSourceType.MELEE);
					if (MinecraftForge.EVENT_BUS.post(modifyEvent)) {
						dmzDamage = 0.0;
					} else {
						dmzDamage = Math.max(0.0, modifyEvent.getAmount());
						passiveDefensePen[0] = modifyEvent.getDefensePenetration();
					}

				double currentSpeed = attacker.getPersistentData().getDouble("dmz_server_speed");
				boolean wasFlying = attacker.getPersistentData().getBoolean("dmz_was_flying");
				boolean isMomentumStrike = currentSpeed >= MomentumImpactHandler.MOMENTUM_SPEED_THRESHOLD && wasFlying;

				if (isMomentumStrike) {
					double ratio = Mth.clamp((currentSpeed - MomentumImpactHandler.MOMENTUM_SPEED_THRESHOLD) / (MomentumImpactHandler.MOMENTUM_MAX_SPEED - MomentumImpactHandler.MOMENTUM_SPEED_THRESHOLD), 0.0, 1.0);
					double multiplier = 1.15 + (ratio * 0.35);
					dmzDamage *= multiplier;

					double mx = attacker.getPersistentData().getDouble("dmz_momentum_x");
					double my = attacker.getPersistentData().getDouble("dmz_momentum_y");
					double mz = attacker.getPersistentData().getDouble("dmz_momentum_z");

					Vec3 knockbackDir = new Vec3(mx, my, mz).normalize();
					if (knockbackDir.lengthSqr() < 1.0E-6) knockbackDir = attacker.getLookAngle();

					livingTarget.setDeltaMovement(knockbackDir.scale(1.8));
					livingTarget.hurtMarked = true;

					MomentumImpactHandler.CollisionImpactType impactType = livingTarget.onGround() || knockbackDir.y < -0.5 ? MomentumImpactHandler.CollisionImpactType.GROUND : MomentumImpactHandler.CollisionImpactType.WALL;
					MomentumImpactHandler.registerCollisionImpact(livingTarget, impactType, (float)(dmzDamage * 0.3), knockbackDir);
					NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.6f, 0.05f, 2, false), livingTarget);
				}

				int baseStaminaRequired = (int) Math.ceil(staminaDamage * ConfigManager.getCombatConfig().getStaminaConsumptionRatio());
				double gravityMult = GravityLogic.getConsumptionMultiplier(attacker);
				int staminaRequired = (int) (baseStaminaRequired * gravityMult * attackerData.getAdjustedStaminaDrainMultiplier());

				long parryPenaltyEnd = attacker.getPersistentData().getLong("dmz_parry_penalty");
				if (System.currentTimeMillis() < parryPenaltyEnd) staminaRequired *= ConfigManager.getCombatConfig().getParryStaminaCostPenalty();

				float currentStamina = attackerData.getResources().getCurrentStamina();
				double finalDmzDamage;

				if (!attackerData.getStatus().isAlive() && attacker.level().dimension().equals(OtherworldDimension.OTHERWORLD_KEY)) staminaRequired = 0;

				boolean firstHit = !attacker.getPersistentData().contains("dmz_first_hit") || attacker.getPersistentData().getBoolean("dmz_first_hit");
				double staminaRatio;

				if (firstHit) {
					if (currentStamina >= staminaRequired) {
						if (!attacker.isCreative()) attackerData.getResources().removeStamina(staminaRequired);
						staminaRatio = 1.0;
					} else {
						staminaRatio = Mth.clamp((double) currentStamina / staminaRequired, 0.0, 1.0);
						if (!attacker.isCreative()) attackerData.getResources().setCurrentStamina(0);
					}
					attacker.getPersistentData().putDouble("dmz_swing_stamina_ratio", staminaRatio);
				} else {
					staminaRatio = attacker.getPersistentData().contains("dmz_swing_stamina_ratio")
							? attacker.getPersistentData().getDouble("dmz_swing_stamina_ratio") : 1.0;
				}

				finalDmzDamage = dmzDamage * staminaRatio;

				if (isEmptyHandOrNoDamageItem(attacker)) currentDamage[0] = finalDmzDamage;
				else currentDamage[0] = baseDamage + finalDmzDamage;

				double normalMeleeDamage = currentDamage[0];
				double kiWeaponBonus = 0.0;
				double kiInfuseBonus = 0.0;
				boolean kiWeaponInUse = false;

				boolean kiWeaponActive = attackerData.getSkills().isSkillActive("kimanipulation");
				int kiWeaponLevel = attackerData.getSkills().getSkillLevel("kimanipulation");
				float kiWeaponMult = kiWeaponLevel * 0.1f;
				if (kiWeaponActive && attacker.getMainHandItem().isEmpty()) {
					String weaponType = attackerData.getStatus().getKiWeaponType();
					var kiCfg = ConfigManager.getCombatConfig().getKiWeaponConfig(weaponType);
					if (kiCfg != null) {
						kiWeaponInUse = true;
						int kiCost = (int) Math.round(kiCfg.getBaseKiCost() + ConfigManager.getCombatConfig().getBaselineFormDrain() * kiCfg.getKiScalingCost());
						if (attackerData.getResources().getCurrentEnergy() >= kiCost) {
							kiWeaponBonus = kiCfg.getBaseDamage() + (attackerData.getKiDamage() * kiCfg.getKiScalingDamage() * kiWeaponMult);
							currentDamage[0] = currentDamage[0] + kiWeaponBonus;
						}
						if (!attacker.isCreative() && !isPunchMachine) attackerData.getResources().removeEnergy(kiCost);
					}
				}

				int kiInfusionLevel = attackerData.getSkills().getSkillLevel("ki_infusion");
				boolean kiInfuseActive = kiInfusionLevel > 0 && attackerData.getSkills().isSkillActive("ki_infusion");
				if (kiInfuseActive) {
					float maxKi = attackerData.getMaxEnergy();
					double baseCost = ConfigManager.getCombatConfig().getKiInfusionBaseCostPct();
					double maxCost = ConfigManager.getCombatConfig().getKiInfusionMaxCostPct();
					double damageMult = ConfigManager.getCombatConfig().getKiInfusionDamagePerLevel();

					double costPct = baseCost + (kiInfusionLevel - 1) * ((maxCost - baseCost) / 9.0);
					int kiCostInfusion = (int) (maxKi * (costPct / 100.0));

					if (attackerData.getResources().getCurrentEnergy() >= kiCostInfusion) {
						kiInfuseBonus = maxKi * (damageMult * kiInfusionLevel);
						currentDamage[0] += kiInfuseBonus;
						if (!attacker.isCreative() && !isPunchMachine) attackerData.getResources().removeEnergy(kiCostInfusion);
					}
				}

				double growthMeleeDamage = kiWeaponInUse ? (kiWeaponBonus + kiInfuseBonus) : (normalMeleeDamage + kiInfuseBonus);
				attacker.getPersistentData().putDouble("dmz_growth_melee_damage", growthMeleeDamage);
				attacker.getPersistentData().putBoolean("dmz_growth_ki_weapon", kiWeaponInUse);
				attacker.getPersistentData().putBoolean("dmz_growth_ki_infuse", kiInfuseActive);

				if (ConfigManager.getCombatConfig().getKillPlayersOnCombatLogout() && event.getEntity() instanceof Player) {
					attackerData.getCooldowns().addCooldown(Cooldowns.COMBAT, 200);
				}

				if (isPunchMachine) {
					((PunchMachineEntity) event.getEntity()).processHit((float) currentDamage[0], attacker);
					attackerData.getResources().addTrainingPoints(ConfigManager.getServerConfig().getGameplay().getTpPerHit());
					event.setCanceled(true);
					event.setAmount(0);
					return;
				}

				if (attacker instanceof ServerPlayer serverPlayer) {
					NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(serverPlayer), serverPlayer);
				}
			});
		}

		if (canceledByBlocking[0]) return;

		if (event.getEntity() instanceof Player victim) {
			if (isExcludedSource(source)) return;

			double finalDefensePenetration;
			double healingReduction = 0.0;
			if (source.getEntity() instanceof LivingEntity sourceLiving) {

				int healMainHandLvl = EnchantmentHelper.getTagEnchantmentLevel(MainEnchants.HEALING_REDUCTION.get(), sourceLiving.getMainHandItem());
				int healOffHandLvl = EnchantmentHelper.getTagEnchantmentLevel(MainEnchants.HEALING_REDUCTION.get(), sourceLiving.getOffhandItem());
				double enchHealReduction = Math.max(healMainHandLvl, healOffHandLvl) * 0.05;

				double skillHealReduction = 0.0;
				if (sourceLiving instanceof Player sourcePlayer) {
					var attackerStats = StatsProvider.get(StatsCapability.INSTANCE, sourcePlayer).orElse(null);
					if (attackerStats != null) {
						skillHealReduction = attackerStats.getSkills().getSkillLevel("healing_reduction") * 0.02;
					}
				}

				finalDefensePenetration = computeDefensePenetration(sourceLiving, source, passiveDefensePen[0]);
				healingReduction = Math.min(HEALING_REDUCTION_CAP, enchHealReduction + skillHealReduction);
			} else finalDefensePenetration = 0.0;

			final double finalHealingReduction = healingReduction;


			StatsProvider.get(StatsCapability.INSTANCE, victim).ifPresent(victimData -> {
				if (victimData.getStatus().isHasCreatedCharacter()) {
					victimData.getStatus().setLastHurtTime(System.currentTimeMillis());

					if (finalHealingReduction > 0.0)
						victimData.getSecondaryStatEffects().apply(SecondaryStatEffects.HP_REGEN, -finalHealingReduction, HEALING_REDUCTION_DURATION_TICKS);

					maybeForceCombatFly(victim);
					if (source.getEntity() instanceof Player attackerPlayer) maybeForceCombatFly(attackerPlayer);
					if (source.getEntity() instanceof LivingEntity sourceLiving) victim.getPersistentData().putInt(DMZ_LAST_ATTACKER_ID_TAG, sourceLiving.getId());

					boolean isPvP = source.getEntity() instanceof Player;
					if (ConfigManager.getCombatConfig().getKillPlayersOnCombatLogout() && isPvP) victimData.getCooldowns().addCooldown(Cooldowns.COMBAT, 120);

					double blockMultiplier = 1.0;

					boolean techCharging = victimData.getTechniques().isTechniqueCharging();
					boolean techFiring = !techCharging && TechniqueDispatcher.isFiringKiAttack(victim);
					boolean techActive = techCharging || techFiring;

					if (techActive) {
						Entity sourceEntity = source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();
						float reductionMult = techFiring ? 0.25f : 0.5f;
						float poiseMult = techFiring ? 4.0f : 2.0f;

						double poiseDamageMultiplier = ConfigManager.getCombatConfig().getPoiseDamageMultiplier();
						if (!(sourceEntity instanceof Player)) poiseDamageMultiplier *= 1.5;
						float poiseDamage = (float) (currentDamage[0] * poiseDamageMultiplier * poiseMult);
						float currentPoise = victimData.getResources().getCurrentPoise();

						if (currentPoise - poiseDamage <= 0) {
							doGuardBreak(victim, victimData);
							cancelActiveTechnique(victim, victimData);
						} else {
							victimData.getResources().removePoise((int) poiseDamage);
							int regenCd = ConfigManager.getCombatConfig().getPoiseRegenCooldown();
							victimData.getCooldowns().setCooldown(Cooldowns.POISE_CD, regenCd);
							victim.addEffect(new MobEffectInstance(MainEffects.POISE_CD.get(), regenCd, 0, false, false, true));
							blockMultiplier = reductionMult;
						}
					}

					if (!techActive && ConfigManager.getCombatConfig().getEnableBlocking()) {
						Entity sourceEntity = source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();
						if (victimData.getStatus().isBlocking() && !victimData.getStatus().isStunEffect() && sourceEntity != null) {
							Vec3 targetLook = victim.getLookAngle();
							Vec3 sourceLoc = sourceEntity.position();
							Vec3 targetLoc = victim.position();
							Vec3 directionToSource = sourceLoc.subtract(targetLoc).normalize();

							if (targetLook.dot(directionToSource) > 0.0) {
								long currentTime = System.currentTimeMillis();
								long blockTime = victimData.getStatus().getLastBlockTime();
								int parryWindow = ConfigManager.getCombatConfig().getParryWindowMs();
								boolean isParry = ((currentTime - blockTime) <= parryWindow) && ConfigManager.getCombatConfig().getEnableParrying();

								double poiseMultiplier = ConfigManager.getCombatConfig().getPoiseDamageMultiplier();
								if (!(sourceEntity instanceof Player)) poiseMultiplier *= 1.5;
								float poiseDamage = (float) (currentDamage[0] * poiseMultiplier);

								if (isParry) poiseDamage *= 0.66f;

								float currentPoise = victimData.getResources().getCurrentPoise();
								float currentStamina = victimData.getResources().getCurrentStamina();
								int blockStaminaCost = (int) (event.getAmount() * ConfigManager.getCombatConfig().getBlockStaminaCost());

								if (currentPoise - poiseDamage <= 0 || currentStamina - blockStaminaCost <= 0) {
									doGuardBreak(victim, victimData);

									if (victim.level() instanceof ServerLevel serverLevel) {
										Vec3 look = victim.getLookAngle();
										Vec3 spawnPos = victim.getEyePosition().add(look.scale(0.6)).subtract(0, 0.3, 0);
										serverLevel.sendParticles(MainParticles.GUARD_BLOCK.get(), spawnPos.x, spawnPos.y, spawnPos.z, 0, 1.0, 0.1, 0.1, 1.0);
									}
								} else {
									victimData.getResources().removePoise((int) poiseDamage);
									victimData.getResources().removeStamina(blockStaminaCost);
									wasBlocked[0] = true;

									int regenCd = ConfigManager.getCombatConfig().getPoiseRegenCooldown();
									victimData.getCooldowns().setCooldown(Cooldowns.POISE_CD, regenCd);
									victim.addEffect(new MobEffectInstance(MainEffects.POISE_CD.get(), regenCd, 0, false, false, true));

									if (isParry) {
										wasParry[0] = true;
										blockMultiplier = 0.0;
										if (sourceEntity instanceof LivingEntity attackerLiving) {
											attackerLiving.knockback(1.5D, victim.getX() - attackerLiving.getX(), victim.getZ() - attackerLiving.getZ());
											attackerLiving.setDeltaMovement(attackerLiving.getDeltaMovement().scale(0.5));
											attackerLiving.addEffect(new MobEffectInstance(MainEffects.STAGGER.get(), 60, 1, false, false, true));
											attackerLiving.getPersistentData().putLong("dmz_parry_penalty", System.currentTimeMillis() + 4000);
										}
										if (MainDamageTypes.isKiblastDamage(source)) {
											divertKiProjectile(source, victim);
										}
										if (MainDamageTypes.isStrikeAttackDamage(source)) {
											applyStrikeCounterGuardBreak(sourceEntity);
											boolean isGuardBrokenTmp = victimData.getStatus().isStunEffect() && victimData.getResources().getCurrentPoise() <= 0;
											double estimatedPostMitigation = victimData.calculatePostMitigationDamage(currentDamage[0], isGuardBrokenTmp, finalDefensePenetration);
											if (!(estimatedPostMitigation <= 0.0)) victimData.getResources().removeStamina((float) (estimatedPostMitigation * 0.5));
										}
										victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), MainSounds.PARRY.get(), SoundSource.PLAYERS, 1.0F, 0.9F + victim.getRandom().nextFloat() * 0.1F);

										if (victim.level() instanceof ServerLevel serverLevel) {
											Vec3 look = victim.getLookAngle();
											Vec3 spawnPos = victim.getEyePosition().add(look.scale(0.6)).subtract(0, 0.3, 0);

											serverLevel.sendParticles(MainParticles.GUARD_BLOCK.get(), spawnPos.x, spawnPos.y, spawnPos.z, 0, 1.0, 1.0, 1.0, 1.0);
											for (int i = 0; i < 15; i++) {
												serverLevel.sendParticles(MainParticles.KI_TRAIL.get(), spawnPos.x, spawnPos.y, spawnPos.z, 0, 1.0, 1.0, 1.0, 1.0);
												serverLevel.sendParticles(MainParticles.SPARKS.get(), spawnPos.x, spawnPos.y, spawnPos.z, 0, 1.0, 1.0, 1.0, 1.0);
											}
										}
									} else {
										double defense = victimData.getDefenseLegacyUnits();
										double reductionCap = ConfigManager.getCombatConfig().getBlockDamageReductionCap();
										double reductionMin = ConfigManager.getCombatConfig().getBlockDamageReductionMin();
										double mitigationPct = (defense * 3.0) / (currentDamage[0] + (defense * 3.0));
										mitigationPct = Math.min(reductionCap, Math.max(mitigationPct, reductionMin));

										blockMultiplier = 1.0 - mitigationPct;

										int randomSound = victim.getRandom().nextInt(3);
										SoundEvent soundToPlay = randomSound == 0 ? MainSounds.BLOCK1.get() : (randomSound == 1 ? MainSounds.BLOCK2.get() : MainSounds.BLOCK3.get());

										victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), soundToPlay, SoundSource.PLAYERS, 1.0F, 0.9F + victim.getRandom().nextFloat() * 0.1F);

										if (victim.level() instanceof ServerLevel serverLevel) {
											double maxPoise = victimData.getMaxPoise();
											double currentPoiseVal = victimData.getResources().getCurrentPoise();
											double percentage = (currentPoiseVal / maxPoise) * 100.0;
											double r, g, b;

											if (percentage > 66) { r = 0.2; g = 0.9; b = 1.0; }
											else if (percentage > 33) { r = 1.0; g = 0.5; b = 0.0; }
											else { r = 1.0; g = 0.1; b = 0.1; }

											Vec3 look = victim.getLookAngle();
											Vec3 spawnPos = victim.getEyePosition().add(look.scale(0.6)).subtract(0, 0.3, 0);
											serverLevel.sendParticles(MainParticles.BLOCK_PARTICLE.get(), spawnPos.x, spawnPos.y, spawnPos.z, 0, r, g, b, 1.0);
										}
									}

									if (victim instanceof ServerPlayer sPlayer) {
										boolean isGuardBrokenTmp = victimData.getStatus().isStunEffect() && victimData.getResources().getCurrentPoise() <= 0;
										double estimatedPostMitigation = victimData.calculatePostMitigationDamage(currentDamage[0], isGuardBrokenTmp, finalDefensePenetration);
										float finalDmg = (float) (estimatedPostMitigation * blockMultiplier);

										DMZEvent.PlayerBlockEvent blockEvent = new DMZEvent.PlayerBlockEvent(sPlayer, source.getEntity() instanceof LivingEntity ? (LivingEntity) source.getEntity() : null, (float)currentDamage[0], finalDmg, isParry, poiseDamage);
										MinecraftForge.EVENT_BUS.post(blockEvent);

										if (blockEvent.isCanceled()) {
											wasBlocked[0] = false;
											wasParry[0] = false;
											blockMultiplier = 1.0;
										}
									}
								}
							}
						}
					}

					victim.getPersistentData().putDouble("dmz_raw_damage", currentDamage[0]);
					victim.getPersistentData().putDouble("dmz_block_multiplier", blockMultiplier);
					victim.getPersistentData().putDouble("dmz_defense_pen", finalDefensePenetration);

					if (victim instanceof ServerPlayer serverPlayer) {
						NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(serverPlayer), serverPlayer);
					}
				}
			});
		} else if (!isExcludedSource(source) && source.getEntity() instanceof LivingEntity sourceLiving) {
			double penetration = computeDefensePenetration(sourceLiving, source, passiveDefensePen[0]);
			if (penetration > 0.0) currentDamage[0] *= (1.0 + penetration);
		}

		if (currentDamage[0] >= 200 && currentDamage[0] >= event.getEntity().getMaxHealth() * 0.5) {
			NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.8f, 0.05f, 2, true), event.getEntity());
		}

		event.setAmount((float) currentDamage[0]);

		if (!event.isCanceled() && source.getMsgId().equals("player")
				&& source.getEntity() instanceof Player dmgAttacker) {
			MinecraftForge.EVENT_BUS.post(new DMZEvent.DamageDealtEvent(
					dmgAttacker, event.getEntity(), currentDamage[0], wasBlocked[0], wasParry[0], DMZEvent.DamageSourceType.MELEE));
		}

		if (!event.isCanceled()
				&& source.getMsgId().equals("player")
				&& source.getEntity() instanceof Player attacker
				&& attacker.level() instanceof ServerLevel serverLevel
				&& event.getAmount() > 0.0F
				&& !wasBlocked[0]
				&& !wasParry[0]
				&& attacker instanceof Player_DMZ dmzAttacker) {
			var currentAttack = dmzAttacker.getCurrentAttack();
			if (currentAttack != null && currentAttack.attack() != null) {
				SoundHelper.playSound(serverLevel, event.getEntity(), currentAttack.attack().impactSound());
			}
		}
	}

	private static void divertKiProjectile(DamageSource source, Player defender) {
		Entity direct = source.getDirectEntity();
		if (!(direct instanceof AbstractKiProjectile projectile)) return;
		Vec3 currentVelocity = projectile.getDeltaMovement();
		double speed = Math.max(0.2, currentVelocity.length());
		Vec3 randomDir = new Vec3(defender.getRandom().nextDouble() - 0.5, defender.getRandom().nextDouble() - 0.25, defender.getRandom().nextDouble() - 0.5);
		if (randomDir.lengthSqr() < 1.0E-6) randomDir = defender.getLookAngle().scale(-1.0);
		Vec3 newVelocity = randomDir.normalize().scale(speed);
		projectile.setDeltaMovement(newVelocity);
		projectile.hasImpulse = true;
		projectile.hurtMarked = true;
	}

	private static void applyStrikeCounterGuardBreak(Entity sourceEntity) {
		if (!(sourceEntity instanceof Player attacker)) return;
		StatsProvider.get(StatsCapability.INSTANCE, attacker).ifPresent(attackerData -> {
			doGuardBreak(attacker, attackerData);
			if (attacker instanceof ServerPlayer serverPlayer) {
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(serverPlayer), serverPlayer);
			}
		});
	}

	private static void cancelActiveTechnique(Player player, StatsData data) {
		data.getTechniques().clearTechniqueCharge();
		var owned = player.level().getEntitiesOfClass(AbstractKiProjectile.class,
				player.getBoundingBox().inflate(64.0),
				p -> p.getOwner() != null && p.getOwner().getUUID().equals(player.getUUID()));
		for (AbstractKiProjectile p : owned) p.discard();
	}

	private static void doGuardBreak(Player attacker, StatsData attackerData) {
		attackerData.getResources().setCurrentPoise(0);
		attackerData.getResources().setCurrentStamina(0);
		attackerData.getStatus().setBlocking(false);
		int stunDuration = ConfigManager.getCombatConfig().getBlockBreakStunDurationTicks();
		attacker.addEffect(new MobEffectInstance(MainEffects.STUN.get(), stunDuration, 0, false, false, true));
		int regenCd = ConfigManager.getCombatConfig().getPoiseRegenCooldown();
		attackerData.getCooldowns().setCooldown(Cooldowns.POISE_CD, regenCd);
		attacker.addEffect(new MobEffectInstance(MainEffects.POISE_CD.get(), regenCd, 0, false, false, true));
		attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), MainSounds.UNBLOCK.get(), SoundSource.PLAYERS, 1.0F, 0.9F + attacker.getRandom().nextFloat() * 0.1F);
	}

	private static boolean isSpecificKiAttack(DamageSource source) {
		if (MainDamageTypes.isKiblastDamage(source)) {
			Entity projectile = source.getDirectEntity();

			if (projectile instanceof AbstractKiProjectile kiProj) {
				AbstractKiProjectile.KiType type = kiProj.getKiType();
				return type == AbstractKiProjectile.KiType.GIANT_BALL || type == AbstractKiProjectile.KiType.EXPLOSION;
			}
			return true;
		}
		return false;
	}

	private static double computeDefensePenetration(LivingEntity sourceLiving, DamageSource source, double passiveDefensePen) {
		double skillPen = 0.0;
		if (sourceLiving instanceof Player sourcePlayer) {
			var attackerStats = StatsProvider.get(StatsCapability.INSTANCE, sourcePlayer).orElse(null);
			if (attackerStats != null) skillPen = attackerStats.getSkills().getSkillLevel("defense_penetration") * 0.025;
		}

		double sourcePen;
		if (MainDamageTypes.isKiblastDamage(source)) {
			sourcePen = getKiAttackArmorPen(source);
		} else if (MainDamageTypes.isStrikeAttackDamage(source)) {
			sourcePen = 0.0;
		} else {
			int mainHandLvl = EnchantmentHelper.getTagEnchantmentLevel(MainEnchants.DEFENSE_PENETRATION.get(), sourceLiving.getMainHandItem());
			int offHandLvl = EnchantmentHelper.getTagEnchantmentLevel(MainEnchants.DEFENSE_PENETRATION.get(), sourceLiving.getOffhandItem());
			sourcePen = Math.max(mainHandLvl, offHandLvl) * 0.025;
		}

		return Math.min(0.50, skillPen + sourcePen + passiveDefensePen);
	}

	private static double getKiAttackArmorPen(DamageSource source) {
		if (!(source.getDirectEntity() instanceof AbstractKiProjectile projectile)) return 0.0;
		if (!(projectile.getOwner() instanceof Player owner)) return 0.0;
		String techId = projectile.getTechniqueId();
		if (techId == null || techId.isEmpty()) return 0.0;
		var stats = StatsProvider.get(StatsCapability.INSTANCE, owner).orElse(null);
		if (stats == null) return 0.0;
		TechniqueData tech = stats.getTechniques().getUnlockedTechniques().get(techId);
		return tech instanceof KiAttackData kiData ? kiData.getActualArmorPenetration() / 100.0 : 0.0;
	}

	private static boolean isEmptyHandOrNoDamageItem(Player player) {
		ItemStack mainHand = player.getMainHandItem();
		if (mainHand.isEmpty()) return true;
		var attackDamageModifier = mainHand.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE);
		return attackDamageModifier.isEmpty();
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void overrideVanillaArmorReduction(LivingDamageEvent event) {
		if (event.getEntity() instanceof Player victim) {
			if (victim.getPersistentData().contains("dmz_raw_damage")) {
				double rawDamage = victim.getPersistentData().getDouble("dmz_raw_damage");
				double defensePenetration = victim.getPersistentData().contains("dmz_defense_pen") ?
						victim.getPersistentData().getDouble("dmz_defense_pen") : 0.0;

				StatsProvider.get(StatsCapability.INSTANCE, victim).ifPresent(stats -> {
					boolean isGuardBroken = stats.getStatus().isStunEffect() && stats.getResources().getCurrentPoise() <= 0;
					double postMitigation = stats.calculatePostMitigationDamage(rawDamage, isGuardBroken, defensePenetration);

					if (victim.getPersistentData().contains("dmz_block_multiplier")) {
						postMitigation *= victim.getPersistentData().getDouble("dmz_block_multiplier");
						victim.getPersistentData().remove("dmz_block_multiplier");
					}

					int kiProtectionLevel = stats.getSkills().getSkillLevel("kiprotection");
					if (kiProtectionLevel > 0 && stats.getSkills().isSkillActive("kiprotection")) {
						double mitigationPerLevel = ConfigManager.getCombatConfig().getKiProtectionMitigationPerLevel();
						double costRatio = ConfigManager.getCombatConfig().getKiProtectionCostRatio();
						double mitigationPct = kiProtectionLevel * mitigationPerLevel;
						int kiCost = (int) Math.ceil(postMitigation * costRatio);
						float currentEnergy = stats.getResources().getCurrentEnergy();

						if (kiCost > 0) {
							if (currentEnergy >= kiCost) {
								postMitigation *= (1.0 - mitigationPct);
								if (!victim.isCreative()) stats.getResources().removeEnergy(kiCost);
							} else if (currentEnergy > 0) {
								double affordableRatio = (double) currentEnergy / kiCost;
								postMitigation *= (1.0 - (mitigationPct * affordableRatio));
								if (!victim.isCreative()) stats.getResources().setCurrentEnergy(0);
							}
						}
					}

					float finalDamage = (float) postMitigation;
					if (!Float.isFinite(finalDamage) || finalDamage < 0.0f) finalDamage = 0.0f;

					if (victim.getHealth() - finalDamage <= 0) {
						Entity damageSource = event.getSource().getEntity();
						boolean shadowKnockdown = damageSource instanceof ShadowDummyEntity dummy
								&& dummy.getPersistentData().getBoolean(SummonPlayerShadowDummyC2S.TAG_PLAYER_SHADOW);
						boolean friendlyKnockdown = false;
						if (damageSource instanceof Player attacker) {
							boolean isSamePartyPvp = PartyManager.areInSameParty(attacker, victim) && PartyManager.isPartyPvpEnabled(attacker);
							boolean isFriendlyFist = StatsProvider.get(StatsCapability.INSTANCE, attacker)
									.map(data -> data.getStatus().isFriendlyFistEnabled())
									.orElse(false);
							friendlyKnockdown = isSamePartyPvp || isFriendlyFist;
						}

						if (shadowKnockdown || friendlyKnockdown) {
							finalDamage = Math.max(0.0F, victim.getHealth() - 1.0F);

							stats.getStatus().setKnockedDown(true);
							stats.getCooldowns().setCooldown(Cooldowns.KNOCKDOWN_DURATION, ConfigManager.getCombatConfig().getKnockdownDurationSeconds() * 20);
							stats.getCharacter().clearActiveForm();
							stats.getCharacter().clearActiveStackForm();

							if (victim instanceof ServerPlayer serverPlayer) {
								NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(serverPlayer), serverPlayer);
							}

							if (shadowKnockdown) {
								SummonPlayerShadowDummyC2S.dismissByDummy((ShadowDummyEntity) damageSource);
							}
						}
					}

					Entity dmzDebugAttacker = event.getSource().getEntity();
					if (dmzDebugAttacker instanceof LivingEntity && !(dmzDebugAttacker instanceof Player)) {
						double dmzDebugDefense = stats.getDefense();
						double dmzDebugMitigatedPct = rawDamage > 0.0 ? (1.0 - (finalDamage / rawDamage)) * 100.0 : 0.0;
						LogUtil.info(Env.SERVER,
								"[DEF-DEBUG] victim={} attacker={} type={} raw={} defense={} guardBroken={} defPen={} final={} mitigated={}%",
								victim.getName().getString(),
								dmzDebugAttacker.getName().getString(),
								event.getSource().getMsgId(),
								String.format("%.1f", rawDamage),
								String.format("%.1f", dmzDebugDefense),
								isGuardBroken,
								String.format("%.2f", defensePenetration),
								String.format("%.1f", (double) finalDamage),
								String.format("%.1f", dmzDebugMitigatedPct));
					}

					event.setAmount(finalDamage);
				});

				victim.getPersistentData().remove("dmz_raw_damage");
				victim.getPersistentData().remove("dmz_defense_pen");
			}
		}
	}

	private static boolean isExcludedSource(DamageSource source) {
		return source.is(DamageTypes.FALL) ||
				source.is(DamageTypes.FELL_OUT_OF_WORLD) ||
				source.is(DamageTypes.OUTSIDE_BORDER) ||
				source.is(DamageTypes.STARVE) ||
				source.is(DamageTypes.WITHER) ||
				source.is(DamageTypes.WITHER_SKULL) ||
				source.is(DamageTypes.DROWN) ||
				source.is(DamageTypes.DRAGON_BREATH) ||
				source.is(DamageTypes.FALLING_ANVIL) ||
				source.is(DamageTypes.BAD_RESPAWN_POINT) ||
				source.is(DamageTypes.MAGIC) ||
				source.is(DamageTypes.INDIRECT_MAGIC);
	}
}
