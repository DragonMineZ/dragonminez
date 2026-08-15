package com.dragonminez.common.dragonball;

import java.util.Set;

/**
 * Pure matching rules for deciding whether a dragon ball set is complete.
 */
public final class DragonBallSetMatcher {
    private DragonBallSetMatcher() {
    }

    public static boolean containsAllRequiredStars(Set<Integer> requiredStars, Set<Integer> foundStars) {
        return requiredStars != null
                && !requiredStars.isEmpty()
                && foundStars != null
                && foundStars.containsAll(requiredStars);
    }
}
