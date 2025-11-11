package com.dragonminez.server.events;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.SyncCreativeFlyingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Evento del servidor que detecta cambios en el estado de vuelo creativo
 * y sincroniza con todos los clientes cercanos
 */
@Mod.EventBusSubscriber
public class PlayerAnimationsSyncHandler {

    private static final Map<UUID, Boolean> lastFlyingState = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer serverPlayer)) return;

        Player player = event.player;
        UUID uuid = player.getUUID();

        boolean isCurrentlyFlying = player.getAbilities().flying && !player.isFallFlying();

        Boolean lastState = lastFlyingState.get(uuid);
        if (lastState == null || lastState != isCurrentlyFlying) {
            // Estado cambió, enviar paquete a todos los jugadores cercanos
            lastFlyingState.put(uuid, isCurrentlyFlying);

            SyncCreativeFlyingPacket packet = new SyncCreativeFlyingPacket(uuid, isCurrentlyFlying);

            // Enviar a todos los jugadores que están rastreando a este jugador
            NetworkHandler.INSTANCE.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverPlayer),
                packet
            );
        }
    }
}

