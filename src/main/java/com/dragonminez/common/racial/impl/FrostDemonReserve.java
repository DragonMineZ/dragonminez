package com.dragonminez.common.racial.impl;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.RacialDataSyncS2C;
import com.dragonminez.common.racial.RacialAbility;
import com.dragonminez.common.racial.RacialContext;
import com.dragonminez.common.racial.RacialData;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.world.damagesource.DamageSource;

public class FrostDemonReserve implements RacialAbility {

	@Override
	public String id() {
		return "frostdemon";
	}

	@Override
	public double modifyMasteryGain(RacialContext ctx, double amount) {
		return amount * (1.0 + ctx.config().getFrostdemon().getMasteryGainBonus());
	}

	@Override
	public double modifyDamageTaken(RacialContext ctx, double postMitigationDamage, DamageSource source) {
		GeneralServerConfig.FrostDemonRacialConfig config = ctx.config().getFrostdemon();
		if (MainDamageTypes.isKiblastDamage(source)) return postMitigationDamage * (1.0 - config.getKiTechniqueResistance());
		if (MainDamageTypes.isStrikeAttackDamage(source)) return postMitigationDamage * (1.0 - config.getStrikeTechniqueResistance());
		return postMitigationDamage;
	}

	@Override
	public int consumeFormUpkeep(RacialContext ctx, int energyDrain) {
		return isReserveFeeding(ctx.data()) ? 0 : energyDrain;
	}

	@Override
	public double modifyFormStatMultiplier(StatsData data, String groupName, double multiplier) {
		if (multiplier <= 1.0 || !isReserveFeeding(data)) return multiplier;
		double bonus = ConfigManager.getServerConfig().getRacialSkills().getFrostdemon().getReserveFormBonus();
		return 1.0 + (multiplier - 1.0) * (1.0 + bonus);
	}

	@Override
	public void onSecond(RacialContext ctx) {
		StatsData data = ctx.data();
		GeneralServerConfig.FrostDemonRacialConfig config = ctx.config().getFrostdemon();
		RacialData racialData = data.getRacialData();

		float maxReserve = (float) (data.getMaxEnergy() * config.getReserveMaxRatio());
		float before = racialData.getEnergyReserve();
		boolean wasActive = racialData.isReserveActive();

		boolean consuming = isConsumptionState(data, config);
		racialData.setReserveActive(consuming && before > 0);

		if (racialData.isReserveActive()) {
			float drainPerSecond = maxReserve / config.getReserveDurationSeconds();
			racialData.setEnergyReserve(Math.max(0f, before - drainPerSecond));
		} else if (!consuming
				&& data.getResources().getPowerRelease() <= config.getReserveChargeReleaseThreshold()
				&& isChargeableForm(data, config)) {

			float chargePerSecond = maxReserve / config.getReserveFullChargeSeconds();
			racialData.setEnergyReserve(Math.min(maxReserve, before + chargePerSecond));
		}

		if (racialData.getEnergyReserve() != before || racialData.isReserveActive() != wasActive) {
			NetworkHandler.sendToPlayer(new RacialDataSyncS2C(ctx.player()), ctx.player());
		}
	}

	private static boolean isReserveFeeding(StatsData data) {
		RacialData racialData = data.getRacialData();
		return racialData.isReserveActive() && racialData.getEnergyReserve() > 0;
	}

	private static boolean isConsumptionState(StatsData data, GeneralServerConfig.FrostDemonRacialConfig config) {
		return hasActiveForm(data) && data.getResources().getPowerRelease() >= 100;
	}

	private static boolean isChargeableForm(StatsData data, GeneralServerConfig.FrostDemonRacialConfig config) {
		if (!hasActiveForm(data)) return true;
		String stackForm = data.getCharacter().getActiveStackForm();
		if (stackForm != null && !stackForm.isEmpty()) return false;

		String form = data.getCharacter().getActiveForm();
		for (String allowed : config.getReserveChargeForms()) {
			if (allowed.equalsIgnoreCase(form)) return true;
		}
		return false;
	}

	@Override
	public void onDeath(RacialContext ctx) {
		ctx.data().getRacialData().setEnergyReserve(0f);
		ctx.data().getRacialData().setReserveActive(false);
	}

	@Override
	public void onDimensionChange(RacialContext ctx) {
		ctx.data().getRacialData().setEnergyReserve(0f);
		ctx.data().getRacialData().setReserveActive(false);
	}

	private static boolean hasActiveForm(StatsData data) {
		String form = data.getCharacter().getActiveForm();
		String stackForm = data.getCharacter().getActiveStackForm();
		return (form != null && !form.isEmpty() && !"base".equalsIgnoreCase(form))
				|| (stackForm != null && !stackForm.isEmpty());
	}
}
