package com.dragonminez.client.render.hair;

import com.dragonminez.Reference;
import com.dragonminez.common.hair.HairMath;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.util.RenderUtils;

public final class SaiyanTailMeshBuilder {
	public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/tail_fur.png");

	private static final int MAX_BONES = 16;
	private static final float LENGTH_SHADE = 0.55f;
	private static final float FACING_SHADE = 0.5f;
	private static final float FACING_PIVOT = 0.2f;
	private static final float FACE_NORMAL_WEIGHT = 0.35f;
	private static final float SHADE_STEP = 0.5f;
	private static final float DARKEST_SHADE = -0.5f;
	private static final float COMB_JITTER = 0.22f;
	private static final float CELLS_PER_UNIT = 1.0f / HairMath.PIXEL;
	private static final float TEXEL_UV = 1.0f / 16.0f;
	private static final float CELL_UV = TEXEL_UV * 2.0f;
	private static final float ALONG_THRESHOLD = 0.7f;
	private static final float SURFACE_INFLATE = 0.004f;
	private static final float SEGMENT_INFLATE_STEP = 0.001f;

	private final HairColorRamp colorRamp = new HairColorRamp();
	private final GeoBone[] bones = new GeoBone[MAX_BONES];
	private final Matrix4f[] poses = new Matrix4f[MAX_BONES];
	private final Matrix3f[] normals = new Matrix3f[MAX_BONES];
	private final PoseStack cubeStack = new PoseStack();
	private final Vector3f tailDirection = new Vector3f();
	private final Vector3f firstCenter = new Vector3f();
	private final Vector3f lastCenter = new Vector3f();
	private final Vector3f edgeAcross = new Vector3f();
	private final Vector3f edgeAlong = new Vector3f();
	private final Vector3f corner = new Vector3f();
	private final Vector3f edgeA = new Vector3f();
	private final Vector3f edgeB = new Vector3f();
	private final Vector3f cell = new Vector3f();
	private final Vector3f cubeCenter = new Vector3f();
	private final Vector3f cubeHalf = new Vector3f();
	private final Vector3f roundNormal = new Vector3f();
	private final float[] rgb = new float[3];
	private int count;
	private int captureEntity = Integer.MIN_VALUE;
	private int captureSequence = -1;
	private float minProjection;
	private float projectionRange;

	private VertexConsumer buffer;
	private Matrix4f pose;
	private Matrix3f normalMatrix;
	private int ramp;
	private int light;
	private int overlay;
	private float alpha;
	private boolean detailed;
	private float inflate;

	public SaiyanTailMeshBuilder() {
		for (int i = 0; i < MAX_BONES; i++) {
			poses[i] = new Matrix4f();
			normals[i] = new Matrix3f();
		}
	}

	public static boolean isTailBone(String name) {
		if (name == null || name.length() <= 4 || !name.startsWith("tail")) return false;
		for (int i = 4; i < name.length(); i++) {
			if (!Character.isDigit(name.charAt(i))) return false;
		}
		return true;
	}

	public void capture(Entity entity, GeoBone bone, PoseStack poseStack) {
		if (!isTailBone(bone.getName())) return;
		if (!HairRenderCapture.isCurrent(entity, captureSequence)) {
			reset();
			captureEntity = entity.getId();
			captureSequence = HairRenderCapture.sequence();
		}
		if (bone.isHidden()) return;
		add(bone, poseStack.last().pose(), poseStack.last().normal());
	}

	public boolean hasCapture(Entity entity) {
		return count > 0 && entity.getId() == captureEntity && HairRenderCapture.isCurrent(entity, captureSequence);
	}

	public void reset() {
		count = 0;
	}

	public void add(GeoBone bone, Matrix4f bonePose, Matrix3f boneNormal) {
		if (count >= MAX_BONES) return;
		bones[count] = bone;
		poses[count].set(bonePose);
		normals[count].set(boneNormal);
		count++;
	}

	public void emit(VertexConsumer buffer, float[] baseRgb, int packedLight, int packedOverlay, float baseAlpha, boolean pixelDetail) {
		if (count == 0 || !measureTail()) return;
		this.buffer = buffer;
		this.light = packedLight;
		this.overlay = packedOverlay;
		this.alpha = baseAlpha;
		this.detailed = pixelDetail;
		this.ramp = colorRamp.resolve(toRgb(baseRgb));
		for (int i = 0; i < count; i++) {
			inflate = SURFACE_INFLATE + i * SEGMENT_INFLATE_STEP;
			int c = 0;
			for (GeoCube cube : bones[i].getCubes()) {
				cubeStack.pushPose();
				cubeStack.last().pose().set(poses[i]);
				cubeStack.last().normal().set(normals[i]);
				RenderUtils.translateToPivotPoint(cubeStack, cube);
				RenderUtils.rotateMatrixAroundCube(cubeStack, cube);
				RenderUtils.translateAwayFromPivotPoint(cubeStack, cube);
				pose = cubeStack.last().pose();
				normalMatrix = cubeStack.last().normal();
				GeoQuad[] quads = cube.quads();
				measureCube(cube);
				for (int q = 0; q < quads.length; q++) {
					if (quads[q] != null) emitQuad(quads[q], (i * 31 + c) * 7 + q);
				}
				cubeStack.popPose();
				c++;
			}
		}
		this.buffer = null;
	}

	private boolean measureTail() {
		center(bones[0], firstCenter);
		center(bones[count - 1], lastCenter);
		tailDirection.set(lastCenter).sub(firstCenter);
		if (tailDirection.lengthSquared() < 1.0e-8f) tailDirection.set(0.0f, 0.0f, 1.0f);
		tailDirection.normalize();
		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;
		for (int i = 0; i < count; i++) {
			for (GeoCube cube : bones[i].getCubes()) {
				for (GeoQuad quad : cube.quads()) {
					if (quad == null) continue;
					for (GeoVertex vertex : quad.vertices()) {
						float projection = vertex.position().dot(tailDirection);
						min = Math.min(min, projection);
						max = Math.max(max, projection);
					}
				}
			}
		}
		if (min > max) return false;
		minProjection = min;
		projectionRange = Math.max(1.0e-4f, max - min);
		return true;
	}

	private void measureCube(GeoCube cube) {
		float minX = Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float minZ = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE;
		float maxY = -Float.MAX_VALUE;
		float maxZ = -Float.MAX_VALUE;
		for (GeoQuad quad : cube.quads()) {
			if (quad == null) continue;
			for (GeoVertex vertex : quad.vertices()) {
				Vector3f position = vertex.position();
				minX = Math.min(minX, position.x);
				minY = Math.min(minY, position.y);
				minZ = Math.min(minZ, position.z);
				maxX = Math.max(maxX, position.x);
				maxY = Math.max(maxY, position.y);
				maxZ = Math.max(maxZ, position.z);
			}
		}
		cubeCenter.set((minX + maxX) * 0.5f, (minY + maxY) * 0.5f, (minZ + maxZ) * 0.5f);
		cubeHalf.set((maxX - minX) * 0.5f, (maxY - minY) * 0.5f, (maxZ - minZ) * 0.5f);
	}

	private static void center(GeoBone bone, Vector3f out) {
		out.zero();
		int vertices = 0;
		for (GeoCube cube : bone.getCubes()) {
			for (GeoQuad quad : cube.quads()) {
				if (quad == null) continue;
				for (GeoVertex vertex : quad.vertices()) {
					out.add(vertex.position());
					vertices++;
				}
			}
		}
		if (vertices > 0) out.div(vertices);
	}

	private void emitQuad(GeoQuad quad, int seed) {
		GeoVertex[] vertices = quad.vertices();
		Vector3f origin = vertices[0].position();
		Vector3f first = vertices[1].position();
		Vector3f last = vertices[3].position();
		edgeA.set(first).sub(origin);
		edgeB.set(last).sub(origin);
		float lengthA = edgeA.length();
		float lengthB = edgeB.length();
		if (lengthA < 1.0e-6f || lengthB < 1.0e-6f) return;
		boolean aAlong = Math.abs(edgeA.dot(tailDirection)) / lengthA >= ALONG_THRESHOLD;
		boolean bAlong = Math.abs(edgeB.dot(tailDirection)) / lengthB >= ALONG_THRESHOLD;
		boolean swap = aAlong && !bAlong;

		int columns = detailed ? cells(swap ? lengthB : lengthA) : 1;
		int rows = detailed ? cells(swap ? lengthA : lengthB) : 1;
		float uBase = ((mix(seed) >>> 8) & 0xF) * TEXEL_UV;
		float vBase = Math.round((origin.dot(tailDirection) - minProjection) * CELLS_PER_UNIT) * CELL_UV;

		for (int column = 0; column < columns; column++) {
			float across = (column + 0.5f) / columns;
			float jitter = detailed ? COMB_JITTER * signedNoise(seed * 131 + column * 31) : 0.0f;
			for (int row = 0; row < rows; row++) {
				locate(vertices, swap, across, (row + 0.5f) / rows);
				float param = (cell.dot(tailDirection) - minProjection) / projectionRange;
				int shadeLevel = level(LENGTH_SHADE * (2.0f * param - 1.0f) + facing(quad.normal()) + jitter);
				emitCell(vertices, quad, swap, (float) column / columns, (float) (column + 1) / columns, (float) row / rows, (float) (row + 1) / rows,
						shadeLevel, uBase + column * CELL_UV, uBase + (column + 1) * CELL_UV, vBase + row * CELL_UV, vBase + (row + 1) * CELL_UV);
			}
		}
	}

	private float facing(Vector3f faceNormal) {
		roundNormal.set(cell).sub(cubeCenter);
		float along = roundNormal.dot(tailDirection);
		roundNormal.sub(tailDirection.x * along, tailDirection.y * along, tailDirection.z * along);
		if (roundNormal.lengthSquared() > 1.0e-10f) roundNormal.normalize();
		roundNormal.add(faceNormal.x * FACE_NORMAL_WEIGHT, faceNormal.y * FACE_NORMAL_WEIGHT, faceNormal.z * FACE_NORMAL_WEIGHT);
		if (roundNormal.lengthSquared() > 1.0e-10f) roundNormal.normalize();
		return FACING_SHADE * (roundNormal.y - FACING_PIVOT);
	}

	private void emitCell(GeoVertex[] vertices, GeoQuad quad, boolean swap, float across0, float across1,
						  float along0, float along1, int shadeLevel, float u0, float u1, float v0, float v1) {
		colorRamp.sample(ramp, shadeLevel * SHADE_STEP, rgb, 0);
		Vector3f normal = quad.normal();
		vertex(vertices, swap, across0, along0, u0, v0, normal);
		vertex(vertices, swap, across0, along1, u0, v1, normal);
		vertex(vertices, swap, across1, along1, u1, v1, normal);
		vertex(vertices, swap, across1, along0, u1, v0, normal);
	}

	private void vertex(GeoVertex[] vertices, boolean swap, float across, float along, float u, float v, Vector3f normal) {
		locate(vertices, swap, across, along);
		cell.set(grow(cell.x, cubeCenter.x, cubeHalf.x), grow(cell.y, cubeCenter.y, cubeHalf.y), grow(cell.z, cubeCenter.z, cubeHalf.z));
		buffer.vertex(pose, cell.x, cell.y, cell.z)
				.color(rgb[0], rgb[1], rgb[2], alpha)
				.uv(u, v)
				.overlayCoords(overlay)
				.uv2(light)
				.normal(normalMatrix, normal.x, normal.y, normal.z)
				.endVertex();
	}

	private void locate(GeoVertex[] vertices, boolean swap, float across, float along) {
		float a = swap ? along : across;
		float b = swap ? across : along;
		edgeAcross.set(vertices[0].position()).lerp(vertices[1].position(), a);
		edgeAlong.set(vertices[3].position()).lerp(vertices[2].position(), a);
		cell.set(edgeAcross).lerp(edgeAlong, b);
	}

	private float grow(float value, float center, float half) {
		return half > 1.0e-6f ? center + (value - center) * (half + inflate) / half : value;
	}

	private static int level(float shade) {
		return Math.round(HairMath.clamp(shade, DARKEST_SHADE, 1.0f) / SHADE_STEP);
	}

	private static int cells(float units) {
		return Math.max(1, Math.round(units * CELLS_PER_UNIT));
	}

	private static float signedNoise(int value) {
		return (mix(value) & 0xFFFF) / 32767.5f - 1.0f;
	}

	private static int mix(int value) {
		int hash = value * 0x9E3779B1;
		hash ^= hash >>> 15;
		hash *= 0x85EBCA6B;
		return hash ^ (hash >>> 13);
	}

	private static int toRgb(float[] rgb) {
		int r = Math.round(HairMath.clamp(rgb[0], 0.0f, 1.0f) * 255.0f);
		int g = Math.round(HairMath.clamp(rgb[1], 0.0f, 1.0f) * 255.0f);
		int b = Math.round(HairMath.clamp(rgb[2], 0.0f, 1.0f) * 255.0f);
		return (r << 16) | (g << 8) | b;
	}
}
