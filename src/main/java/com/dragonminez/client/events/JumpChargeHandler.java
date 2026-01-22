package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.Stats;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class JumpChargeHandler {

    private static int airTicks = 0;
    private static boolean wasJumping = false;
    private static boolean hasAppliedBaseBoost = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        final int[] jumpLevel = {0};
		final boolean[] isStunned = {false};
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (data.getStatus().hasCreatedCharacter()) {
                jumpLevel[0] = data.getSkills().getSkillLevel("jump");
				isStunned[0] = data.getStatus().isStunned();
            }
        });

        if (jumpLevel[0] <= 0 || isStunned[0]) {
            airTicks = 0;
            wasJumping = false;
            hasAppliedBaseBoost = false;
            return;
        }

        boolean isSpacePressed = mc.options.keyJump.isDown();
        boolean isOnGround = player.onGround();
        boolean isJumping = !isOnGround && player.getDeltaMovement().y > 0;

        if (isJumping && !wasJumping && !hasAppliedBaseBoost) {

            float targetBlocks = 1.0f + (jumpLevel[0] * 0.1f);
            float blocksToAdd = targetBlocks - 1.25f;
            float baseBoost = blocksToAdd * 0.18f;

            player.setDeltaMovement(player.getDeltaMovement().add(0, baseBoost, 0));
            hasAppliedBaseBoost = true;
        }

        if (isJumping && isSpacePressed) {
            airTicks++;

            int maxAirTicks = jumpLevel[0];
            if (airTicks <= maxAirTicks) {
                float incrementalBoost = 0.11f;

                player.setDeltaMovement(player.getDeltaMovement().add(0, incrementalBoost, 0));
            }
        }

        if (isOnGround) {
            if (airTicks > 0) airTicks = 0;
            hasAppliedBaseBoost = false;
        }

        wasJumping = isJumping;
    }
}

