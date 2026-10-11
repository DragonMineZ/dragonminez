package com.dragonminez.client.render.util;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class SkinPaintContext {
	public record Overlay(ResourceLocation texture, float[] rgb, float alpha) {}

	private static boolean active;
	private static boolean picking;
	private static ResourceLocation pickTexture;
	private static boolean showHair = true;
	private static boolean showOverlay = true;
	private static List<Overlay> overlays = List.of();
	private static Overlay highlight;
	private static Set<String> hiddenBones = Set.of();
	private static final List<GeoBone> pickedBones = new ArrayList<>();
	private static final Set<GeoBone> pickedBoneSet = Collections.newSetFromMap(new IdentityHashMap<>());

	private SkinPaintContext() {}

	public static void recordPickedBone(GeoBone bone) {
		if (!picking() || bone.isHidden() || bone.getCubes().isEmpty()) return;
		if (pickedBoneSet.add(bone)) pickedBones.add(bone);
	}

	public static List<GeoBone> pickedBones() {
		return Collections.unmodifiableList(pickedBones);
	}

	public static void clearPickedBones() {
		pickedBones.clear();
		pickedBoneSet.clear();
	}

	public static boolean active() {
		return active;
	}

	public static boolean picking() {
		return active && picking;
	}

	public static ResourceLocation pickTexture() {
		return pickTexture;
	}

	public static boolean showHair() {
		return !active || showHair;
	}

	public static boolean showOverlay() {
		return !active || showOverlay;
	}

	public static boolean hidesBone(String name) {
		return active && name != null && hiddenBones.contains(name);
	}

	public static List<Overlay> overlays() {
		return overlays;
	}

	public static Overlay highlight() {
		return highlight;
	}

	public static Scope begin(boolean pickPass, ResourceLocation pick, boolean hair, boolean overlay, Set<String> hidden, List<Overlay> marks, Overlay hover) {
		return new Scope(pickPass, pick, hair, overlay, hidden, marks, hover);
	}

	public static final class Scope implements AutoCloseable {
		private final boolean previousActive;
		private final boolean previousPicking;
		private final ResourceLocation previousPick;
		private final boolean previousHair;
		private final boolean previousOverlay;
		private final Set<String> previousHidden;
		private final List<Overlay> previousOverlays;
		private final Overlay previousHighlight;

		private Scope(boolean pickPass, ResourceLocation pick, boolean hair, boolean overlay, Set<String> hidden, List<Overlay> marks, Overlay hover) {
			previousActive = active;
			previousPicking = picking;
			previousPick = pickTexture;
			previousHair = showHair;
			previousOverlay = showOverlay;
			previousHidden = hiddenBones;
			previousOverlays = overlays;
			previousHighlight = highlight;
			if (pickPass) clearPickedBones();
			active = true;
			picking = pickPass;
			pickTexture = pick;
			showHair = hair;
			showOverlay = overlay;
			hiddenBones = hidden != null ? hidden : Set.of();
			overlays = marks != null ? marks : List.of();
			highlight = hover;
		}

		@Override
		public void close() {
			active = previousActive;
			picking = previousPicking;
			pickTexture = previousPick;
			showHair = previousHair;
			showOverlay = previousOverlay;
			hiddenBones = previousHidden;
			overlays = previousOverlays;
			highlight = previousHighlight;
		}
	}
}
