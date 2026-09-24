package com.dragonminez.common.init.entities.ai;

import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiAreaEntity;
import com.dragonminez.common.init.entities.ki.KiBarrierEntity;
import com.dragonminez.common.init.entities.ki.KiExplosionEntity;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import com.dragonminez.common.init.entities.ki.KiWaveEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class ThreatScanner {

    private static final double BEAM_MARGIN = 0.75D;
    private static final double PROJECTILE_MARGIN = 1.0D;
    private static final double PENDING_MARGIN = 1.5D;
    private static final double MIN_MOVING_SPEED = 0.05D;

    private ThreatScanner() {}

    public static List<IncomingThreat> scan(Mob self, double radius, Function<UUID, Float> chargeLookup) {
        return scan(self, radius, chargeLookup, null);
    }

    public static List<IncomingThreat> scan(Mob self, double radius, Function<UUID, Float> chargeLookup,
                                            BiConsumer<AbstractKiProjectile, LivingEntity> hostileObserver) {
        List<IncomingThreat> out = new ArrayList<>();
        List<AbstractKiProjectile> projectiles = self.level().getEntitiesOfClass(AbstractKiProjectile.class,
                self.getBoundingBox().inflate(radius));
        if (projectiles.isEmpty()) return out;

        Vec3 center = self.position().add(0.0D, self.getBbHeight() * 0.5D, 0.0D);
        double selfRadius = Math.max(self.getBbWidth(), self.getBbHeight()) * 0.5D;
        float maxHealth = self.getMaxHealth();

        for (AbstractKiProjectile p : projectiles) {
            if (!p.isAlive() || p.isHeal() || p instanceof KiBarrierEntity) continue;
            Entity ownerEntity = p.getOwner();
            if (ownerEntity == self) continue;
            LivingEntity owner = ownerEntity instanceof LivingEntity living ? living : null;
            if (owner != null && self.isAlliedTo(owner)) continue;
            if (owner == null && p.getOwnerUUID() != null && p.getOwnerUUID().equals(self.getUUID())) continue;
            if (hostileObserver != null && owner != null) hostileObserver.accept(p, owner);

            IncomingThreat threat;
            if (p instanceof KiWaveEntity || p instanceof KiLaserEntity) {
                threat = evaluateBeam(self, p, owner, center, selfRadius, chargeLookup);
            } else if (p instanceof KiExplosionEntity explosion) {
                threat = evaluateExplosion(self, explosion, owner, chargeLookup);
            } else if (p instanceof KiAreaEntity area) {
                threat = evaluateArea(self, area, owner, chargeLookup);
            } else {
                threat = evaluateProjectile(self, p, owner, center, selfRadius, chargeLookup);
            }
            if (threat != null) out.add(threat);
        }

        out.sort(Comparator.comparingDouble((IncomingThreat t) -> -t.urgency(maxHealth)));
        return out;
    }

    private static float chargeOf(LivingEntity owner, Function<UUID, Float> lookup) {
        if (owner == null || lookup == null) return 1.0F;
        Float value = lookup.apply(owner.getUUID());
        return value != null ? value : 1.0F;
    }

    private static double originDistance(Mob self, AbstractKiProjectile p, LivingEntity owner) {
        return owner != null ? self.distanceTo(owner) : self.distanceTo(p);
    }

    private static IncomingThreat evaluateBeam(Mob self, AbstractKiProjectile p, LivingEntity owner, Vec3 center,
                                               double selfRadius, Function<UUID, Float> chargeLookup) {
        boolean wave = p instanceof KiWaveEntity;
        Vec3 start = p.position();
        Vec3 dir;
        float length;
        boolean firing = p.isFiring();
        boolean steerable = false;
        if (wave) {
            KiWaveEntity w = (KiWaveEntity) p;
            dir = Vec3.directionFromRotation(p.getXRot(), p.getYRot());
            length = w.getBeamLength();
            steerable = w.isContinuousFollow() || w.getSteerRate() > 0.0F;
        } else {
            KiLaserEntity l = (KiLaserEntity) p;
            dir = Vec3.directionFromRotation(l.getFixedPitch(), l.getFixedYaw());
            length = l.getBeamLength();
        }

        if (!firing) {
            if (owner == null) return null;
            return evaluatePending(self, p, owner, center, selfRadius, chargeLookup);
        }

        if (dir.lengthSqr() < 1.0E-6D) return null;
        dir = dir.normalize();
        Vec3 rel = center.subtract(start);
        double t = rel.dot(dir);
        if (t < 0.0D) return null;
        double perp = rel.subtract(dir.scale(t)).length();
        double hitRadius = Math.max(0.5D, p.getSize() * 1.5D) + selfRadius + BEAM_MARGIN;
        if (perp > hitRadius) return null;

        double speed = Math.max(0.5D, p.getKiSpeed());
        int eta = length >= t ? 0 : (int) Math.ceil((t - length) / speed);
        Vec3 approach = new Vec3(dir.x, 0.0D, dir.z);
        boolean sidestep = !steerable && eta >= 2;
        return new IncomingThreat(p, owner, IncomingThreat.Kind.BEAM, eta, p.getKiDamage(),
                originDistance(self, p, owner), approach, sidestep, chargeOf(owner, chargeLookup));
    }

    private static IncomingThreat evaluatePending(Mob self, AbstractKiProjectile p, LivingEntity owner, Vec3 center,
                                                  double selfRadius, Function<UUID, Float> chargeLookup) {
        Vec3 start = owner.getEyePosition();
        Vec3 dir = owner.getLookAngle();
        if (dir.lengthSqr() < 1.0E-6D) return null;
        dir = dir.normalize();
        Vec3 rel = center.subtract(start);
        double t = rel.dot(dir);
        if (t < 0.0D) return null;
        double perp = rel.subtract(dir.scale(t)).length();
        double hitRadius = Math.max(1.0D, p.getSize() * 1.5D) + selfRadius + PENDING_MARGIN;
        if (perp > hitRadius) return null;
        Vec3 approach = new Vec3(dir.x, 0.0D, dir.z);
        return new IncomingThreat(p, owner, IncomingThreat.Kind.PENDING, -1, p.getKiDamage(),
                originDistance(self, p, owner), approach, true, chargeOf(owner, chargeLookup));
    }

    private static IncomingThreat evaluateProjectile(Mob self, AbstractKiProjectile p, LivingEntity owner, Vec3 center,
                                                     double selfRadius, Function<UUID, Float> chargeLookup) {
        Vec3 v = p.getDeltaMovement();
        double speed = v.length();
        if (speed < MIN_MOVING_SPEED) {
            if (owner == null) return null;
            return evaluatePending(self, p, owner, center, selfRadius, chargeLookup);
        }
        Vec3 rel = center.subtract(p.position());
        double dist = rel.length();
        if (dist < 1.0E-3D) {
            return new IncomingThreat(p, owner, IncomingThreat.Kind.PROJECTILE, 0, p.getKiDamage(),
                    originDistance(self, p, owner), new Vec3(v.x, 0.0D, v.z), false, chargeOf(owner, chargeLookup));
        }
        Vec3 vn = v.scale(1.0D / speed);
        double t = rel.dot(vn) / speed;
        if (t < 0.0D) return null;
        double closest = rel.subtract(vn.scale(rel.dot(vn))).length();
        double hitRadius = Math.max(0.4D, p.getSize()) + selfRadius + PROJECTILE_MARGIN;
        double heading = vn.dot(rel.scale(1.0D / dist));
        if (closest > hitRadius && heading < 0.85D) return null;
        int eta = (int) Math.ceil(t);
        Vec3 approach = new Vec3(vn.x, 0.0D, vn.z);
        return new IncomingThreat(p, owner, IncomingThreat.Kind.PROJECTILE, eta, p.getKiDamage(),
                originDistance(self, p, owner), approach, eta >= 2, chargeOf(owner, chargeLookup));
    }

    private static IncomingThreat evaluateExplosion(Mob self, KiExplosionEntity explosion, LivingEntity owner,
                                                    Function<UUID, Float> chargeLookup) {
        double radius = Math.max(2.0D, explosion.getMaxRadius()) + 1.0D;
        double dist = self.distanceTo(explosion);
        if (dist > radius) return null;
        int eta = Math.max(0, explosion.getCastExplosion() - explosion.tickCount);
        Vec3 away = self.position().subtract(explosion.position());
        Vec3 approach = new Vec3(-away.x, 0.0D, -away.z);
        return new IncomingThreat(explosion, owner, IncomingThreat.Kind.EXPLOSION, eta, explosion.getKiDamage(),
                originDistance(self, explosion, owner), approach, false, chargeOf(owner, chargeLookup));
    }

    private static IncomingThreat evaluateArea(Mob self, KiAreaEntity area, LivingEntity owner,
                                               Function<UUID, Float> chargeLookup) {
        double radius = Math.max(2.0D, area.getAreaRadius()) + 1.0D;
        double dist = self.distanceTo(area);
        if (dist > radius) return null;
        int eta = Math.max(0, area.getCastTime() - area.tickCount);
        Vec3 away = self.position().subtract(area.position());
        Vec3 approach = new Vec3(-away.x, 0.0D, -away.z);
        return new IncomingThreat(area, owner, IncomingThreat.Kind.EXPLOSION, eta, area.getKiDamage(),
                originDistance(self, area, owner), approach, false, chargeOf(owner, chargeLookup));
    }
}
