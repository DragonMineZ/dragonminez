package com.dragonminez.common.init.block.custom;

public enum DragonBallType {
    ONE_STAR(1),
    TWO_STAR(2),
    THREE_STAR(3),
    FOUR_STAR(4),
    FIVE_STAR(5),
    SIX_STAR(6),
    SEVEN_STAR(7);

    private final int stars;

    DragonBallType(int stars) {
        this.stars = stars;
    }

    public int getStars() {
        return stars;
    }

    public String getName() {
        return this.name().toLowerCase();
    }
}

