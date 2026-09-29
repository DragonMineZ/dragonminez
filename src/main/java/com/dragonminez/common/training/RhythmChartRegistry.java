package com.dragonminez.common.training;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class RhythmChartRegistry extends SimpleJsonResourceReloadListener {
	public static final String DIRECTORY = "rhythm_charts";
	private static final Gson GSON = new GsonBuilder().create();
	public static final RhythmChartRegistry INSTANCE = new RhythmChartRegistry();

	private static Map<String, RhythmChart> byItem = Map.of();
	private static Map<String, RhythmChart> bySound = Map.of();

	private RhythmChartRegistry() {
		super(GSON, DIRECTORY);
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
		Map<String, RhythmChart> items = new HashMap<>();
		Map<String, RhythmChart> sounds = new HashMap<>();
		for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
			try {
				RhythmChart chart = RhythmChart.fromJson(entry.getKey().toString(), entry.getValue().getAsJsonObject());
				if (!chart.item.isEmpty()) items.put(chart.item, chart);
				if (!chart.sound.isEmpty()) sounds.put(chart.sound, chart);
			} catch (Exception e) {
				LogUtil.warn(Env.COMMON, "Invalid rhythm chart {}: {}", entry.getKey(), e.getMessage());
			}
		}
		byItem = Map.copyOf(items);
		bySound = Map.copyOf(sounds);
		LogUtil.info(Env.COMMON, "Loaded {} rhythm chart(s) from datapacks", items.size());
	}

	@Nullable
	public static RhythmChart find(String itemId, String soundId) {
		RhythmChart chart = itemId == null ? null : byItem.get(itemId);
		if (chart == null && soundId != null) chart = bySound.get(soundId);
		return chart;
	}
}
