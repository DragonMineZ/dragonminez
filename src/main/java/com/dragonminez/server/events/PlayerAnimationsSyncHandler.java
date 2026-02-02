package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.PlayerAnimationsSync;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class PlayerAnimationsSyncHandler {

    private static final Map<UUID, Boolean> lastFlyingState = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer)) return;

        Player player = event.player;
        UUID uuid = player.getUUID();

        boolean isCurrentlyFlying = player.getAbilities().flying || player.isFallFlying();

        Boolean lastState = lastFlyingState.get(uuid);
        if (lastState == null || lastState != isCurrentlyFlying) {
            lastFlyingState.put(uuid, isCurrentlyFlying);
            NetworkHandler.sendToTrackingEntityAndSelf(new PlayerAnimationsSync(uuid, isCurrentlyFlying), player);
        }
    }
}

