package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.PunchMachineEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.Cooldowns;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.ComboManager;
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
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.atomic.AtomicBoolean;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CombatEvent {

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onLivingHurt(LivingHurtEvent event) {
		DamageSource source = event.getSource();
		final double[] currentDamage = {event.getAmount()};

		// Attacker Damage Event
		if (source.getEntity() instanceof Player attacker && source.getMsgId().equals("player")) {
			if (attacker.hasEffect(MainEffects.STUN.get()) || attacker.isBlocking()) {
				event.setCanceled(true);
				return;
			}

			boolean isPunchMachine = event.getEntity() instanceof PunchMachineEntity;

			StatsProvider.get(StatsCapability.INSTANCE, attacker).ifPresent(attackerData -> {
				if (!attackerData.getStatus().hasCreatedCharacter()) return;

				double mcBaseDamage = currentDamage[0];
				double dmzDamage = attackerData.getMeleeDamage();
				boolean wantsCombo = ComboManager.isNextHitCombo(attacker.getUUID());
				ComboManager.setNextHitAsCombo(attacker.getUUID(), false);
				boolean isCooldownFull = false;

				if (ConfigManager.getServerConfig().getCombat().isRespectAttackCooldown()) {
					float adjustedStrength = attacker.getAttackStrengthScale(0.5F);

					if (attackerData.getCharacter().hasActiveForm()) {
						FormConfig.FormData activeForm = attackerData.getCharacter().getActiveFormData();
						if (activeForm != null) {
							adjustedStrength *= (float) activeForm.getAttackSpeed();
						}
					}

					if (adjustedStrength > 1.0F) adjustedStrength = 1.0F;
					isCooldownFull = adjustedStrength >= 0.9F;

					float damageScale = 0.2F + adjustedStrength * adjustedStrength * 0.8F;
					dmzDamage *= damageScale;
				} else {
					isCooldownFull = true;
				}

				int baseStaminaRequired = (int) Math.ceil(dmzDamage * ConfigManager.getServerConfig().getCombat().getStaminaConsumptionRatio());
				double staminaDrainMultiplier = attackerData.getAdjustedStaminaDrain();
				int staminaRequired = (int) Math.ceil(baseStaminaRequired * staminaDrainMultiplier);
				int currentStamina = attackerData.getResources().getCurrentStamina();
				if (isPunchMachine) staminaRequired = 0;

				if (wantsCombo) {
					if (isCooldownFull) {
						int currentCombo = ComboManager.getCombo(attacker.getUUID());
						Entity target = event.getEntity();
						if (currentCombo > 0 && !ComboManager.shouldContinueCombo(attacker.getUUID(), target)) currentCombo = 0;


						int nextCombo = (currentCombo % 4) + 1;
						ComboManager.setCombo(attacker.getUUID(), nextCombo);
						ComboManager.registerHit(attacker.getUUID(), target);

						double dmgBonus = 1.0 + (0.03 * nextCombo);
						dmzDamage *= dmgBonus;
						staminaRequired = (int) (staminaRequired * 1.25);

						if (attacker instanceof ServerPlayer serverPlayer) {
							NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(serverPlayer.getUUID(), "combo", nextCombo), serverPlayer);
						}

						attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), MainSounds.CRITICO1.get(), SoundSource.PLAYERS, 0.5F, 1.0F + (nextCombo * 0.1F));

						if (nextCombo == 4) {
							Entity victim = event.getEntity();
							Vec3 look = attacker.getLookAngle();
							victim.setDeltaMovement(look.x * 3.0, 0.5, look.z * 3.0);
							victim.hurtMarked = true;
							ComboManager.enableTeleportWindow(attacker.getUUID(), victim.getId());
							attacker.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), MainSounds.CRITICO2.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
							ComboManager.resetCombo(attacker.getUUID());
							attackerData.getCooldowns().addCooldown(Cooldowns.COMBO_ATTACK_CD, ConfigManager.getServerConfig().getCombat().getComboAttacksCooldownSeconds() * 20);
						}
					} else {
						ComboManager.resetCombo(attacker.getUUID());
						attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.5F);
						attackerData.getCooldowns().addCooldown(Cooldowns.COMBO_ATTACK_CD, ConfigManager.getServerConfig().getCombat().getComboAttacksCooldownSeconds() * 20);
					}
				} else {
					ComboManager.resetCombo(attacker.getUUID());
					attackerData.getCooldowns().addCooldown(Cooldowns.COMBO_ATTACK_CD, ConfigManager.getServerConfig().getCombat().getComboAttacksCooldownSeconds() * 20);
				}

				double finalDmzDamage;
				if (currentStamina >= staminaRequired) {
					finalDmzDamage = dmzDamage;
					attackerData.getResources().removeStamina(staminaRequired);
				} else {
					double staminaRatio = (double) currentStamina / staminaRequired;
					finalDmzDamage = dmzDamage * staminaRatio;
					attackerData.getResources().setCurrentStamina(0);
				}

				if (attackerData.getCharacter().hasActiveForm()) {
					FormConfig.FormData activeForm = attackerData.getCharacter().getActiveFormData();
					if (activeForm != null) {
						String formGroup = attackerData.getCharacter().getActiveFormGroup();
						String formName = attackerData.getCharacter().getActiveForm();
						attackerData.getCharacter().getFormMasteries().addMastery(formGroup, formName, activeForm.getMasteryPerHit(), activeForm.getMaxMastery());
					}
				}

				if (isEmptyHandOrNoDamageItem(attacker)) {
					currentDamage[0] = finalDmzDamage;
				} else {
					currentDamage[0] = mcBaseDamage + finalDmzDamage;
				}

				boolean kiWeaponActive = attackerData.getSkills().isSkillActive("kimanipulation");

				if (kiWeaponActive) {
					String weaponType = attackerData.getStatus().getKiWeaponType();
					int kiCost = 0;
					switch (weaponType.toLowerCase()) {
						case "blade" -> {
							kiCost = (int) Math.round(attackerData.getMaxEnergy() * ConfigManager.getServerConfig().getCombat().getKiBladeConfig()[1]);
							if (attackerData.getResources().getCurrentEnergy() >= kiCost) {
								currentDamage[0] = currentDamage[0] + attackerData.getKiDamage() * ConfigManager.getServerConfig().getCombat().getKiBladeConfig()[0];
							}
						}
						case "scythe" -> {
							kiCost = (int) Math.round(attackerData.getMaxEnergy() * ConfigManager.getServerConfig().getCombat().getKiScytheConfig()[1]);
							if (attackerData.getResources().getCurrentEnergy() >= kiCost) {
								currentDamage[0] = currentDamage[0] + attackerData.getKiDamage() * ConfigManager.getServerConfig().getCombat().getKiScytheConfig()[0];
							}
						}
						case "clawlance" -> {
							kiCost = (int) Math.round(attackerData.getMaxEnergy() * ConfigManager.getServerConfig().getCombat().getKiClawLanceConfig()[1]);
							if (attackerData.getResources().getCurrentEnergy() >= kiCost) {
								currentDamage[0] = currentDamage[0] + attackerData.getKiDamage() * ConfigManager.getServerConfig().getCombat().getKiClawLanceConfig()[0];
							}
						}
					}
					if (isPunchMachine) kiCost = 0;
					attackerData.getResources().removeEnergy(kiCost);
				}

				if (attacker instanceof ServerPlayer serverPlayer)
					NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(serverPlayer), serverPlayer);

				if (event.getEntity() instanceof Player) {
					if (ConfigManager.getServerConfig().getCombat().isKillPlayersOnCombatLogout())
						attackerData.getCooldowns().addCooldown(Cooldowns.COMBAT, 200);
				}

				if (event.getEntity() instanceof PunchMachineEntity punchMachineEntity) {
					punchMachineEntity.processHit((float) currentDamage[0], attacker);
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
				if (victimData.getStatus().hasCreatedCharacter()) {
					victimData.getStatus().setLastHurtTime(System.currentTimeMillis());
					if (ConfigManager.getServerConfig().getCombat().isKillPlayersOnCombatLogout())
						victimData.getCooldowns().addCooldown(Cooldowns.COMBAT, 200);
					double defense = victimData.getDefense();
					boolean blocked = false;

					if (ConfigManager.getServerConfig().getCombat().isEnableBlocking()) {
						if (victimData.getStatus().isBlocking() && !victimData.getStatus().isStunned() && source.getEntity() != null) {
							Vec3 targetLook = victim.getLookAngle();
							Vec3 sourceLoc = source.getEntity().position();
							Vec3 targetLoc = victim.position();
							Vec3 directionToSource = sourceLoc.subtract(targetLoc).normalize();

							if (targetLook.dot(directionToSource) > 0.0) {
								long currentTime = System.currentTimeMillis();
								long blockTime = victimData.getStatus().getLastBlockTime();
								int parryWindow = ConfigManager.getServerConfig().getCombat().getParryWindowMs();
								boolean isParry = ((currentTime - blockTime) <= parryWindow) && ConfigManager.getServerConfig().getCombat().isEnableParrying();

								double poiseMultiplier = ConfigManager.getServerConfig().getCombat().getPoiseDamageMultiplier();
								if (!(source.getEntity() instanceof Player)) {
									poiseMultiplier *= 5.0;
								}
								float poiseDamage = (float) (currentDamage[0] * poiseMultiplier);

								if (isParry) poiseDamage *= 0.75f;
								int currentPoise = victimData.getResources().getCurrentPoise();
								//System.out.println("Poise actual: " + currentPoise + ", Daño de poise: " + poiseDamage);

								if (currentPoise - poiseDamage <= 0) {
									victimData.getResources().setCurrentPoise(0);
									victimData.getStatus().setBlocking(false);

									int stunDuration = ConfigManager.getServerConfig().getCombat().getBlockBreakStunDurationTicks();
									victim.addEffect(new MobEffectInstance(MainEffects.STUN.get(), stunDuration, 0, false, false, true));
									int regenCd = ConfigManager.getServerConfig().getCombat().getPoiseRegenCooldown();
									victimData.getCooldowns().setCooldown(Cooldowns.POISE_CD, regenCd);

									int currentStamina = victimData.getResources().getCurrentStamina();
									victimData.getResources().setCurrentStamina(currentStamina / 2);

									currentDamage[0] = Math.max(1.0, currentDamage[0] - defense);

									// Acá pondríamos sonido de Rotura de Guardia
									victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
											MainSounds.UNBLOCK.get(),
											net.minecraft.sounds.SoundSource.PLAYERS,
											1.0F,
											0.9F + victim.getRandom().nextFloat() * 0.1F);

									if (victim.level() instanceof ServerLevel serverLevel) {
										Vec3 look = victim.getLookAngle();
										Vec3 spawnPos = victim.getEyePosition().add(look.scale(0.6)).subtract(0, 0.3, 0);

										serverLevel.sendParticles(
												MainParticles.GUARD_BLOCK.get(),
												spawnPos.x, spawnPos.y, spawnPos.z,
												0,
												1.0, 0.1, 0.1,
												1.0
										);
									}

								} else {
									victimData.getResources().removePoise((int) poiseDamage);
									blocked = true;

									int regenCd = ConfigManager.getServerConfig().getCombat().getPoiseRegenCooldown();
									victimData.getCooldowns().setCooldown(Cooldowns.POISE_CD, regenCd);

									float originalDmg = (float) currentDamage[0];
									float finalDmg;

									if (isParry) {
										finalDmg = 0;
										if (source.getEntity() instanceof LivingEntity attackerLiving) {
											attackerLiving.knockback(1.5D, victim.getX() - attackerLiving.getX(), victim.getZ() - attackerLiving.getZ());
											attackerLiving.setDeltaMovement(attackerLiving.getDeltaMovement().scale(0.5));

											// Efecto de Parry (temblor de pantalla pequeño) al atacante
											attackerLiving.addEffect(new MobEffectInstance(MainEffects.STAGGER.get(), 60, 50, false, false, true));
										}
										//System.out.println("Parry!");
										//SONIDO PARRY
										victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
												MainSounds.PARRY.get(),
												net.minecraft.sounds.SoundSource.PLAYERS,
												1.0F,
												0.9F + victim.getRandom().nextFloat() * 0.1F);

										if (victim.level() instanceof ServerLevel serverLevel) {
											Vec3 look = victim.getLookAngle();
											Vec3 spawnPos = victim.getEyePosition().add(look.scale(0.6)).subtract(0, 0.3, 0);

											serverLevel.sendParticles(
													MainParticles.GUARD_BLOCK.get(),
													spawnPos.x, spawnPos.y, spawnPos.z,
													0,
													1.0, 1.0, 1.0,
													1.0
											);
											for (int i = 0; i < 15; i++) {
												serverLevel.sendParticles(
														MainParticles.KI_TRAIL.get(),
														spawnPos.x, spawnPos.y, spawnPos.z,
														0,
														1.0, 1.0, 1.0,
														1.0
												);
												serverLevel.sendParticles(
														MainParticles.SPARKS.get(),
														spawnPos.x, spawnPos.y, spawnPos.z,
														0,
														1.0, 1.0, 1.0,
														1.0
												);
											}
										}

									} else {
										double reductionCap = ConfigManager.getServerConfig().getCombat().getBlockDamageReductionCap();
										double reductionMin = ConfigManager.getServerConfig().getCombat().getBlockDamageReductionMin();
										double mitigationPct = (defense * 3.0) / (currentDamage[0] + (defense * 3.0));
										mitigationPct = Math.min(reductionCap, Math.max(mitigationPct, reductionMin));

										finalDmg = (float) (currentDamage[0] * (1.0 - mitigationPct));
										int randomSound = victim.getRandom().nextInt(3);
										SoundEvent soundToPlay;

										if (randomSound == 0) {
											soundToPlay = MainSounds.BLOCK1.get();
										} else if (randomSound == 1) {
											soundToPlay = MainSounds.BLOCK2.get();
										} else {
											soundToPlay = MainSounds.BLOCK3.get();
										}

//                                        System.out.println("Bloqueo! Daño antes: " + originalDmg + ", después: " + finalDmg);

										victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
												soundToPlay,
												net.minecraft.sounds.SoundSource.PLAYERS,
												1.0F,
												0.9F + victim.getRandom().nextFloat() * 0.1F);

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

											serverLevel.sendParticles(
													MainParticles.BLOCK_PARTICLE.get(),
													spawnPos.x, spawnPos.y, spawnPos.z,
													0,
													r, g, b,
													1.0
											);
										}

									}

									if (victim instanceof ServerPlayer sPlayer) {
										DMZEvent.PlayerBlockEvent blockEvent = new DMZEvent.PlayerBlockEvent(
												sPlayer,
												source.getEntity() instanceof LivingEntity ? (LivingEntity) source.getEntity() : null,
												originalDmg,
												finalDmg,
												isParry,
												poiseDamage
										);
										MinecraftForge.EVENT_BUS.post(blockEvent);

										if (!blockEvent.isCanceled()) {
											currentDamage[0] = blockEvent.getFinalDamage();
										} else {
											blocked = false;
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
							if (!victimData.getStatus().isBlocking()) {
								currentDamage[0] = Math.max(1.0, currentDamage[0] - defense);
							}
						}
					}

					if (victimData.getCharacter().hasActiveForm()) {
						FormConfig.FormData activeForm = victimData.getCharacter().getActiveFormData();
						if (activeForm != null) {
							String formGroup = victimData.getCharacter().getActiveFormGroup();
							String formName = victimData.getCharacter().getActiveForm();
							victimData.getCharacter().getFormMasteries().addMastery(
									formGroup,
									formName,
									activeForm.getMasteryPerDamageReceived(),
									activeForm.getMaxMastery()
							);

							if (victim instanceof ServerPlayer serverPlayer) {
								NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(serverPlayer), serverPlayer);
							}
						}
					}
				}
			});
		}

		event.setAmount((float) currentDamage[0]);
	}

	private static boolean isEmptyHandOrNoDamageItem(Player player) {
		ItemStack mainHand = player.getMainHandItem();
		if (mainHand.isEmpty()) return true;
		var attackDamageModifier = mainHand.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE);
		return attackDamageModifier.isEmpty();
	}

	public static void handleDash(ServerPlayer player, float xInput, float zInput, boolean isDoubleDash) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!data.getStatus().hasCreatedCharacter()) return;
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
			boolean evasionActive = ConfigManager.getServerConfig().getCombat().isEnablePerfectEvasion();

			if (isEvasion && evasionActive) {
				int maxEnergy = data.getMaxEnergy();
				int kiCost = (int) Math.ceil(maxEnergy * 0.08);

				DMZEvent.PlayerEvasionEvent evasionEvent = new DMZEvent.PlayerEvasionEvent(player, null, 0, kiCost);
				MinecraftForge.EVENT_BUS.post(evasionEvent);

				if (evasionEvent.isCanceled()) return;

				kiCost = evasionEvent.getKiCost();
				int currentEnergy = data.getResources().getCurrentEnergy();

				if (currentEnergy >= kiCost) {
					data.getResources().addEnergy(-kiCost);
					data.getStatus().setLastHurtTime(0);

					player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
							MainSounds.PARRY.get(),
							SoundSource.PLAYERS,
							1.0F,
							1.2F + player.getRandom().nextFloat() * 0.2F);

					if (player.level() instanceof ServerLevel serverLevel) {
						Vec3 spawnPos = player.getEyePosition().subtract(0, 0.3, 0);
						for (int i = 0; i < 20; i++) {
							serverLevel.sendParticles(
									MainParticles.KI_TRAIL.get(),
									spawnPos.x, spawnPos.y, spawnPos.z,
									0,
									1.0, 1.0, 1.0,
									1.0
							);
						}
					}

					NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(), "evasion", 0), player);
					NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
					return;
				}
			}

			boolean canDoubleDash = isDoubleDash && data.getCooldowns().hasCooldown(Cooldowns.DASH_ACTIVE) && !data.getCooldowns().hasCooldown(Cooldowns.DOUBLEDASH_CD);
			boolean canNormalDash = !isDoubleDash && !data.getCooldowns().hasCooldown(Cooldowns.DASH_CD);

			if (!canDoubleDash && !canNormalDash) return;

			double baseDistance = 4.0;
			double speedMultiplier = player.getAttributeValue(Attributes.MOVEMENT_SPEED) / 0.1;
			double distance = baseDistance * speedMultiplier;

			int maxEnergy = data.getMaxEnergy();
			int kiCost;
			DMZEvent.PlayerDashEvent.DashType dashType;

			if (canDoubleDash) {
				distance = distance * 2;
				kiCost = (int) Math.ceil(maxEnergy * 0.12);
				dashType = DMZEvent.PlayerDashEvent.DashType.DOUBLE;
			} else {
				kiCost = (int) Math.ceil(maxEnergy * 0.05);
				dashType = DMZEvent.PlayerDashEvent.DashType.NORMAL;
			}

			DMZEvent.PlayerDashEvent dashEvent = new DMZEvent.PlayerDashEvent(player, dashType, distance, kiCost);
			MinecraftForge.EVENT_BUS.post(dashEvent);
			if (dashEvent.isCanceled()) return;
			distance = dashEvent.getDistance();
			kiCost = dashEvent.getKiCost();
			int currentEnergy = data.getResources().getCurrentEnergy();
			if (currentEnergy < kiCost) return;
			data.getResources().addEnergy(-kiCost);

			Vec3 forward = Vec3.directionFromRotation(0, player.getYRot()).normalize();
			Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
			Vec3 direction = forward.scale(zInput).add(right.scale(xInput)).normalize();

			double yVel = player.onGround() ? 0.35 : 0.2;

			Vec3 velocity = direction.scale(distance * 0.3);
			player.setDeltaMovement(player.getDeltaMovement().add(velocity.x, yVel, velocity.z));
			player.hurtMarked = true;

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

			if (player.level() instanceof ServerLevel serverLevel) {
				Vec3 spawnPos = player.position().add(0, 0.5, 0);
				for (int i = 0; i < (canDoubleDash ? 15 : 8); i++) {
					serverLevel.sendParticles(
							MainParticles.KI_TRAIL.get(),
							spawnPos.x, spawnPos.y, spawnPos.z,
							0,
							direction.x * 0.5, 0.1, direction.z * 0.5,
							0.3
					);
				}
			}

			int dashDirection = getDashDirectionFromInput(xInput, zInput);
			if (canDoubleDash) dashDirection += 4;
			NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(), "dash", dashDirection, player.getId()), player);
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
}
