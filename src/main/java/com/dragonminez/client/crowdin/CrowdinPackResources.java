package com.dragonminez.client.crowdin;

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
import java.util.Collections;
import java.util.Set;

public class CrowdinPackResources implements PackResources {

	private final String packId;
	private final byte[] packMcmetaBytes;

	public CrowdinPackResources(String packId) {
		this.packId = packId;

		JsonObject packInfo = new JsonObject();
		packInfo.addProperty("description", "DMZ Live Translations");
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
		if (type == PackType.CLIENT_RESOURCES &&
				location.getNamespace().equals(Reference.MOD_ID) &&
				location.getPath().startsWith("lang/")) {

			String expectedPath = "lang/" + CrowdinManager.getCachedLangCode() + ".json";

			if (location.getPath().equals(expectedPath)) {
				if (CrowdinManager.hasData()) {
					return CrowdinManager::getStream;
				}
			}
		}
		return null;
	}

	@Override
	public void listResources(PackType type, String namespace, String path, ResourceOutput resourceOutput) {}

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
	public String packId() {
		return packId;
	}

	@Override
	public void close() {}

	@Override
	public boolean isBuiltin() {
		return true;
	}
}