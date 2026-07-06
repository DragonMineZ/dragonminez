package com.dragonminez.client.model;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class KiWeaponModelLoader {

    private static final Map<String, ModelPart> CACHE = new HashMap<>();

    public static ModelPart get(String type) {
        if (type == null) return null;
        String key = type.toLowerCase();
        if (CACHE.containsKey(key)) return CACHE.get(key);
        ModelPart part = build(key);
        CACHE.put(key, part);
        return part;
    }

    public static void clear() {
        CACHE.clear();
    }

    private static ModelPart build(String type) {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/kiweapon_" + type + ".geo.json");
        var resource = Minecraft.getInstance().getResourceManager().getResource(loc);
        if (resource.isEmpty()) return null;

        try (var reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject geometry = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            JsonObject description = geometry.getAsJsonObject("description");
            int texWidth = description.has("texture_width") ? description.get("texture_width").getAsInt() : 64;
            int texHeight = description.has("texture_height") ? description.get("texture_height").getAsInt() : 64;

            MeshDefinition mesh = new MeshDefinition();
            PartDefinition rootPart = mesh.getRoot();

            JsonArray bones = geometry.getAsJsonArray("bones");
            Map<String, PartDefinition> partMap = new HashMap<>();
            Map<String, float[]> pivotMap = new HashMap<>();
            Map<String, float[]> javaRotMap = new HashMap<>();

            float[] defaultPivot = new float[]{0, 24, 0};

            for (var boneElem : bones) {
                JsonObject bone = boneElem.getAsJsonObject();
                String name = bone.has("name") ? bone.get("name").getAsString() : "bone";
                String parentName = bone.has("parent") ? bone.get("parent").getAsString() : null;

                PartDefinition parentPart = rootPart;
                float[] parentPivot = defaultPivot;
                float[] parentJavaRot = new float[]{0, 0, 0};

                if (parentName != null && partMap.containsKey(parentName)) {
                    parentPart = partMap.get(parentName);
                    parentPivot = pivotMap.get(parentName);
                    if (javaRotMap.containsKey(parentName)) {
                        parentJavaRot = javaRotMap.get(parentName);
                    }
                }

                float[] pivot = readVec(bone, "pivot", parentPivot[0], parentPivot[1], parentPivot[2]);
                float[] rot = readVec(bone, "rotation", 0, 0, 0);

                CubeListBuilder cubes = CubeListBuilder.create();
                int rotatedCubeIndex = 0;
                java.util.List<JsonObject> rotatedCubes = new java.util.ArrayList<>();

                if (bone.has("cubes")) {
                    for (var cubeElem : bone.getAsJsonArray("cubes")) {
                        JsonObject cube = cubeElem.getAsJsonObject();
                        if (cube.has("rotation")) {
                            rotatedCubes.add(cube);
                        } else {
                            addCube(cubes, cube, pivot);
                        }
                    }
                }

                float dx = pivot[0] - parentPivot[0];
                float dy = parentPivot[1] - pivot[1];
                float dz = pivot[2] - parentPivot[2];

                if (parentJavaRot[0] != 0 || parentJavaRot[1] != 0 || parentJavaRot[2] != 0) {
                    float rx = -parentJavaRot[0];
                    float cx = (float) Math.cos(rx), sx = (float) Math.sin(rx);
                    float dy1 = dy * cx - dz * sx;
                    float dz1 = dy * sx + dz * cx;
                    dy = dy1; dz = dz1;

                    float ry = -parentJavaRot[1];
                    float cy = (float) Math.cos(ry), sy = (float) Math.sin(ry);
                    float dx1 = dx * cy + dz * sy;
                    float dz2 = -dx * sy + dz * cy;
                    dx = dx1; dz = dz2;

                    float rz = -parentJavaRot[2];
                    float cz = (float) Math.cos(rz), sz = (float) Math.sin(rz);
                    float dx2 = dx * cz - dy * sz;
                    float dy2 = dx * sz + dy * cz;
                    dx = dx2; dy = dy2;
                }

                float jRotX = (float) Math.toRadians(rot[0]);
                float jRotY = (float) Math.toRadians(-rot[1]);
                float jRotZ = (float) Math.toRadians(-rot[2]);

                PartDefinition part = parentPart.addOrReplaceChild(name, cubes, PartPose.offsetAndRotation(dx, dy, dz, jRotX, jRotY, jRotZ));

                for (JsonObject cube : rotatedCubes) {
                    float[] cubePivot = readVec(cube, "pivot", pivot[0], pivot[1], pivot[2]);
                    float[] cubeRot = readVec(cube, "rotation", 0, 0, 0);
                    CubeListBuilder rotated = CubeListBuilder.create();
                    addCube(rotated, cube, cubePivot);

                    part.addOrReplaceChild(name + "_r" + (rotatedCubeIndex++), rotated,
                            PartPose.offsetAndRotation(
                                    cubePivot[0] - pivot[0], pivot[1] - cubePivot[1], cubePivot[2] - pivot[2],
                                    (float) Math.toRadians(cubeRot[0]), (float) Math.toRadians(-cubeRot[1]), (float) Math.toRadians(-cubeRot[2])));
                }

                partMap.put(name, part);
                pivotMap.put(name, pivot);
                javaRotMap.put(name, new float[]{jRotX, jRotY, jRotZ});
            }

            return LayerDefinition.create(mesh, texWidth, texHeight).bakeRoot();
        } catch (Exception e) {
            LogUtil.error(Env.CLIENT, "Failed to load Ki weapon model: " + type, e);
            return null;
        }
    }

    private static void addCube(CubeListBuilder builder, JsonObject cube, float[] pivot) {
        float[] origin = readVec(cube, "origin", 0, 0, 0);
        float[] size = readVec(cube, "size", 0, 0, 0);
        int[] uv = readBoxUv(cube);

        builder.texOffs(uv[0], uv[1]).addBox(
                origin[0] - pivot[0],
                pivot[1] - (origin[1] + size[1]),
                origin[2] - pivot[2],
                size[0], size[1], size[2], CubeDeformation.NONE);
    }

    private static int[] readBoxUv(JsonObject cube) {
        if (cube.has("uv")) {
            var uvElem = cube.get("uv");
            if (uvElem.isJsonArray()) {
                JsonArray arr = uvElem.getAsJsonArray();
                return new int[]{arr.get(0).getAsInt(), arr.get(1).getAsInt()};
            }
            if (uvElem.isJsonObject()) {
                JsonObject uvObj = uvElem.getAsJsonObject();
                if (uvObj.has("north")) {
                    JsonArray north = uvObj.getAsJsonObject("north").getAsJsonArray("uv");
                    return new int[]{north.get(0).getAsInt(), north.get(1).getAsInt()};
                }
            }
        }
        return new int[]{0, 0};
    }

    private static float[] readVec(JsonObject obj, String key, float dx, float dy, float dz) {
        if (obj.has(key)) {
            JsonArray arr = obj.getAsJsonArray(key);
            return new float[]{arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat()};
        }
        return new float[]{dx, dy, dz};
    }
}
