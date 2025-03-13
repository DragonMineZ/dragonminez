package com.yuseix.dragonminez.config.races.transformations;

import net.minecraftforge.common.ForgeConfigSpec;

public class DMZTrNamekConfig {

    // FORMA BASE
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_BASE;
    public static final ForgeConfigSpec.ConfigValue<Integer> BASE_FORM_KI_COST;
    // FORMA GIANT
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_GIANT_FORM_STR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_GIANT_FORM_DEF;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_GIANT_FORM_PWR;
    public static final ForgeConfigSpec.ConfigValue<Integer> GIANT_FORM_KI_COST;
    // FORMA FULL POWER
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_FP_FORM_STR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_FP_FORM_DEF;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_FP_FORM_PWR;
    public static final ForgeConfigSpec.ConfigValue<Integer> FP_FORM_KI_COST;
    // FORMA SUPER NAMEK
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SUPER_NAMEK_FORM_STR;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SUPER_NAMEK_FORM_DEF;
    public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SUPER_NAMEK_FORM_PWR;
    public static final ForgeConfigSpec.ConfigValue<Integer> SUPER_NAMEK_FORM_KI_COST;

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    static {

        // TRANSFORMACIONES
        BUILDER.comment(" NAMEK FORMS ");
        BUILDER.comment(" Note: The Ki Cost represents the exact ki the player will lose per second while transformed. It's not a one-time cost or a percentage of the player's ki.");

        // FORMA BASE
        BUILDER.push(" Base Form");

        MULTIPLIER_BASE = BUILDER.comment("Multiplier for Base Form! (Min: 0.1 / Max: 200.0 / Default: 1.0)")
                .defineInRange("Multiplier: ", 1.0, 0.1, 200.0);

        BASE_FORM_KI_COST = BUILDER.comment("Ki Cost for Base Form! (Min: 0 / Max: 20000 / Default: 0)")
                .defineInRange("Ki Cost: ", 0, 0, 20000);

        BUILDER.pop();

        // FORMA GIANT
        BUILDER.push(" Giant Form");

        MULTIPLIER_GIANT_FORM_STR = BUILDER.comment("Multiplier for Giant Form STR! (Min: 1.0 / Max: 200.0 / Default: 1.5)")
                .defineInRange("Multiplier STR: ", 1.5, 0.1, 200.0);

        MULTIPLIER_GIANT_FORM_DEF = BUILDER.comment("Multiplier for Giant Form DEF! (Min: 1.0 / Max: 200.0 / Default: 1.5)")
                .defineInRange("Multiplier DEF: ", 1.5, 0.1, 200.0);

        MULTIPLIER_GIANT_FORM_PWR = BUILDER.comment("Multiplier for Giant Form PWR! (Min: 1.0 / Max: 200.0 / Default: 1.5)")
                .defineInRange("Multiplier PWR: ", 1.5, 0.1, 200.0);

        GIANT_FORM_KI_COST = BUILDER.comment("Ki Cost for Giant Form! (Min: 0 / Max: 20000 / Default: 300)")
                .defineInRange("Ki Cost: ", 300, 0, 20000);

        BUILDER.pop();

        // FORMA FULL POWER
        BUILDER.push(" Full Power Form");

        MULTIPLIER_FP_FORM_STR = BUILDER.comment("Multiplier for Full Power Form STR! (Min: 1.0 / Max: 200.0 / Default: 2.25)")
                .defineInRange("Multiplier STR: ", 2.25, 0.1, 200.0);

        MULTIPLIER_FP_FORM_DEF = BUILDER.comment("Multiplier for Full Power Form DEF! (Min: 1.0 / Max: 200.0 / Default: 2.25)")
                .defineInRange("Multiplier DEF: ", 2.25, 0.1, 200.0);

        MULTIPLIER_FP_FORM_PWR = BUILDER.comment("Multiplier for Full Power Form PWR! (Min: 1.0 / Max: 200.0 / Default: 2.25)")
                .defineInRange("Multiplier PWR: ", 2.25, 0.1, 200.0);

        FP_FORM_KI_COST = BUILDER.comment("Ki Cost for Full Power Form! (Min: 0 / Max: 20000 / Default: 250)")
                .defineInRange("Ki Cost: ", 250, 0, 20000);

        BUILDER.pop();

        // FORMA SUPER NAMEK
        BUILDER.push(" Super Namek Form");

        MULTIPLIER_SUPER_NAMEK_FORM_STR = BUILDER.comment("Multiplier for Super Namek Form STR! (Min: 1.0 / Max: 200.0 / Default: 3.0)")
                .defineInRange("Multiplier STR: ", 3.0, 0.1, 200.0);

        MULTIPLIER_SUPER_NAMEK_FORM_DEF = BUILDER.comment("Multiplier for Super Namek Form DEF! (Min: 1.0 / Max: 200.0 / Default: 3.0)")
                .defineInRange("Multiplier DEF: ", 3.0, 0.1, 200.0);

        MULTIPLIER_SUPER_NAMEK_FORM_PWR = BUILDER.comment("Multiplier for Super Namek Form PWR! (Min: 1.0 / Max: 200.0 / Default: 3.0)")
                .defineInRange("Multiplier PWR: ", 3.0, 0.1, 200.0);

        SUPER_NAMEK_FORM_KI_COST = BUILDER.comment("Ki Cost for Super Namek Form! (Min: 0 / Max: 20000 / Default: 350)")
                .defineInRange("Ki Cost: ", 350, 0, 20000);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}

