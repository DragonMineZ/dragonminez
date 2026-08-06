package com.dragonminez.client.render.shader;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Forwards geometry while forcing mask color (1.21 VertexConsumer API). */
public final class TransformationMaskVertexConsumer implements VertexConsumer {
	private final VertexConsumer delegate;
	private final int packedR;
	private final int packedG;
	private final int packedB;
	private final int packedA;

	public TransformationMaskVertexConsumer(VertexConsumer delegate, int packedR, int packedG, int packedB, int packedA) {
		this.delegate = delegate;
		this.packedR = packedR;
		this.packedG = packedG;
		this.packedB = packedB;
		this.packedA = packedA;
	}

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
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
		this.delegate.setUv1(0, 0);
		return this;
	}

	@Override
	public VertexConsumer setUv2(int u, int v) {
		this.delegate.setUv2(0, 0);
		return this;
	}

	@Override
	public VertexConsumer setNormal(float x, float y, float z) {
		this.delegate.setNormal(0.0f, 1.0f, 0.0f);
		return this;
	}
}
