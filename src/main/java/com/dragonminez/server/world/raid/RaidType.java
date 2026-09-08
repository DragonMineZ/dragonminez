package com.dragonminez.server.world.raid;

import com.dragonminez.common.config.RaidDefinition;
import lombok.Getter;
import net.minecraft.network.chat.Component;

import java.util.List;

@Getter
public class RaidType {

	private static final double DEFAULT_ACTIVATION_RADIUS = 48.0D;
	private static final double DEFAULT_LEASH_DISTANCE = 80.0D;
	private static final int DEFAULT_INTER_WAVE_DELAY_TICKS = 100;
	public static final int DEFAULT_PREPARATION_TICKS = 60 * 20;

	private final String id;
	private final RaidDefinition definition;
	private final Component displayName;
	private final double activationRadius;
	private final double leashDistance;
	private final int interWaveDelayTicks;

	public RaidType(String id, RaidDefinition definition) {
		this.id = id;
		this.definition = definition;
		this.displayName = Component.translatable(definition.displayNameOr("raid.dragonminez." + id));
		this.activationRadius = definition.activationRadiusOr(DEFAULT_ACTIVATION_RADIUS);
		this.leashDistance = definition.leashDistanceOr(DEFAULT_LEASH_DISTANCE);
		this.interWaveDelayTicks = definition.interWaveDelayTicksOr(DEFAULT_INTER_WAVE_DELAY_TICKS);
	}

	public List<RaidDefinition.Wave> getWaves() {
		return definition.getWaves() != null ? definition.getWaves() : List.of();
	}

	public int waveCount() { return getWaves().size(); }

	public RaidDefinition.Wave wave(int index) { return getWaves().get(index); }

	public boolean isFinalWave(int index) { return index == waveCount() - 1; }

	public boolean isEnabled() { return definition.isEnabled(); }

	public RaidDefinition.Trigger getTrigger() { return definition.getTrigger(); }

	public boolean hasTrigger() { return definition.hasTrigger(); }

	public int preparationTicks() {
		return hasTrigger() ? getTrigger().preparationTicksOr(DEFAULT_PREPARATION_TICKS) : DEFAULT_PREPARATION_TICKS;
	}

	public String preparationMusic() { return definition.preparationMusic(); }

	public String battleMusic() { return definition.battleMusic(); }

	public RaidDefinition.Rewards getRewards() { return definition.getRewards(); }

	public boolean isUsable() {
		return isEnabled() && !getWaves().isEmpty();
	}
}
