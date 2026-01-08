package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.init.MainFluids;
import com.dragonminez.common.init.entities.namek.NamekTraderEntity;
import com.dragonminez.common.init.entities.namek.NamekWarriorEntity;
import com.dragonminez.common.init.entities.redribbon.BanditEntity;
import com.dragonminez.common.init.entities.redribbon.RedRibbonSoldierEntity;
import com.dragonminez.common.init.entities.redribbon.RobotEntity;
import com.dragonminez.common.init.entities.sagas.SagaFriezaSoldier01Entity;
import com.dragonminez.common.init.entities.sagas.SagaFriezaSoldier02Entity;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
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

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }

        Player player = event.player;
		if (!(player instanceof ServerPlayer serverPlayer)) return;

        StatsProvider.get(StatsCapability.INSTANCE, serverPlayer).ifPresent(data -> {
            if (!data.getStatus().hasCreatedCharacter()) return;

			// Max Health Handling
            AttributeInstance dmzHealthAttr = serverPlayer.getAttribute(MainAttributes.DMZ_HEALTH.get());
            AttributeInstance maxHealthAttr = serverPlayer.getAttribute(Attributes.MAX_HEALTH);

            if (dmzHealthAttr != null && maxHealthAttr != null) {
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
            }

			if (data.getResources().getCurrentEnergy() > data.getMaxEnergy()) data.getResources().setCurrentEnergy(data.getMaxEnergy());
			if (data.getResources().getCurrentStamina() > data.getMaxStamina()) data.getResources().setCurrentStamina(data.getMaxStamina());
        });
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
						victimData.getStatus().setTransforming(false);
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

                float maxHealth = data.getMaxHealth();
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
            if (player.getCooldowns().isOnCooldown(stack.getItem())) event.setCanceled(true);
            else event.setDuration(1);
        }
    }

	@SubscribeEvent
	public static void onPlayerAttack(AttackEntityEvent event) {
		if (event.getEntity().level().isClientSide) return;

		StatsProvider.get(StatsCapability.INSTANCE, event.getEntity()).ifPresent(data -> {
			if (data.getStatus().isStunned()) event.setCanceled(true);
		});
	}

	@SubscribeEvent
	public static void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getLevel().isClientSide) return;
		if (event.getEntity() == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, event.getEntity()).ifPresent(data -> {
			if (data.getStatus().isStunned()) event.setCanceled(true);
		});
	}

	@SubscribeEvent
	public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
		if (event.getEntity().level().isClientSide) return;

		StatsProvider.get(StatsCapability.INSTANCE, event.getEntity()).ifPresent(data -> {
			if (data.getStatus().isStunned()) {
				event.getEntity().setDeltaMovement(event.getEntity().getDeltaMovement().multiply(1, 0, 1));
				event.getEntity().setJumping(false);
			}
		});
	}

	@SubscribeEvent
	public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
		if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (data.getStatus().isStunned()) {
				if (player.isSprinting()) player.setSprinting(false);
				if (player.isCrouching()) player.setShiftKeyDown(false);
				if (player.isFallFlying()) player.stopFallFlying();
			}
		});
	}
}
