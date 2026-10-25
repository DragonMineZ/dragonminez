package com.dragonminez.common.init.entities.ai;

import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class IncomingThreat {

    public enum Kind { BEAM, PROJECTILE, EXPLOSION, PENDING }

    public final AbstractKiProjectile projectile;
    public final LivingEntity attacker;
    public final Kind kind;
    public final int eta;
    public final float damage;
    public final double originDistance;
    public final Vec3 approachDir;
    public final boolean sidestepWorks;
    public final float charge;

    IncomingThreat(AbstractKiProjectile projectile, LivingEntity attacker, Kind kind, int eta, float damage,
                   double originDistance, Vec3 approachDir, boolean sidestepWorks, float charge) {
        this.projectile = projectile;
        this.attacker = attacker;
        this.kind = kind;
        this.eta = eta;
        this.damage = damage;
        this.originDistance = originDistance;
        this.approachDir = approachDir;
        this.sidestepWorks = sidestepWorks;
        this.charge = charge;
    }

    public UUID id() {
        return this.projectile.getUUID();
    }

    public boolean isPending() {
        return this.kind == Kind.PENDING;
    }

    public boolean imminentWithin(int ticks) {
        return this.eta >= 0 && this.eta <= ticks;
    }

    public float damageFraction(float maxHealth) {
        return maxHealth <= 0.0F ? 1.0F : this.damage / maxHealth;
    }

    public double urgency(float maxHealth) {
        double frac = this.damageFraction(maxHealth);
        int t = this.eta < 0 ? 60 : this.eta;
        return frac * 10.0D / (1.0D + t / 10.0D);
    }

    public boolean stillValid() {
        return this.projectile.isAlive() && !this.projectile.isRemoved();
    }
}
