package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class SagaRedRibbonEntities {

    public static class ColonelSilverEntity extends DBSagasEntity {

        public ColonelSilverEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setDBZStyle(0);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_colonel_silver";
        }

    }

    public static class GeneralBlueEntity extends DBSagasEntity {

        public GeneralBlueEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setDBZStyle(0);
            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(8.0D);
        }
        @Override
        public String getGeckolibModelName() {
            return "saga_colonel_silver";
        }

    }

    public static class SergeantMetallicEntity extends DBSagasEntity {

        public SergeantMetallicEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setDBZStyle(2);
            this.setScaleVal(1.4f);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_sergeant_metallic";
        }

    }
    
    public static class TaoPaiPaiEntity extends DBSagasEntity {

        private static final int DODONPA_COLOR = 0xFFE661;
        private static final int DODONPA_COOLDOWN = 400;

        private static final int TNT_INTERVAL = 600;
        private static final double TNT_SAFE_DISTANCE = 10.0D;
        private static final int TNT_RETREAT_TIMEOUT = 100;

        private static final float PUSH_CHANCE = 0.25F;
        private static final double PUSH_STRENGTH = 2.2D;

        private static final int JUMP_CHECK_INTERVAL = 100;
        private static final float JUMP_CHANCE = 0.4F;
        private static final double JUMP_POWER = 1.1D;

        private int tntTimer = TNT_INTERVAL;
        private int retreatTicks;
        private Vec3 tntPos = Vec3.ZERO;

        public TaoPaiPaiEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setDBZStyle(1);
            this.setEvade(true, 100);

            this.addKiSkill(KiSkillType.KI_LASER, DODONPA_COOLDOWN, 1.0F, DODONPA_COLOR, DODONPA_COLOR);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            this.goalSelector.addGoal(2, new TntRetreatGoal(this));
        }

        @Override
        public boolean doHurtTarget(Entity pTarget) {
            boolean hurt = super.doHurtTarget(pTarget);

            if (hurt && !this.level().isClientSide
                    && pTarget instanceof LivingEntity victim
                    && this.random.nextFloat() < PUSH_CHANCE) {

                victim.knockback(PUSH_STRENGTH, this.getX() - victim.getX(), this.getZ() - victim.getZ());
                this.playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.9F);
            }
            return hurt;
        }

        @Override
        public void tick() {
            super.tick();

            if (this.level().isClientSide || !this.isAlive()) return;
            if (this.getTarget() == null) return;

            if (--this.tntTimer <= 0) {
                this.tntTimer = TNT_INTERVAL;
                this.dropDynamite();
            }

            if (this.onGround() && this.retreatTicks <= 0
                    && this.tickCount % JUMP_CHECK_INTERVAL == 0
                    && this.random.nextFloat() < JUMP_CHANCE) {

                Vec3 motion = this.getDeltaMovement();
                this.setDeltaMovement(motion.x, JUMP_POWER, motion.z);
                this.hasImpulse = true;
            }
        }

        private void dropDynamite() {
            this.tntPos = this.position();
            this.retreatTicks = TNT_RETREAT_TIMEOUT;

            PrimedTnt tnt = new PrimedTnt(this.level(), this.getX(), this.getY(), this.getZ(), this);
            this.level().addFreshEntity(tnt);
            this.playSound(SoundEvents.TNT_PRIMED, 1.0F, 1.0F);
        }

        static class TntRetreatGoal extends Goal {

            private static final double RETREAT_SPEED = 1.6D;
            private static final int FLEE_RADIUS = 14;
            private static final int FLEE_VERTICAL = 7;

            private final TaoPaiPaiEntity tao;

            TntRetreatGoal(TaoPaiPaiEntity tao) {
                this.tao = tao;
                setFlags(EnumSet.of(Flag.MOVE));
            }

            @Override
            public boolean canUse() {
                return this.tao.retreatTicks > 0;
            }

            @Override
            public boolean canContinueToUse() {
                return canUse();
            }

            @Override
            public boolean requiresUpdateEveryTick() {
                return true;
            }

            @Override
            public void start() {
                pickFleePosition();
            }

            @Override
            public void stop() {
                this.tao.retreatTicks = 0;
                this.tao.getNavigation().stop();
            }

            @Override
            public void tick() {
                this.tao.retreatTicks--;

                if (this.tao.position().distanceTo(this.tao.tntPos) >= TNT_SAFE_DISTANCE) {
                    this.tao.retreatTicks = 0;
                    return;
                }

                if (this.tao.getNavigation().isDone()) {
                    pickFleePosition();
                }
            }

            private void pickFleePosition() {
                Vec3 away = DefaultRandomPos.getPosAway(this.tao, FLEE_RADIUS, FLEE_VERTICAL, this.tao.tntPos);
                if (away != null) {
                    this.tao.getNavigation().moveTo(away.x, away.y, away.z, RETREAT_SPEED);
                }
            }
        }

    }

    public static class NinjaMurasakiEntity extends DBSagasEntity {
        private static final int HITS_BEFORE_FLEEING = 3;
        private static final int RETREAT_DURATION = 60;

        private int hitsLanded;
        private int retreatTicks;

        public NinjaMurasakiEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setDBZStyle(0);
            this.setEvade(true, 100);
            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(6.0D);
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MainItems.KATANA_YAJIROBE.get()));
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            this.goalSelector.addGoal(2, new CowardlyRetreatGoal(this));
        }

        @Override
        public boolean doHurtTarget(Entity pTarget) {
            boolean hurt = super.doHurtTarget(pTarget);

            if (hurt && !this.level().isClientSide && ++this.hitsLanded >= HITS_BEFORE_FLEEING) {
                this.hitsLanded = 0;
                this.retreatTicks = RETREAT_DURATION;
            }
            return hurt;
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_nam";
        }

        static class CowardlyRetreatGoal extends Goal {

            private static final double RETREAT_SPEED = 1.5D;
            private static final int FLEE_RADIUS = 12;
            private static final int FLEE_VERTICAL = 6;

            private final NinjaMurasakiEntity murasaki;

            CowardlyRetreatGoal(NinjaMurasakiEntity murasaki) {
                this.murasaki = murasaki;
                setFlags(EnumSet.of(Flag.MOVE));
            }

            @Override
            public boolean canUse() {
                return this.murasaki.retreatTicks > 0 && this.murasaki.getTarget() != null;
            }

            @Override
            public boolean canContinueToUse() {
                return canUse();
            }

            @Override
            public boolean requiresUpdateEveryTick() {
                return true;
            }

            @Override
            public void start() {
                pickFleePosition();
            }

            @Override
            public void stop() {
                this.murasaki.retreatTicks = 0;
                this.murasaki.getNavigation().stop();
            }

            @Override
            public void tick() {
                this.murasaki.retreatTicks--;

                LivingEntity target = this.murasaki.getTarget();
                if (target != null) {
                    this.murasaki.getLookControl().setLookAt(target, 30.0F, 30.0F);
                }

                if (this.murasaki.getNavigation().isDone()) {
                    pickFleePosition();
                }
            }

            private void pickFleePosition() {
                LivingEntity target = this.murasaki.getTarget();
                if (target == null) return;

                Vec3 away = DefaultRandomPos.getPosAway(this.murasaki, FLEE_RADIUS, FLEE_VERTICAL, target.position());
                if (away != null) {
                    this.murasaki.getNavigation().moveTo(away.x, away.y, away.z, RETREAT_SPEED);
                }
            }
        }

    }

    public static class GeneralRedEntity extends DBSagasEntity {

        public GeneralRedEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setDBZStyle(0);
        }

    }

    public static class GeneralBlackRobotEntity extends DBSagasEntity {

        public GeneralBlackRobotEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setDBZStyle(0);
        }

    }

    public static class TaoPaiPaiCyborgEntity extends DBSagasEntity {

        private static final int DODONPA_COLOR = 0xFFE661;

        public TaoPaiPaiCyborgEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setDBZStyle(1);
            this.setEvade(true, 100);
            this.setKiBlastSpeed(1.4F);

            this.addKiSkill(KiSkillType.KI_LASER, 400, 1.0F, DODONPA_COLOR, DODONPA_COLOR);
            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.0F, DODONPA_COLOR, DODONPA_COLOR);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_tao_pai_pai";
        }

    }

}
