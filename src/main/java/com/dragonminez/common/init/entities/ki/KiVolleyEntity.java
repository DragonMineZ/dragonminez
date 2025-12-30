package com.dragonminez.common.init.entities.ki;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class KiVolleyEntity extends AbstractKiProjectile {

    private Vec3 focalPoint = null;
    private Vec3 finalForward = null;
    private int curveDelay = 0;

    private boolean hasSpawnedFlash = false;
    private boolean hasSpawnedSplash = false;

    private static final double[][] VOLLEY_OFFSETS = {
            {0.0, 0.0},
            {5.0, 0.0},
            {-5.0, 0.0},
            {1.2, 3.5},
            {-1.2, 3.5}
    };

    public KiVolleyEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
    }

    public KiVolleyEntity(Level level, LivingEntity owner) {
        this(MainEntities.KI_VOLLEY.get(), level);
        this.setOwner(owner);
        level.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                MainSounds.KIBLAST_ATTACK.get(), SoundSource.PLAYERS, 0.5F, 1.5F);
    }

    public static void shootVolley(LivingEntity attacker, LivingEntity target, float speed, float damage, int colorMain, int colorBorder) {
        Level level = attacker.level();

        Vec3 origin = attacker.getEyePosition();
        Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.6, 0);

        Vec3 viewVector = targetPos.subtract(origin).normalize();
        Vec3 globalUp = new Vec3(0, 1, 0);
        Vec3 rightVector = viewVector.cross(globalUp).normalize();
        Vec3 upVector = rightVector.cross(viewVector).normalize();

        for (double[] offset : VOLLEY_OFFSETS) {
            KiVolleyEntity volley = new KiVolleyEntity(level, attacker);

            volley.setup(attacker, damage, 0.4F, colorMain, colorBorder);

            Vec3 spawnPos = origin
                    .add(rightVector.scale(offset[0]))
                    .add(upVector.scale(offset[1]));
            volley.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

            Vec3 direction = targetPos.subtract(spawnPos).normalize();
            volley.setDeltaMovement(direction.scale(speed));

            volley.setConvergeTarget(targetPos, viewVector, 0);

            level.addFreshEntity(volley);
        }
    }

    public void setConvergeTarget(Vec3 point, Vec3 forwardDirection, int delayTicks) {
        this.focalPoint = point;
        this.finalForward = forwardDirection.normalize();
        this.curveDelay = delayTicks;
    }

    @Override
    protected void onKiTick() {
        if (!this.level().isClientSide && this.getOwner() == null) {
            this.discard();
            return;
        }
        if (this.level().isClientSide) {
            float[] rgb = ColorUtils.rgbIntToFloat(this.getColorBorde());

            if (!hasSpawnedSplash) {
                this.level().addParticle(MainParticles.KI_SPLASH.get(), this.getX(), this.getY(), this.getZ(), rgb[0], rgb[1], rgb[2]);
                this.hasSpawnedSplash = true;
            }

            if (!hasSpawnedFlash) {
                this.level().addParticle(MainParticles.KI_FLASH.get(), this.getX(), this.getY(), this.getZ(), (double) this.getId(), 0.0D, 0.0D);
                this.hasSpawnedFlash = true;
            }

            for (int i = 0; i < 2; i++) {
                this.level().addParticle(MainParticles.KI_TRAIL.get(),
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.3,
                        this.getY() + (this.random.nextDouble() - 0.5) * 0.3,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.3,
                        rgb[0], rgb[1], rgb[2]);
            }
        }

        if (!this.level().isClientSide) {
            if (this.tickCount > 100) this.discard();

            if (this.tickCount % 30 == 0) {
                pulseAreaDamage();
            }

            if (this.focalPoint != null && this.finalForward != null && this.tickCount >= this.curveDelay) {
                double distSqr = this.position().distanceToSqr(this.focalPoint);
                Vec3 currentVel = this.getDeltaMovement();
                double speed = currentVel.length();

                if (distSqr < 4.25D) {
                    this.setDeltaMovement(this.finalForward.scale(speed));
                    this.focalPoint = null;
                    this.hasImpulse = true;
                    return;
                }

                Vec3 directionToFocus = this.focalPoint.subtract(this.position()).normalize();
                double turnSpeed = 0.5;
                Vec3 newVelocity = currentVel.normalize().lerp(directionToFocus, turnSpeed).normalize().scale(speed);
                this.setDeltaMovement(newVelocity);
                this.hasImpulse = true;
            }
        }
    }

    private void pulseAreaDamage() {
        double radius = this.getSize();
        AABB area = this.getBoundingBox().inflate(radius);
        List<LivingEntity> nearby = this.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity target : nearby) {
            if (this.shouldDamage(target)) {
                target.hurt(this.damageSources().indirectMagic(this, this.getOwner()), this.getKiDamage() * 0.2F);
            }
        }
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean hurt(DamageSource source, float amount) { return false; }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (entity instanceof KiVolleyEntity) return false;
        return super.canHitEntity(entity);
    }

	@Override
	protected void onHitEntity(EntityHitResult pResult) {
		if (pResult.getEntity() instanceof KiVolleyEntity) return;
		super.onHitEntity(pResult);
		if (!this.level().isClientSide) {
			if (this.shouldDamage(pResult.getEntity())) {
				pResult.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), this.getKiDamage());
			}
			explodeAndDie();
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult pResult) {
		super.onHitBlock(pResult);
		if (!this.level().isClientSide) {
			explodeAndDie();
		}
	}

	private void explodeAndDie() {
		boolean shouldDestroyBlocks = ConfigManager.getServerConfig().getGameplay().isKiDestroyBlocks();
		float radius = 2.0F;

		AABB area = this.getBoundingBox().inflate(radius);
		List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, area);

		for (LivingEntity target : entities) {
			if (this.shouldDamage(target)) {
				if (this.distanceToSqr(target) <= radius * radius) {
					target.hurt(this.damageSources().explosion(this, this.getOwner()), this.getKiDamage());
				}
			}
		}

		this.level().addParticle(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
		this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0F, 1.5F);

		Level.ExplosionInteraction interaction = shouldDestroyBlocks ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;

		this.level().explode(
				this,
				this.damageSources().explosion(this, this.getOwner()),
				null,
				this.getX(), this.getY(), this.getZ(),
				radius,
				false,
				interaction,
				false
		);

		this.discard();
	}

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("CurveDelay", this.curveDelay);
        if (this.focalPoint != null) {
            pCompound.putDouble("FocalX", this.focalPoint.x);
            pCompound.putDouble("FocalY", this.focalPoint.y);
            pCompound.putDouble("FocalZ", this.focalPoint.z);
        }
        if (this.finalForward != null) {
            pCompound.putDouble("ForwardX", this.finalForward.x);
            pCompound.putDouble("ForwardY", this.finalForward.y);
            pCompound.putDouble("ForwardZ", this.finalForward.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.curveDelay = pCompound.getInt("CurveDelay");
        if (pCompound.contains("FocalX")) {
            this.focalPoint = new Vec3(pCompound.getDouble("FocalX"), pCompound.getDouble("FocalY"), pCompound.getDouble("FocalZ"));
        }
        if (pCompound.contains("ForwardX")) {
            this.finalForward = new Vec3(pCompound.getDouble("ForwardX"), pCompound.getDouble("ForwardY"), pCompound.getDouble("ForwardZ"));
        }
    }
}