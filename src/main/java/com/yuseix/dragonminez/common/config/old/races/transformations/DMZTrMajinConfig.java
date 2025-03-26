package com.yuseix.dragonminez.common.config.old.races.transformations;

import net.minecraftforge.common.ForgeConfigSpec;

public class DMZTrMajinConfig {

    // FORMA BASE
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_BASE;
    public static final ForgeConfigSpec.ConfigValue<Integer> BASE_FORM_KI_COST;
    /// FORMA EVIL
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_EVIL_FORM_STR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_EVIL_FORM_DEF;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_EVIL_FORM_PWR;
    public static final ForgeConfigSpec.ConfigValue<Integer> EVIL_FORM_KI_COST;
    // FORMA KID
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_KID_FORM_STR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_KID_FORM_DEF;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_KID_FORM_PWR;
    public static final ForgeConfigSpec.ConfigValue<Integer> KID_FORM_KI_COST;
    // FORMA SUPER
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SUPER_FORM_STR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SUPER_FORM_DEF;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SUPER_FORM_PWR;
    public static final ForgeConfigSpec.ConfigValue<Integer> SUPER_FORM_KI_COST;
    // FORMA ULTRA
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_ULTRA_FORM_STR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_ULTRA_FORM_DEF;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_ULTRA_FORM_PWR;
    public static final ForgeConfigSpec.ConfigValue<Integer> ULTRA_FORM_KI_COST;

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    static {
        //Transformaciones
        BUILDER.comment(" MAJIN FORMS ");
        BUILDER.comment(" Note: The Ki Cost represents the exact ki the player will lose per second while transformed. It's not a one-time cost or a percentage of the player's ki.");

        BUILDER.push(" Base Form");

        MULTIPLIER_BASE = BUILDER.comment("Multiplier for Base Form STR! (Min: 0.1 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Multiplier STR", 1.0, 0.1, 200.0);

        BASE_FORM_KI_COST = BUILDER.comment("Ki Cost for Base Form! (Min: 0 / Max: 20000 / Default: 0)")
                .defineInRange("Ki Cost", 0, 0, 20000);

        BUILDER.pop();

        BUILDER.push("Evil Form");

        MULTIPLIER_EVIL_FORM_STR = BUILDER.comment("Multiplier for Evil Form STR! (Min: 0.1 / Max: 200.0 / Default: 1.5)")
                .defineInRange("Multiplier STR", 1.5, 0.1, 200.0);

        MULTIPLIER_EVIL_FORM_DEF = BUILDER.comment("Multiplier for Evil Form DEF! (Min: 0.1 / Max: 200.0 / Default: 1.5)")
                .defineInRange("Multiplier DEF", 1.5, 0.1, 200.0);

        MULTIPLIER_EVIL_FORM_PWR = BUILDER.comment("Multiplier for Evil Form PWR! (Min: 0.1 / Max: 200.0 / Default: 1.5)")
                .defineInRange("Multiplier PWR", 1.5, 0.1, 200.0);

        EVIL_FORM_KI_COST = BUILDER.comment("Ki Cost for Evil Form! (Min: 0 / Max: 20000 / Default: 0)")
                .defineInRange("Ki Cost", 0, 0, 20000);

        BUILDER.pop();

        BUILDER.push("Kid Form");

        MULTIPLIER_KID_FORM_STR = BUILDER.comment("Multiplier for Kid Form STR! (Min: 0.1 / Max: 200.0 / Default: 2.25)")
                .defineInRange("Multiplier STR", 2.25, 0.1, 200.0);

        MULTIPLIER_KID_FORM_DEF = BUILDER.comment("Multiplier for Kid Form DEF! (Min: 0.1 / Max: 200.0 / Default: 2.25)")
                .defineInRange("Multiplier DEF", 2.25, 0.1, 200.0);

        MULTIPLIER_KID_FORM_PWR = BUILDER.comment("Multiplier for Kid Form PWR! (Min: 0.1 / Max: 200.0 / Default: 2.25)")
                .defineInRange("Multiplier PWR", 2.25, 0.1, 200.0);

        KID_FORM_KI_COST = BUILDER.comment("Ki Cost for Kid Form! (Min: 0 / Max: 20000 / Default: 0)")
                .defineInRange("Ki Cost", 0, 0, 20000);

        BUILDER.pop();

        BUILDER.push("Super Form");

        MULTIPLIER_SUPER_FORM_STR = BUILDER.comment("Multiplier for Super Form STR! (Min: 0.1 / Max: 200.0 / Default: 3.0)")
                .defineInRange("Multiplier STR", 3.0, 0.1, 200.0);

        MULTIPLIER_SUPER_FORM_DEF = BUILDER.comment("Multiplier for Super Form DEF! (Min: 0.1 / Max: 200.0 / Default: 3.0)")
                .defineInRange("Multiplier DEF", 3.0, 0.1, 200.0);

        MULTIPLIER_SUPER_FORM_PWR = BUILDER.comment("Multiplier for Super Form PWR! (Min: 0.1 / Max: 200.0 / Default: 3.0)")
                .defineInRange("Multiplier PWR", 3.0, 0.1, 200.0);

        SUPER_FORM_KI_COST = BUILDER.comment("Ki Cost for Super Form! (Min: 0 / Max: 20000 / Default: 0)")
                .defineInRange("Ki Cost", 0, 0, 20000);

        BUILDER.pop();

        BUILDER.push("Ultra Form");

        MULTIPLIER_ULTRA_FORM_STR = BUILDER.comment("Multiplier for Ultra Form STR! (Min: 0.1 / Max: 200.0 / Default: 3.5)")
                .defineInRange("Multiplier STR", 3.5, 0.1, 200.0);

        MULTIPLIER_ULTRA_FORM_DEF = BUILDER.comment("Multiplier for Ultra Form DEF! (Min: 0.1 / Max: 200.0 / Default: 3.5)")
                .defineInRange("Multiplier DEF", 3.5, 0.1, 200.0);

        MULTIPLIER_ULTRA_FORM_PWR = BUILDER.comment("Multiplier for Ultra Form PWR! (Min: 0.1 / Max: 200.0 / Default: 3.5)")
                .defineInRange("Multiplier PWR", 3.5, 0.1, 200.0);

        ULTRA_FORM_KI_COST = BUILDER.comment("Ki Cost for Ultra Form! (Min: 0 / Max: 20000 / Default: 650)")
                .defineInRange("Ki Cost", 650, 0, 20000);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}