package com.dragonminez.common.network.S2C;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceStatsConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncServerConfigS2C {

    private final Map<String, RaceStatsData> raceStats;

    public SyncServerConfigS2C(Map<String, RaceStatsConfig> raceConfigs) {
        this.raceStats = new HashMap<>();
        raceConfigs.forEach((raceName, config) -> {
            this.raceStats.put(raceName, new RaceStatsData(config));
        });
    }

    public SyncServerConfigS2C(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.raceStats = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String raceName = buf.readUtf();
            RaceStatsData data = new RaceStatsData(buf);
            this.raceStats.put(raceName, data);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(raceStats.size());
        raceStats.forEach((raceName, data) -> {
            buf.writeUtf(raceName);
            data.encode(buf);
        });
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ConfigManager.applySyncedServerConfig(raceStats);
        });
        ctx.get().setPacketHandled(true);
    }

    public static class RaceStatsData {
        public int strBase, skpBase, resBase, vitBase, pwrBase, eneBase;
        public double strScaling, skpScaling, stmScaling, defScaling, vitScaling, pwrScaling, eneScaling;

        public RaceStatsData(RaceStatsConfig config) {
            RaceStatsConfig.BaseStats base = config.getBaseStats();
            RaceStatsConfig.StatScaling scaling = config.getStatScaling();

            this.strBase = base.getStrength();
            this.skpBase = base.getStrikePower();
            this.resBase = base.getResistance();
            this.vitBase = base.getVitality();
            this.pwrBase = base.getKiPower();
            this.eneBase = base.getEnergy();

            this.strScaling = scaling.getStrengthScaling();
            this.skpScaling = scaling.getStrikePowerScaling();
            this.stmScaling = scaling.getStaminaScaling();
            this.defScaling = scaling.getDefenseScaling();
            this.vitScaling = scaling.getVitalityScaling();
            this.pwrScaling = scaling.getKiPowerScaling();
            this.eneScaling = scaling.getEnergyScaling();
        }

        public RaceStatsData(FriendlyByteBuf buf) {
            this.strBase = buf.readInt();
            this.skpBase = buf.readInt();
            this.resBase = buf.readInt();
            this.vitBase = buf.readInt();
            this.pwrBase = buf.readInt();
            this.eneBase = buf.readInt();

            this.strScaling = buf.readDouble();
            this.skpScaling = buf.readDouble();
            this.stmScaling = buf.readDouble();
            this.defScaling = buf.readDouble();
            this.vitScaling = buf.readDouble();
            this.pwrScaling = buf.readDouble();
            this.eneScaling = buf.readDouble();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeInt(strBase);
            buf.writeInt(skpBase);
            buf.writeInt(resBase);
            buf.writeInt(vitBase);
            buf.writeInt(pwrBase);
            buf.writeInt(eneBase);

            buf.writeDouble(strScaling);
            buf.writeDouble(skpScaling);
            buf.writeDouble(stmScaling);
            buf.writeDouble(defScaling);
            buf.writeDouble(vitScaling);
            buf.writeDouble(pwrScaling);
            buf.writeDouble(eneScaling);
        }

        public RaceStatsConfig toConfig(String raceName) {
            RaceStatsConfig config = new RaceStatsConfig();
            config.setRaceName(raceName);

            RaceStatsConfig.BaseStats base = config.getBaseStats();
            base.setStrength(strBase);
            base.setStrikePower(skpBase);
            base.setResistance(resBase);
            base.setVitality(vitBase);
            base.setKiPower(pwrBase);
            base.setEnergy(eneBase);

            RaceStatsConfig.StatScaling scaling = config.getStatScaling();
            scaling.setStrengthScaling(strScaling);
            scaling.setStrikePowerScaling(skpScaling);
            scaling.setStaminaScaling(stmScaling);
            scaling.setDefenseScaling(defScaling);
            scaling.setVitalityScaling(vitScaling);
            scaling.setKiPowerScaling(pwrScaling);
            scaling.setEnergyScaling(eneScaling);

            return config;
        }
    }
}

