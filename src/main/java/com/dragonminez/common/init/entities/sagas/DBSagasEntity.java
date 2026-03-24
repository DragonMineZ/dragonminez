package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.goals.SagasUseSkillGoal;
import com.dragonminez.common.init.entities.ki.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public abstract class DBSagasEntity extends Monster implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_CASTING = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_FLYING = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_FLYING_FAST = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> SKILL_TYPE = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AURA_COLOR = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> AURA_TYPE = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Boolean> IS_EVADING = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_COMBOING = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> CURRENT_COMBO_ID = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> BATTLE_POWER = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TRANSFORMING = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> KI_CHARGE = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> IS_LIGHTNING = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> LIGHTNING_COLOR = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> DBZ_STYLE = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.INT);

    protected static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation ANIM_RUN = RawAnimation.begin().thenLoop("run1");
    protected static final RawAnimation ANIM_ATTACK1 = RawAnimation.begin().thenPlay("attack1_1");
    protected static final RawAnimation ANIM_ATTACK2 = RawAnimation.begin().thenPlay("attack2_1");
    protected static final RawAnimation ANIM_ATTACK3 = RawAnimation.begin().thenPlay("attack3_1");
    protected static final RawAnimation ANIM_TRANSFORMATION1 = RawAnimation.begin().thenPlay("transformation_1");

    protected static final RawAnimation ANIM_IDLE_2 = RawAnimation.begin().thenLoop("idle2");
    protected static final RawAnimation ANIM_WALK_2 = RawAnimation.begin().thenLoop("walk2");
    protected static final RawAnimation ANIM_RUN_2 = RawAnimation.begin().thenLoop("run2");
    protected static final RawAnimation ANIM_ATTACK1_2 = RawAnimation.begin().thenPlay("attack1_2");
    protected static final RawAnimation ANIM_ATTACK2_2 = RawAnimation.begin().thenPlay("attack2_2");
    protected static final RawAnimation ANIM_ATTACK3_2 = RawAnimation.begin().thenPlay("attack3_2");
    protected static final RawAnimation ANIM_TRANSFORMATION2 = RawAnimation.begin().thenPlay("transformation_2");

    protected static final RawAnimation ANIM_IDLE_3 = RawAnimation.begin().thenLoop("idle3");
    protected static final RawAnimation ANIM_WALK_3 = RawAnimation.begin().thenLoop("walk3");
    protected static final RawAnimation ANIM_RUN_3 = RawAnimation.begin().thenLoop("run3");
    protected static final RawAnimation ANIM_ATTACK1_3 = RawAnimation.begin().thenPlay("attack1_3");
    protected static final RawAnimation ANIM_ATTACK2_3 = RawAnimation.begin().thenPlay("attack2_3");
    protected static final RawAnimation ANIM_ATTACK3_3 = RawAnimation.begin().thenPlay("attack3_3");
    protected static final RawAnimation ANIM_TRANSFORMATION3 = RawAnimation.begin().thenPlay("transformation_3");

    protected static final RawAnimation ANIM_FLY = RawAnimation.begin().thenLoop("fly");
    protected static final RawAnimation ANIM_FLY_FAST = RawAnimation.begin().thenLoop("fly_fast");

    protected static final RawAnimation ANIM_EVADE = RawAnimation.begin().thenPlay("evasion1");
    protected static final RawAnimation ANIM_KIWAVE = RawAnimation.begin().thenPlay("ki_kame");
    protected static final RawAnimation ANIM_KILASER = RawAnimation.begin().thenPlay("kilaser");
    protected static final RawAnimation ANIM_BARRIER = RawAnimation.begin().thenPlay("barrier");
    protected static final RawAnimation ANIM_KIATTACK = RawAnimation.begin().thenPlay("kiattack");
    protected static final RawAnimation ANIM_KIBALL = RawAnimation.begin().thenPlay("kiball");
    protected static final RawAnimation ANIM_KIBLAST = RawAnimation.begin().thenPlay("kiblast");
    protected static final RawAnimation ANIM_TAIL = RawAnimation.begin().thenLoop("tail");
    protected static final RawAnimation ANIM_TRANSFORM = RawAnimation.begin().thenLoop("transform");
    protected static final RawAnimation ANIM_GRAB = RawAnimation.begin().thenLoop("grab");

    protected static final RawAnimation ANIM_KI_MAKKAKO = RawAnimation.begin().thenPlay("ki_makkako");
    protected static final RawAnimation ANIM_KI_KAME = RawAnimation.begin().thenPlay("ki_kame");
    protected static final RawAnimation ANIM_KI_MASENKO = RawAnimation.begin().thenPlay("ki_masenko");
    protected static final RawAnimation ANIM_KI_BARRIER = RawAnimation.begin().thenPlay("ki_barrier");
    protected static final RawAnimation ANIM_KI_GALICK = RawAnimation.begin().thenPlay("ki_galick");
    protected static final RawAnimation ANIM_KI_EXPLOSION = RawAnimation.begin().thenPlay("ki_explosion");
    protected static final RawAnimation ANIM_KI_FINALFLASH = RawAnimation.begin().thenPlay("ki_finalflash");
    protected static final RawAnimation ANIM_KI_DISC = RawAnimation.begin().thenPlay("ki_finalflash");
    protected static final RawAnimation ANIM_KI_LASER = RawAnimation.begin().thenPlay("ki_laser");

    protected static final RawAnimation ANIM_COMBO1 = RawAnimation.begin().thenPlay("combo1");
    protected static final RawAnimation ANIM_COMBO2 = RawAnimation.begin().thenPlay("combo2");

    private double roarDamage = 50.0D;
    private double roarRange = 15.0D;
    private double flySpeed = 0.45D;
    private float kiBlastDamage = 20.0F;
    private float kiBlastSpeed = 0.6F;
    private boolean canFly = true;
    protected int castTimer = 0;
    private int chargeSoundTimer = 0;
    private static final int AURA_LIGHT_LEVEL = 12;
    private static final int AURA_LIGHT_INTERVAL = 2;
    private static final int AURA_LIGHT_STEP = 1;
    private BlockPos auraLightPos;
    private int auraLightLevel = 0;

    private boolean canEvade = false;
    private int evadeCooldownMax = 0;
    private int currentEvadeTimer = 0;
    private int evasionStateTicks = 0;

    private boolean canUseWildSense = false;
    private int wildSenseCooldownMax = 0;
    private int currentWildSenseCooldown = 0;

    private boolean comboEnabled = false;
    private int comboCooldownMax = 0;
    private int currentComboCooldown = 0;
    private int comboTimer = 0;
    private LivingEntity comboTarget = null;
    private int activeComboId = -1;

    private boolean isAttacking = false;

    private boolean canUseKiHame = false;
    private int kiHameCooldownMax = 0;
    private int currentKiHameCooldown = 0;
    private float kiHameDamage = 0.0F;

    private boolean canUseKiGalick = false;
    private int kiGalickCooldownMax = 0;
    private int currentKiGalickCooldown = 0;
    private float kiGalickDamage = 0.0F;

    private boolean canUseKiMakkanko = false;
    private int kiMakkankoCooldownMax = 0;
    private int currentKiMakkankoCooldown = 0;
    private float kiMakkankoDamage = 0.0F;

    private boolean canUseKiLaser = false;
    private int kiLaserCooldownMax = 0;
    private int currentKiLaserCooldown = 0;
    private float kiLaserDamage = 0.0F;

    private boolean canUseKiExplosion = false;
    private int kiExplosionCooldownMax = 0;
    private int currentKiExplosionCooldown = 0;
    private float kiExplosionDamage = 0.0F;

    private boolean canUseKiBarrier = false;
    private int kiBarrierCooldownMax = 0;
    private int currentKiBarrierCooldown = 0;
    private float kiBarrierDamage = 0.0F;

    private boolean canUseSKPRoar = false;
    private int skpRoarCooldownMax = 0;
    private int currentSkpRoarCooldown = 0;
    private float skpRoarDamage = 0.0F;

    private boolean canUseKiWave = false;
    private int kiWaveCooldownMax = 0;
    private int currentKiWaveCooldown = 0;
    private float kiWaveDamage = 0.0F;
    private int kiWaveColorMain = 0xFFFFFF;
    private int kiWaveColorBorder = 0xFFFFFF;
    private float kiWaveSize = 1.0F;

    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);

    protected DBSagasEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public void setDBZStyle(int style) {
        this.entityData.set(DBZ_STYLE, style);
    }

    public int getDBZStyle() {
        return this.entityData.get(DBZ_STYLE);
    }

    public void setCanFly(boolean canFly) {
        this.canFly = canFly;
    }

    public boolean canFly() {
        return this.canFly;
    }

    public void setFlyingFast(boolean flyingFast) {
        this.entityData.set(IS_FLYING_FAST, flyingFast);
    }

    public boolean isFlyingFast() {
        return this.entityData.get(IS_FLYING_FAST);
    }

    public void setCombo(int id, int cooldown) {
        this.comboEnabled = true;
        this.activeComboId = id;
        this.comboCooldownMax = cooldown;
        this.currentComboCooldown = cooldown;
    }

    public void setEvade(boolean active, int cooldown) {
        this.canEvade = active;
        this.evadeCooldownMax = cooldown;
        this.currentEvadeTimer = cooldown;
    }

    public void setWildSense(boolean active, int cooldown) {
        this.canUseWildSense = active;
        this.wildSenseCooldownMax = cooldown;
        this.currentWildSenseCooldown = cooldown;
    }

    public void setKiHame(boolean active, int cooldown, float damage) {
        this.canUseKiHame = active;
        this.kiHameCooldownMax = cooldown;
        this.currentKiHameCooldown = cooldown;
        this.kiHameDamage = damage;
    }

    public void setKiGalick(boolean active, int cooldown, float damage) {
        this.canUseKiGalick = active;
        this.kiGalickCooldownMax = cooldown;
        this.currentKiGalickCooldown = cooldown;
        this.kiGalickDamage = damage;
    }

    public void setKiMakkanko(boolean active, int cooldown, float damage) {
        this.canUseKiMakkanko = active;
        this.kiMakkankoCooldownMax = cooldown;
        this.currentKiMakkankoCooldown = cooldown;
        this.kiMakkankoDamage = damage;
    }

    public void setKiLaser(boolean active, int cooldown, float damage) {
        this.canUseKiLaser = active;
        this.kiLaserCooldownMax = cooldown;
        this.currentKiLaserCooldown = cooldown;
        this.kiLaserDamage = damage;
    }

    public void setKiExplosion(boolean active, int cooldown, float damage) {
        this.canUseKiExplosion = active;
        this.kiExplosionCooldownMax = cooldown;
        this.currentKiExplosionCooldown = cooldown;
        this.kiExplosionDamage = damage;
    }

    public void setKiBarrier(boolean active, int cooldown, float damage) {
        this.canUseKiBarrier = active;
        this.kiBarrierCooldownMax = cooldown;
        this.currentKiBarrierCooldown = cooldown;
        this.kiBarrierDamage = damage;
    }

    public void setSKPRoar(boolean active, int cooldown, float damage) {
        this.canUseSKPRoar = active;
        this.skpRoarCooldownMax = cooldown;
        this.currentSkpRoarCooldown = cooldown;
        this.skpRoarDamage = damage;
    }

    public void setKiWave(boolean active, int cooldown, float damage, int colorMain, int colorBorder, float size) {
        this.canUseKiWave = active;
        this.kiWaveCooldownMax = cooldown;
        this.currentKiWaveCooldown = cooldown;
        this.kiWaveDamage = damage;
        this.kiWaveColorMain = colorMain;
        this.kiWaveColorBorder = colorBorder;
        this.kiWaveSize = size;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this) {
            @Override
            public boolean canUse() {
                return super.canUse() && getTarget() == null;
            }

            @Override
            public boolean canContinueToUse() {
                return super.canContinueToUse() && getTarget() == null;
            }
        });

        this.goalSelector.addGoal(2, new SagasUseSkillGoal(this));

        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.8D, false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 45.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {

            this.handleCommonCombatMovement(this.getTarget(), this.isCasting() || this.isComboing() || this.isTransforming());

            if (this.tickCount % AURA_LIGHT_INTERVAL == 0) updateAuraLight();

            if (!this.isFlying() && this.isFlyingFast()) {
                this.setFlyingFast(false);
            }

            if (!this.isCasting() && !this.isComboing()) {
                if (this.canEvade && this.currentEvadeTimer > 0) this.currentEvadeTimer--;
                if (this.canUseWildSense && this.currentWildSenseCooldown > 0) this.currentWildSenseCooldown--;
                if (this.comboEnabled && this.currentComboCooldown > 0) this.currentComboCooldown--;

                if (this.canUseKiHame && this.currentKiHameCooldown > 0) this.currentKiHameCooldown--;
                if (this.canUseKiGalick && this.currentKiGalickCooldown > 0) this.currentKiGalickCooldown--;
                if (this.canUseKiMakkanko && this.currentKiMakkankoCooldown > 0) this.currentKiMakkankoCooldown--;
                if (this.canUseKiLaser && this.currentKiLaserCooldown > 0) this.currentKiLaserCooldown--;
                if (this.canUseKiExplosion && this.currentKiExplosionCooldown > 0) this.currentKiExplosionCooldown--;
                if (this.canUseKiBarrier && this.currentKiBarrierCooldown > 0) this.currentKiBarrierCooldown--;
                if (this.canUseSKPRoar && this.currentSkpRoarCooldown > 0) this.currentSkpRoarCooldown--;
                if (this.canUseKiWave && this.currentKiWaveCooldown > 0) this.currentKiWaveCooldown--;
            }

            if (this.isCasting()) {
                this.castTimer++;

                this.getNavigation().stop();
                this.setDeltaMovement(0, 0, 0);

                if (this.getTarget() != null) {
                    this.lookAt(this.getTarget(), 360, 360);
                }

                int skill = this.getSkillType();

                if (this.castTimer == 1) {
                    executeSkillEffect(skill);
                }

                if (this.castTimer >= 60) {
                    this.stopCasting();
                }
            }

            if (this.canUseWildSense && this.currentWildSenseCooldown <= 0 && this.getTarget() != null && !this.isCasting() && !this.isComboing()) {
                this.performTeleport(this.getTarget());
                this.currentWildSenseCooldown = this.wildSenseCooldownMax;
            }

            if (this.canEvade && !this.isCasting() && !this.isComboing()) {
                if (this.hurtTime > 0 && this.currentEvadeTimer <= 0) {
                    this.performEvasion();
                }
            }

            if (this.isEvading()) {
                evasionStateTicks++;
                if (evasionStateTicks > 12) {
                    this.setEvading(false);
                    this.evasionStateTicks = 0;
                }
            }

            if (this.comboEnabled) {
                if (this.isComboing()) {
                    this.comboTimer++;
                    handleComboLogic();
                } else if (this.currentComboCooldown <= 0 && !this.isCasting() && this.getTarget() != null) {
                    if (this.distanceTo(this.getTarget()) < 6.0D) {
                        this.comboTarget = this.getTarget();
                        this.setComboing(true);

                        if (this.activeComboId == 10) {
                            this.entityData.set(CURRENT_COMBO_ID, this.random.nextInt(2));
                        } else {
                            this.entityData.set(CURRENT_COMBO_ID, this.activeComboId);
                        }

                        this.comboTimer = 0;
                        this.currentComboCooldown = comboCooldownMax;
                    }
                }
            }

            if (this.isCharge()) {
                if (this.chargeSoundTimer <= 0) {
                    this.playSound(MainSounds.KI_CHARGE_LOOP.get(), 0.8F, 1.0F);
                    this.chargeSoundTimer = 40;
                }
                this.chargeSoundTimer--;
            } else {
                this.chargeSoundTimer = 0;
            }

            if (this.isLightning()) {
                if (this.random.nextInt(30) == 0) {
                    float pitch = 0.9F + this.random.nextFloat() * 0.2F;
                    this.playSound(MainSounds.KI_SPARKS.get(), 0.3F, pitch);
                }
            }
        }
    }

    private void executeSkillEffect(int skillType) {
        if (this.getTarget() == null) return;

        int syncCastTime = 30;

        switch (skillType) {
            case 1:
                KiWaveEntity kamehameha = new KiWaveEntity(this.level(), this);
                kamehameha.setupKiHame(this, this.kiHameDamage, this.getKiBlastSpeed(), 2.0F, syncCastTime);
                break;
            case 2:
                KiWaveEntity galick = new KiWaveEntity(this.level(), this);
                galick.setupKiGalickGun(this, this.kiGalickDamage, this.getKiBlastSpeed(), 2.0F, syncCastTime);
                break;
            case 3:
                KiLaserEntity makkanko = new KiLaserEntity(this.level(), this);
                makkanko.setupKiMakkankosanpo(this, this.kiMakkankoDamage, this.getKiBlastSpeed() * 2.0F, syncCastTime);
                break;
            case 4:
                KiLaserEntity laser = new KiLaserEntity(this.level(), this);
                laser.setupKiLaser(this, this.kiLaserDamage, this.getKiBlastSpeed() * 3.0F, this.getAuraColor(), this.getAuraColor(), syncCastTime);
                break;
            case 5:
                KiExplosionEntity explosion = new KiExplosionEntity(this.level(), this);
                explosion.setupKiExplosion(this, this.kiExplosionDamage, this.getAuraColor(), this.getAuraColor(), syncCastTime);
                break;
            case 6:
                KiBarrierEntity barrier = new KiBarrierEntity(this.level(), this);
                barrier.setupKiBarrier(this, this.getAuraColor(), this.getAuraColor(), syncCastTime);
                break;
            case 7:
                KiExplosionEntity roar = new KiExplosionEntity(this.level(), this);
                roar.setupKiExplosion(this, this.skpRoarDamage, this.getAuraColor(), this.getAuraColor(), syncCastTime);
                break;
            case 8:
                KiWaveEntity genericWave = new KiWaveEntity(this.level(), this);
                genericWave.setupKiWave(this, this.kiWaveDamage, this.getKiBlastSpeed(), this.kiWaveColorMain, this.kiWaveColorBorder, this.kiWaveSize, syncCastTime);
                break;
        }
    }

    public boolean hasSkillReady() {
        if (this.getTarget() == null || this.distanceTo(this.getTarget()) <= 10.0D) {
            return false;
        }
        return (this.canUseKiHame && this.currentKiHameCooldown <= 0) ||
                (this.canUseKiGalick && this.currentKiGalickCooldown <= 0) ||
                (this.canUseKiMakkanko && this.currentKiMakkankoCooldown <= 0) ||
                (this.canUseKiLaser && this.currentKiLaserCooldown <= 0) ||
                (this.canUseKiExplosion && this.currentKiExplosionCooldown <= 0) ||
                (this.canUseKiBarrier && this.currentKiBarrierCooldown <= 0) ||
                (this.canUseSKPRoar && this.currentSkpRoarCooldown <= 0) ||
                (this.canUseKiWave && this.currentKiWaveCooldown <= 0);
    }

    public void startFirstAvailableSkill() {
        if (this.canUseKiHame && this.currentKiHameCooldown <= 0) {
            this.startCasting(1);
            this.currentKiHameCooldown = this.kiHameCooldownMax;
        } else if (this.canUseKiGalick && this.currentKiGalickCooldown <= 0) {
            this.startCasting(2);
            this.currentKiGalickCooldown = this.kiGalickCooldownMax;
        } else if (this.canUseKiMakkanko && this.currentKiMakkankoCooldown <= 0) {
            this.startCasting(3);
            this.currentKiMakkankoCooldown = this.kiMakkankoCooldownMax;
        } else if (this.canUseKiLaser && this.currentKiLaserCooldown <= 0) {
            this.startCasting(4);
            this.currentKiLaserCooldown = this.kiLaserCooldownMax;
        } else if (this.canUseKiExplosion && this.currentKiExplosionCooldown <= 0) {
            this.startCasting(5);
            this.currentKiExplosionCooldown = this.kiExplosionCooldownMax;
        } else if (this.canUseKiBarrier && this.currentKiBarrierCooldown <= 0) {
            this.startCasting(6);
            this.currentKiBarrierCooldown = this.kiBarrierCooldownMax;
        } else if (this.canUseSKPRoar && this.currentSkpRoarCooldown <= 0) {
            this.startCasting(7);
            this.currentSkpRoarCooldown = this.skpRoarCooldownMax;
        } else if (this.canUseKiWave && this.currentKiWaveCooldown <= 0) {
            this.startCasting(8);
            this.currentKiWaveCooldown = this.kiWaveCooldownMax;
        }
    }

    private void updateAuraLight() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        boolean shouldEmitLight = this.isTransforming() || this.isCharge();
        int targetLevel = shouldEmitLight ? AURA_LIGHT_LEVEL : 0;
        this.auraLightLevel = approach(this.auraLightLevel, targetLevel, AURA_LIGHT_STEP);

        if (this.auraLightLevel <= 0) {
            removeAuraLight(serverLevel);
            return;
        }

        BlockPos targetPos = this.blockPosition().above();
        if (!canHostAuraLight(serverLevel, targetPos)) {
            targetPos = this.blockPosition();
            if (!canHostAuraLight(serverLevel, targetPos)) {
                removeAuraLight(serverLevel);
                return;
            }
        }

        if (this.auraLightPos != null && !this.auraLightPos.equals(targetPos)) {
            clearAuraLightIfOwned(serverLevel, this.auraLightPos);
        }

        BlockState currentState = serverLevel.getBlockState(targetPos);
        if (!isAuraLight(currentState) || currentState.getValue(LightBlock.LEVEL) != this.auraLightLevel) {
            BlockState lightState = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, this.auraLightLevel);
            serverLevel.setBlock(targetPos, lightState, 3);
        }

        this.auraLightPos = targetPos.immutable();
    }

    private boolean canHostAuraLight(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || isAuraLight(state);
    }

    private boolean isAuraLight(BlockState state) {
        return state.is(Blocks.LIGHT);
    }

    private void removeAuraLight(ServerLevel level) {
        if (this.auraLightPos == null) return;
        clearAuraLightIfOwned(level, this.auraLightPos);
        this.auraLightPos = null;
        this.auraLightLevel = 0;
    }

    private void clearAuraLightIfOwned(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (isAuraLight(state) && state.getValue(LightBlock.LEVEL) <= AURA_LIGHT_LEVEL) {
            level.removeBlock(pos, false);
        }
    }

    private int approach(int current, int target, int step) {
        if (current < target) return Math.min(target, current + step);
        if (current > target) return Math.max(target, current - step);
        return current;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            removeAuraLight(serverLevel);
        }
        super.remove(reason);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base_controller", 5, this::walkPredicate));
        controllers.add(new AnimationController<>(this, "skill_controller", 5, this::skillPredicate));
        controllers.add(new AnimationController<>(this, "evasion_controller", 5, this::evasionPredicate));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, this::attackPredicate));
    }

    private <T extends GeoAnimatable> PlayState walkPredicate(AnimationState<T> event) {
        DBSagasEntity entity = (DBSagasEntity) event.getAnimatable();

        if (this.isEvading() || this.isCasting() || this.isComboing()) {
            return PlayState.STOP;
        }

        int style = entity.getDBZStyle();

        if (entity.isFlying()) {
            double speedSqr = entity.getDeltaMovement().x * entity.getDeltaMovement().x + entity.getDeltaMovement().z * entity.getDeltaMovement().z;

            if (speedSqr > 0.05D) {
                return event.setAndContinue(ANIM_FLY_FAST);
            }
            return event.setAndContinue(ANIM_FLY);
        }

        if (event.isMoving()) {
            if (entity.isAggressive() || entity.getTarget() != null) {
                if (style == 1) return event.setAndContinue(ANIM_RUN_2);
                if (style == 2) return event.setAndContinue(ANIM_RUN_3);
                return event.setAndContinue(ANIM_RUN);
            } else {
                if (style == 1) return event.setAndContinue(ANIM_WALK_2);
                if (style == 2) return event.setAndContinue(ANIM_WALK_3);
                return event.setAndContinue(ANIM_WALK);
            }
        }

        if (style == 1) return event.setAndContinue(ANIM_IDLE_2);
        if (style == 2) return event.setAndContinue(ANIM_IDLE_3);
        return event.setAndContinue(ANIM_IDLE);
    }

    private <T extends GeoAnimatable> PlayState skillPredicate(AnimationState<T> event) {
        DBSagasEntity entity = (DBSagasEntity) event.getAnimatable();

        if (entity.isComboing()) {
            int comboId = entity.entityData.get(CURRENT_COMBO_ID);
            if (comboId == 0) return event.setAndContinue(ANIM_COMBO1);
            if (comboId == 1) return event.setAndContinue(ANIM_COMBO2);
        }

        if (entity.isCasting()) {
            int skill = entity.getSkillType();
            switch (skill) {
                case 1: return event.setAndContinue(ANIM_KI_KAME);
                case 2: return event.setAndContinue(ANIM_KI_GALICK);
                case 3: return event.setAndContinue(ANIM_KI_MAKKAKO);
                case 4: return event.setAndContinue(ANIM_KI_LASER);
                case 5: return event.setAndContinue(ANIM_KI_EXPLOSION);
                case 6: return event.setAndContinue(ANIM_KI_BARRIER);
                case 7: return event.setAndContinue(ANIM_KI_EXPLOSION);
                case 8: return event.setAndContinue(ANIM_KIWAVE);
                default: return event.setAndContinue(ANIM_KIWAVE);
            }
        }

        if (entity.isTransforming()) {
            int style = entity.getDBZStyle();
            if (style == 1) return event.setAndContinue(ANIM_TRANSFORMATION2);
            if (style == 2) return event.setAndContinue(ANIM_TRANSFORMATION3);
            return event.setAndContinue(ANIM_TRANSFORMATION1);
        }

        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }

    private <T extends GeoAnimatable> PlayState attackPredicate(AnimationState<T> event) {
        DBSagasEntity entity = (DBSagasEntity) event.getAnimatable();

        if (isCasting() || isTransforming() || isComboing() || isEvading()) {
            return PlayState.STOP;
        }

        int style = entity.getDBZStyle();

        if (entity.swingTime > 0 && !isAttacking) {
            isAttacking = true;
            event.getController().forceAnimationReset();

            int randAttack = this.random.nextInt(3);
            if (style == 1) {
                if (randAttack == 0) event.getController().setAnimation(ANIM_ATTACK1_2);
                else if (randAttack == 1) event.getController().setAnimation(ANIM_ATTACK2_2);
                else event.getController().setAnimation(ANIM_ATTACK3_2);
            } else if (style == 2) {
                if (randAttack == 0) event.getController().setAnimation(ANIM_ATTACK1_3);
                else if (randAttack == 1) event.getController().setAnimation(ANIM_ATTACK2_3);
                else event.getController().setAnimation(ANIM_ATTACK3_3);
            } else {
                if (randAttack == 0) event.getController().setAnimation(ANIM_ATTACK1);
                else if (randAttack == 1) event.getController().setAnimation(ANIM_ATTACK2);
                else event.getController().setAnimation(ANIM_ATTACK3);
            }
            return PlayState.CONTINUE;
        }

        if (isAttacking) {
            if (event.getController().getAnimationState() == AnimationController.State.STOPPED) {
                isAttacking = false;
                return PlayState.STOP;
            }
            return PlayState.CONTINUE;
        }

        return PlayState.STOP;
    }

    private <T extends GeoAnimatable> PlayState evasionPredicate(AnimationState<T> event) {
        if (this.isEvading()) {
            return event.setAndContinue(ANIM_EVADE);
        }

        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }

    @Override
    public void travel(Vec3 pTravelVector) {
        if (this.isCasting() || this.isComboing() || this.isTransforming()) {
            this.setDeltaMovement(0, 0, 0);
            super.travel(Vec3.ZERO);
            return;
        }

        if (this.isEffectiveAi() && this.isInWater()) {
            float waterSpeed = 0.15F;

            this.moveRelative(waterSpeed, pTravelVector);
            this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());

            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));

            if (this.getTarget() != null && this.getTarget().getY() > this.getY()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.02D, 0.0D));
            }
        } else {
            super.travel(pTravelVector);
        }
    }

    public void setCasting(boolean casting) {
        this.entityData.set(IS_CASTING, casting);
    }

    public boolean isCasting() {
        return this.entityData.get(IS_CASTING);
    }

    public void setFlying(boolean flying) {
        this.entityData.set(IS_FLYING, flying);
    }

    public boolean isFlying() {
        return this.entityData.get(IS_FLYING);
    }

    public int getSkillType() {
        return this.entityData.get(SKILL_TYPE);
    }

    public void setSkillType(int type) {
        this.entityData.set(SKILL_TYPE, type);
    }

    public int getBattlePower() {
        return this.entityData.get(BATTLE_POWER);
    }

    public void setBattlePower(int type) {
        this.entityData.set(BATTLE_POWER, type);
    }

    public int getAuraColor() {
        return this.entityData.get(AURA_COLOR);
    }

    public void setAuraColor(int color) {
        this.entityData.set(AURA_COLOR, color);
    }

    public String getAuraType() {
        return this.entityData.get(AURA_TYPE);
    }

    public void setAuraType(String type) {
        this.entityData.set(AURA_TYPE, type);
    }

    public boolean isTransforming() {
        return this.entityData.get(TRANSFORMING);
    }

    public void setTransforming(boolean transforming) {
        this.entityData.set(TRANSFORMING, transforming);
    }

    public boolean isCharge() {
        return this.entityData.get(KI_CHARGE);
    }

    public void setKiCharge(boolean charge) {
        this.entityData.set(KI_CHARGE, charge);
    }

    public boolean isLightning() {
        return this.entityData.get(IS_LIGHTNING);
    }

    public void setLightning(boolean active) {
        this.entityData.set(IS_LIGHTNING, active);
    }

    public int getLightningColor() {
        return this.entityData.get(LIGHTNING_COLOR);
    }

    public void setLightningColor(int color) {
        this.entityData.set(LIGHTNING_COLOR, color);
    }

    public void setEvading(boolean evading) {
        this.entityData.set(IS_EVADING, evading);
    }

    public boolean isEvading() {
        return this.entityData.get(IS_EVADING);
    }

    public void setComboing(boolean comboing) {
        this.entityData.set(IS_COMBOING, comboing);
    }

    public boolean isComboing() {
        return this.entityData.get(IS_COMBOING);
    }

    private void stopCombo() {
        this.setComboing(false);
        this.comboTimer = 0;
        this.comboTarget = null;
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }

    public boolean isBattleDamaged() {
        return this.getHealth() <= this.getMaxHealth() / 2.0F;
    }

    private void performEvasion() {
        this.setEvading(true);
        this.evasionStateTicks = 0;
        this.currentEvadeTimer = this.evadeCooldownMax;

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 1, this.getZ(), 5, 0.2, 0.1, 0.2, 0.1);
        }

        Vec3 look = this.getLookAngle();
        this.setDeltaMovement(new Vec3(-look.x * 1.5, 0.3, -look.z * 1.5));
        this.playSound(MainSounds.TP.get(), 1.0F, 1.2F);
    }

    private void handleComboLogic() {
        if (comboTarget == null || !comboTarget.isAlive()) {
            this.stopCombo();
            return;
        }

        this.getNavigation().stop();
        this.lookAt(comboTarget, 360, 360);

        int comboId = this.entityData.get(CURRENT_COMBO_ID);

        if (comboId == 0) {
            if (comboTimer == 1) {
                Vec3 targetLook = comboTarget.getLookAngle().normalize();
                double posX = comboTarget.getX() + (targetLook.x * 1.5);
                double posZ = comboTarget.getZ() + (targetLook.z * 1.5);
                this.teleportTo(posX, comboTarget.getY(), posZ);
                this.playSound(MainSounds.TP.get(), 1.0F, 1.0F);

                Vec3 dashDir = comboTarget.position().subtract(this.position()).normalize().scale(1.2);
                this.setDeltaMovement(dashDir);

                comboTarget.invulnerableTime = 0;
                comboTarget.hurt(this.damageSources().mobAttack(this), 3.0F);
                this.playSound(MainSounds.CRITICO1.get(), 0.7F, 1.4F);
                spawnPunchParticles(comboTarget);
            }

            if (comboTimer == 12) {
                Vec3 targetLook = comboTarget.getLookAngle().normalize();
                double posX = comboTarget.getX() - (targetLook.x * 1.5);
                double posZ = comboTarget.getZ() - (targetLook.z * 1.5);
                this.teleportTo(posX, comboTarget.getY(), posZ);
                this.playSound(MainSounds.TP.get(), 1.0F, 1.2F);

                Vec3 dashDir = comboTarget.position().subtract(this.position()).normalize().scale(1.2);
                this.setDeltaMovement(dashDir);

                comboTarget.invulnerableTime = 0;
                comboTarget.hurt(this.damageSources().mobAttack(this), 4.0F);
                this.playSound(MainSounds.CRITICO1.get(), 0.8F, 1.5F);
                spawnPunchParticles(comboTarget);
            }

            if (comboTimer == 22) {
                Vec3 targetLook = comboTarget.getLookAngle().normalize();
                double posX = comboTarget.getX() + (targetLook.x * 1.5);
                double posZ = comboTarget.getZ() + (targetLook.z * 1.5);
                this.teleportTo(posX, comboTarget.getY() + 0.5, posZ);
                this.playSound(MainSounds.TP.get(), 1.0F, 1.1F);

                this.setDeltaMovement(0, 0, 0);
            }

            if (comboTimer == 31) {
                Vec3 dashDir = comboTarget.position().subtract(this.position()).normalize().scale(2.0);
                this.setDeltaMovement(dashDir);

                comboTarget.invulnerableTime = 0;
                comboTarget.hurt(this.damageSources().mobAttack(this), 10.0F);
                this.playSound(MainSounds.CRITICO1.get(), 1.0F, 0.8F);
                spawnPunchParticles(comboTarget);

                Vec3 pushDir = comboTarget.position().subtract(this.position()).normalize();
                comboTarget.setDeltaMovement(pushDir.x * 2.5, 0.6, pushDir.z * 2.5);
                comboTarget.hasImpulse = true;

                this.stopCombo();
            }
        } else if (comboId == 1) {
            if (comboTimer == 1) {
                Vec3 targetLook = comboTarget.getLookAngle().normalize();
                double posX = comboTarget.getX() + (targetLook.x * 1.5);
                double posZ = comboTarget.getZ() + (targetLook.z * 1.5);
                this.teleportTo(posX, comboTarget.getY(), posZ);
                this.playSound(MainSounds.TP.get(), 1.0F, 1.0F);

                comboTarget.invulnerableTime = 0;
                comboTarget.hurt(this.damageSources().mobAttack(this), 4.0F);
                this.playSound(MainSounds.CRITICO1.get(), 0.8F, 1.2F);
                spawnPunchParticles(comboTarget);

                comboTarget.setDeltaMovement(0, 1.2D, 0);
                comboTarget.hasImpulse = true;
            }

            if (comboTimer == 12) {
                Vec3 targetLook = comboTarget.getLookAngle().normalize();

                double destX = comboTarget.getX() + (targetLook.x * 0.5);
                double destY = comboTarget.getY() + 2.5D;
                double destZ = comboTarget.getZ() + (targetLook.z * 0.5);

                this.moveTo(destX, destY, destZ);
                this.playSound(MainSounds.TP.get(), 1.0F, 1.3F);
                this.setDeltaMovement(0, 0, 0);
                this.lookAt(comboTarget, 360, 360);
            }

            if (comboTimer == 20) {
                comboTarget.invulnerableTime = 0;
                comboTarget.hurt(this.damageSources().mobAttack(this), 12.0F);
                this.playSound(MainSounds.CRITICO1.get(), 1.0F, 0.8F);
                spawnPunchParticles(comboTarget);

                comboTarget.setDeltaMovement(0, -2.5D, 0);
                comboTarget.hasImpulse = true;
            }

            if (comboTimer == 25) {
                comboTarget.addEffect(new MobEffectInstance(MainEffects.STUN.get(), 40, 0, false, false, true));

                this.stopCombo();
            }
        }
    }

    private void spawnPunchParticles(LivingEntity target) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(MainParticles.PUNCH_PARTICLE.get(), target.getX(),
                    target.getY() + (target.getBbHeight() / 2.0), target.getZ(), 0, 1.0f, 1.0f, 1.0f, 1.0);
        }
    }

    public void performTeleport(LivingEntity target) {
        Vec3 targetLook = target.getLookAngle().normalize();

        double distanceBehind = 1.5D;
        double destX = target.getX() - (targetLook.x * distanceBehind);
        double destZ = target.getZ() - (targetLook.z * distanceBehind);
        double destY = target.getY();

        this.teleportTo(destX, destY, destZ);

        this.playSound(MainSounds.TP.get(), 1.0F, 1.0F);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, destX, destY + 1, destZ, 10, 0.2, 0.1, 0.2, 0.1);
        }

        this.lookAt(target, 360, 360);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putDouble("RoarDamage", this.roarDamage);
        pCompound.putDouble("RoarRange", this.roarRange);
        pCompound.putDouble("FlySpeed", this.flySpeed);
        pCompound.putFloat("KiBlastDamage", this.kiBlastDamage);
        pCompound.putFloat("KiBlastSpeed", this.kiBlastSpeed);
        pCompound.putString("AuraType", this.getAuraType());
        pCompound.putInt("DBZStyle", this.getDBZStyle());
        pCompound.putBoolean("CanFly", this.canFly());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("RoarDamage")) this.roarDamage = pCompound.getDouble("RoarDamage");
        if (pCompound.contains("RoarRange")) this.roarRange = pCompound.getDouble("RoarRange");
        if (pCompound.contains("FlySpeed")) this.flySpeed = pCompound.getDouble("FlySpeed");
        if (pCompound.contains("KiBlastDamage")) this.kiBlastDamage = pCompound.getFloat("KiBlastDamage");
        if (pCompound.contains("KiBlastSpeed")) this.kiBlastSpeed = pCompound.getFloat("KiBlastSpeed");
        if (pCompound.contains("AuraType")) this.setAuraType(pCompound.getString("AuraType"));
        if (pCompound.contains("DBZStyle")) this.setDBZStyle(pCompound.getInt("DBZStyle"));
        if (pCompound.contains("CanFly")) this.setCanFly(pCompound.getBoolean("CanFly"));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_CASTING, false);
        this.entityData.define(IS_FLYING, false);
        this.entityData.define(IS_FLYING_FAST, false);
        this.entityData.define(SKILL_TYPE, 0);
        this.entityData.define(BATTLE_POWER, 20);
        this.entityData.define(AURA_COLOR, 0xFFFFFF);
        this.entityData.define(AURA_TYPE, "kakarot");
        this.entityData.define(TRANSFORMING, false);
        this.entityData.define(KI_CHARGE, false);
        this.entityData.define(IS_LIGHTNING, false);
        this.entityData.define(LIGHTNING_COLOR, 0xFFFFFF);
        this.entityData.define(IS_EVADING, false);
        this.entityData.define(IS_COMBOING, false);
        this.entityData.define(CURRENT_COMBO_ID, -1);
        this.entityData.define(DBZ_STYLE, 0);
    }

    public double getRoarDamage() {
        return roarDamage;
    }

    public void setRoarDamage(double roarDamage) {
        this.roarDamage = roarDamage;
    }

    public double getRoarRange() {
        return roarRange;
    }

    public void setRoarRange(double roarRange) {
        this.roarRange = roarRange;
    }

    public float getKiBlastDamage() {
        return kiBlastDamage;
    }

    public void setKiBlastDamage(float kiBlastDamage) {
        this.kiBlastDamage = kiBlastDamage;
    }

    public double getFlySpeed() {
        return flySpeed;
    }

    public void setFlySpeed(double flySpeed) {
        this.flySpeed = flySpeed;
    }

    public float getKiBlastSpeed() {
        return kiBlastSpeed;
    }

    public void setKiBlastSpeed(float kiBlastSpeed) {
        this.kiBlastSpeed = kiBlastSpeed;
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (this.isTransforming()) {
            return false;
        }

        if (!this.level().isClientSide && pAmount >= this.getHealth()) {
            if (shouldTriggerTransformationOnDeath()) {
                this.setHealth(1.0F);
                this.startTransformation();
                return false;
            }
        }

        return super.hurt(pSource, pAmount);
    }

    @Override
    public boolean doHurtTarget(Entity pEntity) {
        if (this.isTransforming()) {
            return false;
        }

        if (this.isCasting() || this.isComboing()) {
            return false;
        }

        return super.doHurtTarget(pEntity);
    }

    protected void handleCommonCombatMovement(LivingEntity target, boolean isActionActive) {
        if (this.level().isClientSide) return;

        if (isActionActive) {
            this.getNavigation().stop();
            this.setDeltaMovement(0, 0, 0);
            if (target != null) rotateBodyToTarget(target);
            return;
        }

        if (target != null && target.isAlive()) {
            double yDiff = target.getY() - this.getY();

            double horizontalDistSqr = this.distanceToSqr(target.getX(), this.getY(), target.getZ());

            if (this.canFly() && (yDiff > 2.0D || horizontalDistSqr > 64.0D) && !isFlying()) {
                setFlying(true);
            } else if (isFlying()) {
                if (!this.canFly() || (yDiff <= 1.0D && horizontalDistSqr <= 64.0D && this.onGround())) {
                    setFlying(false);
                    this.setFlyingFast(false);
                    this.setNoGravity(false);
                }
            }
        } else {
            if (this.onGround() && isFlying()) {
                setFlying(false);
                this.setFlyingFast(false);
                this.setNoGravity(false);
            }
        }

        if (this.isFlying()) {
            this.setNoGravity(true);
            if (target != null) {
                moveTowardsTargetInAir(target);
                rotateBodyToTarget(target);
            } else {
                this.setDeltaMovement(this.getDeltaMovement().add(0, -0.01D, 0));
            }
        } else {
            this.setNoGravity(false);
        }
    }

    public void rotateBodyToTarget(LivingEntity target) {
        double d0 = target.getX() - this.getX();
        double d2 = target.getZ() - this.getZ();
        float targetYaw = (float) (Mth.atan2(d2, d0) * (double) (180F / (float) Math.PI)) - 90.0F;
        this.setYRot(targetYaw);
        this.setYBodyRot(targetYaw);
        this.setYHeadRot(targetYaw);
    }

    public void moveTowardsTargetInAir(LivingEntity target) {
        if (this.isCasting() || this.isComboing() || this.isEvading()) return;
        double flyspeed = this.getFlySpeed();

        double distance = this.distanceTo(target);
        if (distance > 8.0D) {
            if (!this.isFlyingFast()) this.setFlyingFast(true);
        } else if (distance < 4.0D) {
            if (this.isFlyingFast()) this.setFlyingFast(false);
        }

        if (this.isFlyingFast()) {
            flyspeed *= 2.0D;
        }

        double dx = target.getX() - this.getX();
        double dy = (target.getY() + 1.0D) - this.getY();
        double dz = target.getZ() - this.getZ();
        double dist3D = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist3D < 1.0) return;
        Vec3 movement = new Vec3(dx / dist3D * flyspeed, dy / dist3D * flyspeed, dz / dist3D * flyspeed);
        double gravityDrag = (dy < -0.5) ? -0.05D : -0.03D;
        this.setDeltaMovement(movement.add(0, gravityDrag, 0));
    }

    public void startCasting(int type) {
        this.setCasting(true);
        this.setSkillType(type);
        this.castTimer = 0;

        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0D);
        this.getNavigation().stop();
        this.setDeltaMovement(0, 0, 0);
    }

    public void stopCasting() {
        this.setCasting(false);
        this.castTimer = 0;
        this.setSkillType(0);

        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25D);
    }

    protected boolean handleTransformationLogic(int transformTick, int duration) {
        this.getNavigation().stop();
        this.setDeltaMovement(0, 0, 0);

        if (this.isCasting()) this.stopCasting();

        return transformTick >= duration;
    }

    protected void finishTransformationSpawn(DBSagasEntity newEntity, boolean fullHealth) {
        if (this.level().isClientSide || newEntity == null) return;

        Level level = this.level();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1, this.getZ(), 1, 0, 0, 0, 0);

            newEntity.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            newEntity.setTarget(this.getTarget());

            if (fullHealth) {
                newEntity.setHealth(newEntity.getMaxHealth());
            } else {
                newEntity.setHealth(this.getHealth());
            }

            newEntity.setKiBlastDamage(this.getKiBlastDamage() * 1.5F);
            if (newEntity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                newEntity.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(this.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() * 1.5D);
            }

            if (this.getPersistentData().contains("dmz_is_hardmode")) {
                boolean isHardMode = this.getPersistentData().getBoolean("dmz_is_hardmode");
                newEntity.getPersistentData().putBoolean("dmz_is_hardmode", isHardMode);
            }

            if (this.getPersistentData().contains("dmz_quest_owner")) {
                String questOwner = this.getPersistentData().getString("dmz_quest_owner");
                newEntity.getPersistentData().putString("dmz_quest_owner", questOwner);
            }

            level.addFreshEntity(newEntity);
            this.discard();
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor pLevel, MobSpawnType reason) {
        return true;
    }

    public static boolean canSpawnHere(EntityType<? extends DBSagasEntity> entity, ServerLevelAccessor world, MobSpawnType spawn, BlockPos pos, RandomSource random) {
        if (world.getDifficulty() == Difficulty.PEACEFUL) return false;
        if (random.nextFloat() < 0.95f) return false;
        boolean solidGround = world.getBlockState(pos.below()).isSolidRender(world, pos.below());
        boolean noCollision = world.isUnobstructed(world.getBlockState(pos), pos, CollisionContext.empty());
        return solidGround && noCollision;
    }

    protected boolean shouldTriggerTransformationOnDeath() {
        return false;
    }

    protected void startTransformation() {
        this.setTransforming(true);
        this.playSound(MainSounds.KI_CHARGE_LOOP.get(), 1.0F, 1.2F);
    }
}