package com.dragonminez.server.events.players;

import com.dragonminez.common.config.CombatConfig;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.init.entities.MobBattlePowerHelper;
import com.dragonminez.common.init.entities.ki.KiExplosionVisualEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.KiBurstVfxS2C;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.character.Resources;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.server.events.players.combat.KnockbackHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class KiSurgeService {
	public static final String NON_LETHAL_TAG = "dmz_surge_non_lethal";
	private static final String WAS_CHARGING_TAG = "dmz_was_charging_ki_session";
	private static final String EARNED_BURST_TAG = "dmz_earned_ki_burst";

	private KiSurgeService() {}

	public static void tick(ServerPlayer player, StatsData data) {
		CombatConfig config = ConfigManager.getCombatConfig();
		Status status = data.getStatus();
		Resources resources = data.getResources();

		if (config == null || !config.getEnableKiSurge()) {
			if (status.isSurgeActive() || status.isKiBurstArmed() || resources.getSurgeCharge() > 0) {
				clear(data);
				sync(player);
			}
			return;
		}

		if (status.isStunned()) {
			breakSurge(player, data);
			return;
		}

		if (status.isSurgeActive()) {
			float decayPerTick = 100.0f / Math.max(1.0f, (float) (config.getSurgeDurationSeconds() * 20.0));
			resources.setSurgeCharge(resources.getSurgeCharge() - decayPerTick);
			if (resources.getSurgeCharge() <= 0.0f) {
				status.setSurgeActive(false);
				startCooldown(data);
				sync(player);
			}
			return;
		}

		boolean charging = status.isChargingKi() && !status.isDescending();

		boolean wasCharging = player.getPersistentData().getBoolean(WAS_CHARGING_TAG);
		if (charging && !wasCharging) {
			player.getPersistentData().putBoolean(EARNED_BURST_TAG, resources.getCurrentEnergy() < data.getMaxEnergy());
		}
		player.getPersistentData().putBoolean(WAS_CHARGING_TAG, charging);

		if (charging && !status.isKiBurstArmed() && resources.getCurrentEnergy() >= data.getMaxEnergy()
				&& !data.getCooldowns().hasCooldown(Cooldowns.KI_SURGE_CD)) {
			status.setKiBurstArmed(true);
			if (player.getPersistentData().getBoolean(EARNED_BURST_TAG)) {
				player.getPersistentData().putBoolean(EARNED_BURST_TAG, false);
				triggerBurst(player, data, false);
			}
			sync(player);
		}

		if (!status.isKiBurstArmed()) return;

		if (!charging) {
			resources.setSurgeCharge(0.0f);
			status.setKiBurstArmed(false);
			sync(player);
			return;
		}

		float fillPerTick = 100.0f / Math.max(1.0f, (float) (config.getSurgeFillSeconds() * 20.0));
		resources.setSurgeCharge(resources.getSurgeCharge() + fillPerTick);

		if (resources.isSurgeFull()) {
			status.setKiBurstArmed(false);
			status.setSurgeActive(true);
			triggerBurst(player, data, true);
			sync(player);
		}
	}

	public static void forceActivateSurge(ServerPlayer player, StatsData data) {
		if (data.getStatus().isSurgeActive()) return;
		if (!ConfigManager.getCombatConfig().getEnableKiSurge()) return;
		if (data.getCooldowns().hasCooldown(Cooldowns.KI_SURGE_CD)) return;

		data.getStatus().setKiBurstArmed(false);
		data.getStatus().setSurgeActive(true);
		data.getResources().setSurgeCharge(100.0f);
		triggerBurst(player, data, true);
		sync(player);
	}

	public static void interruptCharge(ServerPlayer player, StatsData data) {
		Status status = data.getStatus();
		if (status.isSurgeActive()) return;
		if (!status.isChargingKi() && !status.isKiBurstArmed() && data.getResources().getSurgeCharge() <= 0) return;

		boolean lostProgress = status.isKiBurstArmed() || data.getResources().getSurgeCharge() > 0;

		status.setChargingKi(false);
		status.setKiBurstArmed(false);
		data.getResources().setSurgeCharge(0.0f);
		if (lostProgress) startCooldown(data);

		int lockTicks = Math.max(0, ConfigManager.getCombatConfig().getKiChargeInterruptLockTicks());
		if (lockTicks > 0) {
			player.getPersistentData().putLong("dmz_ki_charge_lock_until", player.level().getGameTime() + lockTicks);
		}
		sync(player);
	}

	public static void breakSurge(ServerPlayer player, StatsData data) {
		if (!data.getStatus().isSurgeActive() && !data.getStatus().isKiBurstArmed()
				&& data.getResources().getSurgeCharge() <= 0) return;
		clear(data);
		startCooldown(data);
		sync(player);
	}

	private static void startCooldown(StatsData data) {
		int seconds = ConfigManager.getCombatConfig().getSurgeCooldownSeconds();
		data.getCooldowns().setCooldown(Cooldowns.KI_SURGE_CD, Math.max(1, seconds * 20));
	}

	public static boolean isChargeLocked(ServerPlayer player) {
		return player.level().getGameTime() < player.getPersistentData().getLong("dmz_ki_charge_lock_until");
	}

	private static void clear(StatsData data) {
		data.getStatus().setKiBurstArmed(false);
		data.getStatus().setSurgeActive(false);
		data.getResources().setSurgeCharge(0.0f);
	}

	private static void sync(ServerPlayer player) {
		NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(player), player);
	}

	private static void triggerBurst(ServerPlayer player, StatsData data, boolean full) {
		CombatConfig config = ConfigManager.getCombatConfig();
		Level level = player.level();

		double radius = full ? config.getSurgeBurstRadius() : config.getKiBurstRadius();
		double knockback = full ? config.getSurgeBurstKnockback() : config.getKiBurstKnockback();
		Vec3 center = player.position().add(0, player.getBbHeight() / 2.0, 0);

		float[] auraRgb = data.getCharacter().getActiveRgbAuraColor();
		int colorMain = packColor(auraRgb, 1.0f);
		int colorBorder = packColor(auraRgb, 0.65f);

		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(MainParticles.KI_EXPLOSION.get(),
					center.x, center.y, center.z, 0, radius * 1.4, 0.0, 0.0, 1.0);

			KiExplosionVisualEntity visual = new KiExplosionVisualEntity(MainEntities.KI_EXPLOSION_VISUAL.get(), level);
			visual.setPos(center.x, center.y, center.z);
			visual.setupExplosion(colorMain, colorBorder, 0xFFFFFF, (float) radius / 2.0F);
			level.addFreshEntity(visual);
		}

		level.playSound(null, center.x, center.y, center.z,
				full ? MainSounds.KI_EXPLOSION_IMPACT.get() : MainSounds.AURA_START.get(),
				SoundSource.PLAYERS, full ? 4.0F : 2.0F, full ? 0.7F : 1.1F);

		NetworkHandler.sendToTrackingEntityAndSelf(
				new KiBurstVfxS2C(player.getId(), full, (float) radius), player);

		double selfBattlePower = data.getBattlePowerExact();
		double burstDamage = full ? data.getMaxEnergy() * config.getSurgeBurstDamageRatio() : 0.0;
		boolean partyPvpEnabled = PartyManager.isPartyPvpEnabled(player);

		List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class,
				player.getBoundingBox().inflate(radius),
				e -> e.isAlive() && !e.isSpectator() && e.distanceTo(player) <= radius);

		for (LivingEntity victim : victims) {
			if (victim == player) continue;
			if (victim instanceof Player victimPlayer && PartyManager.areInSameParty(player, victimPlayer) && !partyPvpEnabled) continue;

			if (!full && battlePowerOf(victim) >= selfBattlePower) continue;

			double distance = victim.distanceTo(player);
			double falloff = Math.max(0.0, 1.0 - (distance / radius));
			if (falloff <= 0.0) continue;

			if (burstDamage > 0.0) applyNonLethal(player, victim, (float) (burstDamage * falloff));

			Vec3 push = victim.position().subtract(player.position());
			if (push.lengthSqr() < 1.0e-4) push = new Vec3(0.0, 1.0, 0.0);
			push = push.normalize().scale(knockback * falloff).add(0.0, 0.25 * knockback * falloff, 0.0);
			KnockbackHelper.apply(victim, push);
		}
	}

	private static void applyNonLethal(ServerPlayer source, LivingEntity victim, float amount) {
		float headroom = victim.getHealth() - 1.0f;
		if (headroom <= 0.0f) return;

		float capped = Math.min(amount, headroom);
		if (capped <= 0.0f) return;

		victim.getPersistentData().putBoolean(NON_LETHAL_TAG, true);
		try {
			victim.hurt(MainDamageTypes.kiblast(victim.level(), source, source), capped);
		} finally {
			victim.getPersistentData().remove(NON_LETHAL_TAG);
		}
	}

	private static double battlePowerOf(LivingEntity entity) {
		if (entity instanceof Player player) {
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
			if (data != null) return data.getBattlePowerExact();
		}
		if (entity instanceof IBattlePower battlePower) return battlePower.getBattlePower();
		return MobBattlePowerHelper.calculate(entity);
	}

	private static int packColor(float[] rgb, float scale) {
		int r = (int) Math.min(255, Math.max(0, rgb[0] * 255.0f * scale));
		int g = (int) Math.min(255, Math.max(0, rgb[1] * 255.0f * scale));
		int b = (int) Math.min(255, Math.max(0, rgb[2] * 255.0f * scale));
		return (r << 16) | (g << 8) | b;
	}
}
