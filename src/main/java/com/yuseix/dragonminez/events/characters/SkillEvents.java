package com.yuseix.dragonminez.events.characters;

import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.stats.DMZStatsProvider;
import com.yuseix.dragonminez.stats.skills.DMZSkill;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DragonMineZ.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SkillEvents {

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) { //Metodo que sirve solo para la habilidad jump
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
            DMZSkill jumpSkill = cap.getDMZSkills().get("jump");

            if (jumpSkill != null && jumpSkill.isActive()) {
                int jumpLevel = jumpSkill.getLevel(); // Nivel actual de la habilidad
                if (jumpLevel > 0) {
                    float jumpBoost = 0.1f * jumpLevel; // Cada nivel aumenta el salto en 0.5 bloques
                    player.setDeltaMovement(player.getDeltaMovement().add(0, jumpBoost, 0));
                }
            }

            if (cap.getBoolean("turbo")) {
                player.setDeltaMovement(player.getDeltaMovement().add(0, 0.6, 0));
            }
        });
    }
}
