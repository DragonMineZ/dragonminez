package com.dragonminez.mixin.client;

import com.dragonminez.common.combat.util.Minecraft_DMZ;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class InGameHudMixin {
	@Inject(method = "renderCrosshair", at = @At("HEAD"))
	private void dragonminez$pre_renderCrosshair(GuiGraphics guiGraphics, CallbackInfo ci) {
		if (((Minecraft_DMZ) Minecraft.getInstance()).hasTargetsInReach()) RenderSystem.setShaderColor(1.0F, 0.0F, 0.0F, 1.0F);
	}

	@Inject(method = "renderCrosshair", at = @At("TAIL"))
	private void dragonminez$post_renderCrosshair(GuiGraphics guiGraphics, CallbackInfo ci) {
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}
}