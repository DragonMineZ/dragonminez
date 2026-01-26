package com.dragonminez.common.init.entities.namek;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.init.entities.goals.VillageAlertSystem;
import com.dragonminez.common.init.entities.redribbon.RedRibbonEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class NamekWarriorEntity extends PathfinderMob implements GeoEntity {

    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);
    private boolean isAttacking = false;

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(NamekWarriorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_FLYING = SynchedEntityData.defineId(NamekWarriorEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int VARIANT_COUNT = 4;

    public static final ResourceLocation[] TEXTURES = new ResourceLocation[VARIANT_COUNT];

    static {
        for (int i = 0; i < VARIANT_COUNT; i++) {
            TEXTURES[i] = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/enemies/namek_warrior_" + i + ".png");
        }
    }

    public NamekWarriorEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        VillageAlertSystem.registerWarrior(this);
        this.setPersistenceRequired();
		if (this instanceof IBattlePower bp) {
			bp.setBattlePower(750);
		}
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.20D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.6D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 15.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        //FALTA EL GOAL DE DETECTAR EL ALINEAMIENTO
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                (entity) -> {
                    return !(entity instanceof NamekWarriorEntity);
                }
        ));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, 0);
        this.entityData.define(IS_FLYING, false);

    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("Variant", getVariantNamek());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        setVariantNamek(pCompound.getInt("Variant"));
    }

    public int getVariantNamek() {return this.entityData.get(VARIANT);}
    public void setVariantNamek(int variant) {this.entityData.set(VARIANT, variant);}

    public void setFlying(boolean flying) { this.entityData.set(IS_FLYING, flying); }
    public boolean isFlying() { return this.entityData.get(IS_FLYING); }


    public ResourceLocation getCurrentTexture() {
        int variant = getVariantNamek();
        if (variant < 0 || variant >= TEXTURES.length) {
            variant = 0;
        }
        return TEXTURES[variant];
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        this.setVariantNamek(this.random.nextInt(VARIANT_COUNT));
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        VillageAlertSystem.unregisterWarrior(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base_controller", 5, this::walkPredicate));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, this::attackPredicate));
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity target = this.getTarget();

        if (target != null && target.isAlive()) {
            if (this.isFlying()) {
                rotateBodyToTarget(target);
            }
        }

        if (!this.level().isClientSide) {
            if (target != null && target.isAlive()) {
                double yDiff = target.getY() - this.getY();
                if (yDiff > 2.0D) {
                    if (!isFlying()) setFlying(true);
                } else if (yDiff <= 1.0D && this.onGround()) {
                    if (isFlying()) {
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
                } else {
                    this.setDeltaMovement(this.getDeltaMovement().add(0, -0.03D, 0));
                }
            } else {
                this.setNoGravity(false);
            }
        }
    }
    private void rotateBodyToTarget(LivingEntity target) {
        double d0 = target.getX() - this.getX();
        double d2 = target.getZ() - this.getZ();
        float targetYaw = (float)(Mth.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F;
        this.setYRot(targetYaw);
        this.setYBodyRot(targetYaw);
        this.setYHeadRot(targetYaw);
        this.yRotO = targetYaw;
        this.yBodyRotO = targetYaw;
        this.yHeadRotO = targetYaw;
    }
    private void moveTowardsTargetInAir(LivingEntity target) {
        double flyspeed = 0.35;
        double dx = target.getX() - this.getX();
        double dy = (target.getY() + 1.0D) - this.getY();
        double dz = target.getZ() - this.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance < 1.0) return;
        Vec3 movement = new Vec3(dx / distance * flyspeed, dy / distance * flyspeed, dz / distance * flyspeed);
        double gravityDrag = (dy < -0.5) ? -0.05D : -0.03D;
        this.setDeltaMovement(movement.add(0, gravityDrag, 0));
    }

    private <T extends GeoAnimatable> PlayState walkPredicate(software.bernie.geckolib.core.animation.AnimationState<T> event) {
        NamekWarriorEntity entity = (NamekWarriorEntity) event.getAnimatable();

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
        NamekWarriorEntity entity = (NamekWarriorEntity) event.getAnimatable();

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
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
