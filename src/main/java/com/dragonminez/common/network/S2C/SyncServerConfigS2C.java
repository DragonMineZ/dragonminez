package com.dragonminez.common.network.S2C;

import com.dragonminez.common.config.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncServerConfigS2C {

    private final Map<String, RaceStatsData> raceStats;
    private final Map<String, RaceCharacterData> raceCharacters;
    private final Map<String, RaceFormsData> raceForms;
    private final GeneralServerData generalServer;

    public SyncServerConfigS2C(Map<String, RaceStatsConfig> statsConfigs,
                               Map<String, RaceCharacterConfig> characterConfigs,
                               Map<String, Map<String, FormConfig>> formsConfigs,
                               GeneralServerConfig serverConfig) {
        this.raceStats = new HashMap<>();
        statsConfigs.forEach((raceName, config) -> {
            this.raceStats.put(raceName, new RaceStatsData(config));
        });

        this.raceCharacters = new HashMap<>();
        characterConfigs.forEach((raceName, config) -> {
            this.raceCharacters.put(raceName, new RaceCharacterData(config));
        });

        this.raceForms = new HashMap<>();
        formsConfigs.forEach((raceName, forms) -> {
            this.raceForms.put(raceName, new RaceFormsData(forms));
        });

        this.generalServer = new GeneralServerData(serverConfig);
    }

    public SyncServerConfigS2C(FriendlyByteBuf buf) {
        int statsSize = buf.readInt();
        this.raceStats = new HashMap<>();
        for (int i = 0; i < statsSize; i++) {
            String raceName = buf.readUtf();
            RaceStatsData data = new RaceStatsData(buf);
            this.raceStats.put(raceName, data);
        }

        int characterSize = buf.readInt();
        this.raceCharacters = new HashMap<>();
        for (int i = 0; i < characterSize; i++) {
            String raceName = buf.readUtf();
            RaceCharacterData data = new RaceCharacterData(buf);
            this.raceCharacters.put(raceName, data);
        }

        int formsSize = buf.readInt();
        this.raceForms = new HashMap<>();
        for (int i = 0; i < formsSize; i++) {
            String raceName = buf.readUtf();
            RaceFormsData data = new RaceFormsData(buf);
            this.raceForms.put(raceName, data);
        }

        this.generalServer = new GeneralServerData(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(raceStats.size());
        raceStats.forEach((raceName, data) -> {
            buf.writeUtf(raceName);
            data.encode(buf);
        });

        buf.writeInt(raceCharacters.size());
        raceCharacters.forEach((raceName, data) -> {
            buf.writeUtf(raceName);
            data.encode(buf);
        });

        buf.writeInt(raceForms.size());
        raceForms.forEach((raceName, data) -> {
            buf.writeUtf(raceName);
            data.encode(buf);
        });

        generalServer.encode(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ConfigManager.applySyncedServerConfig(raceStats, raceCharacters, raceForms, generalServer);
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
        public int defaultBodyType, defaultHairType, defaultEyesType, defaultNoseType, defaultMouthType;
        public String defaultBodyColor, defaultBodyColor2, defaultBodyColor3;
        public String defaultHairColor, defaultEye1Color, defaultEye2Color, defaultAuraColor;

        public RaceCharacterData(RaceCharacterConfig config) {
            this.raceName = config.getRaceName();
            this.hasGender = config.hasGender();
            this.useVanillaSkin = config.useVanillaSkin();
            this.customModel = config.getCustomModel();
            this.defaultBodyType = config.getDefaultBodyType();
            this.defaultHairType = config.getDefaultHairType();
            this.defaultEyesType = config.getDefaultEyesType();
            this.defaultNoseType = config.getDefaultNoseType();
            this.defaultMouthType = config.getDefaultMouthType();
            this.defaultBodyColor = config.getDefaultBodyColor();
            this.defaultBodyColor2 = config.getDefaultBodyColor2();
            this.defaultBodyColor3 = config.getDefaultBodyColor3();
            this.defaultHairColor = config.getDefaultHairColor();
            this.defaultEye1Color = config.getDefaultEye1Color();
            this.defaultEye2Color = config.getDefaultEye2Color();
            this.defaultAuraColor = config.getDefaultAuraColor();
        }

        public RaceCharacterData(FriendlyByteBuf buf) {
            this.raceName = buf.readUtf();
            this.hasGender = buf.readBoolean();
            this.useVanillaSkin = buf.readBoolean();
            this.customModel = buf.readUtf();
            this.defaultBodyType = buf.readInt();
            this.defaultHairType = buf.readInt();
            this.defaultEyesType = buf.readInt();
            this.defaultNoseType = buf.readInt();
            this.defaultMouthType = buf.readInt();
            this.defaultBodyColor = buf.readUtf();
            this.defaultBodyColor2 = buf.readUtf();
            this.defaultBodyColor3 = buf.readUtf();
            this.defaultHairColor = buf.readUtf();
            this.defaultEye1Color = buf.readUtf();
            this.defaultEye2Color = buf.readUtf();
            this.defaultAuraColor = buf.readUtf();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(raceName);
            buf.writeBoolean(hasGender);
            buf.writeBoolean(useVanillaSkin);
            buf.writeUtf(customModel);
            buf.writeInt(defaultBodyType);
            buf.writeInt(defaultHairType);
            buf.writeInt(defaultEyesType);
            buf.writeInt(defaultNoseType);
            buf.writeInt(defaultMouthType);
            buf.writeUtf(defaultBodyColor);
            buf.writeUtf(defaultBodyColor2);
            buf.writeUtf(defaultBodyColor3);
            buf.writeUtf(defaultHairColor);
            buf.writeUtf(defaultEye1Color);
            buf.writeUtf(defaultEye2Color);
            buf.writeUtf(defaultAuraColor);
        }

        public RaceCharacterConfig toConfig() {
            RaceCharacterConfig config = new RaceCharacterConfig();
            config.setRaceName(raceName);
            config.setHasGender(hasGender);
            config.setUseVanillaSkin(useVanillaSkin);
            config.setCustomModel(customModel);
            config.setDefaultBodyType(defaultBodyType);
            config.setDefaultHairType(defaultHairType);
            config.setDefaultEyesType(defaultEyesType);
            config.setDefaultNoseType(defaultNoseType);
            config.setDefaultMouthType(defaultMouthType);
            config.setDefaultBodyColor(defaultBodyColor);
            config.setDefaultBodyColor2(defaultBodyColor2);
            config.setDefaultBodyColor3(defaultBodyColor3);
            config.setDefaultHairColor(defaultHairColor);
            config.setDefaultEye1Color(defaultEye1Color);
            config.setDefaultEye2Color(defaultEye2Color);
            config.setDefaultAuraColor(defaultAuraColor);
            return config;
        }
    }

    public static class RaceFormsData {
        public Map<String, FormGroupData> formGroups;

        public RaceFormsData(Map<String, FormConfig> forms) {
            this.formGroups = new LinkedHashMap<>();
            forms.forEach((groupName, formConfig) -> {
                this.formGroups.put(groupName, new FormGroupData(formConfig));
            });
        }

        public RaceFormsData(FriendlyByteBuf buf) {
            int groupCount = buf.readInt();
            this.formGroups = new LinkedHashMap<>();
            for (int i = 0; i < groupCount; i++) {
                String groupName = buf.readUtf();
                FormGroupData data = new FormGroupData(buf);
                this.formGroups.put(groupName, data);
            }
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeInt(formGroups.size());
            formGroups.forEach((groupName, data) -> {
                buf.writeUtf(groupName);
                data.encode(buf);
            });
        }

        public Map<String, FormConfig> toConfig(String raceName) {
            Map<String, FormConfig> forms = new LinkedHashMap<>();
            formGroups.forEach((groupName, data) -> {
                forms.put(groupName, data.toConfig());
            });
            return forms;
        }
    }

    public static class FormGroupData {
        public String groupName;
        public Map<String, FormDataData> forms;

        public FormGroupData(FormConfig config) {
            this.groupName = config.getGroupName();
            this.forms = new LinkedHashMap<>();
            config.getForms().forEach((formKey, formData) -> {
                this.forms.put(formKey, new FormDataData(formData));
            });
        }

        public FormGroupData(FriendlyByteBuf buf) {
            this.groupName = buf.readUtf();
            int formCount = buf.readInt();
            this.forms = new LinkedHashMap<>();
            for (int i = 0; i < formCount; i++) {
                String formKey = buf.readUtf();
                FormDataData data = new FormDataData(buf);
                this.forms.put(formKey, data);
            }
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(groupName);
            buf.writeInt(forms.size());
            forms.forEach((formKey, data) -> {
                buf.writeUtf(formKey);
                data.encode(buf);
            });
        }

        public FormConfig toConfig() {
            FormConfig config = new FormConfig();
            config.setGroupName(groupName);
            Map<String, FormConfig.FormData> formsMap = new LinkedHashMap<>();
            forms.forEach((formKey, data) -> {
                formsMap.put(formKey, data.toFormData());
            });
            config.setForms(formsMap);
            return config;
        }
    }

    public static class FormDataData {
        public String name, customModel, bodyColor1, bodyColor2, bodyColor3;
        public String hairColor, eye1Color, eye2Color, auraColor;
        public int hairType;
        public double modelScaling, strMult, skpMult, stmMult, defMult, vitMult, pwrMult, eneMult, speedMult, energyDrain;

        public FormDataData(FormConfig.FormData formData) {
            this.name = formData.getName();
            this.customModel = formData.getCustomModel();
            this.bodyColor1 = formData.getBodyColor1();
            this.bodyColor2 = formData.getBodyColor2();
            this.bodyColor3 = formData.getBodyColor3();
            this.hairType = formData.getHairType();
            this.hairColor = formData.getHairColor();
            this.eye1Color = formData.getEye1Color();
            this.eye2Color = formData.getEye2Color();
            this.auraColor = formData.getAuraColor();
            this.modelScaling = formData.getModelScaling();
            this.strMult = formData.getStrMultiplier();
            this.skpMult = formData.getSkpMultiplier();
            this.stmMult = formData.getStmMultiplier();
            this.defMult = formData.getDefMultiplier();
            this.vitMult = formData.getVitMultiplier();
            this.pwrMult = formData.getPwrMultiplier();
            this.eneMult = formData.getEneMultiplier();
            this.speedMult = formData.getSpeedMultiplier();
            this.energyDrain = formData.getEnergyDrain();
        }

        public FormDataData(FriendlyByteBuf buf) {
            this.name = buf.readUtf();
            this.customModel = buf.readUtf();
            this.bodyColor1 = buf.readUtf();
            this.bodyColor2 = buf.readUtf();
            this.bodyColor3 = buf.readUtf();
            this.hairType = buf.readInt();
            this.hairColor = buf.readUtf();
            this.eye1Color = buf.readUtf();
            this.eye2Color = buf.readUtf();
            this.auraColor = buf.readUtf();
            this.modelScaling = buf.readDouble();
            this.strMult = buf.readDouble();
            this.skpMult = buf.readDouble();
            this.stmMult = buf.readDouble();
            this.defMult = buf.readDouble();
            this.vitMult = buf.readDouble();
            this.pwrMult = buf.readDouble();
            this.eneMult = buf.readDouble();
            this.speedMult = buf.readDouble();
            this.energyDrain = buf.readDouble();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(name);
            buf.writeUtf(customModel);
            buf.writeUtf(bodyColor1);
            buf.writeUtf(bodyColor2);
            buf.writeUtf(bodyColor3);
            buf.writeInt(hairType);
            buf.writeUtf(hairColor);
            buf.writeUtf(eye1Color);
            buf.writeUtf(eye2Color);
            buf.writeUtf(auraColor);
            buf.writeDouble(modelScaling);
            buf.writeDouble(strMult);
            buf.writeDouble(skpMult);
            buf.writeDouble(stmMult);
            buf.writeDouble(defMult);
            buf.writeDouble(vitMult);
            buf.writeDouble(pwrMult);
            buf.writeDouble(eneMult);
            buf.writeDouble(speedMult);
            buf.writeDouble(energyDrain);
        }

        public FormConfig.FormData toFormData() {
            FormConfig.FormData formData = new FormConfig.FormData();
            formData.setName(name);
            formData.setCustomModel(customModel);
            formData.setBodyColor1(bodyColor1);
            formData.setBodyColor2(bodyColor2);
            formData.setBodyColor3(bodyColor3);
            formData.setHairType(hairType);
            formData.setHairColor(hairColor);
            formData.setEye1Color(eye1Color);
            formData.setEye2Color(eye2Color);
            formData.setAuraColor(auraColor);
            formData.setModelScaling(modelScaling);
            formData.setStrMultiplier(strMult);
            formData.setSkpMultiplier(skpMult);
            formData.setStmMultiplier(stmMult);
            formData.setDefMultiplier(defMult);
            formData.setVitMultiplier(vitMult);
            formData.setPwrMultiplier(pwrMult);
            formData.setEneMultiplier(eneMult);
            formData.setSpeedMultiplier(speedMult);
            formData.setEnergyDrain(energyDrain);
            return formData;
        }
    }

    public static class GeneralServerData {
        public boolean generateCustomStructures, generateDragonBalls;
        public double tpsMultiplier;
        public boolean respectAttackCooldown;
        public int maxStatValue;

        public GeneralServerData(GeneralServerConfig config) {
            this.generateCustomStructures = config.getWorldGen().isGenerateCustomStructures();
            this.generateDragonBalls = config.getWorldGen().isGenerateDragonBalls();
            this.tpsMultiplier = config.getGameplay().getTpsMultiplier();
            this.respectAttackCooldown = config.getGameplay().isRespectAttackCooldown();
            this.maxStatValue = config.getGameplay().getMaxStatValue();
        }

        public GeneralServerData(FriendlyByteBuf buf) {
            this.generateCustomStructures = buf.readBoolean();
            this.generateDragonBalls = buf.readBoolean();
            this.tpsMultiplier = buf.readDouble();
            this.respectAttackCooldown = buf.readBoolean();
            this.maxStatValue = buf.readInt();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeBoolean(generateCustomStructures);
            buf.writeBoolean(generateDragonBalls);
            buf.writeDouble(tpsMultiplier);
            buf.writeBoolean(respectAttackCooldown);
            buf.writeInt(maxStatValue);
        }

        public GeneralServerConfig toConfig() {
            GeneralServerConfig config = new GeneralServerConfig();
            config.getWorldGen().setGenerateCustomStructures(generateCustomStructures);
            config.getWorldGen().setGenerateDragonBalls(generateDragonBalls);
            config.getGameplay().setTpsMultiplier(tpsMultiplier);
            config.getGameplay().setRespectAttackCooldown(respectAttackCooldown);
            config.getGameplay().setMaxStatValue(maxStatValue);
            return config;
        }
    }
}


