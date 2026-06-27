package com.dragonminez.client;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.client.gui.config.DMZModConfigScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

@OnlyIn(Dist.CLIENT)
public class DMZClient {

    public static void init() {
        LogUtil.info(Env.CLIENT, "Initializing DragonMineZ Client...");

        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> new DMZModConfigScreen(parent)));
    }
}
