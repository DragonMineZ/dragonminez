package com.dragonminez.common.racial;

import com.dragonminez.common.stats.StatsData;
import net.minecraft.world.damagesource.DamageSource;

public interface RacialAbility {

	String id();

	default boolean canActivate(RacialContext ctx) {
		return false;
	}

	default boolean hasActiveAction() {
		return false;
	}

	default int chargeSeconds(RacialContext ctx) {
		return 0;
	}

	default boolean onActivate(RacialContext ctx) {
		return false;
	}

	default boolean onSecondaryActivate(RacialContext ctx) {
		return false;
	}

	default void onTick(RacialContext ctx) {
	}

	default void onSecond(RacialContext ctx) {
	}

	default double modifyDamageTaken(RacialContext ctx, double postMitigationDamage, DamageSource source) {
		return postMitigationDamage;
	}

	default void onDamageTakenPost(RacialContext ctx, float damageTaken) {
	}

	default boolean onLethalDamage(RacialContext ctx, LethalContext lc) {
		return false;
	}

	default void onDeath(RacialContext ctx) {
	}

	default double modifyStatXpGain(RacialContext ctx, String stat, double amount) {
		return amount;
	}

	default double modifyTechniqueXpGain(RacialContext ctx, double amount) {
		return amount;
	}

	default double modifyMasteryGain(RacialContext ctx, double amount) {
		return amount;
	}

	default double modifyHealReceived(RacialContext ctx, double amount, HealSource source) {
		return amount;
	}

	default double modifyHealDealt(RacialContext ctx, double amount, HealSource source) {
		return amount;
	}

	default int consumeFormUpkeep(RacialContext ctx, int energyDrain) {
		return energyDrain;
	}

	default double modifyFormStatMultiplier(StatsData data, String groupName, double multiplier) {
		return multiplier;
	}

	default double maxReleaseMultiplier(StatsData data) {
		return 1.0;
	}

	default void onLogin(RacialContext ctx) {
	}

	default void onLogout(RacialContext ctx) {
	}

	default void onRespawn(RacialContext ctx) {
	}

	default void onDimensionChange(RacialContext ctx) {
	}
}
