package com.dragonminez.common.init.entities;

import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class BlackNimbusEntity extends Mob implements GeoEntity {

    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);

    private static final int BOOST_DURATION = 45;
    private static final double BOOST_INITIAL = 7.2D;
    private static final double BOOST_SUSTAIN = 0.65D;
    private int boostTicks = 0;
    private double boostStrength = 0.0D;
    private boolean boostKeyWasDown = false;

    public BlackNimbusEntity(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
    }

    public static AttributeSupplier createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.ATTACK_DAMAGE, 50.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5F)
                .add(Attributes.FLYING_SPEED, 2.4F).build();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            spawnAuraParticles(0x272d67);
        }
    }

    private void spawnAuraParticles(int colorHex) {
        int particleCount = 15;

        for (int i = 0; i < particleCount; i++) {
            double offsetX = (this.random.nextDouble() - 0.1D) * this.getBbWidth() * 0.01D;
            double offsetY = (this.random.nextDouble() - 0.02D) * this.getBbHeight() * 0.01D;
            double offsetZ = (this.random.nextDouble() - 0.1D) * this.getBbWidth() * 0.01D;

            double spawnX = this.getX() + offsetX - 0.5;
            double spawnY = this.getY() + offsetY + 1.0;
            double spawnZ = this.getZ() + offsetZ;

            this.level().addParticle(
                    MainParticles.KINTON.get(),
                    spawnX, spawnY, spawnZ,
                    colorHex, 0, 0
            );
        }
    }

    private void spawnBoostTrail(int colorHex) {
        Vec3 back = new Vec3(0, 0, 1).yRot((float) -Math.toRadians(this.getYRot())).scale(-1.0D);
        int count = 18;
        for (int i = 0; i < count; i++) {
            double spread = 0.7D;
            double ox = (this.random.nextDouble() - 0.5D) * spread;
            double oy = (this.random.nextDouble() - 0.5D) * spread;
            double oz = (this.random.nextDouble() - 0.5D) * spread;
            double dist = 0.5D + this.random.nextDouble() * 1.2D;

            double spawnX = this.getX() + back.x * dist + ox;
            double spawnY = this.getY() + 1.0D + oy;
            double spawnZ = this.getZ() + back.z * dist + oz;

            this.level().addParticle(
                    MainParticles.KINTON.get(),
                    spawnX, spawnY, spawnZ,
                    colorHex, 0, 0
            );
        }
    }

    private void spawnBoostBurst(int colorHex) {
        Vec3 back = new Vec3(0, 0, 1).yRot((float) -Math.toRadians(this.getYRot())).scale(-1.0D);
        double centerX = this.getX() + back.x * 0.8D;
        double centerY = this.getY() + 1.0D;
        double centerZ = this.getZ() + back.z * 0.8D;

        int count = 60;
        for (int i = 0; i < count; i++) {
            double radius = this.random.nextDouble() * 1.6D;
            double theta = this.random.nextDouble() * Math.PI * 2.0D;
            double phi = (this.random.nextDouble() - 0.5D) * Math.PI;

            double spawnX = centerX + Math.cos(theta) * Math.cos(phi) * radius;
            double spawnY = centerY + Math.sin(phi) * radius;
            double spawnZ = centerZ + Math.sin(theta) * Math.cos(phi) * radius;

            this.level().addParticle(
                    MainParticles.KINTON.get(),
                    spawnX, spawnY, spawnZ,
                    colorHex, 0, 0
            );
        }
    }

    @Override
    public void travel(Vec3 pTravelVector) {
        if (this.isAlive()) {
            if (this.getControllingPassenger() instanceof Player passenger) {
                this.setYRot(passenger.getYRot());
                this.yRotO = this.getYRot();
                this.setXRot(passenger.getXRot() * 0.5F);
                this.setRot(this.getYRot(), this.getXRot());
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.getYRot();


                double speed = this.getAttributeValue(Attributes.FLYING_SPEED) * 0.5D;
                double verticalSpeed = 0;

                // Control Vertical (Cliente)
                if (this.level().isClientSide) {
                    if (net.minecraft.client.Minecraft.getInstance().options.keyJump.isDown()) {
                        verticalSpeed = 0.4;
                    }
                    else if (KeyBinds.SECOND_FUNCTION_KEY.isDown()) {
                        verticalSpeed = -0.4;
                    }
                }

                float forwardInput = passenger.zza;
                float strafeInput = passenger.xxa;

                Vec3 inputVector = new Vec3(strafeInput, 0, forwardInput);
                Vec3 moveVector = inputVector.yRot((float) -Math.toRadians(this.getYRot()));

                if (moveVector.lengthSqr() > 1.0E-7D) {
                    moveVector = moveVector.normalize().scale(speed);
                }

                // Nitro: gestión del impulso temporal (cliente, igual que el control vertical)
                if (this.level().isClientSide) {
                    boolean keyDown = KeyBinds.DASH_KEY.isDown();
                    if (keyDown && !this.boostKeyWasDown && this.boostTicks <= 0) {
                        this.boostTicks = BOOST_DURATION;
                        this.boostStrength = BOOST_INITIAL;
                        spawnBoostBurst(0x272d67); // estallido de explosión al acelerar
                    }
                    this.boostKeyWasDown = keyDown;

                    if (this.boostTicks > 0) {
                        this.boostTicks--;
                        // El empujón brusco decae rápido hacia una velocidad sostenida
                        this.boostStrength = BOOST_SUSTAIN + (this.boostStrength - BOOST_SUSTAIN) * 0.80D;
                    } else if (this.boostStrength > 0.01D) {
                        // Desaceleración fluida de vuelta a la velocidad normal
                        this.boostStrength *= 0.88D;
                        if (this.boostStrength < 0.01D) this.boostStrength = 0.0D;
                    }

                    if (this.boostStrength > 0.05D) {
                        spawnBoostTrail(0x272d67);
                    }
                }

                // Empuje hacia adelante mientras el nitro está activo
                if (this.boostStrength > 0.01D) {
                    Vec3 forward = new Vec3(0, 0, 1).yRot((float) -Math.toRadians(this.getYRot()));
                    moveVector = moveVector.add(forward.scale(this.boostStrength));
                }

                this.setDeltaMovement(moveVector.x, verticalSpeed, moveVector.z);
                this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());

                return;
            }
        }

        Vec3 currentMotion = this.getDeltaMovement();
        this.setDeltaMovement(currentMotion.x * 0.9, -0.07, currentMotion.z * 0.9);

        super.travel(pTravelVector);
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof LivingEntity entity ? entity : null;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.9D;
    }

	@Override
	public void positionRider(Entity passenger, MoveFunction callback) {
		if (this.hasPassenger(passenger)) {
			double yOffset = this.getPassengersRidingOffset() + passenger.getMyRidingOffset();
			Vec3 vec3 = (new Vec3(0.0D, 0.0D, 0.0D)).yRot(-this.getYRot() * ((float)Math.PI / 180F) - ((float)Math.PI / 2F));
			callback.accept(passenger, this.getX() + vec3.x, this.getY() + yOffset, this.getZ() + vec3.z);
			if (passenger instanceof LivingEntity livingPassenger) {
				livingPassenger.yBodyRot = this.getYRot();
				livingPassenger.setYHeadRot(livingPassenger.getYHeadRot());
			}
		}
	}

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide) {
            int alignment = StatsProvider.get(StatsCapability.INSTANCE, player)
                    .map(data -> data.getResources().getAlignment())
                    .orElse(100);
            if (alignment >= 67) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.dragonminez.black_nimbus.not_evil"), true);
                return InteractionResult.SUCCESS;
            }
            if (!player.isPassenger()) {
                player.startRiding(this);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if ("player".equals(pSource.getMsgId()) && pSource.getEntity() instanceof Player) {
            if (!this.level().isClientSide && isAlive()) {
                this.spawnAtLocation(MainItems.NUBE_NEGRA_ITEM.get());
                this.remove(RemovalReason.KILLED);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource pSource) {
        return !"player".equals(pSource.getMsgId()) || super.isInvulnerableTo(pSource);
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
        tAnimationState.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
