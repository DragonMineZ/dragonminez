package com.dragonminez.client.render.shader;

import com.dragonminez.client.render.util.ModRenderTypes;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TransformationMaskBufferSource implements MultiBufferSource {
	private static final int MASK_BUFFER_SIZE = 8192;

	private final LazyMaskBuffers maskBufferSource = new LazyMaskBuffers();
	@Nullable
	private MultiBufferSource delegate;
	private boolean maskCaptureEnabled = true;
	private boolean includeOriginal = true;
	private boolean forceCaptureAll = false;
	private int packedR = 255;
	private int packedG = 255;
	private int packedB = 255;

	public TransformationMaskBufferSource wrap(MultiBufferSource delegate) {
		this.delegate = delegate;
		this.maskCaptureEnabled = true;
		this.includeOriginal = true;
		this.forceCaptureAll = false;
		return this;
	}

	public void setMaskCaptureEnabled(boolean enabled) {
		this.maskCaptureEnabled = enabled;
	}

	public void setForceCaptureAll(boolean forceCaptureAll) {
		this.forceCaptureAll = forceCaptureAll;
	}

	public void setIncludeOriginal(boolean includeOriginal) {
		this.includeOriginal = includeOriginal;
	}

	public void setEntityColors(float primaryR, float primaryG, float primaryB, float secondaryR, float secondaryG, float secondaryB) {
		this.packedR = packChannel(primaryR, secondaryR);
		this.packedG = packChannel(primaryG, secondaryG);
		this.packedB = packChannel(primaryB, secondaryB);
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderType) {
		if (!this.maskCaptureEnabled && !this.forceCaptureAll) {
			if (this.delegate == null || !this.includeOriginal) {
				return new EmptyVertexConsumer();
			}
			return this.delegate.getBuffer(renderType);
		}

		RenderType maskRenderType = ModRenderTypes.transformationMask(renderType);
		if (this.delegate == null) {
			VertexConsumer maskDelegate = this.maskBufferSource.getBuffer(maskRenderType);
			return new TransformationMaskVertexConsumer(maskDelegate, this.packedR, this.packedG, this.packedB, 255);
		}

		VertexConsumer original = this.delegate.getBuffer(renderType);
		if (!ModRenderTypes.hasTransformationMaskShader()) {
			return original;
		}

		VertexConsumer maskDelegate = this.maskBufferSource.getBuffer(maskRenderType);
		VertexConsumer packedMask = new TransformationMaskVertexConsumer(maskDelegate, this.packedR, this.packedG, this.packedB, 255);
		if (!this.includeOriginal) {
			return packedMask;
		}
		return VertexMultiConsumer.create(packedMask, original);
	}

	public void endMaskBatch() {
		this.maskBufferSource.endBatch();
		this.delegate = null;
		this.maskCaptureEnabled = true;
		this.includeOriginal = true;
		this.forceCaptureAll = false;
	}

	private static final class LazyMaskBuffers {
		private final Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();

		VertexConsumer getBuffer(RenderType renderType) {
			BufferBuilder builder = this.builders.computeIfAbsent(renderType, type -> new BufferBuilder(MASK_BUFFER_SIZE));
			if (!builder.building()) {
				builder.begin(renderType.mode(), renderType.format());
			}
			return builder;
		}

		void endBatch() {
			for (Map.Entry<RenderType, BufferBuilder> entry : this.builders.entrySet()) {
				if (entry.getValue().building()) {
					entry.getKey().end(entry.getValue(), RenderSystem.getVertexSorting());
				}
			}
		}
	}

	private static final class EmptyVertexConsumer implements VertexConsumer {
		@Override
		public VertexConsumer vertex(double x, double y, double z) { return this; }
		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) { return this; }
		@Override
		public VertexConsumer uv(float u, float v) { return this; }
		@Override
		public VertexConsumer overlayCoords(int u, int v) { return this; }
		@Override
		public VertexConsumer uv2(int u, int v) { return this; }
		@Override
		public VertexConsumer normal(float x, float y, float z) { return this; }
		@Override
		public void endVertex() {}
		@Override
		public void defaultColor(int defaultR, int defaultG, int defaultB, int defaultA) {}
		@Override
		public void unsetDefaultColor() {}
		@Override
		public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float texU, float texV, int overlayUV, int lightmapUV, float normalX, float normalY, float normalZ) {}
	}

	private static int packChannel(float primary, float secondary) {
		int primary4 = quantize4(primary);
		int secondary4 = quantize4(secondary);
		return (primary4 << 4) | secondary4;
	}

	private static int quantize4(float value) {
		return quantize4(value, 0.0f, 1.0f);
	}

	private static int quantize4(float value, float min, float max) {
		float normalized = normalize(value, min, max);
		return Math.max(0, Math.min(15, Math.round(normalized * 15.0f)));
	}

	private static float normalize(float value, float min, float max) {
		if (max <= min) {
			return 0.0f;
		}
		float normalized = (value - min) / (max - min);
		return Math.max(0.0f, Math.min(1.0f, normalized));
	}
}