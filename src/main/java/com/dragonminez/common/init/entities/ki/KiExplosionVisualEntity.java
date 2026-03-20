package com.dragonminez.common.init.entities.ki;

import com.dragonminez.client.util.ColorUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class KiExplosionVisualEntity extends Entity {
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(KiExplosionVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> MAX_SIZE = SynchedEntityData.defineId(KiExplosionVisualEntity.class, EntityDataSerializers.FLOAT);
    private transient float[] cachedColorMainRgb;

    public KiExplosionVisualEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        this.noPhysics = true;
    }

    public void setupExplosion(int borderColor, float baseSize) {
        this.entityData.set(COLOR, borderColor);
        this.entityData.set(MAX_SIZE, baseSize * 2.0F);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(COLOR, 0xFFFFFF);
        this.entityData.define(MAX_SIZE, 1.0F);
    }

    public int getColor() {
        return this.entityData.get(COLOR);
    }

    public float[] getRgbColorMain() {
        if (this.cachedColorMainRgb == null) this.cachedColorMainRgb = ColorUtils.rgbIntToFloat(this.getColor());
        return this.cachedColorMainRgb;
    }

    public float getMaxSize() {
        return this.entityData.get(MAX_SIZE);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount >= 25 && !this.level().isClientSide) {
            this.discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {}
}
