package com.dragonminez.server;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.server.database.DatabaseManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.DEDICATED_SERVER)
public class DMZServer {

    public static void init() {
        LogUtil.info(Env.SERVER, "Initializing DragonMineZ Server...");

		DatabaseManager.init();

        LogUtil.info(Env.SERVER, "DragonMineZ Server initialized");
    }
}

