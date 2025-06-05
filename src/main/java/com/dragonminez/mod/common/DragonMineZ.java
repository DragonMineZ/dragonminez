package com.dragonminez.mod.common;

import com.dragonminez.mod.client.DragonMineClient;
import com.dragonminez.mod.common.registry.ConfigRegistry;
import com.dragonminez.mod.core.common.network.NetworkManager;
import com.dragonminez.mod.core.common.config.ConfigManager;
import com.dragonminez.mod.server.DragonMineServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import software.bernie.geckolib.GeckoLib;

/**
 * The main class for the DragonMineZ mod.
 * <p>
 * This class is responsible for initializing the mod and its dependencies.
 * </p>
 * <p>
 * <strong>Credits to:</strong><br>
 * Yuseix300 - Founder<br> ezShokkoh - Founder<br> KyoSleep - Tech Lead<br> Bruno - Community
 * Admin<br> And many more amazing contributors and supporters. Refer to README for more info.
 * </p>
 * <p>
 * <strong>License:</strong><br>
 * This mod is distributed under the GNU General Public License v3.0. Third-party tools like
 * GeckoLib are used under their respective open-source licenses.
 * </p>
 */
@Mod(Reference.MOD_ID)
public class DragonMineZ {

  public DragonMineZ() {
    this.instance();
    this.registry();
    this.manager();
    GeckoLib.initialize();
  }

  private void instance() {
    if (FMLEnvironment.dist.isClient()) {
      DragonMineClient.init();
    }
    DragonMineServer.init();
  }

  private void registry() {
    ConfigRegistry.init();
  }

  private void manager() {
    NetworkManager.INSTANCE.init();
    ConfigManager.INSTANCE.init();
  }
}