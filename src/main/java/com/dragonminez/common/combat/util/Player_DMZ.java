package com.dragonminez.common.combat.util;

import com.dragonminez.common.combat.player.AttackHand;
import org.jetbrains.annotations.Nullable;

public interface Player_DMZ {
    @Nullable
    AttackHand getCurrentAttack();
    boolean rollAndGetCriticalStatus(double chance);
}
