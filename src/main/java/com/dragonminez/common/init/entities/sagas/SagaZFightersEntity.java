package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.alignment.AlignmentBand;
import com.dragonminez.common.init.MainGameRules;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.core.BlockPos;
import com.dragonminez.common.init.entities.WildSpawnRules;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class SagaZFightersEntity {

    public static class KidKrillinEntity extends DBSagasEntity {

        public KidKrillinEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setDBZStyle(0);
            this.setisKid(true);
        }
        @Override
        public String getGeckolibModelName() {
            return "saga_kid_goku";
        }

    }

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

            this.addKiSkill(KiSkillType.TAIYOKEN, 200);
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



    public static class OolongTransformedEntity extends DBSagasEntity {
        public OolongTransformedEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setCanFly(false);
            this.setDBZStyle(2);
            this.setScaleVal(1.5f);
        }
    }
    public static class OolongEntity extends DBSagasEntity {
        public OolongEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setCanFly(false);
            this.setDBZStyle(0);
            this.setisKid(true);
        }
    }

    public static class SagaTeenYamchaEntity extends DBSagasEntity {

        public SagaTeenYamchaEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setScaleVal(0.9f);
            this.setDBZStyle(0);
            this.setEvade(true, 100);
            this.addKiSkill(KiSkillType.WOLF_FANG, 400);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_yamcha";
        }
    }

    public static class YoungYamchaEntity extends DBSagasEntity {

        public YoungYamchaEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setDBZStyle(0);
            this.setEvade(true, 200);

            this.addKiSkill(KiSkillType.KAMEHAMEHA, 300);
            this.addKiSkill(KiSkillType.WOLF_FANG, 500);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_young_yamcha";
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
            this.addKiSkill(KiSkillType.KAMEHAMEHA,200);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_yamcha";
        }

    }

    public static class YoungTienEntity extends DBSagasEntity {
        private static final int DODONPA_COLOR = 0xFFE661;
        public YoungTienEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setDBZStyle(0);
            this.setEvade(true, 100);
            this.setKiBlastSpeed(1.4F);

            this.addKiSkill(KiSkillType.KI_LASER, 100, 1.0F, DODONPA_COLOR, DODONPA_COLOR);
            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.0F, DODONPA_COLOR, DODONPA_COLOR);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_young_yamcha";
        }

    }

    public static class TienT23Entity extends DBSagasEntity {

        private static final int DODONPA_COLOR = 0xFFE661;

        public TienT23Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setDBZStyle(0);
            this.setEvade(true, 100);
            this.setKiBlastSpeed(1.4F);
            this.setAllowedCombos(300, ComboType.AIR);

            this.addKiSkill(KiSkillType.KI_LASER, 100, 1.0F, DODONPA_COLOR, DODONPA_COLOR);
            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.0F, DODONPA_COLOR, DODONPA_COLOR);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_goku";
        }

    }

    public static class YamchaT23Entity extends DBSagasEntity {

        public YamchaT23Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setAuraColor(0xFFFFFF);
            this.setDBZStyle(0);
            this.setEvade(true, 150);
            this.setKiBlastSpeed(1.2F);

            this.addKiSkill(KiSkillType.KAMEHAMEHA, 250);
            this.addKiSkill(KiSkillType.WOLF_FANG, 400);
        }
    }

    public static class YajirobeEntity extends DBSagasEntity {
        public YajirobeEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setDBZStyle(0);
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MainItems.KATANA_YAJIROBE.get()));
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
            this.setKiBlastSpeed(1.0F);
            this.setDBZStyle(0);
        }

    }

    public static class GiranEntity extends DBSagasEntity {

        public GiranEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setDBZStyle(2);
            this.setScaleVal(1.3f);
        }

    }

    public static class WildGiranEntity extends GiranEntity {

        private static final float NATURAL_SPAWN_CHANCE = 0.2F;
        private static final double NATURAL_SPAWN_EXCLUSION_RADIUS = 64.0D;

        public WildGiranEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setScaleVal(1.56f);
        }

        public static boolean canSpawnNaturally(EntityType<? extends Mob> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
            if (!WildSpawnRules.canSpawn(type, level, reason, pos, random)) return false;
            if (reason != MobSpawnType.NATURAL) return true;
            if (random.nextFloat() >= NATURAL_SPAWN_CHANCE) return false;
            return level.getEntitiesOfClass(WildGiranEntity.class, new AABB(pos).inflate(NATURAL_SPAWN_EXCLUSION_RADIUS)).isEmpty();
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes()
                    .add(Attributes.MAX_HEALTH, 180.0D)
                    .add(Attributes.MOVEMENT_SPEED, 0.24D)
                    .add(Attributes.ATTACK_DAMAGE, 14.0D)
                    .add(Attributes.FOLLOW_RANGE, 24.0D)
                    .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_giran";
        }

        @Override
        public String getGeckolibTextureName() {
            return "saga_giran";
        }
    }

    public static class NamEntity extends DBSagasEntity {

        private static final double LEAP_POWER = 1.15D;
        private static final double DIVE_SPEED = 1.7D;
        private static final double CRATER_RADIUS = 1.6D;
        private static final double IMPACT_RADIUS = 3.0D;
        private static final float IMPACT_DAMAGE_MULT = 2.0F;
        private static final float INDESTRUCTIBLE_HARDNESS = 50.0F;

        private Vec3 diveDir = Vec3.ZERO;

        public NamEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setDBZStyle(0);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            this.goalSelector.addGoal(2, new LeapSlamGoal(this));
        }

        void beginLeap() {
            this.getNavigation().stop();
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x * 0.2D, LEAP_POWER, motion.z * 0.2D);
            this.hasImpulse = true;
            this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.7F);
        }

        boolean isPastApex() {
            return this.getDeltaMovement().y <= 0.0D;
        }

        void beginDive(LivingEntity target) {
            Vec3 aim = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).subtract(this.position());
            Vec3 dir = aim.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, -1.0D, 0.0D) : aim.normalize();
            this.diveDir = new Vec3(dir.x, Math.min(dir.y, -0.4D), dir.z).normalize();
            this.sustainDive();
        }

        void sustainDive() {
            this.setDeltaMovement(this.diveDir.scale(DIVE_SPEED));
            this.hasImpulse = true;
        }

        boolean hasLanded() {
            return this.onGround() || this.horizontalCollision || this.verticalCollision;
        }

        void slamImpact() {
            if (this.level().isClientSide) return;

            float damage = (float) (this.getAttributeValue(Attributes.ATTACK_DAMAGE) * IMPACT_DAMAGE_MULT);
            DamageSource source = this.level().damageSources().mobAttack(this);

            List<LivingEntity> hit = this.level().getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().inflate(IMPACT_RADIUS));

            for (LivingEntity victim : hit) {
                if (victim == this) continue;
                victim.hurt(source, damage);
                Vec3 push = victim.position().subtract(this.position());
                push = push.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 1.0D, 0.0D) : push.normalize();
                victim.push(push.x * 0.6D, 0.45D, push.z * 0.6D);
            }

            if (this.level() instanceof ServerLevel serverLevel) {
                this.crater(serverLevel);
                serverLevel.sendParticles(MainParticles.DUST.get(),
                        this.getX(), this.getY() + 0.1D, this.getZ(), 45, 1.3D, 0.15D, 1.3D, 0.05D);
            }

            this.playSound(SoundEvents.GENERIC_EXPLODE, 0.7F, 1.5F);
        }

        private void crater(ServerLevel level) {
            BlockPos center = this.blockPosition();
            int r = (int) Math.ceil(CRATER_RADIUS);

            for (int x = -r; x <= r; x++) {
                for (int y = -r; y <= 0; y++) {
                    for (int z = -r; z <= r; z++) {
                        if (x * x + y * y + z * z > CRATER_RADIUS * CRATER_RADIUS) continue;

                        BlockPos pos = center.offset(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        if (state.isAir()) continue;

                        float hardness = state.getDestroySpeed(level, pos);
                        if (hardness < 0.0F || hardness >= INDESTRUCTIBLE_HARDNESS) continue;
                        if (!MainGameRules.canKiGrief(level, pos, this)) continue;

                        level.destroyBlock(pos, true);
                    }
                }
            }
        }

        static class LeapSlamGoal extends Goal {

            private static final int COOLDOWN = 360;
            private static final double MIN_RANGE = 4.0D;
            private static final double MAX_RANGE = 18.0D;
            private static final int MAX_RISE_TICKS = 20;
            private static final int MAX_DIVE_TICKS = 40;

            private final NamEntity nam;
            private int cooldown;
            private int phaseTicks;
            private boolean diving;
            private boolean finished;

            LeapSlamGoal(NamEntity nam) {
                this.nam = nam;
                setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
            }

            @Override
            public boolean canUse() {
                if (this.cooldown > 0) {
                    this.cooldown--;
                    return false;
                }
                if (this.nam.isStunned() || !this.nam.onGround()) return false;

                LivingEntity target = this.nam.getTarget();
                if (target == null || !target.isAlive()) return false;

                double dist = this.nam.distanceTo(target);
                return dist >= MIN_RANGE && dist <= MAX_RANGE && this.nam.getSensing().hasLineOfSight(target);
            }

            @Override
            public boolean canContinueToUse() {
                LivingEntity target = this.nam.getTarget();
                return !this.finished && target != null && target.isAlive();
            }

            @Override
            public boolean requiresUpdateEveryTick() {
                return true;
            }

            @Override
            public void start() {
                this.diving = false;
                this.finished = false;
                this.phaseTicks = 0;
                this.nam.beginLeap();
            }

            @Override
            public void stop() {
                this.cooldown = COOLDOWN;
                this.diving = false;
                this.finished = false;
                this.phaseTicks = 0;
            }

            @Override
            public void tick() {
                LivingEntity target = this.nam.getTarget();
                if (target == null) {
                    this.finished = true;
                    return;
                }

                this.phaseTicks++;
                this.nam.getLookControl().setLookAt(target, 30.0F, 30.0F);

                if (!this.diving) {
                    if (this.nam.isPastApex() || this.phaseTicks >= MAX_RISE_TICKS) {
                        this.diving = true;
                        this.phaseTicks = 0;
                        this.nam.beginDive(target);
                    }
                    return;
                }

                this.nam.sustainDive();

                if (this.phaseTicks > 1
                        && (this.nam.hasLanded() || this.nam.distanceTo(target) <= 2.0D || this.phaseTicks >= MAX_DIVE_TICKS)) {
                    this.nam.slamImpact();
                    this.finished = true;
                }
            }
        }
    }

    public static class JackieChunEntity extends DBSagasEntity {

        public JackieChunEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setEvade(true, 300);
            this.setDBZStyle(0);
            this.setScaleVal(0.85f);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 400, 0.5F);

        }
    }

    public static class JackieChunFPEntity extends DBSagasEntity {

        public JackieChunFPEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setEvade(true, 300);
            this.setDBZStyle(0);
            this.setScaleVal(1.4f);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 400, 0.5F);

        }
    }

    public static class InvisibleManEntity extends DBSagasEntity {

        public InvisibleManEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setDBZStyle(0);
            this.setInvisible(true);
        }

    }

    public static class AkkumanEntity extends DBSagasEntity {

        private static final double MOVE_SPEED = 0.38D;
        private static final UUID EVIL_BONUS_ID = UUID.fromString("0f2f4a1c-6b3d-4c0e-9a71-5c8d2e7b4a10");
        private static final AttributeModifier EVIL_BONUS =
                new AttributeModifier(EVIL_BONUS_ID, "Akkuman evil alignment bonus", 1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL);

        public AkkumanEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);

            this.setDefaultMovementSpeed(MOVE_SPEED);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(MOVE_SPEED);
        }

        @Override
        public boolean doHurtTarget(Entity pTarget) {
            AttributeInstance attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
            boolean boosted = attack != null && pTarget instanceof Player player && hasEvilAlignment(player);

            if (boosted) attack.addTransientModifier(EVIL_BONUS);

            try {
                return super.doHurtTarget(pTarget);
            } finally {
                if (boosted) attack.removeModifier(EVIL_BONUS);
            }
        }

        private static boolean hasEvilAlignment(Player player) {
            StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
            if (data == null) return false;

            return AlignmentBand.fromValue(data.getResources().getAlignment()) == AlignmentBand.EVIL;
        }

    }


    public static class DraculaEntity extends DBSagasEntity {
        private static final double MOVE_SPEED = 0.30D;
        public DraculaEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);

            this.setDefaultMovementSpeed(MOVE_SPEED);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(MOVE_SPEED);
        }
    }

    public static class MaskedWarriorEntity extends DBSagasEntity {
        public MaskedWarriorEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setCanFly(false);
            this.setDBZStyle(0);
            this.setAllowedCombos(500, ComboType.RAPID_KICKS);
        }

    }

    public static class ChiChiEntity extends DBSagasEntity {
        public ChiChiEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setEvade(true, 300);
            this.setDBZStyle(0);
            this.setScaleVal(0.93f);
        }
        @Override
        public String getGeckolibModelName() {
            return "saga_videl";
        }

    }

    public static class SagaVidelEntity extends DBSagasEntity {
        public SagaVidelEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.0F);
            this.setDBZStyle(0);
            this.addKiSkill(KiSkillType.KI_SMALL, 120, 0.7F, 0xFFFFFF, 0xFFFFFF);
        }

    }

    public static class SagaMrSatanEntity extends DBSagasEntity {
        private static final int LEAP_COOLDOWN = 70;
        private static final double LEAP_MIN_DISTANCE = 4.0D;
        private static final double LEAP_MAX_DISTANCE = 14.0D;
        private static final double LEAP_MIN_RISE = 2.0D;
        private static final double LEAP_MAX_RISE = 9.0D;
        private static final double LEAP_ARC = 3.0D;
        private static final double LEAP_MAX_HORIZONTAL_SPEED = 1.1D;
        private static final double GRAVITY = 0.08D;

        private int leapCooldown = 40;
        private int leapTicks = 0;
        private int leapDuration = 0;
        private boolean leapStruck = false;

        public SagaMrSatanEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setDBZStyle(0);
        }

        @Override
        public void tick() {
            super.tick();
            if (this.level().isClientSide || !this.isAlive()) return;

            if (this.leapCooldown > 0) this.leapCooldown--;
            LivingEntity target = this.getTarget();
            if (this.leapDuration > 0) this.tickLeap(target);
            else if (this.canLeapAt(target)) this.startLeap(target);
        }

        private boolean isBusy() {
            return this.isCasting() || this.isComboing() || this.isEvading() || this.isStunned()
                    || this.isTransforming() || this.isCombatFrozen();
        }

        private boolean canLeapAt(LivingEntity target) {
            if (target == null || !target.isAlive() || this.leapCooldown > 0 || !this.onGround() || this.isBusy()) return false;
            double rise = target.getY() - this.getY();
            if (rise > LEAP_MAX_RISE) return false;
            double horizontal = Math.sqrt(this.distanceToSqr(target.getX(), this.getY(), target.getZ()));
            if (horizontal > LEAP_MAX_DISTANCE) return false;
            return horizontal >= LEAP_MIN_DISTANCE || rise >= LEAP_MIN_RISE;
        }

        private void startLeap(LivingEntity target) {
            double rise = target.getY() - this.getY();
            double apex = Math.max(rise, 0.0D) + LEAP_ARC;
            double upward = Math.sqrt(2.0D * GRAVITY * apex) * 1.05D;
            double fall = Math.sqrt(2.0D * Math.max(apex - rise, 0.5D) / GRAVITY);

            this.leapDuration = Math.max(8, Mth.ceil(upward / GRAVITY + fall));
            this.leapTicks = 0;
            this.leapStruck = false;
            this.getNavigation().stop();
            this.rotateBodyToTarget(target);
            this.setDeltaMovement(this.leapVelocity(target, upward));
            this.hasImpulse = true;

            this.playSound(MainSounds.EVASION1.get(), 1.0F, 0.8F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.1D, this.getZ(), 8, 0.3D, 0.05D, 0.3D, 0.03D);
            }
        }

        private Vec3 leapVelocity(LivingEntity target, double vertical) {
            Vec3 toTarget = new Vec3(target.getX() - this.getX(), 0.0D, target.getZ() - this.getZ());
            double distance = toTarget.length();
            double stopShort = (target.getBbWidth() + this.getBbWidth()) * 0.5D + 0.4D;
            if (distance <= stopShort) return new Vec3(0.0D, vertical, 0.0D);

            int remaining = Math.max(1, this.leapDuration - this.leapTicks);
            Vec3 horizontal = toTarget.scale((distance - stopShort) / distance / remaining);
            if (horizontal.length() > LEAP_MAX_HORIZONTAL_SPEED) horizontal = horizontal.normalize().scale(LEAP_MAX_HORIZONTAL_SPEED);
            return new Vec3(horizontal.x, vertical, horizontal.z);
        }

        private void tickLeap(LivingEntity target) {
            this.leapTicks++;
            if (target == null || !target.isAlive() || this.isBusy() || this.leapTicks > this.leapDuration + 20) {
                this.endLeap();
                return;
            }

            this.getNavigation().stop();
            this.rotateBodyToTarget(target);
            boolean landed = this.leapTicks > 2 && this.onGround();
            double contactRange = landed ? 1.5D : 1.0D;
            if (!this.leapStruck && this.getBoundingBox().inflate(contactRange).intersects(target.getBoundingBox())) {
                this.leapStrike(target);
            }

            if (landed) {
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.1D, this.getZ(), 10, 0.4D, 0.05D, 0.4D, 0.02D);
                }
                this.endLeap();
                return;
            }

            if (!this.leapStruck) this.setDeltaMovement(this.leapVelocity(target, this.getDeltaMovement().y));
        }

        private void leapStrike(LivingEntity target) {
            this.leapStruck = true;
            if (this.doHurtTarget(target)) {
                this.playSound(MainSounds.CRITICO1.get(), 1.0F, 1.1F);
                this.spawnPunchParticles(target);
            }
            this.setDeltaMovement(0.0D, Math.min(this.getDeltaMovement().y, 0.0D), 0.0D);
        }

        private void endLeap() {
            this.leapDuration = 0;
            this.leapTicks = 0;
            this.leapCooldown = LEAP_COOLDOWN;
        }

    }

}
