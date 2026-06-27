package com.dragonminez.server.world.raid;

import lombok.Getter;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Getter
public class RaidWave {

	/** A group of identical mobs to spawn in this wave. */
	public record SpawnEntry(Supplier<? extends EntityType<?>> type, int count) {}

	private final List<SpawnEntry> spawns;
	private final double healthMultiplier;
	private final double damageMultiplier;
	private final boolean bossWave;

	private RaidWave(List<SpawnEntry> spawns, double healthMultiplier, double damageMultiplier, boolean bossWave) {
		this.spawns = spawns;
		this.healthMultiplier = healthMultiplier;
		this.damageMultiplier = damageMultiplier;
		this.bossWave = bossWave;
	}

	/** Total number of mobs this wave will attempt to spawn. */
	public int totalMobCount() {
		int total = 0;
		for (SpawnEntry entry : spawns) total += entry.count();
		return total;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private final List<SpawnEntry> spawns = new ArrayList<>();
		private double healthMultiplier = 1.0;
		private double damageMultiplier = 1.0;
		private boolean bossWave = false;

		/** Adds {@code count} mobs of the given type to this wave. */
		public Builder add(Supplier<? extends EntityType<?>> type, int count) {
			this.spawns.add(new SpawnEntry(type, count));
			return this;
		}

		/** Scales the max health (and battle power, where applicable) of every mob in the wave. */
		public Builder health(double multiplier) {
			this.healthMultiplier = multiplier;
			return this;
		}

		/** Scales the attack damage of every mob in the wave. */
		public Builder damage(double multiplier) {
			this.damageMultiplier = multiplier;
			return this;
		}

		/** Marks this as a boss wave (changes the boss bar styling; usually the final wave). */
		public Builder boss(boolean bossWave) {
			this.bossWave = bossWave;
			return this;
		}

		public RaidWave build() {
			if (spawns.isEmpty()) {
				throw new IllegalStateException("A raid wave must contain at least one spawn entry");
			}
			return new RaidWave(List.copyOf(spawns), healthMultiplier, damageMultiplier, bossWave);
		}
	}
}
