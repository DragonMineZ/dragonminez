package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
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

				int pendingAP = data.getResources().getPendingAttributePoints();
				float availableTPs = data.getResources().getTrainingPoints();
				if (pendingAP <= 0 && availableTPs <= 0) return;

				int maxStats = data.getConfiguredMaxValue();

				int statsCanIncrease = data.getMaxAllowedIncreaseForStat(statNameStr, msg.multiplier);
				if (statsCanIncrease <= 0) return;

				int apToUse = Math.min(pendingAP, statsCanIncrease);
				boolean changed = false;
				if (apToUse > 0) {
					increaseStat(data, player, statNameStr, apToUse);
					data.getResources().removePendingAttributePoints(apToUse);
					changed = true;
				}

				int remaining = statsCanIncrease - apToUse;
				if (remaining > 0 && availableTPs > 0) {
					int remainingCap = data.getMaxAllowedIncreaseForStat(statNameStr, remaining);
					if (remainingCap > 0) {
						int tpStats = data.calculateStatIncrease(remainingCap, availableTPs, maxStats);
						if (tpStats > 0) {
							int tpCost = data.calculateRecursiveCost(tpStats, maxStats);
							if (tpCost <= availableTPs) {
								increaseStat(data, player, statNameStr, tpStats);
								data.getResources().removeTrainingPoints(tpCost);
								changed = true;
							}
						}
					}
				}

				if (changed) NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
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