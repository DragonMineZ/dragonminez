package com.dragonminez.client.gui.radial;

import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public interface RadialNode {
	Component label(StatsData stats);

	ResourceLocation icon(StatsData stats);

	default String faceText(StatsData stats) {
		return null;
	}

	default int iconTint(StatsData stats) {
		return -1;
	}

	default int labelColor(StatsData stats) {
		return 0xFFFFFF;
	}

	default boolean active(StatsData stats) {
		return false;
	}

	default boolean visible(StatsData stats) {
		return true;
	}

	default boolean interactive(StatsData stats) {
		return true;
	}

	default List<RadialNode> children(StatsData stats) {
		return List.of();
	}

	default boolean expandable(StatsData stats) {
		return !children(stats).isEmpty();
	}

	default void onSelect(StatsData stats) {
	}

	default String orderKey() {
		return "";
	}

	default FormPreview preview(StatsData stats) {
		return null;
	}
}
