package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.PlayerAnimationsSync;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = Reference.MOD_ID)
public class PlayerAnimationsSyncHandler {

    private static final Map<UUID, Boolean> lastFlyingState = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        if (!event.getEntity().level().isClientSide()) return;

        Player player = event.getEntity();
        UUID uuid = player.getUUID();

        boolean isCurrentlyFlying = player.getAbilities().flying || player.isFallFlying();

        Boolean lastState = lastFlyingState.get(uuid);
        if (lastState == null || lastState != isCurrentlyFlying) {
            lastFlyingState.put(uuid, isCurrentlyFlying);
            NetworkHandler.sendToTrackingEntityAndSelf(new PlayerAnimationsSync(uuid, isCurrentlyFlying), player);
        }
    }
}

