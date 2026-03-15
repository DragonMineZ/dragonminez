package com.dragonminez.common.init.entities.ki;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.FLOAT);
    //0 = Small, 1 = Blast, 2 = Large Blast
    private static final EntityDataAccessor<Integer> KI_BALL_RENDER_TYPE = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> TECHNIQUE_ID = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> ARMOR_PENETRATION = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_HEAL = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> MAX_LIFE = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.INT);

    public AbstractKiProjectile(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.noCulling = true;
    }

    public abstract int getMaxHits();

    public float getDamagePerHit() {
        return this.getKiDamage() / Math.max(1.0F, (float)this.getMaxHits());
    }

    public void setup(LivingEntity owner, float damage, float size, float speed, int colorMain, int colorBorder) {
        this.setOwner(owner);
        this.setKiDamage(damage);
        this.setSize(size);
        this.setColors(colorMain, colorBorder);
        this.setKiSpeed(speed);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
    }

    public boolean shouldDamage(Entity target) {
        if (target == this) return false;
        if (target instanceof AbstractKiProjectile kiProj && kiProj.getOwner() == this.getOwner()) return false;

        if (this.getOwner() instanceof LivingEntity ownerLiving && target instanceof LivingEntity targetLiving) {
            if (this.isHeal()) {
                return ownerLiving.isAlliedTo(targetLiving) || targetLiving == ownerLiving;
            } else {
                if (target == this.getOwner() || target.is(this.getOwner())) return false;
                return !ownerLiving.isAlliedTo(targetLiving);
            }
        }

        return !this.isHeal();
    }

    public void playInitialSound(SoundEvent sound) {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), sound, SoundSource.PLAYERS, 0.1F, 0.8F + (this.random.nextFloat() * 0.2F));
    }

    public boolean applyDamageOrHeal(Entity target, float amount) {
        if (target instanceof LivingEntity livingTarget) {
            if (this.isHeal()) {
                if (livingTarget.getHealth() < livingTarget.getMaxHealth()) {
                    livingTarget.heal(amount);
                    return true;
                }
                return false;
            } else {
                return livingTarget.hurt(com.dragonminez.common.init.MainDamageTypes.kiblast(this.level(), this, this.getOwner()), amount);
            }
        }
        return false;
    }

    public void onSuccessfulHit(Entity target) {
        if (!this.level().isClientSide && this.getOwner() instanceof net.minecraft.world.entity.player.Player player) {
            String techId = this.getTechniqueId();
            if (techId != null && !techId.isEmpty()) {
                StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
                    stats.getTechniques().addExperienceToTechnique(techId, 1);
                });
            }
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(COLOR_MAIN, 0xFFFFFF);
        this.entityData.define(COLOR_BORDER, 0xFFFFFF);
        this.entityData.define(DAMAGE, 5.0f);
        this.entityData.define(SIZE, 1.0f);
        this.entityData.define(SPEED, 1.0f);
        this.entityData.define(KI_BALL_RENDER_TYPE, 1);
        this.entityData.define(TECHNIQUE_ID, "");
        this.entityData.define(ARMOR_PENETRATION, 0);
        this.entityData.define(MAX_LIFE, 100);
        this.entityData.define(IS_HEAL, false);
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

    public void setKiSpeed(float speed) { this.entityData.set(SPEED, speed); }
    public float getKiSpeed() { return this.entityData.get(SPEED); }

    public void setKiRenderType(int type) { this.entityData.set(KI_BALL_RENDER_TYPE, type); }
    public int getKiRenderType() { return this.entityData.get(KI_BALL_RENDER_TYPE); }

    public String getTechniqueId() { return this.entityData.get(TECHNIQUE_ID); }
    public void setTechniqueId(String id) { this.entityData.set(TECHNIQUE_ID, id); }

    public int getArmorPenetration() { return this.entityData.get(ARMOR_PENETRATION); }
    public void setArmorPenetration(int pen) { this.entityData.set(ARMOR_PENETRATION, pen); }

    public boolean isHeal() { return this.entityData.get(IS_HEAL); }
    public void setHeal(boolean heal) { this.entityData.set(IS_HEAL, heal); }

    public void setMaxLife(int lifeInTicks) {this.entityData.set(MAX_LIFE, lifeInTicks);}
    public int getMaxLife() {return this.entityData.get(MAX_LIFE);}

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("ColorMain", getColor());
        pCompound.putInt("ColorBorder", getColorBorde());
        pCompound.putFloat("Damage", getKiDamage());
        pCompound.putFloat("Size", getSize());
        pCompound.putFloat("Speed", getKiSpeed());
        pCompound.putString("TechniqueId", getTechniqueId());
        pCompound.putInt("ArmorPenetration", getArmorPenetration());
        pCompound.putBoolean("IsHeal", isHeal());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("ColorMain")) setColors(pCompound.getInt("ColorMain"), pCompound.getInt("ColorBorder"));
        if (pCompound.contains("Damage")) setKiDamage(pCompound.getFloat("Damage"));
        if (pCompound.contains("Size")) setSize(pCompound.getFloat("Size"));
        if (pCompound.contains("Speed")) setKiSpeed(pCompound.getFloat("Speed"));
        if (pCompound.contains("TechniqueId")) setTechniqueId(pCompound.getString("TechniqueId"));
        if (pCompound.contains("ArmorPenetration")) setArmorPenetration(pCompound.getInt("ArmorPenetration"));
        if (pCompound.contains("IsHeal")) setHeal(pCompound.getBoolean("IsHeal"));
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