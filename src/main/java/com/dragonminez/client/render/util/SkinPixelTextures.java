package com.dragonminez.client.render.util;

import com.dragonminez.Reference;
import com.dragonminez.common.stats.character.SkinPixels;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class SkinPixelTextures {
	private static final int LAYER_BROW = 0;
	private static final int LAYER_SCLERA = 1;
	private static final int LAYER_IRIS = 2;
	private static final int LAYER_HAIR = 3;
	private static final int LAYER_TATTOO = 4;
	private static final String[] LAYER_NAMES = {"brow", "sclera", "iris", "hair", "tattoo"};
	private static final float MAX_SHADE_DEPTH = 0.55f;
	private static final long EVICT_AFTER_TICKS = 1200L;
	private static final long SWEEP_INTERVAL_TICKS = 200L;

	private static final Map<UUID, Entry> ENTRIES = new HashMap<>();
	private static long lastSweep;

	private static final class Entry {
		private final UUID id;
		private SkinPixels source;
		private int revision = Integer.MIN_VALUE;
		private ResourceLocation skin;
		private long lastUsed;
		private final DynamicTexture[] textures = new DynamicTexture[LAYER_NAMES.length];
		private final ResourceLocation[] locations = new ResourceLocation[LAYER_NAMES.length];

		private Entry(UUID id) {
			this.id = id;
		}
	}

	private SkinPixelTextures() {}

	public static ResourceLocation eyeMask(AbstractClientPlayer player, SkinPixels pixels, byte marker) {
		int layer = switch (marker) {
			case SkinPixels.MARK_BROW -> LAYER_BROW;
			case SkinPixels.MARK_SCLERA -> LAYER_SCLERA;
			case SkinPixels.MARK_IRIS -> LAYER_IRIS;
			default -> -1;
		};
		return layer < 0 ? null : layerLocation(player, pixels, layer);
	}

	public static ResourceLocation hairMask(AbstractClientPlayer player, SkinPixels pixels) {
		return layerLocation(player, pixels, LAYER_HAIR);
	}

	public static ResourceLocation tattoo(AbstractClientPlayer player, SkinPixels pixels) {
		return layerLocation(player, pixels, LAYER_TATTOO);
	}

	public static void clear() {
		TextureManager manager = Minecraft.getInstance().getTextureManager();
		for (Entry entry : ENTRIES.values()) releaseAll(entry, manager);
		ENTRIES.clear();
		lastSweep = 0L;
	}

	private static ResourceLocation layerLocation(AbstractClientPlayer player, SkinPixels pixels, int layer) {
		if (!RenderSystem.isOnRenderThread()) return null;
		long now = player.level().getGameTime();
		sweep(now);
		Entry entry = ENTRIES.computeIfAbsent(player.getUUID(), Entry::new);
		entry.lastUsed = now;
		ResourceLocation skin = player.getSkinTextureLocation();
		if (entry.source != pixels || entry.revision != pixels.getRevision() || !Objects.equals(entry.skin, skin)) {
			rebuild(entry, pixels, skin);
		}
		return entry.locations[layer];
	}

	private static void rebuild(Entry entry, SkinPixels pixels, ResourceLocation skin) {
		entry.source = pixels;
		entry.revision = pixels.getRevision();
		entry.skin = skin;
		TextureManager manager = Minecraft.getInstance().getTextureManager();
		NativeImage skinImage = pixels.hasEyeMarks() || pixels.hasHairMarks() ? download(skin, SkinPixels.SKIN_SIZE) : null;
		try {
			byte[] eyeMarks = pixels.getEyeMarks();
			updateMask(entry, manager, LAYER_BROW, eyeMarks, SkinPixels.MARK_BROW, skinImage);
			updateMask(entry, manager, LAYER_SCLERA, eyeMarks, SkinPixels.MARK_SCLERA, skinImage);
			updateMask(entry, manager, LAYER_IRIS, eyeMarks, SkinPixels.MARK_IRIS, skinImage);
			updateMask(entry, manager, LAYER_HAIR, pixels.getHairMarks(), SkinPixels.MARK_HAIR, skinImage);
			updateTattoo(entry, manager, pixels);
		} finally {
			if (skinImage != null) skinImage.close();
		}
	}

	private static void updateMask(Entry entry, TextureManager manager, int layer, byte[] marks, byte marker, NativeImage skinImage) {
		if (marks == null || !contains(marks, marker)) {
			release(entry, manager, layer);
			return;
		}
		int size = SkinPixels.SKIN_SIZE;
		DynamicTexture texture = entry.textures[layer];
		NativeImage image = reusable(texture, size) ? texture.getPixels() : new NativeImage(NativeImage.Format.RGBA, size, size, false);

		float minLight = Float.MAX_VALUE;
		float maxLight = 0.0f;
		if (skinImage != null) {
			for (int i = 0; i < marks.length; i++) {
				if (marks[i] != marker) continue;
				int color = skinImage.getPixelRGBA(i % size, i / size);
				if (alpha(color) == 0) continue;
				float light = lightness(color);
				minLight = Math.min(minLight, light);
				maxLight = Math.max(maxLight, light);
			}
		}
		float span = maxLight - minLight;
		float depth = Math.min(span, MAX_SHADE_DEPTH);

		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int value = 0;
				if (marks[y * size + x] == marker) {
					if (skinImage == null) {
						value = 255;
					} else {
						int color = skinImage.getPixelRGBA(x, y);
						if (alpha(color) > 0) value = shade(lightness(color), maxLight, span, depth);
					}
				}
				image.setPixelRGBA(x, y, value == 0 ? 0 : 0xFF000000 | (value << 16) | (value << 8) | value);
			}
		}
		commit(entry, manager, layer, texture, image);
	}

	private static void updateTattoo(Entry entry, TextureManager manager, SkinPixels pixels) {
		if (!pixels.hasTattoo()) {
			release(entry, manager, LAYER_TATTOO);
			return;
		}
		int size = pixels.getTattooSize();
		byte[] rgba = pixels.getTattoo();
		DynamicTexture texture = entry.textures[LAYER_TATTOO];
		NativeImage image = reusable(texture, size) ? texture.getPixels() : new NativeImage(NativeImage.Format.RGBA, size, size, false);
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int i = (y * size + x) * 4;
				int a = rgba[i + 3] & 0xFF;
				image.setPixelRGBA(x, y, a == 0 ? 0 : (a << 24) | ((rgba[i + 2] & 0xFF) << 16) | ((rgba[i + 1] & 0xFF) << 8) | (rgba[i] & 0xFF));
			}
		}
		commit(entry, manager, LAYER_TATTOO, texture, image);
	}

	private static boolean reusable(DynamicTexture texture, int size) {
		return texture != null && texture.getPixels() != null && texture.getPixels().getWidth() == size && texture.getPixels().getHeight() == size;
	}

	private static void commit(Entry entry, TextureManager manager, int layer, DynamicTexture texture, NativeImage image) {
		if (texture != null && texture.getPixels() == image) {
			texture.upload();
			return;
		}
		if (texture != null) release(entry, manager, layer);
		DynamicTexture created = new DynamicTexture(image);
		ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "pixels/" + entry.id + "/" + LAYER_NAMES[layer]);
		manager.register(location, created);
		entry.textures[layer] = created;
		entry.locations[layer] = location;
	}

	private static void release(Entry entry, TextureManager manager, int layer) {
		if (entry.locations[layer] != null) manager.release(entry.locations[layer]);
		entry.locations[layer] = null;
		entry.textures[layer] = null;
	}

	private static void releaseAll(Entry entry, TextureManager manager) {
		for (int layer = 0; layer < LAYER_NAMES.length; layer++) release(entry, manager, layer);
	}

	private static void sweep(long now) {
		if (now - lastSweep < SWEEP_INTERVAL_TICKS && now >= lastSweep) return;
		lastSweep = now;
		TextureManager manager = Minecraft.getInstance().getTextureManager();
		Iterator<Entry> iterator = ENTRIES.values().iterator();
		while (iterator.hasNext()) {
			Entry entry = iterator.next();
			if (now - entry.lastUsed > EVICT_AFTER_TICKS) {
				releaseAll(entry, manager);
				iterator.remove();
			}
		}
	}

	public static NativeImage download(ResourceLocation location, int expectedSize) {
		if (location == null || !RenderSystem.isOnRenderThread()) return null;
		try {
			AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(location);
			RenderSystem.bindTexture(texture.getId());
			int width = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
			int height = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
			if (width != expectedSize || height != expectedSize) return null;
			NativeImage image = new NativeImage(NativeImage.Format.RGBA, width, height, false);
			image.downloadTexture(0, false);
			return image;
		} catch (Exception e) {
			return null;
		}
	}

	private static boolean contains(byte[] marks, byte marker) {
		for (byte mark : marks) if (mark == marker) return true;
		return false;
	}

	private static int alpha(int abgr) {
		return abgr >>> 24;
	}

	private static int shade(float light, float maxLight, float span, float depth) {
		if (span <= 1.0e-4f) return 255;
		float relative = 1.0f - depth * (maxLight - light) / span;
		return Mth.clamp(Math.round(255.0f * encode(relative * relative * relative)), 1, 255);
	}

	private static float lightness(int abgr) {
		float y = 0.2126f * linear(abgr & 0xFF) + 0.7152f * linear((abgr >> 8) & 0xFF) + 0.0722f * linear((abgr >> 16) & 0xFF);
		return (float) Math.cbrt(y);
	}

	private static float linear(int channel) {
		float c = channel / 255.0f;
		return c <= 0.04045f ? c / 12.92f : (float) Math.pow((c + 0.055f) / 1.055f, 2.4f);
	}

	private static float encode(float linear) {
		return linear <= 0.0031308f ? linear * 12.92f : 1.055f * (float) Math.pow(linear, 1.0f / 2.4f) - 0.055f;
	}
}
