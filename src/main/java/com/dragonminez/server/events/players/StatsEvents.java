package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StatsEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }

        Player player = event.player;

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (!data.getStatus().hasCreatedCharacter()) {
                return;
            }

            AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealthAttr != null) {
                int calculatedMaxHealth = data.getMaxHealth();
                double currentMaxHealth = maxHealthAttr.getBaseValue();

                if (currentMaxHealth != calculatedMaxHealth) {
                    maxHealthAttr.setBaseValue(calculatedMaxHealth);

                    if (player.getHealth() > calculatedMaxHealth) {
                        player.setHealth(calculatedMaxHealth);
                    }
                }

                if (!data.hasInitializedHealth()) {
                    player.setHealth(calculatedMaxHealth);
                    data.setInitializedHealth(true);
                }
            }
        });
    }
}

