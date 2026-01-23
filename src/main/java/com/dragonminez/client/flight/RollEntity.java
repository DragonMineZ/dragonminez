package com.dragonminez.client.flight;

public interface RollEntity {
    boolean dragonminez$isRolling();
    float dragonminez$getRoll(float tickDelta);
    void dragonminez$setRoll(float roll);
    void dragonminez$updateRoll(float deltaYaw, boolean isLeft, boolean isRight);
}
