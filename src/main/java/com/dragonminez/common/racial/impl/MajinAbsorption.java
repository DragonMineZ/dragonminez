package com.dragonminez.common.racial.impl;

import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.init.entities.PunchMachineEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.racial.RacialAbility;
import com.dragonminez.common.racial.RacialContext;
import com.dragonminez.common.racial.RacialData;
import com.dragonminez.common.racial.RacialStatUtil;
import com.dragonminez.common.racial.capture.RacialCapture;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.techniques.KiAttackData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MajinAbsorption implements RacialAbility {

	@Override
	public String id() {
		return "majin";
	}

	@Override
	public boolean hasActiveAction() {
		return true;
	}

	@Override
	public boolean canActivate(RacialContext ctx) {
		ServerPlayer player = ctx.player();
		StatsData data = ctx.data();
		GeneralServerConfig.MajinRacialConfig config = ctx.config().getMajin();
		if (!config.getEnabled()) return false;

		if (data.getCooldowns().hasCooldown(Cooldowns.ABSORPTION)) {
			int secondsLeft = data.getCooldowns().getCooldown(Cooldowns.ABSORPTION) / 20;
			player.displayClientMessage(Component.translatable("message.dragonminez.racial.cooldown", secondsLeft), true);
			return false;
		}

		if (effectiveSlotsUsed(data) >= config.getAbsorptionAmount()) {
			player.displayClientMessage(Component.translatable("message.dragonminez.racial.limit_reached"), true);
			return false;
		}

		LivingEntity target = RacialCapture.findTarget(player, 8.0);
		return target != null && isEligibleTarget(target, player, config);
	}

	private static boolean isEligibleTarget(LivingEntity target, ServerPlayer player, GeneralServerConfig.MajinRacialConfig config) {
		if (target instanceof MastersEntity || target instanceof PunchMachineEntity) return false;
		if (TargetHelper.getRelation(player, target) == TargetHelper.Relation.FRIENDLY) return false;
		return target instanceof ServerPlayer || (target instanceof Mob && config.getAbsorptionOnMobs());
	}

	@Override
	public int chargeSeconds(RacialContext ctx) {
		return 4;
	}

	@Override
	public boolean onActivate(RacialContext ctx) {
		ServerPlayer player = ctx.player();
		StatsData data = ctx.data();
		GeneralServerConfig.MajinRacialConfig config = ctx.config().getMajin();

		LivingEntity target = RacialCapture.findTarget(player, 8.0);
		if (target == null || !isEligibleTarget(target, player, config)) return true;

		boolean knockedDown = target instanceof ServerPlayer targetPlayer && StatsProvider.get(StatsCapability.INSTANCE, targetPlayer)
				.map(tData -> tData.getStatus().isKnockedDown())
				.orElse(false);

		if (!knockedDown && !RacialCapture.isStrongerThan(player, data, target)) {
			if (!player.isCreative()) {
				player.displayClientMessage(Component.translatable("message.dragonminez.racial.target_too_strong"), true);
				return true;
			}
		}

		data.getCooldowns().setCooldown(Cooldowns.ABSORPTION, config.getAbsorptionCooldownSeconds() * 20);
		applyAbsorption(player, data, target);
		return true;
	}

	@Override
	public void onDeath(RacialContext ctx) {
		List<RacialData.AbsorptionSlot> slots = ctx.data().getRacialData().getAbsorptions();
		for (RacialData.AbsorptionSlot slot : slots) {
			for (Map.Entry<String, Integer> entry : slot.grantedStats().entrySet()) {
				ctx.data().getBonusStats().removeBonusSplit(entry.getKey(), slot.bonusName());
			}
			ctx.data().getRacialData().removeOwnedBonusName(slot.bonusName());
		}
		slots.clear();
	}

	public static int effectiveSlotsUsed(StatsData data) {
		int ejectCooldowns = 0;
		for (Map.Entry<String, Integer> entry : data.getCooldowns().getAllCooldowns().entrySet()) {
			if (entry.getKey().startsWith("AbsorptionSlot_") && entry.getValue() > 0) ejectCooldowns++;
		}
		return data.getRacialData().getAbsorptions().size() + ejectCooldowns;
	}

	private static void applyAbsorption(ServerPlayer player, StatsData data, LivingEntity target) {
		GeneralServerConfig.MajinRacialConfig config = ConfigManager.getServerConfig().getRacialSkills().getMajin();
		double ratio = config.getAbsorptionStatCopy();
		int absorptionCap = data.getConfiguredMaxTotalStats();
		int slotIndex = data.getRacialData().getAbsorptionSlotCounter() + 1;
		String bonusName = "Absorption_" + slotIndex;
		Map<String, Integer> grantedStats = new HashMap<>();

		if (target instanceof ServerPlayer targetPlayer) {
			StatsProvider.get(StatsCapability.INSTANCE, targetPlayer).ifPresent(targetData -> {
				for (String stat : config.getAbsorptionBoosts()) {
					int bonus = RacialCapture.cappedAbsorptionBonus(RacialStatUtil.getStat(targetData, stat), ratio, absorptionCap);
					data.getBonusStats().addBonusSplit(stat, bonusName, "+", bonus, true);
					grantedStats.put(stat, bonus);
				}
			});
		} else if (target instanceof Mob) {
			int bonus = RacialCapture.cappedAbsorptionBonus(target.getMaxHealth(), ratio, absorptionCap);
			for (String stat : config.getAbsorptionBoosts()) {
				data.getBonusStats().addBonusSplit(stat, bonusName, "+", bonus, true);
				grantedStats.put(stat, bonus);
			}
		} else {
			return;
		}

		data.getRacialData().setAbsorptionSlotCounter(slotIndex);
		data.getRacialData().addOwnedBonusName(bonusName);
		data.getRacialData().addAbsorption(new RacialData.AbsorptionSlot(
				bonusName, target.getUUID(), target.getName().getString(), grantedStats, player.level().getGameTime()));

		RacialCapture.finalizeKill(player, target, config.getAbsorptionHealthRegen());
		player.displayClientMessage(Component.translatable("message.dragonminez.racial.majin.success"), true);
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
	}

	public static float applyFoodHealBonus(StatsData data, float healAmount) {
		if (healAmount <= 0 || data == null) return healAmount;
		GeneralServerConfig.RacialSkillsConfig racial = ConfigManager.getServerConfig().getRacialSkills();
		if (!racial.getEnableRacialSkills() || !"majin".equals(data.getCharacter().getRaceName())) return healAmount;
		return (float) (healAmount * (1.0 + racial.getMajin().getFoodHealBonus()));
	}

	public static double applyKiHealBonus(StatsData dealerData, StatsData receiverData, double amount) {
		if (amount <= 0) return amount;
		GeneralServerConfig.RacialSkillsConfig racial = ConfigManager.getServerConfig().getRacialSkills();
		if (!racial.getEnableRacialSkills()) return amount;

		double dealtBonus = dealerData != null && "majin".equals(dealerData.getCharacter().getRaceName())
				? racial.getMajin().getKiHealDealtBonus() : 0.0;
		double receivedBonus = receiverData != null && "majin".equals(receiverData.getCharacter().getRaceName())
				? racial.getMajin().getKiHealReceivedBonus() : 0.0;

		return amount * (1.0 + dealtBonus + receivedBonus);
	}

	public static double healTechniqueCooldownMultiplier(StatsData data, KiAttackData kiAttack) {
		if (data == null || kiAttack == null) return 1.0;
		if (kiAttack.getEffectiveUtility() != KiAttackData.Utility.HEAL) return 1.0;
		GeneralServerConfig.RacialSkillsConfig racial = ConfigManager.getServerConfig().getRacialSkills();
		if (!racial.getEnableRacialSkills() || !"majin".equals(data.getCharacter().getRaceName())) return 1.0;
		return 1.0 - racial.getMajin().getHealTechniqueCooldownReduction();
	}

	public static int applyHealTechniqueCostReduction(StatsData data, KiAttackData kiAttack, int cost) {
		if (data == null || kiAttack == null || cost <= 0) return cost;
		if (kiAttack.getEffectiveUtility() != KiAttackData.Utility.HEAL) return cost;
		GeneralServerConfig.RacialSkillsConfig racial = ConfigManager.getServerConfig().getRacialSkills();
		if (!racial.getEnableRacialSkills() || !"majin".equals(data.getCharacter().getRaceName())) return cost;
		return (int) Math.round(cost * (1.0 - racial.getMajin().getHealTechniqueCostReduction()));
	}

	public static void ejectSlot(ServerPlayer player, StatsData data, int index) {
		List<RacialData.AbsorptionSlot> slots = data.getRacialData().getAbsorptions();
		if (index < 0 || index >= slots.size()) return;

		RacialData.AbsorptionSlot slot = slots.remove(index);
		for (Map.Entry<String, Integer> entry : slot.grantedStats().entrySet()) {
			data.getBonusStats().removeBonusSplit(entry.getKey(), slot.bonusName());
		}
		data.getRacialData().removeOwnedBonusName(slot.bonusName());

		int cooldownSeconds = ConfigManager.getServerConfig().getRacialSkills().getMajin().getSlotEjectCooldownSeconds();
		data.getCooldowns().setCooldown("AbsorptionSlot_" + slot.bonusName(), cooldownSeconds * 20);
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
	}
}
