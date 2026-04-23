package com.dragonminez.common.dragonball;

import com.dragonminez.Reference;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DragonBallDataPackResources implements PackResources {
    private final String packId;
    private final byte[] packMcmetaBytes;

    public DragonBallDataPackResources(String packId) {
        this.packId = packId;
        JsonObject packInfo = new JsonObject();
        packInfo.addProperty("description", "DMZ Dragonballs Runtime Data");
        packInfo.addProperty("pack_format", 15);

        JsonObject root = new JsonObject();
        root.add("pack", packInfo);
        this.packMcmetaBytes = root.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        if (elements.length > 0 && "pack.mcmeta".equals(elements[0])) {
            return () -> new ByteArrayInputStream(packMcmetaBytes);
        }
        return null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        if (type != PackType.SERVER_DATA || !location.getNamespace().equals(Reference.MOD_ID)) return null;
        String json = getGeneratedJson(location.getPath());
        if (json == null) return null;
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        return () -> new ByteArrayInputStream(bytes);
    }

    private List<DragonRadarDefinition> getMergedRadars() {
        Map<String, DragonRadarDefinition> merged = new LinkedHashMap<>();
        DragonBallPackManager.LoadedDefinitions external = DragonBallPackManager.getCurrent();
        for (DragonRadarDefinition def : DragonBallDefinitions.getBootstrapRadars()) merged.put(def.getId(), def);
        for (DragonRadarDefinition def : external.radars.values()) merged.put(def.getId(), def);
        for (DragonRadarDefinition def : DragonBallDefinitions.getRadars()) merged.put(def.getId(), def);
        return new ArrayList<>(merged.values());
    }

    @Nullable
    private String getGeneratedJson(String path) {
        for (DragonRadarDefinition radarDefinition : getMergedRadars()) {
            if ("earth_radar".equals(radarDefinition.getId()) || "namek_radar".equals(radarDefinition.getId())) continue;
            DragonRadarRecipeDefinition recipeDefinition = radarDefinition.resolveRecipeDefinition();
            if (recipeDefinition == null) continue;
            String recipePath = "recipes/" + radarDefinition.getItemRegistryName() + ".json";
            if (!path.equals(recipePath)) continue;

            if (recipeDefinition instanceof ShapedDragonRadarRecipeDefinition shaped) {
                return buildShapedRecipeJson(radarDefinition, shaped).toString();
            }
        }
        return null;
    }

    private JsonObject buildShapedRecipeJson(DragonRadarDefinition radarDefinition, ShapedDragonRadarRecipeDefinition recipeDefinition) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:crafting_shaped");
        root.addProperty("category", "redstone");

        var pattern = new com.google.gson.JsonArray();
        pattern.add("OCO");
        pattern.add("PGP");
        pattern.add("CPC");
        root.add("pattern", pattern);

        JsonObject key = new JsonObject();
        JsonObject o = new JsonObject(); o.addProperty("item", "minecraft:observer"); key.add("O", o);
        JsonObject g = new JsonObject(); g.addProperty("item", recipeDefinition.getCpuItemId()); key.add("G", g);
        JsonObject c = new JsonObject(); c.addProperty("item", recipeDefinition.getChipItemId()); key.add("C", c);
        JsonObject p = new JsonObject(); p.addProperty("item", Reference.MOD_ID + ":radar_piece"); key.add("P", p);
        root.add("key", key);

        JsonObject result = new JsonObject();
        result.addProperty("item", Reference.MOD_ID + ":" + radarDefinition.getItemRegistryName());
        result.addProperty("count", 1);
        root.add("result", result);
        return root;
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput resourceOutput) {
        if (type != PackType.SERVER_DATA || !Reference.MOD_ID.equals(namespace)) return;
        for (DragonRadarDefinition radarDefinition : getMergedRadars()) {
            if ("earth_radar".equals(radarDefinition.getId()) || "namek_radar".equals(radarDefinition.getId())) continue;
            DragonRadarRecipeDefinition recipeDefinition = radarDefinition.resolveRecipeDefinition();
            if (recipeDefinition == null) continue;
            publishIfMatches(path, "recipes/" + radarDefinition.getItemRegistryName() + ".json", resourceOutput);
        }
    }

    private void publishIfMatches(String requestedPath, String fullPath, ResourceOutput resourceOutput) {
        if (!fullPath.startsWith(requestedPath)) return;
        resourceOutput.accept(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, fullPath), () -> {
            String json = getGeneratedJson(fullPath);
            byte[] bytes = json == null ? new byte[0] : json.getBytes(StandardCharsets.UTF_8);
            return new ByteArrayInputStream(bytes);
        });
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.SERVER_DATA ? Set.of(Reference.MOD_ID) : Collections.emptySet();
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) {
        try (InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(packMcmetaBytes), StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            String sectionName = deserializer.getMetadataSectionName();
            if (json.has(sectionName)) return deserializer.fromJson(json.getAsJsonObject(sectionName));
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String packId() { return packId; }
    @Override
    public void close() {}
    @Override
    public boolean isBuiltin() { return true; }
}
