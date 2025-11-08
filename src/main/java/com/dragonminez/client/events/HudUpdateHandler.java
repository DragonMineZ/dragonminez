package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HudUpdateHandler {

    // Esto es puro para testear, una vez tengamos interfaz bien se quita esto y ya xd

    private static int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 10;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        tickCounter++;
        if (tickCounter >= UPDATE_INTERVAL) {
            tickCounter = 0;
            updateActionBar(mc);
        }
    }

    private static void updateActionBar(Minecraft mc) {
        StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
            if (data.getStatus().hasCreatedCharacter()) {
                int currentStamina = data.getResources().getCurrentStamina();
                int maxStamina = data.getMaxStamina();
                int currentEnergy = data.getResources().getCurrentEnergy();
                int maxEnergy = data.getMaxEnergy();

                String text = "§2STM: §f" + currentStamina + " §7/ §f" + maxStamina +
                             "     §bKI: §f" + currentEnergy + " §7/ §f" + maxEnergy;

                mc.player.displayClientMessage(Component.literal(text), true);
            }
        });
    }
}

