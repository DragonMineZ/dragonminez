package com.dragonminez.client;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DMZClient {

    public static void init() {
        LogUtil.info(Env.CLIENT, "Initializing DragonMineZ Client...");
    }
}

