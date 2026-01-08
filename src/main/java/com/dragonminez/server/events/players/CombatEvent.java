package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.Cooldowns;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CombatEvent {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        final double[] currentDamage = {event.getAmount()};

		// Attacker Damage Event
        if (source.getEntity() instanceof Player attacker && source.getMsgId().equals("player")) {
            StatsProvider.get(StatsCapability.INSTANCE, attacker).ifPresent(attackerData -> {
                if (!attackerData.getStatus().hasCreatedCharacter()) return;

                double mcBaseDamage = currentDamage[0];
                double dmzDamage = attackerData.getMeleeDamage();

                if (ConfigManager.getServerConfig().getCombat().isRespectAttackCooldown()) {
                    float attackStrength = attacker.getAttackStrengthScale(0.5F);
                    float adjustedStrength = attackStrength;

                    if (attackerData.getCharacter().hasActiveForm()) {
                        FormConfig.FormData activeForm = attackerData.getCharacter().getActiveFormData();
                        if (activeForm != null) {
                            adjustedStrength *= (float) activeForm.getAttackSpeed();
                        }
                    }

                    if (adjustedStrength > 1.0F) adjustedStrength = 1.0F;

                    float damageScale = 0.2F + adjustedStrength * adjustedStrength * 0.8F;
                    dmzDamage *= damageScale;
                }

                int baseStaminaRequired = (int) Math.ceil(dmzDamage * ConfigManager.getServerConfig().getCombat().getStaminaConsumptionRatio());
                double staminaDrainMultiplier = attackerData.getAdjustedStaminaDrain();
                int staminaRequired = (int) Math.ceil(baseStaminaRequired * staminaDrainMultiplier);
                int currentStamina = attackerData.getResources().getCurrentStamina();

                double finalDmzDamage;
                if (currentStamina >= staminaRequired) {
                    finalDmzDamage = dmzDamage;
                    attackerData.getResources().addStamina(-staminaRequired);
                } else {
                    double staminaRatio = (double) currentStamina / staminaRequired;
                    finalDmzDamage = dmzDamage * staminaRatio;
                    attackerData.getResources().setCurrentStamina(0);
                }

                if (attackerData.getCharacter().hasActiveForm()) {
                    FormConfig.FormData activeForm = attackerData.getCharacter().getActiveFormData();
                    if (activeForm != null) {
                        String formGroup = attackerData.getCharacter().getCurrentFormGroup();
                        String formName = attackerData.getCharacter().getCurrentForm();
                        attackerData.getCharacter().getFormMasteries().addMastery(
                                formGroup,
                                formName,
                                activeForm.getMasteryPerHit(),
                                activeForm.getMaxMastery()
                        );
                    }
                }

				if (attacker instanceof ServerPlayer serverPlayer) {
					NetworkHandler.sendToPlayer(new StatsSyncS2C(serverPlayer), serverPlayer);
				}

                if (isEmptyHandOrNoDamageItem(attacker)) {
                    currentDamage[0] = finalDmzDamage;
                } else {
                    currentDamage[0] = mcBaseDamage + finalDmzDamage;
                }
            });
        }

		// Victim Defense Event
        if (event.getEntity() instanceof Player target) {
            StatsProvider.get(StatsCapability.INSTANCE, target).ifPresent(victimData -> {
                if (victimData.getStatus().hasCreatedCharacter()) {
					double defense = victimData.getDefense();
					boolean blocked = false;

					if (ConfigManager.getServerConfig().getCombat().isEnableBlocking()) {
						if (victimData.getStatus().isBlocking() && !victimData.getStatus().isStunned() && source.getEntity() != null) {
							Vec3 targetLook = target.getLookAngle();
							Vec3 sourceLoc = source.getEntity().position();
							Vec3 targetLoc = target.position();
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
								System.out.println("Poise actual: " + currentPoise + ", Daño de poise: " + poiseDamage);

								if (currentPoise - poiseDamage <= 0) {
									victimData.getResources().setCurrentPoise(0);
									victimData.getStatus().setBlocking(false);
									victimData.getStatus().setStunned(true);

									int stunDuration = ConfigManager.getServerConfig().getCombat().getStunDurationTicks();
									victimData.getCooldowns().setCooldown("StunTimer", stunDuration);
									int regenCd = ConfigManager.getServerConfig().getCombat().getPoiseRegenCooldown();
									victimData.getCooldowns().setCooldown(Cooldowns.POISE_CD, regenCd);

									int currentStamina = victimData.getResources().getCurrentStamina();
									victimData.getResources().setCurrentStamina(currentStamina / 2);

									currentDamage[0] = Math.max(1.0, currentDamage[0] - defense);

									// Acá pondríamos sonido de Rotura de Guardia
                                    target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                                            MainSounds.UNBLOCK.get(),
                                            net.minecraft.sounds.SoundSource.PLAYERS,
                                            1.0F,
                                            0.9F + target.getRandom().nextFloat() * 0.1F);
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
											attackerLiving.knockback(1.5F, target.getX() - attackerLiving.getX(), target.getZ() - attackerLiving.getZ());
											attackerLiving.setDeltaMovement(attackerLiving.getDeltaMovement().scale(0.5));
										}
										System.out.println("Parry!");
                                        //SONIDO PARRY
                                        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                                                MainSounds.PARRY.get(),
                                                net.minecraft.sounds.SoundSource.PLAYERS,
                                                1.0F,
                                                0.9F + target.getRandom().nextFloat() * 0.1F);

                                    } else {
										double reductionCap = ConfigManager.getServerConfig().getCombat().getBlockDamageReductionCap();
										double reductionMin = ConfigManager.getServerConfig().getCombat().getBlockDamageReductionMin();
										double mitigationPct = (defense * 3.0) / (currentDamage[0] + (defense * 3.0));
										mitigationPct = Math.min(reductionCap, Math.max(mitigationPct, reductionMin));

										finalDmg = (float) (currentDamage[0] * (1.0 - mitigationPct));
                                        int randomSound = target.getRandom().nextInt(3);
                                        SoundEvent soundToPlay;

                                        if (randomSound == 0) {
                                            soundToPlay = MainSounds.BLOCK1.get();
                                        } else if (randomSound == 1) {
                                            soundToPlay = MainSounds.BLOCK2.get();
                                        } else {
                                            soundToPlay = MainSounds.BLOCK3.get();
                                        }

                                        System.out.println("Bloqueo! Daño antes: " + originalDmg + ", después: " + finalDmg);

                                        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                                                soundToPlay,
                                                net.minecraft.sounds.SoundSource.PLAYERS,
                                                1.0F,
                                                0.9F + target.getRandom().nextFloat() * 0.1F);

                                        //EFECTOS
                                        if (target.level() instanceof ServerLevel serverLevel) {
                                            double maxPoise = victimData.getMaxPoise();
                                            double currentPoiseVal = victimData.getResources().getCurrentPoise();
                                            double percentage = (currentPoiseVal / maxPoise) * 100.0;
                                            double r, g, b;

                                            if (percentage > 66) {
                                                r = 0.2; g = 0.9; b = 1.0;
                                            } else if (percentage > 33) {
                                                r = 1.0; g = 0.5; b = 0.0;
                                            } else {
                                                r = 1.0; g = 0.1; b = 0.1;
                                            }

                                            Vec3 look = target.getLookAngle();
                                            Vec3 spawnPos = target.getEyePosition().add(look.scale(0.6)).subtract(0, 0.3, 0);

                                            serverLevel.sendParticles(
                                                    MainParticles.BLOCK_PARTICLE.get(),
                                                    spawnPos.x, spawnPos.y, spawnPos.z,
                                                    0,
                                                    r, g, b,
                                                    1.0
                                            );
                                        }

									}

									if (target instanceof ServerPlayer sPlayer) {
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
							String formGroup = victimData.getCharacter().getCurrentFormGroup();
							String formName = victimData.getCharacter().getCurrentForm();
							victimData.getCharacter().getFormMasteries().addMastery(
									formGroup,
									formName,
									activeForm.getMasteryPerDamageReceived(),
									activeForm.getMaxMastery()
							);

							if (target instanceof ServerPlayer serverPlayer) {
								NetworkHandler.sendToPlayer(new StatsSyncS2C(serverPlayer), serverPlayer);
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

        if (mainHand.isEmpty()) {
            return true;
        }

        var attackDamageModifier = mainHand.getAttributeModifiers(EquipmentSlot.MAINHAND)
                .get(Attributes.ATTACK_DAMAGE);

        return attackDamageModifier.isEmpty();
    }
}
