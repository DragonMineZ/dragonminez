package com.dragonminez.server.events.players.statuseffect;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.IStatusEffectHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;

public class FlyStatusHandler implements IStatusEffectHandler {
    @Override
    public void handleStatusEffects(ServerPlayer player, StatsData data) {
        if (data.getSkills().isSkillActive("fly")) {
            if (!player.hasEffect(MainEffects.FLY.get())) {
                player.addEffect(new MobEffectInstance(MainEffects.FLY.get(), -1, 0, false, false, true));
            }
        } else {
            player.removeEffect(MainEffects.FLY.get());
        }
    }

    @Override
    public void onPlayerTick(ServerPlayer serverPlayer, StatsData data) {
        if (data.getSkills().isSkillActive("fly") && !serverPlayer.isCreative() && !serverPlayer.isSpectator()) {
            if (serverPlayer.horizontalCollision) {
                double dx = serverPlayer.getX() - serverPlayer.xOld;
                double dz = serverPlayer.getZ() - serverPlayer.zOld;
                double speed = Math.sqrt(dx * dx + dz * dz);
                double minImpactSpeed = 0.35D;

                if (speed > minImpactSpeed) {
                    float maxHealth = serverPlayer.getMaxHealth();
                    double maxImpactSpeedRef = 1.5D;
                    double factor = (speed - minImpactSpeed) / (maxImpactSpeedRef - minImpactSpeed);
                    factor = Mth.clamp(factor, 0.0, 1.0);
                    float finalPct = (float) Mth.lerp(factor, 0.05f, 0.35f);
                    float damage = maxHealth * finalPct;
                    serverPlayer.hurt(serverPlayer.damageSources().flyIntoWall(), damage);
                    serverPlayer.level().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0F, (float) (0.5F + (factor * 0.5F)));
                }
            }
        }
    }

    @Override
    public void onPlayerSecond(ServerPlayer serverPlayer, StatsData data) {
        handleFlightKiDrain(serverPlayer, data);
    }

    private static void handleFlightKiDrain(ServerPlayer player, StatsData data) {
        if (!data.getSkills().isSkillActive("fly")) return;
        if (player.isCreative() || player.isSpectator()) return;

        int flyLevel = data.getSkills().getSkillLevel("fly");
        if (flyLevel >= data.getSkills().getMaxSkillLevel("fly")) return;
        int maxEnergy = data.getMaxEnergy();

        double basePercent = player.isSprinting() ? 0.08 : 0.03;
        double energyCostPercent = Math.max(0.002, basePercent - (flyLevel * 0.005));
        int energyCost = (int) Math.ceil(maxEnergy * energyCostPercent);

        int currentEnergy = data.getResources().getCurrentEnergy();

        if (currentEnergy >= energyCost) {
            data.getResources().removeEnergy(energyCost);
        } else {
            data.getSkills().setSkillActive("fly", false);
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        }
    }
}
