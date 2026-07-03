package com.dragonminez.common.quest.objectives;

import com.dragonminez.common.quest.QuestObjective;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Defeat successive waves of enemies (progress = waves cleared). Waves spawn around the quest
 * controller once the objective is unlocked; dying to a wave fails the quest like any combat
 * quest. Runtime wave state lives in QuestFieldSessions. JSON:
 * {@code {"type": "SURVIVE_WAVES", "entity": "dragonminez:...", "waves": 3, "mobs_per_wave": 4,
 * "wave_delay_seconds": 10, "health": 60, "meleeDamage": 5, "kiDamage": 5, "AITier": 2}}
 */
@Getter
public class SurviveWavesObjective extends QuestObjective {

	private final String entityId;
	private final int waves;
	private final int mobsPerWave;
	private final int waveDelaySeconds;
	private final double health;
	private final double meleeDamage;
	private final double kiDamage;
	private final int textureVariant;
	private final int aiTier;
	private final boolean canTransform;

	public SurviveWavesObjective(String entityId, int waves, int mobsPerWave, int waveDelaySeconds,
								 double health, double meleeDamage, double kiDamage,
								 int textureVariant, int aiTier, boolean canTransform) {
		super(ObjectiveType.SURVIVE_WAVES, Math.max(1, waves));
		this.entityId = entityId;
		this.waves = Math.max(1, waves);
		this.mobsPerWave = Math.max(1, mobsPerWave);
		this.waveDelaySeconds = Math.max(1, waveDelaySeconds);
		this.health = health;
		this.meleeDamage = meleeDamage;
		this.kiDamage = kiDamage;
		this.textureVariant = textureVariant;
		this.aiTier = aiTier;
		this.canTransform = canTransform;
	}

	@Nullable
	public EntityType<?> resolveEntityType() {
		try {
			return ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(entityId));
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public boolean checkProgress(Object... params) {
		return isCompleted();
	}
}
