package com.dragonminez.client;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.client.gui.config.DMZModConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.fml.ModLoadingContext;

@OnlyIn(Dist.CLIENT)
public class DMZClient {

    public static void init() {
        LogUtil.info(Env.CLIENT, "Initializing DragonMineZ Client...");

        ModLoadingContext.get().registerExtensionPoint(
                IConfigScreenFactory.class,
                () -> (IConfigScreenFactory) (container, parent) -> new DMZModConfigScreen(parent));
    }
}
