package com.dragonminez.common.init.entities;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.vehicle.VehicleEntity;
import com.dragonminez.common.init.entities.vehicle.VehicleProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animation.AnimatableManager;

public class CarEntity extends VehicleEntity {

	public enum Color {
		RED("car_red"),
		YELLOW("car_yellow");

		private final String texture;

		Color(String texture) {
			this.texture = texture;
		}

		public String texture() {
			return texture;
		}

		public static Color byId(int id) {
			Color[] values = values();
			return values[Mth.clamp(id, 0, values.length - 1)];
		}
	}

	private static final VehicleProperties.Smoke EXHAUST = new VehicleProperties.Smoke(0.32F, 0.4F, 0.6F, 30, 0.75F, 0.04D, 0.75D);

	private static final VehicleProperties PROPERTIES = VehicleProperties.builder(VehicleProperties.Mobility.GROUND)
			.speed(0.45D)
			.sprint(1.7D)
			.reverse(0.4D)
			.acceleration(0.12D)
			.noStrafe()
			.model("vehicles/car", 1.0F)
			.seatBones(2)
			.fallbackSeat(5.5D, 8.5D, 2.0D)
			.fallbackSeat(-5.5D, 8.5D, 2.0D)
			.exhaust(EXHAUST, -6.0D, 4.75D, 28.5D)
			.exhaust(EXHAUST, 6.0D, 4.75D, 28.5D)
			.engine(MainSounds.VEHICLE_CAR_ENGINE, 0.2F, 0.85F, 0.65F, 1.3F)
			.boardSound(MainSounds.VEHICLE_DOOR)
			.dismountBeside()
			.build();

	private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);
	private static final String COLOR_TAG = "CarColor";
	private static final double WHEEL_RADIUS = 5.0D / 16.0D;
	private static final float MAX_STEER = 28.0F;
	private static final float STEER_PER_YAW = 3.0F;
	private static final Vec3[] REAR_WHEELS = {
			PROPERTIES.modelToLocal(-13.75D, 0.0D, 17.0D),
			PROPERTIES.modelToLocal(13.75D, 0.0D, 17.0D)
	};

	private float wheelSpin;
	private float wheelSpinO;
	private float steer;
	private float steerO;
	private float lastYaw;
	private boolean hasYaw;

	public CarEntity(EntityType<? extends VehicleEntity> type, Level level) {
		super(type, level, PROPERTIES);
	}

	public static CarEntity create(Level level, Color color) {
		CarEntity car = new CarEntity(MainEntities.CAR.get(), level);
		car.setColor(color);
		return car;
	}

	public Color getColor() {
		return Color.byId(this.entityData.get(COLOR));
	}

	public void setColor(Color color) {
		this.entityData.set(COLOR, color.ordinal());
	}

	public float getWheelSpin(float partialTick) {
		return Mth.lerp(partialTick, wheelSpinO, wheelSpin);
	}

	public float getSteer(float partialTick) {
		return Mth.lerp(partialTick, steerO, steer);
	}

	@Override
	protected Item getVehicleItem() {
		return getColor() == Color.YELLOW ? MainItems.CAR_YELLOW_ITEM.get() : MainItems.CAR_RED_ITEM.get();
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(COLOR, Color.RED.ordinal());
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt(COLOR_TAG, getColor().ordinal());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setColor(Color.byId(tag.getInt(COLOR_TAG)));
	}

	@Override
	protected void tickVisuals(double speed) {
		Vec3 forward = forward();
		double forwardSpeed = (this.getX() - this.xo) * forward.x + (this.getZ() - this.zo) * forward.z;
		this.wheelSpinO = this.wheelSpin;
		this.wheelSpin += (float) (forwardSpeed / WHEEL_RADIUS) * Mth.RAD_TO_DEG;
		if (Math.abs(this.wheelSpin) > 360.0F) {
			float wrap = Math.copySign(360.0F, this.wheelSpin);
			this.wheelSpin -= wrap;
			this.wheelSpinO -= wrap;
		}

		float yawRate = this.hasYaw ? Mth.wrapDegrees(this.getYRot() - this.lastYaw) : 0.0F;
		this.lastYaw = this.getYRot();
		this.hasYaw = true;
		float targetSteer = this.getControllingPassenger() != null ? Mth.clamp(yawRate * STEER_PER_YAW, -MAX_STEER, MAX_STEER) : 0.0F;
		this.steerO = this.steer;
		this.steer += (targetSteer - this.steer) * 0.25F;

		if (this.onGround() && speed > 0.3D) spawnWheelDust(speed);
	}

	private void spawnWheelDust(double speed) {
		float chance = smoothstep(0.3D, 0.75D, speed) * 0.8F;
		for (Vec3 wheel : REAR_WHEELS) {
			if (this.random.nextFloat() >= chance) continue;
			Vec3 world = localToWorld(wheel);
			BlockPos below = BlockPos.containing(world.x, this.getY() - 0.2D, world.z);
			BlockState state = this.level().getBlockState(below);
			if (state.getRenderShape() == RenderShape.INVISIBLE) continue;
			this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state).setPos(below),
					world.x + (this.random.nextDouble() - 0.5D) * 0.3D, this.getY() + 0.1D, world.z + (this.random.nextDouble() - 0.5D) * 0.3D,
					-(this.getX() - this.xo) * 0.4D, 0.12D, -(this.getZ() - this.zo) * 0.4D);
		}
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
	}
}
