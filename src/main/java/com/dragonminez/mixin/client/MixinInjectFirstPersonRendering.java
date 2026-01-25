package com.dragonminez.mixin.client;

import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinInjectFirstPersonRendering {

    //@Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    //private void onRenderHand(PoseStack pPoseStack, Camera pActiveRenderInfo, float pPartialTicks, CallbackInfo ci) {
    //    final Minecraft client = Minecraft.getInstance();
    //    final Player clientPlayer = client.player;
    //    if (clientPlayer == null) return;
    //    if (!FirstPersonManager.shouldRenderFirstPerson(clientPlayer)) return;
	//	  ci.cancel();
    //}
}
