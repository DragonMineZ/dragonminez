package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.PunchMachineEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.network.C2S.MeleeAttackIntentC2S;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.ComboManager;
import com.dragonminez.server.util.GravityLogic;
import com.dragonminez.server.world.dimension.OtherworldDimension;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CombatEvent {
	private static final Map<UUID, Integer> ATTACK_CHAIN_INDEX = new HashMap<>();
	private static final Map<UUID, Long> ATTACK_CHAIN_LAST_MS = new HashMap<>();
	private static final Map<UUID, DMZEvent.PlayerAttackStartEvent.AttackType> ATTACK_CHAIN_LAST_TYPE = new HashMap<>();
	private static final Map<UUID, Boolean> ATTACK_CHAIN_MIXED = new HashMap<>();
	private static final Map<UUID, AttackExecutionContext> ATTACK_EXECUTION_CONTEXTS = new HashMap<>();
	private static final Map<UUID, AttackExecutionContext> LAST_CONSUMED_CONTEXT = new HashMap<>();
	private static final Map<UUID, Long> LAST_CONSUMED_CONTEXT_MS = new HashMap<>();
	private static final String DMZ_ATTACK_TYPE_TAG = "dmz_attack_type";
	private static final String DMZ_ATTACK_FINISHER_TAG = "dmz_attack_finisher";
	private static final String DMZ_ATTACK_CHARGED_TAG = "dmz_attack_charged";
	private static final String DMZ_ATTACK_CUSTOM_TAG = "dmz_attack_custom";
	private static final String DMZ_LAST_ATTACKER_ID_TAG = "dmz_last_attacker_id";
	private static final Map<UUID, AttackIntentData> ATTACK_INTENTS = new HashMap<>();
	private static final Map<UUID, CollisionImpactContext> COLLISION_IMPACTS = new HashMap<>();
	private static final Map<UUID, Boolean> DMZ_COMBAT_STYLE_ENABLED = new HashMap<>();
	private static final Map<String, Long> LAST_PLAYER_HIT_GUARD_MS = new HashMap<>();
	private static final Map<UUID, Long> MANUAL_ATTACK_WINDOW_MS = new HashMap<>();

	private enum CollisionImpactType {
		WALL,
		GROUND
	}

	public record AttackExecutionContext(
			int attackIndex,
			boolean offhandAttack,
			boolean finisher,
			boolean mixedChain,
			DMZEvent.PlayerAttackStartEvent.AttackType attackType,
			int chargeTicks,
			boolean charged,
			long expiryMs,
			int remainingHits
	) {}

	public record CollisionImpactContext(
			CollisionImpactType type,
			long expiryMs,
			double startY,
			float extraDamage
	) {}

	public record AttackIntentData(
			DMZEvent.PlayerAttackStartEvent.AttackType attackType,
			int chargeTicks,
			long expiryMs
	) {}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onLivingHurt(LivingHurtEvent event) {
		DamageSource source = event.getSource();
		final double[] currentDamage = {event.getAmount()};

		if (source.getEntity() instanceof LivingEntity livingAttacker && livingAttacker.hasEffect(MainEffects.STUN.get())) {
			event.setCanceled(true);
			return;
		}

		// Attacker Damage Event
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

			boolean isPunchMachine = event.getEntity() instanceof PunchMachineEntity;

			StatsProvider.get(StatsCapability.INSTANCE, attacker).ifPresent(attackerData -> {
				if (!attackerData.getStatus().isHasCreatedCharacter()) return;
				boolean useCustomCombat = canUseCustomCombat(attacker);
				AttackIntentData attackIntent = resolveAttackIntent(attacker);
				DMZEvent.PlayerAttackStartEvent.AttackType attackType = attackIntent.attackType();
				AttackExecutionContext attackContext = useCustomCombat
						? pollOrCreateAttackContext(attacker, attackIntent)
						: new AttackExecutionContext(0, false, false, false, attackType, 0, false, 0L, 0);
				if (useCustomCombat
						&& attackContext.attackType() == DMZEvent.PlayerAttackStartEvent.AttackType.LIGHT
						&& attackContext.chargeTicks() > 0
						&& attackContext.chargeTicks() < MeleeAttackIntentC2S.MIN_EFFECTIVE_CHARGE_TICKS
						&& !consumeManualAttackWindow(attacker)) {
					event.setCanceled(true);
					return;
				}
				int attackIndex = attackContext.attackIndex();
				boolean offhandAttack = attackContext.offhandAttack();

				double mcBaseDamage = currentDamage[0];
				double mcHandDamage = useCustomCombat
						? getMcDamageForAttackHand(attacker, offhandAttack, mcBaseDamage)
						: mcBaseDamage;
				if (useCustomCombat) {
					double dmzDamage = attackerData.getMeleeDamage();
					double dmgNoMult = attackerData.getMeleeDamageWithoutMults();
					double baseDmzDamage = dmzDamage;
					if (ConfigManager.getServerConfig().getCombat().getRespectAttackCooldown()) {
						float adjustedStrength = attacker.getAttackStrengthScale(0.5F);

						if (attackerData.getCharacter().hasActiveForm()) {
							FormConfig.FormData activeForm = attackerData.getCharacter().getActiveFormData();
							if (activeForm != null) {
								adjustedStrength *= activeForm.getAttackSpeed().floatValue();
							}
						}

						if (adjustedStrength > 1.0F) adjustedStrength = 1.0F;

						float damageScale = 0.2F + adjustedStrength * adjustedStrength * 0.8F;
						dmzDamage *= damageScale;
						dmgNoMult *= damageScale;
					}

					double chargedBonus = attackContext.charged()
							? calculateChargedBonus(attackerData, attackContext.chargeTicks())
							: 0.0;
					dmzDamage += chargedBonus;
					double chargedScale = baseDmzDamage > 0.0 ? (dmzDamage / baseDmzDamage) : 1.0;
					dmgNoMult *= chargedScale;

					double typeScale = getAttackTypeDamageScale(attackContext.attackType(), attackContext.finisher(), attackContext.mixedChain());
					dmzDamage *= typeScale;
					dmgNoMult *= typeScale;

					int baseStaminaRequired = (int) Math.ceil(dmgNoMult * ConfigManager.getServerConfig().getCombat().getStaminaConsumptionRatio());
					double gravityMult = GravityLogic.getConsumptionMultiplier(attacker);
					baseStaminaRequired = (int) (baseStaminaRequired * gravityMult);
					double staminaDrainMultiplier = attackerData.getAdjustedStaminaDrainMultiplier();
					int staminaRequired = (int) Math.ceil(baseStaminaRequired * staminaDrainMultiplier);
					float currentStamina = attackerData.getResources().getCurrentStamina();

					ComboManager.resetCombo(attacker.getUUID());

					double finalDmzDamage;
					if (!attackerData.getStatus().isAlive() && attacker.level().dimension().equals(OtherworldDimension.OTHERWORLD_KEY))
						currentStamina = 0;
					if (currentStamina >= staminaRequired) {
						finalDmzDamage = dmzDamage;
						if (!attacker.isCreative()) attackerData.getResources().removeStamina(staminaRequired);
					} else {
						double staminaRatio = (double) currentStamina / staminaRequired;
						finalDmzDamage = dmzDamage * staminaRatio;
						if (!attacker.isCreative()) attackerData.getResources().setCurrentStamina(0);
					}

					if (isHandEmptyOrNoDamageItem(attacker, offhandAttack)) {
						currentDamage[0] = finalDmzDamage;
					} else {
						currentDamage[0] = mcHandDamage + finalDmzDamage;
					}

					boolean kiWeaponActive = attackerData.getSkills().isSkillActive("kimanipulation");

					if (kiWeaponActive) {
					String weaponType = attackerData.getStatus().getKiWeaponType();
					int kiCost = 0;
					switch (weaponType.toLowerCase()) {
						case "blade" -> {
							kiCost = (int) Math.round(ConfigManager.getServerConfig().getCombat().getBaselineFormDrain() * ConfigManager.getServerConfig().getCombat().getKiBladeConfig()[1]);
							if (attackerData.getResources().getCurrentEnergy() >= kiCost) {
								currentDamage[0] = currentDamage[0] + attackerData.getKiDamage() * ConfigManager.getServerConfig().getCombat().getKiBladeConfig()[0];
							}
						}
						case "scythe" -> {
							kiCost = (int) Math.round(ConfigManager.getServerConfig().getCombat().getBaselineFormDrain() * ConfigManager.getServerConfig().getCombat().getKiScytheConfig()[1]);
							if (attackerData.getResources().getCurrentEnergy() >= kiCost) {
								currentDamage[0] = currentDamage[0] + attackerData.getKiDamage() * ConfigManager.getServerConfig().getCombat().getKiScytheConfig()[0];
							}
						}
						case "clawlance" -> {
							kiCost = (int) Math.round(ConfigManager.getServerConfig().getCombat().getBaselineFormDrain() * ConfigManager.getServerConfig().getCombat().getKiClawLanceConfig()[1]);
							if (attackerData.getResources().getCurrentEnergy() >= kiCost) {
								currentDamage[0] = currentDamage[0] + attackerData.getKiDamage() * ConfigManager.getServerConfig().getCombat().getKiClawLanceConfig()[0];
							}
						}
					}
						if (!attacker.isCreative() || !isPunchMachine) attackerData.getResources().removeEnergy(kiCost);
					}
				} else {
					resetAttackChain(attacker);
					currentDamage[0] = mcBaseDamage;
				}

				if (attacker instanceof ServerPlayer serverAttacker) {
					LivingEntity victimEntity = event.getEntity();
					victimEntity.getPersistentData().putBoolean(DMZ_ATTACK_CUSTOM_TAG, useCustomCombat);
					victimEntity.getPersistentData().putString(DMZ_ATTACK_TYPE_TAG, attackContext.attackType().name());
					victimEntity.getPersistentData().putBoolean(DMZ_ATTACK_FINISHER_TAG, attackContext.finisher());
					victimEntity.getPersistentData().putBoolean(DMZ_ATTACK_CHARGED_TAG, attackContext.charged());
					DMZEvent.PlayerAttackHitEvent hitEvent = new DMZEvent.PlayerAttackHitEvent(
							serverAttacker,
							victimEntity,
							(float) currentDamage[0],
							(float) currentDamage[0],
							attackIndex,
							offhandAttack,
							useCustomCombat,
							attackContext.attackType(),
							DMZEvent.PlayerAttackStartEvent.AttackPhase.ACTIVE,
							attackContext.finisher() || attackContext.charged()
					);
					MinecraftForge.EVENT_BUS.post(hitEvent);
					if (hitEvent.isCanceled()) {
						event.setCanceled(true);
						return;
					}
					currentDamage[0] = hitEvent.getFinalDamage();
				}

				if (attacker instanceof ServerPlayer serverPlayer)
					NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(serverPlayer), serverPlayer);

				if (useCustomCombat) {
					LivingEntity livingVictim = event.getEntity();
					boolean controlFromFinisher = attackContext.attackType() == DMZEvent.PlayerAttackStartEvent.AttackType.HEAVY
							? attackContext.attackIndex() == 4
							: attackContext.attackIndex() == 8;
					boolean applyControl = controlFromFinisher || attackContext.charged();
					applyControlEffects(attacker, livingVictim, attackContext.attackType(), applyControl);
				}

				if (event.getEntity() instanceof Player) {
					if (ConfigManager.getServerConfig().getCombat().getKillPlayersOnCombatLogout())
						attackerData.getCooldowns().addCooldown(Cooldowns.COMBAT, 200);
				}

				if (event.getEntity() instanceof PunchMachineEntity punchMachineEntity) {
					punchMachineEntity.processHit((float) currentDamage[0], attacker);
					int baseTps = ConfigManager.getServerConfig().getGameplay().getTpPerHit();
					attackerData.getResources().addTrainingPoints(baseTps);
					event.setCanceled(true);
					event.setAmount(0);
					return;
				}

				event.setAmount((float) currentDamage[0]);
			});
		}

		// Victim Defense Event
		if (event.getEntity() instanceof Player victim) {
			StatsProvider.get(StatsCapability.INSTANCE, victim).ifPresent(victimData -> {
				if (victimData.getStatus().isHasCreatedCharacter()) {
					victimData.getStatus().setLastHurtTime(System.currentTimeMillis());
					if (source.getEntity() instanceof LivingEntity sourceLiving) {
						victim.getPersistentData().putInt(DMZ_LAST_ATTACKER_ID_TAG, sourceLiving.getId());
					}
					boolean isPvP = source.getEntity() instanceof Player;
					if (ConfigManager.getServerConfig().getCombat().getKillPlayersOnCombatLogout() && isPvP)
						victimData.getCooldowns().addCooldown(Cooldowns.COMBAT, 120);
					double defense = victimData.getDefense();
					boolean blocked = false;

					if (ConfigManager.getServerConfig().getCombat().getEnableBlocking()) {
						Entity sourceEntity = source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();
						if (victimData.getStatus().isBlocking() && !victimData.getStatus().isStunned() && sourceEntity != null) {
							Vec3 targetLook = victim.getLookAngle();
							Vec3 sourceLoc = sourceEntity.position();
							Vec3 targetLoc = victim.position();
							Vec3 directionToSource = sourceLoc.subtract(targetLoc).normalize();

							if (targetLook.dot(directionToSource) > 0.0) {
								long currentTime = System.currentTimeMillis();
								long blockTime = victimData.getStatus().getLastBlockTime();
								int parryWindow = ConfigManager.getServerConfig().getCombat().getParryWindowMs();
								boolean isParry = ((currentTime - blockTime) <= parryWindow) && ConfigManager.getServerConfig().getCombat().getEnableParrying();

								double poiseMultiplier = ConfigManager.getServerConfig().getCombat().getPoiseDamageMultiplier();
								if (!(sourceEntity instanceof Player)) poiseMultiplier *= 1.5;
								float poiseDamage = (float) (currentDamage[0] * poiseMultiplier);

								if (isParry) poiseDamage *= 0.66f;
								float currentPoise = victimData.getResources().getCurrentPoise();

								if (currentPoise - poiseDamage <= 0) {
									victimData.getResources().setCurrentPoise(0);
									victimData.getStatus().setBlocking(false);

									int stunDuration = ConfigManager.getServerConfig().getCombat().getBlockBreakStunDurationTicks();
									victim.addEffect(new MobEffectInstance(MainEffects.STUN.get(), stunDuration, 0, false, false, true));
									int regenCd = ConfigManager.getServerConfig().getCombat().getPoiseRegenCooldown();
									victimData.getCooldowns().setCooldown(Cooldowns.POISE_CD, regenCd);
									victim.addEffect(
											new MobEffectInstance(MainEffects.POISE_CD.get(), regenCd, 0, false, false, true)
									);

									float currentStamina = victimData.getResources().getCurrentStamina();
									victimData.getResources().setCurrentStamina(currentStamina / 2);

									currentDamage[0] = Math.max(1.0, currentDamage[0] - defense);

									victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), MainSounds.UNBLOCK.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.9F + victim.getRandom().nextFloat() * 0.1F);

									if (victim.level() instanceof ServerLevel serverLevel) {
										Vec3 look = victim.getLookAngle();
										Vec3 spawnPos = victim.getEyePosition().add(look.scale(0.6)).subtract(0, 0.3, 0);
										serverLevel.sendParticles(MainParticles.GUARD_BLOCK.get(), spawnPos.x, spawnPos.y, spawnPos.z, 0, 1.0, 0.1, 0.1, 1.0);
									}
								} else {
									victimData.getResources().removePoise((int) poiseDamage);
									blocked = true;

									int regenCd = ConfigManager.getServerConfig().getCombat().getPoiseRegenCooldown();
									victimData.getCooldowns().setCooldown(Cooldowns.POISE_CD, regenCd);
									victim.addEffect(
											new MobEffectInstance(MainEffects.POISE_CD.get(), regenCd, 0, false, false, true)
									);

									float originalDmg = (float) currentDamage[0];
									float finalDmg;

									if (isParry) {
										finalDmg = 0;
										if (sourceEntity instanceof LivingEntity attackerLiving) {
											attackerLiving.knockback(1.5D, victim.getX() - attackerLiving.getX(), victim.getZ() - attackerLiving.getZ());
											attackerLiving.setDeltaMovement(attackerLiving.getDeltaMovement().scale(0.5));
											attackerLiving.addEffect(
													new MobEffectInstance(MainEffects.STAGGER.get(), 60, 1, false, false, true)
											);
										}
										victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), MainSounds.PARRY.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.9F + victim.getRandom().nextFloat() * 0.1F);

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
										double reductionCap = ConfigManager.getServerConfig().getCombat().getBlockDamageReductionCap();
										double reductionMin = ConfigManager.getServerConfig().getCombat().getBlockDamageReductionMin();
										double mitigationPct = (defense * 3.0) / (currentDamage[0] + (defense * 3.0));
										mitigationPct = Math.min(reductionCap, Math.max(mitigationPct, reductionMin));

										finalDmg = (float) (currentDamage[0] - defense - (currentDamage[0] * (1.0 - mitigationPct)));
										int randomSound = victim.getRandom().nextInt(3);
										SoundEvent soundToPlay;

										if (randomSound == 0) soundToPlay = MainSounds.BLOCK1.get();
										else if (randomSound == 1) soundToPlay = MainSounds.BLOCK2.get();
										else soundToPlay = MainSounds.BLOCK3.get();

										victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), soundToPlay, SoundSource.PLAYERS, 1.0F, 0.9F + victim.getRandom().nextFloat() * 0.1F);

										//EFECTOS
										if (victim.level() instanceof ServerLevel serverLevel) {
											double maxPoise = victimData.getMaxPoise();
											double currentPoiseVal = victimData.getResources().getCurrentPoise();
											double percentage = (currentPoiseVal / maxPoise) * 100.0;
											double r, g, b;

											if (percentage > 66) {
												r = 0.2;
												g = 0.9;
												b = 1.0;
											} else if (percentage > 33) {
												r = 1.0;
												g = 0.5;
												b = 0.0;
											} else {
												r = 1.0;
												g = 0.1;
												b = 0.1;
											}

											Vec3 look = victim.getLookAngle();
											Vec3 spawnPos = victim.getEyePosition().add(look.scale(0.6)).subtract(0, 0.3, 0);

											serverLevel.sendParticles(MainParticles.BLOCK_PARTICLE.get(), spawnPos.x, spawnPos.y, spawnPos.z, 0, r, g, b, 1.0);
										}

									}

									if (victim instanceof ServerPlayer sPlayer) {
										DMZEvent.PlayerBlockEvent blockEvent = new DMZEvent.PlayerBlockEvent(sPlayer, source.getEntity() instanceof LivingEntity ? (LivingEntity) source.getEntity() : null, originalDmg, finalDmg, isParry, poiseDamage);
										MinecraftForge.EVENT_BUS.post(blockEvent);

										if (!blockEvent.isCanceled()) currentDamage[0] = blockEvent.getFinalDamage();
										else {
											blocked = false;
											currentDamage[0] = Math.max(1.0, currentDamage[0] - defense);
										}
									} else currentDamage[0] = finalDmg;
								}
							}
						}
					}

					if (!blocked) {
						if (shouldApplyHitstun(source)) {
							int hitstunTicks = ConfigManager.getServerConfig().getCombat().getHitstunDurationTicks();
							if (hitstunTicks > 0) {
								victim.addEffect(new MobEffectInstance(MainEffects.STUN.get(), hitstunTicks, 0, false, false, true));
							}
						}

						if (!victimData.getStatus().isStunned() || victimData.getResources().getCurrentPoise() > 0) {
							currentDamage[0] = Math.max(1.0, currentDamage[0] - defense);
						} else {
							currentDamage[0] = Math.max(1.0, currentDamage[0] - (defense * ConfigManager.getServerConfig().getCombat().getEffectiveDefenseOnGuardBreak()));
						}
					}

					victim.getPersistentData().putDouble("dmz_exact_damage", currentDamage[0]);

					if (victim instanceof ServerPlayer serverPlayer) NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(serverPlayer), serverPlayer);
				}
			});
		}

		event.setAmount((float) currentDamage[0]);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void overrideVanillaArmorReduction(LivingDamageEvent event) {
		if (event.getEntity() instanceof Player victim) {
			if (victim.getPersistentData().contains("dmz_exact_damage")) {
				double exactDamage = victim.getPersistentData().getDouble("dmz_exact_damage");
				event.setAmount((float) exactDamage);
				victim.getPersistentData().remove("dmz_exact_damage");
			}
		}
	}

	@SubscribeEvent
	public static void onLivingTick(LivingEvent.LivingTickEvent event) {
		LivingEntity living = event.getEntity();
		if (living.level().isClientSide) return;

		CollisionImpactContext impact = COLLISION_IMPACTS.get(living.getUUID());
		if (impact == null) return;

		long now = System.currentTimeMillis();
		if (impact.expiryMs() < now) {
			COLLISION_IMPACTS.remove(living.getUUID());
			return;
		}

		boolean wallImpact = impact.type() == CollisionImpactType.WALL && living.horizontalCollision;
		boolean groundImpact = impact.type() == CollisionImpactType.GROUND
				&& living.onGround()
				&& (impact.startY() - living.getY() > 0.6 || living.fallDistance > 0.75F);

		if (!wallImpact && !groundImpact) return;

		COLLISION_IMPACTS.remove(living.getUUID());
		int impactStunTicks = Math.max(8, ConfigManager.getServerConfig().getCombat().getHitstunDurationTicks());
		living.addEffect(new MobEffectInstance(MainEffects.STUN.get(), impactStunTicks, 0, false, false, true));
		living.level().playSound(null, living.getX(), living.getY(), living.getZ(), MainSounds.PARRY.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

		if (living.level() instanceof ServerLevel serverLevel) {
			spawnRockImpactCircle(serverLevel, living.position(), impact.type() == CollisionImpactType.GROUND ? 2.75 : 1.9);
		}

		float impactDamage = Math.max(1.0F, impact.extraDamage());
		if (wallImpact) {
			living.hurt(living.damageSources().flyIntoWall(), impactDamage);
		} else {
			living.hurt(living.damageSources().fall(), impactDamage);
		}
	}

	private static boolean isHandEmptyOrNoDamageItem(Player player, boolean offhandAttack) {
		ItemStack hand = offhandAttack ? player.getOffhandItem() : player.getMainHandItem();
		if (hand.isEmpty()) return true;

		EquipmentSlot preferredSlot = offhandAttack ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
		double preferredDamage = getItemAttackDamage(hand, preferredSlot);
		if (preferredDamage > 0) return false;

		EquipmentSlot fallbackSlot = offhandAttack ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
		return getItemAttackDamage(hand, fallbackSlot) <= 0;
	}

	public static boolean canUseCustomCombat(Player attacker) {
		if (!isCombatStyleEnabled(attacker)) return false;

		ItemStack mainHand = attacker.getMainHandItem();
		ItemStack offHand = attacker.getOffhandItem();
		if (mainHand.isEmpty() && offHand.isEmpty()) return true;
		if (isTwoHandedWeapon(mainHand) || isTwoHandedWeapon(offHand)) return false;
		return isCombatWeapon(mainHand) || isCombatWeapon(offHand);
	}

	private static boolean isCombatWeapon(ItemStack stack) {
		if (stack.isEmpty()) return false;
		if (stack.getItem() instanceof SwordItem) return true;

		ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
		if (itemId == null) return false;

		String normalizedId = itemId.toString().toLowerCase(Locale.ROOT);
		String[] allowedItems = ConfigManager.getServerConfig().getCombat().getAllowedCombatItems();

		return Arrays.stream(allowedItems)
				.filter(entry -> entry != null && !entry.isBlank())
				.map(entry -> entry.toLowerCase(Locale.ROOT))
				.anyMatch(normalizedId::equals);
	}

	private static boolean isTwoHandedWeapon(ItemStack stack) {
		if (stack.isEmpty()) return false;

		ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
		if (itemId == null) return false;

		String normalizedId = itemId.toString().toLowerCase(Locale.ROOT);
		String normalizedPath = itemId.getPath().toLowerCase(Locale.ROOT);
		String[] configuredTwoHanded = ConfigManager.getServerConfig().getCombat().getTwoHandedCombatItems();

		boolean explicitMatch = Arrays.stream(configuredTwoHanded)
				.filter(entry -> entry != null && !entry.isBlank())
				.map(entry -> entry.toLowerCase(Locale.ROOT))
				.anyMatch(normalizedId::equals);
		if (explicitMatch) return true;

		return normalizedPath.contains("greatsword")
				|| normalizedPath.contains("two_handed")
				|| normalizedPath.contains("twohanded");
	}

	private static int getNextAttackIndex(Player attacker, DMZEvent.PlayerAttackStartEvent.AttackType attackType) {
		UUID id = attacker.getUUID();
		long now = System.currentTimeMillis();
		long last = ATTACK_CHAIN_LAST_MS.getOrDefault(id, 0L);
		long minWindowMs = 500L;
		long maxWindowMs = 1500L;
		long duplicateGuardMs = 40L;
		int comboCap = attackType == DMZEvent.PlayerAttackStartEvent.AttackType.HEAVY ? 4 : 8;

		int nextIndex;
		long delta = now - last;
		if (last != 0L && delta <= duplicateGuardMs && ATTACK_CHAIN_INDEX.containsKey(id)) {
			int duplicateIndex = ATTACK_CHAIN_INDEX.get(id);
			if (duplicateIndex < 1 || duplicateIndex > comboCap) duplicateIndex = 1;
			return duplicateIndex;
		}

		if (last == 0L || delta <= minWindowMs || delta >= maxWindowMs) {
			nextIndex = 1;
		} else {
			nextIndex = ATTACK_CHAIN_INDEX.getOrDefault(id, 0) + 1;
			if (nextIndex > comboCap) nextIndex = 1;
		}

		ATTACK_CHAIN_INDEX.put(id, nextIndex);
		ATTACK_CHAIN_LAST_MS.put(id, now);
		return nextIndex;
	}

	public static AttackExecutionContext prepareAttackContext(Player attacker, DMZEvent.PlayerAttackStartEvent.AttackType attackType, int estimatedHits) {
		return prepareAttackContext(attacker, new AttackIntentData(attackType, 0, 0L), estimatedHits);
	}

	public static AttackExecutionContext prepareAttackContext(Player attacker, AttackIntentData attackIntent, int estimatedHits) {
		DMZEvent.PlayerAttackStartEvent.AttackType attackType = attackIntent.attackType();
		int attackIndex = getNextAttackIndex(attacker, attackType);
		boolean offhandAttack = (attackIndex % 2 == 0);
		boolean finisher = attackType == DMZEvent.PlayerAttackStartEvent.AttackType.HEAVY ? attackIndex == 4 : attackIndex == 8;
		boolean mixedChain = isMixedCombo(attacker, attackType, attackIndex);
		int chargeTicks = Math.max(0, Math.min(attackIntent.chargeTicks(), MeleeAttackIntentC2S.MAX_TOTAL_HOLD_TICKS));
		boolean charged = chargeTicks >= MeleeAttackIntentC2S.MIN_EFFECTIVE_CHARGE_TICKS;
		long expiryMs = System.currentTimeMillis() + 200L;
		int hits = Math.max(1, estimatedHits);
		AttackExecutionContext ctx = new AttackExecutionContext(attackIndex, offhandAttack, finisher, mixedChain, attackType, chargeTicks, charged, expiryMs, hits);
		ATTACK_EXECUTION_CONTEXTS.put(attacker.getUUID(), ctx);
		return ctx;
	}

	private static AttackExecutionContext pollOrCreateAttackContext(Player attacker, AttackIntentData attackIntent) {
		UUID id = attacker.getUUID();
		AttackExecutionContext current = ATTACK_EXECUTION_CONTEXTS.get(id);
		long now = System.currentTimeMillis();
		long duplicateReuseWindowMs = 70L;

		if (current != null && current.expiryMs() >= now && current.remainingHits() > 0) {
			AttackExecutionContext next = new AttackExecutionContext(
					current.attackIndex(),
					current.offhandAttack(),
					current.finisher(),
					current.mixedChain(),
					current.attackType(),
					current.chargeTicks(),
					current.charged(),
					current.expiryMs(),
					current.remainingHits() - 1
			);
			if (next.remainingHits() <= 0) ATTACK_EXECUTION_CONTEXTS.remove(id);
			else ATTACK_EXECUTION_CONTEXTS.put(id, next);
			LAST_CONSUMED_CONTEXT.put(id, current);
			LAST_CONSUMED_CONTEXT_MS.put(id, now);
			return current;
		}

		if (attackIntent.expiryMs() <= 0L) {
			AttackExecutionContext cached = LAST_CONSUMED_CONTEXT.get(id);
			long lastConsumedAt = LAST_CONSUMED_CONTEXT_MS.getOrDefault(id, 0L);
			if (cached != null && (now - lastConsumedAt) <= duplicateReuseWindowMs) {
				return cached;
			}
		}

		return prepareAttackContext(attacker, attackIntent, 1);
	}

	private static void resetAttackChain(Player attacker) {
		UUID id = attacker.getUUID();
		ATTACK_CHAIN_INDEX.remove(id);
		ATTACK_CHAIN_LAST_MS.remove(id);
		ATTACK_CHAIN_LAST_TYPE.remove(id);
		ATTACK_CHAIN_MIXED.remove(id);
		ATTACK_INTENTS.remove(id);
		LAST_CONSUMED_CONTEXT.remove(id);
		LAST_CONSUMED_CONTEXT_MS.remove(id);
	}

	private static boolean isMixedCombo(Player attacker, DMZEvent.PlayerAttackStartEvent.AttackType currentType, int attackIndex) {
		UUID id = attacker.getUUID();
		if (attackIndex <= 1) {
			ATTACK_CHAIN_MIXED.put(id, false);
			ATTACK_CHAIN_LAST_TYPE.put(id, currentType);
			return false;
		}

		DMZEvent.PlayerAttackStartEvent.AttackType lastType = ATTACK_CHAIN_LAST_TYPE.get(id);
		boolean hadMixed = ATTACK_CHAIN_MIXED.getOrDefault(id, false);
		boolean mixedNow = hadMixed || (lastType != null && lastType != currentType);
		ATTACK_CHAIN_LAST_TYPE.put(id, currentType);
		ATTACK_CHAIN_MIXED.put(id, mixedNow);
		return mixedNow;
	}

	public static void registerAttackIntent(Player attacker, DMZEvent.PlayerAttackStartEvent.AttackType attackType, int chargeTicks) {
		if (attacker == null || attackType == null) return;
		if (!isCombatStyleEnabled(attacker)) return;
		long expiryMs = System.currentTimeMillis() + 4500L;
		int clampedChargeTicks = Math.max(0, Math.min(chargeTicks, MeleeAttackIntentC2S.MAX_TOTAL_HOLD_TICKS));
		ATTACK_INTENTS.put(attacker.getUUID(), new AttackIntentData(attackType, clampedChargeTicks, expiryMs));
	}

	public static void registerManualAttackIntent(Player attacker, DMZEvent.PlayerAttackStartEvent.AttackType attackType, int chargeTicks) {
		registerAttackIntent(attacker, attackType, chargeTicks);
		if (attacker == null) return;
		MANUAL_ATTACK_WINDOW_MS.put(attacker.getUUID(), System.currentTimeMillis() + 250L);
	}

	private static boolean consumeManualAttackWindow(Player attacker) {
		if (attacker == null) return false;
		UUID id = attacker.getUUID();
		long now = System.currentTimeMillis();
		long until = MANUAL_ATTACK_WINDOW_MS.getOrDefault(id, 0L);
		if (until < now) {
			MANUAL_ATTACK_WINDOW_MS.remove(id);
			return false;
		}
		MANUAL_ATTACK_WINDOW_MS.remove(id);
		return true;
	}

	public static void setCombatStyleEnabled(ServerPlayer player, boolean enabled) {
		if (player == null) return;
		UUID id = player.getUUID();
		DMZ_COMBAT_STYLE_ENABLED.put(id, enabled);
		if (!enabled) {
			ATTACK_INTENTS.remove(id);
			ATTACK_EXECUTION_CONTEXTS.remove(id);
			ATTACK_CHAIN_INDEX.remove(id);
			ATTACK_CHAIN_LAST_MS.remove(id);
			ATTACK_CHAIN_LAST_TYPE.remove(id);
			ATTACK_CHAIN_MIXED.remove(id);
			LAST_CONSUMED_CONTEXT.remove(id);
			LAST_CONSUMED_CONTEXT_MS.remove(id);
			MANUAL_ATTACK_WINDOW_MS.remove(id);
		}
	}

	private static boolean isCombatStyleEnabled(Player player) {
		if (player == null) return true;
		return DMZ_COMBAT_STYLE_ENABLED.getOrDefault(player.getUUID(), true);
	}

	public static DMZEvent.PlayerAttackStartEvent.AttackType resolveAttackType(Player attacker) {
		return resolveAttackIntent(attacker).attackType();
	}

	public static AttackIntentData resolveAttackIntent(Player attacker) {
		if (attacker == null) return new AttackIntentData(DMZEvent.PlayerAttackStartEvent.AttackType.LIGHT, 0, 0L);

		UUID id = attacker.getUUID();
		AttackIntentData intent = ATTACK_INTENTS.get(id);
		if (intent == null) return new AttackIntentData(DMZEvent.PlayerAttackStartEvent.AttackType.LIGHT, 0, 0L);

		if (intent.expiryMs() < System.currentTimeMillis()) {
			ATTACK_INTENTS.remove(id);
			return new AttackIntentData(DMZEvent.PlayerAttackStartEvent.AttackType.LIGHT, 0, 0L);
		}

		ATTACK_INTENTS.remove(id);
		return intent;
	}

	public static AttackIntentData peekAttackIntent(Player attacker) {
		if (attacker == null) return new AttackIntentData(DMZEvent.PlayerAttackStartEvent.AttackType.LIGHT, 0, 0L);

		UUID id = attacker.getUUID();
		AttackIntentData intent = ATTACK_INTENTS.get(id);
		if (intent == null) return new AttackIntentData(DMZEvent.PlayerAttackStartEvent.AttackType.LIGHT, 0, 0L);

		if (intent.expiryMs() < System.currentTimeMillis()) {
			ATTACK_INTENTS.remove(id);
			return new AttackIntentData(DMZEvent.PlayerAttackStartEvent.AttackType.LIGHT, 0, 0L);
		}

		return intent;
	}

	private static double getAttackTypeDamageScale(DMZEvent.PlayerAttackStartEvent.AttackType attackType, boolean finisher, boolean mixedChain) {
		double base = attackType == DMZEvent.PlayerAttackStartEvent.AttackType.HEAVY ? 1.15 : 1.0;
		if (!finisher) return base;
		double finisherScale = attackType == DMZEvent.PlayerAttackStartEvent.AttackType.HEAVY ? 1.18 : 1.12;
		if (mixedChain) finisherScale *= 0.9;
		return base * finisherScale;
	}

	private static double calculateChargedBonus(com.dragonminez.common.stats.StatsData attackerData, int chargeTicks) {
		if (chargeTicks < MeleeAttackIntentC2S.MIN_EFFECTIVE_CHARGE_TICKS) return 0.0;

		double meleeDamage = attackerData.getMeleeDamage();
		double strikeDamage = attackerData.getStrikeDamage();
		double meleeComponent = Math.max(0.0, meleeDamage - 1.0);
		double skpComponent = Math.max(0.0, strikeDamage - 1.0 - (meleeComponent * 0.25));

		int effectiveChargeTicks = Math.min(chargeTicks, MeleeAttackIntentC2S.MAX_EFFECTIVE_CHARGE_TICKS);
		double chargeProgress = (double) (effectiveChargeTicks - MeleeAttackIntentC2S.MIN_EFFECTIVE_CHARGE_TICKS)
				/ (MeleeAttackIntentC2S.MAX_EFFECTIVE_CHARGE_TICKS - MeleeAttackIntentC2S.MIN_EFFECTIVE_CHARGE_TICKS);
		chargeProgress = Math.max(0.0, Math.min(1.0, chargeProgress));
		double scaledProgress = 0.25 + (chargeProgress * 0.75);

		double chargedContribution = skpComponent * 0.25;
		return chargedContribution * scaledProgress;
	}

	private static DMZEvent.PlayerAttackStartEvent.AttackType readAttackTypeFromTag(Player victim) {
		String attackTypeRaw = victim.getPersistentData().getString(DMZ_ATTACK_TYPE_TAG);
		if (attackTypeRaw == null || attackTypeRaw.isBlank()) {
			return DMZEvent.PlayerAttackStartEvent.AttackType.LIGHT;
		}
		try {
			return DMZEvent.PlayerAttackStartEvent.AttackType.valueOf(attackTypeRaw);
		} catch (IllegalArgumentException ignored) {
			return DMZEvent.PlayerAttackStartEvent.AttackType.LIGHT;
		}
	}

	private static void applyControlEffects(Player attacker, LivingEntity victim, DMZEvent.PlayerAttackStartEvent.AttackType attackType, boolean finisherOrCharged) {
		if (!finisherOrCharged) return;

		float impactExtraDamage = (float) Math.max(1.0, attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.35);
		if (attacker.level() instanceof ServerLevel serverLevel) {
			SoundEvent critSound = attacker.getRandom().nextBoolean() ? MainSounds.CRITICO1.get() : MainSounds.CRITICO2.get();
			serverLevel.playSound(null, victim.getX(), victim.getY(), victim.getZ(), critSound, SoundSource.PLAYERS, 1.0F, 1.0F);
		}

		if (attackType == DMZEvent.PlayerAttackStartEvent.AttackType.HEAVY) {
			victim.setDeltaMovement(victim.getDeltaMovement().x * 0.2, -1.05, victim.getDeltaMovement().z * 0.2);
			victim.hurtMarked = true;
			registerCollisionImpact(victim, CollisionImpactType.GROUND, impactExtraDamage);
		} else {
			Vec3 pushDir = victim.position().subtract(attacker.position());
			if (pushDir.lengthSqr() < 1.0E-6) pushDir = attacker.getLookAngle();
			pushDir = new Vec3(pushDir.x, 0.0, pushDir.z).normalize();
			if (!Double.isFinite(pushDir.x) || !Double.isFinite(pushDir.z)) {
				pushDir = new Vec3(attacker.getLookAngle().x, 0.0, attacker.getLookAngle().z).normalize();
			}
			double strength = 1.95;
			Vec3 forcedVelocity = new Vec3(pushDir.x * strength, 0.26, pushDir.z * strength);
			victim.setDeltaMovement(forcedVelocity);
			victim.hurtMarked = true;
			registerCollisionImpact(victim, CollisionImpactType.WALL, impactExtraDamage);
			if (attacker.level() instanceof ServerLevel serverLevel) {
				spawnDustTrail(serverLevel, victim.position(), pushDir, 10);
			}
		}

		StatsProvider.get(StatsCapability.INSTANCE, attacker).ifPresent(data -> {
			if (data.getCooldowns().hasCooldown(Cooldowns.DASH_ACTIVE)) {
				ComboManager.enableTeleportWindow(attacker.getUUID(), victim.getId());
			}
		});
	}

	private static void registerCollisionImpact(LivingEntity victim, CollisionImpactType type, float extraDamage) {
		long expiryMs = System.currentTimeMillis() + 1200L;
		COLLISION_IMPACTS.put(victim.getUUID(), new CollisionImpactContext(type, expiryMs, victim.getY(), extraDamage));
	}

	private static void spawnDustTrail(ServerLevel level, Vec3 origin, Vec3 dir, int points) {
		Vec3 norm = dir.lengthSqr() > 1.0E-6 ? dir.normalize() : Vec3.ZERO;
		for (int i = 0; i < points; i++) {
			double t = i * 0.35;
			Vec3 pos = origin.subtract(norm.scale(t));
			level.sendParticles(MainParticles.DUST.get(), pos.x, pos.y + 0.05, pos.z, 2, 0.12, 0.05, 0.12, 0.01);
		}
	}

	private static void spawnRockImpactCircle(ServerLevel level, Vec3 center, double radius) {
		for (int i = 0; i < 20; i++) {
			double angle = (Math.PI * 2.0 * i) / 20.0;
			double x = center.x + Math.cos(angle) * radius;
			double z = center.z + Math.sin(angle) * radius;
			level.sendParticles(MainParticles.ROCK.get(), x, center.y + 0.05, z, 1, 0.08, 0.03, 0.08, 0.01);
		}
	}

	private static double getMcDamageForAttackHand(Player player, boolean offhandAttack, double fallbackMainhandDamage) {
		if (!offhandAttack) return fallbackMainhandDamage;

		ItemStack offhand = player.getOffhandItem();
		if (offhand.isEmpty()) return 0.0;

		double offhandDamage = getItemAttackDamage(offhand, EquipmentSlot.OFFHAND);
		if (offhandDamage > 0) return offhandDamage;

		return Math.max(0.0, getItemAttackDamage(offhand, EquipmentSlot.MAINHAND));
	}

	private static double getItemAttackDamage(ItemStack stack, EquipmentSlot slot) {
		double total = 0.0;
		for (AttributeModifier modifier : stack.getAttributeModifiers(slot).get(Attributes.ATTACK_DAMAGE)) {
			total += modifier.getAmount();
		}
		return total;
	}


	private static boolean shouldApplyHitstun(DamageSource source) {
		var combatCfg = ConfigManager.getServerConfig().getCombat();
		if (!combatCfg.getEnableHitstun()) return false;

		Entity sourceEntity = source.getEntity();
		if (sourceEntity == null) return false;

		String[] allowedSources = combatCfg.getHitstunSources();
		if (allowedSources.length == 0) return false;

		if (sourceEntity instanceof Player && matchesHitstunSource(allowedSources, "player")) {
			return true;
		}

		if (sourceEntity instanceof DBSagasEntity && matchesHitstunSource(allowedSources, "saga_npc")) {
			return true;
		}

		ResourceLocation typeKey = ForgeRegistries.ENTITY_TYPES.getKey(sourceEntity.getType());
		String entityTypeId = typeKey != null ? typeKey.toString() : "";

		return !entityTypeId.isEmpty() && matchesHitstunSource(allowedSources, entityTypeId);
	}

	private static boolean matchesHitstunSource(String[] allowedSources, String sourceKey) {
		String normalizedSource = sourceKey.toLowerCase(Locale.ROOT);
		return Arrays.stream(allowedSources)
				.filter(entry -> entry != null && !entry.isBlank())
				.map(entry -> entry.toLowerCase(Locale.ROOT))
				.anyMatch(normalizedSource::equals);
	}

	public static void handleDash(ServerPlayer player, float xInput, float zInput, boolean isDoubleDash) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;
			if (player.hasEffect(MainEffects.STUN.get())) return;

			if (ComboManager.canTeleport(player.getUUID())) {
				int targetId = ComboManager.getTeleportTarget(player.getUUID());
				Entity target = player.level().getEntity(targetId);

				if (target instanceof LivingEntity livingTarget) {
					Vec3 targetPos = livingTarget.position();
					Vec3 targetLook = livingTarget.getLookAngle();
					Vec3 teleportPos = targetPos.subtract(targetLook.scale(1.5));

					player.teleportTo(teleportPos.x, targetPos.y, teleportPos.z);
					player.setYRot(livingTarget.getYRot());
					player.setYHeadRot(livingTarget.getYRot());
					player.hurtMarked = true;
					player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.TP_SHORT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
					ComboManager.consumeTeleport(player.getUUID());
					return;
				}
			}

			long currentTime = System.currentTimeMillis();
			long lastHurtTime = data.getStatus().getLastHurtTime();
			int evasionWindow = ConfigManager.getServerConfig().getCombat().getPerfectEvasionWindowMs();
			boolean isEvasion = (currentTime - lastHurtTime) <= evasionWindow;
			boolean evasionActive = ConfigManager.getServerConfig().getCombat().getEnablePerfectEvasion();
			int recentAttackerId = player.getPersistentData().getInt(DMZ_LAST_ATTACKER_ID_TAG);
			LivingEntity recentAttacker = player.level().getEntity(recentAttackerId) instanceof LivingEntity living ? living : null;
			int vanishWindow = Math.max(50, evasionWindow / 2);
			boolean isVanish = recentAttacker != null && (currentTime - lastHurtTime) <= vanishWindow;

			if (isEvasion && evasionActive) {
				int maxEnergy = data.getMaxEnergy();
				int kiCost = (int) Math.ceil(maxEnergy * 0.08);

				DMZEvent.PlayerEvasionEvent evasionEvent = new DMZEvent.PlayerEvasionEvent(player, recentAttacker, 0, kiCost);
				MinecraftForge.EVENT_BUS.post(evasionEvent);

				if (evasionEvent.isCanceled()) return;

				kiCost = evasionEvent.getKiCost();
				float currentEnergy = data.getResources().getCurrentEnergy();

				if (currentEnergy >= kiCost) {
					data.getResources().addEnergy(-kiCost);
					data.getStatus().setLastHurtTime(0);

					if (recentAttacker != null) {
						if (isVanish) {
							Vec3 attackerLook = recentAttacker.getLookAngle();
							Vec3 behind = recentAttacker.position().subtract(attackerLook.scale(1.3));
							player.teleportTo(behind.x, recentAttacker.getY(), behind.z);
							player.setYRot(recentAttacker.getYRot());
							player.setYHeadRot(recentAttacker.getYRot());
							player.hurtMarked = true;

							recentAttacker.knockback(1.1D, recentAttacker.getX() - player.getX(), recentAttacker.getZ() - player.getZ());
							float vanishDamage = (float) Math.max(1.0, data.getMeleeDamage() * 0.2);
							recentAttacker.hurt(player.damageSources().playerAttack(player), vanishDamage);
						} else {
							recentAttacker.knockback(1.35D, recentAttacker.getX() - player.getX(), recentAttacker.getZ() - player.getZ());
							recentAttacker.setDeltaMovement(recentAttacker.getDeltaMovement().scale(0.6));
						}
					}

					player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
							isVanish ? MainSounds.TP_SHORT.get() : MainSounds.EVASION1.get(),
							SoundSource.PLAYERS,
							1.0F,
							isVanish ? 1.0F : 1.2F + player.getRandom().nextFloat() * 0.2F);
					NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(), TriggerAnimationS2C.AnimationType.EVASION, 0), player);
					NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(player), player);
					return;
				}
			}

			boolean canDoubleDash = isDoubleDash && data.getCooldowns().hasCooldown(Cooldowns.DASH_ACTIVE) && !data.getCooldowns().hasCooldown(Cooldowns.DOUBLEDASH_CD);
			boolean canNormalDash = !isDoubleDash && !data.getCooldowns().hasCooldown(Cooldowns.DASH_CD);

			if (!canDoubleDash && !canNormalDash) return;

			double baseDistance = 4.0;
			double speedMultiplier = player.getAttributeValue(Attributes.MOVEMENT_SPEED) / 0.1;
			double distance = baseDistance * speedMultiplier;

			int baseDrain = ConfigManager.getServerConfig().getCombat().getBaselineFormDrain();
			int kiCost;
			DMZEvent.PlayerDashEvent.DashType dashType;

			if (canDoubleDash) {
				distance = distance * 1.5;
				kiCost = (int) Math.ceil(baseDrain * 0.25);
				dashType = DMZEvent.PlayerDashEvent.DashType.DOUBLE;
			} else {
				kiCost = (int) Math.ceil(baseDrain * 0.12);
				dashType = DMZEvent.PlayerDashEvent.DashType.NORMAL;
			}

			DMZEvent.PlayerDashEvent dashEvent = new DMZEvent.PlayerDashEvent(player, dashType, distance, kiCost);
			MinecraftForge.EVENT_BUS.post(dashEvent);
			if (dashEvent.isCanceled()) return;
			distance = dashEvent.getDistance();
			kiCost = dashEvent.getKiCost();
			float currentEnergy = data.getResources().getCurrentEnergy();
			if (player.isCreative() || player.isSpectator()) kiCost = 0;
			if (currentEnergy < kiCost) return;
			if (player.getFoodData().getFoodLevel() <= 3) return;
			data.getResources().addEnergy(-kiCost);

			Vec3 forward = Vec3.directionFromRotation(0, player.getYRot()).normalize();
			Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
			Vec3 direction = forward.scale(zInput).add(right.scale(xInput)).normalize();

			double yVel = player.onGround() ? 0.35 : 0.2;

			Vec3 velocity = direction.scale(distance * 0.3);
			player.setDeltaMovement(player.getDeltaMovement().add(velocity.x, yVel, velocity.z));
			player.hurtMarked = true;

			if (player.level() instanceof ServerLevel serverLevel) {
				serverLevel.sendParticles(
						ParticleTypes.EXPLOSION,
						player.getX(), player.getY() + 0.5, player.getZ(),
						1,
						0.0,
						0.0,
						0.0,
						0.0
				);
			}

			int dashCdSeconds = ConfigManager.getServerConfig().getCombat().getDashCooldownSeconds();
			int doubleDashCdSeconds = ConfigManager.getServerConfig().getCombat().getDoubleDashCooldownSeconds();
			int dashCdTicks = dashCdSeconds * 20;
			int doubleDashCdTicks = doubleDashCdSeconds * 20;

			if (canDoubleDash) {
				data.getCooldowns().setCooldown(Cooldowns.DASH_CD, dashCdTicks);
				data.getCooldowns().setCooldown(Cooldowns.DOUBLEDASH_CD, doubleDashCdTicks);
				data.getCooldowns().removeCooldown(Cooldowns.DASH_ACTIVE);
				player.addEffect(new MobEffectInstance(MainEffects.DASH_CD.get(), dashCdTicks, 0, false, false, true));
				player.addEffect(new MobEffectInstance(MainEffects.DOUBLEDASH_CD.get(), doubleDashCdTicks, 0, false, false, true));
			} else {
				data.getCooldowns().setCooldown(Cooldowns.DASH_CD, dashCdTicks);
				data.getCooldowns().setCooldown(Cooldowns.DASH_ACTIVE, 15);
				player.addEffect(new MobEffectInstance(MainEffects.DASH_CD.get(), dashCdTicks, 0, false, false, true));
			}

			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5F, 1.5F + player.getRandom().nextFloat() * 0.3F);

			int dashDirection = getDashDirectionFromInput(xInput, zInput);
			if (canDoubleDash) dashDirection += 4;
			NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(), TriggerAnimationS2C.AnimationType.DASH, dashDirection, player.getId()), player);
			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
		});
	}

	private static int getDashDirectionFromInput(float xInput, float zInput) {
		if (zInput > 0 && xInput == 0) return 1;
		if (zInput < 0 && xInput == 0) return 2;
		if (xInput < 0 && zInput == 0) return 4;
		if (xInput > 0 && zInput == 0) return 3;
		if (zInput > 0) return 1;
		if (zInput < 0) return 2;
		return 1;
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		UUID id = event.getEntity().getUUID();
		ATTACK_CHAIN_INDEX.remove(id);
		ATTACK_CHAIN_LAST_MS.remove(id);
		ATTACK_CHAIN_LAST_TYPE.remove(id);
		ATTACK_CHAIN_MIXED.remove(id);
		ATTACK_EXECUTION_CONTEXTS.remove(id);
		ATTACK_INTENTS.remove(id);
		LAST_CONSUMED_CONTEXT.remove(id);
		LAST_CONSUMED_CONTEXT_MS.remove(id);
		MANUAL_ATTACK_WINDOW_MS.remove(id);
		COLLISION_IMPACTS.remove(id);
		DMZ_COMBAT_STYLE_ENABLED.remove(id);
	}
}
