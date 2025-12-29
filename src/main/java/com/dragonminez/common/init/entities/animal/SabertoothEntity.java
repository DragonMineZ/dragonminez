package com.dragonminez.common.init.entities.animal;

import com.dragonminez.Reference;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class SabertoothEntity extends DinoGlobalEntity{

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(SabertoothEntity.class, EntityDataSerializers.INT);
    private boolean isAttacking = false;

    public static final int VARIANT_COUNT = 3;

    public static final ResourceLocation[] TEXTURES = new ResourceLocation[VARIANT_COUNT];

    static {
        for (int i = 0; i < VARIANT_COUNT; i++) {
            TEXTURES[i] = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/animal/sabertooth_" + i + ".png");
        }
    }

    public SabertoothEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("Variant", getVariantTiger());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        setVariantTiger(pCompound.getInt("Variant"));
    }

    public int getVariantTiger() {return this.entityData.get(VARIANT);}
    public void setVariantTiger(int variant) {this.entityData.set(VARIANT, variant);}

    public ResourceLocation getCurrentTexture() {
        int variant = getVariantTiger();
        if (variant < 0 || variant >= TEXTURES.length) {
            variant = 0;
        }
        return TEXTURES[variant];
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        this.setVariantTiger(this.random.nextInt(VARIANT_COUNT));
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

        controllers.add(new AnimationController<>(this, "base_controller", 5, this::walkPredicate));

        controllers.add(new AnimationController<>(this, "attack_controller", 0, this::attackPredicate));

        controllers.add(new AnimationController<>(this, "tail_controller", 0, this::tailPredicate));
    }

    private <T extends GeoAnimatable> PlayState walkPredicate(AnimationState<T> event) {
        SabertoothEntity entity = (SabertoothEntity) event.getAnimatable();

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
        DinoGlobalEntity entity = (DinoGlobalEntity) event.getAnimatable();

        if (entity.swingTime > 0 && !isAttacking) {
            isAttacking = true;

            event.getController().forceAnimationReset();

            event.getController().setAnimation(RawAnimation.begin().thenPlay("attack"));

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

    private <T extends GeoAnimatable> PlayState tailPredicate(AnimationState<T> event) {

        event.getController().setAnimation(RawAnimation.begin().thenLoop("tail"));

        return PlayState.CONTINUE;
    }
}
