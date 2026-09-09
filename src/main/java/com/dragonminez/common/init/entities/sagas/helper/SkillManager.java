package com.dragonminez.common.init.entities.sagas.helper;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.ki.*;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public class SkillManager {

    @FunctionalInterface
    public interface KiAction {
        void execute(DBSagasEntity user, LivingEntity target, float damage);
    }

    private static final Map<Integer, KiAction> REGISTRY = new HashMap<>();

    static {
        // 1. KAMEHAMEHA
        REGISTRY.put(1, (user, target, dmg) -> {
            KiWaveEntity kame = new KiWaveEntity(user.level(), user);
            kame.setupKiHame(user, dmg, user.getKiBlastSpeed(), user.getCurrentPoolSkillSize(), 37);
            applyColors(user, kame);
        });

        // 2. GALICK GUN
        REGISTRY.put(2, (user, target, dmg) -> {
            KiWaveEntity galick = new KiWaveEntity(user.level(), user);
            galick.setupKiGalickGun(user, dmg, user.getKiBlastSpeed(), user.getCurrentPoolSkillSize(), 37);
            applyColors(user, galick);
        });

        // 3. MAKANKOSAPPO
        REGISTRY.put(3, (user, target, dmg) -> {
            KiLaserEntity makkanko = new KiLaserEntity(user.level(), user);
            makkanko.setupKiMakkankosanpo(user, dmg, user.getKiBlastSpeed() * 2.0F, 37);
            applyColors(user, makkanko);
        });

        // 4. KI LASER
        REGISTRY.put(4, (user, target, dmg) -> {
            KiLaserEntity laser = new KiLaserEntity(user.level(), user);
            laser.setupKiLaser(user, dmg, user.getKiBlastSpeed() * 3.0F, user.getCurrentPoolColorMain(), user.getCurrentPoolColorBorder(), 0);
            applyColors(user, laser);
        });

        // 5. KI EXPLOSION
        REGISTRY.put(5, (user, target, dmg) -> {
            KiExplosionEntity explosion = new KiExplosionEntity(user.level(), user);
            explosion.setupKiExplosion(user, dmg, user.getCurrentPoolColorMain(), user.getCurrentPoolColorBorder(), 37);
            applyColors(user, explosion);
        });

        // 6. KI BARRIER
        REGISTRY.put(6, (user, target, dmg) -> {
            KiBarrierEntity barrier = new KiBarrierEntity(user.level(), user);
            barrier.setupKiBarrier(user, user.getCurrentPoolColorMain(), user.getCurrentPoolColorBorder(), 37);
            applyColors(user, barrier);
            barrier.setKiDamage(dmg);
        });

        // 7. OOZARU ROAR
        REGISTRY.put(7, (user, target, dmg) -> {
            user.playSound(MainSounds.OOZARU_GROWL_PLAYER.get(), 2.0F, 0.8F + user.getRandom().nextFloat() * 0.4F);
            if (!user.level().isClientSide && user.level() instanceof ServerLevel serverLevel) {
                double range = 8.0D;
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, user.getX(), user.getY() + (user.getBbHeight() / 2.0), user.getZ(), 100, range / 1.5, range / 1.5, range / 1.5, 0.2D);
                AABB roarBox = user.getBoundingBox().inflate(range);
                for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, roarBox)) {
                    if (entity != user && entity.isAlive()) {
                        entity.invulnerableTime = 0;
                        entity.hurt(user.damageSources().mobAttack(user), dmg);
                        entity.addEffect(new MobEffectInstance(MainEffects.STUN.get(), 40, 0, false, false, true));
                        Vec3 push = new Vec3(entity.getX() - user.getX(), 0.5D, entity.getZ() - user.getZ()).normalize().scale(3.5D);
                        entity.setDeltaMovement(push);
                        entity.hasImpulse = true;
                    }
                }
            }
        });

        // 8. GENERIC KI WAVE
        REGISTRY.put(8, (user, target, dmg) -> {
            KiWaveEntity wave = new KiWaveEntity(user.level(), user);
            wave.setupKiWave(user, dmg, user.getKiBlastSpeed(), user.getCurrentPoolColorMain(), user.getCurrentPoolColorBorder(), user.getCurrentPoolColorOutline(), user.getCurrentPoolSkillSize(), 37);
        });

        // 9. OOZARU BEAM
        REGISTRY.put(9, (user, target, dmg) -> {
            KiWaveEntity oozaru = new KiWaveEntity(user.level(), user);
            oozaru.setupKiOozaru(user, dmg, user.getKiBlastSpeed(), user.getCurrentPoolColorMain(), user.getCurrentPoolColorBorder(), user.getCurrentPoolSkillSize(), 37);
            applyColors(user, oozaru);
        });

        // 10. KI VOLLEY
        REGISTRY.put(10, (user, target, dmg) -> {
            KiBlastEntity volley = new KiBlastEntity(user.level(), user);
            volley.setupKiVolley(user, dmg, user.getKiBlastSpeed(), user.getCurrentPoolColorMain(), 37);
            applyColors(user, volley);
        });

        // 11. KI SMALL
        REGISTRY.put(11, (user, target, dmg) -> {
            KiBlastEntity small = new KiBlastEntity(user.level(), user);
            small.setupKiSmall(user, dmg, user.getKiBlastSpeed(), user.getCurrentPoolColorMain());
            applyColors(user, small);
            small.shootFromRotation(user, user.getXRot(), user.getYRot(), 0.0F, user.getKiBlastSpeed(), 1.0F);
            user.playSound(MainSounds.KIBLAST_ATTACK.get(), 1.0F, 1.0F + (user.getRandom().nextFloat() * 0.2F));
        });

        // 12. ENE HURRICANE
        REGISTRY.put(12, (user, target, dmg) -> {
            SPBlueHurricaneEntity hurricane = new SPBlueHurricaneEntity(user.level(), user);
            hurricane.setupHurricane(user, dmg, user.getKiBlastSpeed(), 30);
        });

        // 13. TRIPLE LASER
        REGISTRY.put(13, (user, target, dmg) -> {
            KiLaserEntity triple = new KiLaserEntity(user.level(), user);
            triple.setupKiLaser(user, dmg, user.getKiBlastSpeed() * 3.0F, user.getCurrentPoolColorMain(), user.getCurrentPoolColorBorder(), 0);
            applyColors(user, triple);
        });

        // 14. KIENZAN
        REGISTRY.put(14, (user, target, dmg) -> {
            KiDiskEntity disk = new KiDiskEntity(user.level(), user);
            disk.setupKiDisk(user, dmg, user.getKiBlastSpeed() * 1.2F, user.getCurrentPoolColorMain(), user.getCurrentPoolSkillSize(), 30);
            applyColors(user, disk);
        });

        // 15. DEATH BALL
        REGISTRY.put(15, (user, target, dmg) -> {
            KiBlastEntity ball = new KiBlastEntity(user.level(), user);
            ball.setupKiDeathBall(user, dmg, user.getKiBlastSpeed() * 0.7F, user.getCurrentPoolColorMain(), user.getCurrentPoolColorBorder(), 60);
            applyColors(user, ball);
        });

        // 16. MASENKO
        REGISTRY.put(16, (user, target, dmg) -> {
            KiWaveEntity masenko = new KiWaveEntity(user.level(), user);
            masenko.setupKiMasenko(user, dmg, user.getKiBlastSpeed(), user.getCurrentPoolSkillSize(), 40);
            applyColors(user, masenko);
        });

        // 17. BIG BANG
        REGISTRY.put(17, (user, target, dmg) -> {
            KiBlastEntity bigbang = new KiBlastEntity(user.level(), user);
            bigbang.setupKiBlast(user, dmg, user.getKiBlastSpeed(), user.getCurrentPoolColorMain(), user.getCurrentPoolSkillSize(), 30);
            applyColors(user, bigbang);
        });

        // 18. FINAL FLASH
        REGISTRY.put(18, (user, target, dmg) -> {
            KiWaveEntity ff = new KiWaveEntity(user.level(), user);
            ff.setupFinalFlash(user, dmg, user.getKiBlastSpeed(), user.getCurrentPoolSkillSize(), 40);
            applyColors(user, ff);
        });

        // 19. MAJIN CANDY
        REGISTRY.put(19, (user, target, dmg) -> {
            SPMajinCandyEntity candy = new SPMajinCandyEntity(user.level(), user);
            candy.setupCandyBeam(user, dmg, user.getKiBlastSpeed(), 35);
        });

        REGISTRY.put(20, (user, target, dmg) -> {
            KiBlastEntity airVolley = new KiBlastEntity(user.level(), user);
            airVolley.setupKiAirVolley(user, dmg, user.getKiBlastSpeed(), user.getCurrentPoolColorMain(), user.getCurrentPoolColorOutline(), 30);
            applyColors(user, airVolley);
        });

        // 21. DOUBLE SUNDAY (Raditz)
        REGISTRY.put(21, (user, target, dmg) -> {
            KiWaveEntity doubleSunday = new KiWaveEntity(user.level(), user);
            doubleSunday.setupDoubleSunday(user, dmg, user.getKiBlastSpeed(), user.getCurrentPoolColorMain(), user.getCurrentPoolColorBorder(), user.getCurrentPoolColorOutline(), user.getCurrentPoolSkillSize(), 40);
            applyColors(user, doubleSunday);
        });
    }

    private static void applyColors(DBSagasEntity user, AbstractKiProjectile projectile) {
        projectile.setColors(user.getCurrentPoolColorMain(), user.getCurrentPoolColorBorder(),
                user.getCurrentPoolColorOutline());
    }

    public static void execute(int id, DBSagasEntity user, LivingEntity target) {
        KiAction action = REGISTRY.get(id);
        if (action != null) {
            float damage = getCalculatedDamage(id, user);
            action.execute(user, target, damage);
        }
    }

    private static final float VOLLEY_HIT_DIVISOR = 8.0F;

    private static final float SINGLE_IMPACT_HIT_DIVISOR = 4.0F;

    public static float getCalculatedDamage(int id, DBSagasEntity user) {
        float kiDmg = user.getKiBlastDamage();
        float meleeDmg = (float) user.getAttributeValue(Attributes.ATTACK_DAMAGE);

        DBSagasEntity.KiSkillType type = DBSagasEntity.KiSkillType.fromId(id);
        float mult = type != null ? type.getTier().getDamageMultiplier() : DBSagasEntity.Tier.MEDIUM.getDamageMultiplier();

        return switch (id) {
            case 6 -> 0.0F;                             // Ki Barrier: defensive, no damage
            case 7, 12, 19, 22 -> meleeDmg * mult;      // Oozaru Roar / Blue Hurricane / Majin Candy / Wolf Fang: melee-scaled
            case 13 -> kiDmg * mult / 3.0F;             // Triple Laser: 3 instances (ticks 10/20/30)
            case 10, 20 -> kiDmg * mult / VOLLEY_HIT_DIVISOR; // Ki Volley / Air Volley: random spray, per-bullet
            case 11 -> kiDmg * mult / SINGLE_IMPACT_HIT_DIVISOR; // Basic ki blast: single concentrated impact
            default -> kiDmg * mult;                    // every other ki skill: single ki-scaled hit
        };
    }

    public static int getCastDuration(int id) {
        return switch (id) {
            case 4 -> 10;
            case 11 -> 12;
            case 12, 14, 17 -> 30;
            case 13, 16, 18, 21 -> 40;
            case 19 -> 35;
            case 22 -> WOLF_FANG_DURATION;
            case 15 -> 60;
            default -> 60;
        };
    }

    /* ---------------------------------------------------------------------
     * WOLF FANG (id 22)
     *
     * Melee flurry instead of a one-shot projectile, so it is driven per cast
     * tick from DBSagasEntity#tick rather than through the REGISTRY above.
     * It only borrows the presentation of the player-side technique in
     * StrikeAttackHandler (jab rhythm, particles, sounds, closing blow): the
     * user never grabs the target, so the victim keeps full control and is
     * only hit while it stays inside the caster's reach.
     * ------------------------------------------------------------------ */

    public static final int WOLF_FANG_DURATION = 35;
    private static final int WOLF_FANG_HIT_INTERVAL = 10;
    private static final float WOLF_FANG_FINAL_HIT_RATIO = 0.35F;
    private static final int WOLF_FANG_JAB_INTERVAL = 4;
    private static final double WOLF_FANG_REACH = 4.0D;
    private static final double WOLF_FANG_KNOCKBACK_FORCE = 1.8D;

    public static void tickWolfFang(DBSagasEntity user, LivingEntity target, int timer) {
        if (user.level().isClientSide) return;

        float total = getCalculatedDamage(DBSagasEntity.KiSkillType.WOLF_FANG.getId(), user);
        int hitCount = Math.max(1, (int) Math.ceil(WOLF_FANG_DURATION / (double) WOLF_FANG_HIT_INTERVAL));
        float perHit = total * (1.0F - WOLF_FANG_FINAL_HIT_RATIO) / hitCount;
        float finalHit = total * WOLF_FANG_FINAL_HIT_RATIO;

        Vec3 impact = wolfFangImpactPos(user, target);

        if (timer < WOLF_FANG_DURATION && timer % WOLF_FANG_HIT_INTERVAL == 0) {
            wolfFangHit(user, target, perHit, false);
        }

        if (timer < WOLF_FANG_DURATION - 3 && timer % WOLF_FANG_JAB_INTERVAL == 0) {
            wolfFangJab(user, impact, timer);
        }

        if (timer >= WOLF_FANG_DURATION) {
            wolfFangFinish(user, target, impact, finalHit);
        }
    }

    /** Whether the flurry can actually connect; nothing holds the target in place, so it may walk out. */
    private static boolean wolfFangInReach(DBSagasEntity user, LivingEntity target) {
        return target != null && target.isAlive() && user.distanceTo(target) <= WOLF_FANG_REACH;
    }

    /** Where the punch effects play: on the target while it is in reach, on the caster's fists otherwise. */
    private static Vec3 wolfFangImpactPos(DBSagasEntity user, LivingEntity target) {
        if (wolfFangInReach(user, target)) {
            return new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ());
        }

        Vec3 front = user.position().add(user.getLookAngle().normalize().scale(1.2));
        return new Vec3(front.x, user.getY() + user.getBbHeight() * 0.6, front.z);
    }

    private static void wolfFangHit(DBSagasEntity user, LivingEntity target, float damage, boolean finalHit) {
        if (damage <= 0.0F || !wolfFangInReach(user, target)) return;

        // Chip hits never finish the target; the closing blow is what kills.
        float applied = damage;
        if (!finalHit && target.getHealth() - applied <= 1.0F) applied = Math.max(0.01F, target.getHealth() - 1.0F);

        target.invulnerableTime = 0;
        target.hurt(user.damageSources().mobAttack(user), applied);
    }

    private static void wolfFangJab(DBSagasEntity user, Vec3 impact, int beat) {
        if (!(user.level() instanceof ServerLevel level)) return;

        SoundEvent[] punches = {
                MainSounds.GOLPE1.get(), MainSounds.GOLPE2.get(), MainSounds.GOLPE3.get(),
                MainSounds.GOLPE4.get(), MainSounds.GOLPE5.get(), MainSounds.GOLPE6.get()
        };
        SoundEvent punch = punches[Math.floorMod(beat / WOLF_FANG_JAB_INTERVAL, punches.length)];
        level.playSound(null, impact.x, impact.y, impact.z, punch, net.minecraft.sounds.SoundSource.HOSTILE,
                1.0F, 1.1F + (level.random.nextFloat() * 0.3F));

        level.sendParticles(MainParticles.PUNCH_PARTICLE.get(), impact.x, impact.y, impact.z, 0, 0.30, 0.62, 1.0, 1.0);

        for (int i = 0; i < 4; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 0.7;
            double oy = (level.random.nextDouble() - 0.5) * 0.7;
            double oz = (level.random.nextDouble() - 0.5) * 0.7;
            level.sendParticles(MainParticles.SPARKS.get(), impact.x + ox, impact.y + oy, impact.z + oz, 0, 0.25, 0.55, 1.0, 1.0);
        }

        level.sendParticles(ParticleTypes.CRIT, impact.x, impact.y, impact.z, 6, 0.3, 0.3, 0.3, 0.5);
    }

    private static void wolfFangFinish(DBSagasEntity user, LivingEntity target, Vec3 impact, float damage) {
        wolfFangHit(user, target, damage, true);

        user.level().playSound(null, impact.x, impact.y, impact.z, MainSounds.CRITICO2.get(), net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.7F);
        user.level().playSound(null, impact.x, impact.y, impact.z, MainSounds.KI_EXPLOSION_IMPACT.get(), net.minecraft.sounds.SoundSource.HOSTILE, 2.5F, 1.0F);
        user.level().playSound(null, impact.x, impact.y, impact.z, MainSounds.OOZARU_GROWL_PLAYER.get(), net.minecraft.sounds.SoundSource.HOSTILE, 3.0F, 1.15F);

        if (user.level() instanceof ServerLevel level) {
            level.sendParticles(MainParticles.PUNCH_PARTICLE.get(), impact.x, impact.y, impact.z, 0, 0.30, 0.62, 1.0, 1.0);

            for (int i = 0; i < 90; i++) {
                double dirX = level.random.nextDouble() - 0.5;
                double dirY = level.random.nextDouble() - 0.5;
                double dirZ = level.random.nextDouble() - 0.5;
                double len = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
                if (len < 1.0E-4) continue;
                double radius = 0.5 + level.random.nextDouble() * 2.5;
                level.sendParticles(MainParticles.SPARKS.get(),
                        impact.x + (dirX / len) * radius, impact.y + (dirY / len) * radius, impact.z + (dirZ / len) * radius,
                        0, 0.25, 0.55, 1.0, 1.0);
            }
        }

        if (!wolfFangInReach(user, target)) return;

        Vec3 push = target.position().subtract(user.position()).normalize();
        if (push.lengthSqr() < 1.0E-6) push = user.getLookAngle();
        target.setDeltaMovement(push.x * WOLF_FANG_KNOCKBACK_FORCE, 0.5D, push.z * WOLF_FANG_KNOCKBACK_FORCE);
        target.hasImpulse = true;
        target.hurtMarked = true;
    }
}
