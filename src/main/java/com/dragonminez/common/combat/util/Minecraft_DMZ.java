package com.dragonminez.common.combat.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

public interface Minecraft_DMZ {
    int getComboCount();
    boolean hasTargetsInReach();

    @Nullable
    default Entity getCursorTarget() {
        Minecraft client = (Minecraft) this;
        if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.ENTITY) return ((EntityHitResult) client.hitResult).getEntity();
        return null;
    }

    int getUpswingTicks();
    float getSwingProgress();

    default boolean isWeaponSwingInProgress() {
        return getSwingProgress() < 1.0F;
    }

    void cancelUpswing();
}