package com.dragonminez.common.init.entities.ki;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractKiProjectile extends Projectile {

    private static final EntityDataAccessor<Integer> COLOR_MAIN = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR_BORDER = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.FLOAT);

    public AbstractKiProjectile(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.noCulling = true;
    }

    public void setup(LivingEntity owner, float damage, float size, int colorMain, int colorBorder) {
        this.setOwner(owner);
        this.setKiDamage(damage);
        this.setSize(size);
        this.setColors(colorMain, colorBorder);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(COLOR_MAIN, 0xFFFFFF);
        this.entityData.define(COLOR_BORDER, 0xFFFFFF);
        this.entityData.define(DAMAGE, 5.0f);
        this.entityData.define(SIZE, 1.0f);
    }
    @Override
    public void tick() {
        super.tick();
        Vec3 movement = this.getDeltaMovement();
        double nextX = this.getX() + movement.x;
        double nextY = this.getY() + movement.y;
        double nextZ = this.getZ() + movement.z;
        this.setPos(nextX, nextY, nextZ);

        ProjectileUtil.rotateTowardsMovement(this, 0.2F);

        if (!this.level().isClientSide) {
            HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hitResult.getType() != HitResult.Type.MISS) {
                this.onHit(hitResult);
            }
        }

        this.onKiTick();

        if (this.tickCount > 10*20) this.discard();
    }

    public boolean shouldDamage(LivingEntity target) {
        Entity owner = this.getOwner();
        if (target == owner) return false;
        if (owner instanceof LivingEntity livingOwner) {
            if (livingOwner.isAlliedTo(target)) {
                return false;
            }
        }
        return true;
    }

    protected void onKiTick() {}

    public void setColors(int main, int border) {
        this.entityData.set(COLOR_MAIN, main);
        this.entityData.set(COLOR_BORDER, border);
    }
    public int getColor() { return this.entityData.get(COLOR_MAIN); }
    public int getColorBorde() { return this.entityData.get(COLOR_BORDER); }

    public void setKiDamage(float damage) { this.entityData.set(DAMAGE, damage); }
    public float getKiDamage() { return this.entityData.get(DAMAGE); }

    public void setSize(float size) { this.entityData.set(SIZE, size); }
    public float getSize() { return this.entityData.get(SIZE); }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("ColorMain", getColor());
        pCompound.putInt("ColorBorder", getColorBorde());
        pCompound.putFloat("Damage", getKiDamage());
        pCompound.putFloat("Size", getSize());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("ColorMain")) setColors(pCompound.getInt("ColorMain"), pCompound.getInt("ColorBorder"));
        if (pCompound.contains("Damage")) setKiDamage(pCompound.getFloat("Damage"));
        if (pCompound.contains("Size")) setSize(pCompound.getFloat("Size"));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        super.onSyncedDataUpdated(pKey);
        if (SIZE.equals(pKey)) {
            this.refreshDimensions();
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pPose) {
        return super.getDimensions(pPose).scale(this.getSize());
    }
}
