package com.dragonminez.client.render.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * A {@link MultiBufferSource} that funnels every requested render type into the
 * single {@link ModRenderTypes#transformationHull inverted-hull outline} type
 * (a vanilla emissive shader, so Oculus renders it natively). Used for the
 * shader-only outline pass of the player model so the inflated body silhouette
 * is drawn as one bright, front-culled shell regardless of the per-layer texture.
 */
public final class OutlineBufferSource implements MultiBufferSource {
	private final MultiBufferSource.BufferSource immediate =
			MultiBufferSource.immediate(new BufferBuilder(2048));

	private ResourceLocation texture;
	private float[] color = {1.0f, 1.0f, 1.0f};

	/** Configures the silhouette texture and tint for the next outline pass. */
	public void configure(ResourceLocation texture, float[] color) {
		this.texture = texture;
		this.color = color;
	}

	@Override
	public VertexConsumer getBuffer(RenderType ignored) {
		return immediate.getBuffer(ModRenderTypes.transformationHull(texture));
	}

	/** Tints via the ColorModulator and flushes the captured silhouette geometry. */
	public void flush() {
		RenderSystem.setShaderColor(color[0], color[1], color[2], 1.0f);
		immediate.endBatch();
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
	}
}
