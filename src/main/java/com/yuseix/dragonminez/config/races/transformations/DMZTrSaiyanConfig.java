package com.yuseix.dragonminez.config.races.transformations;

import net.minecraftforge.common.ForgeConfigSpec;

public class DMZTrSaiyanConfig {

    //FORMA BASE
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_BASE;
    public static final ForgeConfigSpec.ConfigValue<Integer> BASE_FORM_KI_COST;
    //FORMA OOZARU
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_OOZARU_FORM_STR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_OOZARU_FORM_DEF;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_OOZARU_FORM_PWR;
    public static final ForgeConfigSpec.ConfigValue<Integer> OOZARU_FORM_KI_COST;
    // FORMA SSJ
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SSJ_FORM_STR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SSJ_FORM_DEF;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SSJ_FORM_PWR;
    public static final ForgeConfigSpec.ConfigValue<Integer> SSJ_FORM_KI_COST;
    // FORMA SSGRADE 2
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SSGRADE2_FORM_STR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SSGRADE2_FORM_DEF;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SSGRADE2_FORM_PWR;
    public static final ForgeConfigSpec.ConfigValue<Integer> SSGRADE2_FORM_KI_COST;
    // FORMA SSGRADE 3
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SSGRADE3_FORM_STR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SSGRADE3_FORM_DEF;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SSGRADE3_FORM_PWR;
    public static final ForgeConfigSpec.ConfigValue<Integer> SSGRADE3_FORM_KI_COST;
    // FORMA MSSJ
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_MSSJ_FORM_STR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_MSSJ_FORM_DEF;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_MSSJ_FORM_PWR;
    public static final ForgeConfigSpec.ConfigValue<Integer> MSSJ_FORM_KI_COST;
    // FORMA SSJ2
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SSJ2_FORM_STR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SSJ2_FORM_DEF;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SSJ2_FORM_PWR;
    public static final ForgeConfigSpec.ConfigValue<Integer> SSJ2_FORM_KI_COST;
    // FORMA SSJ3
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SSJ3_FORM_STR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SSJ3_FORM_DEF;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SSJ3_FORM_PWR;
    public static final ForgeConfigSpec.ConfigValue<Integer> SSJ3_FORM_KI_COST;
    // FORMA GOLDEN OOZARU
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_GOLDENOOZARU_FORM_STR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_GOLDENOOZARU_FORM_DEF;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_GOLDENOOZARU_FORM_PWR;
    public static final ForgeConfigSpec.ConfigValue<Integer> GOLDENOOZARU_FORM_KI_COST;

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    static {

        //Transformaciones

        BUILDER.comment(" SAIYAN FORMS ");
        BUILDER.comment(" Note: The Ki Cost represents the exact ki the player will lose per second while transformed. It's not a one-time cost or a percentage of the player's ki.");

        BUILDER.push(" Base Form");

        MULTIPLIER_BASE = BUILDER.comment("Multiplier for Base Form! (Min: 0.1 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Multiplier: ", 1.0, 0.1, 200.0);

        BASE_FORM_KI_COST = BUILDER.comment("Ki Cost for Base Form! (Min: 0 / Max: 20000 / Default: 0)")
                .defineInRange("Ki Cost: ", 0, 0, 20000);

        BUILDER.pop();

        BUILDER.push(" Oozaru Form");

        MULTIPLIER_OOZARU_FORM_STR = BUILDER.comment("Multiplier for Oozaru Form STR! (Min: 0.1 / Max: 200.0 / Default: 1.5)")
                .defineInRange("Multiplier STR: ", 1.5, 0.1, 200.0);

        MULTIPLIER_OOZARU_FORM_DEF = BUILDER.comment("Multiplier for Oozaru Form DEF! (Min: 0.1 / Max: 200.0 / Default: 1.5)")
                .defineInRange("Multiplier DEF: ", 1.5, 0.1, 200.0);

        MULTIPLIER_OOZARU_FORM_PWR = BUILDER.comment("Multiplier for Oozaru Form PWR! (Min: 0.1 / Max: 200.0 / Default: 1.5)")
                .defineInRange("Multiplier PWR: ", 1.5, 0.1, 200.0);

        OOZARU_FORM_KI_COST = BUILDER.comment("Ki Cost for Oozaru Form! (Min: 0 / Max: 20000 / Default: 125)")
                .defineInRange("Ki Cost: ", 125, 0, 20000);

        BUILDER.pop();

        BUILDER.push(" SSJ Form");

        MULTIPLIER_SSJ_FORM_STR = BUILDER.comment("Multiplier for SSJ Form STR! (Min: 0.1 / Max: 200.0 / Default: 2.0)")
                .defineInRange("Multiplier STR: ", 2.0, 0.1, 200.0);

        MULTIPLIER_SSJ_FORM_DEF = BUILDER.comment("Multiplier for SSJ Form DEF! (Min: 0.1 / Max: 200.0 / Default: 2.0)")
                .defineInRange("Multiplier DEF: ", 2.0, 0.1, 200.0);

        MULTIPLIER_SSJ_FORM_PWR = BUILDER.comment("Multiplier for SSJ Form PWR! (Min: 0.1 / Max: 200.0 / Default: 2.0)")
                .defineInRange("Multiplier PWR: ", 2.0, 0.1, 200.0);

        SSJ_FORM_KI_COST = BUILDER.comment("Ki Cost for SSJ Form! (Min: 0 / Max: 20000 / Default: 300)")
                .defineInRange("Ki Cost: ", 300, 0, 20000);

        BUILDER.pop();

        BUILDER.push(" SSGRADE2 Form");

        MULTIPLIER_SSGRADE2_FORM_STR = BUILDER.comment("Multiplier for SSGRADE2 Form STR! (Min: 0.1 / Max: 200.0 / Default: 2.5)")
                .defineInRange("Multiplier STR: ", 2.5, 0.1, 200.0);

        MULTIPLIER_SSGRADE2_FORM_DEF = BUILDER.comment("Multiplier for SSGRADE2 Form DEF! (Min: 0.1 / Max: 200.0 / Default: 2.5)")
                .defineInRange("Multiplier DEF: ", 2.5, 0.1, 200.0);

        MULTIPLIER_SSGRADE2_FORM_PWR = BUILDER.comment("Multiplier for SSGRADE2 Form PWR! (Min: 0.1 / Max: 200.0 / Default: 2.5)")
                .defineInRange("Multiplier PWR: ", 2.5, 0.1, 200.0);

        SSGRADE2_FORM_KI_COST = BUILDER.comment("Ki Cost for SSGRADE2 Form! (Min: 0 / Max: 20000 / Default: 400)")
                .defineInRange("Ki Cost: ", 400, 0, 20000);

        BUILDER.pop();

        BUILDER.push(" SSGRADE3 Form");

        MULTIPLIER_SSGRADE3_FORM_STR = BUILDER.comment("Multiplier for SSGRADE3 Form STR! (Min: 0.1 / Max: 200.0 / Default: 3.5)")
                .defineInRange("Multiplier STR: ", 3.5, 0.1, 200.0);

        MULTIPLIER_SSGRADE3_FORM_DEF = BUILDER.comment("Multiplier for SSGRADE3 Form DEF! (Min: 0.1 / Max: 200.0 / Default: 3.5)")
                .defineInRange("Multiplier DEF: ", 3.5, 0.1, 200.0);

        MULTIPLIER_SSGRADE3_FORM_PWR = BUILDER.comment("Multiplier for SSGRADE3 Form PWR! (Min: 0.1 / Max: 200.0 / Default: 3.5)")
                .defineInRange("Multiplier PWR: ", 3.5, 0.1, 200.0);

        SSGRADE3_FORM_KI_COST = BUILDER.comment("Ki Cost for SSGRADE3 Form! (Min: 0 / Max: 20000 / Default: 600)")
                .defineInRange("Ki Cost: ", 600, 0, 20000);

        BUILDER.pop();

        BUILDER.push("MSSJ Form");

        MULTIPLIER_MSSJ_FORM_STR = BUILDER.comment("Multiplier for MSSJ Form STR! (Min: 0.1 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Multiplier STR: ", 0.1, 0.1, 200.0);

        MULTIPLIER_MSSJ_FORM_DEF = BUILDER.comment("Multiplier for MSSJ Form DEF! (Min: 0.1 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Multiplier DEF: ", 0.1, 0.1, 200.0);

        MULTIPLIER_MSSJ_FORM_PWR = BUILDER.comment("Multiplier for MSSJ Form PWR! (Min: 0.1 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Multiplier PWR: ", 0.1, 0.1, 200.0);

        MSSJ_FORM_KI_COST = BUILDER.comment("Ki Cost for MSSJ Form! (Min: 0 / Max: 20000 / Default: 0)")
                .defineInRange("Ki Cost: ", 0, 0, 20000);

        BUILDER.pop();

        BUILDER.push(" SSJ2 Form");

        MULTIPLIER_SSJ2_FORM_STR = BUILDER.comment("Multiplier for SSJ2 Form STR! (Min: 0.1 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Multiplier STR: ", 0.1, 0.1, 200.0);

        MULTIPLIER_SSJ2_FORM_DEF = BUILDER.comment("Multiplier for SSJ2 Form DEF! (Min: 0.1 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Multiplier DEF: ", 0.1, 0.1, 200.0);

        MULTIPLIER_SSJ2_FORM_PWR = BUILDER.comment("Multiplier for SSJ2 Form PWR! (Min: 0.1 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Multiplier PWR: ", 0.1, 0.1, 200.0);

        SSJ2_FORM_KI_COST = BUILDER.comment("Ki Cost for SSJ2 Form! (Min: 0 / Max: 20000 / Default: 0)")
                .defineInRange("Ki Cost: ", 0, 0, 20000);

        BUILDER.pop();

        BUILDER.push(" SSJ3 Form");

        MULTIPLIER_SSJ3_FORM_STR = BUILDER.comment("Multiplier for SSJ3 Form STR! (Min: 0.1 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Multiplier STR: ", 0.1, 0.1, 200.0);

        MULTIPLIER_SSJ3_FORM_DEF = BUILDER.comment("Multiplier for SSJ3 Form DEF! (Min: 0.1 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Multiplier DEF: ", 0.1, 0.1, 200.0);

        MULTIPLIER_SSJ3_FORM_PWR = BUILDER.comment("Multiplier for SSJ3 Form PWR! (Min: 0.1 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Multiplier PWR: ", 0.1, 0.1, 200.0);

        SSJ3_FORM_KI_COST = BUILDER.comment("Ki Cost for SSJ3 Form! (Min: 0 / Max: 20000 / Default: 0)")
                .defineInRange("Ki Cost: ", 0, 0, 20000);

        BUILDER.pop();

        BUILDER.push(" GoldenOozaru Form");

        MULTIPLIER_GOLDENOOZARU_FORM_STR = BUILDER.comment("Multiplier for GoldenOozaru Form STR! (Min: 0.1 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Multiplier STR: ", 0.1, 0.1, 200.0);

        MULTIPLIER_GOLDENOOZARU_FORM_DEF = BUILDER.comment("Multiplier for GoldenOozaru Form DEF! (Min: 0.1 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Multiplier DEF: ", 0.1, 0.1, 200.0);

        MULTIPLIER_GOLDENOOZARU_FORM_PWR = BUILDER.comment("Multiplier for GoldenOozaru Form PWR! (Min: 0.1 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Multiplier PWR: ", 0.1, 0.1, 200.0);

        GOLDENOOZARU_FORM_KI_COST = BUILDER.comment("Ki Cost for GoldenOozaru Form! (Min: 0 / Max: 20000 / Default: 0)")
                .defineInRange("Ki Cost: ", 0, 0, 20000);


        SPEC = BUILDER.build();
    }
}
