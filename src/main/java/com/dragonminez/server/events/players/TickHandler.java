package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TickHandler {

    private static final int REGEN_INTERVAL = 20;
    private static final int SYNC_INTERVAL = 10;

    private static final double ENERGY_REGEN_RATE = 0.5;
    private static final double STAMINA_REGEN_RATE = 1.0;

    private static final Map<UUID, Integer> playerTickCounters = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }

        if (!(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        UUID playerId = serverPlayer.getUUID();
        int tickCounter = playerTickCounters.getOrDefault(playerId, 0) + 1;

        StatsProvider.get(StatsCapability.INSTANCE, serverPlayer).ifPresent(data -> {
            if (!data.getStatus().hasCreatedCharacter() || !data.getStatus().isAlive()) {
                return;
            }

            boolean shouldRegen = tickCounter >= REGEN_INTERVAL;
            boolean shouldSync = tickCounter % SYNC_INTERVAL == 0;

            if (shouldRegen) {
                int currentEnergy = data.getResources().getCurrentEnergy();
                int maxEnergy = data.getMaxEnergy();
                if (currentEnergy < maxEnergy) {
                    double newEnergyDouble = Math.min(maxEnergy, currentEnergy + ENERGY_REGEN_RATE);
                    int newEnergy = (int) Math.ceil(newEnergyDouble);
                    data.getResources().setCurrentEnergy(newEnergy);
                }

                int currentStamina = data.getResources().getCurrentStamina();
                int maxStamina = data.getMaxStamina();
                if (currentStamina < maxStamina) {
                    double newStaminaDouble = Math.min(maxStamina, currentStamina + STAMINA_REGEN_RATE);
                    int newStamina = (int) Math.ceil(newStaminaDouble);
                    data.getResources().setCurrentStamina(newStamina);
                }

                playerTickCounters.put(playerId, 0);
            } else {
                playerTickCounters.put(playerId, tickCounter);
            }

			if (data.getStatus().isChargingKi() && !data.getStatus().isDescending() && tickCounter % 20 == 0) {
				int currentRelease = data.getResources().getPowerRelease();
				if (currentRelease < 100) {
					int newRelease = Math.min(100, currentRelease + 5);
					data.getResources().setPowerRelease(newRelease);
				}
			} else if (data.getStatus().isDescending() && data.getStatus().isChargingKi() && tickCounter % 20 == 0) {
				int currentRelease = data.getResources().getPowerRelease();
				if (currentRelease > 0) {
					int newRelease = Math.max(0, currentRelease - 5);
					data.getResources().setPowerRelease(newRelease);
				}
			}

            if (shouldSync) {
                NetworkHandler.sendToPlayer(new StatsSyncS2C(serverPlayer), serverPlayer);
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        playerTickCounters.remove(event.getEntity().getUUID());
    }
}

