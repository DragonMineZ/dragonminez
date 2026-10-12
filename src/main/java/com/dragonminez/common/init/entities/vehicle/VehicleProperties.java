package com.dragonminez.common.init.entities.vehicle;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record VehicleProperties(Mobility mobility, double speed, double sprintMultiplier, double verticalSpeed,
								float stepHeight, List<Vec3> seats, @Nullable Boost boost) {

	public enum Mobility {
		GROUND,
		FLYING,
		HYBRID
	}

	public record Boost(int duration, double initial, double sustain) {
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

	public static Builder builder(Mobility mobility) {
		return new Builder(mobility);
	}

	public static final class Builder {
		private final Mobility mobility;
		private double speed = 0.5D;
		private double sprintMultiplier = 1.0D;
		private double verticalSpeed = 0.35D;
		private float stepHeight;
		private final List<Vec3> seats = new ArrayList<>();
		private Boost boost;

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

		public Builder verticalSpeed(double blocksPerTick) {
			this.verticalSpeed = blocksPerTick;
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

		public Builder boost(int duration, double initial, double sustain) {
			this.boost = new Boost(duration, initial, sustain);
			return this;
		}

		public VehicleProperties build() {
			if (seats.isEmpty()) seats.add(new Vec3(0.0D, 0.5D, 0.0D));
			return new VehicleProperties(mobility, speed, sprintMultiplier, verticalSpeed, stepHeight, List.copyOf(seats), boost);
		}
	}
}
