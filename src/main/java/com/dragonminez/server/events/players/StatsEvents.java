package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.client.events.ForgeClientEvents;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StatsEvents {

    public static final UUID DMZ_HEALTH_MODIFIER_UUID = UUID.fromString("b065b873-f4c8-4a0f-aa8c-6e778cd410e0");

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

			if (ForgeClientEvents.hasCreatedCharacterCache != data.getStatus().hasCreatedCharacter()) {
				ForgeClientEvents.hasCreatedCharacterCache = data.getStatus().hasCreatedCharacter();
			}

            AttributeInstance dmzHealthAttr = player.getAttribute(MainAttributes.DMZ_HEALTH.get());
            AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);

            if (dmzHealthAttr != null && maxHealthAttr != null) {
				float dmzHealthBonus = data.getMaxHealth();

                AttributeModifier existingModifier = maxHealthAttr.getModifier(DMZ_HEALTH_MODIFIER_UUID);

                if (existingModifier == null || existingModifier.getAmount() != dmzHealthBonus) {
                    maxHealthAttr.removeModifier(DMZ_HEALTH_MODIFIER_UUID);

                    if (dmzHealthBonus > 0) {
                        AttributeModifier healthModifier = new AttributeModifier(
                            DMZ_HEALTH_MODIFIER_UUID,
                            "DMZ Health Bonus",
                            dmzHealthBonus,
                            AttributeModifier.Operation.ADDITION
                        );
                        maxHealthAttr.addPermanentModifier(healthModifier);
                    }

                    if (player.getHealth() > maxHealthAttr.getValue()) {
                        player.setHealth((float) maxHealthAttr.getValue());
                    }
                }

                if (!data.hasInitializedHealth()) {
                    player.setHealth((float) maxHealthAttr.getValue());
                    data.setInitializedHealth(true);
                }
            }
        });
    }
}

