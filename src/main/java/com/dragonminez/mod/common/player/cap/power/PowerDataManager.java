package com.dragonminez.mod.common.player.cap.power;

import com.dragonminez.core.common.network.capability.PacketS2CCapSync;
import com.dragonminez.core.common.player.capability.CapDataManager;
import com.dragonminez.mod.common.network.player.cap.power.s2c.PacketS2CSyncPowerData;
import com.dragonminez.mod.common.network.player.cap.progress.s2c.PacketS2CSyncProgressData;
import com.dragonminez.mod.common.player.cap.power.PowerData.PowerDataHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import org.jetbrains.annotations.Nullable;

/**
 * Central manager for accessing and updating {@link PowerData} on players via Forge's capability
 * system.
 * <p>
 * This class serves as the main point of interaction for power-related capability data. It is
 * designed as a singleton, accessible via {@link PowerDataManager#INSTANCE}.
 */
public class PowerDataManager extends CapDataManager<PowerData> {

  /**
   * Singleton instance of the power data manager.
   */
  public static final PowerDataManager INSTANCE = new PowerDataManager();

  /**
   * Protected constructor to enforce singleton usage. Registers the capability using Forge's
   * {@link CapabilityManager} with a generic {@link CapabilityToken}.
   */
  protected PowerDataManager() {
    super(PowerDataHolder.ID, CapabilityManager.get(new CapabilityToken<>() {}));
  }

  /**
   * Returns the current power release value (e.g., ZPoints) for the specified player.
   * <p>
   * If the value is not present or invalid, returns {@code 0} as a fallback.
   *
   * @param player the player to query.
   * @return the player's current power release, or {@code 0} if not found.
   */
  public int getPowerRelease(Player player) {
    if (!(this.get(player, PowerDataHolder.POWER_RELEASE.id()) instanceof Integer powerRelease)) {
      return 0;
    }
    return powerRelease;
  }

  /**
   * Returns the current form release value for the specified player.
   * <p>
   * If the value is not present or invalid, returns {@code 0} as a fallback.
   *
   * @param player the player to query.
   * @return the player's current form release, or {@code 0} if not found.
   */
  public int getFormRelease(Player player) {
    if (!(this.get(player, PowerDataHolder.FORM_RELEASE.id()) instanceof Integer formRelease)) {
      return 0;
    }
    return formRelease;
  }

  /**
   * Returns whether the aura is currently active for the specified player.
   * <p>
   * If the value is not present or invalid, returns {@code false} as a fallback.
   *
   * @param player the player to query.
   * @return {@code true} if the aura is on, otherwise {@code false}.
   */
  public boolean isAuraOn(Player player) {
    if (!(this.get(player, PowerDataHolder.AURA_STATE.id()) instanceof Boolean auraState)) {
      return false;
    }
    return auraState;
  }

  /**
   * Builds the sync packet used to send the {@link PowerData} from server to client.
   *
   * @param player   the player to sync (can be {@code null}).
   * @param data     the capability data to sync (can be {@code null}).
   * @param isPublic whether the sync should be visible to other players (ignored here).
   * @return the constructed {@link PacketS2CSyncProgressData} for syncing to client.
   */
  @Override
  public PacketS2CCapSync<PowerData> buildSyncPacket(@Nullable Player player,
      @Nullable PowerData data,
      @Nullable Boolean isPublic) {
    return new PacketS2CSyncPowerData(data, player == null ? null : player.getUUID());
  }

  /**
   * Creates a new instance of {@link PowerData}. This is used when the capability is first attached
   * to a player.
   *
   * @return a new {@link PowerData} object.
   */
  @Override
  public PowerData buildCap() {
    return new PowerData();
  }
}
