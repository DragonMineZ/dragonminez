package com.dragonminez.mixin.client;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;

@Mixin(RenderStateShard.EmptyTextureStateShard.class)
public interface TextureStateShardInvoker {
	@Invoker("cutoutTexture")
	Optional<ResourceLocation> dmz$cutoutTexture();
}
