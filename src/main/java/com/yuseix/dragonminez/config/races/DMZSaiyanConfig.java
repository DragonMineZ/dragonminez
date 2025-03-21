package com.yuseix.dragonminez.config.races;

import net.minecraftforge.common.ForgeConfigSpec;

public class DMZSaiyanConfig {

    //PASIVA
    public static final ForgeConfigSpec.ConfigValue<Integer> ZENKAI_CANT;
    public static final ForgeConfigSpec.ConfigValue<Integer> ZENKAI_HEALTH_REGEN;
    public static final ForgeConfigSpec.ConfigValue<Integer> ZENKAI_STAT_BOOST;
    public static final ForgeConfigSpec.ConfigValue<Integer> ZENKAI_COOLDOWN;

    //WARRIOR
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_STR_WARRIOR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_DEF_WARRIOR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_CON_WARRIOR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_KIPOWER_WARRIOR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_ENERGY_WARRIOR;

    //STATS INICIALES
    public static final ForgeConfigSpec.ConfigValue<Integer> INITIAL_STR_WARRIOR;
    public static final ForgeConfigSpec.ConfigValue<Integer> INITIAL_DEF_WARRIOR;
    public static final ForgeConfigSpec.ConfigValue<Integer> INITIAL_CON_WARRIOR;
    public static final ForgeConfigSpec.ConfigValue<Integer> INITIAL_KIPWR_WARRIOR;
    public static final ForgeConfigSpec.ConfigValue<Integer> INITIAL_ENE_WARRIOR;

    //SPIRITUALIST
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_STR_SPIRITUALIST;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_DEF_SPIRITUALIST;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_CON_SPIRITUALIST;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_KIPOWER_SPIRITUALIST;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_ENERGY_SPIRITUALIST;
    //STATS INICIALES
    public static final ForgeConfigSpec.ConfigValue<Integer> INITIAL_STR_SPIRITUALIST;
    public static final ForgeConfigSpec.ConfigValue<Integer> INITIAL_DEF_SPIRITUALIST;
    public static final ForgeConfigSpec.ConfigValue<Integer> INITIAL_CON_SPIRITUALIST;
    public static final ForgeConfigSpec.ConfigValue<Integer> INITIAL_KIPWR_SPIRITUALIST;
    public static final ForgeConfigSpec.ConfigValue<Integer> INITIAL_ENE_SPIRITUALIST;

    //Regeneracion de ki por clase
    public static final ForgeConfigSpec.ConfigValue<Double> KI_REGEN_WARRIOR;
    public static final ForgeConfigSpec.ConfigValue<Double> KI_REGEN_SPIRITUALIST;


    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    static {

        BUILDER.comment(" DRAGONMINEZ - SAIYAN CLASS CONFIG ");

        //Pasiva
        BUILDER.push(" SAIYAN PASSIVE SKILL:");

        ZENKAI_CANT = BUILDER.comment("Quantity of Zenkai Activations! (Min: 0 / Max: 10 / Default: 3)")
                .defineInRange("Zenkai Activations: ", 3, 0, 10);

        ZENKAI_HEALTH_REGEN = BUILDER.comment("Health Regen for Zenkais in percentage! (Min: 0 / Max: 100 / Default: 30)")
                .defineInRange("Zenkai Health Regen: ", 30, 0, 100);

        ZENKAI_STAT_BOOST = BUILDER.comment("Stat Boost for Zenkais in percentage! (Min: 0 / Max: 30 / Default: 15)")
                .defineInRange("Zenkai Stat Boost: ", 15, 0, 30);

        ZENKAI_COOLDOWN = BUILDER.comment("Cooldown for Zenkai in minutes! (Min: 0 / Max: 600 / Default: 50)")
                .defineInRange("Zenkai Cooldown: ", 50, 0, 600);

        BUILDER.pop();

        //GUERRERO
        BUILDER.push(" WARRIOR CLASS MULTIPLIER");

        MULTIPLIER_STR_WARRIOR = BUILDER.comment("Multiplier for Strenght Warrior Attribute! (Min: 1.0 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Strength: ", 1.0, 1.0, 200.0);

        MULTIPLIER_DEF_WARRIOR = BUILDER.comment("Multiplier for Defense Warrior Attribute! (Min: 1.0 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Defense: ", 1.0, 1.0, 200.0);

        MULTIPLIER_CON_WARRIOR = BUILDER.comment("Multiplier for Constitution Warrior Attribute! (Min: 1.0 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Constitution: ", 1.0, 1.0, 200.0);

        MULTIPLIER_KIPOWER_WARRIOR = BUILDER.comment("Multiplier for KiPower Warrior Attribute! (Min: 1.0 / Max: 200.0 / Default: 1.0)")
                .defineInRange("KiPower: ", 1.0, 1.0, 200.0);

        MULTIPLIER_ENERGY_WARRIOR = BUILDER.comment("Multiplier for Max KI Warrior Attribute! (Min: 1.0 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Energy: ", 1.0, 1.0, 200.0);

        BUILDER.pop();

        BUILDER.push(" WARRIOR CLASS INITIAL STATS");

        INITIAL_STR_WARRIOR = BUILDER.comment("Initial Strenght Warrior STAT! (Min: 1 / Max: 200 / Default: 5)")
                .defineInRange("STR: ", 15, 1, 200);

        INITIAL_DEF_WARRIOR = BUILDER.comment("Initial Defense Warrior STAT! (Min: 1 / Max: 200 / Default: 5)")
                .defineInRange("DEF: ", 10, 1, 200);

        INITIAL_CON_WARRIOR = BUILDER.comment("Initial Constitution Warrior STAT! (Min: 1 / Max: 200 / Default: 5)")
                .defineInRange("CON: ", 10, 1, 200);

        INITIAL_KIPWR_WARRIOR = BUILDER.comment("Initial Ki Power Warrior STAT! (Min: 1 / Max: 200 / Default: 5)")
                .defineInRange("PWR: ", 5, 1, 200);

        INITIAL_ENE_WARRIOR = BUILDER.comment("Initial Energy Warrior STAT! (Min: 1 / Max: 200 / Default: 20)")
                .defineInRange("ENE: ", 5, 1, 200);

        BUILDER.pop();

        BUILDER.push(" WARRIOR CLASS KI REGENERATION");

        KI_REGEN_WARRIOR = BUILDER.comment("Ki Regeneration for Warrior Class IN PERCENTAGE! (Min: 0.0 / Max: 1.0 / Default: 0.01)")
                .defineInRange("Percentage: ", 0.01, 0.00, 1.00);

        BUILDER.pop();

        //ESPIRITUALISTA
        BUILDER.push(" SPIRITUALIST CLASS MULTIPLIER");


        MULTIPLIER_STR_SPIRITUALIST = BUILDER.comment("Multiplier for Strenght Spiritualist Attribute! (Min: 1.0 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Strength: ", 1.0, 1.0, 200.0);

        MULTIPLIER_DEF_SPIRITUALIST = BUILDER.comment("Multiplier for Defense Spiritualist Attribute! (Min: 1.0 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Defense: ", 1.0, 1.0, 200.0);

        MULTIPLIER_CON_SPIRITUALIST = BUILDER.comment("Multiplier for Constitution Spiritualist Attribute! (Min: 1.0 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Constitution: ", 1.0, 1.0, 200.0);

        MULTIPLIER_KIPOWER_SPIRITUALIST = BUILDER.comment("Multiplier for KiPower Spiritualist Attribute! (Min: 1.0 / Max: 200.0 / Default: 1.0)")
                .defineInRange("KiPower: ", 1.0, 1.0, 200.0);

        MULTIPLIER_ENERGY_SPIRITUALIST = BUILDER.comment("Multiplier for Max KI Spiritualist Attribute! (Min: 1.0 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Energy: ", 1.0, 1.0, 200.0);


        BUILDER.pop();

        BUILDER.push(" SPIRITUALIST CLASS INITIAL STATS");

        INITIAL_STR_SPIRITUALIST= BUILDER.comment("Initial Strenght Spiritualist STAT! (Min: 1 / Max: 200 / Default: 5)")
                .defineInRange("STR: ", 5, 1, 200);

        INITIAL_DEF_SPIRITUALIST = BUILDER.comment("Initial Defense Spiritualist STAT! (Min: 1 / Max: 200 / Default: 5)")
                .defineInRange("DEF: ", 5, 1, 200);

        INITIAL_CON_SPIRITUALIST = BUILDER.comment("Initial Constitution Spiritualist STAT! (Min: 1 / Max: 200 / Default: 5)")
                .defineInRange("CON: ", 5, 1, 200);

        INITIAL_KIPWR_SPIRITUALIST= BUILDER.comment("Initial Ki Power Spiritualist STAT! (Min: 1 / Max: 200 / Default: 15)")
                .defineInRange("PWR: ", 15, 1, 200);

        INITIAL_ENE_SPIRITUALIST = BUILDER.comment("Initial Energy Spiritualist STAT! (Min: 1 / Max: 200 / Default: 20)")
                .defineInRange("ENE: ", 20, 1, 200);

        BUILDER.pop();

        BUILDER.push(" SPIRITUALIST CLASS KI REGENERATION");

        KI_REGEN_SPIRITUALIST = BUILDER.comment("Ki Regeneration for Spiritualist Class IN PERCENTAGE! (Min: 0.0 / Max: 1.0 / Default: 0.01)")
                .defineInRange("Percentage: ", 0.01, 0.00, 1.00);

        BUILDER.pop();


        SPEC = BUILDER.build();
    }

}
