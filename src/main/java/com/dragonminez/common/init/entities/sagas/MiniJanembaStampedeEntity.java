package com.dragonminez.common.init.entities.sagas;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class MiniJanembaStampedeEntity extends DBSagasEntity {

    private static final int SCATTER_TICKS = 100;
    private static final int MAX_LIFETIME_TICKS = 20 * 40;
    private static final double SCATTER_SPEED = 1.8D;
    private static final double SCATTER_DISTANCE = 16.0D;
    private static final double SCATTER_ALERT_RADIUS = 32.0D;
    private static final double HIT_KNOCKBACK = 0.9D;

    private int scatterTicks = -1;
    private Vec3 scatterDirection = Vec3.ZERO;

    public MiniJanembaStampedeEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        this.setCanFly(false);
        this.setDBZStyle(6);
        this.setAuraColor(0xFFD700);
        this.setScaleVal(0.5F);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return DBSagasEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.42D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D)
                .add(Attributes.ATTACK_SPEED, 2.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        this.targetSelector.removeAllGoals(goal -> true);
        this.goalSelector.addGoal(0, new ScatterGoal());
    }

    public void charge(LivingEntity victim) {
        this.setTarget(victim);
    }

    public boolean isScattering() {
        return this.scatterTicks >= 0;
    }

    @Override
    public boolean isMeleeAllowed() {
        return !this.isScattering() && super.isMeleeAllowed();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (!hit) return false;

        if (target instanceof LivingEntity victim) {
            Vec3 push = victim.position().subtract(this.position());
            double length = push.horizontalDistance();
            if (length > 1.0E-4D) {
                push = push.scale(HIT_KNOCKBACK / length);
                victim.setDeltaMovement(victim.getDeltaMovement().add(push.x, 0.3D, push.z));
                victim.hasImpulse = true;
                victim.hurtMarked = true;
            }

            for (MiniJanembaStampedeEntity member : this.level().getEntitiesOfClass(MiniJanembaStampedeEntity.class,
                    victim.getBoundingBox().inflate(SCATTER_ALERT_RADIUS), member -> member.getTarget() == victim)) {
                member.scatterFrom(victim);
            }
        }

        return true;
    }

    private void scatterFrom(LivingEntity victim) {
        if (this.isScattering()) return;

        Vec3 away = this.position().subtract(victim.position());
        double length = away.horizontalDistance();
        away = length < 1.0E-4D
                ? Vec3.directionFromRotation(0.0F, this.random.nextFloat() * 360.0F)
                : new Vec3(away.x / length, 0.0D, away.z / length);

        double spread = (this.random.nextDouble() - 0.5D) * 1.2D;
        this.scatterDirection = away.yRot((float) spread);
        this.scatterTicks = SCATTER_TICKS;
        this.setTarget(null);
        this.getNavigation().stop();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        if (this.isScattering()) {
            if (--this.scatterTicks <= 0) {
                this.vanishInSmoke();
            }
            return;
        }

        if (this.tickCount > MAX_LIFETIME_TICKS || (this.tickCount > 20 && this.getTarget() == null)) {
            this.vanishInSmoke();
        }
    }

    private void vanishInSmoke() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getY() + 0.4D, this.getZ(), 12, 0.25D, 0.3D, 0.25D, 0.02D);
        }
        this.discard();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public String getGeckolibModelName() {
        return "saga_janemba_fat";
    }

    @Override
    public String getGeckolibTextureName() {
        return "saga_janemba_fat";
    }

    private class ScatterGoal extends Goal {

        private ScatterGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return MiniJanembaStampedeEntity.this.isScattering();
        }

        @Override
        public void tick() {
            MiniJanembaStampedeEntity self = MiniJanembaStampedeEntity.this;
            Vec3 goal = self.position().add(self.scatterDirection.scale(SCATTER_DISTANCE));
            self.getNavigation().moveTo(goal.x, goal.y, goal.z, SCATTER_SPEED);
            self.getLookControl().setLookAt(goal.x, self.getEyeY(), goal.z);
        }
    }
}
