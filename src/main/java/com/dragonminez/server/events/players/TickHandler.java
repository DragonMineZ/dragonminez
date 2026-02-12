package com.dragonminez.server.events.players;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.server.util.FusionLogic;
import com.dragonminez.server.util.GravityLogic;
import com.dragonminez.server.util.RacialSkillLogic;
import com.dragonminez.server.world.dimension.OtherworldDimension;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

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
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer serverPlayer)) return;


        UUID playerId = serverPlayer.getUUID();
        int tickCounter = playerTickCounters.getOrDefault(playerId, 0) + 1;

        StatsProvider.get(StatsCapability.INSTANCE, serverPlayer).ifPresent(data -> {
            if (!data.getStatus().hasCreatedCharacter()) return;

			handleBioDrainTick(serverPlayer, data);

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

			if (isChargingKi || (data.getStatus().isActionCharging() && (data.getStatus().getSelectedAction() == ActionMode.FORM || data.getStatus().getSelectedAction() == ActionMode.KAIOKEN))) {
				data.getStatus().setAuraActive(true);
			} else {
				data.getStatus().setAuraActive(false);
			}

			if (data.getSkills().isSkillActive("fly") && !serverPlayer.isCreative() && !serverPlayer.isSpectator()) {
				if (serverPlayer.horizontalCollision) {
					double dx = serverPlayer.getX() - serverPlayer.xOld;
					double dz = serverPlayer.getZ() - serverPlayer.zOld;
					double speed = Math.sqrt(dx * dx + dz * dz);
					double minImpactSpeed = 0.35D;

					if (speed > minImpactSpeed) {
						float maxHealth = serverPlayer.getMaxHealth();
						double maxImpactSpeedRef = 1.5D;
						double factor = (speed - minImpactSpeed) / (maxImpactSpeedRef - minImpactSpeed);
						factor = Mth.clamp(factor, 0.0, 1.0);
						float finalPct = (float) Mth.lerp(factor, 0.05f, 0.35f);
						float damage = maxHealth * finalPct;
						serverPlayer.hurt(serverPlayer.damageSources().flyIntoWall(), damage);
						serverPlayer.level().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0F, (float) (0.5F + (factor * 0.5F)));
					}
				}
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

			if (tickCounter % 20 == 0) {
				handleActionCharge(serverPlayer, data);
				handleKaiokenEffects(serverPlayer, data);
				handleFlightKiDrain(serverPlayer, data);
				GravityLogic.tick(serverPlayer);
				if (ConfigManager.getServerConfig().getWorldGen().isOtherworldActive()) {
					if (!data.getStatus().isAlive() && !serverPlayer.serverLevel().dimension().equals(OtherworldDimension.OTHERWORLD_KEY)) {
						if (!serverPlayer.isSpectator() && !serverPlayer.isCreative()) {
							ServerLevel otherworld = serverPlayer.getServer().getLevel(OtherworldDimension.OTHERWORLD_KEY);
							serverPlayer.teleportTo(otherworld, 0, 41, 10, 0, 0);
						}
					}
				}

				if (data.getStatus().isAndroidUpgraded() && (data.getCharacter().getActiveForm().isEmpty() || data.getCharacter().getActiveForm() == null))
					data.getCharacter().setActiveForm("androidforms", "androidbase");
			}

			if (ConfigManager.getServerConfig().getRacialSkills().isEnableRacialSkills() && ConfigManager.getServerConfig().getRacialSkills().isSaiyanRacialSkill()) {
				if (tickCounter % 20 == 0 && data.getCharacter().getRaceName().equals("saiyan")) {
					handleSaiyanPassive(serverPlayer, data);
				}
			}

			fusionTickHandling(serverPlayer, data);
			handleStatusEffects(serverPlayer, data);

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

		if (data.getStatus().isAndroidUpgraded()) {
			double baseRegen = classStats.getEnergyRegenRate();
			double regenAmount = maxEnergy * baseRegen * meditationBonus;
			if (ConfigManager.getServerConfig().getRacialSkills().isEnableRacialSkills() && ConfigManager.getServerConfig().getRacialSkills().isHumanRacialSkill()) {
				if (data.getCharacter().getRace().equals("human")) regenAmount *= ConfigManager.getServerConfig().getRacialSkills().getHumanKiRegenBoost();
			}
			regenAmount *= ConfigManager.getServerConfig().getRacialSkills().getHumanKiRegenBoost();
			if (regenAmount <= 1.0) regenAmount = 0.5;
			energyChange += regenAmount;
		}

        if (hasActiveForm && activeForm != null) {
            if (!data.getStatus().isAndroidUpgraded()) {
                double drainRate = data.getAdjustedEnergyDrain();
                double drainAmount = maxEnergy * (drainRate / 100.0);
                energyChange -= drainAmount;
            }
        }

        if (energyChange != 0) {
            int newEnergy = (int) Math.max(0, Math.min(maxEnergy, currentEnergy + Math.ceil(energyChange)));
            data.getResources().setCurrentEnergy(newEnergy);

            if (newEnergy <= maxEnergy * 0.05 && hasActiveForm && !data.getStatus().isAndroidUpgraded()) {
                data.getCharacter().clearActiveForm();
				data.getResources().setPowerRelease(0);
				data.getResources().setActionCharge(0);
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
						case "android" -> data.getSkills().getSkillLevel("androidforms");
						default -> 1;
					};
					increment = 5 * Math.max(1, skillLvl);
				}
			}
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
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
				return true;
			}
			case RACIAL -> {
				RacialSkillLogic.attemptRacialAction(player);
				return true;
			}
			case FUSION -> {
				return attemptFusion(player, data);
			}
			case FORM -> {
				attemptTransform(player, data);
				return true;
			}
		}
		return false;
	}

	private static void handleKaiokenEffects(ServerPlayer player, StatsData data) {
		if (!data.getSkills().isSkillActive("kaioken")) return;
		if (player.isCreative() || player.isSpectator()) return;

		if (player.tickCount % 20 == 0) {
			float drain = TransformationsHelper.getKaiokenHealthDrain(data);

			if (player.getHealth() - drain <= 1.0f) {
				data.getSkills().setSkillActive("kaioken", false);
				data.getStatus().setActiveKaiokenPhase(0);
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

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.TRANSFORM.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
		} else {
			player.displayClientMessage(Component.translatable("message.dragonminez.form.no_ki", cost), true);
		}
	}

	private static boolean attemptFusion(ServerPlayer player, StatsData data) {
		List<ServerPlayer> nearby = player.level().getEntitiesOfClass(ServerPlayer.class,
				player.getBoundingBox().inflate(5.0), p -> p != player);

		for (ServerPlayer target : nearby) {
			StatsProvider.get(StatsCapability.INSTANCE, target).ifPresent(targetData -> {
				if (targetData.getStatus().getSelectedAction() == ActionMode.FUSION && targetData.getResources().getActionCharge() >= 50 && targetData.getStatus().isActionCharging()) {
					if (data.getResources().getActionCharge() >= 100) {
						if (FusionLogic.executeMetamoru(player, target, data, targetData)) {
							data.getResources().setActionCharge(0);
							targetData.getResources().setActionCharge(0);

                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.FUSION.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
						}
					}
				}
			});
			if (data.getStatus().isFused()) return true;
		}
		return false;
	}

	private static void handleSaiyanPassive(ServerPlayer player, StatsData data) {
		GeneralServerConfig.RacialSkillsConfig config = ConfigManager.getServerConfig().getRacialSkills();

		if (data.getResources().getRacialSkillCount() >= config.getSaiyanZenkaiAmount()) return;
		if (data.getCooldowns().hasCooldown(Cooldowns.ZENKAI)) return;

		float maxHealth = player.getMaxHealth();
		if (player.getHealth() <= maxHealth * 0.15) {
			saiyanZenkaiSeconds = saiyanZenkaiSeconds + 1;
		} else {
			saiyanZenkaiSeconds = 0;
		}

		if (saiyanZenkaiSeconds >= 8) {
			player.heal((float) (maxHealth * config.getSaiyanZenkaiHealthRegen()));

			double boostMult = config.getSaiyanZenkaiStatBoost();
			String[] statsToBoost = config.getSaiyanZenkaiBoosts();

			for (String statKey : statsToBoost) {
				int currentStat = getStatValue(data, statKey);
				int bonus = (int) Math.max(1, currentStat * boostMult);
				data.getBonusStats().addBonus(statKey, "Zenkai_" + (data.getResources().getRacialSkillCount() + 1), "+", bonus);
			}

			player.displayClientMessage(Component.translatable("message.dragonminez.racial.zenkai.used"), true);

			data.getResources().addRacialSkillCount(1);
			data.getCooldowns().setCooldown(Cooldowns.ZENKAI, config.getSaiyanZenkaiCooldownSeconds());
			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			saiyanZenkaiSeconds = 0;
		}
	}

	private static int getStatValue(StatsData data, String statName) {
		return switch (statName) {
			case "STR" -> data.getStats().getStrength();
			case "SKP" -> data.getStats().getStrikePower();
			case "RES" -> data.getStats().getResistance();
			case "VIT" -> data.getStats().getVitality();
			case "PWR" -> data.getStats().getKiPower();
			case "ENE" -> data.getStats().getEnergy();
			default -> 0;
		};
	}

	private static void handleBioDrainTick(ServerPlayer player, StatsData data) {
		int targetId = data.getStatus().getDrainingTargetId();
		if (targetId == -1) return;

		if (data.getCooldowns().getCooldown(Cooldowns.DRAIN_ACTIVE) > 0) {
			Entity entity = player.level().getEntity(targetId);
			if (entity instanceof LivingEntity target && target.isAlive() && player.distanceTo(target) < 6.0f) {
				float targetYRot = target.getYRot();

				double dist = 0.75;
				double rads = Math.toRadians(targetYRot);
				double xOffset = -Math.sin(rads) * dist;
				double zOffset = Math.cos(rads) * dist;

				double finalX = target.getX() - xOffset;
				double finalZ = target.getZ() - zOffset;
				double finalY = target.getY();

				player.connection.teleport(finalX, finalY, finalZ, targetYRot, 15.0F);

				player.setYRot(targetYRot);
				player.setYHeadRot(targetYRot);

				if (player.tickCount % 20 == 0) {
					GeneralServerConfig.RacialSkillsConfig config = ConfigManager.getServerConfig().getRacialSkills();
					double totalDrainRatio = config.getBioAndroidDrainRatio();

					float durationSeconds = 6.0f;
					float totalHealthToDrain = (float) (target.getMaxHealth() * totalDrainRatio);
					float drainPerSecond = totalHealthToDrain / durationSeconds;

					target.setHealth(Math.max(1, target.getHealth() - drainPerSecond));
					if (player.getHealth() < player.getMaxHealth()) {
						player.heal(drainPerSecond);
					}
					if (data.getResources().getCurrentEnergy() < data.getMaxEnergy()) {
						data.getResources().addEnergy((int)(drainPerSecond * 5));
					}
					target.playSound(MainSounds.ABSORB1.get());
					player.playSound(MainSounds.ABSORB1.get());

					if (target.getHealth() <= 1.0f) {
						target.hurtMarked = true;
						data.getStatus().setDrainingTargetId(-1);
						data.getCooldowns().removeCooldown(Cooldowns.DRAIN_ACTIVE);
						player.removeEffect(MainEffects.STUN.get());
						target.kill();
					}
				}

			} else {
				data.getStatus().setDrainingTargetId(-1);
				data.getCooldowns().removeCooldown(Cooldowns.DRAIN_ACTIVE);
				player.removeEffect(MainEffects.STUN.get());
			}

		} else {
			Entity entity = player.level().getEntity(targetId);
			if (entity instanceof LivingEntity target) {
				Vec3 look = player.getLookAngle();
				target.setDeltaMovement(look.scale(1.5).add(0, 0.5, 0));
				target.hurtMarked = true;
				player.setDeltaMovement(look.scale(-1.0).add(0, 0.3, 0));
				player.hurtMarked = true;
				target.playSound(MainSounds.KNOCKBACK_CHARACTER.get());
				player.playSound(MainSounds.KNOCKBACK_CHARACTER.get());
			}
			data.getStatus().setDrainingTargetId(-1);
			player.removeEffect(MainEffects.STUN.get());
		}
	}

	private static void fusionTickHandling(ServerPlayer serverPlayer, StatsData data) {
		if (data.getStatus().isFused()) {
			if (data.getStatus().isFusionLeader()) {
				int timer = data.getStatus().getFusionTimer();
				if (timer > 0) {
					data.getStatus().setFusionTimer(timer - 1);
					if (timer - 1 <= 0) FusionLogic.endFusion(serverPlayer, data, false);
				}
				UUID partnerUUID = data.getStatus().getFusionPartnerUUID();
				ServerPlayer partner = serverPlayer.getServer().getPlayerList().getPlayer(partnerUUID);
				if (partner == null || partner.hasDisconnected()) {
					FusionLogic.endFusion(serverPlayer, data, true);
				} else if (partner.isDeadOrDying()) {
					FusionLogic.endFusion(serverPlayer, data, true);
				} else if (partner.distanceTo(serverPlayer) > 5.0) {
					partner.teleportTo(serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ());
				}
			} else {
				UUID leaderUUID = data.getStatus().getFusionPartnerUUID();
				ServerPlayer leader = serverPlayer.getServer().getPlayerList().getPlayer(leaderUUID);
				if (leader == null || leader.hasDisconnected() || leader.isDeadOrDying()) FusionLogic.endFusion(serverPlayer, data, true);
			}
		}
	}

	private static void handleFlightKiDrain(ServerPlayer player, StatsData data) {
		if (!data.getSkills().isSkillActive("fly")) return;
		if (player.isCreative() || player.isSpectator()) return;

		int flyLevel = data.getSkills().getSkillLevel("fly");
		if (flyLevel >= data.getSkills().getMaxSkillLevel("fly")) return;
		int maxEnergy = data.getMaxEnergy();

		double basePercent = player.isSprinting() ? 0.08 : 0.03;
		double energyCostPercent = Math.max(0.002, basePercent - (flyLevel * 0.001));
		int energyCost = (int) Math.ceil(maxEnergy * energyCostPercent);

		int currentEnergy = data.getResources().getCurrentEnergy();

		if (currentEnergy >= energyCost) {
			data.getResources().removeEnergy(energyCost);
		} else {
			data.getSkills().setSkillActive("fly", false);
			if (!player.isCreative() && !player.isSpectator()) {
				player.getAbilities().mayfly = false;
				player.getAbilities().flying = false;
				player.onUpdateAbilities();
			}
			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
		}
	}

	private static void handleStatusEffects(ServerPlayer player, StatsData data) {
		if (data.getStatus().isChargingKi()) {
			if (!player.hasEffect(MainEffects.KICHARGE.get())) {
				player.addEffect(new MobEffectInstance(MainEffects.KICHARGE.get(), -1, 0, false, false, true));
			}
		} else {
			player.removeEffect(MainEffects.KICHARGE.get());
		}

		if (data.getStatus().isActionCharging()) {
			if (!player.hasEffect(MainEffects.TRANSFORM.get())) {
				player.addEffect(new MobEffectInstance(MainEffects.TRANSFORM.get(), -1, 0, false, false, true));
			}
		} else {
			player.removeEffect(MainEffects.TRANSFORM.get());
		}

		if (data.getSkills().isSkillActive("kaioken")) {
			if (!player.hasEffect(MainEffects.KAIOKEN.get())) {
				player.addEffect(new MobEffectInstance(MainEffects.KAIOKEN.get(), -1, 0, false, false, true));
			}
		} else {
			player.removeEffect(MainEffects.KAIOKEN.get());
		}

		if (data.getSkills().isSkillActive("fly")) {
			if (!player.hasEffect(MainEffects.FLY.get())) {
				player.addEffect(new MobEffectInstance(MainEffects.FLY.get(), -1, 0, false, false, true));
			}
		} else {
			player.removeEffect(MainEffects.FLY.get());
		}

		if (data.getEffects().hasEffect("majin")) {
			if (!player.hasEffect(MainEffects.MAJIN.get())) {
				player.addEffect(new MobEffectInstance(MainEffects.MAJIN.get(), -1, 0, false, false, true));
			}
		} else {
			player.removeEffect(MainEffects.MAJIN.get());
		}

		if (data.getEffects().hasEffect("mightfruit")) {
			if (!player.hasEffect(MainEffects.MIGHTFRUIT.get())) {
				player.addEffect(new MobEffectInstance(MainEffects.MIGHTFRUIT.get(), -1, 0, false, false, true));
			}
		} else {
			player.removeEffect(MainEffects.MIGHTFRUIT.get());
		}
		
		if (!data.getCooldowns().hasCooldown(Cooldowns.DASH_CD)) {
			player.removeEffect(MainEffects.DASH_CD.get());
		}
		
		if (!data.getCooldowns().hasCooldown(Cooldowns.DOUBLEDASH_CD)) {
			player.removeEffect(MainEffects.DOUBLEDASH_CD.get());
		}
	}
}