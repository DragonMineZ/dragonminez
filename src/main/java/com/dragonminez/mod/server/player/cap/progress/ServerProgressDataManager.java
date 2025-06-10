package com.dragonminez.mod.server.player.cap.progress;

import com.dragonminez.core.server.player.capability.IServerCapDataManager;
import com.dragonminez.mod.common.player.cap.progress.ProgressData;
import com.dragonminez.mod.common.player.cap.progress.ProgressData.ProgressDataHolder;
import com.dragonminez.mod.common.player.cap.progress.ProgressDataManager;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side manager for {@link ProgressData}, handling updates and synchronization
 * of progression-related data.
 * <p>
 * This class extends {@link ProgressDataManager} to provide server-specific mutation
 * and retrieval functionality and implements {@link IServerCapDataManager} to support
 * synchronization with connected clients.
 */
public class ServerProgressDataManager extends ProgressDataManager implements
    IServerCapDataManager<ProgressDataManager, ProgressData> {

  /**
   * Singleton instance of the server-side progression data manager.
   */
  public static final ServerProgressDataManager INSTANCE = new ServerProgressDataManager();

  private ServerProgressDataManager() {
    super();
  }

  /**
   * Sets the player's ZPoints value and optionally logs the change.
   * <p>
   * This method updates the value and synchronizes it with the client.
   *
   * @param player  the target server player
   * @param zpoints the new ZPoints value to assign
   * @param log     whether to log this change for auditing/debugging
   */
  public void setZPoints(ServerPlayer player, int zpoints, boolean log) {
    this.set(player, ProgressDataHolder.ZPOINTS.id(), zpoints, log);
  }

  @Override
  public ProgressDataManager manager() {
    return this;
  }
}
