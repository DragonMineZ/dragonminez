package com.dragonminez.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldSelectionList.WorldListEntry.class)
public class WorldSelectionListMixin {
	@Inject(method = "renderExperimentalWarning", at = @At("HEAD"), cancellable = true, remap = false)
	private void renderExperimentalWarning(GuiGraphics stack, int mouseX, int mouseY, int top, int left, CallbackInfo ci) {
		ci.cancel();
	}
}
