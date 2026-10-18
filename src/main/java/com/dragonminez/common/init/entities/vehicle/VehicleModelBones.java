package com.dragonminez.common.init.entities.vehicle;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class VehicleModelBones {

	public record Extents(Vec3 min, Vec3 max) {
	}

	public record Layout(Map<String, Vec3> pivots, Map<String, Vec3> cubeTops, @Nullable Extents extents) {
		public static final Layout EMPTY = new Layout(Map.of(), Map.of(), null);

		@Nullable
		public Vec3 seat(String bone) {
			Vec3 top = cubeTops.get(bone);
			return top != null ? top : pivots.get(bone);
		}
	}

	private static final Map<String, Layout> CACHE = new HashMap<>();

	private VehicleModelBones() {
	}

	public static synchronized Layout load(String model) {
		return CACHE.computeIfAbsent(model, VehicleModelBones::read);
	}

	private static Layout read(String model) {
		String path = "/assets/" + Reference.MOD_ID + "/geo/entity/" + model + ".geo.json";
		try (InputStream stream = open(path)) {
			if (stream == null) {
				LogUtil.warn(Env.COMMON, "Vehicle model {} not found, using fallback seats", path);
				return Layout.EMPTY;
			}
			JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			JsonObject geometry = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
			return parse(geometry.getAsJsonArray("bones"));
		} catch (Exception e) {
			LogUtil.warn(Env.COMMON, "Failed to read vehicle model {}: {}", path, e.getMessage());
			return Layout.EMPTY;
		}
	}

	private static InputStream open(String path) {
		InputStream stream = VehicleModelBones.class.getResourceAsStream(path);
		if (stream == null) stream = VehicleModelBones.class.getResourceAsStream(path.substring(1));
		if (stream == null) stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path.substring(1));
		return stream;
	}

	private static Layout parse(JsonArray bones) {
		Map<String, JsonObject> byName = new HashMap<>();
		for (JsonElement element : bones) {
			JsonObject bone = element.getAsJsonObject();
			byName.put(bone.get("name").getAsString(), bone);
		}

		Map<String, Vec3> pivots = new HashMap<>();
		Map<String, Vec3> cubeTops = new HashMap<>();
		Vector3f min = new Vector3f(Float.MAX_VALUE);
		Vector3f max = new Vector3f(-Float.MAX_VALUE);
		for (JsonObject bone : byName.values()) {
			String name = bone.get("name").getAsString();
			pivots.put(name, toVec(applyParents(bone, vec(bone, "pivot"), byName)));
			if (!bone.has("cubes") || bone.getAsJsonArray("cubes").isEmpty()) continue;
			Vector3f boneMin = new Vector3f(Float.MAX_VALUE);
			Vector3f boneMax = new Vector3f(-Float.MAX_VALUE);
			for (JsonElement cubeElement : bone.getAsJsonArray("cubes")) {
				JsonObject cube = cubeElement.getAsJsonObject();
				Vector3f origin = vec(cube, "origin");
				Vector3f size = vec(cube, "size");
				for (int corner = 0; corner < 8; corner++) {
					Vector3f point = new Vector3f(
							origin.x + ((corner & 1) != 0 ? size.x : 0.0F),
							origin.y + ((corner & 2) != 0 ? size.y : 0.0F),
							origin.z + ((corner & 4) != 0 ? size.z : 0.0F));
					if (cube.has("rotation")) rotateAround(point, vec(cube, "pivot"), vec(cube, "rotation"));
					point = applyParents(bone, rotateAround(point, vec(bone, "pivot"), vec(bone, "rotation")), byName);
					boneMin.min(point);
					boneMax.max(point);
				}
			}
			min.min(boneMin);
			max.max(boneMax);
			cubeTops.put(name, new Vec3((boneMin.x + boneMax.x) * 0.5F, boneMax.y, (boneMin.z + boneMax.z) * 0.5F));
		}
		Extents extents = min.x <= max.x ? new Extents(toVec(min), toVec(max)) : null;
		return new Layout(Map.copyOf(pivots), Map.copyOf(cubeTops), extents);
	}

	private static Vector3f applyParents(JsonObject bone, Vector3f point, Map<String, JsonObject> byName) {
		JsonObject parent = bone.has("parent") ? byName.get(bone.get("parent").getAsString()) : null;
		int guard = 0;
		while (parent != null && guard++ < 64) {
			rotateAround(point, vec(parent, "pivot"), vec(parent, "rotation"));
			parent = parent.has("parent") ? byName.get(parent.get("parent").getAsString()) : null;
		}
		return point;
	}

	private static Vector3f rotateAround(Vector3f point, Vector3f pivot, Vector3f rotation) {
		if (rotation.lengthSquared() == 0.0F) return point;
		Matrix3f matrix = new Matrix3f()
				.rotateZ(rotation.z * Mth.DEG_TO_RAD)
				.rotateY(-rotation.y * Mth.DEG_TO_RAD)
				.rotateX(-rotation.x * Mth.DEG_TO_RAD);
		Vector3f local = new Vector3f(-(point.x - pivot.x), point.y - pivot.y, point.z - pivot.z);
		matrix.transform(local);
		return point.set(pivot.x - local.x, pivot.y + local.y, pivot.z + local.z);
	}

	private static Vector3f vec(JsonObject object, String key) {
		if (!object.has(key)) return new Vector3f();
		JsonArray array = object.getAsJsonArray(key);
		return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
	}

	private static Vec3 toVec(Vector3f vector) {
		return new Vec3(vector.x, vector.y, vector.z);
	}
}
