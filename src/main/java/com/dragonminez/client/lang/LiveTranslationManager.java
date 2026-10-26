package com.dragonminez.client.lang;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public final class LiveTranslationManager {
	private static final String REPOSITORY = "DragonMineZ/dragonminez";
	private static final String BRANCH = "main";
	private static final String REMOTE_FOLDER = "lang";
	private static final String COMMITS_API_URL = "https://api.github.com/repos/" + REPOSITORY + "/commits";
	private static final String RAW_BASE_URL = "https://raw.githubusercontent.com/" + REPOSITORY + "/";
	private static final String ENABLED_PROPERTY = "dmz.translations.enabled";
	private static final String CACHE_DIR_NAME = "dmz_langcache";
	private static final String SOURCE_LANGUAGE = "en_us";
	private static final String USER_AGENT = "DragonMineZ/" + Reference.VERSION;
	private static final Duration TIMEOUT = Duration.ofSeconds(10);
	private static final Pattern LANGUAGE_CODE = Pattern.compile("[a-z0-9_]+");
	private static final Map<String, String> LANGUAGE_ALIASES = Map.of(
			"es_ar", "es_mx",
			"es_cl", "es_mx",
			"es_ec", "es_mx",
			"es_uy", "es_mx",
			"es_ve", "es_mx"
	);

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final HttpClient HTTP = HttpClient.newBuilder()
			.connectTimeout(TIMEOUT)
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();
	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "DMZ-Translations");
		thread.setDaemon(true);
		return thread;
	});

	private static final Map<String, byte[]> loaded = new ConcurrentHashMap<>();
	private static final Set<String> checked = ConcurrentHashMap.newKeySet();
	private static volatile boolean pendingReload = false;

	private static final class CacheMeta {
		String commitSha;
		String commitDate;
		String etag;
		String downloadedAt;
	}

	private record RemoteCommit(String sha, String date) {}

	private LiveTranslationManager() {}

	public static boolean isEnabled() {
		String override = System.getProperty(ENABLED_PROPERTY);
		if (override != null && !override.isBlank()) return Boolean.parseBoolean(override);
		return true;
	}

	public static void requestLanguage(String languageCode) {
		String file = resolveFile(languageCode);
		if (file == null) return;

		if (!loaded.containsKey(file)) loadFromCache(file);
		if (checked.add(file)) EXECUTOR.execute(() -> checkForUpdates(file));
	}

	@Nullable
	public static byte[] getLanguageData(String languageCode) {
		String file = resolveFile(languageCode);
		if (file == null) return null;

		requestLanguage(languageCode);
		return loaded.get(file);
	}

	public static void clientTick() {
		if (!pendingReload) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.getOverlay() != null) return;

		pendingReload = false;
		mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
		LogUtil.info(Env.CLIENT, "[DMZ-TRANSLATIONS] Applied updated translation for " + mc.options.languageCode + ".");
	}

	@Nullable
	private static String resolveFile(String languageCode) {
		if (!isEnabled() || languageCode == null) return null;

		String code = languageCode.toLowerCase(Locale.ROOT);
		if (code.equals(SOURCE_LANGUAGE) || !LANGUAGE_CODE.matcher(code).matches()) return null;
		return LANGUAGE_ALIASES.getOrDefault(code, code);
	}

	private static void checkForUpdates(String file) {
		CacheMeta meta = readMeta(file);
		boolean cached = meta != null && loaded.containsKey(file);

		RemoteCommit commit;
		try {
			commit = fetchLatestCommit(file);
		} catch (Exception e) {
			LogUtil.warn(Env.CLIENT, "[DMZ-TRANSLATIONS] GitHub API unavailable for " + file + " (" + e.getMessage() + "), checking the file directly.");
			if (!download(file, BRANCH, null, cached ? meta : null)) checked.remove(file);
			return;
		}

		if (commit == null) {
			LogUtil.info(Env.CLIENT, "[DMZ-TRANSLATIONS] No remote translation available for " + file + ".");
			return;
		}

		if (cached && commit.sha().equals(meta.commitSha)) {
			LogUtil.info(Env.CLIENT, "[DMZ-TRANSLATIONS] " + file + " is up to date (last change " + commit.date() + ", downloaded " + meta.downloadedAt + ").");
			return;
		}

		LogUtil.info(Env.CLIENT, "[DMZ-TRANSLATIONS] " + file + " changed on " + commit.date() + (cached ? ", cached copy from " + meta.downloadedAt : ", no cached copy") + ". Downloading.");
		if (!download(file, commit.sha(), commit, cached ? meta : null)) checked.remove(file);
	}

	@Nullable
	private static RemoteCommit fetchLatestCommit(String file) throws IOException, InterruptedException {
		URI uri = URI.create(COMMITS_API_URL + "?sha=" + BRANCH + "&per_page=1&path=" + remotePath(file));
		HttpRequest request = HttpRequest.newBuilder(uri)
				.timeout(TIMEOUT)
				.header("Accept", "application/vnd.github+json")
				.header("User-Agent", USER_AGENT)
				.GET()
				.build();

		HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (response.statusCode() != 200) throw new IOException("status " + response.statusCode());

		JsonArray commits = JsonParser.parseString(response.body()).getAsJsonArray();
		if (commits.size() == 0) return null;

		JsonObject latest = commits.get(0).getAsJsonObject();
		String date = latest.getAsJsonObject("commit").getAsJsonObject("committer").get("date").getAsString();
		return new RemoteCommit(latest.get("sha").getAsString(), date);
	}

	private static boolean download(String file, String ref, @Nullable RemoteCommit commit, @Nullable CacheMeta meta) {
		try {
			HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(RAW_BASE_URL + ref + "/" + remotePath(file)))
					.timeout(TIMEOUT)
					.header("Accept-Encoding", "gzip")
					.header("User-Agent", USER_AGENT)
					.GET();
			if (meta != null && meta.etag != null) builder.header("If-None-Match", meta.etag);

			HttpResponse<InputStream> response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
			int status = response.statusCode();

			if (status == 304) {
				response.body().close();
				if (commit != null) {
					meta.commitSha = commit.sha();
					meta.commitDate = commit.date();
					writeMeta(file, meta);
				}
				LogUtil.info(Env.CLIENT, "[DMZ-TRANSLATIONS] " + file + " content unchanged, keeping cached copy.");
				return true;
			}

			if (status != 200) {
				response.body().close();
				if (status == 404) {
					LogUtil.info(Env.CLIENT, "[DMZ-TRANSLATIONS] No remote translation available for " + file + ".");
					return true;
				}
				LogUtil.warn(Env.CLIENT, "[DMZ-TRANSLATIONS] Failed to download " + file + ". Status code: " + status);
				return false;
			}

			byte[] bytes;
			try (InputStream body = response.body()) {
				boolean gzip = "gzip".equalsIgnoreCase(response.headers().firstValue("Content-Encoding").orElse(""));
				try (InputStream stream = gzip ? new GZIPInputStream(body) : body) {
					bytes = stream.readAllBytes();
				}
			}
			JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();

			CacheMeta updated = new CacheMeta();
			updated.commitSha = commit != null ? commit.sha() : null;
			updated.commitDate = commit != null ? commit.date() : null;
			updated.etag = response.headers().firstValue("ETag").orElse(null);
			updated.downloadedAt = Instant.now().toString();

			writeAtomically(cacheDir().resolve(file + ".json"), bytes);
			writeMeta(file, updated);
			loaded.put(file, bytes);

			String selected = resolveFile(Minecraft.getInstance().options.languageCode);
			if (file.equals(selected)) pendingReload = true;

			LogUtil.info(Env.CLIENT, "[DMZ-TRANSLATIONS] Downloaded translation for " + file + ".");
			return true;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		} catch (Exception e) {
			LogUtil.warn(Env.CLIENT, "[DMZ-TRANSLATIONS] Failed to download translation for " + file + ": " + e.getMessage());
			return false;
		}
	}

	private static void loadFromCache(String file) {
		Path path = cacheDir().resolve(file + ".json");
		if (!Files.isRegularFile(path)) return;

		try {
			byte[] bytes = Files.readAllBytes(path);
			JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
			loaded.put(file, bytes);
		} catch (Exception e) {
			LogUtil.warn(Env.CLIENT, "[DMZ-TRANSLATIONS] Discarding unreadable cached translation for " + file + ": " + e.getMessage());
			try {
				Files.deleteIfExists(path);
				Files.deleteIfExists(cacheDir().resolve(file + ".meta.json"));
			} catch (IOException ignored) {}
		}
	}

	@Nullable
	private static CacheMeta readMeta(String file) {
		Path path = cacheDir().resolve(file + ".meta.json");
		if (!Files.isRegularFile(path)) return null;

		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			return GSON.fromJson(reader, CacheMeta.class);
		} catch (Exception e) {
			return null;
		}
	}

	private static void writeMeta(String file, CacheMeta meta) throws IOException {
		writeAtomically(cacheDir().resolve(file + ".meta.json"), GSON.toJson(meta).getBytes(StandardCharsets.UTF_8));
	}

	private static void writeAtomically(Path target, byte[] bytes) throws IOException {
		Files.createDirectories(target.getParent());
		Path temp = target.resolveSibling(target.getFileName() + ".tmp");
		Files.write(temp, bytes);
		try {
			Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static Path cacheDir() {
		return FMLPaths.GAMEDIR.get().resolve(CACHE_DIR_NAME);
	}

	private static String remotePath(String file) {
		return REMOTE_FOLDER + "/" + file + ".json";
	}
}
