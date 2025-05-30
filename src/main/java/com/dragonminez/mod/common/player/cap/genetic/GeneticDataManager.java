package com.dragonminez.mod.common.player.cap.genetic;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.core.common.player.capability.CapDataManager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * Central manager for accessing and updating {@link GeneticData} on players via Forge's capability
 * system.
 * <p>
 * Acts as the main point of interaction for genetic data capabilities, serving as a singleton. This
 * class extends {@link CapDataManager} to provide capability management specific to
 * {@link GeneticData}.
 * <p>
 * Access the global instance statically via {@link GeneticDataManager#INSTANCE}.
 */
public class GeneticDataManager extends CapDataManager<GeneticData> {

  /**
   * Singleton instance of the genetic data manager.
   */
  public static final GeneticDataManager INSTANCE = new GeneticDataManager();

  /**
   * Protected constructor to enforce singleton pattern. Registers the capability using Forge's
   * {@link CapabilityManager} with a generic {@link CapabilityToken}.
   */
  protected GeneticDataManager() {
    super(CapabilityManager.get(new CapabilityToken<>() {
    }));
  }

  /**
   * Retrieves the {@link GeneticData} for a given player.
   *
   * @param player The player whose genetic data is to be retrieved.
   * @return The player's genetic data, or null if not found.
   */
  public String getRace(Player player) {
    final GeneticData geneticData = this.retrieveData(player);
    return geneticData != null ? geneticData.getRace() : Reference.EMPTY;
  }

  /**
   * Retrieves the genetic form of the specified player.
   *
   * @param player The player whose genetic form is to be retrieved.
   * @return The genetic form of the player, or an empty string if not set.
   */
  public String getForm(Player player) {
    final GeneticData geneticData = this.retrieveData(player);
    return geneticData != null ? geneticData.getForm() : Reference.EMPTY;
  }

  /**
   * Builds a new instance of {@link GeneticData}.
   *
   * @return a new {@link GeneticData} object.
   */
  @Override
  public GeneticData buildCap() {
    return new GeneticData();
  }
}
