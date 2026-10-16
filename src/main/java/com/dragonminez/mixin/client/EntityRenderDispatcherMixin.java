package com.dragonminez.mixin.client;

import com.dragonminez.client.render.DMZRendererCache;
import com.dragonminez.client.render.effects.NamekAssimilationEffect;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

	@Unique
	private static boolean dmz$assimilationReentry;

	@Inject(method = "onResourceManagerReload(Lnet/minecraft/server/packs/resources/ResourceManager;)V", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
	public void dmz$onResourceManagerReload(ResourceManager resourceManager, CallbackInfo ci, EntityRendererProvider.Context context) {
		DMZRendererCache.onResourceReload(context);
	}

	@Inject(method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
	private void dmz$assimilationRender(Entity entity, double x, double y, double z, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
		if (dmz$assimilationReentry) return;
		MultiBufferSource wrapped = NamekAssimilationEffect.wrap(entity, buffer, partialTick);
		if (wrapped == buffer) return;
		ci.cancel();
		dmz$assimilationReentry = true;
		try {
			((EntityRenderDispatcher) (Object) this).render(entity, x, y, z, yaw, partialTick, poseStack, wrapped, packedLight);
		} finally {
			dmz$assimilationReentry = false;
		}
	}
}
