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
		return forms(stats, "superforms", type -> type.contains("super") || type.contains("legendary") || type.contains("android"));
	}

	public static List<RadialNode> moreForms(StatsData stats) {
		return forms(stats, "moreforms", type -> !type.contains("super") && !type.contains("legendary") && !type.contains("android"));
	}

	public static List<RadialNode> stackForms(StatsData stats) {
		String race = stats.getCharacter().getRaceName();
		List<RadialNode> heads = new ArrayList<>();
		Map<String, FormConfig> groups = ConfigManager.getAllStackForms();
		if (groups != null) {
			for (String group : groups.keySet()) {
				if (!ConfigManager.getSkillsConfig().isSkillAllowedForRace(group, race)) continue;
				List<String> formNames = TransformationsHelper.getSelectableStackFormNames(stats, group);
				RadialNode head = buildGroupHead(stats, race, group, formNames, "stackforms", true);
				if (head != null) heads.add(head);
			}
		}
		return finish(stats, "stackforms", heads);
	}

	private static List<RadialNode> forms(StatsData stats, String categoryKey, Predicate<String> typeFilter) {
		String race = stats.getCharacter().getRaceName();
		List<RadialNode> heads = new ArrayList<>();
		Map<String, FormConfig> groups = ConfigManager.getAllFormsForRace(race);
		if (groups != null) {
			for (String group : groups.keySet()) {
				FormConfig config = ConfigManager.getFormGroup(race, group);
				if (config == null) continue;
				String type = config.getFormType() != null ? config.getFormType().toLowerCase(Locale.ROOT) : "";
				if (!typeFilter.test(type)) continue;
				if (!ConfigManager.getSkillsConfig().isSkillAllowedForRace(TransformationsHelper.getSkillNameForType(type), race)) continue;
				List<String> formNames = TransformationsHelper.getSelectableFormNames(stats, race, group);
				RadialNode head = buildGroupHead(stats, race, group, formNames, categoryKey, false);
				if (head != null) heads.add(head);
			}
		}
		return finish(stats, categoryKey, heads);
	}

	private static RadialNode buildGroupHead(StatsData stats, String race, String group, List<String> formNames, String parentCategoryKey, boolean stack) {
		if (formNames == null || formNames.isEmpty()) return null;
		List<RadialNode> rest = new ArrayList<>();
		for (int i = 1; i < formNames.size(); i++) rest.add(new FormSelectNode(race, group, formNames.get(i), stack));
		// Forms keep their JSON (config) ordering by default; only user MORE reordering overrides it.
		rest = orderAndCap(parentCategoryKey + ":" + group, rest);
		return new FormGroupHeadNode(race, group, formNames.get(0), stack, rest);
	}

	private static List<RadialNode> finish(StatsData stats, String categoryKey, List<RadialNode> out) {
		// Groups are shown alphabetically by default; user MORE reordering overrides it.
		out.sort(Comparator.comparing(node -> node.label(stats).getString(), String.CASE_INSENSITIVE_ORDER));
		return orderAndCap(categoryKey, out);
	}

	private static List<RadialNode> orderAndCap(String categoryKey, List<RadialNode> out) {
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
