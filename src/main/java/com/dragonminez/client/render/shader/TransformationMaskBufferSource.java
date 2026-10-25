package com.dragonminez.client.render.shader;

import com.dragonminez.client.render.util.ModRenderTypes;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;

public final class TransformationMaskBufferSource implements MultiBufferSource {
	private static final int MASK_BUFFER_SIZE = 8192;
	private final Map<RenderType, MaskBuffer> maskBuffers = new LinkedHashMap<>();
	@Nullable
	private MultiBufferSource delegate;
	private boolean maskCaptureEnabled = true;
	private boolean includeOriginal = true;
	private boolean forceCaptureAll = false;
	private boolean maskCaptureBlocked = false;
	private int packedR = 255;
	private int packedG = 255;
	private int packedB = 255;

	public TransformationMaskBufferSource wrap(MultiBufferSource delegate) {
		this.delegate = delegate;
		this.maskCaptureEnabled = true;
		this.includeOriginal = true;
		this.forceCaptureAll = false;
		this.maskCaptureBlocked = false;
		return this;
	}

	public void setMaskCaptureEnabled(boolean enabled) {
		this.maskCaptureEnabled = enabled;
		if (this.delegate instanceof TransformationMaskBufferSource inner) inner.setMaskCaptureEnabled(enabled);
	}

	public void setMaskCaptureBlocked(boolean blocked) {
		this.maskCaptureBlocked = blocked;
		if (this.delegate instanceof TransformationMaskBufferSource inner) inner.setMaskCaptureBlocked(blocked);
	}

	public void setRawColor(int r, int g, int b) {
		this.packedR = r;
		this.packedG = g;
		this.packedB = b;
	}

	public void detach() {
		this.delegate = null;
		this.maskCaptureEnabled = true;
		this.includeOriginal = true;
		this.forceCaptureAll = false;
		this.maskCaptureBlocked = false;
	}

	public void setEntityColors(float primaryR, float primaryG, float primaryB, float secondaryR, float secondaryG, float secondaryB) {
		this.packedR = packChannel(primaryR, secondaryR);
		this.packedG = packChannel(primaryG, secondaryG);
		this.packedB = packChannel(primaryB, secondaryB);
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderType) {
		boolean capture = !this.maskCaptureBlocked && (this.maskCaptureEnabled || this.forceCaptureAll);
		RenderType maskRenderType = capture ? ModRenderTypes.transformationMask(renderType) : null;
		if (maskRenderType == null) {
			if (this.delegate == null || !this.includeOriginal) {
				return EmptyVertexConsumer.INSTANCE;
			}
			return this.delegate.getBuffer(renderType);
		}
		// Name tags / GUI text must never be captured into the NEW_ENTITY mask buffer:
		// font glyphs omit UV1/Normal and crash BufferBuilder under forceCaptureAll.
		if (isNonGeometryRenderType(renderType)) {
			if (this.delegate == null || !this.includeOriginal) {
				return EmptyVertexConsumer.INSTANCE;
			}
			return this.delegate.getBuffer(renderType);
		}
		if (this.delegate == null) {
			VertexConsumer maskDelegate = this.getMaskBuffer(maskRenderType);
			return new TransformationMaskVertexConsumer(maskDelegate, this.packedR, this.packedG, this.packedB, 255);
		}
		VertexConsumer original = this.delegate.getBuffer(renderType);
		if (!ModRenderTypes.hasTransformationMaskShader()) {
			return original;
		}
		VertexConsumer maskDelegate = this.getMaskBuffer(maskRenderType);
		VertexConsumer packedMask = new TransformationMaskVertexConsumer(maskDelegate, this.packedR, this.packedG, this.packedB, 255);
		if (!this.includeOriginal) {
			return packedMask;
		}
		return VertexMultiConsumer.create(packedMask, original);
	}

	public void endMaskBatch() {
		for (Map.Entry<RenderType, MaskBuffer> entry : this.maskBuffers.entrySet()) {
			MaskBuffer maskBuffer = entry.getValue();
			if (maskBuffer.builder == null) {
				continue;
			}
			MeshData meshData = maskBuffer.builder.build();
			maskBuffer.builder = null;
			if (meshData != null) {
				RenderType renderType = entry.getKey();
				if (renderType.sortOnUpload()) {
					meshData.sortQuads(maskBuffer.byteBuffer, RenderSystem.getVertexSorting());
				}
				renderType.draw(meshData);
			}
		}
		this.delegate = null;
		this.maskCaptureEnabled = true;
		this.includeOriginal = true;
		this.forceCaptureAll = false;
		this.maskCaptureBlocked = false;
	}

	private VertexConsumer getMaskBuffer(RenderType renderType) {
		MaskBuffer maskBuffer = this.maskBuffers.computeIfAbsent(renderType, ignored -> new MaskBuffer());
		if (maskBuffer.builder == null) {
			maskBuffer.builder = new BufferBuilder(maskBuffer.byteBuffer, renderType.mode(), renderType.format());
		}
		return maskBuffer.builder;
	}

	public void setIncludeOriginal(boolean includeOriginal) {
		this.includeOriginal = includeOriginal;
	}

	public void setForceCaptureAll(boolean forceCaptureAll) {
		this.forceCaptureAll = forceCaptureAll;
	}

	private static int packChannel(float primary, float secondary) {
		int p = Math.max(0, Math.min(255, Math.round(primary * 255.0f)));
		int s = Math.max(0, Math.min(255, Math.round(secondary * 255.0f)));
		return (p & 0xF0) | ((s >> 4) & 0x0F);
	}

	/**
	 * Render types used for fonts, lines, particles, etc. that are unsafe to remap
	 * into the transformation mask NEW_ENTITY format.
	 */
	private static boolean isNonGeometryRenderType(RenderType renderType) {
		// The debug string includes texture state, so searching for "text" also
		// rejects textured entity geometry. Only entity vertices fit the mask.
		return renderType == null || renderType.format() != DefaultVertexFormat.NEW_ENTITY;
	}

	private static final class MaskBuffer {
		private final ByteBufferBuilder byteBuffer = new ByteBufferBuilder(MASK_BUFFER_SIZE);
		@Nullable
		private BufferBuilder builder;
	}

	private static final class EmptyVertexConsumer implements VertexConsumer {
		static final EmptyVertexConsumer INSTANCE = new EmptyVertexConsumer();

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			return this;
		}

		@Override
		public VertexConsumer setColor(int red, int green, int blue, int alpha) {
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			return this;
		}
	}
}
