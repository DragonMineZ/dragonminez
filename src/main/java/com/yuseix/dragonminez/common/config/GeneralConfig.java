package com.yuseix.dragonminez.common.config;

public class GeneralConfig {

    // Singleton instance of the configuration
    public static GeneralConfig INSTANCE = new GeneralConfig();

    // Configuration sections
    private final Attributes ATTRIBUTES;
    private final Training TRAINING;
    private final WorldGen WORLD_GEN;

    public GeneralConfig() {
        this.ATTRIBUTES = new Attributes();
        this.TRAINING = new Training();
        this.WORLD_GEN = new WorldGen();
    }

    /**
     * Attributes configuration section
     */
    public static class Attributes {
        public Attributes() {}

        // Maximum number of attributes a player can reach. (Min: 100 / Max: 100000 / Default: 5000)
        public int maxAttributes = 5000;

        // Fall Damage Multiplier Percentage (Min: 0.00 / Max: 1.00 / Default: 0.05)
        public float fallDamageMultiplier = 0.05F;
    }

    /**
     * Training configuration section
     */
    public static class Training {
        public Training() {}

        // Should player win ZPoints doing gameplay activities? KILLING/HITTING enemies (Default: true)
        public boolean enableDynamicGain = true;

        // ZPoints obtained per Hit (Min: 0 / Max: 100 / Default: 1)
        public int perhitGain = 1;

        // ZPoints obtained per Kill based on Enemy max Health (Min: 0.0 / Max: 1.0 / Default: 0.45)
        public double perkillGain = 0.45;

        // Multiplier for ZPoints Cost, this will essentially make the attribute scaling easier/harder
        // depending on how high the number is. (Min: 0.0 / Max: 20.0 / Default: 1.2)
        public double costMultiplier = 1.2;

        // Multiplier for ZPoints Gain (Min: 0.0 / Max: 20.0 / Default: 1.2)
        public double gainMultiplier = 1.2;
    }

    /**
     * World Generation configuration section
     */
    public static class WorldGen {
        public WorldGen() {}

        // Should Otherworld Dimension be Enabled? (Default: true)
        public boolean enableOtherworld = true;

        // Should Namek Dimension be Enabled? (Default: true)
        public boolean enableNamek = true;

        // Should Kami's Lookout Spawn in the World? (Default: true)
        public boolean enableKamilookout = true;

        // Should Goku's House Spawn in the World? (Default: true)
        public boolean enableGokuHouse = true;

        // Should Kame House Spawn in the World? (Default: true)
        public boolean enableKamehouse = true;

        // Should the Elder Guru's House Spawn in the World? (Default: true)
        public boolean enableElderGuru = true;
    }

    /**
     * Get the Attributes configuration
     */
    public static Attributes attributes() {
        return INSTANCE.ATTRIBUTES;
    }

    /**
     * Get the Training configuration
     */
    public static Training training() {
        return INSTANCE.TRAINING;
    }

    /**
     * Get the WorldGen configuration
     */
    public static WorldGen worldGen() {
        return INSTANCE.WORLD_GEN;
    }
}