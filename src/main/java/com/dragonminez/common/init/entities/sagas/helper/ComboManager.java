package com.dragonminez.common.init.entities.sagas.helper;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.ki.KiWaveEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public class ComboManager {

    public static void handleCombo(DBSagasEntity user, LivingEntity target, int comboId, int timer) {
        if (target == null || !target.isAlive() || !user.isAlive() || user.isTransforming()) {
            user.stopCombo();
            return;
        }

        user.getNavigation().stop();
        user.lookAt(target, 360, 360);

        switch (comboId) {
            case 0 -> handleBasicCombo(user, target, timer);
            case 1 -> handleAirCombo(user, target, timer);
            case 2 -> handleChargeAttack(user, target, timer);
            case 3 -> handleMeteorCombination(user, target, timer);
            case 4 -> handleAndroidAbsorption(user, target, timer);
            case 5 -> handleGumPunch(user, target, timer);
            case 6 -> handleGumExpand(user, target, timer);
            case 7 -> handleSleepRecovery(user, timer);
            case 8 -> handleRapidKicks(user, target, timer);

        }
    }

    private static void handleBasicCombo(DBSagasEntity user, LivingEntity target, int timer) {
        if (timer == 1) {
            teleportAndHit(user, target, target.getLookAngle().normalize(), 1.5, (float) user.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.4F, MainSounds.CRITICO1.get(), 1.4F);
        }
        if (timer == 12) {
            teleportAndHit(user, target, target.getLookAngle().normalize().scale(-1), 1.5, (float) user.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.4F, MainSounds.CRITICO1.get(), 1.5F);
        }
        if (timer == 22) {
            Vec3 targetLook = target.getLookAngle().normalize();
            user.teleportTo(target.getX() + (targetLook.x * 1.5), target.getY() + 0.5, target.getZ() + (targetLook.z * 1.5));
            user.playSound(MainSounds.TP.get(), 1.0F, 1.1F);
            user.setDeltaMovement(0, 0, 0);
        }
        if (timer == 31) {
            finalBlow(user, target, (float) user.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.4F, 2.5);
        }
    }

    private static void handleAirCombo(DBSagasEntity user, LivingEntity target, int timer) {
        if (timer == 1) {
            teleportAndHit(user, target, target.getLookAngle().normalize(), 1.5, (float) user.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.6F, MainSounds.CRITICO1.get(), 1.2F);
            target.setDeltaMovement(0, 1.2D, 0);
            target.hasImpulse = true;
        }
        if (timer == 12) {
            Vec3 targetLook = target.getLookAngle().normalize();
            user.moveTo(target.getX() + (targetLook.x * 0.5), target.getY() + 2.5D, target.getZ() + (targetLook.z * 0.5));
            user.playSound(MainSounds.TP.get(), 1.0F, 1.3F);
            user.setDeltaMovement(0, 0, 0);
        }
        if (timer == 20) {
            target.invulnerableTime = 0;
            target.hurt(user.damageSources().mobAttack(user), (float) user.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.6F);
            user.playSound(MainSounds.CRITICO1.get(), 1.0F, 0.8F);
            user.spawnPunchParticles(target);
            target.setDeltaMovement(0, -2.5D, 0);
            target.hasImpulse = true;
        }
        if (timer == 25) {
            target.addEffect(new MobEffectInstance(MainEffects.STUN.get(), 40, 0, false, false, true));
            user.stopCombo();
        }
    }

    private static void handleChargeAttack(DBSagasEntity user, LivingEntity target, int timer) {
        if (timer == 1) {
            user.setKiCharge(true);
            user.playSound(MainSounds.KI_CHARGE_LOOP.get(), 1.0F, 1.5F);
            user.setDeltaMovement(target.position().subtract(user.position()).normalize().scale(1.5));
        }
        if (timer == 6) {
            user.spawnPunchParticles(target);
            target.hurt(user.damageSources().mobAttack(user), (float) user.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.1F);
            user.setDeltaMovement(0, 0, 0);
            target.setDeltaMovement(0, 0, 0);
        }
        if (timer == 11) {
            Vec3 targetLook = target.getLookAngle().normalize();
            user.teleportTo(target.getX() - (targetLook.x * 1.5), target.getY(), target.getZ() - (targetLook.z * 1.5));
            user.playSound(MainSounds.TP.get(), 1.0F, 1.3F);
        }
        if (timer == 17) {
            finalBlow(user, target, (float) user.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.1F, 3.0);
            user.setKiCharge(false);
        }
    }

    private static void handleMeteorCombination(DBSagasEntity user, LivingEntity target, int timer) {
        if (timer == 1) {
            meleeHit(user, target, 0.3, 2.0);
        }
        if (timer == 10) {
            Vec3 look = target.getLookAngle().normalize();
            user.teleportTo(target.getX() + (look.x * 1.5), target.getY(), target.getZ() + (look.z * 1.5));
            user.playSound(MainSounds.TP.get(), 1.0F, 1.3F);
        }
        if (timer == 15 || timer == 20 || timer == 25 || timer == 30) {
            meleeHit(user, target, 0.0, 0.0);
        }
        if (timer == 35) {
            meleeHit(user, target, 0.0, 0.0);
            target.addEffect(new MobEffectInstance(MainEffects.STUN.get(), 60, 0, false, false, true));
        }
        if (timer == 45) {
            user.teleportTo(target.getX(), target.getY() + 4.0D, target.getZ());
            user.playSound(MainSounds.TP.get(), 1.0F, 1.0F);
            user.setDeltaMovement(0, 0, 0);

            float damage = (float) user.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.0F;
            KiWaveEntity kame = new KiWaveEntity(user.level(), user);
            kame.setupKiHame(user, damage, user.getKiBlastSpeed(), 1.5F, 30);
        }
        if (timer >= 76) user.stopCombo();
    }

    private static void handleAndroidAbsorption(DBSagasEntity user, LivingEntity target, int timer) {
        int duration = 30;
        if (timer == 1) {
            Vec3 look = target.getLookAngle().normalize();
            user.teleportTo(target.getX() + (look.x * 0.8), target.getY(), target.getZ() + (look.z * 0.8));
            user.playSound(MainSounds.TP.get(), 1.0F, 1.0F);
        }
        if (timer > 0 && timer < duration && timer % 10 == 0) {
            float drain = target.getMaxHealth() * 0.05F;
            target.hurt(user.damageSources().mobAttack(user), drain);
            user.heal(drain);
            if (target instanceof ServerPlayer sp) {
                StatsProvider.get(StatsCapability.INSTANCE, sp).ifPresent(data -> {
                    double currentEnergy = data.getResources().getCurrentEnergy();
                    data.getResources().setCurrentEnergy((float) Math.max(0, currentEnergy - (data.getMaxEnergy() * 0.05)));
                });
            }
            user.playSound(MainSounds.ABSORB1.get(), 0.5F, 0.8F);
            user.spawnPunchParticles(target);
        }
        if (timer < duration) {
            user.setDeltaMovement(0, 0, 0);
            Vec3 grabPos = user.position().add(user.getLookAngle().scale(0.6));
            target.setPos(grabPos.x, grabPos.y, grabPos.z);
            target.setDeltaMovement(0, 0, 0);
        } else {
            target.setDeltaMovement(1.5, 0.5, 1.5);
            target.hasImpulse = true;
            user.playSound(MainSounds.CRITICO2.get(), 0.8F, 0.5F);
            user.stopCombo();
        }
    }

    private static void teleportAndHit(DBSagasEntity user, LivingEntity target, Vec3 offsetDir, double distance, float damage, net.minecraft.sounds.SoundEvent sound, float pitch) {
        user.teleportTo(target.getX() + (offsetDir.x * distance), target.getY(), target.getZ() + (offsetDir.z * distance));
        user.playSound(MainSounds.TP.get(), 1.0F, pitch);
        target.invulnerableTime = 0;
        target.hurt(user.damageSources().mobAttack(user), damage);
        user.playSound(sound, 0.8F, pitch);
        user.spawnPunchParticles(target);
    }

    private static void meleeHit(DBSagasEntity user, LivingEntity target, double pushY, double pushStrength) {
        user.lookAt(target, 360, 360);
        user.doHurtTarget(target);
        user.swing(InteractionHand.MAIN_HAND);
        user.playSound(MainSounds.CRITICO1.get(), 0.8F, 1.2F);
        if (pushStrength > 0) {
            Vec3 push = target.position().subtract(user.position()).normalize();
            target.setDeltaMovement(push.x * pushStrength, pushY, push.z * pushStrength);
            target.hasImpulse = true;
        }
    }

    private static void finalBlow(DBSagasEntity user, LivingEntity target, float damage, double pushPower) {
        target.invulnerableTime = 0;
        target.hurt(user.damageSources().mobAttack(user), damage);
        user.playSound(MainSounds.CRITICO1.get(), 1.0F, 0.8F);
        user.spawnPunchParticles(target);
        Vec3 push = target.position().subtract(user.position()).normalize();
        target.setDeltaMovement(push.x * pushPower, 0.5, push.z * pushPower);
        target.hasImpulse = true;
        user.stopCombo();
    }

    private static void handleGumPunch(DBSagasEntity user, LivingEntity target, int timer) {
        if (timer == 1) {
            Vec3 look = target.getLookAngle().normalize();
            user.teleportTo(target.getX() + (look.x * 1.2), target.getY(), target.getZ() + (look.z * 1.2));
            user.playSound(MainSounds.TP.get(), 1.0F, 1.0F);
            user.setDeltaMovement(0, 0, 0);
        }

        if (timer == 7) {
            float damage = (float) user.getAttributeValue(Attributes.ATTACK_DAMAGE) * 3.0F;

            target.invulnerableTime = 0;
            target.hurt(user.damageSources().mobAttack(user), damage);
            user.playSound(MainSounds.CRITICO1.get(), 1.2F, 0.7F);
            user.spawnPunchParticles(target);


            Vec3 pushDir = target.position().subtract(user.position()).normalize();
            target.setDeltaMovement(pushDir.x * 5.0, 0.5, pushDir.z * 5.0);
            target.hasImpulse = true;
        }

        if (timer == 13) {
            target.addEffect(new MobEffectInstance(MainEffects.STUN.get(), 60, 0, false, true, true));
        }

        if (timer >= 20) {
            user.stopCombo();
        }
    }

    private static void handleGumExpand(DBSagasEntity user, LivingEntity target, int timer) {
        if (timer == 1) {
            Vec3 look = target.getLookAngle().normalize();
            user.teleportTo(target.getX() + (look.x * 1.0), target.getY(), target.getZ() + (look.z * 1.0));
            user.playSound(MainSounds.TP.get(), 1.0F, 1.0F);
            user.setDeltaMovement(0, 0, 0);
        }

        if (timer == 5) {
            float damage = (float) user.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.2F;

            target.invulnerableTime = 0;
            target.hurt(user.damageSources().mobAttack(user), damage);
            user.playSound(MainSounds.CRITICO1.get(), 1.0F, 1.2F);
            user.spawnPunchParticles(target);

            Vec3 pushDir = target.position().subtract(user.position()).normalize();
            target.setDeltaMovement(pushDir.x * 2.5, 0.3, pushDir.z * 2.5);
            target.hasImpulse = true;
        }

        if (timer >= 10) {
            user.stopCombo();
        }
    }

    private static void handleSleepRecovery(DBSagasEntity user, int timer) {
        if (timer > 0 && timer < 100 && timer % 10 == 0) {
            float healAmount = user.getMaxHealth() * 0.05F;
            user.heal(healAmount);

            if (user.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        user.getX(), user.getY() + 2, user.getZ(),
                        5, 0.5, 0.5, 0.5, 0.1);
            }
        }

        if (timer >= 100) {
            user.stopCombo();
        }
    }

    private static void handleRapidKicks(DBSagasEntity user, LivingEntity target, int timer) {
        int duration = 20;

        if (timer == 1) {
            if (target != null) {
                Vec3 look = target.getLookAngle().normalize();
                user.teleportTo(target.getX() + (look.x * 1.0), target.getY(), target.getZ() + (look.z * 1.0));
                user.playSound(MainSounds.TP.get(), 1.0F, 1.0F);
            }

            user.playSound(MainSounds.KI_CHARGE_LOOP.get(), 0.5F, 2.0F);
            user.setDeltaMovement(0, 0, 0);
        }

        if (timer > 1 && timer < duration && target != null) {
            user.lookAt(target, 360, 360);

            if (user.distanceTo(target) <= 3.0D) {
                if (timer % 2 == 0) {
                    float damage = (float) user.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.4F;

                    target.invulnerableTime = 0;
                    target.hurt(user.damageSources().mobAttack(user), damage);

                    user.playSound(MainSounds.CRITICO1.get(), 0.4F, 1.6F);
                    user.spawnPunchParticles(target);

                    Vec3 push = target.position().subtract(user.position()).normalize().scale(0.05);
                    target.setDeltaMovement(push.x, 0.05, push.z);
                }
            }
        }

        if (timer >= duration) {
            user.stopCombo();
        }
    }

}