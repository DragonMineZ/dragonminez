package com.dragonminez.common.network.C2S;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
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

				int maxStats = ConfigManager.getServerConfig().getGameplay().getMaxStatValue();
				int currentStat = getCurrentStat(data, statNameStr);

				if (currentStat >= maxStats) return;

				int availableTPs = data.getResources().getTrainingPoints();
				if (availableTPs <= 0) return;

				double multiplier = ConfigManager.getServerConfig().getGameplay().getTpCostMultiplier();
				int baseCost = (int) Math.round((data.getLevel() * multiplier) * multiplier * 4.0);

				int statsCanIncrease = Math.min(msg.multiplier, maxStats - currentStat);

				int statsToIncrease = data.calculateStatIncrease(baseCost, statsCanIncrease, availableTPs, maxStats, multiplier);

				if (statsToIncrease <= 0) return;

				int finalIncrease = Math.min(statsToIncrease, maxStats - currentStat);

				int tpCost = data.calculateRecursiveCost(finalIncrease, baseCost, maxStats, multiplier);

				if (tpCost > availableTPs) return;

				increaseStat(data, player, statNameStr, finalIncrease);
				data.getResources().removeTrainingPoints(tpCost);
			});
		});
		ctx.get().setPacketHandled(true);
	}

	private static int getCurrentStat(StatsData data, String statName) {
		return switch (statName.toUpperCase()) {
			case "STR" -> data.getStats().getStrength();
			case "SKP" -> data.getStats().getStrikePower();
			case "RES" -> data.getStats().getResistance();
			case "VIT" -> data.getStats().getVitality();
			case "PWR" -> data.getStats().getKiPower();
			case "ENE" -> data.getStats().getEnergy();
			default -> 0;
		};
	}

	private static void increaseStat(StatsData data, ServerPlayer player, String statName, int amount) {
		switch (statName.toUpperCase()) {
			case "STR" -> data.getStats().addStrength(amount);
			case "SKP" -> data.getStats().addStrikePower(amount);
			case "RES" -> {
				int oldMaxStamina = data.getMaxStamina();

				data.getStats().addResistance(amount);

				int newMaxStamina = data.getMaxStamina();
				if (newMaxStamina > oldMaxStamina) {
					data.getResources().addStamina(newMaxStamina - oldMaxStamina);
				}
			}
			case "VIT" -> {
				float oldMaxHealth = data.getMaxHealth();

				data.getStats().addVitality(amount);

				float newMaxHealth = data.getMaxHealth();
				if (newMaxHealth > oldMaxHealth) {
					player.heal(newMaxHealth - oldMaxHealth);
				}
			}
			case "PWR" -> data.getStats().addKiPower(amount);
			case "ENE" -> {
				int oldMaxEnergy = data.getMaxEnergy();

				data.getStats().addEnergy(amount);

				int newMaxEnergy = data.getMaxEnergy();
				if (newMaxEnergy > oldMaxEnergy) {
					data.getResources().addEnergy(newMaxEnergy - oldMaxEnergy);
				}
			}
		}
	}
}