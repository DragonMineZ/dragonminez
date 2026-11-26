package com.dragonminez.mixin.common;

import com.dragonminez.common.util.IHealthFixable;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    /** Preserve the deferred health restore before vanilla sets health, without capturing locals. */
    @WrapOperation(
            method = "respawn(Lnet/minecraft/server/level/ServerPlayer;ZLnet/minecraft/world/entity/Entity$RemovalReason;)Lnet/minecraft/server/level/ServerPlayer;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setHealth(F)V"),
            require = 1
    )
    private void dragonminez$onPlayerRespawn(ServerPlayer newPlayer, float health,
                                            Operation<Void> original, ServerPlayer oldPlayer,
                                            boolean fromEnd, Entity.RemovalReason reason) {
        if (newPlayer instanceof IHealthFixable fixable) {
            fixable.dragonminez$setHealthRestorePoint(oldPlayer.getMaxHealth());
        }
        original.call(newPlayer, health);
    }
}
