package com.dragonminez.client.crowdin;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;

public class CrowdinManager {
	private static final String DISTRIBUTION_HASH = "b1c2021adfbeed64af36778ssuc";
	private static final String BASE_URL = "https://distributions.crowdin.net/" + DISTRIBUTION_HASH;
	private static JsonObject cachedLangData = null;
	private static String cachedLangCode = "";


	public static void fetchLanguage(String mcLangCode) {
		String crowdinPath = formatPath(mcLangCode);

		if ((mcLangCode.equals(cachedLangCode) && cachedLangData != null) || mcLangCode.equals("en_us")) return;

		LogUtil.info(Env.CLIENT, "[DMZ-CROWDIN] Searching updates for: " + crowdinPath);

		HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE_URL + crowdinPath)).GET().build();

		CompletableFuture<HttpResponse<InputStream>> response = client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream());

		response.thenAccept(res -> {
			if (res.statusCode() == 200) {
				try (InputStream bodyStream = res.body()) {
					String encoding = res.headers().firstValue("Content-Encoding").orElse("");
					InputStream effectiveStream = "gzip".equalsIgnoreCase(encoding) ? new GZIPInputStream(bodyStream) : bodyStream;

					try (Reader reader = new InputStreamReader(effectiveStream, StandardCharsets.UTF_8)) {
						cachedLangData = JsonParser.parseReader(reader).getAsJsonObject();
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

	private static String formatPath(String mcCode) {
		String[] parts = mcCode.split("_");
		String lang = parts[0];
		String region = (parts.length > 1) ? parts[1].toUpperCase() : "";

		if (region.isEmpty()) {
			return "/content/" + lang + ".json";
		}

		return "/content/" + lang + "_" + region + ".json";
	}

	public static InputStream getStream() {
		if (cachedLangData == null) return null;
		return new java.io.ByteArrayInputStream(cachedLangData.toString().getBytes());
	}

	public static boolean hasData() {
		return cachedLangData != null;
	}

	public static String getCachedLangCode() {
		return cachedLangCode;
	}
}