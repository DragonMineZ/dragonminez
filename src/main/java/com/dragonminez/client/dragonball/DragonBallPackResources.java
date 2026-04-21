package com.dragonminez.client.dragonball;

import com.dragonminez.Reference;
import com.dragonminez.common.dragonball.*;
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

public class DragonBallPackResources implements PackResources {
	private final String packId;
	private final byte[] packMcmetaBytes;

	public DragonBallPackResources(String packId) {
		this.packId = packId;
		JsonObject packInfo = new JsonObject();
		packInfo.addProperty("description", "DMZ Dragonballs Runtime Resources");
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
		if (type != PackType.CLIENT_RESOURCES || !location.getNamespace().equals(Reference.MOD_ID)) return null;
		String json = getGeneratedJson(location.getPath());
		if (json == null) return null;
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		return () -> new ByteArrayInputStream(bytes);
	}

	private List<DragonBallSetDefinition> getMergedBallSets() {
		Map<String, DragonBallSetDefinition> merged = new LinkedHashMap<>();
		for (DragonBallSetDefinition def : DragonBallDefinitions.getBootstrapBallSets()) merged.put(def.getId(), def);
		DragonBallPackManager.LoadedDefinitions external = DragonBallPackManager.loadAll();
		for (DragonBallSetDefinition def : external.ballSets.values()) merged.put(def.getId(), def);
		for (DragonBallSetDefinition def : DragonBallDefinitions.getBallSets()) merged.put(def.getId(), def);
		return new ArrayList<>(merged.values());
	}

	private List<DragonRadarDefinition> getMergedRadars() {
		Map<String, DragonRadarDefinition> merged = new LinkedHashMap<>();
		DragonBallPackManager.LoadedDefinitions external = DragonBallPackManager.loadAll();
		for (DragonRadarDefinition def : DragonBallDefinitions.getBootstrapRadars()) merged.put(def.getId(), def);
		for (DragonRadarDefinition def : external.radars.values()) merged.put(def.getId(), def);
		for (DragonRadarDefinition def : DragonBallDefinitions.getRadars()) merged.put(def.getId(), def);
		return new ArrayList<>(merged.values());
	}

	private String humanize(String raw) {
		String[] parts = raw.replace('-', '_').split("_");
		StringBuilder out = new StringBuilder();
		for (String part : parts) {
			if (part.isBlank()) continue;
			if (out.length() > 0) out.append(' ');
			out.append(Character.toUpperCase(part.charAt(0)));
			if (part.length() > 1) out.append(part.substring(1).toLowerCase(Locale.ROOT));
		}
		return out.length() == 0 ? raw : out.toString();
	}

	private boolean isBuiltinVanillaStyleSet(String setId) {
		return "earth".equals(setId) || "namek".equals(setId);
	}

	private String getSetName(DragonBallSetDefinition setDefinition) {
		return setDefinition.getDisplayName().orElseGet(() -> humanize(setDefinition.getId()));
	}

	private String getRadarDisplay(DragonRadarDefinition radarDefinition) {
		if (radarDefinition.getBallSetId() != null) {
			for (DragonBallSetDefinition set : getMergedBallSets()) {
				if (radarDefinition.getBallSetId().equals(set.getId())) {
					String setName = getSetName(set);
					return "§eDragon Radar (§9" + setName + "§e)";
				}
			}
		}
		String setName = radarDefinition.getDisplayName().orElseGet(() -> humanize(radarDefinition.getItemRegistryName()));
		return "§eDragon Radar (§9" + setName + "§e)";
	}

	private String getBallDisplay(DragonBallSetDefinition setDefinition, int star) {
		String setName = getSetName(setDefinition);
		return "§9" + setName + "'s §rDragon Ball (" + star + " Star)";
	}

	private String getRadarTooltip(DragonRadarDefinition radarDefinition) {
		if (radarDefinition.getBallSetId() != null) {
			for (DragonBallSetDefinition set : getMergedBallSets()) {
				if (radarDefinition.getBallSetId().equals(set.getId())) {
					String setName = getSetName(set);
					return "§7Let's go catch the " + setName + " Dragon Balls!";
				}
			}
		}
		String setName = radarDefinition.getDisplayName().orElseGet(() -> humanize(radarDefinition.getItemRegistryName()));
		return "§7Let's go catch the " + setName + " Dragon Balls!";
	}

	private String getChipDisplay(DragonRadarDefinition radarDefinition) {
		if (radarDefinition.getBallSetId() != null) {
			for (DragonBallSetDefinition set : getMergedBallSets()) {
				if (radarDefinition.getBallSetId().equals(set.getId())) {
					return "Radar's Chip (" + getSetName(set) + ")";
				}
			}
		}
		String setName = radarDefinition.getDisplayName().orElseGet(() -> humanize(radarDefinition.getItemRegistryName()));
		return "Radar's Chip (" + setName + ")";
	}

	private String getCpuDisplay(DragonRadarDefinition radarDefinition) {
		if (radarDefinition.getBallSetId() != null) {
			for (DragonBallSetDefinition set : getMergedBallSets()) {
				if (radarDefinition.getBallSetId().equals(set.getId())) {
					return "Radar's CPU (" + getSetName(set) + ")";
				}
			}
		}
		String setName = radarDefinition.getDisplayName().orElseGet(() -> humanize(radarDefinition.getItemRegistryName()));
		return "Radar's CPU (" + setName + ")";
	}

	private String generateLangJson() {
		JsonObject root = new JsonObject();
		for (DragonBallSetDefinition setDefinition : getMergedBallSets()) {
			if (isBuiltinVanillaStyleSet(setDefinition.getId())) continue;
			for (int star : setDefinition.getStars()) {
				String registryName = setDefinition.getBlockRegistryNameForStar(star);
				if (registryName == null) continue;
				String label = getBallDisplay(setDefinition, star);
				root.addProperty("block." + Reference.MOD_ID + "." + registryName, label);
				root.addProperty("item." + Reference.MOD_ID + "." + registryName, label);
			}
		}
		for (DragonRadarDefinition radarDefinition : getMergedRadars()) {
			if (radarDefinition.getBallSetId() != null && isBuiltinVanillaStyleSet(radarDefinition.getBallSetId())) continue;
			String label = getRadarDisplay(radarDefinition);
			root.addProperty("item." + Reference.MOD_ID + "." + radarDefinition.getItemRegistryName(), label);
			root.addProperty(radarDefinition.getTooltipKey(), getRadarTooltip(radarDefinition));
			radarDefinition.getChipRegistryName().ifPresent(registryName ->
				root.addProperty("item." + Reference.MOD_ID + "." + registryName, getChipDisplay(radarDefinition)));
			radarDefinition.getCpuRegistryName().ifPresent(registryName ->
				root.addProperty("item." + Reference.MOD_ID + "." + registryName, getCpuDisplay(radarDefinition)));
		}
		return root.toString();
	}

	private String getChipTexture(DragonRadarDefinition radarDefinition) {
		String source = radarDefinition.getChipModelItemId().orElse("dragonminez:t1_radar_chip");
		ResourceLocation rl = ResourceLocation.tryParse(source);
		if (rl == null) return Reference.MOD_ID + ":item/t1_radar_chip";
		return rl.getNamespace() + ":item/" + rl.getPath();
	}

	private String getCpuTexture(DragonRadarDefinition radarDefinition) {
		String source = radarDefinition.getCpuModelItemId().orElse("dragonminez:t1_radar_cpu");
		ResourceLocation rl = ResourceLocation.tryParse(source);
		if (rl == null) return Reference.MOD_ID + ":item/t1_radar_cpu";
		return rl.getNamespace() + ":item/" + rl.getPath();
	}

	@Nullable
	private String getGeneratedJson(String path) {
		if (path.equals("lang/en_us.json")) {
			return generateLangJson();
		}
		for (DragonBallSetDefinition setDefinition : getMergedBallSets()) {
			DragonBallSetAssetDefinition assets = setDefinition.resolveAssetDefinition();
			for (int star : setDefinition.getStars()) {
				String registryName = setDefinition.getBlockRegistryNameForStar(star);
				if (registryName == null) continue;

				if (path.equals("blockstates/" + registryName + ".json")) {
					JsonObject root = new JsonObject();
					JsonObject variants = new JsonObject();
					JsonObject variant = new JsonObject();
					variant.addProperty("model", Reference.MOD_ID + ":block/" + registryName);
					variants.add("", variant);
					root.add("variants", variants);
					return root.toString();
				}

				if (path.equals("models/block/" + registryName + ".json")) {
					String texture = assets != null && assets.getFlatTexturePathForStar(star).isPresent()
						? assets.getFlatTexturePathForStar(star).get()
						: Reference.MOD_ID + ":item/" + registryName;
					JsonObject root = new JsonObject();
					root.addProperty("parent", "minecraft:block/cube_all");
					JsonObject textures = new JsonObject();
					textures.addProperty("all", texture);
					root.add("textures", textures);
					return root.toString();
				}

				if (path.equals("models/item/" + registryName + ".json")) {
					String texture = assets != null && assets.getInventoryTexturePathForStar(star).isPresent()
						? assets.getInventoryTexturePathForStar(star).get()
						: Reference.MOD_ID + ":item/" + registryName;
					JsonObject root = new JsonObject();
					root.addProperty("parent", "minecraft:item/generated");
					JsonObject textures = new JsonObject();
					textures.addProperty("layer0", texture);
					root.add("textures", textures);
					return root.toString();
				}
			}
		}

		for (DragonRadarDefinition radarDefinition : getMergedRadars()) {
			if (path.equals("models/item/" + radarDefinition.getItemRegistryName() + ".json")) {
				DragonRadarAssetDefinition assets = radarDefinition.resolveAssetDefinition();
				String texture = assets != null && assets.getItemTexturePath().isPresent()
					? assets.getItemTexturePath().get()
					: Reference.MOD_ID + ":item/dball_radar";
				JsonObject root = new JsonObject();
				root.addProperty("parent", "minecraft:item/generated");
				JsonObject textures = new JsonObject();
				textures.addProperty("layer0", texture);
				root.add("textures", textures);
				return root.toString();
			}
			radarDefinition.getChipRegistryName().ifPresent(registryName -> {});
			if (radarDefinition.getChipRegistryName().isPresent() && path.equals("models/item/" + radarDefinition.getChipRegistryName().get() + ".json")) {
				JsonObject root = new JsonObject();
				root.addProperty("parent", "minecraft:item/generated");
				JsonObject textures = new JsonObject();
				textures.addProperty("layer0", getChipTexture(radarDefinition));
				root.add("textures", textures);
				return root.toString();
			}
			if (radarDefinition.getCpuRegistryName().isPresent() && path.equals("models/item/" + radarDefinition.getCpuRegistryName().get() + ".json")) {
				JsonObject root = new JsonObject();
				root.addProperty("parent", "minecraft:item/generated");
				JsonObject textures = new JsonObject();
				textures.addProperty("layer0", getCpuTexture(radarDefinition));
				root.add("textures", textures);
				return root.toString();
			}
		}
		return null;
	}

	@Override
	public void listResources(PackType type, String namespace, String path, ResourceOutput resourceOutput) {
		if (type != PackType.CLIENT_RESOURCES || !Reference.MOD_ID.equals(namespace)) return;
		publishIfMatches(path, "lang/en_us.json", resourceOutput);
		for (DragonBallSetDefinition setDefinition : getMergedBallSets()) {
			for (int star : setDefinition.getStars()) {
				String registryName = setDefinition.getBlockRegistryNameForStar(star);
				if (registryName == null) continue;
				publishIfMatches(path, "blockstates/" + registryName + ".json", resourceOutput);
				publishIfMatches(path, "models/block/" + registryName + ".json", resourceOutput);
				publishIfMatches(path, "models/item/" + registryName + ".json", resourceOutput);
			}
		}
		for (DragonRadarDefinition radarDefinition : getMergedRadars()) {
			publishIfMatches(path, "models/item/" + radarDefinition.getItemRegistryName() + ".json", resourceOutput);
			radarDefinition.getChipRegistryName().ifPresent(registryName -> publishIfMatches(path, "models/item/" + registryName + ".json", resourceOutput));
			radarDefinition.getCpuRegistryName().ifPresent(registryName -> publishIfMatches(path, "models/item/" + registryName + ".json", resourceOutput));
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
		return type == PackType.CLIENT_RESOURCES ? Set.of(Reference.MOD_ID) : Collections.emptySet();
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
