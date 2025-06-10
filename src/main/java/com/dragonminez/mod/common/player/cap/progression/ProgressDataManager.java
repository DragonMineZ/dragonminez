package com.dragonminez.mod.common.player.cap.progression;

import com.dragonminez.core.common.network.capability.PacketS2CCapSync;
import com.dragonminez.core.common.player.capability.CapDataManager;
import com.dragonminez.mod.common.network.player.cap.progress.s2c.PacketS2CSyncProgressData;
import com.dragonminez.mod.common.player.cap.progression.ProgressData.ProgressDataHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import org.jetbrains.annotations.Nullable;

/**
 * Central manager for accessing and updating {@link ProgressData} on players via Forge's capability system.
 * <p>
 * This class serves as the main point of interaction for progression-related capability data.
 * It is designed as a singleton, accessible via {@link ProgressDataManager#INSTANCE}.
 */
public class ProgressDataManager extends CapDataManager<ProgressData> {

  /**
   * Singleton instance of the progression data manager.
   */
  public static final ProgressDataManager INSTANCE = new ProgressDataManager();

  /**
   * Protected constructor to enforce singleton usage. Registers the capability using Forge's
   * {@link CapabilityManager} with a generic {@link CapabilityToken}.
   */
  protected ProgressDataManager() {
    super(ProgressDataHolder.ID, CapabilityManager.get(new CapabilityToken<>() {}));
  }

  /**
   * Returns the current ZPoints value for the specified player.
   * <p>
   * If the value is not present or invalid, returns {@code 0} as a fallback.
   *
   * @param player the player to query.
   * @return the player's current ZPoints, or {@code 0} if not found.
   */
  public int getZPoints(Player player) {
    if (!(this.get(player, ProgressDataHolder.ZPOINTS.id()) instanceof Integer zPoints)) {
      return 0;
    }
    return zPoints;
  }

  /**
   * Builds the sync packet used to send the {@link ProgressData} from server to client.
   *
   * @param player   the player to sync (can be {@code null}).
   * @param data     the capability data to sync (can be {@code null}).
   * @param isPublic whether the sync should be visible to other players (ignored here).
   * @return the constructed {@link PacketS2CSyncProgressData} for syncing to client.
   */
  @Override
  public PacketS2CCapSync<ProgressData> buildSyncPacket(@Nullable Player player,
      @Nullable ProgressData data,
      @Nullable Boolean isPublic) {
    return new PacketS2CSyncProgressData(data, player == null ? null : player.getUUID());
  }

  /**
   * Creates a new instance of {@link ProgressData}.
   * This is used when the capability is first attached to a player.
   *
   * @return a new {@link ProgressData} object.
   */
  @Override
  public ProgressData buildCap() {
    return new ProgressData();
  }
}
