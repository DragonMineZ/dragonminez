package com.dragonminez.common.racial.impl;

import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.init.entities.PunchMachineEntity;
import com.dragonminez.common.init.entities.namek.NamekTraderEntity;
import com.dragonminez.common.init.entities.namek.NamekWarriorEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.racial.RacialAbility;
import com.dragonminez.common.racial.RacialContext;
import com.dragonminez.common.racial.RacialData;
import com.dragonminez.common.racial.RacialStatUtil;
import com.dragonminez.common.racial.capture.CaptureRequest;
import com.dragonminez.common.racial.capture.RacialCapture;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.character.SecondaryStatEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NamekAssimilation implements RacialAbility {

	@Override
	public String id() {
		return "namekian";
	}

	@Override
	public boolean hasActiveAction() {
		return true;
	}

	@Override
	public boolean canActivate(RacialContext ctx) {
		ServerPlayer player = ctx.player();
		StatsData data = ctx.data();
		GeneralServerConfig.NamekianRacialConfig config = ctx.config().getNamekian();
		if (!config.getEnabled()) return false;

		if (data.getCooldowns().hasCooldown(Cooldowns.ASSIMILATION)) {
			int secondsLeft = data.getCooldowns().getCooldown(Cooldowns.ASSIMILATION) / 20;
			player.displayClientMessage(Component.translatable("message.dragonminez.racial.cooldown", secondsLeft), true);
			return false;
		}

		if (data.getRacialData().getAssimilations().size() >= config.getAssimilationAmount()) {
			player.displayClientMessage(Component.translatable("message.dragonminez.racial.limit_reached"), true);
			return false;
		}

		LivingEntity target = RacialCapture.findTarget(player, 3.0);
		return target != null && isEligibleTarget(target, player, config);
	}

	private static boolean isEligibleTarget(LivingEntity target, ServerPlayer player, GeneralServerConfig.NamekianRacialConfig config) {
		if (target instanceof MastersEntity || target instanceof PunchMachineEntity) return false;
		if (TargetHelper.getRelation(player, target) == TargetHelper.Relation.FRIENDLY) return false;

		if (target instanceof ServerPlayer targetPlayer) {
			return StatsProvider.get(StatsCapability.INSTANCE, targetPlayer)
					.map(tData -> tData.getCharacter().getRaceName().equals("namekian"))
					.orElse(false);
		}
		if (!config.getAssimilationOnNamekNpcs()) return false;
		return target instanceof NamekWarriorEntity || target instanceof NamekTraderEntity
				|| target.getName().getString().contains("Piccolo");
	}

	@Override
	public int chargeSeconds(RacialContext ctx) {
		return 4;
	}

	@Override
	public boolean onActivate(RacialContext ctx) {
		ServerPlayer player = ctx.player();
		StatsData data = ctx.data();
		GeneralServerConfig.NamekianRacialConfig config = ctx.config().getNamekian();

		LivingEntity target = RacialCapture.findTarget(player, 3.0);
		if (target == null || !isEligibleTarget(target, player, config)) {
			player.displayClientMessage(Component.translatable("message.dragonminez.racial.namek.invalid_target"), true);
			return true;
		}

		data.getCooldowns().setCooldown(Cooldowns.ASSIMILATION, config.getAssimilationCooldownSeconds() * 20);

		if (target instanceof ServerPlayer targetPlayer) {
			boolean knockedDown = StatsProvider.get(StatsCapability.INSTANCE, targetPlayer)
					.map(tData -> tData.getStatus().isKnockedDown())
					.orElse(false);

			if (knockedDown) applyAssimilation(player, data, target);
			else CaptureRequest.send(player, targetPlayer, config.getAssimilationRequestTimeoutSeconds());
		} else {
			applyAssimilation(player, data, target);
		}

		return true;
	}

	@Override
	public void onDeath(RacialContext ctx) {
		List<RacialData.AssimilationSlot> slots = ctx.data().getRacialData().getAssimilations();
		if (slots.isEmpty()) return;

		RacialData.AssimilationSlot lost = slots.remove(slots.size() - 1);
		for (Map.Entry<String, Integer> entry : lost.grantedStats().entrySet()) {
			ctx.data().getBonusStats().removeBonusSplit(entry.getKey(), lost.bonusName());
		}
		ctx.data().getRacialData().removeOwnedBonusName(lost.bonusName());
	}

	@Override
	public void onDamageTakenPost(RacialContext ctx, float rawDamage) {
		StatsData data = ctx.data();
		if (!data.getCooldowns().hasCooldown(Cooldowns.NAMEK_REGEN_ACTIVE)) return;

		data.getCooldowns().removeCooldown(Cooldowns.NAMEK_REGEN_ACTIVE);
		int fullCooldownTicks = ctx.config().getNamekian().getRegenCooldownSeconds() * 20;
		data.getCooldowns().setCooldown(Cooldowns.NAMEK_REGEN, fullCooldownTicks / 2);
	}

	@Override
	public void onSecond(RacialContext ctx) {
		ServerPlayer player = ctx.player();
		StatsData data = ctx.data();
		GeneralServerConfig.NamekianRacialConfig config = ctx.config().getNamekian();

		if (data.getCooldowns().hasCooldown(Cooldowns.NAMEK_REGEN_ACTIVE)) {
			int channelSeconds = Math.max(1, config.getRegenChannelSeconds());
			player.heal((float) (player.getMaxHealth() * config.getRegenHealthRatio() / channelSeconds));
			data.getResources().removeEnergy((float) (data.getMaxEnergy() * config.getRegenEnergyCost() / channelSeconds));
			data.getResources().removeStamina((float) (data.getMaxStamina() * config.getRegenStaminaCost() / channelSeconds));
		}

		if (player.isUnderWater()) refreshWaterRegenBuff(data, config);
	}

	public static void startRegen(ServerPlayer player, StatsData data) {
		GeneralServerConfig.NamekianRacialConfig config = ConfigManager.getServerConfig().getRacialSkills().getNamekian();
		if (!config.getEnabled()) return;
		if (data.getCooldowns().hasCooldown(Cooldowns.NAMEK_REGEN)) return;
		if (data.getCooldowns().hasCooldown(Cooldowns.NAMEK_REGEN_ACTIVE)) return;

		data.getCooldowns().setCooldown(Cooldowns.NAMEK_REGEN_ACTIVE, config.getRegenChannelSeconds() * 20);
		data.getCooldowns().setCooldown(Cooldowns.NAMEK_REGEN, config.getRegenCooldownSeconds() * 20);
	}

	public static void refreshWaterRegenBuff(StatsData data, GeneralServerConfig.NamekianRacialConfig config) {
		double factor = 1.0 + config.getWaterRegenBonus();
		int durationTicks = config.getWaterRegenSeconds() * 20;
		data.getSecondaryStatEffects().apply(SecondaryStatEffects.HP_REGEN, factor, durationTicks);
		data.getSecondaryStatEffects().apply(SecondaryStatEffects.ENE_REGEN, factor, durationTicks);
		data.getSecondaryStatEffects().apply(SecondaryStatEffects.STM_REGEN, factor, durationTicks);
	}

	public static void applyAssimilation(ServerPlayer player, StatsData data, LivingEntity target) {
		GeneralServerConfig.NamekianRacialConfig config = ConfigManager.getServerConfig().getRacialSkills().getNamekian();

		double boostMult = config.getAssimilationStatBoost();
		int maxBonus = ConfigManager.getServerConfig().getGameplay().getMaxValue();
		int slotIndex = data.getRacialData().getAssimilations().size() + 1;
		String bonusName = "Assimilation_" + slotIndex;

		Map<String, Integer> grantedStats = new HashMap<>();
		for (String statKey : config.getAssimilationBoosts()) {
			int currentStat = RacialStatUtil.getStat(data, statKey);
			int bonus = (int) Math.max(1, Math.min(maxBonus, currentStat * boostMult));
			data.getBonusStats().addBonusSplit(statKey, bonusName, "+", bonus, true);
			grantedStats.put(statKey, bonus);
		}
		data.getRacialData().addOwnedBonusName(bonusName);
		data.getRacialData().addAssimilation(new RacialData.AssimilationSlot(
				bonusName, target.getUUID(), target.getName().getString(), grantedStats, player.level().getGameTime()));

		RacialCapture.finalizeKill(player, target, config.getAssimilationHealthRegen());
		player.displayClientMessage(Component.translatable("message.dragonminez.racial.namek.success"), true);
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
	}
}
