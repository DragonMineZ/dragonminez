package com.dragonminez.client.animation;

public interface IPlayerAnimatable {
  int CHARGE_ATTACK_NONE = 0;
  int CHARGE_ATTACK_LIGHT = 1;
  int CHARGE_ATTACK_HEAVY = 2;

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

	void dragonminez$triggerMeleeAttack(int variant);

  void dragonminez$setChargeAttackState(int attackType, boolean charging, boolean charged);

  void dragonminez$triggerChargeAttackFire(int attackType, boolean charged);

  void dragonminez$clearChargeAttackState();
}

