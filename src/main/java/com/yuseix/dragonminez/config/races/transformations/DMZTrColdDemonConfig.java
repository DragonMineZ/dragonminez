package com.yuseix.dragonminez.config.races.transformations;

import net.minecraftforge.common.ForgeConfigSpec;

public class DMZTrColdDemonConfig {

    // FORMA BASE
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_BASE;
    public static final ForgeConfigSpec.ConfigValue<Integer> BASE_FORM_KI_COST;
    // FORMA SECOND
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SECOND_FORM_STR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SECOND_FORM_DEF;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SECOND_FORM_PWR;
    public static final ForgeConfigSpec.ConfigValue<Integer> SECOND_FORM_KI_COST;
    // FORMA THIRD
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_THIRD_FORM_STR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_THIRD_FORM_DEF;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_THIRD_FORM_PWR;
    public static final ForgeConfigSpec.ConfigValue<Integer> THIRD_FORM_KI_COST;
    // FORMA BASE
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_FOURTH_FORM_STR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_FOURTH_FORM_DEF;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_FOURTH_FORM_PWR;
    public static final ForgeConfigSpec.ConfigValue<Integer> FOURTH_FORM_KI_COST;
    // FORMA FULL POWER
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_FULL_POWER_FORM_STR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_FULL_POWER_FORM_DEF;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_FULL_POWER_FORM_PWR;
    public static final ForgeConfigSpec.ConfigValue<Integer> FULL_POWER_FORM_KI_COST;

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    static {

        // TRANSFORMACIONES
        BUILDER.comment(" ColdDemon FORMS ");
        BUILDER.comment(" Note: The Ki Cost represents the exact ki the player will lose per second while transformed. It's not a one-time cost or a percentage of the player's ki.");

        // FORMA MINIMAL
        BUILDER.push(" Minimal Form");

        MULTIPLIER_BASE = BUILDER.comment("Multiplier for Minimal Form! (Min: 0.1 / Max: 200.0 / Default: 0.8)")
                .defineInRange("Multiplier: ", 0.8, 0.1, 200.0);

        BASE_FORM_KI_COST = BUILDER.comment("Ki Cost for Minimal Form! (Min: 0 / Max: 20000 / Default: 0)")
                .defineInRange("Ki Cost: ", 0, 0, 20000);

        BUILDER.pop();

        // FORMA SECOND
        BUILDER.push(" Second Form");

        MULTIPLIER_SECOND_FORM_STR = BUILDER.comment("Multiplier for Second Form STR! (Min: 0.1 / Max: 200.0 / Default: 1.15)")
                .defineInRange("Multiplier STR: ", 1.15, 0.1, 200.0);

        MULTIPLIER_SECOND_FORM_DEF = BUILDER.comment("Multiplier for Second Form DEF! (Min: 0.1 / Max: 200.0 / Default: 1.15)")
                .defineInRange("Multiplier DEF: ", 1.15, 0.1, 200.0);

        MULTIPLIER_SECOND_FORM_PWR = BUILDER.comment("Multiplier for Second Form PWR! (Min: 0.1 / Max: 200.0 / Default: 1.15)")
                .defineInRange("Multiplier PWR: ", 1.15, 0.1, 200.0);

        SECOND_FORM_KI_COST = BUILDER.comment("Ki Cost for Second Form! (Min: 0 / Max: 20000 / Default: 0)")
                .defineInRange("Ki Cost: ", 0, 0, 20000);

        BUILDER.pop();

        // FORMA THIRD
        BUILDER.push(" Third Form");

        MULTIPLIER_THIRD_FORM_STR = BUILDER.comment("Multiplier for Third Form STR! (Min: 0.1 / Max: 200.0 / Default: 1.5)")
                .defineInRange("Multiplier STR: ", 1.5, 0.1, 200.0);

        MULTIPLIER_THIRD_FORM_DEF = BUILDER.comment("Multiplier for Third Form DEF! (Min: 0.1 / Max: 200.0 / Default: 1.5)")
                .defineInRange("Multiplier DEF: ", 1.5, 0.1, 200.0);

        MULTIPLIER_THIRD_FORM_PWR = BUILDER.comment("Multiplier for Third Form PWR! (Min: 0.1 / Max: 200.0 / Default: 1.5)")
                .defineInRange("Multiplier PWR: ", 1.5, 0.1, 200.0);

        THIRD_FORM_KI_COST = BUILDER.comment("Ki Cost for Third Form! (Min: 0 / Max: 20000 / Default: 0)")
                .defineInRange("Ki Cost: ", 0, 0, 20000);

        BUILDER.pop();

        // FORMA BASE
        BUILDER.push(" Base Form");

        MULTIPLIER_FOURTH_FORM_STR = BUILDER.comment("Multiplier for Base Form STR! (Min: 0.1 / Max: 200.0 / Default: 2.5)")
                .defineInRange("Multiplier STR: ", 2.5, 0.1, 200.0);

        MULTIPLIER_FOURTH_FORM_DEF = BUILDER.comment("Multiplier for Base Form DEF! (Min: 0.1 / Max: 200.0 / Default: 2.5)")
                .defineInRange("Multiplier DEF: ", 2.5, 0.1, 200.0);

        MULTIPLIER_FOURTH_FORM_PWR = BUILDER.comment("Multiplier for Base Form PWR! (Min: 0.1 / Max: 200.0 / Default: 2.5)")
                .defineInRange("Multiplier PWR: ", 2.5, 0.1, 200.0);

        FOURTH_FORM_KI_COST = BUILDER.comment("Ki Cost for Base Form! (Min: 0 / Max: 20000 / Default: 0)")
                .defineInRange("Ki Cost: ", 0, 0, 20000);

        BUILDER.pop();

        // FORMA FULL POWER
        BUILDER.push(" Full Power Form");

        MULTIPLIER_FULL_POWER_FORM_STR = BUILDER.comment("Multiplier for Full Power Form STR! (Min: 0.1 / Max: 200.0 / Default: 3.25)")
                .defineInRange("Multiplier STR: ", 3.25, 0.1, 200.0);

        MULTIPLIER_FULL_POWER_FORM_DEF = BUILDER.comment("Multiplier for Full Power Form DEF! (Min: 0.1 / Max: 200.0 / Default: 3.25)")
                .defineInRange("Multiplier DEF: ", 3.25, 0.1, 200.0);

        MULTIPLIER_FULL_POWER_FORM_PWR = BUILDER.comment("Multiplier for Full Power Form PWR! (Min: 0.1 / Max: 200.0 / Default: 3.25)")
                .defineInRange("Multiplier PWR: ", 3.25, 0.1, 200.0);

        FULL_POWER_FORM_KI_COST = BUILDER.comment("Ki Cost for Full Power Form! (Min: 0 / Max: 20000 / Default: 450)")
                .defineInRange("Ki Cost: ", 450, 0, 20000);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}

