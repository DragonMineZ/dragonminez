package com.dragonminez.common.racial.impl;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.racial.LethalContext;
import com.dragonminez.common.racial.RacialAbility;
import com.dragonminez.common.racial.RacialContext;
import com.dragonminez.common.racial.RacialData;
import com.dragonminez.common.racial.RacialStatUtil;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Cooldowns;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;

public class SaiyanZenkai implements RacialAbility {

	@Override
	public String id() {
		return "saiyan";
	}

	@Override
	public void onDamageTakenPost(RacialContext ctx, float damageTaken) {
		RacialData racialData = ctx.data().getRacialData();
		GeneralServerConfig.SaiyanRacialConfig config = ctx.config().getSaiyan();

		if (damageTaken > racialData.getPeakRawHitWindow()) racialData.setPeakRawHitWindow(damageTaken);
		racialData.setPeakRawHitTicksLeft(config.getPeakWindowSeconds() * 20);
	}

	@Override
	public boolean onLethalDamage(RacialContext ctx, LethalContext lc) {
		ServerPlayer player = ctx.player();
		StatsData data = ctx.data();
		RacialData racialData = data.getRacialData();
		GeneralServerConfig.SaiyanRacialConfig config = ctx.config().getSaiyan();

		if (!ctx.config().getEnableRacialSkills() || !config.getEnabled()) return false;
		if (lc.attacker() == null) return false;
		if (lc.source().is(DamageTypes.FELL_OUT_OF_WORLD) || lc.source().is(DamageTypes.GENERIC_KILL)) return false;

		float maxHealth = player.getMaxHealth();

		String blocked = blockingReason(data, config, maxHealth, lc);
		if (blocked != null) {

			player.displayClientMessage(Component.translatable("message.dragonminez.racial.zenkai.blocked." + blocked), true);
			LogUtil.debug(Env.SERVER, "Zenkai skipped for {}: {} (level {}/{}, finalDamage {}, maxHealth {})",
					player.getGameProfile().getName(), blocked, data.getLevel(), config.getMinLevel(), lc.finalDamage(), maxHealth);
			return false;
		}

		float peakHit = Math.max(racialData.getPeakRawHitWindow(), lc.finalDamage());
		double hitRatio = peakHit / maxHealth;
		double potency = Math.max(0.0, Math.min(1.0, hitRatio / config.getMaxBuffHitRatio()));
		double buffPct = lerp(config.getMinBuffPct(), config.getMaxBuffPct(), potency);
		boolean isMaxBuff = hitRatio >= config.getMaxBuffHitRatio();

		applyBuff(data, config, buffPct, isMaxBuff, potency, player.level().getGameTime());

		int knockoutTicks = config.getKnockoutSeconds() * 20;
		data.getStatus().setKnockedDown(true);
		data.getCooldowns().setCooldown(Cooldowns.KNOCKDOWN_DURATION, knockoutTicks);
		data.getCooldowns().setCooldown(Cooldowns.KNOCKDOWN_INVULN, knockoutTicks);
		data.getCooldowns().setCooldown(Cooldowns.ZENKAI_KNOCKOUT, knockoutTicks);
		data.getCooldowns().setCooldown(Cooldowns.ZENKAI, config.getCooldownSeconds() * 20);
		racialData.setPeakRawHitWindow(0f);
		racialData.setPeakRawHitTicksLeft(0);

		player.displayClientMessage(Component.translatable("message.dragonminez.racial.zenkai.used"), true);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.TRANSFORM_ON.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
		player.addEffect(new MobEffectInstance(MainEffects.SAIYAN_PASSIVE.get(), config.getCooldownSeconds() * 20, 0, false, false, true));
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);

		return true;
	}

	private static String blockingReason(StatsData data, GeneralServerConfig.SaiyanRacialConfig config, float maxHealth, LethalContext lc) {
		if (data.getLevel() < config.getMinLevel()) return "level";
		if (data.getCooldowns().hasCooldown(Cooldowns.ZENKAI)) return "cooldown";
		if (lc.finalDamage() > maxHealth * config.getOverkillThreshold()) return "overkill";
		return null;
	}

	private static void applyBuff(StatsData data, GeneralServerConfig.SaiyanRacialConfig config,
								   double buffPct, boolean isMaxBuff, double potency, long gameTime) {
		RacialData racialData = data.getRacialData();
		boolean permanent = isMaxBuff && racialData.getZenkaiPermanentUses() < config.getPermanentMaxBuffs();

		String bonusName = permanent
				? "Zenkai_" + (racialData.getZenkaiPermanentUses() + 1)
				: "Zenkai_Temp_" + gameTime;

		for (String stat : config.getBuffStats()) {
			int currentStat = RacialStatUtil.getStat(data, stat);
			int bonus = (int) Math.max(1, currentStat * buffPct);
			data.getBonusStats().addBonusSplit(stat, bonusName, "+", bonus, true);
		}
		racialData.addOwnedBonusName(bonusName);

		if (permanent) {
			racialData.setZenkaiPermanentUses(racialData.getZenkaiPermanentUses() + 1);
		} else {
			racialData.setTempZenkaiBuffName(bonusName);
			int durationSeconds = (int) lerp(config.getTempBuffMinSeconds(), config.getTempBuffMaxSeconds(), potency);
			data.getCooldowns().setCooldown(Cooldowns.ZENKAI_TEMP_BUFF, durationSeconds * 20);
		}

		if (isMaxBuff) {
			double newBonus = Math.min(config.getReleaseBonusCap(), racialData.getZenkaiReleaseBonus() + config.getReleaseBonusPerZenkai());
			racialData.setZenkaiReleaseBonus(newBonus);
		}
	}

	@Override
	public double maxReleaseMultiplier(StatsData data) {
		return 1.0 + data.getRacialData().getZenkaiReleaseBonus();
	}

	@Override
	public void onSecond(RacialContext ctx) {
		ServerPlayer player = ctx.player();
		StatsData data = ctx.data();
		GeneralServerConfig.SaiyanRacialConfig config = ctx.config().getSaiyan();

		if (data.getCooldowns().hasCooldown(Cooldowns.ZENKAI_KNOCKOUT)) {
			int knockoutSeconds = Math.max(1, config.getKnockoutSeconds());
			player.heal((float) (player.getMaxHealth() * config.getKnockoutHealthRegen() / knockoutSeconds));
			data.getResources().addStamina((float) (data.getMaxStamina() * config.getKnockoutStaminaRegen() / knockoutSeconds));
			data.getResources().addEnergy((float) (data.getMaxEnergy() * config.getKnockoutEnergyRegen() / knockoutSeconds));
		}

		RacialData racialData = data.getRacialData();
		if (racialData.getPeakRawHitTicksLeft() > 0) {
			int remaining = racialData.getPeakRawHitTicksLeft() - 20;
			racialData.setPeakRawHitTicksLeft(Math.max(0, remaining));
			if (remaining <= 0) racialData.setPeakRawHitWindow(0f);
		}

		if (!racialData.getTempZenkaiBuffName().isEmpty() && !data.getCooldowns().hasCooldown(Cooldowns.ZENKAI_TEMP_BUFF)) {
			for (String stat : config.getBuffStats()) {
				data.getBonusStats().removeBonusSplit(stat, racialData.getTempZenkaiBuffName());
			}
			racialData.removeOwnedBonusName(racialData.getTempZenkaiBuffName());
			racialData.setTempZenkaiBuffName("");
			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
		}
	}

	private static double lerp(double min, double max, double t) {
		return min + (max - min) * t;
	}
}
