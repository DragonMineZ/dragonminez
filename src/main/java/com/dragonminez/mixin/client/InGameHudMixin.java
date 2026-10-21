package com.dragonminez.mixin.client;

import com.dragonminez.Reference;
import com.dragonminez.client.render.camera.OverShoulderCamera;
import com.dragonminez.common.combat.util.Minecraft_DMZ;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class InGameHudMixin {
	private static final ResourceLocation DMZ$BLOCK_SHIELD = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/block_shield.png");
	private static final int DMZ$SHIELD_SIZE = 18;

	@Unique
	private boolean dragonminez$crosshairShifted;

	@Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
	private void dragonminez$pre_renderCrosshair(GuiGraphics guiGraphics, CallbackInfo ci) {
		float[] offset = OverShoulderCamera.crosshairOffset(guiGraphics.guiWidth(), guiGraphics.guiHeight());
		this.dragonminez$crosshairShifted = offset[0] != 0.0F || offset[1] != 0.0F;
		if (this.dragonminez$crosshairShifted) {
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(offset[0], offset[1], 0.0F);
		}
		if (dragonminez$isBlocking() && dragonminez$crosshairVisible()) {
			int x = (guiGraphics.guiWidth() - DMZ$SHIELD_SIZE) / 2;
			int y = (guiGraphics.guiHeight() - DMZ$SHIELD_SIZE) / 2;
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			guiGraphics.blit(DMZ$BLOCK_SHIELD, x, y, 0, 0, DMZ$SHIELD_SIZE, DMZ$SHIELD_SIZE, DMZ$SHIELD_SIZE, DMZ$SHIELD_SIZE);
			RenderSystem.disableBlend();
			dragonminez$restoreCrosshairPose(guiGraphics);
			ci.cancel();
			return;
		}
		if (((Minecraft_DMZ) Minecraft.getInstance()).hasTargetsInReach()) RenderSystem.setShaderColor(1.0F, 0.0F, 0.0F, 1.0F);
	}

	@Redirect(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z"))
	private boolean dragonminez$shoulderCrosshair(CameraType cameraType) {
		return cameraType.isFirstPerson() || OverShoulderCamera.isDecoupled();
	}

	@Inject(method = "renderCrosshair", at = @At("TAIL"))
	private void dragonminez$post_renderCrosshair(GuiGraphics guiGraphics, CallbackInfo ci) {
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		dragonminez$restoreCrosshairPose(guiGraphics);
	}

	@Unique
	private void dragonminez$restoreCrosshairPose(GuiGraphics guiGraphics) {
		if (!this.dragonminez$crosshairShifted) return;
		this.dragonminez$crosshairShifted = false;
		guiGraphics.pose().popPose();
	}

	private static boolean dragonminez$isBlocking() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return false;
		return StatsProvider.get(StatsCapability.INSTANCE, player)
				.map(data -> data.getStatus().isBlocking())
				.orElse(false);
	}

	private static boolean dragonminez$crosshairVisible() {
		Minecraft mc = Minecraft.getInstance();
		if (!mc.options.getCameraType().isFirstPerson() && !OverShoulderCamera.isDecoupled()) return false;
		return mc.gameMode == null || mc.gameMode.getPlayerMode() != net.minecraft.world.level.GameType.SPECTATOR;
	}
}
