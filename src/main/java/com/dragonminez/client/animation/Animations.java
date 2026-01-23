package com.dragonminez.client.animation;

import software.bernie.geckolib.core.animation.RawAnimation;

public class Animations {
    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.base.idle");
    public static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.base.walk");
    public static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.base.run");
    public static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.base.attack1");
    public static final RawAnimation ATTACK2 = RawAnimation.begin().thenPlay("animation.base.attack2");
    public static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.base.fly");
	public static final RawAnimation FLY_FAST = RawAnimation.begin().thenLoop("animation.base.fly_fast");
    public static final RawAnimation JUMP = RawAnimation.begin().thenPlay("animation.base.jump");
    public static final RawAnimation SWIMMING = RawAnimation.begin().thenLoop("animation.base.swimming");
    public static final RawAnimation CROUCHING = RawAnimation.begin().thenLoop("animation.base.crouching");
    public static final RawAnimation CROUCHING_WALK = RawAnimation.begin().thenLoop("animation.base.crouching_walk");
    public static final RawAnimation SHIELD_RIGHT = RawAnimation.begin().thenLoop("animation.base.shield_right");
    public static final RawAnimation SHIELD_LEFT = RawAnimation.begin().thenLoop("animation.base.shield_left");
    public static final RawAnimation CRAWLING = RawAnimation.begin().thenLoop("animation.base.crawling");
    public static final RawAnimation CRAWLING_MOVE = RawAnimation.begin().thenLoop("animation.base.crawling_move");
    public static final RawAnimation TAIL = RawAnimation.begin().thenLoop("animation.base.tail");
	public static final RawAnimation BLOCK = RawAnimation.begin().thenPlay("animation.base.block");
	public static final RawAnimation DRAIN = RawAnimation.begin().thenPlay("animation.base.absorb");
	public static final RawAnimation MINING1 = RawAnimation.begin().thenPlay("animation.base.mining1");
	public static final RawAnimation MINING2 = RawAnimation.begin().thenPlay("animation.base.mining2");
}
