package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.common.combat.util.Player_DMZ;
import com.dragonminez.common.combat.util.SoundHelper;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.PunchMachineEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CombatEvent {
	private static final Map<UUID, CollisionImpactContext> COLLISION_IMPACTS = new HashMap<>();
	private static final Map<String, Long> LAST_PLAYER_HIT_GUARD_MS = new HashMap<>();
	public static final String DMZ_LAST_ATTACKER_ID_TAG = "dmz_last_attacker_id";

	public enum CollisionImpactType {
		WALL,
		GROUND
	}

	public record CollisionImpactContext(
			CollisionImpactType type,
			long expiryMs,
			double startY,
			float extraDamage
	) {}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onLivingHurt(LivingHurtEvent event) {
		DamageSource source = event.getSource();
		final double[] currentDamage = {event.getAmount()};
		final boolean[] wasBlocked = {false};
		final boolean[] wasParry = {false};

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

				double baseDamage = currentDamage[0];
				double dmzDamage = attackerData.getMeleeDamage();

				var dmzPlayer = (Player_DMZ) attacker;
				var currentAttack = dmzPlayer.getCurrentAttack();

				if (currentAttack != null) {
					dmzDamage *= currentAttack.attack().damageMultiplier();
					if (currentAttack.isOffHand()) dmzDamage *= 0.5;
				}

				int baseStaminaRequired = (int) Math.ceil(dmzDamage * ConfigManager.getCombatConfig().getStaminaConsumptionRatio());
				double gravityMult = GravityLogic.getConsumptionMultiplier(attacker);
				int staminaRequired = (int) (baseStaminaRequired * gravityMult * attackerData.getAdjustedStaminaDrainMultiplier());

				float currentStamina = attackerData.getResources().getCurrentStamina();
				double finalDmzDamage;

				if (!attackerData.getStatus().isAlive() && attacker.level().dimension().equals(OtherworldDimension.OTHERWORLD_KEY)) {
					staminaRequired = 0;
				}

				boolean isFirstHit = attacker.getPersistentData().getBoolean("dmz_first_hit");

				if (currentStamina >= staminaRequired) {
					if (!attacker.isCreative() && isFirstHit) attackerData.getResources().removeStamina(staminaRequired);
					finalDmzDamage = dmzDamage;
				} else {
					double staminaRatio = (double) currentStamina / staminaRequired;
					finalDmzDamage = dmzDamage * staminaRatio;
					if (!attacker.isCreative()) attackerData.getResources().setCurrentStamina(0);
				}

				if (isEmptyHandOrNoDamageItem(attacker)) {
					currentDamage[0] = finalDmzDamage;
				} else {
					currentDamage[0] = baseDamage + finalDmzDamage;
				}

				boolean kiWeaponActive = attackerData.getSkills().isSkillActive("kimanipulation");
				if (kiWeaponActive) {
					String weaponType = attackerData.getStatus().getKiWeaponType();
					int kiCost = 0;
					switch (weaponType.toLowerCase()) {
						case "blade" -> {
							kiCost = (int) Math.round(ConfigManager.getCombatConfig().getBaselineFormDrain() * ConfigManager.getCombatConfig().getKiBladeConfig()[1]);
							if (attackerData.getResources().getCurrentEnergy() >= kiCost) {
								currentDamage[0] = currentDamage[0] + attackerData.getKiDamage() * ConfigManager.getCombatConfig().getKiBladeConfig()[0];
							}
						}
						case "scythe" -> {
							kiCost = (int) Math.round(ConfigManager.getCombatConfig().getBaselineFormDrain() * ConfigManager.getCombatConfig().getKiScytheConfig()[1]);
							if (attackerData.getResources().getCurrentEnergy() >= kiCost) {
								currentDamage[0] = currentDamage[0] + attackerData.getKiDamage() * ConfigManager.getCombatConfig().getKiScytheConfig()[0];
							}
						}
						case "clawlance" -> {
							kiCost = (int) Math.round(ConfigManager.getCombatConfig().getBaselineFormDrain() * ConfigManager.getCombatConfig().getKiClawLanceConfig()[1]);
							if (attackerData.getResources().getCurrentEnergy() >= kiCost) {
								currentDamage[0] = currentDamage[0] + attackerData.getKiDamage() * ConfigManager.getCombatConfig().getKiClawLanceConfig()[0];
							}
						}
					}
					if (!attacker.isCreative() && !isPunchMachine && isFirstHit) attackerData.getResources().removeEnergy(kiCost);
				}

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

		// Victim Defense Event
		if (event.getEntity() instanceof Player victim) {
			StatsProvider.get(StatsCapability.INSTANCE, victim).ifPresent(victimData -> {
				if (victimData.getStatus().isHasCreatedCharacter()) {
					victimData.getStatus().setLastHurtTime(System.currentTimeMillis());

					if (source.getEntity() instanceof LivingEntity sourceLiving) {
						victim.getPersistentData().putInt(DMZ_LAST_ATTACKER_ID_TAG, sourceLiving.getId());
					}

					boolean isPvP = source.getEntity() instanceof Player;
					if (ConfigManager.getCombatConfig().getKillPlayersOnCombatLogout() && isPvP) {
						victimData.getCooldowns().addCooldown(Cooldowns.COMBAT, 120);
					}

					double defense = victimData.getDefense();
					boolean blocked = false;

					if (ConfigManager.getCombatConfig().getEnableBlocking()) {
						Entity sourceEntity = source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();
						if (victimData.getStatus().isBlocking() && !victimData.getStatus().isStunned() && sourceEntity != null) {
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

								if (currentPoise - poiseDamage <= 0) {
									victimData.getResources().setCurrentPoise(0);
									victimData.getStatus().setBlocking(false);

									int stunDuration = ConfigManager.getCombatConfig().getBlockBreakStunDurationTicks();
									victim.addEffect(new MobEffectInstance(MainEffects.STUN.get(), stunDuration, 0, false, false, true));
									int regenCd = ConfigManager.getCombatConfig().getPoiseRegenCooldown();
									victimData.getCooldowns().setCooldown(Cooldowns.POISE_CD, regenCd);
									victim.addEffect(new MobEffectInstance(MainEffects.POISE_CD.get(), regenCd, 0, false, false, true));

									float currentStamina = victimData.getResources().getCurrentStamina();
									victimData.getResources().setCurrentStamina(currentStamina / 2);

									currentDamage[0] = Math.max(1.0, currentDamage[0] - defense);

									victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), MainSounds.UNBLOCK.get(), SoundSource.PLAYERS, 1.0F, 0.9F + victim.getRandom().nextFloat() * 0.1F);

									if (victim.level() instanceof ServerLevel serverLevel) {
										Vec3 look = victim.getLookAngle();
										Vec3 spawnPos = victim.getEyePosition().add(look.scale(0.6)).subtract(0, 0.3, 0);
										serverLevel.sendParticles(MainParticles.GUARD_BLOCK.get(), spawnPos.x, spawnPos.y, spawnPos.z, 0, 1.0, 0.1, 0.1, 1.0);
									}
								} else {
									victimData.getResources().removePoise((int) poiseDamage);
									blocked = true;

									int regenCd = ConfigManager.getCombatConfig().getPoiseRegenCooldown();
									victimData.getCooldowns().setCooldown(Cooldowns.POISE_CD, regenCd);
									victim.addEffect(new MobEffectInstance(MainEffects.POISE_CD.get(), regenCd, 0, false, false, true));

									float originalDmg = (float) currentDamage[0];
									float finalDmg;

									if (isParry) {
										wasParry[0] = true;
										finalDmg = 0;
										if (sourceEntity instanceof LivingEntity attackerLiving) {
											attackerLiving.knockback(1.5D, victim.getX() - attackerLiving.getX(), victim.getZ() - attackerLiving.getZ());
											attackerLiving.setDeltaMovement(attackerLiving.getDeltaMovement().scale(0.5));
											attackerLiving.addEffect(new MobEffectInstance(MainEffects.STAGGER.get(), 60, 1, false, false, true));
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
													double reductionCap = ConfigManager.getCombatConfig().getBlockDamageReductionCap();
													double reductionMin = ConfigManager.getCombatConfig().getBlockDamageReductionMin();
										double mitigationPct = (defense * 3.0) / (currentDamage[0] + (defense * 3.0));
										mitigationPct = Math.min(reductionCap, Math.max(mitigationPct, reductionMin));

										finalDmg = (float) (currentDamage[0] - defense - (currentDamage[0] * (1.0 - mitigationPct)));
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
										DMZEvent.PlayerBlockEvent blockEvent = new DMZEvent.PlayerBlockEvent(sPlayer, source.getEntity() instanceof LivingEntity ? (LivingEntity) source.getEntity() : null, originalDmg, finalDmg, isParry, poiseDamage);
										MinecraftForge.EVENT_BUS.post(blockEvent);

										if (!blockEvent.isCanceled()) {
											currentDamage[0] = blockEvent.getFinalDamage();
										} else {
											blocked = false;
											wasParry[0] = false;
											currentDamage[0] = Math.max(1.0, currentDamage[0] - defense);
										}
									} else {
										currentDamage[0] = finalDmg;
									}
								}
							}
						}
					}

					if (!blocked) {
						if (!victimData.getStatus().isStunned() || victimData.getResources().getCurrentPoise() > 0) {
							currentDamage[0] = Math.max(1.0, currentDamage[0] - defense);
						} else {
							currentDamage[0] = Math.max(1.0, currentDamage[0] - (defense * ConfigManager.getCombatConfig().getEffectiveDefenseOnGuardBreak()));
						}
					} else {
						wasBlocked[0] = true;
					}

					victim.getPersistentData().putDouble("dmz_exact_damage", currentDamage[0]);

					if (victim instanceof ServerPlayer serverPlayer) {
						NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(serverPlayer), serverPlayer);
					}
				}
			});
		}

		event.setAmount((float) currentDamage[0]);

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

	private static boolean isEmptyHandOrNoDamageItem(Player player) {
		ItemStack mainHand = player.getMainHandItem();
		if (mainHand.isEmpty()) return true;
		var attackDamageModifier = mainHand.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE);
		return attackDamageModifier.isEmpty();
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
		boolean groundImpact = impact.type() == CollisionImpactType.GROUND && living.onGround() && (impact.startY() - living.getY() > 0.6 || living.fallDistance > 0.75F);

		if (!wallImpact && !groundImpact) return;

		COLLISION_IMPACTS.remove(living.getUUID());
		living.addEffect(new MobEffectInstance(MainEffects.STUN.get(), 30, 0, false, false, true));
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

	// Utilidad a conectar al registro de ataques finalizados de BetterCombat
	public static void triggerFinisherImpact(Player attacker, LivingEntity victim, boolean isHeavy) {
		float impactExtraDamage = (float) Math.max(1.0, attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.35);
		if (attacker.level() instanceof ServerLevel serverLevel) {
			SoundEvent critSound = attacker.getRandom().nextBoolean() ? MainSounds.CRITICO1.get() : MainSounds.CRITICO2.get();
			serverLevel.playSound(null, victim.getX(), victim.getY(), victim.getZ(), critSound, SoundSource.PLAYERS, 1.0F, 1.0F);
		}

		if (isHeavy) {
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
			int evasionWindow = ConfigManager.getCombatConfig().getPerfectEvasionWindowMs();
			boolean isEvasion = (currentTime - lastHurtTime) <= evasionWindow;
			boolean evasionActive = ConfigManager.getCombatConfig().getEnablePerfectEvasion();
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
							SoundSource.PLAYERS, 1.0F, isVanish ? 1.0F : 1.2F + player.getRandom().nextFloat() * 0.2F);
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

			int baseDrain = ConfigManager.getCombatConfig().getBaselineFormDrain();
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
				serverLevel.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.5, player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
			}

			int dashCdSeconds = ConfigManager.getCombatConfig().getDashCooldownSeconds();
			int doubleDashCdSeconds = ConfigManager.getCombatConfig().getDoubleDashCooldownSeconds();
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
		COLLISION_IMPACTS.remove(id);
	}
}

