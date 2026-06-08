package com.dragonminez.server.events.players;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainEnchants;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.ki.*;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.AppearanceSyncS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TechniqueChargeSyncS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.dragonminez.common.stats.techniques.TechniqueDispatcher;
import com.dragonminez.common.stats.techniques.Techniques;
import com.dragonminez.common.util.TransformationItemCostHelper;
import com.dragonminez.server.events.players.actionmode.FormModeHandler;
import com.dragonminez.server.events.players.actionmode.FusionModeHandler;
import com.dragonminez.server.events.players.actionmode.RacialModeHandler;
import com.dragonminez.server.events.players.actionmode.StackFormModeHandler;
import com.dragonminez.server.events.players.statuseffect.*;
import com.dragonminez.server.util.GravityLogic;
import com.dragonminez.server.util.PotionEffectHelper;
import com.dragonminez.server.world.dimension.OtherworldDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.*;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TickHandler {
	private static final Env LOG_ENV = Env.SERVER;
	private static final Map<String, IActionModeHandler> ACTION_MODE_HANDLERS = new HashMap<>();
	private static final List<IStatusEffectHandler> STATUS_EFFECT_HANDLERS = new ArrayList<>();
	private static final Map<UUID, AbstractKiProjectile> CHARGING_CACHE = new HashMap<>();
	private static final Map<UUID, Float> CHARGE_COST_ACCUM = new HashMap<>();

	private static final int REGEN_INTERVAL = 20;
	private static final int SYNC_INTERVAL = 10;
	private static final int FORCED_KILL_GRACE_TICKS = 40;
	private static final int AURA_LIGHT_INTERVAL = 2;
	private static final int AURA_LIGHT_LEVEL = 12;
	private static final int AURA_LIGHT_STEP = 1;
	private static final double MEDITATION_BONUS_PER_LEVEL = 0.05;
	private static final Map<UUID, Integer> masterySecondsByPlayer = new HashMap<>();
	private static final Map<UUID, Integer> chargeTicksByPlayer = new HashMap<>();
	private static final Map<UUID, Integer> playerTickCounters = new HashMap<>();
	private static final Map<UUID, BlockPos> auraLightPositions = new HashMap<>();
	private static final Map<UUID, Integer> auraLightLevels = new HashMap<>();
	private static final Map<UUID, Integer> forceKillGraceByPlayer = new HashMap<>();

	static {
		registerActionModeHandlers();
		registerStatusEffectHandlers();
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
		if (!(event.player instanceof ServerPlayer serverPlayer)) return;

		UUID playerId = serverPlayer.getUUID();
		int graceTicks = forceKillGraceByPlayer.getOrDefault(playerId, 0);
		if (graceTicks > 0) forceKillGraceByPlayer.put(playerId, graceTicks - 1);

		int tickCounter = playerTickCounters.getOrDefault(playerId, 0) + 1;
		if (tickCounter >= REGEN_INTERVAL) playerTickCounters.put(playerId, 0);
		else playerTickCounters.put(playerId, tickCounter);

		if (shouldForceKillForInvalidHealth(serverPlayer, playerId)) {
			serverPlayer.kill();
			forceKillGraceByPlayer.put(playerId, FORCED_KILL_GRACE_TICKS);
			return;
		}

		StatsProvider.get(StatsCapability.INSTANCE, serverPlayer).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;

			if (serverPlayer.tickCount % AURA_LIGHT_INTERVAL == 0) updateAuraLight(serverPlayer, data);

			if (serverPlayer.hasEffect(MainEffects.STUN.get())) {
				data.getStatus().setChargingKi(false);
				data.getStatus().setActionCharging(false);
				data.getTechniques().clearTechniqueCharge();
				data.getResources().setActionCharge(0);
				if (!data.getStatus().isStunned()) data.getStatus().setStunned(true);

				data.getCooldowns().tick();
				data.getEffects().tick();
				clearExpiredKnockdown(data);

				for (IStatusEffectHandler handler : STATUS_EFFECT_HANDLERS) {
					handler.onPlayerTick(serverPlayer, data);
					handler.handleStatusEffects(serverPlayer, data);
				}
				if (tickCounter % 20 == 0) for (IStatusEffectHandler handler : STATUS_EFFECT_HANDLERS)
					handler.onPlayerSecond(serverPlayer, data);
				if (serverPlayer.tickCount % SYNC_INTERVAL == 0)
					NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(serverPlayer), serverPlayer);
				return;
			} else {
				data.getCooldowns().tick();
				data.getEffects().tick();
				clearExpiredKnockdown(data);
				if (data.getStatus().isStunned()) data.getStatus().setStunned(false);
			}

			handleTechniqueCharge(serverPlayer, data);

			boolean shouldRegen = tickCounter >= REGEN_INTERVAL && !serverPlayer.isDeadOrDying();
			boolean shouldSync = tickCounter % SYNC_INTERVAL == 0;
			boolean isChargingKi = data.getStatus().isChargingKi();
			boolean isDescending = data.getStatus().isDescending();
			int meditationLevel = data.getSkills().getSkillLevel("meditation");

			double currentRegenMod = 1.0;
			boolean isGuardBroken = data.getStatus().isStunned() && data.getResources().getCurrentPoise() <= 0;
			boolean isFastFly = data.getSkills().isSkillActive("fly") && serverPlayer.isSprinting();
			boolean isBlocking = data.getStatus().isBlocking();
			boolean isAttacking = serverPlayer.swingTime > 0;
			boolean isStill = serverPlayer.getDeltaMovement().lengthSqr() < 0.001 && serverPlayer.onGround();
			boolean isWalk = !serverPlayer.isSprinting() && serverPlayer.onGround() && serverPlayer.getDeltaMovement().lengthSqr() >= 0.001;

			if (isFastFly) currentRegenMod = 0.0;
			else if (isGuardBroken) currentRegenMod = 4.0;
			else if (isBlocking || isAttacking) currentRegenMod = 0.5;
			else if (isStill) currentRegenMod = 2.0;
			else if (isWalk) currentRegenMod = 1.5;

			double savedMod = serverPlayer.getPersistentData().getDouble("dmz_stamina_regen_mod");
			long modTimestamp = serverPlayer.getPersistentData().getLong("dmz_stamina_mod_time");
			long now = System.currentTimeMillis();

			if (currentRegenMod != savedMod) {
				if (currentRegenMod < savedMod || (now - modTimestamp) >= 1000) {
					serverPlayer.getPersistentData().putDouble("dmz_stamina_regen_mod", currentRegenMod);
					serverPlayer.getPersistentData().putLong("dmz_stamina_mod_time", now);
				}
			} else serverPlayer.getPersistentData().putLong("dmz_stamina_mod_time", now);

			if (shouldRegen) {
				double meditationBonus = meditationLevel > 0 ? 1.0 + (meditationLevel * MEDITATION_BONUS_PER_LEVEL) : 1.0;
				boolean activeCharging = isChargingKi && !isDescending;
				double foodRegenMod = getFoodRegenMultiplier(serverPlayer);

				regenerateHealth(serverPlayer, data, foodRegenMod);
				regenerateEnergy(serverPlayer, data, activeCharging, foodRegenMod);
				regenerateStamina(serverPlayer, data, foodRegenMod);
				regeneratePoise(data, meditationBonus);

				playerTickCounters.put(playerId, 0);
			} else {
				playerTickCounters.put(playerId, tickCounter);
			}

			boolean isMovementRestricted = TechniqueDispatcher.isMovementRestrictedKiAttack(serverPlayer, data);
			boolean isFiring = TechniqueDispatcher.isFiringKiAttack(serverPlayer);
			boolean wasExecuting = serverPlayer.getPersistentData().getBoolean("dmz_was_executing_ki");

			if (isMovementRestricted) {
				serverPlayer.setDeltaMovement(0, serverPlayer.getDeltaMovement().y < 0 ? serverPlayer.getDeltaMovement().y : 0, 0);
				serverPlayer.hasImpulse = true;
				serverPlayer.setJumping(false);
				serverPlayer.setSprinting(false);

				if (serverPlayer.getPose() != Pose.STANDING) serverPlayer.setPose(Pose.STANDING);

				if (isFiring) {
					serverPlayer.setYRot(serverPlayer.yRotO);
					serverPlayer.setXRot(serverPlayer.xRotO);
					serverPlayer.yHeadRot = serverPlayer.yHeadRotO;
					serverPlayer.yBodyRot = serverPlayer.yBodyRotO;
				}

				serverPlayer.getPersistentData().putBoolean("dmz_was_executing_ki", true);
			} else if (wasExecuting) {
				serverPlayer.getPersistentData().putBoolean("dmz_was_executing_ki", false);
			}

			boolean kiAnimShouldBeActive = playerOwnsKiProjectile(serverPlayer) || data.getTechniques().isTechniqueCharging() || data.getTechniques().isTechniqueChargeActive();
			boolean kiAnimWasActive = serverPlayer.getPersistentData().getBoolean("dmz_ki_anim_active");
			if (kiAnimShouldBeActive) serverPlayer.getPersistentData().putBoolean("dmz_ki_anim_active", true);
			else if (kiAnimWasActive) {
				serverPlayer.getPersistentData().putBoolean("dmz_ki_anim_active", false);
				NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(serverPlayer.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP, 0, -1, ""), serverPlayer);
			}

			// Holding the action key sets isActionCharging client-side without validating
			// the action. Clear it here when the selected action is not actually possible
			// so the aura, the transformation animation and the movement lock (all keyed on
			// isActionCharging) never trigger on an impossible action.
			if (data.getStatus().isActionCharging() && !canChargeSelectedAction(serverPlayer, data)) {
				data.getStatus().setActionCharging(false);
				if (data.getResources().getActionCharge() > 0) data.getResources().setActionCharge(0);
			}

			boolean isReleaseCharging = isChargingKi || data.getStatus().isActionCharging();
			if (isReleaseCharging) {
				int chargeTicks = chargeTicksByPlayer.getOrDefault(playerId, 0) + 1;
				chargeTicksByPlayer.put(playerId, chargeTicks);
				if (chargeTicks % 2 == 0) chargePowerRelease(data, chargeTicks, isChargingKi && isDescending);
			} else if (chargeTicksByPlayer.containsKey(playerId)) chargeTicksByPlayer.remove(playerId);

			boolean auraFromActions = isChargingKi || (data.getStatus().isActionCharging() && (data.getStatus().getSelectedAction() == ActionMode.FORM || data.getStatus().getSelectedAction() == ActionMode.STACK));
			boolean auraFromFlySprint = data.getSkills().isSkillActive("fly") && serverPlayer.isSprinting() && serverPlayer.getDeltaMovement().length() > 0.65F;
			data.getStatus().setAuraActive(auraFromActions || auraFromFlySprint);

			if (tickCounter % 5 == 0) {
				boolean hasYajirobe = serverPlayer.getInventory().hasAnyOf(Set.of(MainItems.KATANA_YAJIROBE.get()));
				boolean holdingYajirobe = serverPlayer.getMainHandItem().getItem() == MainItems.KATANA_YAJIROBE.get() || serverPlayer.getOffhandItem().getItem() == MainItems.KATANA_YAJIROBE.get();
				boolean renderKatanaTarget = hasYajirobe && !holdingYajirobe;

				boolean playedSound = false;

				if (data.getStatus().isRenderKatana() != renderKatanaTarget) {
					if (renderKatanaTarget) {
						serverPlayer.level().playSound(null, serverPlayer.blockPosition(), MainSounds.SWORD_IN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
						playedSound = true;
					} else if (holdingYajirobe) {
						serverPlayer.level().playSound(null, serverPlayer.blockPosition(), MainSounds.SWORD_OUT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
						playedSound = true;
					}
					data.getStatus().setRenderKatana(renderKatanaTarget);
				}

				ItemStack backItem = ItemStack.EMPTY;
				boolean holdingOtherWeapon = false;
				for (int i = 0; i < serverPlayer.getInventory().getContainerSize(); i++) {
					ItemStack stack = serverPlayer.getInventory().getItem(i);
					if (stack.isEmpty()) continue;
					Item item = stack.getItem();

					if (item == MainItems.Z_SWORD.get() || item == MainItems.BRAVE_SWORD.get() || item == MainItems.POWER_POLE.get()) {
						boolean isHeld = serverPlayer.getMainHandItem().getItem() == item || serverPlayer.getOffhandItem().getItem() == item;
						if (isHeld) holdingOtherWeapon = true;
						else if (backItem == ItemStack.EMPTY) backItem = item.getDefaultInstance();
					}
				}

				String newBackWeapon = backItem != ItemStack.EMPTY ? backItem.getDescriptionId() : "";
				String currentBackWeapon = data.getStatus().getBackWeapon();

				if (!currentBackWeapon.equals(newBackWeapon)) {
					if (!playedSound) {
						if (!newBackWeapon.isEmpty()) {
							serverPlayer.level().playSound(null, serverPlayer.blockPosition(), MainSounds.SWORD_IN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
						} else if (holdingOtherWeapon) {
							serverPlayer.level().playSound(null, serverPlayer.blockPosition(), MainSounds.SWORD_OUT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
						}
					}
					data.getStatus().setBackWeapon(newBackWeapon);
				}

				ItemStack headTechStack = CuriosApi.getCuriosInventory(serverPlayer)
						.map(inv -> inv.getCurios().get("head_tech"))
						.map(stacksHandler -> stacksHandler.getStacks().getStackInSlot(0))
						.orElse(ItemStack.EMPTY);
				String itemId = headTechStack.getDescriptionId();

				boolean hasScouter = itemId.contains("scouter");
				if (hasScouter) {
					if (!data.getStatus().getScouterItem().equals(itemId)) data.getStatus().setScouterItem(itemId);
				} else if (!data.getStatus().getScouterItem().isEmpty()) data.getStatus().setScouterItem("");

				boolean hasPothala = itemId.contains("pothala");
				if (hasPothala) {
					boolean isGreenPothala = itemId.contains("green");
					data.getStatus().setPothalaColor(isGreenPothala ? "green" : "yellow");
				} else if (!data.getStatus().getPothalaColor().isEmpty()) data.getStatus().setPothalaColor("");
			}

			for (IStatusEffectHandler handler : STATUS_EFFECT_HANDLERS) {
				handler.onPlayerTick(serverPlayer, data);
				handler.handleStatusEffects(serverPlayer, data);
			}

			if (tickCounter % 20 == 0) {
				handleActionCharge(serverPlayer, data);
				handleActiveFormDrains(serverPlayer, data);
				GravityLogic.tick(serverPlayer);
				if (ConfigManager.getServerConfig().getWorldGen().getOtherworldActive()) {
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

			if (shouldSync) NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(serverPlayer), serverPlayer);
		});

	}

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        serverPlayer.getPersistentData().putBoolean("dmz_was_executing_ki", false);
        CHARGING_CACHE.remove(serverPlayer.getUUID());

        StatsProvider.get(StatsCapability.INSTANCE, serverPlayer).ifPresent(data -> {
            data.getStatus().setChargingKi(false);
            data.getStatus().setActionCharging(false);
            data.getResources().setActionCharge(0);

            data.getTechniques().clearTechniqueCharge();
            data.getTechniques().setTechniqueChargePercent(0.0f);

            NetworkHandler.sendToTrackingEntityAndSelf(new TechniqueChargeSyncS2C(serverPlayer.getId(), 0.0f, false), serverPlayer);
        });

        NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(serverPlayer.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP, 0, -1, ""), serverPlayer);
    }

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		UUID playerId = event.getEntity().getUUID();
		if (event.getEntity() instanceof ServerPlayer serverPlayer) {
			serverPlayer.getPersistentData().putBoolean("dmz_was_executing_ki", false);
			NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(serverPlayer.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP, 0, -1, ""), serverPlayer);
		}
		CHARGING_CACHE.remove(playerId);
		CHARGE_COST_ACCUM.remove(playerId);
		chargeTicksByPlayer.remove(playerId);
		masterySecondsByPlayer.remove(playerId);
		playerTickCounters.remove(playerId);
		forceKillGraceByPlayer.remove(playerId);
		auraLightLevels.remove(playerId);
		if (event.getEntity() instanceof ServerPlayer serverPlayer) {
			removeAuraLight(serverPlayer.serverLevel(), playerId);
			clearHumanKiAccumulators(serverPlayer);
		}
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		UUID playerId = event.getEntity().getUUID();
		forceKillGraceByPlayer.put(playerId, FORCED_KILL_GRACE_TICKS);
		playerTickCounters.remove(playerId);
	}

	private static void clearExpiredKnockdown(StatsData data) {
		if (data.getStatus().isKnockedDown() && !data.getCooldowns().hasCooldown(Cooldowns.KNOCKDOWN_DURATION)) {
			data.getStatus().setKnockedDown(false);
		}
	}

	private static boolean shouldForceKillForInvalidHealth(ServerPlayer serverPlayer, UUID playerId) {
		if (!serverPlayer.isAlive() || serverPlayer.isDeadOrDying() || serverPlayer.deathTime > 0) return false;

		float health = serverPlayer.getHealth();
		if (Float.isNaN(health) || Float.isInfinite(health)) return true;

		if (forceKillGraceByPlayer.getOrDefault(playerId, 0) > 0) return false;
		return health <= 0.0F;
	}

	public static void registerForceKillGrace(UUID playerId) {
		forceKillGraceByPlayer.put(playerId, FORCED_KILL_GRACE_TICKS);
	}


	private static void updateAuraLight(ServerPlayer player, StatsData data) {
		boolean auraActive = data.getStatus().isAuraActive() || data.getStatus().isPermanentAura();
		ServerLevel level = player.serverLevel();
		UUID playerId = player.getUUID();
		int currentLevel = auraLightLevels.getOrDefault(playerId, 0);
		int targetLevel = auraActive ? AURA_LIGHT_LEVEL : 0;
		int nextLevel = approach(currentLevel, targetLevel, AURA_LIGHT_STEP);

		if (nextLevel <= 0) {
			auraLightLevels.remove(playerId);
			removeAuraLight(level, playerId);
			return;
		}
		auraLightLevels.put(playerId, nextLevel);

		BlockPos targetPos = player.blockPosition().above();
		if (!canHostAuraLight(level, targetPos)) {
			targetPos = player.blockPosition();
			if (!canHostAuraLight(level, targetPos)) {
				removeAuraLight(level, playerId);
				return;
			}
		}

		BlockPos previousPos = auraLightPositions.get(playerId);
		if (previousPos != null && !previousPos.equals(targetPos)) {
			clearAuraLightIfOwned(level, previousPos);
		}

		BlockState currentState = level.getBlockState(targetPos);
		if (!isAuraLight(currentState) || currentState.getValue(LightBlock.LEVEL) != nextLevel) {
			BlockState lightState = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, nextLevel);
			level.setBlock(targetPos, lightState, 3);
		}

		auraLightPositions.put(playerId, targetPos.immutable());
	}

	private static boolean canHostAuraLight(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.isAir() || isAuraLight(state);
	}

	private static boolean isAuraLight(BlockState state) {
		return state.is(Blocks.LIGHT);
	}

	private static void removeAuraLight(ServerLevel level, UUID playerId) {
		BlockPos previousPos = auraLightPositions.remove(playerId);
		if (previousPos != null) {
			clearAuraLightIfOwned(level, previousPos);
		}
	}

	private static void clearAuraLightIfOwned(ServerLevel level, BlockPos pos) {
		BlockState currentState = level.getBlockState(pos);
		if (isAuraLight(currentState) && currentState.getValue(LightBlock.LEVEL) <= AURA_LIGHT_LEVEL) {
			level.removeBlock(pos, false);
		}
	}

	private static int approach(int current, int target, int step) {
		if (current < target) return Math.min(target, current + step);
		if (current > target) return Math.max(target, current - step);
		return current;
	}

	private static double getFoodRegenMultiplier(ServerPlayer player) {
		int drumsticks = player.getFoodData().getFoodLevel() / 2;
		if (drumsticks >= 9) return 1.0;
		if (drumsticks <= 3) return 0.0;
		return 1.0 - ((9 - drumsticks) * 0.10);
	}

	private static void regenerateHealth(ServerPlayer player, StatsData data, double foodRegenMod) {
		float currentHealth = player.getHealth();
		if (!Float.isFinite(currentHealth)) {
			player.setHealth(1.0f);
			return;
		}
		if (foodRegenMod <= 0.0) return;

		float maxHealth = player.getMaxHealth();
		if (currentHealth >= maxHealth) return;

		DMZEvent.HealthRegenEvent event = new DMZEvent.HealthRegenEvent(player, data, data.getHealthRegenPerSecond());
		if (MinecraftForge.EVENT_BUS.post(event)) return;

		double finalRegen = Math.max(0.0, event.getAmount()) * foodRegenMod;
		if (!Double.isFinite(finalRegen) || finalRegen <= 0.0) return;

		player.setHealth((float) Math.min(maxHealth, currentHealth + finalRegen));
	}

	private static void regenerateEnergy(ServerPlayer player, StatsData data, boolean activeCharging, double foodRegenMod) {
		float currentEnergy = data.getResources().getCurrentEnergy();
		float maxEnergy = data.getMaxEnergy();

		boolean hasActiveForm = data.getCharacter().hasActiveForm();
		FormConfig.FormData activeForm = hasActiveForm ? data.getCharacter().getActiveFormData() : null;
		boolean hasActiveStackForm = data.getCharacter().hasActiveStackForm();
		FormConfig.FormData activeStackForm = hasActiveStackForm ? data.getCharacter().getActiveStackFormData() : null;

		double energyChange = data.getEnergyRegenPerSecond(activeCharging);

		if (activeCharging) {
			DMZEvent.KiChargeEvent kiEvent = new DMZEvent.KiChargeEvent(player, currentEnergy, maxEnergy);
			if (MinecraftForge.EVENT_BUS.post(kiEvent)) energyChange = 0;
		}

		UUID masteryPlayerId = player.getUUID();
		int masterySeconds = masterySecondsByPlayer.getOrDefault(masteryPlayerId, 0);
		if (masterySeconds < 5) masterySecondsByPlayer.put(masteryPlayerId, masterySeconds + 1);
		else {
			masterySecondsByPlayer.put(masteryPlayerId, 0);

			if (hasActiveForm && activeForm != null) {
				String activeFormName = activeForm.getName().toLowerCase();
				String activeFormGroup = data.getCharacter().getActiveFormGroup();

				double maxMastery = 100.0;
				FormConfig.FormData formData = ConfigManager.getForm(data.getCharacter().getRaceName(), activeFormGroup, activeFormName);
				if (formData != null) maxMastery = formData.getMaxMastery();

				if (!data.getCharacter().getFormMasteries().hasMaxMastery(activeFormGroup, activeFormName, maxMastery)) {
					double masteryGain = formData != null ? formData.getPassiveMasteryEveryFiveSeconds() : 0.001;
					masteryGain = PotionEffectHelper.applyMasteryGainMultiplier(player, masteryGain);
					data.getCharacter().getFormMasteries().addMastery(activeFormGroup, activeFormName, masteryGain, maxMastery);
					NetworkHandler.sendToTrackingEntityAndSelf(new AppearanceSyncS2C(player), player);
				}
			}

			if (hasActiveStackForm && activeStackForm != null) {
				String activeFormName = activeStackForm.getName().toLowerCase();
				String activeFormGroup = data.getCharacter().getActiveStackFormGroup();

				double maxMastery = 100.0;
				FormConfig.FormData formData = ConfigManager.getStackForm(activeFormGroup, activeFormName);
				if (formData != null) maxMastery = formData.getMaxMastery();

				if (!data.getCharacter().getStackFormMasteries().hasMaxMastery(activeFormGroup, activeFormName, maxMastery)) {
					double masteryGain = formData != null ? formData.getPassiveMasteryEveryFiveSeconds() : 0.001;
					masteryGain = PotionEffectHelper.applyMasteryGainMultiplier(player, masteryGain);
					data.getCharacter().getStackFormMasteries().addMastery(activeFormGroup, activeFormName, masteryGain, maxMastery);
					NetworkHandler.sendToTrackingEntityAndSelf(new AppearanceSyncS2C(player), player);
				}
			}
		}

		if (energyChange > 0) energyChange *= foodRegenMod;

		if (energyChange != 0) {
			DMZEvent.EnergyRegenEvent regenEvent = new DMZEvent.EnergyRegenEvent(player, data, energyChange);
			energyChange = MinecraftForge.EVENT_BUS.post(regenEvent) ? 0 : regenEvent.getAmount();
		}

		if (energyChange != 0) {
			float effectiveMaxEnergy = maxEnergy;
			if (data.getStatus().hasActiveShadowDummy()) {
				int pct = data.getStatus().getShadowDummyPercent();
				effectiveMaxEnergy = maxEnergy * (1.0f - pct / 100.0f);
			}
			int newEnergy = (int) Math.max(0, Math.min(effectiveMaxEnergy, currentEnergy + Math.ceil(energyChange)));
			data.getResources().setCurrentEnergy(newEnergy);

			if (newEnergy <= maxEnergy * 0.05 && !data.getStatus().isAndroidUpgraded() && (hasActiveForm || hasActiveStackForm)) {
				data.getCharacter().clearActiveForm(player);
				data.getCharacter().clearActiveStackForm(player);
				data.getResources().setPowerRelease(0);
				data.getResources().setActionCharge(0);
				player.refreshDimensions();
			}
		}
	}

	private static void regenerateStamina(ServerPlayer player, StatsData data, double foodRegenMod) {
		if (foodRegenMod <= 0.0) return;
		if (data.getCooldowns().hasCooldown(Cooldowns.STAMINA_PAUSE)) return;

		float currentStamina = data.getResources().getCurrentStamina();
		float maxStamina = data.getMaxStamina();
		if (currentStamina >= maxStamina) return;

		double regenPerSecond = data.getStaminaRegenPerSecond();
		if (regenPerSecond <= 0.0) return;

		DMZEvent.StaminaRegenEvent event = new DMZEvent.StaminaRegenEvent(player, data, regenPerSecond);
		if (MinecraftForge.EVENT_BUS.post(event)) return;
		regenPerSecond = Math.max(0.0, event.getAmount()) * foodRegenMod;
		if (regenPerSecond <= 0.0) return;

		float effectiveMaxStamina = maxStamina;
		if (data.getStatus().hasActiveShadowDummy()) {
			int pct = data.getStatus().getShadowDummyPercent();
			effectiveMaxStamina = maxStamina * (1.0f - pct / 100.0f);
		}
		float newStamina = (float) Math.min(effectiveMaxStamina, currentStamina + Math.ceil(regenPerSecond));
		data.getResources().setCurrentStamina(newStamina);
	}

	private static void regeneratePoise(StatsData data, double meditationBonus) {
		if (data.getCooldowns().hasCooldown(Cooldowns.POISE_CD) || data.getStatus().isBlocking() || data.getStatus().isStunned())
			return;

		float currentPoise = data.getResources().getCurrentPoise();
		float maxPoise = data.getMaxPoise();

		if (currentPoise < maxPoise) {
			double baseRegen = 0.1;

			int totalEnchLvl = getTotalArmorEnchantmentLevel(MainEnchants.RESISTANCE_RECOVERY.get(), data.getPlayer());
			double enchMult = getRecoveryMultiplier(totalEnchLvl);

			double regenAmount = maxPoise * baseRegen * meditationBonus * enchMult;
			if (regenAmount < 1.0) regenAmount = 1.0;
			data.getResources().addPoise((float) regenAmount);
		}
	}

	public static int getTotalArmorEnchantmentLevel(Enchantment enchantment, LivingEntity entity) {
		int totalLevel = 0;
		for (ItemStack stack : entity.getArmorSlots()) {
			totalLevel += EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
		}
		return totalLevel;
	}

	public static double getRecoveryMultiplier(int totalLevel) {
		double bonus = 0.0;
		if (totalLevel > 0) bonus += Math.min(totalLevel, 4) * 0.0625;
		if (totalLevel > 4) bonus += Math.min(totalLevel - 4, 4) * 0.03125;
		if (totalLevel > 8) bonus += Math.min(totalLevel - 8, 4) * 0.015625;
		if (totalLevel > 12) bonus += Math.min(totalLevel - 12, 4) * 0.0078125;

		return 1.0 + bonus;
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
		if (handler != null) increment += handler.handleActionCharge(player, data);

		if (increment > 0) {
			if (!(mode == ActionMode.FUSION && currentRelease >= 100)) currentRelease += increment;
			if (currentRelease >= 100) {
				currentRelease = 100;
				execute = true;
			}
			data.getResources().setActionCharge(currentRelease);
		}

		if (execute) {
			boolean success = performAction(player, data, mode);
			if (success) {
				data.getResources().setActionCharge(0);
			}
		}
	}

	private static void chargePowerRelease(StatsData data, int chargeTicks, boolean descending) {
		int currentRelease = data.getResources().getPowerRelease();
		int potentialUnlockLevel = data.getSkills().getSkillLevel("potentialunlock");
		int maxRelease = 50 + (potentialUnlockLevel * 5);

		int effectiveLevel = Math.min(10, potentialUnlockLevel);

		float temporalMultiplier = (float) Math.pow(chargeTicks / 50.0f, 2);
		float levelMultiplier = 1.0f + (effectiveLevel * 0.1f);

		int step = (int) Math.min(10, Math.max(1, temporalMultiplier * levelMultiplier));

		if (!descending && currentRelease < maxRelease) {
			data.getResources().setPowerRelease(Math.min(maxRelease, currentRelease + step));
		} else if (descending && currentRelease > 0) {
			data.getResources().setPowerRelease(Math.max(0, currentRelease - step));
		}
	}

	private static void handleTechniqueCharge(ServerPlayer player, StatsData data) {
		Techniques techniques = data.getTechniques();
		String chargingTechniqueId = techniques.getChargingTechniqueId();

		if (chargingTechniqueId == null || chargingTechniqueId.isEmpty()) {
			boolean hadCharge = techniques.getTechniqueChargePercent() > 0.0f || techniques.isTechniqueCharging();
			if (hadCharge || CHARGING_CACHE.containsKey(player.getUUID())) {
				var leftover = findChargingEntity(player);
				if (leftover != null) leftover.discard();
				CHARGING_CACHE.remove(player.getUUID());
				CHARGE_COST_ACCUM.remove(player.getUUID());
				clearHumanKiAccumulators(player);
			}
			if (hadCharge) techniques.clearTechniqueCharge();
			return;
		}

		TechniqueData techniqueData = techniques.getUnlockedTechniques().get(chargingTechniqueId);
		if (!(techniqueData instanceof KiAttackData kiAttack)) {
			techniques.clearTechniqueCharge();
			return;
		}

		String cooldownKey = getTechniqueCooldownKey(chargingTechniqueId);
		if (data.getCooldowns().hasCooldown(cooldownKey)) {
			techniques.clearTechniqueCharge();
			return;
		}

		if (!techniques.isTechniqueCharging()) return;

		var activeKi = findChargingEntity(player);
		if (activeKi != null && !chargingTechniqueId.equals(activeKi.getTechniqueId())) {
			activeKi.discard();
			CHARGING_CACHE.remove(player.getUUID());
			activeKi = null;
		}
		if (activeKi == null && techniques.getTechniqueChargePercent() == 0.0f) {
			TechniqueDispatcher.executeKiAttack(player, player.level(), kiAttack, data, 0.01f);
		}

		final float OVER = KiAttackData.OVERCHARGE_MAX_PERCENT;
		boolean instant = kiAttack.isInstantCast();
		int baseTicks = Math.max(1, kiAttack.getBaseChargeTicks());
		float percent = techniques.getTechniqueChargePercent();
		boolean holding = techniques.isChargeHolding();
		float base = (float) kiAttack.getCalculatedCost(data);

		boolean creative = player.isCreative();
		float ceiling = (holding && !instant) ? OVER : 100.0f;

		boolean outOfKi = false;
		if (percent < ceiling - 0.01f) {
			float rate = (percent < 100.0f) ? 100.0f / baseTicks : KiAttackData.OVERCHARGE_TIER_PERCENT / (float) baseTicks;
			float newP = Math.min(ceiling, percent + rate);

			if (creative) percent = newP;
			else {
				double chargeCost = 0.5 * base * (KiAttackData.costMultiplier(newP) - KiAttackData.costMultiplier(percent));
				float accum = CHARGE_COST_ACCUM.getOrDefault(player.getUUID(), 0.0f) + (float) chargeCost;
				float energy = data.getResources().getCurrentEnergy();
				int whole = (int) accum;
				int effectiveWhole = (int) Math.round(whole * data.getKiAttackCostModifier());

				if (energy >= effectiveWhole) {
					if (effectiveWhole > 0) {
						data.getResources().removeEnergy(effectiveWhole);
						applyHumanKiPassiveDuringCharge(player, data, whole);
					}
					accum -= whole;
					CHARGE_COST_ACCUM.put(player.getUUID(), accum);
					percent = newP;
				} else {
					float affordFrac = effectiveWhole > 0 ? Math.max(0.0f, Math.min(1.0f, energy / effectiveWhole)) : 1.0f;
					percent = percent + (newP - percent) * affordFrac;
					data.getResources().setCurrentEnergy(0);
					CHARGE_COST_ACCUM.put(player.getUUID(), 0.0f);
					applyHumanKiPassiveDuringCharge(player, data, Math.round(whole * affordFrac));
					outOfKi = true;
				}
			}
			techniques.setTechniqueChargePercent(percent);
		}

		boolean reachedCeiling = percent >= ceiling - 0.01f;
		if ((!holding || instant) && (reachedCeiling || outOfKi)) {
			resolveKiAttackOnRelease(player, data, techniques);
			return;
		}

		NetworkHandler.sendToTrackingEntityAndSelf(new TechniqueChargeSyncS2C(player.getId(), techniques.getTechniqueChargePercent(), true), player);
	}

	private static void resolveKiAttackOnRelease(ServerPlayer player, StatsData data, Techniques techniques) {
		float effectiveCharge = Math.min(techniques.getTechniqueChargePercent(), KiAttackData.OVERCHARGE_MAX_PERCENT);

		var activeKi = findChargingEntity(player);

		if (activeKi != null) {
			if (effectiveCharge < 50.0f) {
				activeKi.discard();
			} else {
				float chargeMultiplier = effectiveCharge / 100.0f;

				TechniqueData techniqueData = techniques.getUnlockedTechniques().get(techniques.getChargingTechniqueId());

				if (techniqueData instanceof KiAttackData kiAttack) {
					boolean fired = TechniqueDispatcher.executeKiAttack(player, player.level(), kiAttack, data, chargeMultiplier);

					if (fired) {
						boolean creative = player.isCreative();
						int baseCooldown = creative ? 60 : Math.max(1, (int) Math.ceil(kiAttack.getActualCooldown() * chargeMultiplier));
						DMZEvent.KiAttackFireEvent fireEvent = new DMZEvent.KiAttackFireEvent(player, data, kiAttack, chargeMultiplier, baseCooldown);
						MinecraftForge.EVENT_BUS.post(fireEvent);
						int cooldownTicks = Math.max(1, fireEvent.getCooldownTicks());
						data.getCooldowns().setCooldown(getTechniqueCooldownKey(kiAttack.getId()), cooldownTicks);

						if (!creative) {
							double base = kiAttack.getCalculatedCost(data);
							float costMult = KiAttackData.costMultiplier(effectiveCharge);
							boolean drainsOverLife = activeKi.isMovementRestrictedType();
							double fireFraction = drainsOverLife ? 0.25 : 0.50;
							int originalFireCost = (int) Math.round(fireFraction * base * costMult);
							int modifiedFireCost = (int) Math.round(originalFireCost * data.getKiAttackCostModifier());
							if (modifiedFireCost > 0) data.getResources().removeEnergy(modifiedFireCost);

							applyHumanKiPassiveDuringCharge(player, data, originalFireCost);
							player.getPersistentData().putFloat("dmz_human_hp_drain_accum", 0.0f);

							if (drainsOverLife) {
								double lifeCost = 0.25 * base * costMult;
								int maxLife = Math.max(1, activeKi.getMaxLife());
								activeKi.setKiLifetimeDrainPerTick((float) (lifeCost / maxLife));
							}
						}
					} else activeKi.discard();
				} else activeKi.discard();
			}
		}

		CHARGE_COST_ACCUM.remove(player.getUUID());
		CHARGING_CACHE.remove(player.getUUID());
		techniques.clearTechniqueCharge();
	}

	private static AbstractKiProjectile findChargingEntity(ServerPlayer player) {
		UUID playerId = player.getUUID();
		AbstractKiProjectile cached = CHARGING_CACHE.get(playerId);

		if (cached != null && !cached.isRemoved() && !isEntityFiring(cached)) {
			return cached;
		}

		List<AbstractKiProjectile> nearbyEntities = player.level().getEntitiesOfClass(
				AbstractKiProjectile.class,
				player.getBoundingBox().inflate(30.0D)
		);

		for (AbstractKiProjectile ki : nearbyEntities) {
			if (ki.getOwner() != null && ki.getOwner().getUUID().equals(playerId)) {
				if (!isEntityFiring(ki)) {
					CHARGING_CACHE.put(playerId, ki);
					return ki;
				}
			}
		}

		CHARGING_CACHE.remove(playerId);
		return null;
	}

	private static boolean playerOwnsKiProjectile(ServerPlayer player) {
		List<AbstractKiProjectile> list = player.level().getEntitiesOfClass(
				AbstractKiProjectile.class, player.getBoundingBox().inflate(48.0D));
		for (AbstractKiProjectile ki : list) {
			if (ki.getOwner() != null && ki.getOwner().getUUID().equals(player.getUUID())) return true;
		}
		return false;
	}

	private static boolean isEntityFiring(AbstractKiProjectile ki) {
		if (ki instanceof KiWaveEntity wave) return wave.isFiring();
		if (ki instanceof KiBlastEntity blast) return blast.isFiring();
		if (ki instanceof KiLaserEntity laser) return laser.isFiring();
		if (ki instanceof KiDiskEntity disk) return disk.isFiring();
		if (ki instanceof KiExplosionEntity explosion) return explosion.isFiring();
		if (ki instanceof KiBarrierEntity barrier) return barrier.isFiring();
        if (ki instanceof KiAreaEntity area) return area.isFiring();
		return false;
	}

	private static String getTechniqueCooldownKey(String techniqueId) {
		return "TechniqueCooldown_" + techniqueId;
	}

	private static boolean performAction(ServerPlayer player, StatsData data, ActionMode mode) {
		IActionModeHandler handler = ACTION_MODE_HANDLERS.get(mode.name());
		if (handler != null) return handler.performAction(player, data);
		return false;
	}

	public static boolean canChargeSelectedAction(ServerPlayer player, StatsData data) {
		IActionModeHandler handler = ACTION_MODE_HANDLERS.get(data.getStatus().getSelectedAction().name());
		return handler == null || handler.canCharge(player, data);
	}

	private static void handleActiveFormDrains(ServerPlayer player, StatsData data) {
		boolean hasActiveForm = data.getCharacter().getActiveForm() != null && !data.getCharacter().getActiveForm().isEmpty();
		boolean hasActiveStackForm = data.getCharacter().getActiveStackForm() != null && !data.getCharacter().getActiveStackForm().isEmpty();

		if (hasActiveForm && data.getCharacter().getSelectedFormGroup().contains("oozaru") && !data.getCharacter().isHasSaiyanTail()) {
			data.getCharacter().clearActiveForm(player);
			TransformationItemCostHelper.clearFormDurationSecondsRemaining(player);
			player.removeEffect(MainEffects.TRANSFORMED.get());
			player.refreshDimensions();
		}

		if (!data.getStatus().isAlive() && player.level().dimension().equals(OtherworldDimension.OTHERWORLD_KEY)) {
			if (player.getFoodData().getFoodLevel() <= 20) player.getFoodData().setFoodLevel(20);
			return;
		}

		if (hasActiveForm || hasActiveStackForm) {
			handleDurationItemCosts(player, data, hasActiveForm, hasActiveStackForm);
			hasActiveForm = data.getCharacter().getActiveForm() != null && !data.getCharacter().getActiveForm().isEmpty();
			hasActiveStackForm = data.getCharacter().getActiveStackForm() != null && !data.getCharacter().getActiveStackForm().isEmpty();
		}

		if ((hasActiveForm || hasActiveStackForm) && !player.isCreative() && !player.isSpectator()) {

			double totalOffense = data.getMeleeDamage() + data.getStrikeDamage() + data.getKiDamage();
			double ratioTolerance = 1.5;

			double maxEnergy = data.getMaxEnergy();
			double baseEnergyDrain = data.getAdjustedEnergyDrain();
			double finalEnergyDrain = 0.0;

			if (baseEnergyDrain > 0.0) {
				double energyRatio = Math.max(1.0, totalOffense / Math.max(1.0, maxEnergy * ratioTolerance));

				double formRawEneDrain = 0.0;
				if (hasActiveForm && data.getCharacter().getActiveFormData() != null) {
					formRawEneDrain += Math.max(0.0, data.getCharacter().getActiveFormData().getEnergyDrain());
				}
				if (hasActiveStackForm && data.getCharacter().getActiveStackFormData() != null) {
					formRawEneDrain += Math.max(0.0, data.getCharacter().getActiveStackFormData().getEnergyDrain());
				}

				double percentageEnergy = maxEnergy * (formRawEneDrain * 0.01);
				finalEnergyDrain = (baseEnergyDrain * energyRatio) + percentageEnergy;
			}

			double maxStamina = data.getMaxStamina();
			double baseStaminaDrain = data.getAdjustedStaminaDrain();
			double finalStaminaDrain = 0.0;

			if (baseStaminaDrain > 0.0) {
				double staminaRatio = Math.max(1.0, totalOffense / Math.max(1.0, maxStamina * ratioTolerance));
				double percentageStamina = maxStamina * 0.005;
				finalStaminaDrain = (baseStaminaDrain * staminaRatio) + percentageStamina;
			}

			double maxHealth = player.getMaxHealth();
			double baseHealthDrain = data.getAdjustedHealthDrain();
			double finalHealthDrain = 0.0;

			if (baseHealthDrain > 0.0) {
				double healthRatio = Math.max(1.0, totalOffense / Math.max(1.0, maxHealth * ratioTolerance));
				double percentageHealth = maxHealth * 0.005;
				finalHealthDrain = (baseHealthDrain * healthRatio) + percentageHealth;
			}

			int energyDrain = (int) Math.round(finalEnergyDrain);
			int staminaDrain = (int) Math.round(finalStaminaDrain);
			double healthDrain = Math.round(finalHealthDrain);

			boolean hasEnoughEnergy = data.getResources().getCurrentEnergy() >= energyDrain;
			boolean hasEnoughStamina = data.getResources().getCurrentStamina() >= staminaDrain;
			boolean hasEnoughHealth = player.getHealth() > healthDrain;

			if (hasEnoughEnergy && hasEnoughStamina && hasEnoughHealth) {
				if (energyDrain > 0) data.getResources().removeEnergy(energyDrain);
				if (staminaDrain > 0) data.getResources().removeStamina(staminaDrain);
				if (healthDrain > 0) player.setHealth((float) (player.getHealth() - healthDrain));
			} else {
				data.getCharacter().clearActiveStackForm(player);
				TransformationItemCostHelper.clearStackFormDurationSecondsRemaining(player);
				player.removeEffect(MainEffects.STACK_TRANSFORMED.get());
				data.getCharacter().clearActiveForm(player);
				TransformationItemCostHelper.clearFormDurationSecondsRemaining(player);
				player.removeEffect(MainEffects.TRANSFORMED.get());
				player.refreshDimensions();
			}
		}
	}

	private static void handleDurationItemCosts(ServerPlayer player, StatsData data, boolean hasActiveForm, boolean hasActiveStackForm) {
		if (player.isCreative() || player.isSpectator()) {
			if (hasActiveForm) TransformationItemCostHelper.clearFormDurationSecondsRemaining(player);
			if (hasActiveStackForm) TransformationItemCostHelper.clearStackFormDurationSecondsRemaining(player);
			return;
		}

		if (!hasActiveForm) {
			TransformationItemCostHelper.clearFormDurationSecondsRemaining(player);
		} else {
			handleSingleDurationCost(player, data, true);
		}

		if (!hasActiveStackForm) {
			TransformationItemCostHelper.clearStackFormDurationSecondsRemaining(player);
		} else {
			handleSingleDurationCost(player, data, false);
		}
	}

	private static void handleSingleDurationCost(ServerPlayer player, StatsData data, boolean baseForm) {
		FormConfig.FormData activeData = baseForm ? data.getCharacter().getActiveFormData() : data.getCharacter().getActiveStackFormData();
		if (activeData == null || !activeData.hasDurationItemCosts()) {
			if (baseForm) TransformationItemCostHelper.clearFormDurationSecondsRemaining(player);
			else TransformationItemCostHelper.clearStackFormDurationSecondsRemaining(player);
			return;
		}

		int remaining = baseForm ? TransformationItemCostHelper.getFormDurationSecondsRemaining(player) : TransformationItemCostHelper.getStackFormDurationSecondsRemaining(player);
		if (remaining <= 0) {
			int addedSeconds = TransformationItemCostHelper.consumeDurationItem(player, activeData);
			if (addedSeconds <= 0) {
				clearTransformationForMissingDurationItem(player, data, baseForm);
				return;
			}
			remaining += addedSeconds;
		}

		remaining = Math.max(0, remaining - 1);
		if (baseForm) TransformationItemCostHelper.setFormDurationSecondsRemaining(player, remaining);
		else TransformationItemCostHelper.setStackFormDurationSecondsRemaining(player, remaining);
	}

	private static void clearTransformationForMissingDurationItem(ServerPlayer player, StatsData data, boolean baseForm) {
		if (baseForm) {
			data.getCharacter().clearActiveForm(player);
			TransformationItemCostHelper.clearFormDurationSecondsRemaining(player);
			player.removeEffect(MainEffects.TRANSFORMED.get());
		} else {
			data.getCharacter().clearActiveStackForm(player);
			TransformationItemCostHelper.clearStackFormDurationSecondsRemaining(player);
			player.removeEffect(MainEffects.STACK_TRANSFORMED.get());
		}
		player.sendSystemMessage(Component.translatable("message.dragonminez.form.no_duration_item"), true);
		player.refreshDimensions();
	}

	private static void applyHumanKiPassiveDuringCharge(ServerPlayer player, StatsData data, int originalKiCost) {
		if (!data.isHumanRacialActive() || data.isAndroidRacialActive() || originalKiCost <= 0) return;

		float hpAccum = player.getPersistentData().getFloat("dmz_human_hp_drain_accum") + originalKiCost * 0.125f;
		int hpWhole = (int) hpAccum;

		if (hpWhole > 0 && player.getHealth() > hpWhole + 2.0f) {
			player.setHealth(player.getHealth() - hpWhole);
			double bonusDmg = player.getPersistentData().getDouble("dmz_human_ki_bonus_dmg") + hpWhole;
			player.getPersistentData().putDouble("dmz_human_ki_bonus_dmg", bonusDmg);
			hpAccum -= hpWhole;
		}
		player.getPersistentData().putFloat("dmz_human_hp_drain_accum", hpAccum);
	}

	private static void clearHumanKiAccumulators(ServerPlayer player) {
		player.getPersistentData().putFloat("dmz_human_hp_drain_accum", 0.0f);
		player.getPersistentData().putDouble("dmz_human_ki_bonus_dmg", 0.0);
	}

	public static void registerActionModeHandlers() {
		ACTION_MODE_HANDLERS.put(ActionMode.FORM.name(), new FormModeHandler());
		ACTION_MODE_HANDLERS.put(ActionMode.FUSION.name(), new FusionModeHandler());
		ACTION_MODE_HANDLERS.put(ActionMode.STACK.name(), new StackFormModeHandler());
		ACTION_MODE_HANDLERS.put(ActionMode.RACIAL.name(), new RacialModeHandler());
	}

	public static void registerStatusEffectHandlers() {
		STATUS_EFFECT_HANDLERS.add(new TransformStatusHandler());
		STATUS_EFFECT_HANDLERS.add(new BioDrainHandler());
		STATUS_EFFECT_HANDLERS.add(new DashStatusHandler());
		STATUS_EFFECT_HANDLERS.add(new DoubleDashStatusHandler());
		STATUS_EFFECT_HANDLERS.add(new FlyStatusHandler());
		STATUS_EFFECT_HANDLERS.add(new FusionStatusHandler());
		STATUS_EFFECT_HANDLERS.add(new KiChargeStatusHandler());
		STATUS_EFFECT_HANDLERS.add(new MajinStatusHandler());
		STATUS_EFFECT_HANDLERS.add(new MightFruitStatusHandler());
		STATUS_EFFECT_HANDLERS.add(new SaiyanPassiveHandler());
		STATUS_EFFECT_HANDLERS.add(new BioPassiveHandler());
		STATUS_EFFECT_HANDLERS.add(new MajinReviveHandler());
	}

	public static void registerActionModeHandler(String actionMode, IActionModeHandler actionModeHandler) {
		ACTION_MODE_HANDLERS.putIfAbsent(actionMode, actionModeHandler);
	}

	public static void registerStatusEffectHandler(IStatusEffectHandler statusEffectHandler) {
		STATUS_EFFECT_HANDLERS.add(statusEffectHandler);
	}
}
