package com.dragonminez.client.gui.character.minigames;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.training.RhythmAutoCharter;
import com.dragonminez.common.training.RhythmChart;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCStdlib;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;

final class RhythmClientCharts {
	private static final int GENERATOR_VERSION = 6;
	private static final Gson GSON = new GsonBuilder().create();
	private static final Map<String, RhythmChart> CACHE = new ConcurrentHashMap<>();

	private RhythmClientCharts() {}

	static RhythmChart cached(String key) {
		return key == null ? null : CACHE.get(key);
	}

	private static Path chartFolder() {
		return FMLPaths.GAMEDIR.get().resolve("dragonminez").resolve("rhythm_charts");
	}

	private static String fileStem(String key) {
		return key.replace(':', '_').replaceAll("[^a-zA-Z0-9_.-]", "_");
	}

	private static Path chartFile(String key) {
		return chartFolder().resolve(fileStem(key) + ".json");
	}

	static CompletableFuture<RhythmChart> analyze(String key, ResourceLocation soundId, IntConsumer phase) {
		WeighedSoundEvents events = Minecraft.getInstance().getSoundManager().getSoundEvent(soundId);
		if (events == null) return CompletableFuture.completedFuture(null);
		Sound sound = events.getSound(RandomSource.create());
		ResourceLocation file = sound.getPath();
		return CompletableFuture.supplyAsync(() -> {
			try {
				if (phase != null) phase.accept(RhythmAutoCharter.PHASE_ANALYZING);
				Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(file);
				if (resource.isEmpty()) return null;
				byte[] bytes;
				try (InputStream in = resource.get().open()) {
					bytes = in.readAllBytes();
				}
				String audioHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));

				RhythmChart bundled = readBundled(key, audioHash);
				if (bundled != null) {
					CACHE.put(key, bundled);
					return bundled;
				}

				RhythmChart stored = readStored(key, audioHash);
				if (stored != null) {
					CACHE.put(key, stored);
					return stored;
				}

				float[][] channels = decode(bytes);
				if (channels == null) return null;
				RhythmChart chart = RhythmAutoCharter.generate("auto:" + key, key, soundId.toString(), channels[0], channels[1], phase);
				if (chart != null && RhythmAutoCharter.validate(chart, 0) == null) {
					chart = chart.withAudioHash(audioHash);
					CACHE.put(key, chart);
					writeStored(key, audioHash, chart);
					return chart;
				}
			} catch (Exception e) {
				LogUtil.warn(Env.CLIENT, "Could not analyze song {}: {}", key, e.getMessage());
			}
			return null;
		}, Util.backgroundExecutor());
	}

	private static RhythmChart readBundled(String key, String audioHash) {
		ResourceLocation location = ResourceLocation.tryBuild(Reference.MOD_ID, "rhythm_charts/" + fileStem(key).toLowerCase(java.util.Locale.ROOT) + ".json");
		if (location == null) return null;
		try {
			Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
			if (resource.isEmpty()) return null;
			try (InputStream in = resource.get().open()) {
				JsonObject root = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
				RhythmChart chart = RhythmChart.fromJson("auto:" + key, root);
				if (!chart.matchesAudio(audioHash)) {
					LogUtil.info(Env.CLIENT, "Bundled rhythm chart for {} ignored: the song audio was replaced by a resource pack", key);
					return null;
				}
				return RhythmAutoCharter.validate(chart, 0) == null ? chart : null;
			}
		} catch (Exception e) {
			LogUtil.warn(Env.CLIENT, "Ignoring bundled rhythm chart {}: {}", location, e.getMessage());
			return null;
		}
	}

	private static RhythmChart readStored(String key, String audioHash) {
		Path path = chartFile(key);
		if (!Files.isRegularFile(path)) return null;
		try {
			JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
			if (!root.has("generator") || root.get("generator").getAsInt() != GENERATOR_VERSION) return null;
			if (!root.has("audioHash") || !audioHash.equals(root.get("audioHash").getAsString())) return null;
			RhythmChart chart = RhythmChart.fromJson("auto:" + key, root).withAudioHash(audioHash);
			return RhythmAutoCharter.validate(chart, 0) == null ? chart : null;
		} catch (Exception e) {
			LogUtil.warn(Env.CLIENT, "Ignoring unreadable rhythm chart {}: {}", path.getFileName(), e.getMessage());
			return null;
		}
	}

	private static void writeStored(String key, String audioHash, RhythmChart chart) {
		try {
			Files.createDirectories(chartFolder());
			JsonObject root = new JsonObject();
			root.addProperty("generator", GENERATOR_VERSION);
			root.addProperty("audioHash", audioHash);
			chart.toJson().entrySet().forEach(entry -> root.add(entry.getKey(), entry.getValue()));
			Files.writeString(chartFile(key), GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (Exception e) {
			LogUtil.warn(Env.CLIENT, "Could not save rhythm chart for {}: {}", key, e.getMessage());
		}
	}

	private static float[][] decode(byte[] bytes) {
		ByteBuffer data = MemoryUtil.memAlloc(bytes.length);
		try (MemoryStack stack = MemoryStack.stackPush()) {
			data.put(bytes).flip();
			IntBuffer channels = stack.mallocInt(1);
			IntBuffer rate = stack.mallocInt(1);
			ShortBuffer pcm = STBVorbis.stb_vorbis_decode_memory(data, channels, rate);
			if (pcm == null) return null;
			try {
				int ch = Math.max(1, channels.get(0));
				int frames = pcm.remaining() / ch;
				int sourceRate = rate.get(0);
				if (ch >= 2) {
					float[] left = new float[frames];
					float[] right = new float[frames];
					for (int i = 0; i < frames; i++) {
						left[i] = pcm.get(i * ch) / 32768f;
						right[i] = pcm.get(i * ch + 1) / 32768f;
					}
					return new float[][]{RhythmAutoCharter.resample(left, sourceRate), RhythmAutoCharter.resample(right, sourceRate)};
				}
				float[] mono = new float[frames];
				for (int i = 0; i < frames; i++) mono[i] = pcm.get(i) / 32768f;
				return new float[][]{RhythmAutoCharter.resample(mono, sourceRate), null};
			} finally {
				LibCStdlib.free(pcm);
			}
		} finally {
			MemoryUtil.memFree(data);
		}
	}
}
