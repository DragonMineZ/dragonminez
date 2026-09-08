package com.dragonminez.common.init.entities.animal;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.ITextureVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
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

import java.util.EnumSet;

public class GiantFishEntity extends WaterAnimal implements GeoEntity, ITextureVariant {

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(GiantFishEntity.class, EntityDataSerializers.INT);
    
    private static final EntityDataAccessor<Boolean> HUNTING = SynchedEntityData.defineId(GiantFishEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int VARIANT_COUNT = 4;
    public static final int SPECIAL_VARIANT = VARIANT_COUNT - 1;
    private static final float SPECIAL_CHANCE = 0.03F;
    private static final int WATER_COLUMN_RADIUS = 1;
    private static final int MIN_SPAWN_DEPTH = 2;

    public static final ResourceLocation[] TEXTURES = new ResourceLocation[VARIANT_COUNT];

    static {
        for (int i = 0; i < VARIANT_COUNT; i++) {
            TEXTURES[i] = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/animal/giantfish_" + i + ".png");
        }
    }

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");

    private static final String ATTACK_CONTROLLER = "attack_controller";

    private static final int ATTACK_COOLDOWN = 20;

    private static final double HUNT_SPEED = 1.6D;
    private static final double HUNT_ANIM_SPEED = 1.5D;

    private static final int WATER_SEARCH_RADIUS = 8;
    private static final int WATER_SEARCH_HEIGHT = 4;

    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);

    public GiantFishEntity(EntityType<? extends WaterAnimal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        this.moveControl = new SmoothSwimmingMoveControl(this, 25, 10, 0.03F, 0.1F, false);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
    }

    public static AttributeSupplier createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.1D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .build();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LungeBiteGoal(this, HUNT_SPEED));
        this.goalSelector.addGoal(2, new RandomSwimmingGoal(this, 1.0D, 40));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
    }

    @Override
    protected PathNavigation createNavigation(Level pLevel) {
        return new WaterBoundPathNavigation(this, pLevel);
    }

    @Override
    public void travel(Vec3 pTravelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), pTravelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));

            if (!this.level().getFluidState(this.blockPosition().above()).is(FluidTags.WATER)) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.03D, 0.0D));
            }
        } else {
            super.travel(pTravelVector);
        }
    }

    @Override
    public boolean doHurtTarget(Entity pTarget) {
        boolean hurt = super.doHurtTarget(pTarget);

        if (hurt && !this.level().isClientSide) {
            triggerAnim(ATTACK_CONTROLLER, "attack");
        }
        return hurt;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        this.entityData.set(HUNTING, this.getTarget() != null);

        if (this.isInWater() || !this.onGround()) return;

        BlockPos water = findNearbyWater();

        if (water != null) {
            Vec3 dir = Vec3.atCenterOf(water).subtract(this.position()).normalize();
            this.setDeltaMovement(this.getDeltaMovement().add(dir.x * 0.4D, 0.5D, dir.z * 0.4D));
            this.setYRot((float) (Mth.atan2(dir.z, dir.x) * (180F / (float) Math.PI)) - 90.0F);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().add(
                    (this.random.nextFloat() * 2.0F - 1.0F) * 0.4F, 0.5D,
                    (this.random.nextFloat() * 2.0F - 1.0F) * 0.4F));
            this.setYRot(this.random.nextFloat() * 360.0F);
        }

        this.setOnGround(false);
        this.hasImpulse = true;
    }

    @Nullable
    private BlockPos findNearbyWater() {
        BlockPos origin = this.blockPosition();
        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-WATER_SEARCH_RADIUS, -WATER_SEARCH_HEIGHT, -WATER_SEARCH_RADIUS),
                origin.offset(WATER_SEARCH_RADIUS, WATER_SEARCH_HEIGHT, WATER_SEARCH_RADIUS))) {

            if (!this.level().getFluidState(pos).is(FluidTags.WATER)) continue;

            double dist = pos.distSqr(origin);
            if (dist < closestDist) {
                closestDist = dist;
                closest = pos.immutable();
            }
        }
        return closest;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(1.5D);
    }

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(VARIANT, variant);
    }

    @Override
    public int getTextureVariant() {
        return getVariant();
    }

    @Override
    public void setTextureVariant(int variant) {
        setVariant(variant);
    }

    public ResourceLocation getCurrentTexture() {
        int variant = getVariant();
        if (variant < 0 || variant >= TEXTURES.length) {
            variant = 0;
        }
        return TEXTURES[variant];
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, 0);
        this.entityData.define(HUNTING, false);
    }

    public boolean isHunting() {
        return this.entityData.get(HUNTING);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("Variant", getVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        setVariant(pCompound.getInt("Variant"));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason,
                                        @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        this.setVariant(this.random.nextFloat() < SPECIAL_CHANCE
                ? SPECIAL_VARIANT
                : this.random.nextInt(VARIANT_COUNT - 1));
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }
    public static boolean canSpawnHere(EntityType<GiantFishEntity> pType, ServerLevelAccessor pLevel, MobSpawnType pReason,
                                       BlockPos pPos, RandomSource pRandom) {
        if (pReason == MobSpawnType.SPAWNER) return true;
        if (pPos.getY() > pLevel.getSeaLevel() - MIN_SPAWN_DEPTH) return false;

        for (int dy = -WATER_COLUMN_RADIUS; dy <= WATER_COLUMN_RADIUS; dy++) {
            if (!pLevel.getFluidState(pPos.above(dy)).is(FluidTags.WATER)) return false;
        }
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base_controller", 5, this::swimPredicate)
                .setAnimationSpeedHandler(fish -> fish.isHunting() ? HUNT_ANIM_SPEED : 1.0D));
        controllers.add(new AnimationController<>(this, ATTACK_CONTROLLER, 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    private <T extends GeoAnimatable> PlayState swimPredicate(AnimationState<T> event) {
        if (event.isMoving()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("swim"));
            return PlayState.CONTINUE;
        }
        event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    static class LungeBiteGoal extends Goal {

        private static final int REPATH_INTERVAL = 10;

        private final GiantFishEntity fish;
        private final double speedModifier;
        private int cooldown;
        private int repathCooldown;

        LungeBiteGoal(GiantFishEntity fish, double speedModifier) {
            this.fish = fish;
            this.speedModifier = speedModifier;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.fish.getTarget();
            return target != null && target.isAlive() && this.fish.isInWater();
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
        public void stop() {
            this.cooldown = 0;
            this.repathCooldown = 0;
            this.fish.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = this.fish.getTarget();
            if (target == null) return;

            this.fish.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (--this.repathCooldown <= 0) {
                this.repathCooldown = REPATH_INTERVAL;
                this.fish.getNavigation().moveTo(target, this.speedModifier);
            }

            if (this.fish.getNavigation().isDone()) {
                Vec3 charge = target.position().subtract(this.fish.position()).normalize().scale(0.02D * this.speedModifier);
                this.fish.setDeltaMovement(this.fish.getDeltaMovement().add(charge));
            }

            if (this.cooldown > 0) this.cooldown--;

            double reach = this.fish.getBbWidth() * this.fish.getBbWidth() * 2.0D + target.getBbWidth();

            if (this.cooldown <= 0 && this.fish.distanceToSqr(target) <= reach) {
                this.cooldown = ATTACK_COOLDOWN;
                this.fish.doHurtTarget(target);
            }
        }
    }
}
