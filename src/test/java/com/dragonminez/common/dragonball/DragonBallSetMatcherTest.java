package com.dragonminez.common.dragonball;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DragonBallSetMatcherTest {
    @Test
    void acceptsASetWithFewerThanSevenDeclaredStarsWhenAllArePresent() {
        assertTrue(DragonBallSetMatcher.containsAllRequiredStars(Set.of(1, 2, 3), Set.of(1, 2, 3)));
    }

    @Test
    void rejectsASetWhenOneDeclaredStarIsMissing() {
        assertFalse(DragonBallSetMatcher.containsAllRequiredStars(Set.of(1, 2, 3), Set.of(1, 2)));
    }

    @Test
    void rejectsAnEmptyDefinition() {
        assertFalse(DragonBallSetMatcher.containsAllRequiredStars(Set.of(), Set.of(1, 2, 3)));
    }
}
