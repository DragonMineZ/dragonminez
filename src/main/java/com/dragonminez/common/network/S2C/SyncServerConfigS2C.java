package com.dragonminez.common.network.S2C;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncServerConfigS2C {

    private final Map<String, RaceStatsData> raceStats;
    private final Map<String, RaceCharacterData> raceCharacters;

    public SyncServerConfigS2C(Map<String, RaceStatsConfig> statsConfigs, Map<String, RaceCharacterConfig> characterConfigs) {
        this.raceStats = new HashMap<>();
        statsConfigs.forEach((raceName, config) -> {
            this.raceStats.put(raceName, new RaceStatsData(config));
        });

        this.raceCharacters = new HashMap<>();
        characterConfigs.forEach((raceName, config) -> {
            this.raceCharacters.put(raceName, new RaceCharacterData(config));
        });
    }

    public SyncServerConfigS2C(FriendlyByteBuf buf) {
        // Leer stats
        int statsSize = buf.readInt();
        this.raceStats = new HashMap<>();
        for (int i = 0; i < statsSize; i++) {
            String raceName = buf.readUtf();
            RaceStatsData data = new RaceStatsData(buf);
            this.raceStats.put(raceName, data);
        }

        // Leer character configs
        int characterSize = buf.readInt();
        this.raceCharacters = new HashMap<>();
        for (int i = 0; i < characterSize; i++) {
            String raceName = buf.readUtf();
            RaceCharacterData data = new RaceCharacterData(buf);
            this.raceCharacters.put(raceName, data);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        // Escribir stats
        buf.writeInt(raceStats.size());
        raceStats.forEach((raceName, data) -> {
            buf.writeUtf(raceName);
            data.encode(buf);
        });

        // Escribir character configs
        buf.writeInt(raceCharacters.size());
        raceCharacters.forEach((raceName, data) -> {
            buf.writeUtf(raceName);
            data.encode(buf);
        });
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ConfigManager.applySyncedServerConfig(raceStats, raceCharacters);
        });
        ctx.get().setPacketHandled(true);
    }

    public static class RaceStatsData {
        public ClassStatsData warrior;
        public ClassStatsData spiritualist;
        public ClassStatsData martialArtist;

        public RaceStatsData(RaceStatsConfig config) {
            this.warrior = new ClassStatsData(config.getWarrior());
            this.spiritualist = new ClassStatsData(config.getSpiritualist());
            this.martialArtist = new ClassStatsData(config.getMartialArtist());
        }

        public RaceStatsData(FriendlyByteBuf buf) {
            this.warrior = new ClassStatsData(buf);
            this.spiritualist = new ClassStatsData(buf);
            this.martialArtist = new ClassStatsData(buf);
        }

        public void encode(FriendlyByteBuf buf) {
            warrior.encode(buf);
            spiritualist.encode(buf);
            martialArtist.encode(buf);
        }

        public RaceStatsConfig toConfig(String raceName) {
            RaceStatsConfig config = new RaceStatsConfig();

            config.setWarrior(warrior.toClassStats());
            config.setSpiritualist(spiritualist.toClassStats());
            config.setMartialArtist(martialArtist.toClassStats());

            return config;
        }
    }

    public static class ClassStatsData {
        public int strBase, skpBase, resBase, vitBase, pwrBase, eneBase;
        public double strScaling, skpScaling, stmScaling, defScaling, vitScaling, pwrScaling, eneScaling;

        public ClassStatsData(RaceStatsConfig.ClassStats classStats) {
            RaceStatsConfig.BaseStats base = classStats.getBaseStats();
            RaceStatsConfig.StatScaling scaling = classStats.getStatScaling();

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

        public ClassStatsData(FriendlyByteBuf buf) {
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

        public RaceStatsConfig.ClassStats toClassStats() {
            RaceStatsConfig.ClassStats classStats = new RaceStatsConfig.ClassStats();

            RaceStatsConfig.BaseStats base = classStats.getBaseStats();
            base.setStrength(strBase);
            base.setStrikePower(skpBase);
            base.setResistance(resBase);
            base.setVitality(vitBase);
            base.setKiPower(pwrBase);
            base.setEnergy(eneBase);

            RaceStatsConfig.StatScaling scaling = classStats.getStatScaling();
            scaling.setStrengthScaling(strScaling);
            scaling.setStrikePowerScaling(skpScaling);
            scaling.setStaminaScaling(stmScaling);
            scaling.setDefenseScaling(defScaling);
            scaling.setVitalityScaling(vitScaling);
            scaling.setKiPowerScaling(pwrScaling);
            scaling.setEnergyScaling(eneScaling);

            return classStats;
        }
    }

    public static class RaceCharacterData {
        public String raceName;
        public boolean hasGender;
        public boolean useVanillaSkin;
        public String customModel;

        public RaceCharacterData(RaceCharacterConfig config) {
            this.raceName = config.getRaceName();
            this.hasGender = config.hasGender();
            this.useVanillaSkin = config.useVanillaSkin();
            this.customModel = config.getCustomModel();
        }

        public RaceCharacterData(FriendlyByteBuf buf) {
            this.raceName = buf.readUtf();
            this.hasGender = buf.readBoolean();
            this.useVanillaSkin = buf.readBoolean();
            this.customModel = buf.readUtf();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(raceName);
            buf.writeBoolean(hasGender);
            buf.writeBoolean(useVanillaSkin);
            buf.writeUtf(customModel);
        }

        public RaceCharacterConfig toConfig() {
            RaceCharacterConfig config = new RaceCharacterConfig();
            config.setRaceName(raceName);
            config.setHasGender(hasGender);
            config.setUseVanillaSkin(useVanillaSkin);
            config.setCustomModel(customModel);
            return config;
        }
    }
}
