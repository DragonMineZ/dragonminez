package com.dragonminez.client.util;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.util.BetaWhitelist;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public final class SkinCacheManager {
    public enum State { OK, NO_PREMIUM, NO_SKIN, TIMEOUT }

    public static final class CacheEntry {
        State state;
        String skinHash;
        String file;

        CacheEntry() {}
        CacheEntry(State state, String skinHash, String file) {
            this.state = state;
            this.skinHash = skinHash;
            this.file = file;
        }
    }

    private static final String CACHE_DIR_NAME = "dmz_skincache";
    private static final String INDEX_FILE = "index.json";
    private static final int CONNECT_TIMEOUT_MS = 6000;
    private static final int READ_TIMEOUT_MS = 6000;
    private static final long THROTTLE_DELAY_MS = 150L;

    private static final int MAX_CONCURRENT_DOWNLOADS = 2;
    private static final long GATE_POLL_MS = 500L;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type INDEX_TYPE = new TypeToken<HashMap<String, CacheEntry>>() {}.getType();

    private static final Map<String, CacheEntry> index = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> registered = new ConcurrentHashMap<>();
    private static final Set<String> inFlight = ConcurrentHashMap.newKeySet();

    private static final BlockingQueue<Task> downloadQueue = new LinkedBlockingQueue<>();

    private static Path cacheDir;
    private static boolean initialized;

    private record Task(String username, boolean force) {}

    private SkinCacheManager() {}

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        startWorkers();

        try {
            cacheDir = Minecraft.getInstance().gameDirectory.toPath().resolve(CACHE_DIR_NAME);
            Files.createDirectories(cacheDir);
            loadIndex();
            registerCachedFromDisk();
        } catch (Exception e) {
            LogUtil.error(Env.CLIENT, "SkinCache: failed to init cache dir: " + e.getMessage());
        }

        preloadAll();
    }

    public static void preloadAll() {
        if (!initialized) return;
        List<String> candidates = BetaWhitelist.getSkinCandidates();
        for (String name : candidates) queueResolve(name, false);
    }

    public static void revalidate() {
        if (!initialized) return;
        registered.clear();
        registerCachedFromDisk();
        List<String> candidates = BetaWhitelist.getSkinCandidates();
        for (String name : candidates) queueResolve(name, true);

    }

    public static ResourceLocation resolveTexture(String username) {
        if (username == null || username.isEmpty()) return DefaultPlayerSkin.getDefaultSkin();
        String key = username.toLowerCase();
        ResourceLocation loc = registered.get(key);
        if (loc != null) return loc;

        if (initialized && !index.containsKey(key)) queueResolve(username, false);
        return DefaultPlayerSkin.getDefaultSkin();
    }

    private static void queueResolve(String username, boolean forceRecheck) {
        if (username == null || username.isEmpty()) return;
        String key = username.toLowerCase();
        if (!inFlight.add(key)) return;
        downloadQueue.add(new Task(username, forceRecheck));
    }

    private static void startWorkers() {
        for (int i = 0; i < MAX_CONCURRENT_DOWNLOADS; i++) {
            Thread t = new Thread(SkinCacheManager::workerLoop, "DMZ-SkinCache-" + (i + 1));
            t.setDaemon(true);
            t.start();
        }
    }

    private static void workerLoop() {
        while (true) {
            Task task;
            try {
                task = downloadQueue.take();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
            String key = task.username().toLowerCase();
            try {
                awaitDownloadableState();
                resolveBlocking(task.username(), task.force());
                Thread.sleep(THROTTLE_DELAY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                LogUtil.error(Env.CLIENT, "SkinCache: error resolving {}: {}", task.username(), e.getMessage());
            } finally {
                inFlight.remove(key);
            }
        }
    }

    private static void awaitDownloadableState() throws InterruptedException {
        while (!canDownloadNow()) {
            Thread.sleep(GATE_POLL_MS);
        }
    }

    private static boolean canDownloadNow() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return false;
        if (mc.getOverlay() != null) return false;
        Screen screen = mc.screen;
        return !(screen instanceof LevelLoadingScreen
                || screen instanceof ReceivingLevelScreen
                || screen instanceof ProgressScreen
                || screen instanceof GenericDirtMessageScreen);
    }

    private static void resolveBlocking(String username, boolean forceRecheck) {
        String key = username.toLowerCase();
        CacheEntry entry = index.get(key);

        if (entry != null && entry.state == State.NO_PREMIUM) return;
        if (!forceRecheck && entry != null && entry.state == State.OK && registered.containsKey(key)) return;

        String uuid = fetchUuid(username);
        if (uuid == null) return;

        String skinUrl = fetchSkinUrl(uuid);
        if (skinUrl == null) {
            putState(key, State.NO_SKIN, null, null);
            return;
        }
        String hash = hashFromUrl(skinUrl);

        if (entry != null && entry.state == State.OK && hash.equals(entry.skinHash) && registered.containsKey(key)) return;
        if (entry != null && entry.file != null) deleteFileQuietly(entry.file);

        byte[] png = downloadBytes(skinUrl);
        if (png == null) {
            putState(key, State.NO_SKIN, hash, null);
            return;
        }

        String fileName = key + "-" + hash + ".png";
        try {
            Files.write(cacheDir.resolve(fileName), png);
        } catch (IOException e) {
            LogUtil.error(Env.CLIENT, "SkinCache: failed to write {}: {}", fileName, e.getMessage());
            putState(key, State.NO_SKIN, hash, null);
            return;
        }

        registerTexture(key, png);
        putState(key, State.OK, hash, fileName);
        LogUtil.info(Env.CLIENT, "SkinCache: cached skin for {} ({})", username, hash);
    }

    private static String fetchUuid(String username) {
        try {
            String body = httpGet("https://api.mojang.com/users/profiles/minecraft/" + username);
            if (body == null || body.isEmpty()) {
                putState(username.toLowerCase(), State.NO_PREMIUM, null, null);
                return null;
            }
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("id")) return json.get("id").getAsString();
            putState(username.toLowerCase(), State.NO_PREMIUM, null, null);
            return null;
        } catch (IOException io) {
            putState(username.toLowerCase(), State.TIMEOUT, null, null);
            return null;
        } catch (Exception e) {
            putState(username.toLowerCase(), State.NO_PREMIUM, null, null);
            return null;
        }
    }

    private static String fetchSkinUrl(String uuid) {
        try {
            String body = httpGet("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
            if (body == null) return null;
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            var properties = json.getAsJsonArray("properties");
            if (properties == null) return null;
            for (var el : properties) {
                JsonObject prop = el.getAsJsonObject();
                if ("textures".equals(prop.get("name").getAsString())) {
                    String decoded = new String(Base64.getDecoder().decode(prop.get("value").getAsString()), StandardCharsets.UTF_8);
                    JsonObject tex = JsonParser.parseString(decoded).getAsJsonObject().getAsJsonObject("textures");
                    if (tex != null && tex.has("SKIN")) {
                        return tex.getAsJsonObject("SKIN").get("url").getAsString();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String httpGet(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "DragonMineZ/" + Reference.MOD_ID);
        try {
            int code = conn.getResponseCode();
            if (code == 204 || code == 404) return null;
            if (code != 200) throw new IOException("HTTP " + code);
            try (InputStream is = conn.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } finally {
            conn.disconnect();
        }
    }

    private static byte[] downloadBytes(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            try {
                if (conn.getResponseCode() != 200) return null;
                try (InputStream is = conn.getInputStream()) {
                    return is.readAllBytes();
                }
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static String hashFromUrl(String url) {
        int slash = url.lastIndexOf('/');
        return slash >= 0 ? url.substring(slash + 1) : url;
    }

    private static void registerCachedFromDisk() {
        for (Map.Entry<String, CacheEntry> e : index.entrySet()) {
            CacheEntry entry = e.getValue();
            if (entry.state != State.OK || entry.file == null) continue;
            Path file = cacheDir.resolve(entry.file);
            if (!Files.exists(file)) {
                entry.state = State.NO_SKIN;
                continue;
            }
            try {
                registerTexture(e.getKey(), Files.readAllBytes(file));
            } catch (IOException io) {
                LogUtil.error(Env.CLIENT, "SkinCache: failed to load cached {}: {}", entry.file, io.getMessage());
            }
        }
    }

    private static void registerTexture(String key, byte[] png) {
        Minecraft.getInstance().execute(() -> {
            NativeImage image;
            try {
                image = NativeImage.read(new ByteArrayInputStream(png));
            } catch (Exception e) {
                LogUtil.error(Env.CLIENT, "SkinCache: failed to decode texture for {}: {}", key, e.getMessage());
                return;
            }
            if (image.getHeight() < 64) {
                image.close();
                return;
            }
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "skins/" + key);
            Minecraft.getInstance().getTextureManager().register(loc, new DynamicTexture(image));
            registered.put(key, loc);
        });
    }

    private static void putState(String key, State state, String hash, String file) {
        index.put(key, new CacheEntry(state, hash, file));
        saveIndex();
    }

    private static synchronized void loadIndex() {
        Path file = cacheDir.resolve(INDEX_FILE);
        if (!Files.exists(file)) return;
        try {
            String json = Files.readString(file);
            Map<String, CacheEntry> loaded = GSON.fromJson(json, INDEX_TYPE);
            if (loaded != null) index.putAll(loaded);
        } catch (Exception e) {
            LogUtil.error(Env.CLIENT, "SkinCache: failed to read index: " + e.getMessage());
        }
    }

    private static synchronized void saveIndex() {
        if (cacheDir == null) return;
        try {
            Files.writeString(cacheDir.resolve(INDEX_FILE), GSON.toJson(index, INDEX_TYPE));
        } catch (IOException e) {
            LogUtil.error(Env.CLIENT, "SkinCache: failed to write index: " + e.getMessage());
        }
    }

    private static void deleteFileQuietly(String fileName) {
        try {
            Files.deleteIfExists(cacheDir.resolve(fileName));
        } catch (IOException ignored) {}
    }
}
