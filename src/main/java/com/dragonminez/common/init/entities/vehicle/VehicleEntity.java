package com.dragonminez.common.init.entities.vehicle;

import com.dragonminez.common.init.entities.vehicle.VehicleProperties.Mobility;
import com.dragonminez.common.network.C2S.VehicleFlightModeC2S;
import com.dragonminez.common.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;

public abstract class VehicleEntity extends Mob implements GeoEntity {

	private static final EntityDataAccessor<Boolean> FLIGHT_MODE = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
	private static final String FLIGHT_MODE_TAG = "VehicleFlightMode";

	private static final double GRAVITY = 0.08D;
	private static final double AIR_DRAG = 0.98D;
	private static final double TERMINAL_VELOCITY = 3.0D;
	private static final double UNMANNED_DESCENT_SPEED = 0.07D;
	private static final double SINK_ACCELERATION = 0.02D;
	private static final double MAX_SINK_SPEED = 0.1D;
	private static final double FLUID_RISE_SPEED = 0.2D;
	private static final double FLUID_EPSILON = 1.0E-3D;
	private static final int FLUID_SCAN_LIMIT = 64;
	private static final double MAX_HORIZONTAL_SPEED = 9.0D;
	private static final int DOUBLE_TAP_TICKS = 6;
	private static final int LIFTOFF_TICKS = 5;

	private final VehicleProperties properties;
	private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);

	private boolean flightMode;
	private boolean inputAscend;
	private boolean inputDescend;
	private boolean inputBoost;
	private boolean lastAscend;
	private boolean lastBoost;
	private int lastJumpTapTick = Integer.MIN_VALUE / 2;
	private int liftoffTicks;
	private int boostTicks;
	private double boostStrength;

	protected VehicleEntity(EntityType<? extends VehicleEntity> type, Level level, VehicleProperties properties) {
		super(type, level);
		this.properties = properties;
		this.setNoGravity(true);
		this.setMaxUpStep(properties.stepHeight());
	}

	public static AttributeSupplier createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 50.0D)
				.add(Attributes.ATTACK_DAMAGE, 50.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.5F)
				.add(Attributes.FLYING_SPEED, 2.4F).build();
	}

	protected abstract Item getVehicleItem();

	protected boolean canBeMountedBy(Player player) {
		return true;
	}

	protected void onBoostStart() {
	}

	public VehicleProperties getProperties() {
		return properties;
	}

	public boolean isFlightMode() {
		return switch (properties.mobility()) {
			case FLYING -> true;
			case GROUND -> false;
			case HYBRID -> flightMode;
		};
	}

	public void setDriverInput(boolean ascend, boolean descend, boolean boost) {
		this.inputAscend = ascend;
		this.inputDescend = descend;
		this.inputBoost = boost;
	}

	public void receiveDriverFlightMode(ServerPlayer driver, boolean flying) {
		if (properties.mobility() != Mobility.HYBRID || this.getControllingPassenger() != driver) return;
		this.flightMode = flying;
		this.entityData.set(FLIGHT_MODE, flying);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(FLIGHT_MODE, false);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (FLIGHT_MODE.equals(key) && !(this.level().isClientSide && this.isControlledByLocalInstance())) {
			this.flightMode = this.entityData.get(FLIGHT_MODE);
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (properties.mobility() == Mobility.HYBRID) tag.putBoolean(FLIGHT_MODE_TAG, this.flightMode);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (properties.mobility() == Mobility.HYBRID) setFlightMode(tag.getBoolean(FLIGHT_MODE_TAG));
	}

	@Override
	protected void tickRidden(Player driver, Vec3 input) {
		super.tickRidden(driver, input);
		this.setRot(driver.getYRot(), isFlightMode() ? driver.getXRot() * 0.5F : 0.0F);
		this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
	}

	@Override
	public void travel(Vec3 travelVector) {
		if (!this.isAlive() || !this.isControlledByLocalInstance()) return;
		if (this.getControllingPassenger() instanceof Player driver) travelDriven(driver);
		else travelUnmanned();
	}

	private void travelDriven(Player driver) {
		boolean ascend = this.inputAscend;
		boolean descend = this.inputDescend;
		boolean boost = this.inputBoost;
		this.inputAscend = this.inputDescend = this.inputBoost = false;
		boolean ascendPressed = ascend && !this.lastAscend;
		boolean boostPressed = boost && !this.lastBoost;
		this.lastAscend = ascend;
		this.lastBoost = boost;

		this.resetFallDistance();
		tickBoost(boostPressed);

		boolean hybrid = properties.mobility() == Mobility.HYBRID;
		if (hybrid && !this.flightMode && ascendPressed && isDoubleTap()) {
			setFlightMode(true);
			this.liftoffTicks = LIFTOFF_TICKS;
		}

		boolean flying = isFlightMode();
		Vec3 horizontal = horizontalMotion(driver);
		double vertical;
		boolean landedOnFluid = false;
		if (flying) {
			vertical = flightVertical(ascend, descend);
			if (hybrid && this.liftoffTicks == 0 && vertical < 0.0D) {
				double support = fluidSupport(vertical);
				if (!Double.isNaN(support)) {
					vertical = support;
					landedOnFluid = true;
				}
			}
		} else {
			vertical = Math.max((this.getDeltaMovement().y - GRAVITY) * AIR_DRAG, -TERMINAL_VELOCITY);
			double support = fluidSupport(vertical);
			if (!Double.isNaN(support)) {
				vertical = support;
				this.setOnGround(true);
			}
		}

		this.setDeltaMovement(horizontal.x, vertical, horizontal.z);
		this.move(MoverType.SELF, this.getDeltaMovement());

		if (flying && hybrid && this.liftoffTicks == 0 && (landedOnFluid || (vertical < 0.0D && this.verticalCollisionBelow))) {
			setFlightMode(false);
		}
	}

	private void travelUnmanned() {
		Vec3 motion = this.getDeltaMovement();
		double vertical;
		if (this.isInFluidType()) vertical = Math.max(motion.y * 0.8D - SINK_ACCELERATION, -MAX_SINK_SPEED);
		else if (isFlightMode()) vertical = -UNMANNED_DESCENT_SPEED;
		else vertical = Math.max((motion.y - GRAVITY) * AIR_DRAG, -TERMINAL_VELOCITY);

		double friction = this.onGround() ? groundFriction() : 0.91D;
		this.setDeltaMovement(motion.x * friction, vertical, motion.z * friction);
		this.move(MoverType.SELF, this.getDeltaMovement());

		if (properties.mobility() == Mobility.HYBRID && this.flightMode && this.verticalCollisionBelow) setFlightMode(false);
	}

	private Vec3 horizontalMotion(Player driver) {
		Vec3 input = new Vec3(driver.xxa, 0.0D, driver.zza);
		Vec3 motion = Vec3.ZERO;
		if (input.lengthSqr() > 1.0E-7D) {
			double speed = properties.speed();
			if (properties.canSprint() && driver.isSprinting()) speed *= properties.sprintMultiplier();
			motion = input.normalize().scale(speed).yRot(-this.getYRot() * Mth.DEG_TO_RAD);
		}
		if (this.boostStrength > 0.0D) motion = motion.add(forward().scale(this.boostStrength));
		double length = motion.horizontalDistance();
		return length > MAX_HORIZONTAL_SPEED ? motion.scale(MAX_HORIZONTAL_SPEED / length) : motion;
	}

	private double flightVertical(boolean ascend, boolean descend) {
		if (this.liftoffTicks > 0) {
			this.liftoffTicks--;
			return properties.verticalSpeed();
		}
		if (ascend) return properties.verticalSpeed();
		if (descend) return -properties.verticalSpeed();
		return 0.0D;
	}

	private void tickBoost(boolean pressed) {
		VehicleProperties.Boost boost = properties.boost();
		if (boost == null) return;
		if (pressed && this.boostTicks <= 0) {
			this.boostTicks = boost.duration();
			this.boostStrength = boost.initial();
			onBoostStart();
		}
		if (this.boostTicks > 0) {
			this.boostTicks--;
			this.boostStrength = boost.sustain() + (this.boostStrength - boost.sustain()) * 0.8D;
		} else if (this.boostStrength > 0.0D) {
			this.boostStrength *= 0.88D;
			if (this.boostStrength < 0.01D) this.boostStrength = 0.0D;
		}
	}

	private boolean isDoubleTap() {
		boolean doubleTap = this.tickCount - this.lastJumpTapTick <= DOUBLE_TAP_TICKS;
		this.lastJumpTapTick = doubleTap ? Integer.MIN_VALUE / 2 : this.tickCount;
		return doubleTap;
	}

	private void setFlightMode(boolean flying) {
		if (properties.mobility() != Mobility.HYBRID || this.flightMode == flying) return;
		this.flightMode = flying;
		if (this.level().isClientSide) NetworkHandler.sendToServer(new VehicleFlightModeC2S(flying));
		else this.entityData.set(FLIGHT_MODE, flying);
	}

	private double fluidSupport(double vertical) {
		double surface = findFluidSurface(vertical);
		if (Double.isNaN(surface) || this.getY() + vertical > surface) return Double.NaN;
		return Math.min(surface - this.getY(), FLUID_RISE_SPEED);
	}

	private double findFluidSurface(double vertical) {
		AABB box = this.getBoundingBox();
		int minX = Mth.floor(box.minX + FLUID_EPSILON);
		int maxX = Mth.floor(box.maxX - FLUID_EPSILON);
		int minZ = Mth.floor(box.minZ + FLUID_EPSILON);
		int maxZ = Mth.floor(box.maxZ - FLUID_EPSILON);
		int top = Mth.floor(box.minY);
		int bottom = Mth.floor(box.minY + Math.min(vertical, 0.0D) - FLUID_EPSILON);
		Level level = this.level();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		double surface = Double.NaN;
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				for (int y = top; y >= bottom; y--) {
					if (!isFloatable(level.getFluidState(pos.set(x, y, z)))) continue;
					int fluidTop = y;
					while (fluidTop - y < FLUID_SCAN_LIMIT && isFloatable(level.getFluidState(pos.set(x, fluidTop + 1, z)))) fluidTop++;
					pos.set(x, fluidTop, z);
					double height = fluidTop + level.getFluidState(pos).getHeight(level, pos);
					if (Double.isNaN(surface) || height > surface) surface = height;
					break;
				}
			}
		}
		return surface;
	}

	private static boolean isFloatable(FluidState fluid) {
		if (fluid.isEmpty()) return false;
		return !fluid.hasProperty(FlowingFluid.FALLING) || !fluid.getValue(FlowingFluid.FALLING);
	}

	private double groundFriction() {
		BlockPos below = this.getBlockPosBelowThatAffectsMyMovement();
		return this.level().getBlockState(below).getFriction(this.level(), below, this) * 0.91D;
	}

	private Vec3 forward() {
		return new Vec3(0.0D, 0.0D, 1.0D).yRot(-this.getYRot() * Mth.DEG_TO_RAD);
	}

	protected void spawnBoostBurst(ParticleOptions particle, int color) {
		Vec3 back = forward().scale(-0.8D);
		double centerX = this.getX() + back.x;
		double centerY = this.getY() + 1.0D;
		double centerZ = this.getZ() + back.z;
		for (int i = 0; i < 60; i++) {
			double radius = this.random.nextDouble() * 1.6D;
			double theta = this.random.nextDouble() * Math.PI * 2.0D;
			double phi = (this.random.nextDouble() - 0.5D) * Math.PI;
			this.level().addParticle(particle,
					centerX + Math.cos(theta) * Math.cos(phi) * radius,
					centerY + Math.sin(phi) * radius,
					centerZ + Math.sin(theta) * Math.cos(phi) * radius,
					color, 0.0D, 0.0D);
		}
	}

	@Override
	public boolean canSprint() {
		return properties.canSprint();
	}

	@Override
	public LivingEntity getControllingPassenger() {
		return this.getFirstPassenger() instanceof Player player ? player : null;
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return this.getPassengers().size() < properties.seatCount();
	}

	@Override
	public double getPassengersRidingOffset() {
		return properties.seats().get(0).y;
	}

	@Override
	protected void positionRider(Entity passenger, MoveFunction callback) {
		if (!this.hasPassenger(passenger)) return;
		int index = Mth.clamp(this.getPassengers().indexOf(passenger), 0, properties.seatCount() - 1);
		Vec3 seat = properties.seats().get(index);
		Vec3 offset = new Vec3(seat.x, 0.0D, seat.z).yRot(-this.getYRot() * Mth.DEG_TO_RAD);
		callback.accept(passenger, this.getX() + offset.x, this.getY() + seat.y + passenger.getMyRidingOffset(), this.getZ() + offset.z);
		if (passenger instanceof LivingEntity living) living.yBodyRot = this.getYRot();
	}

	@Override
	protected void removePassenger(Entity passenger) {
		boolean wasDriver = passenger == this.getControllingPassenger();
		super.removePassenger(passenger);
		if (!wasDriver) return;
		this.inputAscend = this.inputDescend = this.inputBoost = false;
		this.lastAscend = this.lastBoost = false;
		this.liftoffTicks = 0;
		this.boostTicks = 0;
		this.boostStrength = 0.0D;
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!this.level().isClientSide) tryMount(player);
		return InteractionResult.SUCCESS;
	}

	protected boolean tryMount(Player player) {
		if (player.isPassenger() || !canAddPassenger(player) || !canBeMountedBy(player)) return false;
		return player.startRiding(this);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (!"player".equals(source.getMsgId()) || !(source.getEntity() instanceof Player)) return false;
		if (!this.level().isClientSide && this.isAlive()) {
			this.spawnAtLocation(getVehicleItem());
			this.remove(RemovalReason.KILLED);
		}
		return true;
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return !"player".equals(source.getMsgId()) || super.isInvulnerableTo(source);
	}

	@Override
	public ItemStack getPickResult() {
		return new ItemStack(getVehicleItem());
	}

	@Override
	public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	public boolean fireImmune() {
		return true;
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	protected MovementEmission getMovementEmission() {
		return MovementEmission.EVENTS;
	}

	@Override
	public boolean requiresCustomPersistence() {
		return true;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return geoCache;
	}
}
