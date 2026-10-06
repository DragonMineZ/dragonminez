package com.dragonminez.client.util;

import com.dragonminez.Reference;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class PanelSkin {
	private static final ResourceLocation MENU_BIG = texture("textures/gui/menu/menubig.png");
	private static final ResourceLocation MENU_SMALL = texture("textures/gui/menu/menusmall.png");
	private static final ResourceLocation MENU_NPC = texture("textures/gui/menu/menunpc.png");
	private static final ResourceLocation QUEST_MENU = texture("textures/gui/menu/questmenu.png");
	private static final ResourceLocation BUTTONS = texture("textures/gui/buttons/characterbuttons.png");
	private static final int GRID = 7;
	private static final int CACHE_LIMIT = 64;
	private static final int[] NO_LAYOUT = new int[0];

	public static final PanelSkin SMALL_PANEL = new PanelSkin(MENU_SMALL, 256, 256, 0, 0, null,
			new Segment[]{fixed(14), tile(63), decor(28, 0), tile(21), fixed(15)},
			new Segment[]{fixed(14), band(21), fixed(7), decor(28, 0), tile(7), fixed(17)});

	public static final PanelSkin PANEL = new PanelSkin(MENU_BIG, 256, 256, 0, 0, null,
			new Segment[]{fixed(14), tile(63), decor(28, 0), tile(21), fixed(15)},
			new Segment[]{fixed(14), band(21), fixed(7), decor(28, 2), tile(28), decor(21, 1), tile(77), fixed(17)});

	public static final PanelSkin NPC_PANEL = new PanelSkin(MENU_NPC, 512, 512, 0, 0, SMALL_PANEL,
			new Segment[]{fixed(14), tile(63), decor(28, 1), tile(175), decor(28, 2), tile(22), fixed(15)},
			new Segment[]{fixed(14), band(21), fixed(7), decor(35, 0), fixed(17)});

	public static final PanelSkin QUEST_PANEL = new PanelSkin(QUEST_MENU, 512, 512, 1, 0, null,
			new Segment[]{fixed(14), tile(63), decor(49, 1), tile(91), decor(28, 2), tile(21), fixed(15)},
			new Segment[]{fixed(14), band(21), fixed(7), tile(7), decor(21, 5), tile(35), decor(21, 4), tile(35), decor(21, 3),
					tile(35), decor(21, 2), tile(35), decor(21, 1), tile(112), fixed(17)});

	public static final PanelSkin BUTTON = button(28);
	public static final PanelSkin BUTTON_HOVER = button(48);
	public static final PanelSkin BUTTON_ACTIVE = wideButton(68);
	public static final PanelSkin BUTTON_ACCENT = wideButton(88);
	public static final PanelSkin FIELD = field(108);
	public static final PanelSkin FIELD_EDITING = field(126);

	private final ResourceLocation texture;
	private final int atlasWidth;
	private final int atlasHeight;
	private final int u;
	private final int v;
	private final PanelSkin fallback;
	private final Segment[] columns;
	private final Segment[] rows;
	private final int width;
	private final int height;
	private final Int2ObjectOpenHashMap<int[]> columnCache = new Int2ObjectOpenHashMap<>();
	private final Int2ObjectOpenHashMap<int[]> rowCache = new Int2ObjectOpenHashMap<>();

	private enum Kind {FIXED, TILE, DECOR, BAND}

	private record Segment(Kind kind, int length, int period, int priority) {}

	private PanelSkin(ResourceLocation texture, int atlasWidth, int atlasHeight, int u, int v, PanelSkin fallback, Segment[] columns, Segment[] rows) {
		this.texture = texture;
		this.atlasWidth = atlasWidth;
		this.atlasHeight = atlasHeight;
		this.u = u;
		this.v = v;
		this.fallback = fallback;
		this.columns = columns;
		this.rows = rows;
		this.width = total(columns);
		this.height = total(rows);
	}

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}

	public void draw(GuiGraphics graphics, float x, float y, float width, float height) {
		draw(graphics, x, y, width, height, 1.0f, 1.0f, 1.0f, 1.0f);
	}

	public void draw(GuiGraphics graphics, float x, float y, float width, float height, float red, float green, float blue, float alpha) {
		int targetWidth = Math.round(width);
		int targetHeight = Math.round(height);
		if (targetWidth <= 0 || targetHeight <= 0) return;
		PanelSkin skin = this;
		int[] xs = skin.columnLayout(targetWidth);
		int[] ys = skin.rowLayout(targetHeight);
		while ((xs == null || ys == null) && skin.fallback != null) {
			skin = skin.fallback;
			xs = skin.columnLayout(targetWidth);
			ys = skin.rowLayout(targetHeight);
		}
		if (xs == null || ys == null) return;
		skin.emit(graphics, x, y, xs, ys, red, green, blue, alpha);
	}

	private void emit(GuiGraphics graphics, float x, float y, int[] xs, int[] ys, float red, float green, float blue, float alpha) {
		graphics.flush();
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		Matrix4f matrix = graphics.pose().last().pose();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		for (int column = 0; column < xs.length; column += 3) {
			float x0 = x + xs[column];
			float x1 = x0 + xs[column + 1];
			float u0 = (u + xs[column + 2]) / (float) atlasWidth;
			float u1 = (u + xs[column + 2] + xs[column + 1]) / (float) atlasWidth;
			for (int row = 0; row < ys.length; row += 3) {
				float y0 = y + ys[row];
				float y1 = y0 + ys[row + 1];
				float v0 = (v + ys[row + 2]) / (float) atlasHeight;
				float v1 = (v + ys[row + 2] + ys[row + 1]) / (float) atlasHeight;
				buffer.vertex(matrix, x0, y0, 0.0f).uv(u0, v0).color(red, green, blue, alpha).endVertex();
				buffer.vertex(matrix, x0, y1, 0.0f).uv(u0, v1).color(red, green, blue, alpha).endVertex();
				buffer.vertex(matrix, x1, y1, 0.0f).uv(u1, v1).color(red, green, blue, alpha).endVertex();
				buffer.vertex(matrix, x1, y0, 0.0f).uv(u1, v0).color(red, green, blue, alpha).endVertex();
			}
		}
		BufferUploader.drawWithShader(buffer.end());
	}

	private int[] columnLayout(int target) {
		return cached(columnCache, columns, target);
	}

	private int[] rowLayout(int target) {
		return cached(rowCache, rows, target);
	}

	private int[] cached(Int2ObjectOpenHashMap<int[]> cache, Segment[] segments, int target) {
		int[] pieces = cache.get(target);
		if (pieces == null) {
			if (cache.size() >= CACHE_LIMIT) cache.clear();
			pieces = layout(segments, target, fallback != null);
			cache.put(target, pieces != null ? pieces : NO_LAYOUT);
		}
		return pieces == NO_LAYOUT ? null : pieces;
	}

	private static int[] layout(Segment[] segments, int target, boolean strict) {
		int fixed = 0;
		int band = 0;
		int tile = 0;
		List<Integer> decors = new ArrayList<>();
		for (int i = 0; i < segments.length; i++) {
			switch (segments[i].kind()) {
				case FIXED -> fixed += segments[i].length();
				case BAND -> band += segments[i].length();
				case TILE -> tile += segments[i].length();
				case DECOR -> decors.add(i);
			}
		}
		decors.sort((a, b) -> Integer.compare(segments[b].priority(), segments[a].priority()));

		int flexNative = total(segments) - fixed - band;
		int flexTarget = target - fixed - band;
		int keep = flexTarget >= flexNative || flexNative <= 0 ? decors.size()
				: Math.max(0, Math.min(decors.size(), Math.round(decors.size() * flexTarget / (float) flexNative)));
		int decorLength = 0;
		for (int i = 0; i < keep; i++) decorLength += segments[decors.get(i)].length();
		while (keep > 0 && decorLength > Math.max(flexTarget, 0)) {
			keep--;
			decorLength -= segments[decors.get(keep)].length();
		}

		int available = flexTarget - decorLength;
		int bandLength = band;
		if (available > 0 && tile == 0) {
			if (strict) return null;
			bandLength += available;
			available = 0;
		}
		int deficit = 0;
		if (available < 0) {
			bandLength = band + available;
			available = 0;
			if (bandLength < 0) {
				deficit = -bandLength;
				bandLength = 0;
			}
		}

		int[] lengths = new int[segments.length];
		for (int i = 0; i < segments.length; i++) {
			if (segments[i].kind() == Kind.FIXED) lengths[i] = segments[i].length();
		}
		for (int i = 0; i < keep; i++) lengths[decors.get(i)] = segments[decors.get(i)].length();
		distribute(segments, Kind.TILE, available, lengths);
		distribute(segments, Kind.BAND, bandLength, lengths);

		int firstFixed = -1;
		int lastFixed = -1;
		for (int i = 0; i < segments.length; i++) {
			if (segments[i].kind() != Kind.FIXED) continue;
			if (firstFixed < 0) firstFixed = i;
			lastFixed = i;
		}
		int cropFirst = (deficit + 1) / 2;
		int cropLast = deficit - cropFirst;

		IntArrayList pieces = new IntArrayList();
		int position = 0;
		int start = 0;
		for (int i = 0; i < segments.length; i++) {
			Segment segment = segments[i];
			int length = lengths[i];
			int source = start;
			if (i == firstFixed && cropFirst > 0) length = Math.max(0, length - cropFirst);
			if (i == lastFixed && cropLast > 0) {
				source += Math.min(cropLast, segment.length());
				length = Math.max(0, length - cropLast);
			}
			if (length > 0) {
				if (segment.kind() == Kind.TILE || segment.kind() == Kind.BAND) {
					int chunk = Math.max(segment.period(), segment.length() / segment.period() * segment.period());
					for (int offset = 0; offset < length; offset += chunk) {
						pieces.add(position + offset);
						pieces.add(Math.min(chunk, length - offset));
						pieces.add(source);
					}
				} else {
					pieces.add(position);
					pieces.add(length);
					pieces.add(source);
				}
			}
			position += length;
			start += segment.length();
		}
		return pieces.toIntArray();
	}

	private static void distribute(Segment[] segments, Kind kind, int amount, int[] lengths) {
		int nativeTotal = 0;
		int last = -1;
		for (int i = 0; i < segments.length; i++) {
			if (segments[i].kind() != kind) continue;
			nativeTotal += segments[i].length();
			last = i;
		}
		if (last < 0) return;
		if (amount == nativeTotal) {
			for (int i = 0; i < segments.length; i++) {
				if (segments[i].kind() == kind) lengths[i] = segments[i].length();
			}
			return;
		}
		double[] remainders = new double[segments.length];
		int used = 0;
		for (int i = 0; i < segments.length; i++) {
			if (segments[i].kind() != kind) continue;
			double ideal = (double) amount * segments[i].length() / nativeTotal / segments[i].period();
			int units = (int) ideal;
			lengths[i] = units * segments[i].period();
			remainders[i] = ideal - units;
			used += lengths[i];
		}
		boolean[] bumped = new boolean[segments.length];
		while (true) {
			int best = -1;
			for (int i = 0; i < segments.length; i++) {
				if (segments[i].kind() != kind || bumped[i] || used + segments[i].period() > amount) continue;
				if (best < 0 || remainders[i] > remainders[best]) best = i;
			}
			if (best < 0 || remainders[best] <= 0.0) break;
			bumped[best] = true;
			lengths[best] += segments[best].period();
			used += segments[best].period();
		}
		lengths[last] += amount - used;
	}

	private static int total(Segment[] segments) {
		int sum = 0;
		for (Segment segment : segments) sum += segment.length();
		return sum;
	}

	private static Segment fixed(int length) {
		return new Segment(Kind.FIXED, length, 1, 0);
	}

	private static Segment tile(int length) {
		return new Segment(Kind.TILE, length, GRID, 0);
	}

	private static Segment stretch(int length) {
		return new Segment(Kind.TILE, length, 1, 0);
	}

	private static Segment decor(int length, int priority) {
		return new Segment(Kind.DECOR, length, 1, priority);
	}

	private static Segment band(int length) {
		return new Segment(Kind.BAND, length, GRID, 0);
	}

	private static Segment shrink(int length) {
		return new Segment(Kind.BAND, length, 1, 0);
	}

	private static PanelSkin button(int v) {
		return new PanelSkin(BUTTONS, 256, 256, 0, v, null,
				new Segment[]{fixed(4), shrink(9), stretch(49), shrink(9), fixed(3)},
				new Segment[]{fixed(4), stretch(10), shrink(2), fixed(4)});
	}

	private static PanelSkin wideButton(int v) {
		return new PanelSkin(BUTTONS, 256, 256, 0, v, null,
				new Segment[]{fixed(3), shrink(9), stretch(124), shrink(11), fixed(3)},
				new Segment[]{fixed(3), stretch(13), fixed(4)});
	}

	private static PanelSkin field(int v) {
		return new PanelSkin(BUTTONS, 256, 256, 0, v, null,
				new Segment[]{fixed(3), stretch(101), fixed(3)},
				new Segment[]{fixed(3), stretch(11), fixed(4)});
	}

	private static ResourceLocation texture(String path) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, path);
	}
}
