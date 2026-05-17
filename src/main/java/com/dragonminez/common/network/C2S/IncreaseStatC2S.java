package com.dragonminez.common.network.C2S;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.events.players.StatsEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class IncreaseStatC2S {

	public enum StatType {
		STR, SKP, RES, VIT, PWR, ENE
	}

	private final StatType statType;
	private final int multiplier;

	public IncreaseStatC2S(StatType statType, int multiplier) {
		this.statType = statType;
		this.multiplier = multiplier;
	}

	public static void encode(IncreaseStatC2S msg, FriendlyByteBuf buf) {
		buf.writeEnum(msg.statType);
		buf.writeInt(msg.multiplier);
	}

	public static IncreaseStatC2S decode(FriendlyByteBuf buf) {
		return new IncreaseStatC2S(
				buf.readEnum(StatType.class),
				buf.readInt()
		);
	}

	public static void handle(IncreaseStatC2S msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				String statNameStr = msg.statType.name();

				float availableTPs = data.getResources().getTrainingPoints();
				if (availableTPs <= 0) return;

				int maxStats = data.getConfiguredMaxValue();
				boolean maxByLevel = ConfigManager.getServerConfig().getGameplay().getMaxLevelValueInsteadOfStats();
				int statsCanIncrease;

				if (maxByLevel) {
					statsCanIncrease = Math.min(msg.multiplier, data.getRemainingAssignableStats());
				} else {
					int currentStat = data.getCurrentStatValue(statNameStr);
					statsCanIncrease = Math.min(msg.multiplier, Math.max(0, maxStats - currentStat));
				}

				statsCanIncrease = data.getMaxAllowedIncreaseForStat(statNameStr, statsCanIncrease);
				if (statsCanIncrease <= 0) return;

				int statsToIncrease = data.calculateStatIncrease(statsCanIncrease, availableTPs, maxStats);

				if (statsToIncrease <= 0) return;

				int finalIncrease = data.getMaxAllowedIncreaseForStat(statNameStr, statsToIncrease);
				if (finalIncrease <= 0) return;

				int tpCost = data.calculateRecursiveCost(finalIncrease, maxStats);

				if (tpCost > availableTPs) return;

				increaseStat(data, player, statNameStr, finalIncrease);
				data.getResources().removeTrainingPoints(tpCost);
			});
		});
		ctx.get().setPacketHandled(true);
	}


	private static void increaseStat(StatsData data, ServerPlayer player, String statName, int amount) {
		switch (statName.toUpperCase()) {
			case "STR" -> data.getStats().addStrength(amount);
			case "SKP" -> data.getStats().addStrikePower(amount);
			case "RES" -> {
				float oldMaxStamina = data.getMaxStamina();
				data.getStats().addResistance(amount);
				float newMaxStamina = data.getMaxStamina();
				if (newMaxStamina > oldMaxStamina) data.getResources().addStamina(newMaxStamina - oldMaxStamina);
			}
			case "VIT" -> {
				float oldHealthBonus = data.getHealthBonus();
				data.getStats().addVitality(amount);
				float newHealthBonus = data.getHealthBonus();
				float healthDiff = newHealthBonus - oldHealthBonus;

				if (healthDiff > 0) {
					var attribute = player.getAttribute(Attributes.MAX_HEALTH);
					if (attribute != null) {
						attribute.removePermanentModifier(StatsEvents.DMZ_HEALTH_MODIFIER_UUID);
						attribute.addPermanentModifier(new AttributeModifier(StatsEvents.DMZ_HEALTH_MODIFIER_UUID, "DMZ Health", newHealthBonus, AttributeModifier.Operation.ADDITION));
					}
					player.heal(healthDiff);
				}
			}
			case "PWR" -> data.getStats().addKiPower(amount);
			case "ENE" -> {
				float oldMaxEnergy = data.getMaxEnergy();
				data.getStats().addEnergy(amount);
				float newMaxEnergy = data.getMaxEnergy();
				if (newMaxEnergy > oldMaxEnergy) data.getResources().addEnergy(newMaxEnergy - oldMaxEnergy);
			}
		}
	}
}