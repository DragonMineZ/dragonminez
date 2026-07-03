package com.dragonminez.common.quest.objectives;

/**
 * A non-lethal KILL variant: defeat the sparring partner without killing them. Shares all
 * KillObjective fields, spawning, scaling, and crediting; the finishing blow is intercepted
 * server-side (see QuestEvents.onSparHurt) — the partner concedes and departs instead of dying.
 * JSON is identical to KILL with {@code "type": "SPAR"}.
 */
public class SparObjective extends KillObjective {

	public SparObjective(String entityId, int count, double health, double meleeDamage, double kiDamage,
						 SpawnMode spawnMode, CountMode countMode, int textureVariant, int aiTier, boolean canTransform) {
		super(entityId, count, health, meleeDamage, kiDamage, spawnMode, countMode, textureVariant, aiTier, canTransform);
	}

	@Override
	public String getTypeKey() {
		return "SPAR";
	}
}
