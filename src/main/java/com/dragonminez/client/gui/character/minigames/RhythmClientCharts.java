package com.dragonminez.client.gui.character.minigames;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
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
import net.minecraft.world.item.RecordItem;
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

final class RhythmClientCharts {
	private static final int GENERATOR_VERSION = 2;
	private static final Gson GSON = new GsonBuilder().create();
	private static final Map<String, RhythmChart> CACHE = new ConcurrentHashMap<>();

	private RhythmClientCharts() {}

	static RhythmChart cached(String itemId) {
		return itemId == null ? null : CACHE.get(itemId);
	}

	private static Path chartFolder() {
		return FMLPaths.GAMEDIR.get().resolve("dragonminez").resolve("rhythm_charts");
	}

	private static Path chartFile(String itemId) {
		return chartFolder().resolve(itemId.replace(':', '_').replaceAll("[^a-zA-Z0-9_.-]", "_") + ".json");
	}

	static CompletableFuture<RhythmChart> analyze(String itemId, RecordItem record) {
		ResourceLocation soundId = record.getSound().getLocation();
		WeighedSoundEvents events = Minecraft.getInstance().getSoundManager().getSoundEvent(soundId);
		if (events == null) return CompletableFuture.completedFuture(null);
		Sound sound = events.getSound(RandomSource.create());
		ResourceLocation file = sound.getPath();
		return CompletableFuture.supplyAsync(() -> {
			try {
				Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(file);
				if (resource.isEmpty()) return null;
				byte[] bytes;
				try (InputStream in = resource.get().open()) {
					bytes = in.readAllBytes();
				}
				String audioHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));

				RhythmChart stored = readStored(itemId, audioHash);
				if (stored != null) {
					CACHE.put(itemId, stored);
					return stored;
				}

				float[] mono = decode(bytes);
				if (mono == null) return null;
				RhythmChart chart = RhythmAutoCharter.generate("auto:" + itemId, itemId, soundId.toString(), mono);
				if (chart != null && RhythmAutoCharter.validate(chart, 0) == null) {
					CACHE.put(itemId, chart);
					writeStored(itemId, audioHash, chart);
					return chart;
				}
			} catch (Exception e) {
				LogUtil.warn(Env.CLIENT, "Could not analyze music disc {}: {}", itemId, e.getMessage());
			}
			return null;
		}, Util.backgroundExecutor());
	}

	private static RhythmChart readStored(String itemId, String audioHash) {
		Path path = chartFile(itemId);
		if (!Files.isRegularFile(path)) return null;
		try {
			JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
			if (!root.has("generator") || root.get("generator").getAsInt() != GENERATOR_VERSION) return null;
			if (!root.has("audioHash") || !audioHash.equals(root.get("audioHash").getAsString())) return null;
			RhythmChart chart = RhythmChart.fromJson("auto:" + itemId, root);
			return RhythmAutoCharter.validate(chart, 0) == null ? chart : null;
		} catch (Exception e) {
			LogUtil.warn(Env.CLIENT, "Ignoring unreadable rhythm chart {}: {}", path.getFileName(), e.getMessage());
			return null;
		}
	}

	private static void writeStored(String itemId, String audioHash, RhythmChart chart) {
		try {
			Files.createDirectories(chartFolder());
			JsonObject root = new JsonObject();
			root.addProperty("generator", GENERATOR_VERSION);
			root.addProperty("audioHash", audioHash);
			chart.toJson().entrySet().forEach(entry -> root.add(entry.getKey(), entry.getValue()));
			Files.writeString(chartFile(itemId), GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (Exception e) {
			LogUtil.warn(Env.CLIENT, "Could not save rhythm chart for {}: {}", itemId, e.getMessage());
		}
	}

	private static float[] decode(byte[] bytes) {
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
				float[] mono = new float[frames];
				for (int i = 0; i < frames; i++) {
					float sum = 0;
					for (int c = 0; c < ch; c++) sum += pcm.get(i * ch + c);
					mono[i] = sum / (ch * 32768f);
				}
				return RhythmAutoCharter.resample(mono, rate.get(0));
			} finally {
				LibCStdlib.free(pcm);
			}
		} finally {
			MemoryUtil.memFree(data);
		}
	}
}
