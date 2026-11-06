package com.dragonminez.common.init.entities.vehicle;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record VehicleProperties(Mobility mobility, double speed, double sprintMultiplier, double reverseMultiplier,
								double verticalSpeed, double acceleration, boolean strafe, float stepHeight,
								List<Vec3> seats, @Nullable Boost boost, @Nullable String model, float modelScale,
								Vec3 modelOffset, @Nullable VehicleModelBones.Extents extents, List<Exhaust> exhausts,
								Tilt tilt, float hoverBob, @Nullable Engine engine,
								@Nullable Supplier<SoundEvent> boardSound, boolean dismountBeside, double cameraDistance) {

	private static final double PIXEL = 1.0D / 16.0D;
	private static final double SEATED_HIP_HEIGHT = 0.6D;
	private static final double PLAYER_RIDING_OFFSET = -0.35D;

	public enum Mobility {
		GROUND,
		FLYING,
		HYBRID
	}

	public record Boost(int duration, double initial, double sustain) {
	}

	public record Smoke(float size, float shade, float alpha, int lifetime, float density, double minSpeed, double fullSpeed) {
	}

	public record Exhaust(Vec3 position, Smoke smoke) {
	}

	public record Tilt(float bank, float climb, float lean, double pivotHeight) {
		public static final Tilt NONE = new Tilt(0.0F, 0.0F, 0.0F, 0.0D);

		public boolean isNone() {
			return bank == 0.0F && climb == 0.0F && lean == 0.0F;
		}
	}

	public record Engine(Supplier<SoundEvent> sound, float idleVolume, float maxVolume, float minPitch, float maxPitch) {
	}

	public boolean canFly() {
		return mobility != Mobility.GROUND;
	}

	public boolean canSprint() {
		return sprintMultiplier > 1.0D;
	}

	public int seatCount() {
		return seats.size();
	}

	public double topSpeed() {
		return speed * Math.max(1.0D, sprintMultiplier);
	}

	public Vec3 modelToLocal(double x, double y, double z) {
		return new Vec3((x + modelOffset.x) * modelScale * PIXEL, (y + modelOffset.y) * modelScale * PIXEL, -(z + modelOffset.z) * modelScale * PIXEL);
	}

	public static Builder builder(Mobility mobility) {
		return new Builder(mobility);
	}

	public static final class Builder {
		private final Mobility mobility;
		private double speed = 0.5D;
		private double sprintMultiplier = 1.0D;
		private double reverseMultiplier = 1.0D;
		private double verticalSpeed = 0.35D;
		private double acceleration = 1.0D;
		private boolean strafe = true;
		private float stepHeight;
		private final List<Vec3> seats = new ArrayList<>();
		private final List<Vec3> fallbackSeatPixels = new ArrayList<>();
		private int seatBones;
		private Boost boost;
		private String model;
		private float modelScale = 1.0F;
		private Vec3 modelOffset = Vec3.ZERO;
		private final List<Vec3> exhaustPixels = new ArrayList<>();
		private final List<Smoke> exhaustSmokes = new ArrayList<>();
		private Tilt tilt = Tilt.NONE;
		private float hoverBob;
		private Engine engine;
		private Supplier<SoundEvent> boardSound;
		private boolean dismountBeside;
		private double cameraDistance;

		private Builder(Mobility mobility) {
			this.mobility = mobility;
			this.stepHeight = mobility == Mobility.FLYING ? 0.6F : 1.0F;
		}

		public Builder speed(double blocksPerTick) {
			this.speed = blocksPerTick;
			return this;
		}

		public Builder sprint(double multiplier) {
			this.sprintMultiplier = multiplier;
			return this;
		}

		public Builder reverse(double multiplier) {
			this.reverseMultiplier = multiplier;
			return this;
		}

		public Builder verticalSpeed(double blocksPerTick) {
			this.verticalSpeed = blocksPerTick;
			return this;
		}

		public Builder acceleration(double blend) {
			this.acceleration = blend;
			return this;
		}

		public Builder noStrafe() {
			this.strafe = false;
			return this;
		}

		public Builder stepHeight(float blocks) {
			this.stepHeight = blocks;
			return this;
		}

		public Builder seat(double side, double height, double forward) {
			this.seats.add(new Vec3(side, height, forward));
			return this;
		}

		public Builder model(String path, float scale) {
			this.model = path;
			this.modelScale = scale;
			return this;
		}

		public Builder modelOffset(double x, double y, double z) {
			this.modelOffset = new Vec3(x, y, z);
			return this;
		}

		public Builder seatBones(int count) {
			this.seatBones = count;
			return this;
		}

		public Builder fallbackSeat(double x, double y, double z) {
			this.fallbackSeatPixels.add(new Vec3(x, y, z));
			return this;
		}

		public Builder exhaust(Smoke smoke, double x, double y, double z) {
			this.exhaustPixels.add(new Vec3(x, y, z));
			this.exhaustSmokes.add(smoke);
			return this;
		}

		public Builder tilt(float bank, float climb, float lean, double pivotHeight) {
			this.tilt = new Tilt(bank, climb, lean, pivotHeight);
			return this;
		}

		public Builder hoverBob(float blocks) {
			this.hoverBob = blocks;
			return this;
		}

		public Builder engine(Supplier<SoundEvent> sound, float idleVolume, float maxVolume, float minPitch, float maxPitch) {
			this.engine = new Engine(sound, idleVolume, maxVolume, minPitch, maxPitch);
			return this;
		}

		public Builder boardSound(Supplier<SoundEvent> sound) {
			this.boardSound = sound;
			return this;
		}

		public Builder dismountBeside() {
			this.dismountBeside = true;
			return this;
		}

		public Builder cameraDistance(double extraBlocks) {
			this.cameraDistance = extraBlocks;
			return this;
		}

		public Builder boost(int duration, double initial, double sustain) {
			this.boost = new Boost(duration, initial, sustain);
			return this;
		}

		public VehicleProperties build() {
			VehicleModelBones.Layout layout = model != null ? VehicleModelBones.load(model) : VehicleModelBones.Layout.EMPTY;
			VehicleProperties pending = create(List.of(), List.of(), layout.extents());
			List<Vec3> resolvedSeats = new ArrayList<>(seats);
			for (int i = 0; i < seatBones; i++) {
				Vec3 pivot = layout.seat("passenger" + (i + 1));
				if (pivot == null && i < fallbackSeatPixels.size()) pivot = fallbackSeatPixels.get(i);
				if (pivot == null) continue;
				Vec3 hip = pending.modelToLocal(pivot.x, pivot.y, pivot.z);
				resolvedSeats.add(new Vec3(hip.x, hip.y - SEATED_HIP_HEIGHT - PLAYER_RIDING_OFFSET, hip.z));
			}
			if (resolvedSeats.isEmpty()) resolvedSeats.add(new Vec3(0.0D, 0.5D, 0.0D));
			List<Exhaust> exhausts = new ArrayList<>();
			for (int i = 0; i < exhaustPixels.size(); i++) {
				Vec3 pixel = exhaustPixels.get(i);
				exhausts.add(new Exhaust(pending.modelToLocal(pixel.x, pixel.y, pixel.z), exhaustSmokes.get(i)));
			}
			return create(List.copyOf(resolvedSeats), List.copyOf(exhausts), layout.extents());
		}

		private VehicleProperties create(List<Vec3> seatList, List<Exhaust> exhaustList, @Nullable VehicleModelBones.Extents extents) {
			return new VehicleProperties(mobility, speed, sprintMultiplier, reverseMultiplier, verticalSpeed, acceleration, strafe,
					stepHeight, seatList, boost, model, modelScale, modelOffset, extents, exhaustList, tilt, hoverBob, engine,
					boardSound, dismountBeside, cameraDistance);
		}
	}
}
