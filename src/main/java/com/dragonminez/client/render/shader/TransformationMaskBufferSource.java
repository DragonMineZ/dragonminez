package com.dragonminez.client.render.shader;

import com.dragonminez.client.util.ModRenderTypes;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import javax.annotation.Nullable;
import java.util.Map;

public final class TransformationMaskBufferSource implements MultiBufferSource {
	private static final int MASK_BUFFER_SIZE = 8192;
	private static final float NOISE_SCALE_MIN = 0.25f;
	private static final float NOISE_SCALE_MAX = 16.0f;
	private static final float COLOR_MIX_SPEED_MIN = 0.0f;
	private static final float COLOR_MIX_SPEED_MAX = 4.0f;
	private static final float NOISE_SCROLL_MIN = -1.0f;
	private static final float NOISE_SCROLL_MAX = 1.0f;

	private final MultiBufferSource.BufferSource maskBufferSource = MultiBufferSource.immediateWithBuffers(
			Map.of(
					ModRenderTypes.transformationMask(), new BufferBuilder(MASK_BUFFER_SIZE),
					ModRenderTypes.transformationMaskViewOffset(), new BufferBuilder(MASK_BUFFER_SIZE),
					ModRenderTypes.transformationParams(), new BufferBuilder(MASK_BUFFER_SIZE),
					ModRenderTypes.transformationParamsViewOffset(), new BufferBuilder(MASK_BUFFER_SIZE)
			),
			new BufferBuilder(MASK_BUFFER_SIZE)
	);
	@Nullable
	private MultiBufferSource delegate;
	private int packedR = 255;
	private int packedG = 255;
	private int packedB = 255;
	private int packedNoiseScaleAndMix = 255;
	private int packedNoiseIntensity = 64;
	private int packedNoiseScrollX = 128;
	private int packedNoiseScrollY = 128;

	public TransformationMaskBufferSource wrap(MultiBufferSource delegate) {
		this.delegate = delegate;
		return this;
	}

	public void setEntityColors(float primaryR, float primaryG, float primaryB, float secondaryR, float secondaryG, float secondaryB) {
		this.packedR = packChannel(primaryR, secondaryR);
		this.packedG = packChannel(primaryG, secondaryG);
		this.packedB = packChannel(primaryB, secondaryB);
	}

	public void setEntityNoiseAndMix(float noiseScale, float noiseIntensity, float noiseScrollX, float noiseScrollY, float colorMixSpeed) {
		this.packedNoiseScaleAndMix = packPair4(noiseScale, NOISE_SCALE_MIN, NOISE_SCALE_MAX, colorMixSpeed, COLOR_MIX_SPEED_MIN, COLOR_MIX_SPEED_MAX);
		this.packedNoiseIntensity = quantize8(noiseIntensity, 0.0f, 1.0f);
		this.packedNoiseScrollX = quantize8(noiseScrollX, NOISE_SCROLL_MIN, NOISE_SCROLL_MAX);
		this.packedNoiseScrollY = quantize8(noiseScrollY, NOISE_SCROLL_MIN, NOISE_SCROLL_MAX);
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderType) {
		RenderType maskRenderType = ModRenderTypes.transformationMask(renderType);
		RenderType paramsRenderType = ModRenderTypes.transformationParams(renderType);
		if (this.delegate == null) {
			VertexConsumer maskDelegate = this.maskBufferSource.getBuffer(maskRenderType);
			VertexConsumer paramsDelegate = this.maskBufferSource.getBuffer(paramsRenderType);
			VertexConsumer packedMask = new TransformationMaskVertexConsumer(maskDelegate, this.packedR, this.packedG, this.packedB, 255);
			VertexConsumer packedParams = new TransformationMaskVertexConsumer(paramsDelegate, this.packedNoiseScaleAndMix, this.packedNoiseIntensity, this.packedNoiseScrollX, this.packedNoiseScrollY);
			return VertexMultiConsumer.create(packedMask, packedParams);
		}

		VertexConsumer original = this.delegate.getBuffer(renderType);
		if (!ModRenderTypes.hasTransformationMaskShader()) {
			return original;
		}

		VertexConsumer maskDelegate = this.maskBufferSource.getBuffer(maskRenderType);
		VertexConsumer paramsDelegate = this.maskBufferSource.getBuffer(paramsRenderType);
		VertexConsumer packedMask = new TransformationMaskVertexConsumer(maskDelegate, this.packedR, this.packedG, this.packedB, 255);
		VertexConsumer packedParams = new TransformationMaskVertexConsumer(paramsDelegate, this.packedNoiseScaleAndMix, this.packedNoiseIntensity, this.packedNoiseScrollX, this.packedNoiseScrollY);
		return VertexMultiConsumer.create(packedMask, packedParams, original);
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

	private static int packPair4(float highValue, float highMin, float highMax, float lowValue, float lowMin, float lowMax) {
		int high4 = quantize4(highValue, highMin, highMax);
		int low4 = quantize4(lowValue, lowMin, lowMax);
		return (high4 << 4) | low4;
	}

	private static int quantize4(float value) {
		return quantize4(value, 0.0f, 1.0f);
	}

	private static int quantize4(float value, float min, float max) {
		float normalized = normalize(value, min, max);
		return Math.max(0, Math.min(15, Math.round(normalized * 15.0f)));
	}

	private static int quantize8(float value, float min, float max) {
		float normalized = normalize(value, min, max);
		return Math.max(0, Math.min(255, Math.round(normalized * 255.0f)));
	}

	private static float normalize(float value, float min, float max) {
		if (max <= min) {
			return 0.0f;
		}
		float normalized = (value - min) / (max - min);
		return Math.max(0.0f, Math.min(1.0f, normalized));
	}
}