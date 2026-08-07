package com.dragonminez.client.render.shader;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Forwards geometry while forcing mask color (1.21 VertexConsumer API).
 * <p>
 * Completes overlay/light/normal defaults so mixed formats (e.g. name-tag text
 * funneled into a NEW_ENTITY mask buffer via VertexMultiConsumer) do not crash
 * with "Missing elements in vertex: UV1, Normal".
 */
public final class TransformationMaskVertexConsumer implements VertexConsumer {
	private final VertexConsumer delegate;
	private final int packedR;
	private final int packedG;
	private final int packedB;
	private final int packedA;

	private boolean vertexOpen;
	private boolean hasUv1;
	private boolean hasUv2;
	private boolean hasNormal;

	public TransformationMaskVertexConsumer(VertexConsumer delegate, int packedR, int packedG, int packedB, int packedA) {
		this.delegate = delegate;
		this.packedR = packedR;
		this.packedG = packedG;
		this.packedB = packedB;
		this.packedA = packedA;
	}

	private void finishOpenVertex() {
		// Ensure the open vertex has required NEW_ENTITY attributes before the next begin.
		if (!this.vertexOpen) return;
		if (!this.hasUv1) {
			this.delegate.setUv1(0, 10);
			this.hasUv1 = true;
		}
		if (!this.hasUv2) {
			this.delegate.setUv2(0xF0, 0xF0);
			this.hasUv2 = true;
		}
		if (!this.hasNormal) {
			this.delegate.setNormal(0.0f, 1.0f, 0.0f);
			this.hasNormal = true;
		}
	}

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		this.finishOpenVertex();
		this.vertexOpen = true;
		this.hasUv1 = false;
		this.hasUv2 = false;
		this.hasNormal = false;
		this.delegate.addVertex(x, y, z);
		this.delegate.setColor(this.packedR, this.packedG, this.packedB, this.packedA);
		return this;
	}

	@Override
	public VertexConsumer setColor(int red, int green, int blue, int alpha) {
		// Force mask color; ignore upstream tint.
		return this;
	}

	@Override
	public VertexConsumer setUv(float u, float v) {
		this.delegate.setUv(u, v);
		return this;
	}

	@Override
	public VertexConsumer setUv1(int u, int v) {
		this.delegate.setUv1(u, v);
		this.hasUv1 = true;
		return this;
	}

	@Override
	public VertexConsumer setUv2(int u, int v) {
		this.delegate.setUv2(u, v);
		this.hasUv2 = true;
		return this;
	}

	@Override
	public VertexConsumer setNormal(float x, float y, float z) {
		this.delegate.setNormal(x, y, z);
		this.hasNormal = true;
		return this;
	}
}
