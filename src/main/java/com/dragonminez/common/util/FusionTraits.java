package com.dragonminez.common.util;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.character.Character;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class FusionTraits {
	public enum Category { TAIL, EARS, ANTENNA, HORNS, CREST, WINGS, OTHER }

	public enum Source { RACE_PARTS, SAIYAN_TAIL, FROST_DEMON, BIO_ANDROID }

	public record Part(Category category, Source source, String bone, boolean leader) {
		public boolean onBody() {
			return category == Category.TAIL || category == Category.WINGS;
		}

		private boolean modelIntegrated() {
			return source != Source.RACE_PARTS;
		}

		private boolean sameAs(Part other) {
			return source == other.source && bone.equals(other.bone);
		}
	}

	public record Plan(List<Part> borrowed, Set<String> hiddenBones) {
		public static final Plan EMPTY = new Plan(List.of(), Set.of());

		public boolean hides(String bone) {
			return hiddenBones.contains(bone);
		}

		public boolean replacesOwnTail() {
			return hiddenBones.contains("tail1");
		}
	}

	private static final String[] TAIL_BONES = {"tail1", "cola", "xenotail"};

	private FusionTraits() {}

	public static boolean hasSaiyanTail(Character character) {
		if (character == null || !character.isHasSaiyanTail()) return false;
		String race = character.getRaceName().toLowerCase(Locale.ROOT);
		if (race.equals("saiyan")) return true;
		RaceCharacterConfig config = ConfigManager.getRaceCharacter(race);
		return config != null && Boolean.TRUE.equals(config.getHasSaiyanTail());
	}

	public static String modelFamily(String key) {
		if (key == null || key.isEmpty()) return "human";
		String k = key.toLowerCase(Locale.ROOT);
		if (k.startsWith("oozaru")) return "oozaru";
		if (k.startsWith("namekian")) return "namekian";
		if (k.startsWith("frostdemon")) return "frostdemon";
		if (k.startsWith("bioandroid")) return "bioandroid";
		if (k.startsWith("majin") || k.startsWith("janemba")) return "majin";
		if (k.startsWith("human") || k.startsWith("saiyan") || k.contains("ssj4d") || k.contains("ssj4gt")
				|| k.startsWith("buffed") || k.equals("4arms")) return "human";
		return "custom";
	}

	public static String raceFamily(String race) {
		String normalized = race == null ? "" : race.toLowerCase(Locale.ROOT);
		RaceCharacterConfig config = ConfigManager.getRaceCharacter(normalized);
		String customModel = config != null && config.getCustomModel() != null ? config.getCustomModel() : "";
		return modelFamily(customModel.isEmpty() ? normalized : customModel);
	}

	public static List<Part> partsOf(String race, String headBone, boolean saiyanTail, boolean leader) {
		List<Part> parts = new ArrayList<>();
		if (headBone != null) {
			for (String bone : headBone.split("\\+")) {
				if (bone.isEmpty() || bone.equals("hair")) continue;
				parts.add(new Part(categoryOf(bone), Source.RACE_PARTS, bone, leader));
			}
		}
		switch (race == null ? "" : race.toLowerCase(Locale.ROOT)) {
			case "namekian" -> parts.add(new Part(Category.ANTENNA, Source.RACE_PARTS, "antennas1", leader));
			case "majin" -> parts.add(new Part(Category.EARS, Source.RACE_PARTS, "ears3", leader));
			case "frostdemon" -> {
				parts.add(new Part(Category.EARS, Source.FROST_DEMON, "orejas", leader));
				parts.add(new Part(Category.TAIL, Source.FROST_DEMON, "tail1", leader));
			}
			case "bioandroid" -> {
				parts.add(new Part(Category.EARS, Source.BIO_ANDROID, "orejas", leader));
				parts.add(new Part(Category.CREST, Source.BIO_ANDROID, "cabeza2", leader));
				parts.add(new Part(Category.WINGS, Source.BIO_ANDROID, "alas", leader));
				parts.add(new Part(Category.TAIL, Source.BIO_ANDROID, "tail1", leader));
			}
			default -> {}
		}
		if (saiyanTail) {
			parts.removeIf(part -> part.category() == Category.TAIL);
			parts.add(new Part(Category.TAIL, Source.SAIYAN_TAIL, "tail1", leader));
		}
		return parts;
	}

	public static Plan plan(List<Part> leader, List<Part> partner, boolean partnerModel, boolean partnerHeadBones) {
		List<Part> chosen = choose(leader, partner);
		List<Part> modelParts = new ArrayList<>();
		for (Part part : partnerModel ? partner : leader) {
			if (part.modelIntegrated()) modelParts.add(part);
		}

		List<Part> borrowed = new ArrayList<>();
		for (Part part : chosen) {
			if (part.leader() && part.source() == Source.RACE_PARTS) continue;
			if (!part.leader() && part.source() == Source.RACE_PARTS && partnerModel && !partnerHeadBones) continue;
			if (contains(modelParts, part)) continue;
			borrowed.add(part);
		}

		Set<String> hidden = new HashSet<>();
		for (Part part : modelParts) {
			if (contains(chosen, part)) continue;
			if (part.category() == Category.TAIL) hidden.addAll(List.of(TAIL_BONES));
			else hidden.add(part.bone());
		}
		for (Part part : borrowed) {
			if (part.category() == Category.TAIL) hidden.addAll(List.of(TAIL_BONES));
		}
		return borrowed.isEmpty() && hidden.isEmpty() ? Plan.EMPTY : new Plan(List.copyOf(borrowed), Set.copyOf(hidden));
	}

	private static List<Part> choose(List<Part> leader, List<Part> partner) {
		EnumSet<Category> taken = EnumSet.noneOf(Category.class);
		Set<String> ownBones = new HashSet<>();
		Part leaderTail = null;
		for (Part part : leader) {
			taken.add(part.category());
			ownBones.add(part.bone());
			if (part.category() == Category.TAIL) leaderTail = part;
		}

		List<Part> chosen = new ArrayList<>(leader);
		for (Part part : partner) {
			if (part.category() == Category.TAIL) {
				boolean saiyanOverride = part.source() == Source.SAIYAN_TAIL && (leaderTail == null || leaderTail.source() != Source.SAIYAN_TAIL);
				if (saiyanOverride) {
					if (leaderTail != null) chosen.remove(leaderTail);
					chosen.add(part);
				} else if (leaderTail == null) {
					chosen.add(part);
				}
				continue;
			}
			boolean free = part.category() == Category.OTHER ? !ownBones.contains(part.bone()) : !taken.contains(part.category());
			if (free) chosen.add(part);
		}
		return chosen;
	}

	private static boolean contains(List<Part> parts, Part part) {
		for (Part candidate : parts) {
			if (candidate.sameAs(part) && candidate.leader() == part.leader()) return true;
		}
		return false;
	}

	private static Category categoryOf(String bone) {
		String name = bone.toLowerCase(Locale.ROOT);
		if (name.startsWith("ears")) return Category.EARS;
		if (name.startsWith("horns") || name.startsWith("cuernos")) return Category.HORNS;
		if (name.startsWith("majin") || name.startsWith("antennas")) return Category.ANTENNA;
		return Category.OTHER;
	}
}
