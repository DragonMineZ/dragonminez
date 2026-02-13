package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainSounds;
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

public class DBSagasEntity extends Monster implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_CASTING = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_FLYING = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SKILL_TYPE = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AURA_COLOR = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> BATTLE_POWER = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TRANSFORMING = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> KI_CHARGE = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> IS_LIGHTNING = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> LIGHTNING_COLOR = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.INT);

    private double roarDamage = 50.0D;
    private double roarRange = 15.0D;
    private double flySpeed = 0.45D;
    private float kiBlastDamage = 20.0F;
    private float kiBlastSpeed = 0.6F;
    protected int teleportCooldown = 0;
    protected int castTimer = 0;
    private int chargeSoundTimer = 0;

    private boolean isAttacking = false;


    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);

    protected DBSagasEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.8D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 45.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

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
            if (this.teleportCooldown > 0) this.teleportCooldown--;

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
                    float pitch = 0.9F + this.random.nextFloat() * 0.2F; // Variación ligera de tono
                    this.playSound(MainSounds.KI_SPARKS.get(), 0.3F, pitch);
                }
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base_controller", 5, this::walkPredicate));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, this::attackPredicate));

    }

    private <T extends GeoAnimatable> PlayState walkPredicate(AnimationState<T> event) {
        DBSagasEntity entity = (DBSagasEntity) event.getAnimatable();

        if (isCasting()) {
            return PlayState.STOP;
        }
        if (entity.isFlying()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("fly"));
            return PlayState.CONTINUE;
        }

        if (event.isMoving()) {
            if (entity.isAggressive() || entity.getTarget() != null) {
                event.getController().setAnimation(RawAnimation.begin().thenLoop("run"));
            } else {
                event.getController().setAnimation(RawAnimation.begin().thenLoop("walk"));
            }
            return PlayState.CONTINUE;
        }

        event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }

    private <T extends GeoAnimatable> PlayState attackPredicate(AnimationState<T> event) {
        DBSagasEntity entity = (DBSagasEntity) event.getAnimatable();

        if (isCasting()) {
            return PlayState.STOP;
        }
        if (isTransforming()) {
            return PlayState.STOP;
        }

        if (entity.swingTime > 0 && !isAttacking) {
            isAttacking = true;
            event.getController().forceAnimationReset();
            if (this.random.nextBoolean()) {
                event.getController().setAnimation(RawAnimation.begin().thenPlay("attack1"));
            } else {
                event.getController().setAnimation(RawAnimation.begin().thenPlay("attack2"));
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

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_CASTING, false);
        this.entityData.define(IS_FLYING, false);
        this.entityData.define(SKILL_TYPE, 0);
        this.entityData.define(BATTLE_POWER, 20);
        this.entityData.define(AURA_COLOR, 0xFFFFFF);
        this.entityData.define(TRANSFORMING, false);
        this.entityData.define(KI_CHARGE, false);
        this.entityData.define(IS_LIGHTNING, false);
        this.entityData.define(LIGHTNING_COLOR, 0xFFFFFF);
    }

    public void setCasting(boolean casting) { this.entityData.set(IS_CASTING, casting); }
    public boolean isCasting() { return this.entityData.get(IS_CASTING); }

    public void setFlying(boolean flying) { this.entityData.set(IS_FLYING, flying); }
    public boolean isFlying() { return this.entityData.get(IS_FLYING); }

    public int getSkillType() { return this.entityData.get(SKILL_TYPE); }
    public void setSkillType(int type) { this.entityData.set(SKILL_TYPE, type); }

    public int getBattlePower() { return this.entityData.get(BATTLE_POWER); }
    public void setBattlePower(int type) { this.entityData.set(BATTLE_POWER, type); }

    public int getAuraColor() { return this.entityData.get(AURA_COLOR); }
    public void setAuraColor(int color) { this.entityData.set(AURA_COLOR, color); }

    public boolean isTransforming() {return this.entityData.get(TRANSFORMING);}
    public void setTransforming(boolean transforming) {this.entityData.set(TRANSFORMING, transforming);}

    public boolean isCharge() {return this.entityData.get(KI_CHARGE);}
    public void setKiCharge(boolean charge) {this.entityData.set(KI_CHARGE, charge);}

    public boolean isLightning() { return this.entityData.get(IS_LIGHTNING); }
    public void setLightning(boolean active) { this.entityData.set(IS_LIGHTNING, active); }

    public int getLightningColor() { return this.entityData.get(LIGHTNING_COLOR); }
    public void setLightningColor(int color) { this.entityData.set(LIGHTNING_COLOR, color); }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }
    public boolean isBattleDamaged() {
        return this.getHealth() <= this.getMaxHealth() / 2.0F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putDouble("RoarDamage", this.roarDamage);
        pCompound.putDouble("RoarRange", this.roarRange);
        pCompound.putDouble("FlySpeed", this.flySpeed);
        pCompound.putFloat("KiBlastDamage", this.kiBlastDamage);
        pCompound.putFloat("KiBlastSpeed", this.kiBlastSpeed);
    }
    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("RoarDamage")) this.roarDamage = pCompound.getDouble("RoarDamage");
        if (pCompound.contains("RoarRange")) this.roarRange = pCompound.getDouble("RoarRange");
        if (pCompound.contains("FlySpeed")) this.flySpeed = pCompound.getDouble("FlySpeed");
        if (pCompound.contains("KiBlastDamage")) this.kiBlastDamage = pCompound.getFloat("KiBlastDamage");
        if (pCompound.contains("KiBlastSpeed")) this.kiBlastSpeed = pCompound.getFloat("KiBlastSpeed");
    }

    public double getRoarDamage() { return roarDamage;}
    public void setRoarDamage(double roarDamage) {this.roarDamage = roarDamage;}

    public double getRoarRange() {return roarRange;}
    public void setRoarRange(double roarRange) {this.roarRange = roarRange;}

    public float getKiBlastDamage() {return kiBlastDamage;}
    public void setKiBlastDamage(float kiBlastDamage) {this.kiBlastDamage = kiBlastDamage;}

    public double getFlySpeed() {return flySpeed;}
    public void setFlySpeed(double flySpeed) {this.flySpeed = flySpeed;}

    public float getKiBlastSpeed() {return kiBlastSpeed;}
    public void setKiBlastSpeed(float kiBlastSpeed) {this.kiBlastSpeed = kiBlastSpeed;}

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (this.isTransforming()) {
            return false;
        }

        return super.hurt(pSource, pAmount);
    }

    @Override
    public boolean doHurtTarget(Entity pEntity) {
        if (this.isTransforming()) {
            return false;
        }

        if (this.isCasting()) {
            return false;
        }

        return super.doHurtTarget(pEntity);
    }

    /**
     * @param target El objetivo.
     * @param isActionActive Si está casteando/transformando (se detiene).
     * @param canFly Si esta entidad tiene la capacidad de volar.
     */
    protected void handleCommonCombatMovement(LivingEntity target, boolean isActionActive, boolean canFly) {
        if (this.level().isClientSide) return;

        if (isActionActive) {
            this.getNavigation().stop();
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.5, 0.5));
            if (target != null) rotateBodyToTarget(target);
            return;
        }

        if (target != null && target.isAlive()) {
            double yDiff = target.getY() - this.getY();

            if (canFly && yDiff > 2.0D && !isFlying()) {
                setFlying(true);
            }
            else if (isFlying()) {
                if (!canFly || (yDiff <= 1.0D && this.onGround())) {
                    setFlying(false);
                    this.setNoGravity(false);
                }
            }
        } else {
            if (this.onGround() && isFlying()) {
                setFlying(false);
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
        float targetYaw = (float)(Mth.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F;
        this.setYRot(targetYaw);
        this.setYBodyRot(targetYaw);
        this.setYHeadRot(targetYaw);
    }

    public void moveTowardsTargetInAir(LivingEntity target) {
        if (this.isCasting()) return;
        double flyspeed = this.getFlySpeed();
        double dx = target.getX() - this.getX();
        double dy = (target.getY() + 1.0D) - this.getY();
        double dz = target.getZ() - this.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance < 1.0) return;
        Vec3 movement = new Vec3(dx / distance * flyspeed, dy / distance * flyspeed, dz / distance * flyspeed);
        double gravityDrag = (dy < -0.5) ? -0.05D : -0.03D;
        this.setDeltaMovement(movement.add(0, gravityDrag, 0));
    }

    public void performTeleport(LivingEntity target) {
        Vec3 targetLook = target.getLookAngle().normalize();

        double distanceBehind = 0.7D;
        double destX = target.getX() - (targetLook.x * distanceBehind);
        double destZ = target.getZ() - (targetLook.z * distanceBehind);
        double destY = target.getY();

        this.teleportTo(destX, destY, destZ);

        this.playSound(MainSounds.TP.get(), 1.0F, 1.0F);

        this.teleportCooldown = 8 * 20;

        this.lookAt(target, 360, 360);
    }

    public void startCasting(int type) {
        this.setCasting(true);
        this.setSkillType(type);
        this.castTimer = 0;

        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0D);
        this.getNavigation().stop();
        this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
    }

    public void stopCasting() {
        this.setCasting(false);
        this.castTimer = 0;
        this.setSkillType(0);

        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25D);
    }

    public void shootGenericKiBlast(LivingEntity target, float size, int colorCore, int colorBorder, float speedMod) {
        if (target == null) return;

        KiBlastEntity kiBlast = new KiBlastEntity(this.level(), this);

        double sx = this.getX();
        double sy = this.getY() + 1.0D;
        double sz = this.getZ();

        kiBlast.setPos(sx, sy, sz);
        kiBlast.setColors(colorCore, colorBorder);
        kiBlast.setSize(size);
        kiBlast.setKiDamage(getKiBlastDamage());
        kiBlast.setOwner(this);

        double tx = target.getX() - sx;
        double ty = (target.getY() + target.getEyeHeight() * 0.5D) - sy;
        double tz = target.getZ() - sz;

        kiBlast.shoot(tx, ty, tz, speedMod, 1.0F);

        this.level().addFreshEntity(kiBlast);
    }

    public void shootGenericKiLaser(LivingEntity target, float speed, int colorCore, int colorBorder) {
        if (target == null) return;

        KiLaserEntity laser = new KiLaserEntity(this.level(), this);

        double sx = this.getX();
        double sy = this.getEyeY() - 0.2D;
        double sz = this.getZ();

        laser.setPos(sx, sy, sz);
        laser.setColors(colorCore, colorBorder);
        laser.setKiDamage(getKiBlastDamage());
        laser.setKiSpeed(speed);

        //laser.shoot(tx, ty, tz, speed, 0.5F);

        this.level().addFreshEntity(laser);
    }

    public void shootGenericKiWave(LivingEntity target, float size, int colorCore, int colorBorder, float speedMod) {
        if (target == null) return;

        KiWaveEntity wave = new KiWaveEntity(this.level(), this);

        double sx = this.getX();
        double sy = this.getY() + 1.0D;
        double sz = this.getZ();

        wave.setPos(sx, sy, sz);
        wave.setColors(colorCore, colorBorder);
        wave.setSize(size);
        wave.setKiDamage(getKiBlastDamage());
        wave.setOwner(this);
        wave.setKiSpeed(speedMod);

        this.level().addFreshEntity(wave);
    }

    public void shootGenericKiVolley(LivingEntity target, float speed, int colorCore, int colorBorder) {
        if (target == null) return;

        KiVolleyEntity.shootVolley(
                this,
                target,
                speed,
                this.getKiBlastDamage(),
                colorCore,
                colorBorder
        );
        this.playSound(MainSounds.KIBLAST_ATTACK.get(), 1.0F, 1.5F);
    }

    /**
     * Genera una explosión de Ki alrededor de la entidad.
     * @param damage Daño de la explosión.
     * @param colorCore Color central (Hex).
     * @param colorBorder Color borde (Hex).
     */
    public void performKiExplosion(float damage, int colorCore, int colorBorder) {
        if (this.level().isClientSide) return;

        KiExplosionEntity explosion = new KiExplosionEntity(this.level(), this);

        explosion.setupExplosion(
                this,
                damage,
                colorCore,
                colorBorder
        );

        this.level().addFreshEntity(explosion);
    }

    /**
     * Dispara un Ki Disc hacia donde está mirando la entidad.
     * @param size Tamaño del disco.
     * @param color Color del disco (Hex).
     * @param speed Velocidad del proyectil.
     */
    public void shootGenericKiDisc(float size, int color, float speed) {
        if (this.level().isClientSide) return;

        KiDiscEntity disc = new KiDiscEntity(this.level(), this);

        disc.setKiDamage(this.getKiBlastDamage());
        disc.setSize(size);
        disc.setColors(color, color);

        disc.shootFromRotation(this, this.getXRot(), this.getYRot(), 0.0F, speed, 1.0F);

        this.level().addFreshEntity(disc);
        this.playSound(MainSounds.KI_DISK_CHARGE.get(), 1.0F, 1.1F);
    }

    public void shootKiBarrier(int color, int colorborder) {
        if (this.level().isClientSide) return;

        KiBarrierEntity disc = new KiBarrierEntity(this.level(), this);

        disc.setColors(color, colorborder);


        this.level().addFreshEntity(disc);
        this.playSound(MainSounds.KI_EXPLOSION_IMPACT.get(), 1.0F, 1.1F);
    }


    /**
     * Maneja la lógica de "espera" durante la transformación (paralizar entidad, cancelar casteos).
     * @param transformTick Tick actual de la transformación.
     * @param duration Duración total en ticks (ej: 60).
     * @return true si el tiempo se ha cumplido y está listo para cambiar de forma.
     */
    protected boolean handleTransformationLogic(int transformTick, int duration) {
        this.getNavigation().stop();
        this.setDeltaMovement(0, 0, 0);

        if (this.isCasting()) this.stopCasting();


        return transformTick >= duration;
    }

    /**
     * Finaliza la transformación spawneando la nueva entidad.
     * @param newEntity La instancia ya creada de la nueva entidad
     */
    protected void finishTransformationSpawn(DBSagasEntity newEntity) {
        if (this.level().isClientSide || newEntity == null) return;

        Level level = this.level();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1, this.getZ(), 1, 0, 0, 0, 0);

            newEntity.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            newEntity.setTarget(this.getTarget());
            newEntity.setHealth(newEntity.getMaxHealth());

            if (this.getPersistentData().contains("dmz_is_hardmode")) {
                boolean isHardMode = this.getPersistentData().getBoolean("dmz_is_hardmode");
                newEntity.getPersistentData().putBoolean("dmz_is_hardmode", isHardMode);
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
}
