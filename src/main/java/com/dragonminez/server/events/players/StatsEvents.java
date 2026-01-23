package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainFluids;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.entities.namek.NamekTraderEntity;
import com.dragonminez.common.init.entities.namek.NamekWarriorEntity;
import com.dragonminez.common.init.entities.redribbon.BanditEntity;
import com.dragonminez.common.init.entities.redribbon.RedRibbonSoldierEntity;
import com.dragonminez.common.init.entities.redribbon.RobotEntity;
import com.dragonminez.common.init.entities.sagas.SagaFriezaSoldier01Entity;
import com.dragonminez.common.init.entities.sagas.SagaFriezaSoldier02Entity;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.events.DragonBallsHandler;
import com.dragonminez.server.util.FusionLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StatsEvents {

    public static final UUID DMZ_HEALTH_MODIFIER_UUID = UUID.fromString("b065b873-f4c8-4a0f-aa8c-6e778cd410e0");
    public static final UUID FORM_SPEED_UUID = UUID.fromString("c8c07577-3365-4b1c-9917-26b237da6e08");

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }

        Player player = event.player;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        StatsProvider.get(StatsCapability.INSTANCE, serverPlayer).ifPresent(data -> {
            if (!data.getStatus().hasCreatedCharacter()) return;

            applyHealthBonus(serverPlayer);

            if (data.getResources().getCurrentEnergy() > data.getMaxEnergy())
                data.getResources().setCurrentEnergy(data.getMaxEnergy());
            if (data.getResources().getCurrentStamina() > data.getMaxStamina())
                data.getResources().setCurrentStamina(data.getMaxStamina());
        });
    }

    public static void applyHealthBonus(ServerPlayer serverPlayer) {
        StatsProvider.get(StatsCapability.INSTANCE, serverPlayer).ifPresent(data -> {
            AttributeInstance maxHealthAttr = serverPlayer.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealthAttr == null) return;

            float dmzHealthBonus = data.getMaxHealth();
            AttributeModifier existingModifier = maxHealthAttr.getModifier(DMZ_HEALTH_MODIFIER_UUID);

            if (existingModifier == null || existingModifier.getAmount() != dmzHealthBonus) {
                maxHealthAttr.removeModifier(DMZ_HEALTH_MODIFIER_UUID);

                if (dmzHealthBonus > 0) {
                    AttributeModifier healthModifier = new AttributeModifier(
                            DMZ_HEALTH_MODIFIER_UUID,
                            "DMZ Health Bonus",
                            dmzHealthBonus,
                            AttributeModifier.Operation.ADDITION
                    );
                    maxHealthAttr.addPermanentModifier(healthModifier);
                }

                if (serverPlayer.getHealth() > maxHealthAttr.getValue()) {
                    serverPlayer.setHealth((float) maxHealthAttr.getValue());
                }
            }

            if (!data.hasInitializedHealth()) {
                serverPlayer.setHealth((float) maxHealthAttr.getValue());
                data.setInitializedHealth(true);
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DragonBallsHandler.syncRadar(player.serverLevel());
            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                applyHealthBonus(player);
                player.setHealth(player.getMaxHealth());
                data.getResources().setCurrentEnergy(data.getMaxEnergy());
                data.getResources().setCurrentStamina(data.getMaxStamina());
            });
        }
    }

    private static boolean dropTps(Entity entity) {
        List<Class<?>> listaEnemigos = List.of(
                Monster.class,
                Animal.class,
                Player.class,
                FlyingMob.class,
                Mob.class
        );
        return listaEnemigos.stream().anyMatch(clase -> clase.isInstance(entity));
    }

    private static boolean removeAlignment = false;
    private static boolean addAlignment = false;

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }

        if (event.getEntity() instanceof Player victim) {
            StatsProvider.get(StatsCapability.INSTANCE, attacker).ifPresent(attackerData -> {
                StatsProvider.get(StatsCapability.INSTANCE, victim).ifPresent(victimData -> {
                    if (victimData.getResources().getAlignment() < 50 || !victimData.getStatus().hasCreatedCharacter()) {
                        addAlignment = true;
                    } else {
                        removeAlignment = true;
                    }

                    if (victimData.getStatus().hasCreatedCharacter()) {
                        victimData.getEffects().removeAllEffects();
                        victimData.getStatus().setChargingKi(false);
                        victimData.getStatus().setActionCharging(false);
                        victimData.getCharacter().setActiveForm(null, null);
                    }
                });
            });
        }

        if (event.getEntity() instanceof NamekTraderEntity || event.getEntity() instanceof NamekWarriorEntity || event.getEntity() instanceof Villager) {
            removeAlignment = true;
        }

        if (event.getEntity() instanceof RedRibbonSoldierEntity || event.getEntity() instanceof SagaFriezaSoldier01Entity || event.getEntity() instanceof SagaFriezaSoldier02Entity
                || event.getEntity() instanceof RobotEntity || event.getEntity() instanceof BanditEntity) {
            addAlignment = true;
        }

        StatsProvider.get(StatsCapability.INSTANCE, attacker).ifPresent(data -> {
            if (!data.getStatus().hasCreatedCharacter()) {
                return;
            }

            if (dropTps(event.getEntity())) {
                int tpsHealth = (int) Math.round(event.getEntity().getMaxHealth() * ConfigManager.getServerConfig().getGameplay().getTpHealthRatio());

                data.getResources().addTrainingPoints(tpsHealth);
            }

            if (removeAlignment) {
                data.getResources().removeAlignment(5);
                removeAlignment = false;
            }

            if (addAlignment) {
                data.getResources().addAlignment(2);
                addAlignment = false;
            }
        });
    }

    @SubscribeEvent
    public static void onEntityHit(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }

        StatsProvider.get(StatsCapability.INSTANCE, attacker).ifPresent(data -> {
            if (!data.getStatus().hasCreatedCharacter()) {
                return;
            }

            int baseTps = ConfigManager.getServerConfig().getGameplay().getTpPerHit();
            data.getResources().addTrainingPoints(baseTps);
        });
    }

    private static final double HEAL_PERCENTAGE = 0.08;
    private static final int HEAL_TICKS = 3 * 20;
    private static final Map<Player, Long> lastHealingTime = new WeakHashMap<>();

    @SubscribeEvent
    public static void onLivingTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (player.level().isClientSide || event.phase != TickEvent.Phase.END) return;

        FluidState fluidState = player.level().getFluidState(player.blockPosition());

        if (fluidState.isEmpty()) {
            return;
        }

        if (fluidState.is(MainFluids.SOURCE_HEALING.get()) || fluidState.is(MainFluids.FLOWING_HEALING.get())) {
            long currentTime = player.level().getGameTime();
            long lastHealTime = lastHealingTime.getOrDefault(player, 0L);

            if (currentTime - lastHealTime >= HEAL_TICKS) {
                funcHealingLiquid(player);
                lastHealingTime.put(player, currentTime);
            }
        } else if (fluidState.is(MainFluids.SOURCE_NAMEK.get()) || fluidState.is(MainFluids.FLOWING_NAMEK.get())) {
            funcNamekWater(player);
        }
    }

    private static void funcHealingLiquid(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            StatsProvider.get(StatsCapability.INSTANCE, serverPlayer).ifPresent(data -> {
                float maxHp = data.getMaxHealth();
                float healHp = (int) (maxHp * HEAL_PERCENTAGE);
                int maxKi = data.getMaxEnergy();
                int healKi = (int) (maxKi * HEAL_PERCENTAGE);
                int maxStamina = data.getMaxStamina();
                int healStamina = (int) (maxStamina * HEAL_PERCENTAGE);
                boolean hasCreatedChar = data.getStatus().hasCreatedCharacter();

                if (healHp > maxHp) healHp = maxHp;
                if (healKi > maxKi) healKi = maxKi;
                if (healStamina > maxStamina) healStamina = maxStamina;

                if (hasCreatedChar) {
                    serverPlayer.heal(healHp);
                    data.getResources().setCurrentEnergy(healKi);
                    data.getResources().setCurrentStamina(healStamina);
                }

            });
        }
        if (player.isOnFire()) {
            player.clearFire();
        }
    }

    private static void funcNamekWater(Player player) {
        if (player.isOnFire()) {
            player.clearFire();
        }
    }

    @SubscribeEvent
    public static void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide) return;

        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        String itemId = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();

        float[] regens = ConfigManager.getServerConfig().getGameplay().getFoodRegeneration(itemId);
        if (regens != null && regens.length >= 3) {
            player.startUsingItem(event.getHand());
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = event.getItem();
        String itemId = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        float[] regens = ConfigManager.getServerConfig().getGameplay().getFoodRegeneration(itemId);

        if (regens != null && regens.length >= 3) {
            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                boolean isSenzu = itemId.equals("dragonminez:senzu_bean");
                boolean isHeartMedicine = itemId.equals("dragonminez:heart_medicine");

                if ((isSenzu || isHeartMedicine) && player.getCooldowns().isOnCooldown(stack.getItem())) return;

                float maxHealth = player.getMaxHealth();
                int maxEnergy = data.getMaxEnergy();
                int maxStamina = data.getMaxStamina();

                int currentEnergy = data.getResources().getCurrentEnergy();
                int currentStamina = data.getResources().getCurrentStamina();

                float healAmount = (maxHealth * regens[0]);
                int energyAmount = (int) (maxEnergy * regens[1]);
                int staminaAmount = (int) (maxStamina * regens[2]);

                player.heal(healAmount);

                int newEnergy = Math.min(maxEnergy, currentEnergy + energyAmount);
                int newStamina = Math.min(maxStamina, currentStamina + staminaAmount);

                data.getResources().setCurrentEnergy(newEnergy);
                data.getResources().setCurrentStamina(newStamina);

                if (isSenzu || isHeartMedicine) {
                    int cooldownTicks = ConfigManager.getServerConfig().getGameplay().getSenzuCooldownTicks();
                    player.getCooldowns().addCooldown(stack.getItem(), cooldownTicks);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = event.getItem();
        String itemId = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();

        if (itemId.equals("dragonminez:senzu_bean") || itemId.equals("dragonminez:heart_medicine")) {
            if (player.getCooldowns().isOnCooldown(stack.getItem()) || player.hasEffect(MainEffects.STUN.get()))
                event.setCanceled(true);
            else event.setDuration(1);
        }
    }

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide) return;

        if (event.getEntity().hasEffect(MainEffects.STUN.get())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getLevel().isClientSide) return;
        if (event.getEntity() == null) return;

        if (event.getEntity().hasEffect(MainEffects.STUN.get())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity().level().isClientSide) return;

        if (event.getEntity().hasEffect(MainEffects.STUN.get()))
            event.getEntity().setDeltaMovement(event.getEntity().getDeltaMovement().multiply(1, 0, 1));
    }

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        if (entity.hasEffect(MainEffects.STUN.get())) {
            entity.setDeltaMovement(0, entity.getDeltaMovement().y, 0);
            entity.setJumping(false);
            entity.setSprinting(false);

            if (entity.getPose() != Pose.CROUCHING) entity.setPose(Pose.CROUCHING);

            if (entity instanceof Mob mob) {
                mob.getNavigation().stop();
                mob.setTarget(null);
                mob.setAggressive(false);
            }
        }

        if (entity instanceof ServerPlayer serverPlayer) {
            StatsProvider.get(StatsCapability.INSTANCE, serverPlayer).ifPresent(data -> {

                AttributeInstance speedAttr = serverPlayer.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedAttr != null) {
                    if (speedAttr.getModifier(FORM_SPEED_UUID) != null) speedAttr.removeModifier(FORM_SPEED_UUID);
                    if (data.getCharacter().hasActiveForm()) {
                        FormConfig.FormData activeForm = data.getCharacter().getActiveFormData();
                        if (activeForm != null) {
                            double multiplier = activeForm.getSpeedMultiplier();
                            if (multiplier != 1.0) {
                                double bonus = multiplier - 1.0;
                                speedAttr.addTransientModifier(new AttributeModifier(FORM_SPEED_UUID, "Form Speed Bonus", bonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
                            }
                        }
                    }
                }
            });
        }
    }

	@SubscribeEvent
	public static void onFall(LivingFallEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;

		final int[] jumpLevel = {0};
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (data.getStatus().hasCreatedCharacter()) {
				jumpLevel[0] = data.getSkills().getSkillLevel("jump");
			}
		});

		if (jumpLevel[0] <= 0) return;

		float maxHeight = 1.25f + (jumpLevel[0] * 1.0f);
		float safeHeight = maxHeight + 1.0f;

		float fallDistance = event.getDistance();

		if (fallDistance <= safeHeight) {
			player.resetFallDistance();
			event.setCanceled(true);
		} else {
			float reducedDistance = fallDistance - safeHeight;
			event.setDistance(reducedDistance);
		}
	}

	@SubscribeEvent
	public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
		if (event.getLevel().isClientSide) return;
		if (!(event.getTarget() instanceof ServerPlayer target)) return;

		ServerPlayer source = (ServerPlayer) event.getEntity();

		if (!source.getMainHandItem().isEmpty()) return;

		StatsProvider.get(StatsCapability.INSTANCE, source).ifPresent(sData -> {
			StatsProvider.get(StatsCapability.INSTANCE, target).ifPresent(tData -> {

				if (!tData.getStatus().isBlocking()) return;

				boolean sHasRight = hasPothala(source, "right");
				boolean tHasLeft = hasPothala(target, "left");

				boolean sameColor = checkPothalaColorMatch(source, target);

				if (sHasRight && tHasLeft && sameColor) {
					FusionLogic.executePothala(source, target, sData, tData);
					event.setCanceled(true);
				}
			});
		});
	}

	private static boolean hasPothala(ServerPlayer player, String side) {
		ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
		if (side.equals("left") && (head.getItem() == MainItems.POTHALA_LEFT.get() || head.getItem() == MainItems.GREEN_POTHALA_LEFT.get())) {
			return true;
		} else return side.equals("right") && (head.getItem() == MainItems.POTHALA_RIGHT.get() || head.getItem() == MainItems.GREEN_POTHALA_RIGHT.get());
	}

	private static boolean checkPothalaColorMatch(ServerPlayer p1, ServerPlayer p2) {
		if (hasPothala(p1, "right") && hasPothala(p2, "left")) {
			ItemStack p1Head = p1.getItemBySlot(EquipmentSlot.HEAD);
			ItemStack p2Head = p2.getItemBySlot(EquipmentSlot.HEAD);

			boolean p1IsGreen = (p1Head.getItem() == MainItems.GREEN_POTHALA_RIGHT.get());
			boolean p2IsGreen = (p2Head.getItem() == MainItems.GREEN_POTHALA_LEFT.get());

			return p1IsGreen == p2IsGreen;
		}
		return false;
	}

    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Player player)) return;

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            float configScale = 0.9375f;

            if (data.getCharacter().hasActiveForm()) {
                FormConfig.FormData activeForm = data.getCharacter().getActiveFormData();
                if (activeForm != null) {
                    configScale *= activeForm.getModelScaling();
                }
            } else {
                configScale = (float) data.getCharacter().getModelScaling();
            }

            if (Math.abs(configScale - 0.9375f) > 0.001F) {
                float ratio = configScale / 0.9375f;
                EntityDimensions newDims = EntityDimensions.scalable(0.6F * ratio, 1.8F * ratio);
                event.setNewSize(newDims);
                float defaultEyeHeight = player.getEyeHeight(Pose.STANDING);
                float currentBaseEye = event.getNewEyeHeight() > 0 ? event.getNewEyeHeight() : defaultEyeHeight;
                event.setNewEyeHeight(currentBaseEye * ratio);
            }
        });
    }

}
