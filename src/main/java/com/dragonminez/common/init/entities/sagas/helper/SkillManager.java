package com.dragonminez.common.init.entities.sagas.helper;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.ki.*;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
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
        });

        // 2. GALICK GUN
        REGISTRY.put(2, (user, target, dmg) -> {
            KiWaveEntity galick = new KiWaveEntity(user.level(), user);
            galick.setupKiGalickGun(user, dmg, user.getKiBlastSpeed(), user.getCurrentPoolSkillSize(), 37);
        });

        // 3. MAKANKOSAPPO
        REGISTRY.put(3, (user, target, dmg) -> {
            KiLaserEntity makkanko = new KiLaserEntity(user.level(), user);
            makkanko.setupKiMakkankosanpo(user, dmg, user.getKiBlastSpeed() * 2.0F, 37);
        });

        // 4. KI LASER
        REGISTRY.put(4, (user, target, dmg) -> {
            KiLaserEntity laser = new KiLaserEntity(user.level(), user);
            laser.setupKiLaser(user, dmg, user.getKiBlastSpeed() * 3.0F, user.getCurrentPoolColorMain(), user.getCurrentPoolColorBorder(), 0);
        });

        // 5. KI EXPLOSION
        REGISTRY.put(5, (user, target, dmg) -> {
            KiExplosionEntity explosion = new KiExplosionEntity(user.level(), user);
            explosion.setupKiExplosion(user, dmg, user.getCurrentPoolColorMain(), user.getCurrentPoolColorBorder(), 37);
        });

        // 6. KI BARRIER
        REGISTRY.put(6, (user, target, dmg) -> {
            KiBarrierEntity barrier = new KiBarrierEntity(user.level(), user);
            barrier.setupKiBarrier(user, user.getCurrentPoolColorMain(), user.getCurrentPoolColorBorder(), 37);
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
        });

        // 10. KI VOLLEY
        REGISTRY.put(10, (user, target, dmg) -> {
            KiBlastEntity volley = new KiBlastEntity(user.level(), user);
            volley.setupKiVolley(user, dmg, user.getKiBlastSpeed(), user.getCurrentPoolColorMain(), 37);
        });

        // 11. KI SMALL
        REGISTRY.put(11, (user, target, dmg) -> {
            KiBlastEntity small = new KiBlastEntity(user.level(), user);
            small.setupKiSmall(user, dmg, user.getKiBlastSpeed(), user.getCurrentPoolColorMain());
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
        });

        // 14. KIENZAN
        REGISTRY.put(14, (user, target, dmg) -> {
            KiDiskEntity disk = new KiDiskEntity(user.level(), user);
            disk.setupKiDisk(user, dmg, user.getKiBlastSpeed() * 1.2F, user.getCurrentPoolColorMain(), user.getCurrentPoolSkillSize(), 30);
        });

        // 15. DEATH BALL
        REGISTRY.put(15, (user, target, dmg) -> {
            KiBlastEntity ball = new KiBlastEntity(user.level(), user);
            ball.setupKiDeathBall(user, dmg, user.getKiBlastSpeed() * 0.7F, user.getCurrentPoolColorMain(), user.getCurrentPoolColorBorder(), 60);
        });

        // 16. MASENKO
        REGISTRY.put(16, (user, target, dmg) -> {
            KiWaveEntity masenko = new KiWaveEntity(user.level(), user);
            masenko.setupKiMasenko(user, dmg, user.getKiBlastSpeed(), user.getCurrentPoolSkillSize(), 40);
        });

        // 17. BIG BANG
        REGISTRY.put(17, (user, target, dmg) -> {
            KiBlastEntity bigbang = new KiBlastEntity(user.level(), user);
            bigbang.setupKiBlast(user, dmg, user.getKiBlastSpeed(), user.getCurrentPoolColorMain(), user.getCurrentPoolSkillSize(), 30);
        });

        // 18. FINAL FLASH
        REGISTRY.put(18, (user, target, dmg) -> {
            KiWaveEntity ff = new KiWaveEntity(user.level(), user);
            ff.setupFinalFlash(user, dmg, user.getKiBlastSpeed(), user.getCurrentPoolSkillSize(), 40);
        });

        // 19. MAJIN CANDY
        REGISTRY.put(19, (user, target, dmg) -> {
            SPMajinCandyEntity candy = new SPMajinCandyEntity(user.level(), user);
            candy.setupCandyBeam(user, dmg, user.getKiBlastSpeed(), 35);
        });

        REGISTRY.put(20, (user, target, dmg) -> {
            KiBlastEntity airVolley = new KiBlastEntity(user.level(), user);
            airVolley.setupKiAirVolley(user, dmg, user.getKiBlastSpeed(), user.getCurrentPoolColorMain(), user.getCurrentPoolColorOutline(), 30);
        });
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
            case 7, 12, 19 -> meleeDmg * mult;          // Oozaru Roar / Blue Hurricane / Majin Candy: melee-scaled
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
            case 13, 16, 18 -> 40;
            case 19 -> 35;
            case 15 -> 60;
            default -> 60;
        };
    }
}