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
    private static final EntityDataAccessor<Integer> COLOR_MAIN = SynchedEntityData.defineId(KiExplosionVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR_BORDER = SynchedEntityData.defineId(KiExplosionVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR_OUTLINE = SynchedEntityData.defineId(KiExplosionVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> MAX_SIZE = SynchedEntityData.defineId(KiExplosionVisualEntity.class, EntityDataSerializers.FLOAT);
    private transient float[] cachedColorMainRgb;
    private transient float[] cachedColorBorderRgb;
    private transient float[] cachedColorOutlineRgb;

    public KiExplosionVisualEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        this.noPhysics = true;
    }

    public void setupExplosion(int colorMain, int colorBorder, float baseSize) {
        this.setupExplosion(colorMain, colorBorder, 0xFFFFFF, baseSize);
    }

    public void setupExplosion(int colorMain, int colorBorder, int colorOutline, float baseSize) {
        this.entityData.set(COLOR_MAIN, colorMain);
        this.entityData.set(COLOR_BORDER, colorBorder);
        this.entityData.set(COLOR_OUTLINE, colorOutline);
        this.entityData.set(MAX_SIZE, baseSize * 2.0F);
        this.cachedColorMainRgb = null;
        this.cachedColorBorderRgb = null;
        this.cachedColorOutlineRgb = null;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(COLOR_MAIN, 0xFFFFFF);
        this.entityData.define(COLOR_BORDER, 0xFFFFFF);
        this.entityData.define(COLOR_OUTLINE, 0xFFFFFF);
        this.entityData.define(MAX_SIZE, 1.0F);
    }

    public int getColorMain() { return this.entityData.get(COLOR_MAIN); }
    public int getColorBorder() { return this.entityData.get(COLOR_BORDER); }
    public int getColorOutline() { return this.entityData.get(COLOR_OUTLINE); }

    public float[] getRgbColorMain() {
        if (this.cachedColorMainRgb == null) this.cachedColorMainRgb = ColorUtils.rgbIntToFloat(this.getColorMain());
        return this.cachedColorMainRgb;
    }

    public float[] getRgbColorBorder() {
        if (this.cachedColorBorderRgb == null) this.cachedColorBorderRgb = ColorUtils.rgbIntToFloat(this.getColorBorder());
        return this.cachedColorBorderRgb;
    }

    public float[] getRgbColorOutline() {
        if (this.cachedColorOutlineRgb == null) this.cachedColorOutlineRgb = ColorUtils.rgbIntToFloat(this.getColorOutline());
        return this.cachedColorOutlineRgb;
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
