package com.dragonminez.client.gui.hud;

import com.dragonminez.Reference;
import net.minecraft.resources.ResourceLocation;

public final class HudSprites {
	public static final float TEXELS_PER_PIXEL = 2.0f;

	private static final ResourceLocation DEFAULT_SHEET = sheet("hud_default");
	private static final ResourceLocation MINECRAFT_SHEET = sheet("hud_minecraft");
	private static final ResourceLocation PARTY_SHEET = sheet("hud_party");
	private static final ResourceLocation RESERVE_SHEET = sheet("hud_reserve");

	public static final BarSkin DEFAULT_HP = new BarSkin(def(0, 0, 256, 32), def(0, 33, 256, 32), def(0, 66, 250, 26), def(0, 93, 250, 26));
	public static final BarSkin DEFAULT_KI = new BarSkin(def(0, 120, 256, 26), null, def(0, 147, 250, 20), def(0, 168, 250, 20));
	public static final BarSkin DEFAULT_STAMINA = new BarSkin(def(0, 189, 256, 22), null, def(0, 212, 250, 16), def(0, 229, 250, 16));
	public static final BarSkin DEFAULT_RELEASE = new BarSkin(def(260, 150, 74, 12), null, def(260, 163, 68, 6), def(260, 170, 68, 6));
	public static final Sprite DEFAULT_PORTRAIT_BACK = def(260, 0, 74, 74);
	public static final Sprite DEFAULT_PORTRAIT_TINT = def(260, 75, 74, 74);

	public static final BarSkin MINECRAFT_HP = new BarSkin(mc(0, 0, 198, 23), mc(0, 24, 198, 23), mc(200, 0, 192, 17), mc(200, 18, 192, 17));
	public static final BarSkin MINECRAFT_STAMINA = new BarSkin(mc(0, 48, 198, 23), null, mc(200, 36, 192, 17), mc(200, 54, 192, 17));
	public static final BarSkin MINECRAFT_KI = new BarSkin(mc(0, 72, 198, 23), null, mc(200, 72, 192, 17), mc(200, 90, 192, 17));
	public static final BarSkin MINECRAFT_RELEASE = new BarSkin(mc(0, 96, 198, 23), null, mc(200, 108, 192, 17), mc(200, 126, 192, 17));
	public static final Sprite MINECRAFT_CAPSULE = mc(400, 0, 23, 23);
	public static final Sprite MINECRAFT_ICON_HEART = mc(400, 24, 17, 17);
	public static final Sprite MINECRAFT_ICON_STAMINA = mc(400, 42, 17, 17);
	public static final Sprite MINECRAFT_ICON_KI = mc(400, 60, 17, 17);
	public static final Sprite MINECRAFT_ICON_RELEASE = mc(400, 78, 17, 17);

	public static final BarSkin PARTY_HP = new BarSkin(party(0, 0, 146, 18), party(0, 19, 146, 18), party(0, 38, 140, 12), party(0, 51, 140, 12));
	public static final BarSkin PARTY_KI = new BarSkin(party(0, 64, 146, 15), null, party(0, 80, 140, 9), party(0, 90, 140, 9));
	public static final Sprite PARTY_HEAD_BACK = party(150, 0, 46, 46);
	public static final Sprite PARTY_HEAD_TINT = party(150, 47, 46, 46);
	public static final Sprite PARTY_MARKER = party(200, 0, 32, 32);

	public static final BarSkin RESERVE_BAR = new BarSkin(reserve(0, 0, 23, 150), null, reserve(25, 0, 17, 144), reserve(44, 0, 17, 144));
	public static final Sprite RESERVE_CAPSULE = reserve(63, 0, 23, 23);
	public static final Sprite RESERVE_ICON = reserve(88, 0, 17, 17);
	public static final Sprite RAGE_ICON = reserve(88, 18, 17, 17);

	private HudSprites() {}

	public record Sprite(ResourceLocation sheet, int sheetWidth, int sheetHeight, int u, int v, int width, int height) {
		public float guiWidth() {
			return width / TEXELS_PER_PIXEL;
		}

		public float guiHeight() {
			return height / TEXELS_PER_PIXEL;
		}
	}

	public record BarSkin(Sprite frame, Sprite alert, Sprite fill, Sprite shine) {
		public float width() {
			return fill.guiWidth();
		}

		public float height() {
			return fill.guiHeight();
		}

		public float border() {
			return (frame.guiHeight() - fill.guiHeight()) / 2.0f;
		}
	}

	private static ResourceLocation sheet(String name) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/" + name + ".png");
	}

	private static Sprite def(int u, int v, int width, int height) {
		return new Sprite(DEFAULT_SHEET, 512, 256, u, v, width, height);
	}

	private static Sprite mc(int u, int v, int width, int height) {
		return new Sprite(MINECRAFT_SHEET, 512, 256, u, v, width, height);
	}

	private static Sprite party(int u, int v, int width, int height) {
		return new Sprite(PARTY_SHEET, 256, 128, u, v, width, height);
	}

	private static Sprite reserve(int u, int v, int width, int height) {
		return new Sprite(RESERVE_SHEET, 128, 256, u, v, width, height);
	}
}
