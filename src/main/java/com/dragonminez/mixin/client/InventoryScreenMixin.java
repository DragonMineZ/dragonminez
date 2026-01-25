package com.dragonminez.mixin.client;

import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {
	@Inject(method = "renderEntityInInventory(Lnet/minecraft/client/gui/GuiGraphics;IIILorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"))
	private static void dmz$enableGuiRender(CallbackInfo ci) {
		FirstPersonManager.isRenderingInGui = true;
	}

	@Inject(method = "renderEntityInInventory(Lnet/minecraft/client/gui/GuiGraphics;IIILorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("RETURN"))
	private static void dmz$disableGuiRender(CallbackInfo ci) {
		FirstPersonManager.isRenderingInGui = false;
	}
}
