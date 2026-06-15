package com.dragonminez.server.world.raid;

import lombok.Getter;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
public class RaidType {

	private final String id;
	private final Component displayName;
	private final List<RaidWave> waves;
	/** Players within this many blocks of the centre are pulled into the raid / boss bar. */
	private final double activationRadius;
	/** A participant beyond this distance from the centre is treated as having left the raid. */
	private final double leashDistance;
	/** Ticks to wait between a wave being cleared and the next one spawning. */
	private final int interWaveDelayTicks;
	private final RaidReward reward;

	private RaidType(Builder builder) {
		this.id = builder.id;
		this.displayName = builder.displayName;
		this.waves = builder.waves;
		this.activationRadius = builder.activationRadius;
		this.leashDistance = builder.leashDistance;
		this.interWaveDelayTicks = builder.interWaveDelayTicks;
		this.reward = builder.reward;
	}

	public int waveCount() {
		return waves.size();
	}

	public RaidWave wave(int index) {
		return waves.get(index);
	}

	public boolean isFinalWave(int index) {
		return index == waves.size() - 1;
	}

	public static Builder builder(String id) {
		return new Builder(id);
	}

	public static class Builder {
		private final String id;
		private Component displayName;
		private final List<RaidWave> waves = new ArrayList<>();
		private double activationRadius = 48.0;
		private double leashDistance = 64.0;
		private int interWaveDelayTicks = 100; // 5 seconds
		private RaidReward reward = RaidReward.builder().build();

		private Builder(String id) {
			this.id = id;
			this.displayName = Component.literal(id);
		}

		public Builder name(Component displayName) {
			this.displayName = displayName;
			return this;
		}

		public Builder wave(RaidWave wave) {
			this.waves.add(wave);
			return this;
		}

		public Builder activationRadius(double radius) {
			this.activationRadius = radius;
			return this;
		}

		public Builder leashDistance(double distance) {
			this.leashDistance = distance;
			return this;
		}

		public Builder interWaveDelay(int ticks) {
			this.interWaveDelayTicks = ticks;
			return this;
		}

		public Builder reward(RaidReward reward) {
			this.reward = reward;
			return this;
		}

		public RaidType build() {
			if (waves.isEmpty()) {
				throw new IllegalStateException("Raid type '" + id + "' must have at least one wave");
			}
			return new RaidType(this);
		}
	}
}
