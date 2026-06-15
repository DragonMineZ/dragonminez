package com.dragonminez.common.init.entities.sagas.ai;

import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity.KiSkill;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity.SkillRole;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueDispatcher;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class CombatContext {

    public static final double APPROACH_THRESHOLD = 0.05D;

    public final DBSagasEntity self;
    public final LivingEntity target;

    public double dist3D;
    public double horizontalDist;
    public double verticalDiff;
    public boolean hasLineOfSight;
    public double closingSpeed;

    public float selfHpPct;
    public float targetHpPct;
    public boolean canFly;

    public boolean zanzokenReady;
    public boolean wildSenseReady;
    public boolean comboReady;
    public boolean dashReady;
    public List<KiSkill> readySkills = new ArrayList<>();

    public boolean targetBlocking;
    public boolean targetStunned;
    public boolean targetKnockedDown;
    public boolean targetTransforming;
    public boolean targetCasting;
    public float targetChargePercent;
    public boolean targetCommittedCast;
    public boolean targetFiring;

    private CombatContext(DBSagasEntity self, LivingEntity target) {
        this.self = self;
        this.target = target;
    }

    public static CombatContext snapshot(DBSagasEntity self, LivingEntity target) {
        CombatContext c = new CombatContext(self, target);

        c.dist3D = self.distanceTo(target);
        double dx = target.getX() - self.getX();
        double dz = target.getZ() - self.getZ();
        c.horizontalDist = Math.sqrt(dx * dx + dz * dz);
        c.verticalDiff = target.getY() - self.getY();
        c.hasLineOfSight = self.getSensing().hasLineOfSight(target);

        Vec3 toSelf = self.position().subtract(target.position());
        Vec3 toSelfNorm = toSelf.lengthSqr() > 1.0e-6 ? toSelf.normalize() : Vec3.ZERO;
        c.closingSpeed = target.getDeltaMovement().dot(toSelfNorm);

        c.selfHpPct = self.getMaxHealth() > 0 ? self.getHealth() / self.getMaxHealth() : 0.0F;
        c.targetHpPct = target.getMaxHealth() > 0 ? target.getHealth() / target.getMaxHealth() : 0.0F;
        c.canFly = self.canFly();

        c.zanzokenReady = self.isZanzokenReady();
        c.wildSenseReady = self.isWildSenseReady();
        c.comboReady = self.isComboReady();
        c.dashReady = self.isDashReady();

        for (KiSkill skill : self.getSkillPool()) {
            if (skill.currentCooldown <= 0) c.readySkills.add(skill);
        }

        if (target instanceof ServerPlayer sp) {
            StatsData data = StatsProvider.get(StatsCapability.INSTANCE, sp).resolve().orElse(null);
            if (data != null) {
                c.targetBlocking = data.getStatus().isBlocking();
                c.targetStunned = data.getStatus().isStunned();
                c.targetKnockedDown = data.getStatus().isKnockedDown();
                c.targetTransforming = data.getStatus().isActionCharging();
                c.targetCasting = data.getTechniques().isTechniqueCharging();
                c.targetChargePercent = data.getTechniques().getTechniqueChargePercent();

                KiAttackData.KiType chargingType = TechniqueDispatcher.getChargingKiType(data);
                c.targetCommittedCast = chargingType != null && TechniqueDispatcher.restrictsMovementWhileCharging(chargingType);
                c.targetFiring = TechniqueDispatcher.isFiringKiAttack(sp);
            }
        }

        return c;
    }

    public boolean targetApproaching() {
        return this.closingSpeed > APPROACH_THRESHOLD;
    }

    public boolean targetRetreating() {
        return this.closingSpeed < -APPROACH_THRESHOLD;
    }

    public boolean targetHelpless() {
        return this.targetStunned || this.targetKnockedDown;
    }

    public List<KiSkill> readyByRole(SkillRole... roles) {
        List<KiSkill> out = new ArrayList<>();
        for (KiSkill skill : this.readySkills) {
            for (SkillRole role : roles) {
                if (skill.role == role) {
                    out.add(skill);
                    break;
                }
            }
        }
        return out;
    }

    public boolean hasReadyRole(SkillRole... roles) {
        return !readyByRole(roles).isEmpty();
    }
}
