package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.network.C2S.ExecuteActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.*;
import com.dragonminez.server.events.players.actionmode.*;
import com.dragonminez.server.events.players.statuseffect.*;
import com.dragonminez.server.util.GravityLogic;
import com.dragonminez.server.world.dimension.OtherworldDimension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TickHandler {
	private static final Map<String, IActionModeHandler> ACTION_MODE_HANDLERS = new HashMap<>();
	private static final List<IStatusEffectHandler> STATUS_EFFECT_HANDLERS = new ArrayList<>();

    private static final int REGEN_INTERVAL = 20;
    private static final int SYNC_INTERVAL = 10;
    private static final double MEDITATION_BONUS_PER_LEVEL = 0.025;
    private static final double ACTIVE_CHARGE_MULTIPLIER = 1.5;
	private static int masterySeconds = 0;

    private static final Map<UUID, Integer> playerTickCounters = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer serverPlayer)) return;


        UUID playerId = serverPlayer.getUUID();
        int tickCounter = playerTickCounters.getOrDefault(playerId, 0) + 1;

        StatsProvider.get(StatsCapability.INSTANCE, serverPlayer).ifPresent(data -> {
            if (!data.getStatus().hasCreatedCharacter()) return;

			if (serverPlayer.hasEffect(MainEffects.STUN.get())) {
				data.getStatus().setChargingKi(false);
				data.getStatus().setActionCharging(false);
				data.getResources().setActionCharge(0);
				if (!data.getStatus().isStunned()) data.getStatus().setStunned(true);

				data.getCooldowns().tick();
				data.getEffects().tick();
				if (serverPlayer.tickCount % SYNC_INTERVAL == 0) NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(serverPlayer), serverPlayer);
				return;
			} else {
				data.getCooldowns().tick();
				data.getEffects().tick();
				if (data.getStatus().isStunned()) data.getStatus().setStunned(false);
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

			if (isChargingKi || (data.getStatus().isActionCharging() && (data.getStatus().getSelectedAction() == ActionMode.FORM || data.getStatus().getSelectedAction() == ActionMode.STACK))) {
				data.getStatus().setAuraActive(true);
			} else {
				data.getStatus().setAuraActive(false);
			}

			if (tickCounter % 5 == 0) {
				boolean hasYajirobe = serverPlayer.getInventory().hasAnyOf(Set.of(MainItems.KATANA_YAJIROBE.get()));
				boolean holdingYajirobe = serverPlayer.getMainHandItem().getItem() == MainItems.KATANA_YAJIROBE.get() || serverPlayer.getOffhandItem().getItem() == MainItems.KATANA_YAJIROBE.get();
				if (data.getStatus().isRenderKatana() != (hasYajirobe && !holdingYajirobe)) data.getStatus().setRenderKatana(hasYajirobe && !holdingYajirobe);

				ItemStack backItem = ItemStack.EMPTY;
				for (int i = 0; i < serverPlayer.getInventory().getContainerSize(); i++) {
					ItemStack stack = serverPlayer.getInventory().getItem(i);
					if (stack.isEmpty()) continue;
					Item item = stack.getItem();

					if (item == MainItems.Z_SWORD.get() || item == MainItems.BRAVE_SWORD.get() || item == MainItems.POWER_POLE.get()) {
						boolean isHeld = serverPlayer.getMainHandItem().getItem() == item || serverPlayer.getOffhandItem().getItem() == item;
						if (!isHeld) {
							backItem = item.getDefaultInstance();
							break;
						}
					}
				}

				if (backItem != ItemStack.EMPTY) {
					if (!data.getStatus().getBackWeapon().equals(backItem.getDescriptionId())) data.getStatus().setBackWeapon(backItem.getDescriptionId());
				} else data.getStatus().setBackWeapon("");

				boolean hasScouter = serverPlayer.getItemBySlot(EquipmentSlot.HEAD).getDescriptionId().contains("scouter");
				if (hasScouter) {
					String scouterItem = serverPlayer.getItemBySlot(EquipmentSlot.HEAD).getDescriptionId();
					if (!data.getStatus().getScouterItem().equals(scouterItem)) data.getStatus().setScouterItem(scouterItem);
				} else if (!data.getStatus().getScouterItem().isEmpty()) data.getStatus().setScouterItem("");

			}

			for (IStatusEffectHandler handler : STATUS_EFFECT_HANDLERS) {
				handler.onPlayerTick(serverPlayer, data);
			}

			if (tickCounter % 20 == 0) {
				handleActionCharge(serverPlayer, data);
				handleActiveFormDrains(serverPlayer, data);
				GravityLogic.tick(serverPlayer);
				if (ConfigManager.getServerConfig().getWorldGen().isOtherworldActive()) {
					if (!data.getStatus().isAlive() && !serverPlayer.serverLevel().dimension().equals(OtherworldDimension.OTHERWORLD_KEY)) {
						if (!serverPlayer.isSpectator() && !serverPlayer.isCreative()) {
							ServerLevel otherworld = serverPlayer.getServer().getLevel(OtherworldDimension.OTHERWORLD_KEY);
							serverPlayer.teleportTo(otherworld, 0, 41, 10, 0, 0);
						}
					}
				}

				if (data.getStatus().isAndroidUpgraded() && (data.getCharacter().getActiveForm().isEmpty() || data.getCharacter().getActiveForm() == null)) {
					data.getCharacter().setActiveForm("androidforms", "androidbase");
					serverPlayer.refreshDimensions();
				}

				for (IStatusEffectHandler handler : STATUS_EFFECT_HANDLERS) {
					handler.onPlayerSecond(serverPlayer, data);
				}
			}

			if (shouldSync) {
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(serverPlayer), serverPlayer);
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

        if (currentHealth < maxHealth) {
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
		boolean hasActiveStackForm = data.getCharacter().hasActiveStackForm();
		FormConfig.FormData activeStackForm = hasActiveStackForm ? data.getCharacter().getActiveStackFormData() : null;

        double energyChange = 0;

        if (activeCharging) {
            double baseRegen = classStats.getEnergyRegenRate();
            double regenAmount = maxEnergy * baseRegen * meditationBonus * ACTIVE_CHARGE_MULTIPLIER;
            if (ConfigManager.getServerConfig().getRacialSkills().isEnableRacialSkills()
                    && ConfigManager.getServerConfig().getRacialSkills().isHumanRacialSkill()
					&& ConfigManager.getRaceCharacter(data.getCharacter().getRace()).getRacialSkill().equals("human")) {
                regenAmount *= ConfigManager.getServerConfig().getRacialSkills().getHumanKiRegenBoost();
            }

			if (regenAmount <= 1.0) {
				regenAmount = 0.5;
			}
            energyChange += regenAmount;

            DMZEvent.KiChargeEvent kiEvent = new DMZEvent.KiChargeEvent(player, currentEnergy, maxEnergy);
            if (MinecraftForge.EVENT_BUS.post(kiEvent)) {
                energyChange = 0;
            }
        } else if (currentEnergy < maxEnergy) {
            double baseRegen = classStats.getEnergyRegenRate();
            double regenAmount = maxEnergy * baseRegen * meditationBonus;
            if (ConfigManager.getServerConfig().getRacialSkills().isEnableRacialSkills()
					&& ConfigManager.getServerConfig().getRacialSkills().isHumanRacialSkill()
					&& ConfigManager.getRaceCharacter(data.getCharacter().getRace()).getRacialSkill().equals("human")) {
				regenAmount *= ConfigManager.getServerConfig().getRacialSkills().getHumanKiRegenBoost();
			}
			if (regenAmount <= 1.0) {
				regenAmount = 0.5;
			}
            energyChange += regenAmount;
        }

		if (data.getStatus().isAndroidUpgraded()) {
			double baseRegen = classStats.getEnergyRegenRate();
			double regenAmount = maxEnergy * baseRegen * meditationBonus;
            if (ConfigManager.getServerConfig().getRacialSkills().isEnableRacialSkills()
                    && ConfigManager.getServerConfig().getRacialSkills().isHumanRacialSkill()
					&& ConfigManager.getRaceCharacter(data.getCharacter().getRace()).getRacialSkill().equals("human")) {
				regenAmount *= ConfigManager.getServerConfig().getRacialSkills().getHumanKiRegenBoost();
			}
			regenAmount *= ConfigManager.getServerConfig().getRacialSkills().getHumanKiRegenBoost();
			if (regenAmount <= 1.0) {
				regenAmount = 0.5;
			}
			energyChange += regenAmount;
		}

		if (masterySeconds < 5) {
			masterySeconds++;
		} else {
			masterySeconds = 0;

			if (hasActiveForm && activeForm != null) {
				String activeFormName = activeForm.getName().toLowerCase();
				String activeFormGroup = data.getCharacter().getActiveFormGroup();

				double maxMastery = 100.0;
				FormConfig.FormData formData = ConfigManager.getForm(data.getCharacter().getRaceName(), activeFormGroup, activeFormName);
				if (formData != null) maxMastery = formData.getMaxMastery();

				if (!data.getCharacter().getFormMasteries().hasMaxMastery(activeFormGroup, activeFormName, maxMastery)) {
					double masteryGain = formData != null ? formData.getPassiveMastery() : 0.001;
					data.getCharacter().getFormMasteries().addMastery(activeFormGroup, activeFormName, masteryGain, maxMastery);
				}
			}

			if (hasActiveStackForm && activeStackForm != null) {
				String activeFormName = activeStackForm.getName().toLowerCase();
				String activeFormGroup = data.getCharacter().getActiveStackFormGroup();

				double maxMastery = 100.0;
				FormConfig.FormData formData = ConfigManager.getStackForm(activeFormGroup, activeFormName);
				if (formData != null) maxMastery = formData.getMaxMastery();

				if (!data.getCharacter().getStackFormMasteries().hasMaxMastery(activeFormGroup, activeFormName, maxMastery)) {
					double masteryGain = formData != null ? formData.getPassiveMastery() : 0.001;
					data.getCharacter().getStackFormMasteries().addMastery(activeFormGroup, activeFormName, masteryGain, maxMastery);
				}
			}
		}

        if (energyChange != 0) {
            int newEnergy = (int) Math.max(0, Math.min(maxEnergy, currentEnergy + Math.ceil(energyChange)));
            data.getResources().setCurrentEnergy(newEnergy);

            if (newEnergy <= maxEnergy * 0.05 && !data.getStatus().isAndroidUpgraded() && (hasActiveForm || hasActiveStackForm )) {
                data.getCharacter().clearActiveForm();
				data.getCharacter().clearActiveStackForm();
				data.getResources().setPowerRelease(0);
				data.getResources().setActionCharge(0);
				player.refreshDimensions();
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
			data.getResources().addPoise((int) regenAmount);
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

		IActionModeHandler handler = ACTION_MODE_HANDLERS.get(mode.name());
		if (handler != null) {
			increment += handler.handleActionCharge(player, data);
		}

		if (increment > 0) {
			if (!(mode == ActionMode.FUSION && currentRelease >= 100)) {
				currentRelease += increment;
			}
			if (currentRelease >= 100) {
				currentRelease = 100;
				execute = true;
			}
			data.getResources().setActionCharge(currentRelease);

			int powerRelease = data.getResources().getPowerRelease();
			int potentialUnlockLevel = data.getSkills().getSkillLevel("potentialunlock");
			int maxRelease = 50 + (potentialUnlockLevel * 5);
			if (powerRelease < maxRelease) {
				int newRelease = Math.min(maxRelease, currentRelease + 5);
				data.getResources().setPowerRelease(newRelease);
			}
		}

		if (execute) {
			boolean success = performAction(player, data, mode);
			if (success) {
				data.getResources().setActionCharge(0);
			}
		}
	}

	private static boolean performAction(ServerPlayer player, StatsData data, ActionMode mode) {
		IActionModeHandler handler = ACTION_MODE_HANDLERS.get(mode.name());
		if (handler != null) {
			return handler.performAction(player, data);
		}
		return false;
	}

	private static void handleActiveFormDrains(ServerPlayer player, StatsData data) {
		boolean hasActiveForm = data.getCharacter().getActiveForm() != null && !data.getCharacter().getActiveForm().isEmpty();
		boolean hasActiveStackForm = data.getCharacter().getActiveStackForm() != null && !data.getCharacter().getActiveStackForm().isEmpty();
		if ((hasActiveForm || hasActiveStackForm) && !player.isCreative() && !player.isSpectator()) {
			int energyDrain = (int) data.getAdjustedEnergyDrain();
			int staminaDrain = (int) data.getAdjustedStaminaDrain();
			int healthDrain = (int) data.getAdjustedHealthDrain();

			boolean hasEnoughEnergy = data.getResources().getCurrentEnergy() >= energyDrain;
			boolean hasEnoughStamina = data.getResources().getCurrentStamina() >= staminaDrain;
			boolean hasEnoughHealth = data.getPlayer().getHealth() >= healthDrain;

			if (hasEnoughEnergy && hasEnoughStamina && hasEnoughHealth) {
				data.getResources().removeEnergy(energyDrain);
				data.getResources().removeStamina(staminaDrain);
				player.setHealth(player.getHealth() - healthDrain);
			} else {
				NetworkHandler.sendToServer(new ExecuteActionC2S("force_descend"));
			}
		}
	}

	public static void registerActionModeHandlers() {
		ACTION_MODE_HANDLERS.put(ActionMode.FORM.name(), new FormModeHandler());
		ACTION_MODE_HANDLERS.put(ActionMode.FUSION.name(), new FusionModeHandler());
		ACTION_MODE_HANDLERS.put(ActionMode.STACK.name(), new StackFormModeHandler());
		ACTION_MODE_HANDLERS.put(ActionMode.RACIAL.name(), new RacialModeHandler());
	}

	public static void registerStatusEffectHandlers() {
		STATUS_EFFECT_HANDLERS.add(new BioDrainHandler());
		STATUS_EFFECT_HANDLERS.add(new DashStatusHandler());
		STATUS_EFFECT_HANDLERS.add(new DoubleDashStatusHandler());
		STATUS_EFFECT_HANDLERS.add(new FlyStatusHandler());
		STATUS_EFFECT_HANDLERS.add(new FusionStatusHandler());
		STATUS_EFFECT_HANDLERS.add(new KiChargeStatusHandler());
		STATUS_EFFECT_HANDLERS.add(new MajinStatusHandler());
		STATUS_EFFECT_HANDLERS.add(new MightFruitStatusHandler());
		STATUS_EFFECT_HANDLERS.add(new SaiyanPassiveHandler());
		STATUS_EFFECT_HANDLERS.add(new TransformStatusHandler());
	}

	public static void registerActionModeHandler(String actionMode, IActionModeHandler actionModeHandler) {
		// FIXME: Should we write an error to the log when someone attempts to register a handler for an existing mode?
		ACTION_MODE_HANDLERS.putIfAbsent(actionMode, actionModeHandler);
	}

	public static void registerStatusEffectHandler(IStatusEffectHandler statusEffectHandler) {
		STATUS_EFFECT_HANDLERS.add(statusEffectHandler);
	}
}