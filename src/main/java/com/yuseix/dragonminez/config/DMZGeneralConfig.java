package com.yuseix.dragonminez.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class DMZGeneralConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Integer> MAX_ATTRIBUTE_VALUE;
    public static final ForgeConfigSpec.ConfigValue<Boolean> TRANSFORMATIONS_WITH_TP;
    public static final ForgeConfigSpec.ConfigValue<Integer> TPCOST_TRANSFORMATIONS;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SAVE_INVENTORY;

    //CONFIGS GENERALES
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_FALLDMG;
    public static final ForgeConfigSpec.ConfigValue<Integer> PERHIT_ZPOINTS_GAIN;
    public static final ForgeConfigSpec.ConfigValue<Double> PERKILL_ZPOINTS_GAIN;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_ZPOINTS_COST;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_ZPOINTS_GAIN;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_ZPOINTS_HTC;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_TREE_MIGHT;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_MAJIN;
    public static final ForgeConfigSpec.ConfigValue<Integer> BABA_COOLDOWN;
    public static final ForgeConfigSpec.ConfigValue<Integer> BABA_DURATION;

    public static final ForgeConfigSpec.ConfigValue<Integer> SENZU_COOLDOWN;
    public static final ForgeConfigSpec.ConfigValue<Integer> SENZU_GIVE;
    public static final ForgeConfigSpec.ConfigValue<Integer> SENZU_DAILY_COOLDOWN;

    public static final ForgeConfigSpec.ConfigValue<Boolean> OTHERWORLD_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOULD_DBALL_SPAWN;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOULD_DBALL_DRAGON_SPAWN;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOULD_KAMILOOKOUT_SPAWN;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOULD_GOKUHOUSE_SPAWN;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOULD_KAMEHOUSE_SPAWN;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOULD_ELDERGURU_SPAWN;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOULD_DBALLEVENTS_ACTIVE;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOULD_DRAGON_SPAWN;

    public static final ForgeConfigSpec.ConfigValue<Integer> DBALL_SPAWN_RANGE;
    public static final ForgeConfigSpec.ConfigValue<Integer> SENZU_SHENRON_WISH;
    public static final ForgeConfigSpec.ConfigValue<Integer> CAPSULE_SHENRON_WISH;
    public static final ForgeConfigSpec.ConfigValue<Integer> SENZU_PORUNGA_WISH;
    public static final ForgeConfigSpec.ConfigValue<Integer> CAPSULE_PORUNGA_WISH;
    public static final ForgeConfigSpec.ConfigValue<Integer> GETE_PORUNGA_WISH;

    public static final ForgeConfigSpec.ConfigValue<Integer> JUMP_TP_COST_MASTER;
    public static final ForgeConfigSpec.ConfigValue<Integer> JUMP_TP_COST_LEVELS;
    public static final ForgeConfigSpec.ConfigValue<Integer> FLY_TP_COST_MASTER;
    public static final ForgeConfigSpec.ConfigValue<Integer> FLY_TP_COST_LEVELS;
    public static final ForgeConfigSpec.ConfigValue<Integer> POTUNLOCK_TP_COST_MASTER;
    public static final ForgeConfigSpec.ConfigValue<Integer> POTUNLOCK_TP_COST_LEVELS;
    public static final ForgeConfigSpec.ConfigValue<Integer> MEDITATION_TP_COST_MASTER;
    public static final ForgeConfigSpec.ConfigValue<Integer> MEDITATION_TP_COST_LEVELS;
    public static final ForgeConfigSpec.ConfigValue<Integer> KI_CONTROL_TP_COST_MASTER;
    public static final ForgeConfigSpec.ConfigValue<Integer> KI_CONTROL_TP_COST_LEVELS;
    public static final ForgeConfigSpec.ConfigValue<Integer> KI_MANIPULATION_TP_COST_MASTER;
    public static final ForgeConfigSpec.ConfigValue<Integer> KI_MANIPULATION_TP_COST_LEVELS;

    public static final ForgeConfigSpec.ConfigValue<Double> DINO_TAIL_RAW_HEALTHREGEN;
    public static final ForgeConfigSpec.ConfigValue<Double> DINO_TAIL_RAW_KIREGEN;
    public static final ForgeConfigSpec.ConfigValue<Double> DINO_TAIL_RAW_STAMREGEN;
    public static final ForgeConfigSpec.ConfigValue<Double> DINO_TAIL_COOKED_HEALTHREGEN;
    public static final ForgeConfigSpec.ConfigValue<Double> DINO_TAIL_COOKED_KIREGEN;
    public static final ForgeConfigSpec.ConfigValue<Double> DINO_TAIL_COOKED_STAMREGEN;
    public static final ForgeConfigSpec.ConfigValue<Double> DINO_MEAT_RAW_HEALTHREGEN;
    public static final ForgeConfigSpec.ConfigValue<Double> DINO_MEAT_RAW_KIREGEN;
    public static final ForgeConfigSpec.ConfigValue<Double> DINO_MEAT_RAW_STAMREGEN;
    public static final ForgeConfigSpec.ConfigValue<Double> DINO_MEAT_COOKED_HEALTHREGEN;
    public static final ForgeConfigSpec.ConfigValue<Double> DINO_MEAT_COOKED_KIREGEN;
    public static final ForgeConfigSpec.ConfigValue<Double> DINO_MEAT_COOKED_STAMREGEN;
    public static final ForgeConfigSpec.ConfigValue<Double> FROG_LEGS_RAW_HEALTHREGEN;
    public static final ForgeConfigSpec.ConfigValue<Double> FROG_LEGS_RAW_KIREGEN;
    public static final ForgeConfigSpec.ConfigValue<Double> FROG_LEGS_RAW_STAMREGEN;
    public static final ForgeConfigSpec.ConfigValue<Double> FROG_LEGS_COOKED_HEALTHREGEN;
    public static final ForgeConfigSpec.ConfigValue<Double> FROG_LEGS_COOKED_KIREGEN;
    public static final ForgeConfigSpec.ConfigValue<Double> FROG_LEGS_COOKED_STAMREGEN;

    static {
        BUILDER.push("Configs for Attributes of DragonMineZ");

        MAX_ATTRIBUTE_VALUE = BUILDER.comment("Max Attributes! (Min: 100 / Max: 100000 / Default: 5000)")
                .defineInRange("Attributes: ", 5000, 100, 100000);

        TRANSFORMATIONS_WITH_TP = BUILDER.comment("Allow Transformations to be buyable with TP instead of Obtainable via Storyline (Default: false)")
                .define("Transformations: ", true);

        TPCOST_TRANSFORMATIONS = BUILDER.comment("ZPoints Cost for Buying Transformations (Min: 0 / Max: 1000000000 / Default: 40000)")
                .defineInRange("Transformations Cost: ", 40000, 0, 1000000000);

        SAVE_INVENTORY = BUILDER.comment("Save Player Inventory on Death (Default: true)")
                .comment("This function is currently disabled due to a big bug. It will reworked an re enabled in v1.3.0.")
                .comment("Either you put it True or False, it will not work.")
                .define("Save Inventory: ", true);

        BUILDER.pop();

        BUILDER.push("ZPoints Configs");

        PERHIT_ZPOINTS_GAIN = BUILDER.comment("ZPoints Obtained per Hit (Min: 0 / Max: 100 / Default: 2)")
                .defineInRange("ZPoints per Hit: ", 2, 0, 100);

        PERKILL_ZPOINTS_GAIN = BUILDER.comment("ZPoints Obtained per Kill based on Enemy max Health (Min: 0.0 / Max: 1.0 / Default: 0.45)")
                .defineInRange("ZPoints per Kill percentage: ", 0.45, 0.0, 1);

        MULTIPLIER_ZPOINTS_COST = BUILDER.comment("Multiplier for ZPoints Cost (Min: 0.0 / Max: 20.0 / Default: 1.2)")
                .defineInRange("ZPoints Cost: ", 1.2, 0.0, 20.0);

        MULTIPLIER_ZPOINTS_GAIN = BUILDER.comment("Multiplier for ZPoints Gain (Min: 0.0 / Max: 20.0 / Default: 1.2)")
                .defineInRange("ZPoints Gain: ", 1.2, 0.0, 20.0);

        MULTIPLIER_ZPOINTS_HTC = BUILDER.comment("Multiplier for ZPoints in the Hyperbolic Time Chamber (Min: 0.0 / Max: 20.0 / Default: 3.0)")
                .defineInRange("ZPoints (HTC): ", 3.0, 0.0, 60.0);


        BUILDER.pop();


        BUILDER.push("General Configs");

        MULTIPLIER_FALLDMG = BUILDER.comment("Fall Damage Multiplier Percentage (Min: 0.00 / Max: 1.00 / Default: 0.05)")
                .defineInRange("FallDmg: ", 0.05, 0.00, 1.00);

        SENZU_COOLDOWN = BUILDER.comment("Cooldown for Senzu Beans (Min: 0 / Max: 60 / Default: 10)")
                .defineInRange("Seconds: ", 10, 0, 60);

        SENZU_GIVE = BUILDER.comment("Number of Senzu Beans the Master Korin will give (Min: 1 / Max: 10 / Default: 5)")
                .defineInRange("Number: ", 5, 1, 10);

        SENZU_DAILY_COOLDOWN = BUILDER.comment("Wait time to claim the next Senzu Bean (seconds) (Min: 1 / Max: 36000 / Default: 300)")
                .defineInRange("Time: ", 300, 1, 36000);

        MULTIPLIER_TREE_MIGHT = BUILDER.comment("Multiplier for the Fruit of the Tree of Might Effect (Min: 1.0 / Max: 20.0 / Default: 1.3)")
                .defineInRange("MightFruit Multiplier: ", 1.3, 1.0, 20.0);

        MULTIPLIER_MAJIN = BUILDER.comment("Multiplier for the Majin Mark Effect (Min: 1.0 / Max: 20.0 / Default: 1.5)")
                .defineInRange("Majin Multiplier: ", 1.5, 1.0, 20.0);

        BABA_COOLDOWN = BUILDER.comment("Cooldown for Baba's Temporal Revive in Minutes (Min: 0 / Max: 600 / Default: 20)")
                .defineInRange("Baba Cooldown: ", 20, 0, 600);

        BABA_DURATION = BUILDER.comment("Duration for Baba's Temporal Revive in Minutes (Min: 0 / Max: 60 / Default: 15)")
                .defineInRange("Baba Duration: ", 15, 0, 60);

        BUILDER.pop();

        BUILDER.push("World Generations Configs");

        OTHERWORLD_ENABLED = BUILDER.comment("Should Otherworld Dimension be Enabled? (Default: true)")
                .define("Otherworld Enabled: ", true);

        SHOULD_DBALL_SPAWN = BUILDER.comment("Should Dragon Balls Spawn in the World when it is first generated? (Default: true)")
                .define("First Spawn: ", true);

        SHOULD_DBALL_DRAGON_SPAWN = BUILDER.comment("Should Dragon Balls Spread in the World when Shenron/Porunga despawns? (Default: true)")
                .define("Spread on Despawn: ", true);

        SHOULD_KAMILOOKOUT_SPAWN = BUILDER.comment("Should Kami's Lookout Spawn in the World when it is first generated? (Default: true)")
                .define("Kami Lookout Spawn: ", true);

        SHOULD_GOKUHOUSE_SPAWN = BUILDER.comment("Should Goku's House Spawn in the World when it is first generated? (Default: true)")
                .define("Goku House Spawn: ", true);

        SHOULD_KAMEHOUSE_SPAWN = BUILDER.comment("Should Kame House Spawn in the World when it is first generated? (Default: true)")
                .define("Kame House Spawn: ", true);

        SHOULD_ELDERGURU_SPAWN = BUILDER.comment("Should Elder Guru's House Spawn in the World when it is first generated? (Default: true)")
                .define("Elder Guru Spawn: ", true);

        SHOULD_DBALLEVENTS_ACTIVE = BUILDER.comment("Should Dragon Ball Events be Active? (Default: true)")
                .comment("DragonBall Events made Old Dragon Balls disappear when a new one is placed.")
                .comment("Disabling this will allow Multiple Sets of DragonBalls to be placed.")
                .comment("But it will also make conflicts with the Dragon Radar, due to it only tracking the first set of Dragon Balls.")
                .define("DBall Events Active: ", true);

        SHOULD_DRAGON_SPAWN = BUILDER.comment("Should Shenron and Porunga Spawn when Interacting with DragonBalls? (Default: true)")
                .define("Dragon Spawn: ", true);

        BUILDER.push("Shenron / Porunga Wishes");

        DBALL_SPAWN_RANGE = BUILDER.comment("Range in blocks for the Dragon Balls to spawn (Min: 2000 / Max: 20000 / Default: 3000)")
                .defineInRange("Spawn Range: ", 3000, 2000, 20000);

        SENZU_SHENRON_WISH = BUILDER.comment("Number of Senzu Beans Shenron will give (Min: 1 / Max: 64 / Default: 4)")
                .defineInRange("[Shenron] Number of Senzus: ", 4, 1, 64);

        CAPSULE_SHENRON_WISH = BUILDER.comment("Number of Capsules Shenron will give (Min: 1 / Max: 64 / Default: 2)")
                .defineInRange("[Shenron] Number of Capsules: ", 2, 1, 64);

        SENZU_PORUNGA_WISH = BUILDER.comment("Number of Senzu Beans Porunga will give (Min: 1 / Max: 64 / Default: 8)")
                .defineInRange("[Porunga] Number of Senzus: ", 8, 1, 64);

        CAPSULE_PORUNGA_WISH = BUILDER.comment("Number of Capsules Porunga will give (Min: 1 / Max: 64 / Default: 3)")
                .defineInRange("[Porunga] Number of Capsules: ", 4, 1, 64);

        GETE_PORUNGA_WISH = BUILDER.comment("Number of Gete Scraps Porunga will give (Min: 1 / Max: 64 / Default: 3)")
                .defineInRange("[Porunga] Number of Gete Scraps: ", 3, 1, 64);

        BUILDER.pop();

        BUILDER.push("Skills Configs");

        JUMP_TP_COST_MASTER = BUILDER.comment("ZPoints Cost for Buying the Jump Skill from Masters (Min: 1 / Max: 1000000000 / Default: 400)")
                .defineInRange("Jump Buy: ", 400, 1, 1000000000);

        JUMP_TP_COST_LEVELS = BUILDER.comment("ZPoints Cost for Leveling Up the Jump Skill (Min: 1 / Max: 1000000000 / Default: 250) (Formula: Cost * Level * ZPointsCostMultiplier)")
                .defineInRange("Jump Levels: ", 250, 1, 1000000000);

        FLY_TP_COST_MASTER = BUILDER.comment("ZPoints Cost for Buying the Fly Skill from Masters (Min: 1 / Max: 1000000000 / Default: 1750)")
                .defineInRange("Fly Buy: ", 1750, 1, 1000000000);

        FLY_TP_COST_LEVELS = BUILDER.comment("ZPoints Cost for Leveling Up the Fly Skill (Min: 1 / Max: 1000000000 / Default: 750) (Formula: Cost * Level * ZPointsCostMultiplier)")
                .defineInRange("Fly Levels: ", 750, 1, 1000000000);

        POTUNLOCK_TP_COST_MASTER = BUILDER.comment("ZPoints Cost for Buying the Potential Unlock Skill from Masters (Min: 1 / Max: 1000000000 / Default: 3500)")
                .defineInRange("Potential Unlock Buy: ", 3500, 1, 1000000000);

        POTUNLOCK_TP_COST_LEVELS = BUILDER.comment("ZPoints Cost for Leveling Up the Potential Unlock Skill (Min: 1 / Max: 1000000000 / Default: 600) (Formula: Cost * Level * ZPointsCostMultiplier)")
                .defineInRange("Potential Unlock Levels: ", 1300, 1, 1000000000);

        MEDITATION_TP_COST_MASTER = BUILDER.comment("ZPoints Cost for Buying the Meditation Skill from Masters (Min: 1 / Max: 1000000000 / Default: 500)")
                .defineInRange("Meditation Buy: ", 500, 1, 1000000000);

        MEDITATION_TP_COST_LEVELS = BUILDER.comment("ZPoints Cost for Leveling Up the Meditation Skill (Min: 1 / Max: 1000000000 / Default: 150) (Formula: Cost * Level * ZPointsCostMultiplier)")
                .defineInRange("Meditation Levels: ", 150, 1, 1000000000);

        KI_CONTROL_TP_COST_MASTER = BUILDER.comment("ZPoints Cost for Buying the Ki Control Skill from Masters (Min: 1 / Max: 1000000000 / Default: 500)")
                .defineInRange("Ki Control Buy: ", 500, 1, 1000000000);

        KI_CONTROL_TP_COST_LEVELS = BUILDER.comment("ZPoints Cost for Leveling Up the Ki Control Skill (Min: 1 / Max: 1000000000 / Default: 100) (Formula: Cost * Level * ZPointsCostMultiplier)")
                .defineInRange("Ki Control Levels: ", 100, 1, 1000000000);

        KI_MANIPULATION_TP_COST_MASTER = BUILDER.comment("ZPoints Cost for Buying the Ki Manipulation Skill from Masters (Min: 1 / Max: 1000000000 / Default: 12500)")
                .defineInRange("Ki Manipulation Buy: ", 12500, 1, 1000000000);

        KI_MANIPULATION_TP_COST_LEVELS = BUILDER.comment("ZPoints Cost for Leveling Up the Ki Manipulation Skill (Min: 1 / Max: 1000000000 / Default: 5000) (Formula: Cost * Level * ZPointsCostMultiplier)")
                .defineInRange("Ki Manipulation Levels: ", 5000, 1, 1000000000);

        BUILDER.pop();

        BUILDER.push("Foods Configs");

        DINO_TAIL_RAW_HEALTHREGEN = BUILDER.comment("Health Regeneration for Raw Dino Tail (Min: 0.0 / Max: 1.0 / Default: 0.09)")
                .defineInRange("Raw Dino Tail Health Regen: ", 0.09, 0.0, 1.0);

        DINO_TAIL_RAW_KIREGEN = BUILDER.comment("Ki Regeneration for Raw Dino Tail (Min: 0.0 / Max: 1.0 / Default: 0.06)")
                .defineInRange("Raw Dino Tail Ki Regen: ", 0.06, 0.0, 1.0);

        DINO_TAIL_RAW_STAMREGEN = BUILDER.comment("Stamina Regeneration for Raw Dino Tail (Min: 0.0 / Max: 1.0 / Default: 0.06)")
                .defineInRange("Raw Dino Tail Stamina Regen: ", 0.06, 0.0, 1.0);

        DINO_TAIL_COOKED_HEALTHREGEN = BUILDER.comment("Health Regeneration for Cooked Dino Tail (Min: 0.0 / Max: 1.0 / Default: 0.18)")
                .defineInRange("Cooked Dino Tail Health Regen: ", 0.18, 0, 100);

        DINO_TAIL_COOKED_KIREGEN = BUILDER.comment("Ki Regeneration for Cooked Dino Tail (Min: 0.0 / Max: 1.0 / Default: 0.12)")
                .defineInRange("Cooked Dino Tail Ki Regen: ", 0.12, 0.0, 1.0);

        DINO_TAIL_COOKED_STAMREGEN = BUILDER.comment("Stamina Regeneration for Cooked Dino Tail (Min: 0.0 / Max: 1.0 / Default: 0.12)")
                .defineInRange("Cooked Dino Tail Stamina Regen: ", 0.12, 0.0, 1.0);

        DINO_MEAT_RAW_HEALTHREGEN = BUILDER.comment("Health Regeneration for Raw Dino Meat (Min: 0.0 / Max: 1.0 / Default: 0.04)")
                .defineInRange("Raw Dino Meat Health Regen: ", 0.04, 0.0, 1.0);

        DINO_MEAT_RAW_KIREGEN = BUILDER.comment("Ki Regeneration for Raw Dino Meat (Min: 0.0 / Max: 1.0 / Default: 0.02)")
                .defineInRange("Raw Dino Meat Ki Regen: ", 0.02, 0.0, 1.0);

        DINO_MEAT_RAW_STAMREGEN = BUILDER.comment("Stamina Regeneration for Raw Dino Meat (Min: 0.0 / Max: 1.0 / Default: 0.02)")
                .defineInRange("Raw Dino Meat Stamina Regen: ", 0.02, 0.0, 1.0);

        DINO_MEAT_COOKED_HEALTHREGEN = BUILDER.comment("Health Regeneration for Cooked Dino Meat (Min: 0.0 / Max: 1.0 / Default: 0.1)")
                .defineInRange("Cooked Dino Meat Health Regen: ", 0.1, 0.0, 1.0);

        DINO_MEAT_COOKED_KIREGEN = BUILDER.comment("Ki Regeneration for Cooked Dino Meat (Min: 0.0 / Max: 1.0 / Default: 0.02)")
                .defineInRange("Cooked Dino Meat Ki Regen: ", 0.075, 0.0, 1.0);

        DINO_MEAT_COOKED_STAMREGEN = BUILDER.comment("Stamina Regeneration for Cooked Dino Meat (Min: 0.0 / Max: 1.0 / Default: 0.02)")
                .defineInRange("Cooked Dino Meat Stamina Regen: ", 0.075, 0.0, 1.0);

        FROG_LEGS_RAW_HEALTHREGEN = BUILDER.comment("Health Regeneration for Raw Frog Legs (Min: 0.0 / Max: 1.0 / Default: 0.02)")
                .defineInRange("Raw Frog Legs Health Regen: ", 0.02, 0.0, 1.0);

        FROG_LEGS_RAW_KIREGEN = BUILDER.comment("Ki Regeneration for Raw Frog Legs (Min: 0.0 / Max: 1.0 / Default: 0.02)")
                .defineInRange("Raw Frog Legs Ki Regen: ", 0.01, 0.0, 1.0);

        FROG_LEGS_RAW_STAMREGEN = BUILDER.comment("Stamina Regeneration for Raw Frog Legs (Min: 0.0 / Max: 1.0 / Default: 0.02)")
                .defineInRange("Raw Frog Legs Stamina Regen: ", 0.01, 0.0, 1.0);

        FROG_LEGS_COOKED_HEALTHREGEN = BUILDER.comment("Health Regeneration for Cooked Frog Legs (Min: 0.0 / Max: 1.0 / Default: 0.07)")
                .defineInRange("Cooked Frog Legs Health Regen: ", 0.07, 0.0, 1.0);

        FROG_LEGS_COOKED_KIREGEN = BUILDER.comment("Ki Regeneration for Cooked Frog Legs (Min: 0.0 / Max: 1.0 / Default: 0.04)")
                .defineInRange("Cooked Frog Legs Ki Regen: ", 0.04, 0.0, 1.0);

        FROG_LEGS_COOKED_STAMREGEN = BUILDER.comment("Stamina Regeneration for Cooked Frog Legs (Min: 0.0 / Max: 1.0 / Default: 0.04)")
                .defineInRange("Cooked Frog Legs Stamina Regen: ", 0.04, 0.0, 1.0);

        SPEC = BUILDER.build();
    }
}
