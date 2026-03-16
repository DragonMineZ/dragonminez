package com.dragonminez.mixin.client;

import com.dragonminez.client.render.DMZRendererCache;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

	@Inject(method = "onResourceManagerReload(Lnet/minecraft/server/packs/resources/ResourceManager;)V", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
	public void dmz$onResourceManagerReload(ResourceManager resourceManager, CallbackInfo ci, EntityRendererProvider.Context context) {
		DMZRendererCache.onResourceReload(context);
	}
}