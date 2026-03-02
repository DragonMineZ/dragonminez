package com.dragonminez.client.render.shader;

import com.dragonminez.client.util.ModRenderTypes;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import javax.annotation.Nullable;

public final class TransformationMaskBufferSource implements MultiBufferSource {
	private static final int MASK_BUFFER_SIZE = 8192;

	private final MultiBufferSource.BufferSource maskBufferSource = MultiBufferSource.immediate(new BufferBuilder(MASK_BUFFER_SIZE));
	@Nullable
	private MultiBufferSource delegate;
	private int packedR = 255;
	private int packedG = 255;
	private int packedB = 255;

	public TransformationMaskBufferSource wrap(MultiBufferSource delegate) {
		this.delegate = delegate;
		return this;
	}

	public void setEntityColors(float primaryR, float primaryG, float primaryB, float secondaryR, float secondaryG, float secondaryB) {
		this.packedR = packChannel(primaryR, secondaryR);
		this.packedG = packChannel(primaryG, secondaryG);
		this.packedB = packChannel(primaryB, secondaryB);
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderType) {
		if (this.delegate == null) {
			return this.maskBufferSource.getBuffer(ModRenderTypes.transformationMask());
		}

		VertexConsumer original = this.delegate.getBuffer(renderType);
		if (!ModRenderTypes.hasTransformationMaskShader()) {
			return original;
		}

		VertexConsumer maskDelegate = this.maskBufferSource.getBuffer(ModRenderTypes.transformationMask());
		VertexConsumer packedMask = new TransformationMaskVertexConsumer(maskDelegate, this.packedR, this.packedG, this.packedB, 255);
		return VertexMultiConsumer.create(packedMask, original);
	}

	public void endMaskBatch() {
		this.maskBufferSource.endBatch();
		this.delegate = null;
	}

	private static int packChannel(float primary, float secondary) {
		int primary4 = quantize4(primary);
		int secondary4 = quantize4(secondary);
		return (primary4 << 4) | secondary4;
	}

	private static int quantize4(float value) {
		float clamped = Math.max(0.0f, Math.min(1.0f, value));
		return Math.max(0, Math.min(15, Math.round(clamped * 15.0f)));
	}
}
