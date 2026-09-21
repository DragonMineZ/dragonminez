package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.FalseSuperSaiyanHelper;
import com.dragonminez.common.util.TransformationsHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FalseSuperSaiyanEvents {
	private static final Map<UUID, Long> LAST_HIT_MS = new HashMap<>();

	private static GeneralServerConfig.FalseSuperSaiyanConfig config() {
		return FalseSuperSaiyanHelper.config();
	}

	public static void onFinalDamage(LivingDamageEvent event) {
		if (event.isCanceled() || event.getAmount() <= 0.0f) return;
		LivingEntity victim = event.getEntity();
		if (victim.level().isClientSide) return;
		GeneralServerConfig.FalseSuperSaiyanConfig cfg = config();
		if (cfg == null || !cfg.getEnabled()) return;

		float amount = Math.min(event.getAmount(), victim.getHealth());
		DamageSource source = event.getSource();

		if (victim instanceof ServerPlayer hurt) {
			StatsProvider.get(StatsCapability.INSTANCE, hurt).ifPresent(data -> {
				if (!canGain(hurt, data)) return;
				double fraction = amount / Math.max(1.0f, hurt.getMaxHealth());
				if (fraction < cfg.getSignificantDamageTakenRatio()) return;
				addRage(hurt, data, cfg, fraction * cfg.getGainPerReceivedHealthFraction());
			});
		}

		if (source.getEntity() instanceof ServerPlayer attacker && attacker != victim) {
			StatsProvider.get(StatsCapability.INSTANCE, attacker).ifPresent(data -> {
				if (!canGain(attacker, data)) return;
				if (!isWorthyTarget(attacker, victim, event.getAmount(), cfg)) return;
				double fraction = amount / Math.max(1.0f, victim.getMaxHealth());
				addRage(attacker, data, cfg, fraction * cfg.getGainPerDealtHealthFraction());
			});
		}
	}

	private static void addRage(ServerPlayer player, StatsData data, GeneralServerConfig.FalseSuperSaiyanConfig cfg, double gain) {
		if (FalseSuperSaiyanHelper.isActiveForm(data)) gain *= cfg.getActiveGainMultiplier();
		gain = Math.min(gain, cfg.getMaxGainPerHit());
		LAST_HIT_MS.put(player.getUUID(), System.currentTimeMillis());
		if (gain <= 0.0) return;

		data.getResources().setRage((float) (data.getResources().getRage() + gain));
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
		GeneralServerConfig.FalseSuperSaiyanConfig cfg = config();
		if (cfg == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			boolean active = FalseSuperSaiyanHelper.isActiveForm(data);

			if (!active && !FalseSuperSaiyanHelper.meetsBaseRequirements(data)) {
				setChargeState(player, data, false);
				LAST_HIT_MS.remove(player.getUUID());
				return;
			}

			if (active && !FalseSuperSaiyanHelper.meetsBaseRequirements(data)) {
				end(player, data);
				return;
			}

			if (data.getStatus().isKnockedDown()) {
				if (!active) setChargeState(player, data, false);
				return;
			}

			var resources = data.getResources();

			if (!active) {
				boolean canCharge = FalseSuperSaiyanHelper.canCharge(player, data);
				setChargeState(player, data, canCharge);
				if (canCharge) {
					if (resources.isRageFull()) {
						transform(player, data, cfg);
						return;
					}
					double idleGain = cfg.getIdleGainPerSecond();
					if (idleGain > 0.0) {
						resources.setRage((float) (resources.getRage() + idleGain / 20.0));
						if (resources.isRageFull()) transform(player, data, cfg);
						return;
					}
				}
				if (resources.getRage() <= 0.0f) return;
			}

			if (isInCombat(player, cfg)) return;

			double drainSeconds = active ? cfg.getActiveDrainSeconds() : cfg.getDrainSeconds();
			resources.setRage(resources.getRage() - (float) (100.0 / (drainSeconds * 20.0)));
			if (active && resources.getRage() <= 0.0f) end(player, data);
		});
	}

	private static void setChargeState(ServerPlayer player, StatsData data, boolean charging) {
		var status = data.getStatus();

		if (charging) {
			if (status.isForcedAura() && status.isForcedCharge()) return;
			status.setForcedAura(true);
			status.setForcedCharge(true);
			sync(player);
			return;
		}

		if (!FalseSuperSaiyanHelper.RACE.equalsIgnoreCase(data.getCharacter().getRaceName())) return;
		if (data.getEffects().hasEffect("mutant")) return;
		if (!status.isForcedAura() && !status.isForcedCharge() && !status.isRageActive()) return;

		status.setForcedAura(false);
		status.setForcedCharge(false);
		status.setRageActive(false);
		sync(player);
	}

	private static boolean canGain(ServerPlayer player, StatsData data) {
		if (data.getStatus().isKnockedDown()) return false;
		if (FalseSuperSaiyanHelper.isActiveForm(data)) return true;
		return FalseSuperSaiyanHelper.canCharge(player, data);
	}

	private static boolean isWorthyTarget(ServerPlayer attacker, LivingEntity victim, float damage, GeneralServerConfig.FalseSuperSaiyanConfig cfg) {
		if (victim instanceof Player) return true;
		if (victim.getMaxHealth() < attacker.getMaxHealth() * cfg.getSignificantTargetHealthRatio()) return false;
		boolean oneShot = victim.getHealth() >= victim.getMaxHealth() * 0.99f && damage >= victim.getHealth();
		return !oneShot;
	}

	private static boolean isInCombat(ServerPlayer player, GeneralServerConfig.FalseSuperSaiyanConfig cfg) {
		Long last = LAST_HIT_MS.get(player.getUUID());
		if (last == null) return false;
		return System.currentTimeMillis() - last <= cfg.getCombatGraceSeconds() * 1000.0;
	}

	private static void transform(ServerPlayer player, StatsData data, GeneralServerConfig.FalseSuperSaiyanConfig cfg) {
		FormConfig.FormData form = FalseSuperSaiyanHelper.formData();
		if (form == null) return;

		float[] resourceSnapshot = data.snapshotMultiplierResources();
		data.getCharacter().clearPreviousFormRecord();
		data.getCharacter().setActiveForm(cfg.getGroupName(), form.getName());
		data.restoreMultiplierGains(player, resourceSnapshot);

		data.getStatus().setForcedAura(true);
		data.getStatus().setForcedCharge(false);
		data.getStatus().setRageActive(true);
		data.getResources().setRage(100.0f);

		if (!player.hasEffect(MainEffects.TRANSFORMED.get())) {
			player.addEffect(new MobEffectInstance(MainEffects.TRANSFORMED.get(), -1, 0, false, false, true));
		}
		player.refreshDimensions();
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.TRANSFORM_ON.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
		player.sendSystemMessage(Component.translatable("message.dragonminez.falsesupersaiyan.transformed"), true);
		sync(player);
	}

	private static void end(ServerPlayer player, StatsData data) {
		boolean wasActive = FalseSuperSaiyanHelper.isActiveForm(data);

		data.getResources().setRage(0.0f);
		data.getStatus().setRageActive(false);
		data.getStatus().setForcedAura(false);
		data.getStatus().setForcedCharge(false);

		if (wasActive) {
			float[] resourceSnapshot = data.snapshotMultiplierResources();
			data.getCharacter().clearPreviousFormRecord();
			TransformationsHelper.revertToBaseForm(player, data, false);
			data.restoreMultiplierGains(player, resourceSnapshot);
			player.removeEffect(MainEffects.TRANSFORMED.get());
			player.refreshDimensions();
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.INSTA_FORM_OFF.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
			player.sendSystemMessage(Component.translatable("message.dragonminez.falsesupersaiyan.ended"), true);
		}

		LAST_HIT_MS.remove(player.getUUID());
		sync(player);
	}

	@SubscribeEvent
	public static void onDeath(LivingDeathEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!FalseSuperSaiyanHelper.isActiveForm(data) && !FalseSuperSaiyanHelper.meetsBaseRequirements(data)) return;
			data.getResources().setRage(0.0f);
			data.getStatus().setRageActive(false);
			data.getStatus().setForcedAura(false);
			data.getStatus().setForcedCharge(false);
		});
		LAST_HIT_MS.remove(player.getUUID());
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		LAST_HIT_MS.remove(event.getEntity().getUUID());
	}

	private static void sync(ServerPlayer player) {
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
	}
}
