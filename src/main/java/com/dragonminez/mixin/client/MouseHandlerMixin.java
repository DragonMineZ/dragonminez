package com.dragonminez.mixin.client;

import com.dragonminez.client.gui.tooltip.ScrollTracker;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void dragonminez$trackMouseWheel(long pWindowPointer, double pXOffset, double pYOffset, CallbackInfo ci) {
		if (this.minecraft.screen != null && pWindowPointer == this.minecraft.getWindow().getWindow()) {
			if (InputConstants.isKeyDown(pWindowPointer, GLFW.GLFW_KEY_LEFT_ALT)) {
				if (InputConstants.isKeyDown(pWindowPointer, GLFW.GLFW_KEY_LEFT_SHIFT)) ScrollTracker.addHorizontalScroll((int) Math.signum(pYOffset));
				else {
					ScrollTracker.addVerticalScroll((int) Math.signum(pYOffset));
					ScrollTracker.addHorizontalScroll((int) Math.signum(pXOffset));
				}
				ci.cancel();
			}
		}
	}
}

