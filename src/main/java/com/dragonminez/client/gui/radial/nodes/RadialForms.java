package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.RadialLayoutStore;
import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

public final class RadialForms {
	private static final int MAX_SLOTS = 5;

	private RadialForms() {
	}

	public static List<RadialNode> superForms(StatsData stats) {
		return forms(stats, "superforms", type -> type.contains("super") || type.contains("legendary"));
	}

	public static List<RadialNode> moreForms(StatsData stats) {
		return forms(stats, "moreforms", type -> !type.contains("super") && !type.contains("legendary"));
	}

	public static List<RadialNode> stackForms(StatsData stats) {
		String race = stats.getCharacter().getRaceName();
		List<RadialNode> out = new ArrayList<>();
		Map<String, FormConfig> groups = ConfigManager.getAllStackForms();
		if (groups != null) {
			for (String group : groups.keySet()) {
				for (String form : TransformationsHelper.getSelectableStackFormNames(stats, group)) {
					out.add(new FormSelectNode(race, group, form, true));
				}
			}
		}
		return finish(stats, "stackforms", out);
	}

	private static List<RadialNode> forms(StatsData stats, String categoryKey, Predicate<String> typeFilter) {
		String race = stats.getCharacter().getRaceName();
		List<RadialNode> out = new ArrayList<>();
		Map<String, FormConfig> groups = ConfigManager.getAllFormsForRace(race);
		if (groups != null) {
			for (String group : groups.keySet()) {
				FormConfig config = ConfigManager.getFormGroup(race, group);
				if (config == null) continue;
				String type = config.getFormType() != null ? config.getFormType().toLowerCase(Locale.ROOT) : "";
				if (!typeFilter.test(type)) continue;
				for (String form : TransformationsHelper.getSelectableFormNames(stats, race, group)) {
					out.add(new FormSelectNode(race, group, form, false));
				}
			}
		}
		return finish(stats, categoryKey, out);
	}

	private static List<RadialNode> finish(StatsData stats, String categoryKey, List<RadialNode> out) {
		out.sort(Comparator.comparing(node -> node.label(stats).getString(), String.CASE_INSENSITIVE_ORDER));
		out = new ArrayList<>(RadialLayoutStore.applyOrder(categoryKey, out));
		return capWithMore(categoryKey, out);
	}

	private static List<RadialNode> capWithMore(String categoryKey, List<RadialNode> all) {
		if (all.size() <= MAX_SLOTS) return all;
		List<RadialNode> head = new ArrayList<>(all.subList(0, MAX_SLOTS - 1));
		head.add(new MoreNode(categoryKey, new ArrayList<>(all)));
		return head;
	}
}
