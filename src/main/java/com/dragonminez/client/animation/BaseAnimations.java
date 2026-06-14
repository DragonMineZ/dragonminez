package com.dragonminez.client.animation;

import software.bernie.geckolib.core.animation.RawAnimation;

public class BaseAnimations {
	public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("base.idle");
	public static final RawAnimation IDLE_OOZARU = RawAnimation.begin().thenLoop("base.idle_ozaru");
	public static final RawAnimation WALK = RawAnimation.begin().thenLoop("base.walk");
	public static final RawAnimation WALK_OOZARU = RawAnimation.begin().thenLoop("base.walk_ozaru");
	public static final RawAnimation RUN = RawAnimation.begin().thenLoop("base.run");
	public static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("base.attack1");
	public static final RawAnimation ATTACK2 = RawAnimation.begin().thenPlay("base.attack2");
	public static final RawAnimation FLY = RawAnimation.begin().thenLoop("base.fly");
	public static final RawAnimation FLY_FAST = RawAnimation.begin().thenLoop("base.fly_fast");
	public static final RawAnimation JUMP = RawAnimation.begin().thenPlay("base.jump");
	public static final RawAnimation SWIMMING = RawAnimation.begin().thenLoop("base.swimming");
	public static final RawAnimation CROUCHING = RawAnimation.begin().thenLoop("base.crouching");
	public static final RawAnimation CROUCHING_WALK = RawAnimation.begin().thenLoop("base.crouching_walk");
	public static final RawAnimation SHIELD_RIGHT = RawAnimation.begin().thenLoop("base.shield_right");
	public static final RawAnimation SHIELD_LEFT = RawAnimation.begin().thenLoop("base.shield_left");
	public static final RawAnimation CRAWLING = RawAnimation.begin().thenLoop("base.crawling");
	public static final RawAnimation CRAWLING_MOVE = RawAnimation.begin().thenLoop("base.crawling_move");
	public static final RawAnimation TAIL = RawAnimation.begin().thenLoop("base.tail");
	public static final RawAnimation BLOCK = RawAnimation.begin().thenPlay("base.block");
	public static final RawAnimation DRAIN = RawAnimation.begin().thenPlay("base.absorb");
	public static final RawAnimation MINING1 = RawAnimation.begin().thenPlay("base.mining1");
	public static final RawAnimation MINING2 = RawAnimation.begin().thenPlay("base.mining2");
	public static final RawAnimation KI_CHARGE = RawAnimation.begin().thenLoop("base.ki_charge");
	public static final RawAnimation TRANSFORMATION = RawAnimation.begin().thenLoop("transf.generic");
	public static final RawAnimation OOZARU_TRANSFORMATION = RawAnimation.begin().thenLoop("transf.ozaru");
	public static final RawAnimation ABSORB = RawAnimation.begin().thenLoop("transf.absorb");
	public static final RawAnimation DASH_FORWARD = RawAnimation.begin().thenPlay("base.dash_front");
	public static final RawAnimation DASH_BACKWARD = RawAnimation.begin().thenPlay("base.evasion_back");
	public static final RawAnimation DOUBLEDASH_BACKWARD = RawAnimation.begin().thenPlay("base.dash_back");
	public static final RawAnimation DASH_LEFT = RawAnimation.begin().thenPlay("base.evasion_left");
	public static final RawAnimation DOUBLEDASH_LEFT = RawAnimation.begin().thenPlay("base.dash_left");
	public static final RawAnimation DASH_RIGHT = RawAnimation.begin().thenPlay("base.evasion_right");
	public static final RawAnimation DOUBLEDASH_RIGHT = RawAnimation.begin().thenPlay("base.dash_right");
	public static final RawAnimation EVASION_FRONT = RawAnimation.begin().thenPlay("base.dodge_front");
	public static final RawAnimation EVASION_BACK = RawAnimation.begin().thenPlay("base.dodge_back");
	public static final RawAnimation EVASION_LEFT = RawAnimation.begin().thenPlay("base.dodge_left");
	public static final RawAnimation EVASION_RIGHT = RawAnimation.begin().thenPlay("base.dodge_right");
	public static final RawAnimation SIT = RawAnimation.begin().thenLoop("base.sit");
	public static final RawAnimation FLEX = RawAnimation.begin().thenPlay("base.flex");
	public static final RawAnimation MEDITATION = RawAnimation.begin().thenLoop("base.meditation");
	public static final RawAnimation FLYBACK = RawAnimation.begin().thenPlay("base.flyback");
	public static final RawAnimation KNOCKBACK_HORIZONTAL = RawAnimation.begin().thenPlay("base.faint_horizontal");
	public static final RawAnimation KNOCKBACK_VERTICAL = RawAnimation.begin().thenPlay("base.faint_vertical");
}