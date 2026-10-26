package com.dragonminez.client.lang;

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

public class LiveTranslationPackResources implements PackResources {
	private static final String LANG_PREFIX = "lang/";
	private static final String LANG_SUFFIX = ".json";

	private final String packId;
	private final byte[] packMcmetaBytes;

	public LiveTranslationPackResources(String packId) {
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
		if (type != PackType.CLIENT_RESOURCES || !location.getNamespace().equals(Reference.MOD_ID)) return null;

		String path = location.getPath();
		if (!path.startsWith(LANG_PREFIX) || !path.endsWith(LANG_SUFFIX)) return null;

		String languageCode = path.substring(LANG_PREFIX.length(), path.length() - LANG_SUFFIX.length());
		byte[] data = LiveTranslationManager.getLanguageData(languageCode);
		if (data == null) return null;
		return () -> new ByteArrayInputStream(data);
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
