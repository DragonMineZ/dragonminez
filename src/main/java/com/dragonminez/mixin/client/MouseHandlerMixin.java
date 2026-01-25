package com.dragonminez.mixin.client;

import com.dragonminez.client.events.FlySkillEvent;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

	@Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
	private void dragonminez$redirectTurn(LocalPlayer player, double yawDelta, double pitchDelta) {
		if (FlySkillEvent.isFlyingFast()) {
			float f = (float)pitchDelta * 0.15F;
			float f1 = (float)yawDelta * 0.15F;

			player.setXRot(player.getXRot() + f);
			player.setYRot(player.getYRot() + f1);

			player.setXRot(Mth.wrapDegrees(player.getXRot()));
			player.setYRot(Mth.wrapDegrees(player.getYRot()));

			player.xRotO += f;
			player.yRotO += f1;

		} else {
			player.turn(yawDelta, pitchDelta);
		}
	}
}
