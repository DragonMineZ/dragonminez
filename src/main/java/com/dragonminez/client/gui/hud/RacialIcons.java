package com.dragonminez.client.gui.hud;

import com.dragonminez.Reference;
import com.dragonminez.client.render.layer.DMZSkinLayer;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RacialIcons {
	public static final int SIZE = 16;
	public static final int TEXTURE_WIDTH = 16;
	public static final int TEXTURE_HEIGHT = 32;
	public static final int FILL_V = 16;

	private static final String FOLDER = "textures/gui/hud/racial_icons/";
	private static final ResourceLocation CUSTOM = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, FOLDER + "custom.png");
	private static final Map<String, ResourceLocation> CANDIDATES = new ConcurrentHashMap<>();

	private RacialIcons() {}

	public static ResourceLocation forRace(String raceName) {
		String race = raceName == null ? "" : raceName.toLowerCase(Locale.ROOT);
		ResourceLocation candidate = CANDIDATES.computeIfAbsent(race, key -> {
			ResourceLocation location = ResourceLocation.tryBuild(Reference.MOD_ID, FOLDER + key + ".png");
			return location != null ? location : CUSTOM;
		});
		return DMZSkinLayer.getSafeTexture(candidate, CUSTOM);
	}
}
