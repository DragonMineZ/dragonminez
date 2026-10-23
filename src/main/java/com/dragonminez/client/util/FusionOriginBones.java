package com.dragonminez.client.util;

import com.dragonminez.common.stats.FusedData;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.util.FusionTraits;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class FusionOriginBones {
	private static final Set<String> SKELETON = Set.of("root", "waist", "head", "body", "right_arm", "left_arm", "right_leg", "left_leg", "boobas");
	private static final Set<String> HUMANOID_FAMILIES = Set.of("human", "namekian", "oozaru");

	private static List<GeoBone> concealed = List.of();

	private FusionOriginBones() {}

	public static boolean applies(FusedData fused, Character character) {
		if (fused == null || !fused.usesPartnerModel(character)) return false;
		return !HUMANOID_FAMILIES.contains(FusionTraits.modelFamily(character.getRenderLogicKey()));
	}

	public static void reveal(BakedGeoModel model) {
		concealed = List.of();
		for (GeoBone bone : model.topLevelBones()) reveal(bone);
	}

	private static void reveal(GeoBone bone) {
		if (!isSkeleton(bone.getName())) {
			if (bone.isHidden()) bone.setHidden(false);
			return;
		}
		for (GeoBone child : bone.getChildBones()) reveal(child);
	}

	public static void conceal(BakedGeoModel model, FusedData fused, Character character) {
		if (!applies(fused, character)) return;
		List<GeoBone> bones = new ArrayList<>();
		for (GeoBone bone : model.topLevelBones()) collect(bone, bones);
		for (GeoBone bone : bones) bone.setHidden(true);
		concealed = bones;
	}

	private static void collect(GeoBone bone, List<GeoBone> out) {
		if (!isSkeleton(bone.getName())) {
			if (!bone.isHidden()) out.add(bone);
			return;
		}
		if (bone.isHidingChildren()) return;
		for (GeoBone child : bone.getChildBones()) collect(child, out);
	}

	public static List<GeoBone> childrenOf(GeoBone parent) {
		List<GeoBone> out = null;
		for (GeoBone bone : concealed) {
			if (bone.getParent() != parent) continue;
			if (out == null) out = new ArrayList<>(2);
			out.add(bone);
		}
		return out != null ? out : List.of();
	}

	private static boolean isSkeleton(String name) {
		return SKELETON.contains(name) || name.startsWith("armor") || name.endsWith("_layer") || name.endsWith("_hand_item");
	}
}
