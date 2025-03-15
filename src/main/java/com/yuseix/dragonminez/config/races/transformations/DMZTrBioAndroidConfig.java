package com.yuseix.dragonminez.config.races.transformations;

import net.minecraftforge.common.ForgeConfigSpec;

public class DMZTrBioAndroidConfig {

	//FORMA BASE
	public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_BASE;
	public static final ForgeConfigSpec.ConfigValue<Integer> BASE_FORM_KI_COST;
	// FORMA SEMI-PERFECT
	public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SEMIPERFECT_FORM_STR;
	public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SEMIPERFECT_FORM_DEF;
	public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_SEMIPERFECT_FORM_PWR;
	public static final ForgeConfigSpec.ConfigValue<Integer> SEMIPERFECT_FORM_KI_COST;
	// FORMA PERFECT
	public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_PERFECT_FORM_STR;
	public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_PERFECT_FORM_DEF;
	public static final ForgeConfigSpec.ConfigValue<Double> MULTIPLIER_PERFECT_FORM_PWR;
	public static final ForgeConfigSpec.ConfigValue<Integer> PERFECT_FORM_KI_COST;


	public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static final ForgeConfigSpec SPEC;

	static {

		//Transformaciones

		BUILDER.comment(" BioAndroid FORMS ");
		BUILDER.comment(" Note: The Ki Cost represents the exact ki the player will lose per second while transformed. It's not a one-time cost or a percentage of the player's ki.");

		BUILDER.push(" Imperfect Form");

		MULTIPLIER_BASE = BUILDER.comment("Multiplier for Imperfect Form! (Min: 0.1 / Max: 200.0 / Default: 1.25)")
				.defineInRange("Multiplier: ", 1.25, 0.1, 200.0);

		BASE_FORM_KI_COST = BUILDER.comment("Ki Cost for Imperfect Form! (Min: 0 / Max: 20000 / Default: 0)")
				.defineInRange("Ki Cost: ", 0, 0, 20000);

		BUILDER.pop();

		BUILDER.push(" Semi-Perfect Form");

		MULTIPLIER_SEMIPERFECT_FORM_STR = BUILDER.comment("Multiplier for Semi-Perfect Form STR! (Min: 0.1 / Max: 200.0 / Default: 2.0)")
				.defineInRange("Multiplier STR: ", 2.0, 0.1, 200.0);

		MULTIPLIER_SEMIPERFECT_FORM_DEF = BUILDER.comment("Multiplier for Semi-Perfect Form DEF! (Min: 0.1 / Max: 200.0 / Default: 2.0)")
				.defineInRange("Multiplier DEF: ", 2.0, 0.1, 200.0);

		MULTIPLIER_SEMIPERFECT_FORM_PWR = BUILDER.comment("Multiplier for Semi-Perfect Form PWR! (Min: 0.1 / Max: 200.0 / Default: 2.0)")
				.defineInRange("Multiplier PWR: ", 2.0, 0.1, 200.0);

		SEMIPERFECT_FORM_KI_COST = BUILDER.comment("Ki Cost for Semi-Perfect Form! (Min: 0 / Max: 20000 / Default: 0)")
				.defineInRange("Ki Cost: ", 0, 0, 20000);

		BUILDER.pop();

		BUILDER.push(" Perfect Form");

		MULTIPLIER_PERFECT_FORM_STR = BUILDER.comment("Multiplier for Perfect Form STR! (Min: 0.1 / Max: 200.0 / Default: 3.0)")
				.defineInRange("Multiplier STR: ", 3.0, 0.1, 200.0);

		MULTIPLIER_PERFECT_FORM_DEF = BUILDER.comment("Multiplier for Perfect Form DEF! (Min: 0.1 / Max: 200.0 / Default: 3.0)")
				.defineInRange("Multiplier DEF: ", 3.0, 0.1, 200.0);

		MULTIPLIER_PERFECT_FORM_PWR = BUILDER.comment("Multiplier for Perfect Form PWR! (Min: 0.1 / Max: 200.0 / Default: 3.0)")
				.defineInRange("Multiplier PWR: ", 3.0, 0.1, 200.0);

		PERFECT_FORM_KI_COST = BUILDER.comment("Ki Cost for Perfect Form! (Min: 0 / Max: 20000 / Default: 0)")
				.defineInRange("Ki Cost: ", 0, 0, 20000);

		BUILDER.pop();


		SPEC = BUILDER.build();
	}
}