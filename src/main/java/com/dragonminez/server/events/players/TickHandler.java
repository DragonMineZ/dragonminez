package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.TransformationHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TickHandler {

    private static final int REGEN_INTERVAL = 20;
    private static final int SYNC_INTERVAL = 10;
    private static final double MEDITATION_BONUS_PER_LEVEL = 0.025;
    private static final double ACTIVE_CHARGE_MULTIPLIER = 1.5;

    private static final Map<UUID, Integer> playerTickCounters = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }

        if (!(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        UUID playerId = serverPlayer.getUUID();
        int tickCounter = playerTickCounters.getOrDefault(playerId, 0) + 1;

        StatsProvider.get(StatsCapability.INSTANCE, serverPlayer).ifPresent(data -> {
            if (!data.getStatus().hasCreatedCharacter() || !data.getStatus().isAlive()) {
                return;
            }

            data.getEffects().tick();

            boolean shouldRegen = tickCounter >= REGEN_INTERVAL;
            boolean shouldSync = tickCounter % SYNC_INTERVAL == 0;
            boolean isChargingKi = data.getStatus().isChargingKi();
            boolean isDescending = data.getStatus().isDescending();
            boolean isTransforming = data.getStatus().isTransforming();
            int meditationLevel = data.getSkills().getSkillLevel("meditation");
            boolean isFormActive = data.getCharacter().hasActiveForm();

            if (shouldRegen) {
                String raceName = data.getCharacter().getRaceName();
                String characterClass = data.getCharacter().getCharacterClass();
                
                RaceStatsConfig raceConfig = ConfigManager.getRaceStats(raceName);
                if (raceConfig != null) {
                    RaceStatsConfig.ClassStats classStats = getClassStats(raceConfig, characterClass);
                    
                    double meditationBonus = meditationLevel > 0 ? 1.0 + (meditationLevel * MEDITATION_BONUS_PER_LEVEL) : 1.0;
                    boolean activeCharging = isChargingKi && !isDescending;
                    
                    regenerateHealth(serverPlayer, data, classStats, meditationBonus);
                    regenerateEnergy(serverPlayer, data, classStats, meditationBonus, activeCharging);
                    regenerateStamina(data, classStats, meditationBonus, activeCharging);
                }

                playerTickCounters.put(playerId, 0);
            } else {
                playerTickCounters.put(playerId, tickCounter);
            }

			if (isChargingKi && tickCounter % 20 == 0) {
				int currentRelease = data.getResources().getPowerRelease();
				if (!isDescending && currentRelease < 100) {
					int newRelease = Math.min(100, currentRelease + 5);
					data.getResources().setPowerRelease(newRelease);
				} else if (isDescending && currentRelease > 0) {
					int newRelease = Math.max(0, currentRelease - 5);
					data.getResources().setPowerRelease(newRelease);
				}
			}

			if (isTransforming && tickCounter % 20 == 0) {
				boolean canTransform = TransformationHelper.canTransform(data);
				boolean hasKaiokenSelected = TransformationHelper.hasKaiokenSelected(data);

				if (canTransform || hasKaiokenSelected) {
					int currentFormRelease = data.getResources().getFormRelease();
					if (currentFormRelease < 100) {
						int newFormRelease = Math.min(100, currentFormRelease + 10);
						data.getResources().setFormRelease(newFormRelease);

						if (newFormRelease >= 100) {
							handleTransformation(serverPlayer, data, hasKaiokenSelected);
							data.getResources().setFormRelease(0);
						}
					}
				} else {
					data.getResources().setFormRelease(0);
				}
			} else if (data.getResources().getFormRelease() > 0) {
				data.getResources().setFormRelease(0);
			}

            if (shouldSync) {
                NetworkHandler.sendToPlayer(new StatsSyncS2C(serverPlayer), serverPlayer);
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        playerTickCounters.remove(event.getEntity().getUUID());
    }

    private static RaceStatsConfig.ClassStats getClassStats(RaceStatsConfig config, String characterClass) {
        return switch (characterClass.toLowerCase()) {
            case "warrior" -> config.getWarrior();
            case "spiritualist" -> config.getSpiritualist();
            case "martialartist", "martial_artist" -> config.getMartialArtist();
            default -> config.getWarrior();
        };
    }

    private static void regenerateHealth(ServerPlayer player, StatsData data, 
                                        RaceStatsConfig.ClassStats classStats, double meditationBonus) {
        int currentHealth = (int) player.getHealth();
        int maxHealth = data.getMaxHealth();
        
        if (currentHealth < maxHealth) {
            double baseRegen = classStats.getHealthRegenRate();
            double regenAmount = maxHealth * baseRegen * meditationBonus;
            
            int newHealth = (int) Math.min(maxHealth, currentHealth + Math.ceil(regenAmount));
            player.setHealth(newHealth);
        }
    }

    private static void regenerateEnergy(ServerPlayer player, StatsData data,
                                        RaceStatsConfig.ClassStats classStats, double meditationBonus, boolean activeCharging) {
        int currentEnergy = data.getResources().getCurrentEnergy();
        int maxEnergy = data.getMaxEnergy();
        
        boolean hasActiveForm = data.getCharacter().hasActiveForm();
        FormConfig.FormData activeForm = hasActiveForm ? data.getCharacter().getActiveFormData() : null;

        double energyChange = 0;

        if (activeCharging) {
            double baseRegen = classStats.getEnergyRegenRate();
            double regenAmount = maxEnergy * baseRegen * meditationBonus * ACTIVE_CHARGE_MULTIPLIER;
            energyChange += regenAmount;

            DMZEvent.KiChargeEvent kiEvent = new DMZEvent.KiChargeEvent(player, currentEnergy, maxEnergy);
            if (MinecraftForge.EVENT_BUS.post(kiEvent)) {
                energyChange = 0;
            }
        } else if (!hasActiveForm && currentEnergy < maxEnergy) {
            double baseRegen = classStats.getEnergyRegenRate();
            double regenAmount = maxEnergy * baseRegen * meditationBonus;
            energyChange += regenAmount;
        }

        if (hasActiveForm && activeForm != null) {
            double drainRate = data.getAdjustedEnergyDrain();
            double drainAmount = maxEnergy * (drainRate / 100.0);
            energyChange -= drainAmount;
        }

        if (energyChange != 0) {
            int newEnergy = (int) Math.max(0, Math.min(maxEnergy, currentEnergy + Math.ceil(energyChange)));
            data.getResources().setCurrentEnergy(newEnergy);

            if (newEnergy <= 0 && hasActiveForm) {
                data.getCharacter().clearActiveForm();
            }
        }
    }

    private static void regenerateStamina(StatsData data,
                                         RaceStatsConfig.ClassStats classStats, double meditationBonus, boolean activeCharging) {
        int currentStamina = data.getResources().getCurrentStamina();
        int maxStamina = data.getMaxStamina();
        
        if (currentStamina < maxStamina) {
            double baseRegen = classStats.getStaminaRegenRate();
            double multiplier = activeCharging ? ACTIVE_CHARGE_MULTIPLIER : 1.0;
            double regenAmount = maxStamina * baseRegen * meditationBonus * multiplier;
            
            int newStamina = (int) Math.min(maxStamina, currentStamina + Math.ceil(regenAmount));
            data.getResources().setCurrentStamina(newStamina);
        }
    }

    private static void handleTransformation(ServerPlayer player, StatsData data, boolean hasKaiokenSelected) {
        if (hasKaiokenSelected) {
            int kaiokenLevel = data.getSkills().getSkillLevel("kaioken");
            if (kaiokenLevel > 0) {
                data.getSkills().setSkillActive("kaioken", true);
            }
        } else {
            String raceName = data.getCharacter().getRaceName();

            if (data.getCharacter().hasActiveForm()) {
                String currentGroup = data.getCharacter().getActiveFormGroup();
                String currentFormName = data.getCharacter().getActiveFormName();

                FormConfig.FormData nextForm = TransformationHelper.getNextForm(data, raceName, currentGroup, currentFormName);
                if (nextForm != null) {
                    data.getCharacter().setActiveForm(currentGroup, nextForm.getName());
                }
            } else {
                String selectedGroup = data.getCharacter().getSelectedFormGroup();
                if (selectedGroup != null && !selectedGroup.isEmpty()) {
                    List<FormConfig.FormData> unlockedForms = TransformationHelper.getUnlockedForms(data, raceName, selectedGroup);
                    if (!unlockedForms.isEmpty()) {
                        FormConfig.FormData firstForm = unlockedForms.get(0);
                        data.getCharacter().setActiveForm(selectedGroup, firstForm.getName());
                    }
                }
            }
        }
    }
}

