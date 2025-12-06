package com.dragonminez;

import com.dragonminez.client.DMZClient;
import com.dragonminez.common.DMZCommon;
import com.dragonminez.server.DMZServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(Reference.MOD_ID)
public class DragonMineZ {

	public DragonMineZ() {
		LogUtil.info(Env.COMMON, "Initializing DragonMineZ...");

		DMZCommon.init();

		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> DMZClient::init);
		DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> DMZServer::init);

		LogUtil.info(Env.COMMON, "DragonMineZ initialized successfully");
	}
}
