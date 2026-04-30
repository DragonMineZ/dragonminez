package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.List;

public class SagaZFightersEntity {

    public static class SagaKrillinEntity extends DBSagasEntity {

        public SagaKrillinEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(13000);
            }

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 100);
            this.setisKid(true);

            this.addKiSkill(KiSkillType.KIENZAN, 100, 1.4F, 0xFFFB73, 0xFFFB73);
            this.addKiSkill(KiSkillType.KAMEHAMEHA,200);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta";
        }

    }

    public static class SagaTienShinhanEntity extends DBSagasEntity {

        public SagaTienShinhanEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 100);
            this.addKiSkill(KiSkillType.KI_LASER, 100, 1.0F, 0xFFE661, 0xFFE661);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_goku";
        }

    }
    public static class SagaYamchaEntity extends DBSagasEntity {

        public SagaYamchaEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.0F);
            this.setDBZStyle(0);
            this.setEvade(true, 100);
            this.addKiSkill(KiSkillType.KI_LASER, 100, 1.0F, 0xFFE661, 0xFFE661);
            this.addKiSkill(KiSkillType.KAMEHAMEHA,200);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_yamcha";
        }

    }

    public static class SagaShinEntity extends DBSagasEntity {

        public SagaShinEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 100);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 170, 1.0F, 0xFFE661, 0xFFE661);
            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.0F, 0xFFE661, 0xFFE661);

        }

    }

    public static class SagaKibitoEntity extends DBSagasEntity {

        public SagaKibitoEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 100);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 170, 1.0F, 0xFFE661, 0xFFE661);
            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.0F, 0xFFE661, 0xFFE661);

        }

    }

    public static class ChaozEntity extends DBSagasEntity {

        private static final EntityDataAccessor<Boolean> IS_EXPLODING = SynchedEntityData.defineId(ChaozEntity.class, EntityDataSerializers.BOOLEAN);

        private boolean isAttached = false;
        private int fuseTimer = 0;
        private int explodeTimer = 3;
        private boolean hasCheckedExplosionChance = false;

        public ChaozEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 100);
            this.setisKid(true);
            this.addKiSkill(KiSkillType.KI_LASER, 100, 1.0F, 0xFFE661, 0xFFE661);
        }

        @Override
        public void tick() {
            super.tick();

            if (!this.isAlive()) return;

            float healthThreshold = this.getMaxHealth() * 0.25f;

            if (this.getHealth() <= healthThreshold && !this.hasCheckedExplosionChance && !isExploding()) {
                this.hasCheckedExplosionChance = true;

                if (this.random.nextFloat() < 1.0F) {
                    this.setExploding(true);
                }
            }

            if (isExploding()) {
                LivingEntity target = this.getTarget();

                if (target == null) {
                    target = this.level().getNearestPlayer(this, 15.0D);
                    if (target != null) {
                        this.setTarget(target);
                    }
                }

                if (target != null) {
                    if (this.isAttached || this.distanceTo(target) <= 2.0D) {
                        this.isAttached = true;

                        Vec3 lookAngle = target.getLookAngle();
                        double behindX = target.getX() - (lookAngle.x * 0.8D);
                        double behindZ = target.getZ() - (lookAngle.z * 0.8D);

                        this.setPos(behindX, target.getY(), behindZ);
                        this.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
                        this.setYBodyRot(this.getYRot());
                        this.setYHeadRot(this.getYRot());

                        this.setDeltaMovement(0, 0, 0);
                        this.getNavigation().stop();
                        this.setAggressive(false);

                        fuseTimer++;

                        if (fuseTimer == 1 || fuseTimer % 15 == 0) {
                            this.playSound(net.minecraft.sounds.SoundEvents.CREEPER_PRIMED, 1.0F, 0.5F + (fuseTimer / 60.0F));
                        }

                        if (fuseTimer >= explodeTimer * 20) {
                            explode();
                        }
                    } else {
                        this.getNavigation().moveTo(target, 1.8D);
                    }
                }
            }
        }

        private void explode() {
            if (!this.level().isClientSide) {
                float radius = 5.0F;

                double baseDamage = this.getAttributeValue(Attributes.ATTACK_DAMAGE);
                float finalDamage = (float) (baseDamage * 4.0);

                DamageSource damageSource = this.level().damageSources().explosion(this, this);

                AABB area = this.getBoundingBox().inflate(radius);
                List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, area);

                for (LivingEntity target : entities) {
                    if (target != this && this.distanceToSqr(target) <= radius * radius) {
                        target.hurt(damageSource, finalDamage);
                    }
                }

                this.level().explode(this, this.getX(), this.getY(), this.getZ(), radius, Level.ExplosionInteraction.MOB);

                this.discard();
            }
        }

        @Override
        public boolean doHurtTarget(Entity pEntity) {
            if (this.isExploding()) return false;
            return super.doHurtTarget(pEntity);
        }

        @Override
        public boolean canAttack(LivingEntity pTarget) {
            if (this.isExploding()) return false;
            return super.canAttack(pTarget);
        }

        @Override
        protected void defineSynchedData() {
            super.defineSynchedData();
            this.entityData.define(IS_EXPLODING, false);
        }

        public void setExploding(boolean exploding) {
            this.entityData.set(IS_EXPLODING, exploding);
        }

        public boolean isExploding() {
            return this.entityData.get(IS_EXPLODING);
        }

        @Override
        public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
            super.registerControllers(controllers);

            controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(this, "explode_controller", 0, this::explodePredicate));
        }

        private <T extends software.bernie.geckolib.core.animatable.GeoAnimatable> PlayState explodePredicate(AnimationState<T> event) {
            if (this.isExploding()) {
                event.getController().setAnimation(RawAnimation.begin().thenPlay("cell_absorb"));
                return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }
    }

    public static class BasicNPCEntity extends DBSagasEntity {

        public BasicNPCEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
        }

    }


}
