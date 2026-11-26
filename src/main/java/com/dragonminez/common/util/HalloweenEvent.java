package com.dragonminez.common.util;

import java.time.LocalDate;
import java.time.Month;

public final class HalloweenEvent {

    private static final boolean FORCE_ACTIVE = false;

    private HalloweenEvent() {}

    public static boolean isActive() {
        if (FORCE_ACTIVE) return true;
        LocalDate today = LocalDate.now();
        return today.getMonth() == Month.OCTOBER && today.getDayOfMonth() == 31;
    }
}
