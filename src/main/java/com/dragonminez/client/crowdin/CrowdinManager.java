package com.dragonminez.client.crowdin;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import net.minecraft.client.Minecraft;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;

public class CrowdinManager {
	private static final String DISTRIBUTION_HASH = "d73150d692be7df04ba8738ssuc";
	private static final String BASE_URL = "https://distributions.crowdin.net/" + DISTRIBUTION_HASH;
	private static final String CROWDIN_OVERRIDE_PROPERTY = "dmz.crowdin.enabled";
	private static JsonObject cachedLangData = null;
	@Getter
	private static String cachedLangCode = "";


	public static void fetchLanguage(String mcLangCode) {
		if (!isLiveTranslationsEnabled()) return;

		String crowdinPath = formatPath(mcLangCode);

		if ((mcLangCode.equals(cachedLangCode) && cachedLangData != null) || mcLangCode.equals("en_us")) return;

		LogUtil.info(Env.CLIENT, "[DMZ-CROWDIN] Searching updates for: " + crowdinPath);

		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(BASE_URL + crowdinPath))
				.header("Accept-Encoding", "gzip")
				.GET()
				.build();

		CompletableFuture<HttpResponse<InputStream>> responseFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream());

		responseFuture.thenAccept(res -> {
			if (res.statusCode() == 200) {
				try (InputStream bodyStream = res.body()) {
					String encoding = res.headers().firstValue("Content-Encoding").orElse("");
					InputStream effectiveStream = "gzip".equalsIgnoreCase(encoding) ? new GZIPInputStream(bodyStream) : bodyStream;

					try (Reader reader = new InputStreamReader(effectiveStream, StandardCharsets.UTF_8)) {
						JsonObject rawJson = JsonParser.parseReader(reader).getAsJsonObject();
						fixColors(rawJson);
						cachedLangData = rawJson;
						cachedLangCode = mcLangCode;

						LogUtil.info(Env.CLIENT, "[DMZ-CROWDIN] Remote translation for " + mcLangCode + " loaded successfully.");

						if (Minecraft.getInstance().screen != null) {
							Minecraft.getInstance().execute(() -> Minecraft.getInstance().reloadResourcePacks());
						}
					}
				} catch (Exception e) {
					LogUtil.error(Env.CLIENT, "[DMZ-CROWDIN] Failed to parse translation data for " + mcLangCode, e);
				}
			} else {
				LogUtil.warn(Env.CLIENT, "[DMZ-CROWDIN] No remote translation found for " + mcLangCode + ". Status code: " + res.statusCode());
			}
		});
	}

	public static boolean isLiveTranslationsEnabled() {
		String override = System.getProperty(CROWDIN_OVERRIDE_PROPERTY);
		if (override != null && !override.isBlank()) {
			return Boolean.parseBoolean(override);
		}

		boolean production = isProductionEnvironment();
		boolean defaultValue = production;
		Boolean configuredValue = null;
		try {
			configuredValue = ConfigManager.getUserConfig().getHud().getLiveCrowdinTranslations();
		} catch (Exception ignored) {
		}

		if (configuredValue == null) return defaultValue;
		return configuredValue;
	}

	public static void clearCache() {
		cachedLangData = null;
		cachedLangCode = "";
	}

	private static boolean isProductionEnvironment() {
		try {
			Class<?> fmlLoaderClass = Class.forName("net.minecraftforge.fml.loading.FMLLoader");
			Object result = fmlLoaderClass.getMethod("isProduction").invoke(null);
			if (result instanceof Boolean bool) {
				return bool;
			}
		} catch (Exception ignored) {
		}
		return false;
	}

	private static void fixColors(JsonObject json) {
		Set<Map.Entry<String, JsonElement>> entries = json.entrySet();
		for (Map.Entry<String, JsonElement> entry : entries) {
			JsonElement element = entry.getValue();

			if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
				String originalText = element.getAsString();
				if (originalText.contains("&")) {
					json.addProperty(entry.getKey(), originalText.replace("&", "§"));
				}
			} else if (element.isJsonObject()) {
				fixColors(element.getAsJsonObject());
			}
		}
	}

	private static String formatPath(String mcCode) {
		String basePath = "/content/src/main/resources/assets/dragonminez/lang/";

		String[] parts = mcCode.split("_");
		String lang = parts[0];
		String region = (parts.length > 1) ? parts[1].toUpperCase() : "";

		String fileName;
		if (region.isEmpty()) fileName = lang + ".json";
		else fileName = lang + "_" + region + ".json";

		return basePath + fileName;
	}

	public static InputStream getStream() {
		if (cachedLangData == null) return new ByteArrayInputStream(new byte[0]);
		return new ByteArrayInputStream(cachedLangData.toString().getBytes(StandardCharsets.UTF_8));
	}

	public static boolean hasData() {
		return cachedLangData != null;
	}

}