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

    private final String statName;
    private final int multiplier;

    public IncreaseStatC2S(String statName, int multiplier) {
        this.statName = statName;
        this.multiplier = multiplier;
    }

    public static void encode(IncreaseStatC2S msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.statName);
        buf.writeInt(msg.multiplier);
    }

    public static IncreaseStatC2S decode(FriendlyByteBuf buf) {
        return new IncreaseStatC2S(
                buf.readUtf(),
                buf.readInt()
        );
    }

    public static void handle(IncreaseStatC2S msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                int maxStats = ConfigManager.getServerConfig().getGameplay().getMaxStatValue();
                int currentStat = getCurrentStat(data, msg.statName);

                if (currentStat >= maxStats) return;

                int availableTPs = data.getResources().getTrainingPoints();
                if (availableTPs <= 0) return;

                double multiplier = ConfigManager.getServerConfig().getGameplay().getTpsMultiplier();
                int baseCost = (int) Math.round((data.getLevel() * multiplier) * multiplier * 1.5);

                int statsCanIncrease = Math.min(msg.multiplier, maxStats - currentStat);

                int statsToIncrease = data.calculateStatIncrease(baseCost, statsCanIncrease, availableTPs, maxStats, multiplier);

                if (statsToIncrease <= 0) return;

                int finalIncrease = Math.min(statsToIncrease, maxStats - currentStat);

                int tpCost = data.calculateRecursiveCost(finalIncrease, baseCost, maxStats, multiplier);

                if (tpCost > availableTPs) return;

                increaseStat(data, player, msg.statName, finalIncrease);
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
                double stmScaling = data.getStatScaling("STM");
                int staminaIncrease = (int) (amount * stmScaling);

                data.getStats().addResistance(amount);
                data.getResources().addStamina(staminaIncrease);
            }
            case "VIT" -> {
                double vitScaling = data.getStatScaling("VIT");
                int healthIncrease = (int) (amount * vitScaling);

                data.getStats().addVitality(amount);
                player.heal(healthIncrease);
            }
            case "PWR" -> data.getStats().addKiPower(amount);
            case "ENE" -> {
                double eneScaling = data.getStatScaling("ENE");
                int energyIncrease = (int) (amount * eneScaling);

                data.getStats().addEnergy(amount);
                data.getResources().addEnergy(energyIncrease);
            }
        }
    }
}

