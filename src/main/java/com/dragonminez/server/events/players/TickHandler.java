package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.server.util.RacialSkillLogic;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

	private static final UUID STUN_SLOW_UUID = UUID.fromString("7107DE5E-7CE8-4030-940E-514C1F160890");
    private static final int REGEN_INTERVAL = 20;
    private static final int SYNC_INTERVAL = 10;
    private static final double MEDITATION_BONUS_PER_LEVEL = 0.025;
    private static final double ACTIVE_CHARGE_MULTIPLIER = 1.5;
	private static int saiyanZenkaiSeconds = 0;

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

			var movementAttr = serverPlayer.getAttribute(Attributes.MOVEMENT_SPEED);

			if (data.getStatus().isStunned()) {
				if (!data.getCooldowns().hasCooldown(Cooldowns.STUN_TIMER)) data.getStatus().setStunned(false);
				else data.getStatus().setBlocking(false);

				if (movementAttr != null && movementAttr.getModifier(STUN_SLOW_UUID) == null) {
					AttributeModifier stunSlow = new AttributeModifier(
							STUN_SLOW_UUID,
							"Stun Immobilization",
							-1.0,
							AttributeModifier.Operation.MULTIPLY_TOTAL
					);
					movementAttr.addTransientModifier(stunSlow);
				}

			} else {
				if (movementAttr != null && movementAttr.getModifier(STUN_SLOW_UUID) != null) {
					movementAttr.removeModifier(STUN_SLOW_UUID);
				}
			}

            boolean shouldRegen = tickCounter >= REGEN_INTERVAL;
            boolean shouldSync = tickCounter % SYNC_INTERVAL == 0;
            boolean isChargingKi = data.getStatus().isChargingKi();
            boolean isDescending = data.getStatus().isDescending();
            int meditationLevel = data.getSkills().getSkillLevel("meditation");

            if (shouldRegen) {
                String raceName = data.getCharacter().getRaceName();
                String characterClass = data.getCharacter().getCharacterClass();
                
                RaceStatsConfig raceConfig = ConfigManager.getRaceStats(raceName);
                if (raceConfig != null) {
                    RaceStatsConfig.ClassStats classStats = getClassStats(raceConfig, characterClass);
                    
                    double meditationBonus = meditationLevel > 0 ? 1.0 + (meditationLevel * MEDITATION_BONUS_PER_LEVEL) : 1.0;
                    boolean activeCharging = isChargingKi && !isDescending;
                    
                    regenerateHealth(serverPlayer, data, classStats);
                    regenerateEnergy(serverPlayer, data, classStats, meditationBonus, activeCharging);
                    regenerateStamina(data, classStats, meditationBonus);
					regeneratePoise(data, meditationBonus);
                }

                playerTickCounters.put(playerId, 0);
            } else {
                playerTickCounters.put(playerId, tickCounter);
            }

			if (isChargingKi && tickCounter % 20 == 0) {
				int currentRelease = data.getResources().getPowerRelease();

				int potentialUnlockLevel = data.getSkills().getSkillLevel("potentialunlock");
				int maxRelease = 50 + (potentialUnlockLevel * 5);

				if (!isDescending && currentRelease < maxRelease) {
					int newRelease = Math.min(maxRelease, currentRelease + 5);
					data.getResources().setPowerRelease(newRelease);
				} else if (isDescending && currentRelease > 0) {
					int newRelease = Math.max(0, currentRelease - 5);
					data.getResources().setPowerRelease(newRelease);
				}
			}

			if (tickCounter % 20 == 0) {
				handleActionCharge(serverPlayer, data);
				handleKaiokenEffects(serverPlayer, data);
			}

			if (ConfigManager.getServerConfig().getRacialSkills().isEnableRacialSkills() && ConfigManager.getServerConfig().getRacialSkills().isSaiyanRacialSkill()) {
				if (tickCounter % 20 == 0 && data.getCharacter().getRaceName().equals("saiyan")) {
					handleSaiyanPassive(serverPlayer, data);
				}
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
                                        RaceStatsConfig.ClassStats classStats) {
        int currentHealth = (int) player.getHealth();
        float maxHealth = player.getMaxHealth();
        
        if (currentHealth < maxHealth && !data.getSkills().isSkillActive("kaioken")) {
            double baseRegen = classStats.getHealthRegenRate();
            double regenAmount = maxHealth * baseRegen;
			if (regenAmount <= 1.0) return;

			float newHealth = (float) Math.min(maxHealth, currentHealth + Math.ceil(regenAmount));
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
			if (ConfigManager.getServerConfig().getRacialSkills().isEnableRacialSkills() && ConfigManager.getServerConfig().getRacialSkills().isHumanRacialSkill()) {
				if (data.getCharacter().getRace().equals("human")) regenAmount *= ConfigManager.getServerConfig().getRacialSkills().getHumanKiRegenBoost();
			}
			if (regenAmount <= 1.0) regenAmount = 0.5;
            energyChange += regenAmount;

            DMZEvent.KiChargeEvent kiEvent = new DMZEvent.KiChargeEvent(player, currentEnergy, maxEnergy);
            if (MinecraftForge.EVENT_BUS.post(kiEvent)) {
                energyChange = 0;
            }
        } else if (!hasActiveForm && currentEnergy < maxEnergy) {
            double baseRegen = classStats.getEnergyRegenRate();
            double regenAmount = maxEnergy * baseRegen * meditationBonus;
			if (ConfigManager.getServerConfig().getRacialSkills().isEnableRacialSkills() && ConfigManager.getServerConfig().getRacialSkills().isHumanRacialSkill()) {
				if (data.getCharacter().getRace().equals("human")) regenAmount *= ConfigManager.getServerConfig().getRacialSkills().getHumanKiRegenBoost();
			}
			if (regenAmount <= 1.0) regenAmount = 0.5;
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

            if (newEnergy <= maxEnergy * 0.05 && hasActiveForm) {
                data.getCharacter().clearActiveForm();
            }
        }
    }

    private static void regenerateStamina(StatsData data,
                                         RaceStatsConfig.ClassStats classStats, double meditationBonus) {
        int currentStamina = data.getResources().getCurrentStamina();
        int maxStamina = data.getMaxStamina();
        
        if (currentStamina < maxStamina) {
            double baseRegen = classStats.getStaminaRegenRate();
            double regenAmount = maxStamina * baseRegen * meditationBonus;
			if (regenAmount <= 1.0) regenAmount = 0.5;
            
            int newStamina = (int) Math.min(maxStamina, currentStamina + Math.ceil(regenAmount));
            data.getResources().setCurrentStamina(newStamina);
        }
    }

	private static void regeneratePoise(StatsData data, double meditationBonus) {
		if (data.getCooldowns().hasCooldown(Cooldowns.POISE_CD) || data.getStatus().isBlocking() || data.getStatus().isStunned()) return;

		int currentPoise = data.getResources().getCurrentPoise();
		int maxPoise = data.getMaxPoise();

		if (currentPoise < maxPoise) {
			double baseRegen = 0.1;
			double regenAmount = maxPoise * baseRegen * meditationBonus;
			if (regenAmount < 1.0) regenAmount = 1.0;
			int newPoise = (int) Math.min(maxPoise, currentPoise + Math.ceil(regenAmount));
			data.getResources().setCurrentPoise(newPoise);
		}
	}

	private static void handleActionCharge(ServerPlayer player, StatsData data) {
		if (!data.getStatus().isActionCharging()) {
			if (data.getResources().getActionCharge() > 0) {
				data.getResources().setActionCharge(0);
			}
			return;
		}

		ActionMode mode = data.getStatus().getSelectedAction();
		int currentRelease = data.getResources().getActionCharge();
		int increment = 0;
		boolean execute = false;

		switch (mode) {
			case KAIOKEN -> {
				if (TransformationsHelper.canStackKaioken(data)) {
					int skillLvl = data.getSkills().getSkillLevel("kaioken");
					int currentPhase = data.getStatus().getActiveKaiokenPhase();
					int maxPhase = TransformationsHelper.getMaxKaiokenPhase(skillLvl);
					if (currentPhase < maxPhase) {
						increment = 25;
					}
				} else {
					data.getStatus().setActionCharging(false);
					return;
				}
			}
			case FUSION -> {
				if (data.getSkills().hasSkill("fusion") && !data.getCooldowns().hasCooldown(Cooldowns.COMBAT) && !data.getCooldowns().hasCooldown(Cooldowns.FUSION_CD)) {
					increment = 10;
				}
			}
			case RACIAL -> {
				String race = data.getCharacter().getRaceName();
				if ("bioandroid".equals(race)) {
					RacialSkillLogic.attemptRacialAction(player);
					data.getStatus().setActionCharging(false);
					return;
				}
				increment = 25;
			}
			case FORM -> {
				FormConfig.FormData nextForm = TransformationsHelper.getNextAvailableForm(data);
				if (nextForm != null) {
					String group = data.getCharacter().hasActiveForm() ? data.getCharacter().getActiveFormGroup() : data.getCharacter().getSelectedFormGroup();

					String type = ConfigManager.getFormGroup(data.getCharacter().getRaceName(), group).getFormType();
					int skillLvl = switch (type) {
						case "super" -> data.getSkills().getSkillLevel("superform");
						case "god" -> data.getSkills().getSkillLevel("godform");
						case "legendary" -> data.getSkills().getSkillLevel("legendaryforms");
						default -> 1;
					};
					increment = 5 * Math.max(1, skillLvl);
				}
			}
		}

		if (increment > 0) {
			currentRelease += increment;
			if (currentRelease >= 100) {
				currentRelease = 0;
				execute = true;
			}
			data.getResources().setActionCharge(currentRelease);
		}

		if (execute) {
			performAction(player, data, mode);
			data.getResources().setActionCharge(0);
		}
	}

	private static void performAction(ServerPlayer player, StatsData data, ActionMode mode) {
		switch (mode) {
			case KAIOKEN -> {
				int currentPhase = data.getStatus().getActiveKaiokenPhase();
				int skillLvl = data.getSkills().getSkillLevel("kaioken");
				int maxPhase = TransformationsHelper.getMaxKaiokenPhase(skillLvl);

				if (currentPhase < maxPhase) {
					data.getStatus().setActiveKaiokenPhase(currentPhase + 1);
					String name = TransformationsHelper.getKaiokenName(currentPhase + 1);
					player.displayClientMessage(Component.translatable("message.dragonminez.kaioken.activate", name), true);
				}

				if (!data.getSkills().isSkillActive("kaioken")) data.getSkills().setSkillActive("kaioken", true);
				NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
			}
			case RACIAL -> RacialSkillLogic.attemptRacialAction(player);
			case FUSION -> attemptFusion(player, data);
			case FORM -> attemptTransform(player, data);
		}
	}

	private static void handleKaiokenEffects(ServerPlayer player, StatsData data) {
		if (!data.getSkills().isSkillActive("kaioken")) return;
		if (player.isCreative() || player.isSpectator()) return;

		if (player.tickCount % 20 == 0) {
			float drain = TransformationsHelper.getKaiokenHealthDrain(data);

			if (player.getHealth() - drain <= 1.0f) {
				data.getSkills().setSkillActive("kaioken", false);
				data.getResources().setPowerRelease(0);
				data.getResources().setActionCharge(0);
			} else {
				player.setHealth(player.getHealth() - drain);
				player.hurtTime = 0;
				player.hurtDuration = 0;
				player.invulnerableTime = 0;
			}
		}
	}


	private static void attemptTransform(ServerPlayer player, StatsData data) {
		FormConfig.FormData nextForm = TransformationsHelper.getNextAvailableForm(data);
		if (nextForm == null) return;

		int cost = (int) (data.getMaxEnergy() * nextForm.getEnergyDrain());
		if (data.getResources().getCurrentEnergy() >= cost) {
			data.getResources().removeEnergy(cost);

			String group = data.getCharacter().hasActiveForm() ?
					data.getCharacter().getActiveFormGroup() :
					data.getCharacter().getSelectedFormGroup();

			data.getCharacter().setActiveForm(group, nextForm.getName());
		} else {
			player.displayClientMessage(Component.translatable("message.dragonminez.form.no_ki", cost), true);
		}
	}

	private static void attemptFusion(ServerPlayer player, StatsData data) {
		List<ServerPlayer> nearby = player.level().getEntitiesOfClass(ServerPlayer.class,
				player.getBoundingBox().inflate(5.0), p -> p != player);

		for (ServerPlayer target : nearby) {
			StatsProvider.get(StatsCapability.INSTANCE, target).ifPresent(targetData -> {
				if (targetData.getStatus().getSelectedAction() == ActionMode.FUSION
						&& targetData.getResources().getActionCharge() >= 50
						&& targetData.getStatus().isActionCharging()) {

					MinecraftForge.EVENT_BUS.post(new DMZEvent.FusionEvent(player, target, DMZEvent.FusionEvent.FusionType.METAMORU));
					player.sendSystemMessage(Component.translatable("message.dragonminez.fusion.success", target.getName().getString()));
					targetData.getResources().setActionCharge(0);
					targetData.getStatus().setActionCharging(false);
				}
			});
		}
	}

	private static void handleSaiyanPassive(ServerPlayer player, StatsData data) {
		if (data.getResources().getRacialSkillCount() >= ConfigManager.getServerConfig().getRacialSkills().getSaiyanZenkaiAmount()) return;
		if (data.getCooldowns().hasCooldown(Cooldowns.ZENKAI)) return;

		float maxHealth = player.getMaxHealth();
		if (player.getHealth() <= maxHealth * 0.15) saiyanZenkaiSeconds = saiyanZenkaiSeconds + 1;
		else saiyanZenkaiSeconds = 0;

		if (saiyanZenkaiSeconds >= 8) {
			data.getResources().setRacialSkillCount(data.getResources().getRacialSkillCount() + 1);
			player.heal((float) (maxHealth * ConfigManager.getServerConfig().getRacialSkills().getSaiyanZenkaiHealthRegen()));
			String[] boosts = ConfigManager.getServerConfig().getRacialSkills().getSaiyanZenkaiBoosts();
			for (String boost : boosts) {
				int bonusValue;
				switch (boost) {
					case "STR" -> bonusValue = (int) Math.round(data.getStats().getStrength() * ConfigManager.getServerConfig().getRacialSkills().getSaiyanZenkaiStatBoost());
					case "SKP" -> bonusValue = (int) Math.round(data.getStats().getStrikePower() * ConfigManager.getServerConfig().getRacialSkills().getSaiyanZenkaiStatBoost());
					case "RES" -> bonusValue = (int) Math.round(data.getStats().getResistance() * ConfigManager.getServerConfig().getRacialSkills().getSaiyanZenkaiStatBoost());
					case "VIT" -> bonusValue = (int) Math.round(data.getStats().getVitality() * ConfigManager.getServerConfig().getRacialSkills().getSaiyanZenkaiStatBoost());
					case "ENE" -> bonusValue = (int) Math.round(data.getStats().getEnergy() * ConfigManager.getServerConfig().getRacialSkills().getSaiyanZenkaiStatBoost());
					case "PWR" -> bonusValue = (int) Math.round(data.getStats().getKiPower() * ConfigManager.getServerConfig().getRacialSkills().getSaiyanZenkaiStatBoost());
					default -> bonusValue = 0;
				}
				if (bonusValue >= 1) {
					data.getBonusStats().addBonus(boost, "Zenkai_" + data.getResources().getRacialSkillCount(), "+", bonusValue);
				}
			}

			player.displayClientMessage(Component.translatable("message.dragonminez.racial.zenkai.used"), true);

			data.getResources().addRacialSkillCount(1);
			data.getCooldowns().addCooldown(Cooldowns.ZENKAI, ConfigManager.getServerConfig().getRacialSkills().getSaiyanZenkaiCooldownSeconds());
			NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
			saiyanZenkaiSeconds = 0;
		}
	}
}

