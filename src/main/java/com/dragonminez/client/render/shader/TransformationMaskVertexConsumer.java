package com.dragonminez.client.render.shader;

import com.mojang.blaze3d.vertex.DefaultedVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;

public final class TransformationMaskVertexConsumer extends DefaultedVertexConsumer {
	private final VertexConsumer delegate;
	private final int packedR;
	private final int packedG;
	private final int packedB;
	private final int packedA;

	private double x;
	private double y;
	private double z;
	private float u;
	private float v;

	public TransformationMaskVertexConsumer(VertexConsumer delegate, int packedR, int packedG, int packedB, int packedA) {
		this.delegate = delegate;
		this.packedR = packedR;
		this.packedG = packedG;
		this.packedB = packedB;
		this.packedA = packedA;
	}

	@Override
	public void defaultColor(int defaultR, int defaultG, int defaultB, int defaultA) {
	}

	@Override
	public void unsetDefaultColor() {
	}

	@Override
	public VertexConsumer vertex(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}

	@Override
	public VertexConsumer color(int red, int green, int blue, int alpha) {
		return this;
	}

	@Override
	public VertexConsumer uv(float u, float v) {
		this.u = u;
		this.v = v;
		return this;
	}

	@Override
	public VertexConsumer overlayCoords(int u, int v) {
		return this;
	}

	@Override
	public VertexConsumer uv2(int u, int v) {
		return this;
	}

	@Override
	public VertexConsumer normal(float x, float y, float z) {
		return this;
	}

	@Override
	public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float texU, float texV, int overlayUV, int lightmapUV, float normalX, float normalY, float normalZ) {
		this.delegate.vertex(x, y, z)
				.color(this.packedR, this.packedG, this.packedB, this.packedA)
				.uv(texU, texV)
				.overlayCoords(0)
				.uv2(0)
				.normal(0.0f, 1.0f, 0.0f)
				.endVertex();
	}

	@Override
	public void endVertex() {
		this.delegate.vertex(this.x, this.y, this.z)
				.color(this.packedR, this.packedG, this.packedB, this.packedA)
				.uv(this.u, this.v)
				.overlayCoords(0)
				.uv2(0)
				.normal(0.0f, 1.0f, 0.0f)
				.endVertex();
	}
}