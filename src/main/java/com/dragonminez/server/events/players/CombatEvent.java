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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CombatEvent {

    private static final double STAMINA_CONSUMPTION_RATIO = 0.125; // 1/8

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        DamageSource source = event.getSource();

        if (source.getEntity() instanceof Player attacker) {
            boolean respectCooldown = ConfigManager.getServerConfig().getGameplay().isRespectAttackCooldown();

            if (respectCooldown) {
                float attackStrength = attacker.getAttackStrengthScale(0.5F);

                StatsProvider.get(StatsCapability.INSTANCE, attacker).ifPresent(attackerData -> {
                    if (attackerData.getStatus().hasCreatedCharacter() && attackerData.getCharacter().hasActiveForm()) {
                        FormConfig.FormData activeForm = attackerData.getCharacter().getActiveFormData();
                        if (activeForm != null) {
                            double attackSpeed = activeForm.getAttackSpeed();
                            float adjustedStrength = (float) (attackStrength * attackSpeed);

                            if (adjustedStrength < 0.9F) {
                                event.setCanceled(true);
                            }
                        } else if (attackStrength < 0.9F) {
                            event.setCanceled(true);
                        }
                    } else if (attackStrength < 0.9F) {
                        event.setCanceled(true);
                    }
                });
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();

        if (!(source.getEntity() instanceof Player attacker) || !source.getMsgId().equals("player")) {
            return;
        }

        StatsProvider.get(StatsCapability.INSTANCE, attacker).ifPresent(attackerData -> {
            if (!attackerData.getStatus().hasCreatedCharacter()) {
                return;
            }

            double mcBaseDamage = event.getAmount();
            double dmzDamage = attackerData.getMeleeDamage();

            int baseStaminaRequired = (int) Math.ceil(dmzDamage * STAMINA_CONSUMPTION_RATIO);

            double staminaDrainMultiplier = 1.0;
            if (attackerData.getCharacter().hasActiveForm()) {
                FormConfig.FormData activeForm = attackerData.getCharacter().getActiveFormData();
                if (activeForm != null) {
                    staminaDrainMultiplier = activeForm.getStaminaDrain();
                }
            }

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

            double totalDamage;
            if (isEmptyHandOrNoDamageItem(attacker)) {
                totalDamage = finalDmzDamage;
            } else {
                totalDamage = mcBaseDamage + finalDmzDamage;
            }

            double finalTotalDamage = totalDamage;
            if (event.getEntity() instanceof Player target) {
                var targetDataOpt = StatsProvider.get(StatsCapability.INSTANCE, target);
                final double[] adjustedDamage = {finalTotalDamage};

                targetDataOpt.ifPresent(targetData -> {
                    if (targetData.getStatus().hasCreatedCharacter()) {
                        double defense = targetData.getDefense();
                        adjustedDamage[0] = Math.max(1.0, adjustedDamage[0] - defense);
                    }
                });

                event.setAmount((float) adjustedDamage[0]);
            } else {
                event.setAmount((float) finalTotalDamage);
            }
        });
    }

    private static boolean isEmptyHandOrNoDamageItem(Player player) {
        ItemStack mainHand = player.getMainHandItem();

        if (mainHand.isEmpty()) {
            return true;
        }

        var attackDamageModifier = mainHand.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                .get(Attributes.ATTACK_DAMAGE);

        return attackDamageModifier.isEmpty();
    }
}

