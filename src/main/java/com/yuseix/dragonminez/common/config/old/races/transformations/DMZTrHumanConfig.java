package com.yuseix.dragonminez.common.config.old.races.transformations;

import net.minecraftforge.common.ForgeConfigSpec;

public class DMZTrHumanConfig {

    //FORMA BASE
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_BASE;
    public static final ForgeConfigSpec.ConfigValue<Integer> BASE_FORM_KI_COST;
    //FORMA BUFFED
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_BUFFED_FORM_STR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_BUFFED_FORM_DEF;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_BUFFED_FORM_PWR;
    public static final ForgeConfigSpec.ConfigValue<Integer> BUFFED_FORM_KI_COST;
    //FORMA FULL POWER
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_FP_FORM_STR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_FP_FORM_DEF;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_FP_FORM_PWR;
    public static final ForgeConfigSpec.ConfigValue<Integer> FP_FORM_KI_COST;
    //FORMA POTENTIAL UNLEASHED
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_PU_FORM_STR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_PU_FORM_DEF;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_PU_FORM_PWR;
    public static final ForgeConfigSpec.ConfigValue<Integer> PU_FORM_KI_COST;


    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    static {

        //Transformaciones

        BUILDER.comment(" HUMAN FORMS ");
        BUILDER.comment(" Note: The Ki Cost represents the exact ki the player will lose per second while transformed. It's not a one-time cost or a percentage of the player's ki.");

        BUILDER.push(" Base Form");

        MULTIPLIER_BASE = BUILDER.comment("Multiplier for Base Form! (Min: 0.1 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Multiplier: ", 1.0, 0.1, 200.0);

        BASE_FORM_KI_COST = BUILDER.comment("Ki Cost for Base Form! (Min: 0 / Max: 20000 / Default: 0)")
                .defineInRange("Ki Cost: ", 0, 0, 20000);

        BUILDER.pop();

        BUILDER.push(" Buffed Form");

        MULTIPLIER_BUFFED_FORM_STR = BUILDER.comment("Multiplier for Buffed Form STR! (Min: 0.1 / Max: 200.0 / Default: 1.5)")
                .defineInRange("Multiplier STR: ", 1.5, 0.1, 200.0);

        MULTIPLIER_BUFFED_FORM_DEF = BUILDER.comment("Multiplier for Buffed Form DEF! (Min: 0.1 / Max: 200.0 / Default: 1.5)")
                .defineInRange("Multiplier DEF: ", 1.5, 0.1, 200.0);

        MULTIPLIER_BUFFED_FORM_PWR = BUILDER.comment("Multiplier for Buffed Form PWR! (Min: 0.1 / Max: 200.0 / Default: 1.5)")
                .defineInRange("Multiplier PWR: ", 1.5, 0.1, 200.0);

        BUFFED_FORM_KI_COST = BUILDER.comment("Ki Cost for Buffed Form! (Min: 0 / Max: 20000 / Default: 350)")
                .defineInRange("Ki Cost: ", 350, 0, 20000);

        BUILDER.pop();

        BUILDER.push(" Full Power Form");

        MULTIPLIER_FP_FORM_STR = BUILDER.comment("Multiplier for FullPower Form STR! (Min: 0.1 / Max: 200.0 / Default: 2.25)")
                .defineInRange("Multiplier STR: ", 2.25, 0.1, 200.0);

        MULTIPLIER_FP_FORM_DEF = BUILDER.comment("Multiplier for FullPower Form DEF! (Min: 0.1 / Max: 200.0 / Default: 2.25)")
                .defineInRange("Multiplier DEF: ", 2.25, 0.1, 200.0);

        MULTIPLIER_FP_FORM_PWR = BUILDER.comment("Multiplier for FullPower Form PWR! (Min: 0.1 / Max: 200.0 / Default: 2.25)")
                .defineInRange("Multiplier PWR: ", 2.25, 0.1, 200.0);

        FP_FORM_KI_COST = BUILDER.comment("Ki Cost for FullPower Form! (Min: 0 / Max: 20000 / Default: 350)")
                .defineInRange("Ki Cost: ", 350, 0, 20000);

        BUILDER.pop();

        BUILDER.push(" Potential Unleashed Form");

        MULTIPLIER_PU_FORM_STR = BUILDER.comment("Multiplier for Potential Unleashed Form STR! (Min: 0.1 / Max: 200.0 / Default: 3.0)")
                .defineInRange("Multiplier STR: ", 3.0, 0.1, 200.0);

        MULTIPLIER_PU_FORM_DEF = BUILDER.comment("Multiplier for Potential Unleashed Form DEF! (Min: 0.1 / Max: 200.0 / Default: 3.0)")
                .defineInRange("Multiplier DEF: ", 3.0, 0.1, 200.0);

        MULTIPLIER_PU_FORM_PWR = BUILDER.comment("Multiplier for Potential Unleashed Form PWR! (Min: 0.1 / Max: 200.0 / Default: 3.0)")
                .defineInRange("Multiplier PWR: ", 3.0, 0.1, 200.0);

        PU_FORM_KI_COST = BUILDER.comment("Ki Cost for Potential Unleashed Form! (Min: 0 / Max: 20000 / Default: 450)")
                .defineInRange("Ki Cost: ", 450, 0, 20000);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}