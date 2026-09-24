package com.dragonminez.common.init.entities.sagas.helper;

import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.ki.KiWaveEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ImpactBurstVfxS2C;
import com.dragonminez.common.network.S2C.ShockwaveVfxS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.events.players.combat.KnockbackHelper;
import com.dragonminez.server.events.players.combat.MomentumImpactHandler;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public class ComboManager {

    private static final int SPIRIT_CANNON_KNEE_TICK = 6;
    private static final int SPIRIT_CANNON_UPPERCUT_TICK = 13;
    private static final int SPIRIT_CANNON_LAUNCH_TICK = 24;
    private static final int SPIRIT_CANNON_ELBOW_TICK = 40;
    private static final int SPIRIT_CANNON_END_TICK = 47;
    private static final int SPIRIT_CANNON_LIFT_TICKS = 12;
    private static final double SPIRIT_CANNON_LIFT_SPEED = 0.45D;
    private static final double SPIRIT_CANNON_CASTER_LIFT_BONUS = 0.15D;
    private static final double SPIRIT_CANNON_SLAM_POWER = 2.8D;
    private static final float SPIRIT_CANNON_FINAL_HIT_RATIO = 0.35F;
    private static final float SPIRIT_CANNON_IMPACT_DAMAGE_RATIO = 0.20F;
    private static final int SPIRIT_CANNON_COLOR_PRIMARY = 0xF3E4FF;
    private static final int SPIRIT_CANNON_COLOR_SECONDARY = 0xA63EF0;
    private static final int SPIRIT_CANNON_BURST_TICKS = 14;
    private static final float SPIRIT_CANNON_HIT_BURST_SCALE = 3.2F;
    private static final float SPIRIT_CANNON_LAUNCH_BURST_SCALE = 4.0F;
    private static final float SPIRIT_CANNON_ELBOW_BURST_SCALE = 5.0F;
    private static final float SPIRIT_CANNON_GROUND_BURST_SCALE = 6.0F;
    private static final int GOD_FIST_DASH_END_TICK = 12;
    private static final int GOD_FIST_IMPACT_TICK = 14;
    private static final int GOD_FIST_END_TICK = 35;
    private static final double GOD_FIST_DASH_SPEED = 1.5D;
    private static final double GOD_FIST_KNOCKBACK = 4.0D;
    private static final float GOD_FIST_IMPACT_DAMAGE_RATIO = 0.20F;
    private static final int GOD_FIST_COLOR_PRIMARY = 0xFFD23A;
    private static final int GOD_FIST_COLOR_SECONDARY = 0xFF4A12;
    private static final int GOD_FIST_SHOCKWAVE_COLOR = 0xF5C527;
    private static final float GOD_FIST_BURST_SCALE = 5.5F;
    private static final int GOD_FIST_BURST_TICKS = 18;

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
            case 9 -> handleSpiritBreakingCannon(user, target, timer);
            case 11 -> handleSuperGodFist(user, target, timer);

        }
    }

    public static void onComboStopped(DBSagasEntity user, int comboId, LivingEntity target) {
        if (user.level().isClientSide) return;
        boolean spiritCannon = comboId == DBSagasEntity.ComboType.SPIRIT_BREAKING_CANNON.getId();
        boolean godFist = comboId == DBSagasEntity.ComboType.SUPER_GOD_FIST.getId();
        if (!spiritCannon && !godFist) return;

        if (spiritCannon && !user.isFlying()) user.setNoGravity(false);
        if (target == null) return;

        if (target instanceof ServerPlayer victim) {
            NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(victim.getUUID(),
                    TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP, 0, -1, ""), victim);
        } else if (target instanceof DBSagasEntity saga) {
            if (godFist) {
                saga.stopTriggeredAnimation(DBSagasEntity.HURT_CONTROLLER, DBSagasEntity.HURT_ANIM_GODFIST);
                return;
            }
            saga.stopTriggeredAnimation(DBSagasEntity.HURT_CONTROLLER, DBSagasEntity.HURT_ANIM_TOP2);
            saga.stopTriggeredAnimation(DBSagasEntity.HURT_CONTROLLER, DBSagasEntity.HURT_ANIM_TOP);
            saga.stopTriggeredAnimation(DBSagasEntity.HURT_CONTROLLER, DBSagasEntity.HURT_ANIM_DOWN);
        }
    }

    private static void handleSuperGodFist(DBSagasEntity user, LivingEntity target, int timer) {
        if (user.level().isClientSide) return;

        user.invulnerableTime = 20;
        if (timer < GOD_FIST_IMPACT_TICK) faceTowards(target, user);

        if (timer <= GOD_FIST_DASH_END_TICK) {
            target.invulnerableTime = 20;
            if (user.distanceTo(target) > 1.5D) {
                Vec3 dir = target.position().subtract(user.position()).normalize();
                Vec3 step = new Vec3(dir.x * GOD_FIST_DASH_SPEED, 0.0D, dir.z * GOD_FIST_DASH_SPEED);
                user.move(MoverType.SELF, step);
                spawnSuperGodFistTrail(user, step);
            }
            freeze(target);
        } else if (timer < GOD_FIST_IMPACT_TICK) {
            target.invulnerableTime = 20;
            freeze(target);
        } else if (timer == GOD_FIST_IMPACT_TICK) {
            float damage = comboHitDamage(user, DBSagasEntity.ComboType.SUPER_GOD_FIST, 1);
            Vec3 impactPos = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());

            float sizeFactor = Math.max(1.0F, target.getBbHeight() / 1.8F);
            Vec3 waveCenter = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ());
            Vec3 toAttacker = new Vec3(user.getX() - target.getX(), 0.0D, user.getZ() - target.getZ());
            if (toAttacker.lengthSqr() > 1.0E-4D) waveCenter = waveCenter.add(toAttacker.normalize().scale(target.getBbWidth() * 0.5D));
            NetworkHandler.sendToTrackingEntity(new ShockwaveVfxS2C(waveCenter.x, waveCenter.y, waveCenter.z,
                    4.5F * sizeFactor, GOD_FIST_SHOCKWAVE_COLOR, 13), user);

            target.invulnerableTime = 0;
            target.hurt(user.damageSources().mobAttack(user), damage);

            user.level().playSound(null, impactPos.x, impactPos.y, impactPos.z, MainSounds.CRITICO2.get(), SoundSource.HOSTILE, 2.5F, 0.7F);
            NetworkHandler.sendToTrackingEntity(new ImpactBurstVfxS2C(impactPos, user.getLookAngle(), GOD_FIST_BURST_SCALE,
                    GOD_FIST_COLOR_PRIMARY, GOD_FIST_COLOR_SECONDARY, false, GOD_FIST_BURST_TICKS), user);

            Vec3 pushDir = user.getLookAngle().normalize();
            KnockbackHelper.apply(target, new Vec3(pushDir.x * GOD_FIST_KNOCKBACK, 0.6D, pushDir.z * GOD_FIST_KNOCKBACK));
            MomentumImpactHandler.CollisionImpactType impactType = target.onGround() || pushDir.y < -0.5D
                    ? MomentumImpactHandler.CollisionImpactType.GROUND
                    : MomentumImpactHandler.CollisionImpactType.WALL;
            MomentumImpactHandler.registerCollisionImpact(target, impactType, damage * GOD_FIST_IMPACT_DAMAGE_RATIO, pushDir);
            playVictimHurtPose(target, "base.hurt_supergodfist", DBSagasEntity.HURT_ANIM_GODFIST);
        }

        user.setDeltaMovement(0.0D, user.getDeltaMovement().y, 0.0D);

        if (timer >= GOD_FIST_END_TICK) user.stopCombo();
    }

    private static void spawnSuperGodFistTrail(DBSagasEntity user, Vec3 motion) {
        if (!(user.level() instanceof ServerLevel level)) return;
        Vec3 center = user.position().add(0.0D, user.getBbHeight() * 0.5D, 0.0D);
        for (int i = 0; i < 4; i++) {
            Vec3 point = center.subtract(motion.scale(i / 4.0D));
            level.sendParticles(MainParticles.SPARKS.get(), point.x, point.y, point.z, 0, 0.96D, 0.77D, 0.15D, 1.0D);
        }
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 2, 0.15D, 0.3D, 0.15D, 0.0D);
    }

    private static void handleSpiritBreakingCannon(DBSagasEntity user, LivingEntity target, int timer) {
        if (user.level().isClientSide) return;

        float total = comboHitDamage(user, DBSagasEntity.ComboType.SPIRIT_BREAKING_CANNON, 1);
        float hitDamage = total * (1.0F - SPIRIT_CANNON_FINAL_HIT_RATIO) / 3.0F;

        user.invulnerableTime = 20;
        user.fallDistance = 0.0F;

        if (timer <= SPIRIT_CANNON_LAUNCH_TICK) faceTowards(target, user);

        if (timer < SPIRIT_CANNON_KNEE_TICK) {
            target.invulnerableTime = 20;
            if (user.distanceTo(target) > 1.6D) {
                Vec3 dir = target.position().subtract(user.position()).normalize();
                user.move(MoverType.SELF, new Vec3(dir.x * 1.4D, 0.0D, dir.z * 1.4D));
            }
            freeze(target);
        } else if (timer == SPIRIT_CANNON_KNEE_TICK) {
            spiritCannonHit(user, target, hitDamage, false, 0.45D, SPIRIT_CANNON_HIT_BURST_SCALE, MainSounds.GOLPE4.get(), 1.0F);
            playVictimHurtPose(target, "base.hurt_top2", DBSagasEntity.HURT_ANIM_TOP2);
            freeze(target);
        } else if (timer == SPIRIT_CANNON_UPPERCUT_TICK) {
            spiritCannonHit(user, target, hitDamage, false, 0.8D, SPIRIT_CANNON_HIT_BURST_SCALE, MainSounds.GOLPE5.get(), 1.15F);
            playVictimHurtPose(target, "base.hurt_top2", DBSagasEntity.HURT_ANIM_TOP2);
            freeze(target);
        } else if (timer == SPIRIT_CANNON_LAUNCH_TICK) {
            spiritCannonHit(user, target, hitDamage, false, 0.55D, SPIRIT_CANNON_LAUNCH_BURST_SCALE, MainSounds.CRITICO2.get(), 1.25F);
            playVictimHurtPose(target, "base.hurt_top", DBSagasEntity.HURT_ANIM_TOP);
            KnockbackHelper.apply(target, new Vec3(0.0D, 1.2D, 0.0D));
            user.setNoGravity(true);
            user.setDeltaMovement(Vec3.ZERO);
        } else if (timer == SPIRIT_CANNON_ELBOW_TICK) {
            spiritCannonHit(user, target, total * SPIRIT_CANNON_FINAL_HIT_RATIO, true, 0.9D, SPIRIT_CANNON_ELBOW_BURST_SCALE, MainSounds.CRITICO1.get(), 0.8F);
            playVictimHurtPose(target, "base.hurt_down", DBSagasEntity.HURT_ANIM_DOWN);
            KnockbackHelper.apply(target, new Vec3(0.0D, -SPIRIT_CANNON_SLAM_POWER, 0.0D));
            MomentumImpactHandler.registerCollisionImpact(target, MomentumImpactHandler.CollisionImpactType.GROUND,
                    total * SPIRIT_CANNON_IMPACT_DAMAGE_RATIO, new Vec3(0.0D, -1.0D, 0.0D),
                    SPIRIT_CANNON_COLOR_PRIMARY, SPIRIT_CANNON_COLOR_SECONDARY, SPIRIT_CANNON_GROUND_BURST_SCALE);
            user.setDeltaMovement(Vec3.ZERO);
        } else if (timer < SPIRIT_CANNON_LAUNCH_TICK) {
            target.invulnerableTime = 20;
            freeze(target);
        } else if (timer < SPIRIT_CANNON_ELBOW_TICK) {
            target.invulnerableTime = 20;
            boolean lifting = timer <= SPIRIT_CANNON_LAUNCH_TICK + SPIRIT_CANNON_LIFT_TICKS;
            KnockbackHelper.apply(target, new Vec3(0.0D, lifting ? SPIRIT_CANNON_LIFT_SPEED : 0.04D, 0.0D));
            target.fallDistance = 0.0F;

            int risen = Math.min(timer - SPIRIT_CANNON_LAUNCH_TICK, SPIRIT_CANNON_LIFT_TICKS);
            user.setPos(user.getX(), target.getY() + risen * SPIRIT_CANNON_CASTER_LIFT_BONUS, user.getZ());
            user.setDeltaMovement(Vec3.ZERO);
        } else {
            user.setDeltaMovement(Vec3.ZERO);
        }

        if (timer >= SPIRIT_CANNON_END_TICK) user.stopCombo();
    }

    private static void spiritCannonHit(DBSagasEntity user, LivingEntity target, float damage, boolean finalHit,
                                        double heightRatio, float burstScale, SoundEvent sound, float pitch) {
        float applied = damage;
        if (!finalHit && target.getHealth() - applied <= 1.0F) applied = Math.max(0.01F, target.getHealth() - 1.0F);

        float sizeFactor = Math.max(1.0F, target.getBbHeight() / 1.8F);
        Vec3 waveCenter = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ());
        Vec3 toAttacker = new Vec3(user.getX() - target.getX(), 0.0D, user.getZ() - target.getZ());
        if (toAttacker.lengthSqr() > 1.0E-4D) waveCenter = waveCenter.add(toAttacker.normalize().scale(target.getBbWidth() * 0.5D));
        NetworkHandler.sendToTrackingEntity(new ShockwaveVfxS2C(waveCenter.x, waveCenter.y, waveCenter.z,
                (finalHit ? 4.5F : 1.8F) * sizeFactor, SPIRIT_CANNON_COLOR_SECONDARY, finalHit ? 13 : 7), user);

        target.invulnerableTime = 0;
        target.hurt(user.damageSources().mobAttack(user), applied);

        Vec3 impactPos = new Vec3(target.getX(), target.getY() + target.getBbHeight() * heightRatio, target.getZ());
        user.level().playSound(null, impactPos.x, impactPos.y, impactPos.z, sound, SoundSource.HOSTILE, 2.0F, pitch);
        NetworkHandler.sendToTrackingEntity(new ImpactBurstVfxS2C(impactPos, user.getLookAngle(), burstScale,
                SPIRIT_CANNON_COLOR_PRIMARY, SPIRIT_CANNON_COLOR_SECONDARY, false, SPIRIT_CANNON_BURST_TICKS), user);
    }

    private static void playVictimHurtPose(LivingEntity victim, String playerAnim, String sagaAnim) {
        if (victim == null || victim.isDeadOrDying()) return;
        if (victim instanceof ServerPlayer victimPlayer) {
            NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(victimPlayer.getUUID(),
                    TriggerAnimationS2C.AnimationType.KI_ANIMATION, 0, -1, playerAnim), victimPlayer);
        } else if (victim instanceof DBSagasEntity saga) {
            saga.triggerAnim(DBSagasEntity.HURT_CONTROLLER, sagaAnim);
        }
    }

    private static void faceTowards(LivingEntity source, LivingEntity target) {
        source.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
        source.setYHeadRot(source.getYRot());
    }

    private static void freeze(LivingEntity entity) {
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
    }

    /**
     * Per-hit melee damage so that a combo's hits sum to {@code attackDamage * tier.multiplier}.
     * Hybrid combos (Meteor) call this only for their melee portion; the ki portion is computed
     * separately against ki blast damage.
     */
    private static float comboHitDamage(DBSagasEntity user, DBSagasEntity.ComboType combo, int totalHits) {
        float melee = (float) user.getAttributeValue(Attributes.ATTACK_DAMAGE);
        return melee * combo.getTier().getDamageMultiplier() / totalHits;
    }

    private static void handleBasicCombo(DBSagasEntity user, LivingEntity target, int timer) {
        float perHit = comboHitDamage(user, DBSagasEntity.ComboType.BASIC, 3);
        if (timer == 1) {
            teleportAndHit(user, target, target.getLookAngle().normalize(), 1.5, perHit, MainSounds.CRITICO1.get(), 1.4F);
        }
        if (timer == 12) {
            teleportAndHit(user, target, target.getLookAngle().normalize().scale(-1), 1.5, perHit, MainSounds.CRITICO1.get(), 1.5F);
        }
        if (timer == 22) {
            Vec3 targetLook = target.getLookAngle().normalize();
            user.teleportTo(target.getX() + (targetLook.x * 1.5), target.getY() + 0.5, target.getZ() + (targetLook.z * 1.5));
            user.playSound(MainSounds.TP.get(), 1.0F, 1.1F);
            user.setDeltaMovement(0, 0, 0);
        }
        if (timer == 31) {
            finalBlow(user, target, perHit, 2.5);
        }
    }

    private static void handleAirCombo(DBSagasEntity user, LivingEntity target, int timer) {
        float perHit = comboHitDamage(user, DBSagasEntity.ComboType.AIR, 2);
        if (timer == 1) {
            teleportAndHit(user, target, target.getLookAngle().normalize(), 1.5, perHit, MainSounds.CRITICO1.get(), 1.2F);
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
            target.hurt(user.damageSources().mobAttack(user), perHit);
            user.playSound(MainSounds.CRITICO1.get(), 1.0F, 0.8F);
            user.spawnPunchParticles(target);
            target.setDeltaMovement(0, -2.5D, 0);
            target.hasImpulse = true;
        }
        if (timer == 25) {
            user.stopCombo();
        }
    }

    private static void handleChargeAttack(DBSagasEntity user, LivingEntity target, int timer) {
        float perHit = comboHitDamage(user, DBSagasEntity.ComboType.KI_CHARGE_ATTACK, 2);
        if (timer == 1) {
            user.setKiCharge(true);
            user.playSound(MainSounds.KI_CHARGE_LOOP.get(), 1.0F, 1.5F);
            user.setDeltaMovement(target.position().subtract(user.position()).normalize().scale(1.5));
        }
        if (timer == 6) {
            user.spawnPunchParticles(target);
            target.hurt(user.damageSources().mobAttack(user), perHit);
            user.setDeltaMovement(0, 0, 0);
            target.setDeltaMovement(0, 0, 0);
        }
        if (timer == 11) {
            Vec3 targetLook = target.getLookAngle().normalize();
            user.teleportTo(target.getX() - (targetLook.x * 1.5), target.getY(), target.getZ() - (targetLook.z * 1.5));
            user.playSound(MainSounds.TP.get(), 1.0F, 1.3F);
        }
        if (timer == 17) {
            finalBlow(user, target, perHit, 3.0);
            user.setKiCharge(false);
        }
    }

    private static void handleMeteorCombination(DBSagasEntity user, LivingEntity target, int timer) {
        // Hybrid: 6 melee hits sum to melee * tier, then a final Kamehameha for ki * tier (double instance).
        float perHit = comboHitDamage(user, DBSagasEntity.ComboType.METEOR_COMBINATION, 6);
        if (timer == 1) {
            meleeHit(user, target, perHit, 0.3, 2.0);
        }
        if (timer == 10) {
            Vec3 look = target.getLookAngle().normalize();
            user.teleportTo(target.getX() + (look.x * 1.5), target.getY(), target.getZ() + (look.z * 1.5));
            user.playSound(MainSounds.TP.get(), 1.0F, 1.3F);
        }
        if (timer == 15 || timer == 20 || timer == 25 || timer == 30) {
            meleeHit(user, target, perHit, 0.0, 0.0);
        }
        if (timer == 35) {
            meleeHit(user, target, perHit, 0.0, 0.0);
        }
        if (timer == 45) {
            user.teleportTo(target.getX(), target.getY() + 4.0D, target.getZ());
            user.playSound(MainSounds.TP.get(), 1.0F, 1.0F);
            user.setDeltaMovement(0, 0, 0);

            float damage = user.getKiBlastDamage() * DBSagasEntity.ComboType.METEOR_COMBINATION.getTier().getDamageMultiplier();
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
            user.getCombatBrain().onAbsorb(drain);
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

    private static void meleeHit(DBSagasEntity user, LivingEntity target, float damage, double pushY, double pushStrength) {
        user.lookAt(target, 360, 360);
        target.invulnerableTime = 0;
        target.hurt(user.damageSources().mobAttack(user), damage);
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
            float damage = comboHitDamage(user, DBSagasEntity.ComboType.GUM_PUNCH, 1);

            target.invulnerableTime = 0;
            target.hurt(user.damageSources().mobAttack(user), damage);
            user.playSound(MainSounds.CRITICO1.get(), 1.2F, 0.7F);
            user.spawnPunchParticles(target);


            Vec3 pushDir = target.position().subtract(user.position()).normalize();
            target.setDeltaMovement(pushDir.x * 5.0, 0.5, pushDir.z * 5.0);
            target.hasImpulse = true;
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
            float damage = comboHitDamage(user, DBSagasEntity.ComboType.GUM_EXPAND, 1);

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
                    // 9 potential hits across ticks 2..18 sum to melee * tier.
                    float damage = comboHitDamage(user, DBSagasEntity.ComboType.RAPID_KICKS, 9);

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