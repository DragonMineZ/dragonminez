package com.dragonminez.common.init.entities.vehicle;

import com.dragonminez.common.init.entities.vehicle.VehicleProperties.Mobility;
import com.dragonminez.common.init.particles.VehicleSmokeOptions;
import com.dragonminez.common.network.C2S.VehicleFlightModeC2S;
import com.dragonminez.common.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public abstract class VehicleEntity extends Mob implements GeoEntity {

	private static final EntityDataAccessor<Boolean> FLIGHT_MODE = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<CompoundTag> SEAT_OCCUPANTS = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.COMPOUND_TAG);
	private static final String SEATS_KEY = "Seats";
	private static final int EMPTY_SEAT = -1;
	private static final double PLAYER_SEAT_OFFSET = 0.25D;
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
	private static final double SMOKE_SPACING = 0.35D;
	private static final int MAX_SMOKE_STEPS = 8;
	private static final float FULL_BANK_YAW_RATE = 12.0F;
	private static final float TILT_SMOOTHING = 0.18F;
	private static final float HOVER_BLEND_SPEED = 0.08F;
	private static final double CULL_MARGIN = 0.75D;
	private static final double DISMOUNT_GAP = 0.3D;
	private static final int[] DISMOUNT_HEIGHTS = {0, 1, -1, 2, -2};

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

	private double clientSpeed;
	private float tiltRoll;
	private float tiltRollO;
	private float tiltPitch;
	private float tiltPitchO;
	private float lastYaw;
	private boolean hasLastYaw;
	private float hoverBlend;
	private float hoverBlendO;
	private double[] cullExtents;

	protected VehicleEntity(EntityType<? extends VehicleEntity> type, Level level, VehicleProperties properties) {
		super(type, level);
		this.properties = properties;
		this.setNoGravity(true);
		this.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(properties.stepHeight());
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

	protected void onFlightModeChanged(boolean flying) {
	}

	protected void tickVisuals(double speed) {
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

	public double getClientSpeed() {
		return clientSpeed;
	}

	public float getSpeedFactor() {
		return (float) Mth.clamp(clientSpeed / Math.max(properties.topSpeed(), 1.0E-3D), 0.0D, 1.0D);
	}

	public float getTiltRoll(float partialTick) {
		return Mth.lerp(partialTick, tiltRollO, tiltRoll);
	}

	public float getTiltPitch(float partialTick) {
		return Mth.lerp(partialTick, tiltPitchO, tiltPitch);
	}

	public float getHoverOffset(float partialTick) {
		if (properties.hoverBob() == 0.0F) return 0.0F;
		float blend = Mth.lerp(partialTick, hoverBlendO, hoverBlend);
		return Mth.sin((this.tickCount + partialTick) * 0.12F) * properties.hoverBob() * blend;
	}

	public Vec3 applyVisualTransform(Vec3 local, float partialTick) {
		float roll = getTiltRoll(partialTick) * Mth.DEG_TO_RAD;
		float pitch = getTiltPitch(partialTick) * Mth.DEG_TO_RAD;
		double pivot = properties.tilt().pivotHeight();
		double side = local.x;
		double up = local.y - pivot;
		double forward = local.z;
		if (roll != 0.0F) {
			double cos = Mth.cos(roll);
			double sin = Mth.sin(roll);
			double rolledSide = side * cos - up * sin;
			up = side * sin + up * cos;
			side = rolledSide;
		}
		if (pitch != 0.0F) {
			double cos = Mth.cos(pitch);
			double sin = Mth.sin(pitch);
			double pitchedUp = up * cos - forward * sin;
			forward = up * sin + forward * cos;
			up = pitchedUp;
		}
		return new Vec3(side, up + pivot + getHoverOffset(partialTick), forward);
	}

	public Vec3 localToWorld(Vec3 local) {
		Vec3 horizontal = new Vec3(local.x, 0.0D, local.z).yRot(-this.getYRot() * Mth.DEG_TO_RAD);
		return new Vec3(this.getX() + horizontal.x, this.getY() + local.y, this.getZ() + horizontal.z);
	}

	public void setDriverInput(boolean ascend, boolean descend, boolean boost) {
		this.inputAscend = ascend;
		this.inputDescend = descend;
		this.inputBoost = boost;
	}

	public void receiveDriverFlightMode(ServerPlayer driver, boolean flying) {
		if (properties.mobility() != Mobility.HYBRID || this.getControllingPassenger() != driver) return;
		boolean changed = this.flightMode != flying;
		this.flightMode = flying;
		this.entityData.set(FLIGHT_MODE, flying);
		if (changed) onFlightModeChanged(flying);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(FLIGHT_MODE, false);
		builder.define(SEAT_OCCUPANTS, new CompoundTag());
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
		if (properties.mobility() == Mobility.HYBRID) {
			this.flightMode = tag.getBoolean(FLIGHT_MODE_TAG);
			this.entityData.set(FLIGHT_MODE, this.flightMode);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide) tickClientEffects();
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
		double forwardInput = driver.zza;
		Vec3 input = new Vec3(properties.strafe() ? driver.xxa : 0.0D, 0.0D, forwardInput);
		Vec3 target = Vec3.ZERO;
		if (input.lengthSqr() > 1.0E-7D) {
			double speed = properties.speed();
			if (properties.canSprint() && driver.isSprinting()) speed *= properties.sprintMultiplier();
			if (forwardInput < 0.0D) speed *= properties.reverseMultiplier();
			target = input.normalize().scale(speed).yRot(-this.getYRot() * Mth.DEG_TO_RAD);
		}
		if (this.boostStrength > 0.0D) target = target.add(forward().scale(this.boostStrength));
		Vec3 motion = target;
		double acceleration = properties.acceleration();
		if (acceleration < 1.0D) {
			Vec3 current = this.getDeltaMovement();
			motion = new Vec3(Mth.lerp(acceleration, current.x, target.x), 0.0D, Mth.lerp(acceleration, current.z, target.z));
			if (target.lengthSqr() == 0.0D && motion.horizontalDistanceSqr() < 1.0E-5D) motion = Vec3.ZERO;
		}
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
		if (this.level().isClientSide) {
			NetworkHandler.sendToServer(new VehicleFlightModeC2S(flying));
		} else {
			this.entityData.set(FLIGHT_MODE, flying);
			onFlightModeChanged(flying);
		}
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

	protected Vec3 forward() {
		return new Vec3(0.0D, 0.0D, 1.0D).yRot(-this.getYRot() * Mth.DEG_TO_RAD);
	}

	private void tickClientEffects() {
		double dx = this.getX() - this.xo;
		double dy = this.getY() - this.yo;
		double dz = this.getZ() - this.zo;
		this.clientSpeed = Math.sqrt(dx * dx + dy * dy + dz * dz);
		tickTilt(dx, dy, dz);
		this.hoverBlendO = this.hoverBlend;
		this.hoverBlend = Mth.approach(this.hoverBlend, isFlightMode() ? 1.0F : 0.0F, HOVER_BLEND_SPEED);
		if (this.getControllingPassenger() != null) {
			for (VehicleProperties.Exhaust exhaust : properties.exhausts()) emitSmoke(exhaust, dx, dy, dz);
		}
		tickVisuals(this.clientSpeed);
	}

	private void tickTilt(double dx, double dy, double dz) {
		this.tiltRollO = this.tiltRoll;
		this.tiltPitchO = this.tiltPitch;
		VehicleProperties.Tilt tilt = properties.tilt();
		if (tilt.isNone()) return;
		float yawRate = this.hasLastYaw ? Mth.wrapDegrees(this.getYRot() - this.lastYaw) : 0.0F;
		this.lastYaw = this.getYRot();
		this.hasLastYaw = true;

		float targetRoll = 0.0F;
		float targetPitch = 0.0F;
		if (isFlightMode() && this.getControllingPassenger() != null) {
			double top = Math.max(properties.topSpeed(), 1.0E-3D);
			Vec3 forward = forward();
			float speedFactor = (float) Mth.clamp(Math.sqrt(dx * dx + dz * dz) / top, 0.0D, 1.0D);
			float forwardFactor = (float) Mth.clamp((dx * forward.x + dz * forward.z) / top, -1.0D, 1.0D);
			float climbFactor = (float) Mth.clamp(dy / Math.max(properties.verticalSpeed(), 1.0E-3D), -1.0D, 1.0D);
			targetRoll = Mth.clamp(yawRate / FULL_BANK_YAW_RATE, -1.0F, 1.0F) * tilt.bank() * Math.min(1.0F, speedFactor * 2.0F);
			targetPitch = forwardFactor * tilt.lean() - climbFactor * tilt.climb();
		}
		this.tiltRoll += (targetRoll - this.tiltRoll) * TILT_SMOOTHING;
		this.tiltPitch += (targetPitch - this.tiltPitch) * TILT_SMOOTHING;
	}

	private void emitSmoke(VehicleProperties.Exhaust exhaust, double dx, double dy, double dz) {
		VehicleProperties.Smoke smoke = exhaust.smoke();
		float intensity = smoothstep(smoke.minSpeed(), smoke.fullSpeed(), this.clientSpeed);
		if (intensity <= 0.0F) return;
		Vec3 now = localToWorld(applyVisualTransform(exhaust.position(), 1.0F));
		Vec3 before = now.subtract(dx, dy, dz);
		int steps = Mth.clamp(Mth.ceil(now.distanceTo(before) / SMOKE_SPACING), 1, MAX_SMOKE_STEPS);
		float expected = smoke.density() * intensity;
		float size = smoke.size() * (0.7F + 0.6F * intensity);
		float alpha = smoke.alpha() * (0.55F + 0.45F * intensity);
		int lifetime = Math.round(smoke.lifetime() * (0.75F + 0.5F * intensity));
		for (int step = 0; step < steps; step++) {
			int count = Mth.floor(expected) + (this.random.nextFloat() < expected - Mth.floor(expected) ? 1 : 0);
			for (int i = 0; i < count; i++) {
				double t = (step + this.random.nextDouble()) / steps;
				double spread = size * 0.15D;
				this.level().addParticle(
						new VehicleSmokeOptions(size * (0.85F + 0.3F * this.random.nextFloat()), smoke.shade(), alpha, lifetime),
						Mth.lerp(t, before.x, now.x) + (this.random.nextDouble() - 0.5D) * spread,
						Mth.lerp(t, before.y, now.y) + (this.random.nextDouble() - 0.5D) * spread,
						Mth.lerp(t, before.z, now.z) + (this.random.nextDouble() - 0.5D) * spread,
						-dx * 0.12D + (this.random.nextDouble() - 0.5D) * 0.02D,
						-dy * 0.12D + 0.01D,
						-dz * 0.12D + (this.random.nextDouble() - 0.5D) * 0.02D);
			}
		}
	}

	protected static float smoothstep(double edge0, double edge1, double value) {
		double t = Mth.clamp((value - edge0) / Math.max(edge1 - edge0, 1.0E-6D), 0.0D, 1.0D);
		return (float) (t * t * (3.0D - 2.0D * t));
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
	public AABB getBoundingBoxForCulling() {
		AABB box = super.getBoundingBoxForCulling();
		VehicleModelBones.Extents extents = properties.extents();
		if (extents == null) return box;
		if (this.cullExtents == null) {
			double radius = 0.0D;
			double minY = Double.MAX_VALUE;
			double maxY = -Double.MAX_VALUE;
			for (int corner = 0; corner < 8; corner++) {
				Vec3 local = properties.modelToLocal(
						(corner & 1) != 0 ? extents.max().x : extents.min().x,
						(corner & 2) != 0 ? extents.max().y : extents.min().y,
						(corner & 4) != 0 ? extents.max().z : extents.min().z);
				radius = Math.max(radius, Math.sqrt(local.x * local.x + local.z * local.z));
				minY = Math.min(minY, local.y);
				maxY = Math.max(maxY, local.y);
			}
			this.cullExtents = new double[]{radius + CULL_MARGIN, minY - CULL_MARGIN, maxY + CULL_MARGIN};
		}
		return box.minmax(new AABB(
				this.getX() - this.cullExtents[0], this.getY() + this.cullExtents[1], this.getZ() - this.cullExtents[0],
				this.getX() + this.cullExtents[0], this.getY() + this.cullExtents[2], this.getZ() + this.cullExtents[0]));
	}

	@Override
	public boolean canSprint() {
		return properties.canSprint();
	}

	@Override
	public LivingEntity getControllingPassenger() {
		int driverId = resolveSeats()[0];
		if (driverId == EMPTY_SEAT) return null;
		for (Entity passenger : this.getPassengers()) {
			if (passenger.getId() == driverId) return passenger instanceof Player player ? player : null;
		}
		return null;
	}

	public int getSeatIndex(Entity passenger) {
		return indexOf(resolveSeats(), passenger.getId());
	}

	public boolean isSeatFree(int seat) {
		return seat >= 0 && seat < properties.seatCount() && resolveSeats()[seat] == EMPTY_SEAT;
	}

	public boolean moveToSeat(Player player, int seat) {
		if (this.level().isClientSide || !this.hasPassenger(player) || !isSeatFree(seat)) return false;
		int[] seats = resolveSeats();
		int current = indexOf(seats, player.getId());
		if (current >= 0) seats[current] = EMPTY_SEAT;
		seats[seat] = player.getId();
		storeSeats(seats);
		if (current == 0 || seat == 0) resetDriverState();
		return true;
	}

	private int[] resolveSeats() {
		int count = properties.seatCount();
		int[] seats = new int[count];
		Arrays.fill(seats, EMPTY_SEAT);
		List<Entity> passengers = this.getPassengers();
		int[] stored = this.entityData.get(SEAT_OCCUPANTS).getIntArray(SEATS_KEY);
		for (int i = 0; i < Math.min(stored.length, count); i++) {
			int id = stored[i];
			if (id != EMPTY_SEAT && indexOf(seats, id) < 0 && hasPassengerId(passengers, id)) seats[i] = id;
		}
		for (Entity passenger : passengers) {
			if (indexOf(seats, passenger.getId()) >= 0) continue;
			int free = indexOf(seats, EMPTY_SEAT);
			if (free < 0) break;
			seats[free] = passenger.getId();
		}
		return seats;
	}

	private void storeSeats(int[] seats) {
		CompoundTag tag = new CompoundTag();
		tag.putIntArray(SEATS_KEY, seats);
		this.entityData.set(SEAT_OCCUPANTS, tag);
	}

	private static boolean hasPassengerId(List<Entity> passengers, int id) {
		for (Entity passenger : passengers) {
			if (passenger.getId() == id) return true;
		}
		return false;
	}

	private static int indexOf(int[] values, int value) {
		for (int i = 0; i < values.length; i++) {
			if (values[i] == value) return i;
		}
		return -1;
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return this.getPassengers().size() < properties.seatCount();
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		int index = Mth.clamp(getSeatIndex(passenger), 0, properties.seatCount() - 1);
		Vec3 seat = properties.seats().get(index);
		// Upstream seat origins include the 1.20 player offsets: hip - 0.6 + 0.35.
		// Convert back to a hip anchor before 1.21 subtracts the rider attachment.
		if (passenger instanceof Player) seat = seat.add(0.0D, PLAYER_SEAT_OFFSET, 0.0D);
		if (this.level().isClientSide) seat = applyVisualTransform(seat, 1.0F);
		return seat.yRot(-this.getYRot() * Mth.DEG_TO_RAD);
	}

	@Override
	protected void positionRider(Entity passenger, MoveFunction callback) {
		if (!this.hasPassenger(passenger)) return;
		super.positionRider(passenger, callback);
		if (passenger instanceof LivingEntity living) living.yBodyRot = this.getYRot();
	}

	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		if (!properties.dismountBeside()) return super.getDismountLocationForPassenger(passenger);
		Vec3 forward = forward();
		Vec3 left = new Vec3(forward.z, 0.0D, -forward.x);
		double relX = passenger.getX() - this.getX();
		double relZ = passenger.getZ() - this.getZ();
		double halfWidth = this.getBbWidth() * 0.5D;
		double along = Mth.clamp(relX * forward.x + relZ * forward.z, -halfWidth, halfWidth);
		double sideSign = relX * left.x + relZ * left.z >= 0.0D ? 1.0D : -1.0D;
		double reach = halfWidth + passenger.getBbWidth() * 0.5D + DISMOUNT_GAP;
		Vec3[] candidates = {
				left.scale(sideSign * reach).add(forward.scale(along)),
				left.scale(-sideSign * reach).add(forward.scale(along)),
				forward.scale(-reach),
				forward.scale(reach)
		};
		for (Vec3 candidate : candidates) {
			Vec3 spot = findDismountSpot(passenger, this.getX() + candidate.x, this.getZ() + candidate.z);
			if (spot != null) return spot;
		}
		return super.getDismountLocationForPassenger(passenger);
	}

	@Nullable
	private Vec3 findDismountSpot(LivingEntity passenger, double x, double z) {
		for (int dy : DISMOUNT_HEIGHTS) {
			BlockPos pos = BlockPos.containing(x, this.getY() + dy, z);
			double floor = this.level().getBlockFloorHeight(pos);
			if (!DismountHelper.isBlockFloorValid(floor)) continue;
			Vec3 spot = new Vec3(x, pos.getY() + floor, z);
			for (Pose pose : passenger.getDismountPoses()) {
				if (DismountHelper.canDismountTo(this.level(), spot, passenger, pose)) {
					passenger.setPose(pose);
					return spot;
				}
			}
		}
		Vec3 midair = new Vec3(x, this.getY(), z);
		return DismountHelper.canDismountTo(this.level(), midair, passenger, Pose.STANDING) ? midair : null;
	}

	@Override
	protected void addPassenger(Entity passenger) {
		super.addPassenger(passenger);
		if (!this.level().isClientSide) storeSeats(resolveSeats());
		playBoardSound();
	}

	@Override
	protected void removePassenger(Entity passenger) {
		boolean wasDriver = passenger == this.getControllingPassenger();
		super.removePassenger(passenger);
		if (!this.level().isClientSide) storeSeats(resolveSeats());
		playBoardSound();
		if (wasDriver) resetDriverState();
	}

	private void resetDriverState() {
		this.inputAscend = this.inputDescend = this.inputBoost = false;
		this.lastAscend = this.lastBoost = false;
		this.liftoffTicks = 0;
		this.boostTicks = 0;
		this.boostStrength = 0.0D;
	}

	private void playBoardSound() {
		Supplier<SoundEvent> sound = properties.boardSound();
		if (sound == null || this.level().isClientSide || !this.isAlive()) return;
		this.level().playSound(null, this.getX(), this.getY(), this.getZ(), sound.get(), SoundSource.NEUTRAL, 0.7F, 0.9F + this.random.nextFloat() * 0.2F);
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
	protected MovementEmission getMovementEmission() {
		return MovementEmission.EVENTS;
	}

	@Override
	protected int decreaseAirSupply(int air) {
		return air;
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
