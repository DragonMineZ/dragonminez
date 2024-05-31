package com.yuseix.dragonminez.listener;

import com.mojang.logging.LogUtils;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;

public final class ForgeListener {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final List<String> ALLOWED_USERNAMES = Arrays.asList(
            "Dev",
            "MrBrunoh",
            "Yuseix",
            "ezShokkoh");

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {

        String username = event.getEntity().getGameProfile().getName();

        if (!ALLOWED_USERNAMES.contains(username)) {
            LOGGER.error("The user {} is not allowed to play the mod. The game session will now be terminated.", username);
            throw new IllegalStateException("DMZ: Username not allowed to start gameplay!");
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Hacer algo cuando el servidor empiece???
        LOGGER.info("HOLA SOY DRAGON BLOCK TEST UWU");
    }
}