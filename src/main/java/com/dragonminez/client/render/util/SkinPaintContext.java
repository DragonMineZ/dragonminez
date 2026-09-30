package com.dragonminez.client.render.util;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class SkinPaintContext {
	public record Overlay(ResourceLocation texture, float[] rgb, float alpha) {}

	private static boolean active;
	private static boolean picking;
	private static ResourceLocation pickTexture;
	private static boolean showHair = true;
	private static boolean showOverlay = true;
	private static List<Overlay> overlays = List.of();
	private static Overlay highlight;

	private SkinPaintContext() {}

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

	public static List<Overlay> overlays() {
		return overlays;
	}

	public static Overlay highlight() {
		return highlight;
	}

	public static Scope begin(boolean pickPass, ResourceLocation pick, boolean hair, boolean overlay, List<Overlay> marks, Overlay hover) {
		return new Scope(pickPass, pick, hair, overlay, marks, hover);
	}

	public static final class Scope implements AutoCloseable {
		private final boolean previousActive;
		private final boolean previousPicking;
		private final ResourceLocation previousPick;
		private final boolean previousHair;
		private final boolean previousOverlay;
		private final List<Overlay> previousOverlays;
		private final Overlay previousHighlight;

		private Scope(boolean pickPass, ResourceLocation pick, boolean hair, boolean overlay, List<Overlay> marks, Overlay hover) {
			previousActive = active;
			previousPicking = picking;
			previousPick = pickTexture;
			previousHair = showHair;
			previousOverlay = showOverlay;
			previousOverlays = overlays;
			previousHighlight = highlight;
			active = true;
			picking = pickPass;
			pickTexture = pick;
			showHair = hair;
			showOverlay = overlay;
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
			overlays = previousOverlays;
			highlight = previousHighlight;
		}
	}
}
