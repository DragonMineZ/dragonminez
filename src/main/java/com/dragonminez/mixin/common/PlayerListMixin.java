package com.dragonminez.mixin.common;

import com.dragonminez.common.util.IHealthFixable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Optional;

@Mixin(PlayerList.class)
public class PlayerListMixin {

	@Inject(
			method = "respawn(Lnet/minecraft/server/level/ServerPlayer;Z)Lnet/minecraft/server/level/ServerPlayer;",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setHealth(F)V"),
			locals = LocalCapture.CAPTURE_FAILSOFT,
			require = 0
	)
	private void dragonminez$onPlayerRespawn(
			ServerPlayer oldPlayer,
			boolean fromEnd,
			CallbackInfoReturnable<ServerPlayer> callback,
			BlockPos respawnPos,
			float respawnAngle,
			boolean wasForced,
			ServerLevel oldDimension,
			Optional<Vec3> calculatedPos,
			ServerLevel overworld,
			ServerPlayer newPlayer
	) {
		if (newPlayer instanceof IHealthFixable fixable) {
			fixable.dragonminez$setHealthRestorePoint(oldPlayer.getMaxHealth());
		}
	}
}

