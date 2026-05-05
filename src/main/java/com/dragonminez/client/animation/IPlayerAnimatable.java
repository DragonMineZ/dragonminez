package com.dragonminez.client.animation;

public interface IPlayerAnimatable {

	void dragonminez$setFlying(boolean flying);
	boolean dragonminez$isFlying();

	void dragonminez$triggerDash(int direction);
	void dragonminez$triggerEvasion();

	void dragonminez$setShootingKi(boolean shootingKi);
	boolean dragonminez$isShootingKi();

	void dragonminez$playMeleeAnimation(String animationName, boolean isOffhand, float speedMultiplier);

	void dragonminez$playKiAnimation(String animationName);
	void dragonminez$stopKiAnimation();
}