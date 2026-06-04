package com.dragonminez.server.dynamicgrowth;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig.DynamicGrowthConfig;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.init.entities.ShadowDummyEntity;
import com.dragonminez.common.init.entities.dragon.DragonWishEntity;
import com.dragonminez.common.init.entities.namek.NamekTraderEntity;
import com.dragonminez.common.init.entities.namek.NamekVillagerEntity;
import com.dragonminez.common.init.entities.questnpc.QuestNPCEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.extras.DynamicGrowthData;
import com.dragonminez.common.stats.extras.DynamicGrowthMath;
import com.dragonminez.common.stats.extras.DynamicGrowthStat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

public final class DynamicGrowthService {
	private static final long COMBAT_WINDOW_MS = 15000L;

	private DynamicGrowthService() {}

	private static DynamicGrowthConfig config() {
		return ConfigManager.getServerConfig().getDynamicGrowth();
	}

	public static void award(ServerPlayer player, StatsData data, DynamicGrowthStat stat, double baseXp, LivingEntity repeatedTarget) {
		if (player == null || data == null) return;
		DynamicGrowthConfig cfg = config();
		if (!cfg.isEnabled()) return;
		if (baseXp <= 0.0) return;
		if (player.isCreative() || player.isSpectator()) return;
		if (!data.getStatus().isHasCreatedCharacter()) return;

		DynamicGrowthData growth = data.getDynamicGrowth();
		long nowMs = System.currentTimeMillis();
		double xp = baseXp;

		if (repeatedTarget != null) {
			double repeatMultiplier = growth.recordTargetAndGetMultiplier(
					repeatedTarget.getUUID().toString(),
					nowMs,
					cfg.getRepeatTargetWindowSeconds(),
					cfg.getRepeatTargetSoftCap(),
					cfg.getRepeatTargetHardCap(),
					cfg.getRepeatTargetSoftMultiplier(),
					cfg.getRepeatTargetHardMultiplier()
			);
			xp = baseXp * repeatMultiplier;
		}

		xp *= cfg.getPracticeXpMultiplier();
		xp *= cfg.getStatPracticeMultiplier(stat.key());
		if (xp <= 0.0) return;

		growth.addPracticeXp(stat, xp);
		processLevelUps(player, data, stat);
	}

	public static void awardStaminaSpent(ServerPlayer player, StatsData data, double spent) {
		if (spent <= 0) return;
		award(player, data, DynamicGrowthStat.RES, spent * config().getStaminaSpentXpRatio(), null);
	}

	public static void awardEnergySpent(ServerPlayer player, StatsData data, double spent) {
		if (spent <= 0) return;
		award(player, data, DynamicGrowthStat.ENE, spent * config().getEnergySpentXpRatio(), null);
	}

	public static void awardStrike(ServerPlayer player, StatsData data, LivingEntity target, double damage) {
		double xp = practiceDamageXp(player, target, (float) damage);
		award(player, data, DynamicGrowthStat.SKP, xp, target);
	}

	public static void markCombat(StatsData data) {
		if (data != null) data.getDynamicGrowth().markCombat(System.currentTimeMillis());
	}

	public static boolean isRecentlyInCombat(StatsData data) {
		return data != null && data.getDynamicGrowth().isRecentlyInCombat(System.currentTimeMillis(), COMBAT_WINDOW_MS);
	}

	public static double practiceDamageXp(ServerPlayer player, LivingEntity target, float damage) {
		DynamicGrowthConfig cfg = config();
		double cappedDamage = Math.min(damage, Math.max(1.0, target.getMaxHealth() * 0.2));
		double multiplier = 1.0;

		if (damage < 0.5F) multiplier *= cfg.getLowDamagePracticeMultiplier();

		if (target instanceof ShadowDummyEntity) multiplier *= cfg.getShadowDummyPracticeMultiplier();
		else if (isProtectedNpc(target)) multiplier *= cfg.getVillagerPracticeMultiplier();
		else if (target instanceof Animal) multiplier *= cfg.getPassiveAnimalPracticeMultiplier();

		if (isNoRiskTarget(player, target)) multiplier *= cfg.getNoRiskPracticeMultiplier();

		return cappedDamage * multiplier;
	}

	private static boolean isProtectedNpc(LivingEntity target) {
		return target instanceof Villager || target instanceof NamekVillagerEntity || target instanceof NamekTraderEntity || target instanceof MastersEntity || target instanceof DragonWishEntity;
	}

	private static boolean isNoRiskTarget(ServerPlayer player, LivingEntity target) {
		if (target instanceof Player || target instanceof Monster) return false;
		if (!(target instanceof Mob mob)) return true;
		LivingEntity currentTarget = mob.getTarget();
		return currentTarget == null || !currentTarget.is(player);
	}

	private static void processLevelUps(ServerPlayer player, StatsData data, DynamicGrowthStat stat) {
		DynamicGrowthData growth = data.getDynamicGrowth();
		boolean leveled = false;

		while (data.getMaxAllowedIncreaseForStat(stat.key(), 1) > 0) {
			int currentStat = data.getCurrentStatValue(stat.key());
			int requiredXp = DynamicGrowthMath.requiredXp(currentStat);
			if (growth.getPracticeXp(stat) < requiredXp) break;

			growth.consumePracticeXp(stat, requiredXp);
			grantStatPoint(player, data, stat);
			leveled = true;
			notifyStatGain(player, stat, data.getCurrentStatValue(stat.key()));
		}

		if (leveled) NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
	}

	private static void grantStatPoint(ServerPlayer player, StatsData data, DynamicGrowthStat stat) {
		switch (stat) {
			case RES -> {
				float oldMaxStamina = data.getMaxStamina();
				data.getStats().addResistance(1);
				float newMaxStamina = data.getMaxStamina();
				if (newMaxStamina > oldMaxStamina) data.getResources().addStamina(newMaxStamina - oldMaxStamina);
			}
			case VIT -> {
				float oldMaxHealth = data.getMaxHealth();
				data.getStats().addVitality(1);
				float newMaxHealth = data.getMaxHealth();
				if (newMaxHealth > oldMaxHealth) player.heal(newMaxHealth - oldMaxHealth);
			}
			case ENE -> {
				float oldMaxEnergy = data.getMaxEnergy();
				data.getStats().addEnergy(1);
				float newMaxEnergy = data.getMaxEnergy();
				if (newMaxEnergy > oldMaxEnergy) data.getResources().addEnergy(newMaxEnergy - oldMaxEnergy);
			}
			case STR -> data.getStats().addStrength(1);
			case SKP -> data.getStats().addStrikePower(1);
			case PWR -> data.getStats().addKiPower(1);
		}
	}

	private static void notifyStatGain(ServerPlayer player, DynamicGrowthStat stat, int newValue) {
		Component message = Component.translatable("dynamicgrowth.dragonminez.stat_gain",
				Component.translatable("gui.dragonminez.character_stats." + stat.key().toLowerCase(Locale.ROOT)), newValue).withStyle(ChatFormatting.GREEN);
		player.displayClientMessage(message, true);
		player.playNotifySound(SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.35F, 1.25F);
	}

	private static String fmt(double value) {
		return String.format(Locale.ROOT, "%.3f", value);
	}
}
