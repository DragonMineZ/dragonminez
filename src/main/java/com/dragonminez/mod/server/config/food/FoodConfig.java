package com.dragonminez.mod.server.config.food;

/**
 * Represents the configuration for a food item that provides regeneration effects. This includes
 * health, ki, and stamina regeneration values.
 */
public class FoodConfig {

  /**
   * The item ID (e.g., "minecraft:apple") associated with this food configuration.
   */
  private String itemID;

  /**
   * The amount of health regenerated when this food is consumed.
   */
  private double healthRegen;

  /**
   * The amount of ki regenerated when this food is consumed.
   */
  private double kiRegen;

  /**
   * The amount of stamina regenerated when this food is consumed.
   */
  private double staminaRegen;

  /**
   * No-arg constructor for deserialization.
   */
  public FoodConfig() {
  }

  /**
   * Constructs a new {@link FoodConfig} with the specified values.
   *
   * @param itemID       the identifier of the item
   * @param healthRegen  the amount of health regeneration
   * @param kiRegen      the amount of ki regeneration
   * @param staminaRegen the amount of stamina regeneration
   */
  public FoodConfig(String itemID, double healthRegen, double kiRegen, double staminaRegen) {
    this.itemID = itemID;
    this.healthRegen = healthRegen;
    this.kiRegen = kiRegen;
    this.staminaRegen = staminaRegen;
  }

  /**
   * @return the item ID for this food configuration
   */
  public String getItemID() {
    return itemID;
  }

  /**
   * @return the health regeneration value
   */
  public double getHealthRegen() {
    return healthRegen;
  }

  /**
   * @return the ki regeneration value
   */
  public double getKiRegen() {
    return kiRegen;
  }

  /**
   * @return the stamina regeneration value
   */
  public double getStaminaRegen() {
    return staminaRegen;
  }
}
