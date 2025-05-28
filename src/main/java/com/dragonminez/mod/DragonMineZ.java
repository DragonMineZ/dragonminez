package com.dragonminez.mod;

import com.dragonminez.mod.client.registry.KeybindRegistry;
import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.common.registry.ConfigRegistry;
import com.dragonminez.mod.common.registry.NetworkRegistry;
import com.dragonminez.mod.core.common.config.ConfigManager;
import net.minecraftforge.fml.common.Mod;
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
    this.registry();
    this.manager();
    GeckoLib.initialize();
  }

  private void registry() {
    ConfigRegistry.init();
    KeybindRegistry.init();
    NetworkRegistry.init();
  }

  private void manager() {
    ConfigManager.INSTANCE.init();
  }
}