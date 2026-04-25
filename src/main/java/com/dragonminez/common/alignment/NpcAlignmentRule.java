package com.dragonminez.common.alignment;

import com.dragonminez.common.combat.logic.player.TargetHelper;

public record NpcAlignmentRule(TargetHelper.Relation defaultRelation,
							   Integer minInteractionAlignment,
							   Integer maxInteractionAlignment,
							   Integer hostileBelow,
							   Integer hostileAbove) {
	public NpcAlignmentRule {
		defaultRelation = defaultRelation != null ? defaultRelation : TargetHelper.Relation.NEUTRAL;
	}

	public boolean allowsInteraction(int alignment) {
		if (minInteractionAlignment != null && alignment < minInteractionAlignment) {
			return false;
		}
		return maxInteractionAlignment == null || alignment <= maxInteractionAlignment;
	}

	public boolean isHostileFor(int alignment) {
		if (hostileBelow != null && alignment < hostileBelow) {
			return true;
		}
		return hostileAbove != null && alignment > hostileAbove;
	}
}
