package com.dragonminez.mod.server.config.dimensions;

public class DimensionConfig {

  // The dimension ID
  private String dimensionID;

  // Training-related config values
  private Training training;

  // World generation config values
  private WorldGen worldgen;

  // No-arg constructor for deserialization
  public DimensionConfig() {
  }

  // Main constructor
  public DimensionConfig(String dimensionID, Training training, WorldGen worldgen) {
    this.dimensionID = dimensionID;
    this.training = training;
    this.worldgen = worldgen;
  }

  public String dimensionID() {
    return dimensionID;
  }

  public Training training() {
    return training;
  }

  public WorldGen worldGen() {
    return worldgen;
  }

  public static class Training {

    /**
     * Multiplier for ZPoints gained by hitting an entity (Min: 1.0 / Max: 20.0 / Default: 1.0)
     */
    private double hitMultiplier = 1.0;

    /**
     * Multiplier for ZPoints gained by killing an entity (Min: 1.0 / Max: 20.0 / Default: 1.0)
     */
    private double killMultiplier = 1.0;

    // No-arg constructor for deserialization
    public Training() {
    }

    // Constructor with validation
    public Training(double hitMultiplier, double killMultiplier) {
      this.hitMultiplier = Math.max(1.0, Math.min(20.0, hitMultiplier));
      this.killMultiplier = Math.max(1.0, Math.min(20.0, killMultiplier));
    }

    public double getHitMultiplier() {
      return hitMultiplier;
    }

    public double getKillMultiplier() {
      return killMultiplier;
    }
  }

  public static class WorldGen {

    /**
     * Should the dimension have Dragon Balls? (Default: true)
     */
    private boolean spawnDragonBalls = true;

    /**
     * Should the dimension have a unique set of Dragon Balls? (Default: true)
     * <p><b>Disabling</b> this allows multiple sets of Dragon Balls to be placed.</p>
     * <p><b>Warning:</b> This may cause conflicts with tracking systems such as the Dragon Radar,
     * which only detects the first set.
     * Additionally, <b>Dragon Balls may disappear</b> when a new one is placed if this is
     * enabled.</p>
     */
    private boolean uniqueDragonBalls = true;

    /**
     * Range in blocks for the Dragon Balls to spawn (Min: 2000 / Max: 20000 / Default: 3000)
     */
    private int dballSpawnRange = 3000;

    /**
     * How many Dragon Balls should spawn (Min: 1 / Default: 7)
     */
    private int dragonBallCount = 7;

    /**
     * If true, the mod will create a custom block and you should add custom model and texture. If
     * false, the default Earth Dragon Ball model will be used.
     * <p><b>Note:</b> Custom models and textures should be added to the assets folder if this is
     * enabled.</p>
     */
    private boolean useCustomDragonBalls = false;

    // No-arg constructor for deserialization
    public WorldGen() {
    }

    // Constructor with validation
    public WorldGen(boolean spawnDragonBalls, boolean uniqueDragonBalls, int dballSpawnRange,
        int dragonBallCount, boolean useCustomDragonBalls) {
      this.spawnDragonBalls = spawnDragonBalls;
      this.uniqueDragonBalls = uniqueDragonBalls;
      this.dballSpawnRange = dballSpawnRange;
      this.dragonBallCount = Math.max(1, dragonBallCount); // Ensure at least one Dragon Ball
      this.useCustomDragonBalls = useCustomDragonBalls;
    }

    public boolean shouldSpawnDragonBalls() {
      return spawnDragonBalls;
    }

    public boolean hasUniqueDragonBalls() {
      return uniqueDragonBalls;
    }

    public int getDballSpawnRange() {
      return dballSpawnRange;
    }

    public int getDragonBallCount() {
      return dragonBallCount;
    }

    public boolean useCustomDragonBalls() {
      return useCustomDragonBalls;
    }
  }
}
