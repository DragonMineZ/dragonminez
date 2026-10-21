package com.dragonminez.client.model;

import com.dragonminez.Reference;
import com.dragonminez.client.systems.FormVisualTransition;
import com.dragonminez.common.stats.FormTransition;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.core.state.BoneSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ModelMorph {
	private static final String KEY_PREFIX = "morph/";
	private static final long STALE_NANOS = 5_000_000_000L;
	private static final long PRUNE_INTERVAL_NANOS = 1_000_000_000L;
	private static final float MIN_FACTOR = 0.001f;
	private static final float UV_EPSILON = 0.002f;
	private static final float EDGE_EPSILON = 1.0e-9f;
	private static final Map<Integer, Entry> ENTRIES = new HashMap<>();
	private static long lastPrune;

	private ModelMorph() {}

	public static ResourceLocation select(AbstractClientPlayer player, StatsData stats, ResourceLocation current, Function<FormTransition.Look, ResourceLocation> resolver) {
		long now = System.nanoTime();
		prune(now);

		ResourceLocation target = null;
		float factor = 0.0f;
		FormTransition.Charge charge = FormVisualTransition.charge(stats);
		if (charge != null) {
			ResourceLocation model = resolver.apply(charge.look());
			if (!model.equals(current)) {
				target = model;
				factor = charge.factor();
			}
		}
		FormTransition.Revert revert = FormVisualTransition.revert(stats);
		if (revert != null && revert.factor() > factor) {
			ResourceLocation model = resolver.apply(revert.look());
			if (!model.equals(current)) {
				target = model;
				factor = revert.factor();
			}
		}

		if (target == null || factor <= MIN_FACTOR) {
			ENTRIES.remove(player.getId());
			return current;
		}

		Entry entry = ENTRIES.computeIfAbsent(player.getId(), Entry::new);
		entry.from = current;
		entry.to = target;
		entry.factor = Math.min(factor, 1.0f);
		entry.lastUsed = now;
		return entry.key;
	}

	public static ResourceLocation source(ResourceLocation location, ResourceLocation fallback) {
		if (!isMorphKey(location)) return location;
		Entry entry = entry(location);
		return entry != null ? entry.from : fallback;
	}

	public static BakedGeoModel baked(ResourceLocation location) {
		if (!isMorphKey(location)) return null;
		Entry entry = entry(location);
		if (entry == null) return null;
		BakedGeoModel from = GeckoLibCache.getBakedModels().get(entry.from);
		BakedGeoModel to = GeckoLibCache.getBakedModels().get(entry.to);
		if (from == null || to == null) return null;
		if (entry.blend == null || entry.blend.from != from || entry.blend.to != to) entry.blend = new Blend(from, to);
		entry.blend.apply(entry.factor);
		return entry.blend.model;
	}

	private static boolean isMorphKey(ResourceLocation location) {
		return Reference.MOD_ID.equals(location.getNamespace()) && location.getPath().startsWith(KEY_PREFIX);
	}

	private static Entry entry(ResourceLocation location) {
		try {
			return ENTRIES.get(Integer.parseInt(location.getPath().substring(KEY_PREFIX.length())));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static void prune(long now) {
		if (now - lastPrune < PRUNE_INTERVAL_NANOS) return;
		lastPrune = now;
		ENTRIES.values().removeIf(entry -> now - entry.lastUsed > STALE_NANOS);
	}

	private static final class Entry {
		private final ResourceLocation key;
		private ResourceLocation from;
		private ResourceLocation to;
		private float factor;
		private long lastUsed;
		private Blend blend;

		private Entry(int id) {
			this.key = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, KEY_PREFIX + id);
		}
	}

	private record BoneLink(GeoBone bone, GeoBone from, GeoBone to, float[] fromRest, float[] toRest) {}

	private static final class Blend {
		private final BakedGeoModel from;
		private final BakedGeoModel to;
		private final BakedGeoModel model;
		private final List<BoneLink> links = new ArrayList<>();
		private final List<Piece> pieces = new ArrayList<>();

		private Blend(BakedGeoModel from, BakedGeoModel to) {
			this.from = from;
			this.to = to;
			Map<String, GeoBone> fromBones = new HashMap<>();
			Map<String, GeoBone> toBones = new HashMap<>();
			for (GeoBone bone : from.topLevelBones()) index(bone, fromBones);
			for (GeoBone bone : to.topLevelBones()) index(bone, toBones);

			Map<String, GeoBone> built = new HashMap<>();
			List<GeoBone> top = new ArrayList<>();
			for (GeoBone bone : to.topLevelBones()) copy(bone, null, built, top, fromBones);
			for (GeoBone bone : from.topLevelBones()) copyMissing(bone, built, top);
			for (BoneLink link : links) matchCubes(link);
			this.model = new BakedGeoModel(top, to.properties());
		}

		private static void index(GeoBone bone, Map<String, GeoBone> out) {
			out.putIfAbsent(bone.getName(), bone);
			for (GeoBone child : bone.getChildBones()) index(child, out);
		}

		private void copy(GeoBone source, GeoBone parent, Map<String, GeoBone> built, List<GeoBone> top, Map<String, GeoBone> fromBones) {
			if (built.containsKey(source.getName())) return;
			GeoBone bone = attach(source, parent, built, top);
			GeoBone fromBone = fromBones.get(source.getName());
			float[] toRest = rest(source);
			links.add(new BoneLink(bone, fromBone, source, fromBone != null ? rest(fromBone) : toRest, toRest));
			for (GeoBone child : source.getChildBones()) copy(child, bone, built, top, fromBones);
		}

		private void copyMissing(GeoBone source, Map<String, GeoBone> built, List<GeoBone> top) {
			if (!built.containsKey(source.getName())) {
				GeoBone parent = source.getParent() != null ? built.get(source.getParent().getName()) : null;
				GeoBone bone = attach(source, parent, built, top);
				float[] fromRest = rest(source);
				links.add(new BoneLink(bone, source, null, fromRest, fromRest));
			}
			for (GeoBone child : source.getChildBones()) copyMissing(child, built, top);
		}

		private static GeoBone attach(GeoBone source, GeoBone parent, Map<String, GeoBone> built, List<GeoBone> top) {
			GeoBone bone = new GeoBone(parent, source.getName(), source.getMirror(), source.getInflate(), source.shouldNeverRender(), source.getReset());
			float[] rest = rest(source);
			bone.setPivotX(rest[0]);
			bone.setPivotY(rest[1]);
			bone.setPivotZ(rest[2]);
			bone.setRotX(rest[3]);
			bone.setRotY(rest[4]);
			bone.setRotZ(rest[5]);
			if (parent != null) parent.getChildBones().add(bone);
			else top.add(bone);
			built.put(source.getName(), bone);
			return bone;
		}

		private static float[] rest(GeoBone bone) {
			BoneSnapshot snapshot = bone.getInitialSnapshot();
			return new float[]{
					bone.getPivotX(), bone.getPivotY(), bone.getPivotZ(),
					snapshot != null ? snapshot.getRotX() : bone.getRotX(),
					snapshot != null ? snapshot.getRotY() : bone.getRotY(),
					snapshot != null ? snapshot.getRotZ() : bone.getRotZ()
			};
		}

		private void matchCubes(BoneLink link) {
			List<CubeShape> sources = shapes(link.from());
			List<CubeShape> targets = shapes(link.to());
			boolean[] usedSource = new boolean[sources.size()];
			boolean[] usedTarget = new boolean[targets.size()];

			for (int t = 0; t < targets.size(); t++) {
				CubeShape target = targets.get(t);
				int s = container(sources, target);
				if (s < 0) continue;
				CubeShape source = sources.get(s);
				usedSource[s] = true;
				usedTarget[t] = true;
				add(link.bone(), target, source.slice(target), target.box, source, source, target);
			}

			for (int s = 0; s < sources.size(); s++) {
				if (usedSource[s]) continue;
				CubeShape source = sources.get(s);
				int t = container(targets, source);
				if (t < 0) continue;
				CubeShape target = targets.get(t);
				usedSource[s] = true;
				usedTarget[t] = true;
				add(link.bone(), source, source.box, target.slice(source), null, source, target);
			}

			for (int t = 0; t < targets.size(); t++) {
				if (usedTarget[t]) continue;
				CubeShape target = targets.get(t);
				int s = closest(sources, usedSource, target);
				if (s < 0) continue;
				CubeShape source = sources.get(s);
				usedSource[s] = true;
				usedTarget[t] = true;
				add(link.bone(), target, source.box, target.box, source, source, target);
			}

			for (int t = 0; t < targets.size(); t++) {
				if (usedTarget[t]) continue;
				CubeShape target = targets.get(t);
				add(link.bone(), target, target.box.collapsed(), target.box, null, target, target);
			}

			for (int s = 0; s < sources.size(); s++) {
				if (usedSource[s]) continue;
				CubeShape source = sources.get(s);
				add(link.bone(), source, source.box, source.box.collapsed(), null, source, source);
			}
		}

		private static List<CubeShape> shapes(GeoBone bone) {
			List<CubeShape> out = new ArrayList<>();
			if (bone == null) return out;
			for (GeoCube cube : bone.getCubes()) {
				Box box = Box.of(cube);
				if (box != null) out.add(new CubeShape(cube, box));
			}
			return out;
		}

		private static int container(List<CubeShape> candidates, CubeShape inner) {
			if (inner.northRect == null) return -1;
			int best = -1;
			float bestArea = Float.MAX_VALUE;
			for (int i = 0; i < candidates.size(); i++) {
				CubeShape outer = candidates.get(i);
				if (!outer.contains(inner)) continue;
				float area = outer.northArea();
				if (area < bestArea) {
					bestArea = area;
					best = i;
				}
			}
			return best;
		}

		private static int closest(List<CubeShape> candidates, boolean[] used, CubeShape other) {
			if (other.northRect == null) return -1;
			int best = -1;
			float bestOverlap = 0.0f;
			for (int i = 0; i < candidates.size(); i++) {
				if (used[i]) continue;
				float overlap = candidates.get(i).overlap(other);
				if (overlap > bestOverlap) {
					bestOverlap = overlap;
					best = i;
				}
			}
			return best;
		}

		private void add(GeoBone owner, CubeShape topology, Box start, Box end, CubeShape uvSource, CubeShape pivotFrom, CubeShape pivotTo) {
			GeoCube cube = topology.cube;
			GeoQuad[] quads = new GeoQuad[cube.quads().length];
			List<Vector3f> positions = new ArrayList<>();
			List<Vector3f> starts = new ArrayList<>();
			List<Vector3f> ends = new ArrayList<>();

			for (int q = 0; q < quads.length; q++) {
				GeoQuad quad = cube.quads()[q];
				if (quad == null) continue;
				GeoVertex[] vertices = new GeoVertex[quad.vertices().length];
				for (int v = 0; v < vertices.length; v++) {
					GeoVertex vertex = quad.vertices()[v];
					Vector3f p = vertex.position();
					float tx = topology.box.fraction(p.x, 0);
					float ty = topology.box.fraction(p.y, 1);
					float tz = topology.box.fraction(p.z, 2);
					Vector3f from = start.at(tx, ty, tz);
					Vector3f to = end.at(tx, ty, tz);
					float u = vertex.texU();
					float w = vertex.texV();
					if (uvSource != null) {
						float[] uv = uvSource.uvAt(quad.direction(), from);
						if (uv != null) {
							u = uv[0];
							w = uv[1];
						}
					}
					Vector3f position = new Vector3f(from);
					vertices[v] = new GeoVertex(position, u, w);
					positions.add(position);
					starts.add(from);
					ends.add(to);
				}
				quads[q] = new GeoQuad(vertices, quad.normal(), quad.direction());
			}

			GeoCube fromCube = pivotFrom.cube;
			GeoCube toCube = pivotTo.cube;
			owner.getCubes().add(new GeoCube(quads, fromCube.pivot(), fromCube.rotation(), cube.size(), cube.inflate(), cube.mirror()));
			pieces.add(new Piece(owner, owner.getCubes().size() - 1, quads, cube.size(), cube.inflate(), cube.mirror(),
					fromCube.pivot(), toCube.pivot(), fromCube.rotation(), toCube.rotation(),
					positions.toArray(Vector3f[]::new), starts.toArray(Vector3f[]::new), ends.toArray(Vector3f[]::new)));
		}

		private void apply(float factor) {
			for (BoneLink link : links) {
				float[] a = link.fromRest();
				float[] b = link.toRest();
				GeoBone bone = link.bone();
				bone.setPivotX(Mth.lerp(factor, a[0], b[0]));
				bone.setPivotY(Mth.lerp(factor, a[1], b[1]));
				bone.setPivotZ(Mth.lerp(factor, a[2], b[2]));
				float rotX = Mth.lerp(factor, a[3], b[3]);
				float rotY = Mth.lerp(factor, a[4], b[4]);
				float rotZ = Mth.lerp(factor, a[5], b[5]);
				BoneSnapshot snapshot = bone.getInitialSnapshot();
				if (snapshot != null) {
					snapshot.updateRotation(rotX, rotY, rotZ);
				} else {
					bone.setRotX(rotX);
					bone.setRotY(rotY);
					bone.setRotZ(rotZ);
				}
			}
			for (Piece piece : pieces) piece.apply(factor);
		}
	}

	private record Piece(GeoBone owner, int index, GeoQuad[] quads, Vec3 size, double inflate, boolean mirror,
						 Vec3 pivotFrom, Vec3 pivotTo, Vec3 rotationFrom, Vec3 rotationTo,
						 Vector3f[] positions, Vector3f[] starts, Vector3f[] ends) {
		private void apply(float factor) {
			for (int i = 0; i < positions.length; i++) starts[i].lerp(ends[i], factor, positions[i]);
			if (pivotFrom.equals(pivotTo) && rotationFrom.equals(rotationTo)) return;
			owner.getCubes().set(index, new GeoCube(quads, pivotFrom.lerp(pivotTo, factor), rotationFrom.lerp(rotationTo, factor), size, inflate, mirror));
		}
	}

	private record Box(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		private static Box of(GeoCube cube) {
			float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
			float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
			boolean found = false;
			for (GeoQuad quad : cube.quads()) {
				if (quad == null) continue;
				for (GeoVertex vertex : quad.vertices()) {
					Vector3f p = vertex.position();
					minX = Math.min(minX, p.x);
					minY = Math.min(minY, p.y);
					minZ = Math.min(minZ, p.z);
					maxX = Math.max(maxX, p.x);
					maxY = Math.max(maxY, p.y);
					maxZ = Math.max(maxZ, p.z);
					found = true;
				}
			}
			return found ? new Box(minX, minY, minZ, maxX, maxY, maxZ) : null;
		}

		private float fraction(float value, int axis) {
			float min = axis == 0 ? minX : axis == 1 ? minY : minZ;
			float max = axis == 0 ? maxX : axis == 1 ? maxY : maxZ;
			float span = max - min;
			return span > EDGE_EPSILON ? (value - min) / span : 0.0f;
		}

		private Vector3f at(float tx, float ty, float tz) {
			return new Vector3f(Mth.lerp(tx, minX, maxX), Mth.lerp(ty, minY, maxY), Mth.lerp(tz, minZ, maxZ));
		}

		private Box collapsed() {
			float x = (minX + maxX) * 0.5f;
			float y = (minY + maxY) * 0.5f;
			float z = (minZ + maxZ) * 0.5f;
			return new Box(x, y, z, x, y, z);
		}
	}

	private static final class CubeShape {
		private final GeoCube cube;
		private final Box box;
		private final GeoQuad north;
		private final float[] northRect;

		private CubeShape(GeoCube cube, Box box) {
			this.cube = cube;
			this.box = box;
			this.north = quad(cube, Direction.NORTH);
			this.northRect = north != null ? uvRect(north) : null;
		}

		private static GeoQuad quad(GeoCube cube, Direction direction) {
			for (GeoQuad quad : cube.quads()) {
				if (quad != null && quad.direction() == direction) return quad;
			}
			return null;
		}

		private static float[] uvRect(GeoQuad quad) {
			float minU = Float.MAX_VALUE, minV = Float.MAX_VALUE, maxU = -Float.MAX_VALUE, maxV = -Float.MAX_VALUE;
			for (GeoVertex vertex : quad.vertices()) {
				minU = Math.min(minU, vertex.texU());
				minV = Math.min(minV, vertex.texV());
				maxU = Math.max(maxU, vertex.texU());
				maxV = Math.max(maxV, vertex.texV());
			}
			return new float[]{minU, minV, maxU, maxV};
		}

		private float northArea() {
			return (northRect[2] - northRect[0]) * (northRect[3] - northRect[1]);
		}

		private boolean contains(CubeShape inner) {
			if (northRect == null || inner.northRect == null) return false;
			float[] o = northRect;
			float[] i = inner.northRect;
			return i[0] >= o[0] - UV_EPSILON && i[1] >= o[1] - UV_EPSILON && i[2] <= o[2] + UV_EPSILON && i[3] <= o[3] + UV_EPSILON;
		}

		private float overlap(CubeShape other) {
			if (northRect == null || other.northRect == null) return 0.0f;
			float width = Math.min(northRect[2], other.northRect[2]) - Math.max(northRect[0], other.northRect[0]);
			float height = Math.min(northRect[3], other.northRect[3]) - Math.max(northRect[1], other.northRect[1]);
			return width > 0.0f && height > 0.0f ? width * height : 0.0f;
		}

		private Box slice(CubeShape inner) {
			if (north == null || inner.northRect == null) return box;
			Vector3f a = positionAt(inner.northRect[0], inner.northRect[1]);
			Vector3f b = positionAt(inner.northRect[2], inner.northRect[3]);
			if (a == null || b == null) return box;
			float minX = Mth.clamp(Math.min(a.x, b.x), box.minX, box.maxX);
			float maxX = Mth.clamp(Math.max(a.x, b.x), box.minX, box.maxX);
			float minY = Mth.clamp(Math.min(a.y, b.y), box.minY, box.maxY);
			float maxY = Mth.clamp(Math.max(a.y, b.y), box.minY, box.maxY);
			return new Box(minX, minY, box.minZ, maxX, maxY, box.maxZ);
		}

		private Vector3f positionAt(float u, float v) {
			GeoVertex[] vertices = north.vertices();
			if (vertices.length < 4) return null;
			GeoVertex v0 = vertices[0];
			GeoVertex v1 = vertices[1];
			GeoVertex v3 = vertices[3];
			float du1 = v1.texU() - v0.texU();
			float dv1 = v1.texV() - v0.texV();
			float du3 = v3.texU() - v0.texU();
			float dv3 = v3.texV() - v0.texV();
			float det = du1 * dv3 - dv1 * du3;
			if (Math.abs(det) < EDGE_EPSILON) return null;
			float du = u - v0.texU();
			float dv = v - v0.texV();
			float alpha = (du * dv3 - dv * du3) / det;
			float beta = (du1 * dv - dv1 * du) / det;
			Vector3f origin = v0.position();
			Vector3f edge1 = new Vector3f(v1.position()).sub(origin);
			Vector3f edge3 = new Vector3f(v3.position()).sub(origin);
			return new Vector3f(origin).add(edge1.mul(alpha)).add(edge3.mul(beta));
		}

		private float[] uvAt(Direction direction, Vector3f point) {
			GeoQuad quad = quad(cube, direction);
			if (quad == null || quad.vertices().length < 4) return null;
			GeoVertex v0 = quad.vertices()[0];
			GeoVertex v1 = quad.vertices()[1];
			GeoVertex v3 = quad.vertices()[3];
			Vector3f offset = new Vector3f(point).sub(v0.position());
			float alpha = projection(offset, new Vector3f(v1.position()).sub(v0.position()));
			float beta = projection(offset, new Vector3f(v3.position()).sub(v0.position()));
			return new float[]{
					v0.texU() + alpha * (v1.texU() - v0.texU()) + beta * (v3.texU() - v0.texU()),
					v0.texV() + alpha * (v1.texV() - v0.texV()) + beta * (v3.texV() - v0.texV())
			};
		}

		private static float projection(Vector3f offset, Vector3f edge) {
			float length = edge.lengthSquared();
			return length > EDGE_EPSILON ? Mth.clamp(offset.dot(edge) / length, 0.0f, 1.0f) : 0.0f;
		}
	}
}
