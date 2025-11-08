package com.dragonminez;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import net.minecraftforge.fml.common.Mod;

@Mod(Reference.MOD_ID)
public class DragonMineZ {

	public DragonMineZ() {
		LogUtil.info(Env.COMMON, "Inicializando DragonMineZ...");

		ConfigManager.initialize();
		NetworkHandler.register();

		LogUtil.info(Env.COMMON, "DragonMineZ inicializado correctamente");
	}
}
