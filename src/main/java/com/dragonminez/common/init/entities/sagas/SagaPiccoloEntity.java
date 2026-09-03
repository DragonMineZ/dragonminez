package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SagaPiccoloEntity{

    public static class SagaPiccoloEarlyEntity extends DBSagasEntity {

        public SagaPiccoloEarlyEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(230);
            }
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFFFFFF);
            this.setTextureVariant(0); //Ejemplo para cambiar de variante de textura

            this.addKiSkill(KiSkillType.MAKANKOSAPPO, 250, 1.0F);
            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.0F, 0xFFF554, 0xFFF554);

            this.setEvade(true, 150);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_piccolo";
        }
    }

    public static class PiccoloDaimaoOldEntity extends DBSagasEntity {
        private static final int TRAP_COOLDOWN = 200;
        private static final int TRAP_FUSE = 60;
        private static final int STUN_DURATION = 60;
        private static final double TRAP_RADIUS = 2.0D;
        private static final int MARKER_RINGS = 6;
        private static final int MARKER_POINTS = 42;
        private static final double MARKER_CENTER_OFFSET = 1.0D;
        private static final int SPARK_INTERVAL = 20;

        private int trapCooldown = TRAP_COOLDOWN;
        private int trapFuse;
        private Vec3 trapPos;

        public PiccoloDaimaoOldEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFF3B3B);
            this.setKiBlastSpeed(1.2F);
            this.setEvade(true, 150);
            this.setScaleVal(1.1f);
            this.setAllowedCombos(350, ComboType.AIR);

            this.addKiSkill(KiSkillType.KI_LASER, 200, 1.0F, 0xFF3B3B, 0xFF3B3B);
        }

        @Override
        public void tick() {
            super.tick();

            if (this.level().isClientSide || !this.isAlive()) return;

            if (this.trapFuse > 0) {
                this.trapFuse--;
                this.drawTrapMarker();

                if (this.trapFuse <= 0) this.springTrap();
                return;
            }

            if (this.getTarget() == null) return;

            if (--this.trapCooldown <= 0) {
                this.trapCooldown = TRAP_COOLDOWN;
                this.trapPos = this.getTarget().position();
                this.trapFuse = TRAP_FUSE;
            }
        }

        private void drawTrapMarker() {
            if (this.trapPos == null || !(this.level() instanceof ServerLevel serverLevel)) return;

            if (this.trapFuse % SPARK_INTERVAL == 0) {
                this.level().playSound(null, this.trapPos.x, this.trapPos.y, this.trapPos.z,
                        MainSounds.KI_SPARKS.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
            }

            if (this.trapFuse % 4 != 0) return;

            double centerY = this.trapPos.y + MARKER_CENTER_OFFSET;
            for (int ring = 0; ring < MARKER_RINGS; ring++) {
                double phi = Math.PI * (ring + 0.5D) / MARKER_RINGS;
                double ringRadius = Math.sin(phi) * TRAP_RADIUS;
                double y = centerY + Math.cos(phi) * TRAP_RADIUS;

                for (int i = 0; i < MARKER_POINTS; i++) {
                    double theta = (Math.PI * 2.0D / MARKER_POINTS) * i;
                    double x = this.trapPos.x + Math.cos(theta) * ringRadius;
                    double z = this.trapPos.z + Math.sin(theta) * ringRadius;

                    serverLevel.sendParticles(MainParticles.KI_LIGHTNING.get(), x, y, z,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        }

        private void springTrap() {
            if (this.trapPos == null) return;

            AABB area = new AABB(
                    this.trapPos.subtract(TRAP_RADIUS, 1.0D, TRAP_RADIUS),
                    this.trapPos.add(TRAP_RADIUS, 2.0D, TRAP_RADIUS));

            for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, area)) {
                if (victim == this) continue;

                double dx = victim.getX() - this.trapPos.x;
                double dz = victim.getZ() - this.trapPos.z;
                if (dx * dx + dz * dz > TRAP_RADIUS * TRAP_RADIUS) continue;

                victim.addEffect(new MobEffectInstance(MainEffects.STUN.get(), STUN_DURATION, 0, false, false, true));
            }

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(MainParticles.KI_FLASH.get(),
                        this.trapPos.x, this.trapPos.y + 0.2D, this.trapPos.z, 65, 0.9D, 0.2D, 0.9D, 0.05D);
            }

            this.playSound(SoundEvents.GENERIC_EXPLODE, 0.5F, 1.8F);
            this.trapPos = null;
        }

    }

    public static class PiccoloDaimaoYoungEntity extends DBSagasEntity {

        private static final int KI_COLOR = 0xFF3B3B;
        private static final int KI_COLOR_INNER = 0xC41212;
        private static final double MOVE_SPEED = 0.40D;
        private static final double ATTACK_SPEED = 5.0D;

        public PiccoloDaimaoYoungEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setScaleVal(1.2f);
            this.setAuraColor(KI_COLOR);
            this.setKiBlastSpeed(1.2F);
            this.setLightning(true);
            this.setEvade(true, 120);
            this.setAllowedCombos(250, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);
            this.setDefaultMovementSpeed(MOVE_SPEED);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(MOVE_SPEED);
            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(ATTACK_SPEED);

            this.addKiSkill(KiSkillType.KI_LASER, 250, 1.0F, KI_COLOR, KI_COLOR);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 400, 1.4F, KI_COLOR, KI_COLOR_INNER);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.4F, KI_COLOR, KI_COLOR_INNER);
        }

    }

    public static class DrumEntity extends DBSagasEntity {

        private static final double MOVE_SPEED = 0.20D;
        private static final double ATTACK_SPEED = 6.0D;
        private static final double KNOCKBACK_RESISTANCE = 0.9D;
        private static final float PUSH_CHANCE = 0.25F;
        private static final double PUSH_STRENGTH = 2.2D;

        public DrumEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setDBZStyle(0);
            this.setEvade(true, 300);
            this.setAllowedCombos(600, ComboType.RAPID_KICKS);

            this.setDefaultMovementSpeed(MOVE_SPEED);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(MOVE_SPEED);
            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(ATTACK_SPEED);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(KNOCKBACK_RESISTANCE);
        }

        @Override
        public boolean doHurtTarget(Entity pTarget) {
            boolean hurt = super.doHurtTarget(pTarget);

            if (hurt && !this.level().isClientSide
                    && pTarget instanceof LivingEntity victim
                    && this.random.nextFloat() < PUSH_CHANCE) {

                victim.knockback(PUSH_STRENGTH, this.getX() - victim.getX(), this.getZ() - victim.getZ());
                this.playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.7F);
            }
            return hurt;
        }

    }

    public static class TambourineEntity extends DBSagasEntity {

        public TambourineEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFF3B3B);
            this.setKiBlastSpeed(1.0F);
            this.setEvade(true, 150);

            this.addKiSkill(KiSkillType.KI_SMALL, 50, 1.2F, 0xFF3B3B, 0xC41212);
        }

    }

    public static class SagaNailEntity extends DBSagasEntity {

        public SagaNailEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(42000);
            }
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFFFFFF);

            this.addKiSkill(KiSkillType.KI_VOLLEY, 250, 1.0F,0xFFF554, 0xFFF554 );
            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.0F, 0xFFF554, 0xFFF554);


            this.setEvade(true, 150);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_piccolo";
        }
    }

    public static class SagaPiccoloKamiEntity extends DBSagasEntity {

        public SagaPiccoloKamiEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(320000000);
            }
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFFFFFF);
            this.setLightning(true);
            this.setTextureVariant(0);
            this.setKiBlastSpeed(2.0f);

            this.setAllowedCombos(150, ComboType.KI_CHARGE_ATTACK, ComboType.AIR);

            this.addKiSkill(KiSkillType.MAKANKOSAPPO, 350, 1.0F);
            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.0F, 0xFFF554, 0xFFF554);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.4F, 0xFFF554, 0xFFF554);

            this.setEvade(true, 150);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_piccolo";
        }
    }


}
