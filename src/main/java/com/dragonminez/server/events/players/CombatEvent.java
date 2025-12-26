package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CombatEvent {

    private static final double STAMINA_CONSUMPTION_RATIO = 0.125; // 1/8

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        final double[] currentDamage = {event.getAmount()};

        if (source.getEntity() instanceof Player attacker && source.getMsgId().equals("player")) {
            StatsProvider.get(StatsCapability.INSTANCE, attacker).ifPresent(attackerData -> {
                if (!attackerData.getStatus().hasCreatedCharacter()) {
                    return;
                }

                double mcBaseDamage = currentDamage[0];
                double dmzDamage = attackerData.getMeleeDamage();

                if (ConfigManager.getServerConfig().getGameplay().isRespectAttackCooldown()) {
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

                int baseStaminaRequired = (int) Math.ceil(dmzDamage * STAMINA_CONSUMPTION_RATIO);
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

                if (attacker instanceof ServerPlayer serverPlayer) {
                    NetworkHandler.sendToPlayer(new StatsSyncS2C(serverPlayer), serverPlayer);
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

                if (isEmptyHandOrNoDamageItem(attacker)) {
                    currentDamage[0] = finalDmzDamage;
                } else {
                    currentDamage[0] = mcBaseDamage + finalDmzDamage;
                }
            });
        }

        if (event.getEntity() instanceof Player target) {
            StatsProvider.get(StatsCapability.INSTANCE, target).ifPresent(targetData -> {
                if (targetData.getStatus().hasCreatedCharacter()) {
                    double defense = targetData.getDefense();
                    currentDamage[0] = Math.max(1.0, currentDamage[0] - defense);

                    if (targetData.getCharacter().hasActiveForm()) {
                        FormConfig.FormData activeForm = targetData.getCharacter().getActiveFormData();
                        if (activeForm != null) {
                            String formGroup = targetData.getCharacter().getCurrentFormGroup();
                            String formName = targetData.getCharacter().getCurrentForm();
                            targetData.getCharacter().getFormMasteries().addMastery(
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
