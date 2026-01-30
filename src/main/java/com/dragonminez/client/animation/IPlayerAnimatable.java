package com.dragonminez.client.animation;

public interface IPlayerAnimatable {

    void dragonminez$setUseAttack2(boolean useAttack2);

    boolean dragonminez$useAttack2();

    void dragonminez$setPlayingAttack(boolean playingAttack);

    boolean dragonminez$isPlayingAttack();

    void dragonminez$setFlying(boolean flying);

    boolean dragonminez$isFlying();

    void dragonminez$triggerDash(int direction);

    void dragonminez$triggerEvasion();

	void dragonminez$setShootingKi(boolean shootingKi);

	boolean dragonminez$isShootingKi();

	void dragonminez$triggerCombo(int comboNumber);
}

