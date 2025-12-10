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
    private final SkillsData skills;

    public SyncServerConfigS2C(Map<String, RaceStatsConfig> statsConfigs,
                               Map<String, RaceCharacterConfig> characterConfigs,
                               Map<String, Map<String, FormConfig>> formsConfigs,
                               GeneralServerConfig serverConfig,
                               SkillsConfig skillsConfig) {
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
        this.skills = new SkillsData(skillsConfig);
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
        this.skills = new SkillsData(buf);
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
        skills.encode(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ConfigManager.applySyncedServerConfig(raceStats, raceCharacters, raceForms, generalServer, skills);
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
        public int[] superformTpCost, godformTpCost, legendaryformsTpCost;

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
            this.superformTpCost = config.getSuperformTpCost();
            this.godformTpCost = config.getGodformTpCost();
            this.legendaryformsTpCost = config.getLegendaryformsTpCost();
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

            int superformLen = buf.readInt();
            this.superformTpCost = new int[superformLen];
            for (int i = 0; i < superformLen; i++) {
                this.superformTpCost[i] = buf.readInt();
            }

            int godformLen = buf.readInt();
            this.godformTpCost = new int[godformLen];
            for (int i = 0; i < godformLen; i++) {
                this.godformTpCost[i] = buf.readInt();
            }

            int legendaryLen = buf.readInt();
            this.legendaryformsTpCost = new int[legendaryLen];
            for (int i = 0; i < legendaryLen; i++) {
                this.legendaryformsTpCost[i] = buf.readInt();
            }
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

            buf.writeInt(superformTpCost != null ? superformTpCost.length : 0);
            if (superformTpCost != null) {
                for (int cost : superformTpCost) {
                    buf.writeInt(cost);
                }
            }

            buf.writeInt(godformTpCost != null ? godformTpCost.length : 0);
            if (godformTpCost != null) {
                for (int cost : godformTpCost) {
                    buf.writeInt(cost);
                }
            }

            buf.writeInt(legendaryformsTpCost != null ? legendaryformsTpCost.length : 0);
            if (legendaryformsTpCost != null) {
                for (int cost : legendaryformsTpCost) {
                    buf.writeInt(cost);
                }
            }
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
            config.setSuperformTpCost(superformTpCost);
            config.setGodformTpCost(godformTpCost);
            config.setLegendaryformsTpCost(legendaryformsTpCost);
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
        public String formType;
        public Map<String, FormDataData> forms;

        public FormGroupData(FormConfig config) {
            this.groupName = config.getGroupName();
            this.formType = config.getFormType();
            this.forms = new LinkedHashMap<>();
            config.getForms().forEach((formKey, formData) -> {
                this.forms.put(formKey, new FormDataData(formData));
            });
        }

        public FormGroupData(FriendlyByteBuf buf) {
            this.groupName = buf.readUtf();
            this.formType = buf.readUtf();
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
            buf.writeUtf(formType);
            buf.writeInt(forms.size());
            forms.forEach((formKey, data) -> {
                buf.writeUtf(formKey);
                data.encode(buf);
            });
        }

        public FormConfig toConfig() {
            FormConfig config = new FormConfig();
            config.setGroupName(groupName);
            config.setFormType(formType);
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
        public int unlockOnSuperformLevel;
        public float modelScaling;
		public double strMult, skpMult, stmMult, defMult, vitMult, pwrMult, eneMult, speedMult, energyDrain, staminaDrain, attackSpeed;

        public FormDataData(FormConfig.FormData formData) {
            this.name = formData.getName();
            this.unlockOnSuperformLevel = formData.getUnlockOnSuperformLevel();
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
            this.staminaDrain = formData.getStaminaDrain();
            this.attackSpeed = formData.getAttackSpeed();
        }

        public FormDataData(FriendlyByteBuf buf) {
            this.name = buf.readUtf();
            this.unlockOnSuperformLevel = buf.readInt();
            this.customModel = buf.readUtf();
            this.bodyColor1 = buf.readUtf();
            this.bodyColor2 = buf.readUtf();
            this.bodyColor3 = buf.readUtf();
            this.hairType = buf.readInt();
            this.hairColor = buf.readUtf();
            this.eye1Color = buf.readUtf();
            this.eye2Color = buf.readUtf();
            this.auraColor = buf.readUtf();
            this.modelScaling = buf.readFloat();
            this.strMult = buf.readDouble();
            this.skpMult = buf.readDouble();
            this.stmMult = buf.readDouble();
            this.defMult = buf.readDouble();
            this.vitMult = buf.readDouble();
            this.pwrMult = buf.readDouble();
            this.eneMult = buf.readDouble();
            this.speedMult = buf.readDouble();
            this.energyDrain = buf.readDouble();
            this.staminaDrain = buf.readDouble();
            this.attackSpeed = buf.readDouble();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(name);
            buf.writeInt(unlockOnSuperformLevel);
            buf.writeUtf(customModel);
            buf.writeUtf(bodyColor1);
            buf.writeUtf(bodyColor2);
            buf.writeUtf(bodyColor3);
            buf.writeInt(hairType);
            buf.writeUtf(hairColor);
            buf.writeUtf(eye1Color);
            buf.writeUtf(eye2Color);
            buf.writeUtf(auraColor);
            buf.writeFloat(modelScaling);
            buf.writeDouble(strMult);
            buf.writeDouble(skpMult);
            buf.writeDouble(stmMult);
            buf.writeDouble(defMult);
            buf.writeDouble(vitMult);
            buf.writeDouble(pwrMult);
            buf.writeDouble(eneMult);
            buf.writeDouble(speedMult);
            buf.writeDouble(energyDrain);
            buf.writeDouble(staminaDrain);
            buf.writeDouble(attackSpeed);
        }

        public FormConfig.FormData toFormData() {
            FormConfig.FormData formData = new FormConfig.FormData();
            formData.setName(name);
            formData.setUnlockOnSuperformLevel(unlockOnSuperformLevel);
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
            formData.setStaminaDrain(staminaDrain);
            formData.setAttackSpeed(attackSpeed);
            return formData;
        }
    }

    public static class GeneralServerData {
        public boolean generateCustomStructures, generateDragonBalls;
        public double tpsMultiplier;
        public boolean respectAttackCooldown;
        public int maxStatValue;
        public boolean kaiokenStackable;

        public GeneralServerData(GeneralServerConfig config) {
            this.generateCustomStructures = config.getWorldGen().isGenerateCustomStructures();
            this.generateDragonBalls = config.getWorldGen().isGenerateDragonBalls();
            this.tpsMultiplier = config.getGameplay().getTpsMultiplier();
            this.respectAttackCooldown = config.getGameplay().isRespectAttackCooldown();
            this.maxStatValue = config.getGameplay().getMaxStatValue();
            this.kaiokenStackable = config.getGameplay().isKaiokenStackable();
        }

        public GeneralServerData(FriendlyByteBuf buf) {
            this.generateCustomStructures = buf.readBoolean();
            this.generateDragonBalls = buf.readBoolean();
            this.tpsMultiplier = buf.readDouble();
            this.respectAttackCooldown = buf.readBoolean();
            this.maxStatValue = buf.readInt();
            this.kaiokenStackable = buf.readBoolean();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeBoolean(generateCustomStructures);
            buf.writeBoolean(generateDragonBalls);
            buf.writeDouble(tpsMultiplier);
            buf.writeBoolean(respectAttackCooldown);
            buf.writeInt(maxStatValue);
            buf.writeBoolean(kaiokenStackable);
        }

        public GeneralServerConfig toConfig() {
            GeneralServerConfig config = new GeneralServerConfig();
            config.getWorldGen().setGenerateCustomStructures(generateCustomStructures);
            config.getWorldGen().setGenerateDragonBalls(generateDragonBalls);
            config.getGameplay().setTpsMultiplier(tpsMultiplier);
            config.getGameplay().setRespectAttackCooldown(respectAttackCooldown);
            config.getGameplay().setMaxStatValue(maxStatValue);
            config.getGameplay().setKaiokenStackable(kaiokenStackable);
            return config;
        }
    }

    public static class SkillsData {
        public Map<String, SkillCostsData> skills;

        public SkillsData(SkillsConfig config) {
            this.skills = new LinkedHashMap<>();
            config.getSkills().forEach((skillName, skillCosts) -> {
                this.skills.put(skillName, new SkillCostsData(skillCosts));
            });
        }

        public SkillsData(FriendlyByteBuf buf) {
            int size = buf.readInt();
            this.skills = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                String skillName = buf.readUtf();
                SkillCostsData data = new SkillCostsData(buf);
                this.skills.put(skillName, data);
            }
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeInt(skills.size());
            skills.forEach((skillName, data) -> {
                buf.writeUtf(skillName);
                data.encode(buf);
            });
        }

        public SkillsConfig toConfig() {
            SkillsConfig config = new SkillsConfig();
            Map<String, SkillsConfig.SkillCosts> skillsMap = new LinkedHashMap<>();
            skills.forEach((skillName, data) -> {
                skillsMap.put(skillName, data.toSkillCosts());
            });
            config.getSkills().clear();
            config.getSkills().putAll(skillsMap);
            return config;
        }
    }

    public static class SkillCostsData {
        public int[] costs;

        public SkillCostsData(SkillsConfig.SkillCosts skillCosts) {
            this.costs = skillCosts.getCosts().stream().mapToInt(Integer::intValue).toArray();
        }

        public SkillCostsData(FriendlyByteBuf buf) {
            int length = buf.readInt();
            this.costs = new int[length];
            for (int i = 0; i < length; i++) {
                this.costs[i] = buf.readInt();
            }
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeInt(costs.length);
            for (int cost : costs) {
                buf.writeInt(cost);
            }
        }

        public SkillsConfig.SkillCosts toSkillCosts() {
            java.util.List<Integer> costsList = new java.util.ArrayList<>();
            for (int cost : costs) {
                costsList.add(cost);
            }
            return new SkillsConfig.SkillCosts(costsList);
        }
    }
}


