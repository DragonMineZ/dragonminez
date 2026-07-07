package com.dragonminez.common.init.entities.sagas;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.entity.PartEntity;

public class DBSagasPart extends PartEntity<DBSagasEntity> {

    public final DBSagasEntity owner;
    public final String partName;
    private final EntityDimensions size;

    public final float forwardOffset;
    public final float sideOffset;
    public final float yOffset;

    public DBSagasPart(DBSagasEntity owner, String partName, float width, float height,
                       float forwardOffset, float sideOffset, float yOffset) {
        super(owner);
        this.owner = owner;
        this.partName = partName;
        this.size = EntityDimensions.scalable(width, height);
        this.forwardOffset = forwardOffset;
        this.sideOffset = sideOffset;
        this.yOffset = yOffset;
        this.refreshDimensions();
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
    }

    @Override
    public EntityDimensions getDimensions(Pose pPose) {
        return this.size;
    }

    @Override
    public boolean isPickable() {
        return this.owner.isAlive();
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        return this.isInvulnerableTo(pSource) ? false : this.owner.receivePartDamage(pSource, pAmount, this);
    }

    @Override
    public boolean is(Entity pEntity) {
        return this == pEntity || this.owner == pEntity;
    }
}
