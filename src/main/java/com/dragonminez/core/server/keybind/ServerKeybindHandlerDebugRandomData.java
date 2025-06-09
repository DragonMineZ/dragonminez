package com.dragonminez.core.server.keybind;

import com.dragonminez.core.common.keybind.model.KeybindHandler;
import com.dragonminez.core.common.player.capability.CapManagerRegistry;
import com.dragonminez.core.server.player.capability.IServerCapDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;

/**
 * Debug-only keybind handler for server-side randomization of all registered capability data.
 * <p>
 * When activated, this keybind triggers {@link IServerCapDataManager#randomize(ServerPlayer, boolean)}
 * on all capability managers registered for the {@link Dist#DEDICATED_SERVER}, effectively
 * reinitializing all data with randomized values for the specified player.
 * <p>
 * This class is intended for development and testing purposes only.
 *
 * @see CapManagerRegistry#values(Dist)
 * @see IServerCapDataManager#randomize(ServerPlayer, boolean)
 * @see KeybindHandler
 */
//Goofy ass name
public class ServerKeybindHandlerDebugRandomData extends KeybindHandler {

  /**
   * Called when the keybind is pressed. Iterates through all server-side capability managers
   * and applies randomized data to the given player.
   *
   * @param player the player instance who triggered the keybind
   */
  @Override
  public void onPressed(Player player) {
    CapManagerRegistry.INSTANCE.values(Dist.DEDICATED_SERVER)
        .forEach(capDataManager -> {
          if (!(capDataManager instanceof IServerCapDataManager<?, ?> serverManager)) {
            return;
          }
          serverManager.randomize((ServerPlayer) player, true);
        });
  }
}
