package com.dragonminez.client.gui;

import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.client.gui.radial.FormPreview;
import com.dragonminez.client.gui.radial.IUtilityMenuSlotAdapter;
import com.dragonminez.client.gui.radial.ModelFormPreview;
import com.dragonminez.client.gui.radial.RadialLayoutStore;
import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.client.gui.radial.nodes.*;
import com.dragonminez.client.gui.utilitymenu.IUtilityMenuSlot;
import com.dragonminez.client.render.layer.DMZSkinLayer;
import com.dragonminez.client.render.shader.UtilityMenuBlur;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class UtilityMenuScreen extends ScaledScreen {
	private static final List<IUtilityMenuSlot> ADDON_SLOTS = new ArrayList<>();
	private static long utilityMenuReopenBlockedUntilMs = 0L;

	private static final long ANIMATION_DURATION = 140L;
	private static final int SLOTS = 8;

	private static final float R_INNER = 40f;
	private static final float R_OUTER = 86f;
	private static final float CHILD_BAND = 42f;
	private static final float SECTOR_GAP_DEG = 3f;

	private static final float ICON_BASE = 16f;
	private static final float ICON_CHILD = 14f;

	private static final int MODEL_SCALE = 32;
	private static final int MODEL_Y_OFFSET = 26;
	private static final float ANIM_SPEED = 14f;

	private static final float[] PANEL = {0.11f, 0.11f, 0.13f, 0.60f};
	private static final float[] PANEL_HOVER = {0.20f, 0.52f, 0.96f, 0.90f};
	private static final float[] PANEL_INACTIVE = {0.09f, 0.09f, 0.10f, 0.42f};
	private static final float[] CHILD_PANEL = {0.10f, 0.12f, 0.11f, 0.64f};
	private static final float[] CHILD_HOVER = {0.22f, 0.46f, 0.30f, 0.86f};

	private final long openTime;
	private boolean closing = false;
	private long closeStartTime = -1L;
	private long lastFrameNanos = 0L;
	private float frameDt = 0f;

	private StatsData statsData;
	private final List<RadialNode> baseNodes = new ArrayList<>();
	private final List<RadialNode> chain = new ArrayList<>();
	private FormPreview currentPreview = null;

	private static final int PANEL_WIDTH = 130;
	private static final int PANEL_ROW_H = 13;
	private static final int PANEL_TITLE_H = 15;
	private static final int PANEL_PAD = 5;
	private static final int PANEL_SCREEN_MARGIN = 4;
	private static final float PANEL_CLEAR_GAP = 12f;
	private static final int PANEL_MAX_ROWS = 8;

	private List<RadialNode> panelOptions = null;
	private Component panelTitle = null;
	private boolean panelScrollable = false;
	private int panelScroll = 0;
	private MoreNode openMore = null;
	private float panelAngleDeg = 0f;
	private int panelLevel = 0;
	private Hover frozenHover = new Hover();
	private int dragIndex = -1;
	private boolean dragging = false;
	private double dragStartY = 0;
	private double dragCurrentY = 0;

	public UtilityMenuScreen() {
		super(Component.literal("Menu").withStyle(Style.EMPTY.withFont(DMZ_FONT)));
		this.openTime = System.currentTimeMillis();
	}

	@Override
	protected void init() {
		super.init();
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> this.statsData = data);
		}
		buildBaseNodes();
		UtilityMenuBlur.start();
	}

	@Override
	public void removed() {
		super.removed();
		UtilityMenuBlur.stop();
	}

	private void buildBaseNodes() {
		baseNodes.clear();
		int[] addon = {0};
		baseNodes.add(new SuperFormNode());        // slot 0  top
		baseNodes.add(new MoreFormsNode());        // slot 1  upper-right
		baseNodes.add(nextAddonOrEmpty(addon));    // slot 2  right
		baseNodes.add(new MovementNode());         // slot 3  lower-right
		baseNodes.add(new DescendNode());          // slot 4  bottom
		baseNodes.add(new ActionsNode());          // slot 5  lower-left
		baseNodes.add(nextAddonOrEmpty(addon));    // slot 6  left
		baseNodes.add(new StackSkillNode());       // slot 7  upper-left
	}

	private RadialNode nextAddonOrEmpty(int[] addonIndex) {
		if (addonIndex[0] < ADDON_SLOTS.size()) return new IUtilityMenuSlotAdapter(ADDON_SLOTS.get(addonIndex[0]++));
		return new EmptyNode();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (statsData == null) return;

		updateUiScale();

		long nowNanos = System.nanoTime();
		frameDt = lastFrameNanos == 0L ? 0f : Math.min(0.1f, (nowNanos - lastFrameNanos) / 1.0e9f);
		lastFrameNanos = nowNanos;

		float openScale = computeOpenScale();

		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);

		beginUiScale(graphics);

		int w = getUiWidth();
		int h = getUiHeight();

		float cx = w / 2f;
		float cy = h / 2f;

		Hover hover = closing ? new Hover()
				: (panelOptions != null ? frozenHover : resolveHover(cx, cy, uiMouseX, uiMouseY, openScale));
		currentPreview = hover.deepest != null ? hover.deepest.preview(statsData) : null;

		drawBaseSectors(graphics, cx, cy, hover, openScale);
		for (RadialNode node : baseNodes) {
			if (isActiveSlot(node)) drawChildSectors(graphics, cx, cy, node, baseCenter(baseNodes.indexOf(node)), 1, hover, false, openScale);
		}

		int modelX = Math.round(cx);
		int modelY = Math.round(cy + MODEL_Y_OFFSET * openScale + 12 * openScale);
		ModelFormPreview.render(graphics, modelX, modelY,
				Math.max(1, Math.round(MODEL_SCALE * openScale)), (float) modelX, (float) modelY, currentPreview);

		drawBaseFaces(graphics, cx, cy, openScale);
		for (RadialNode node : baseNodes) {
			if (isActiveSlot(node)) drawChildSectors(graphics, cx, cy, node, baseCenter(baseNodes.indexOf(node)), 1, hover, true, openScale);
		}

		if (panelOptions != null) drawPanel(graphics, cx, cy, uiMouseX, uiMouseY);

		super.render(graphics, (int) Math.round(uiMouseX), (int) Math.round(uiMouseY), partialTick);
		endUiScale(graphics);
	}

	private static float radiusForLevel(int level) {
		if (level <= 0) return (R_INNER + R_OUTER) / 2f;
		return R_OUTER + (level - 0.5f) * CHILD_BAND;
	}

	private int visiblePanelRows() {
		int total = panelOptions.size();
		return panelScrollable ? Math.min(total, PANEL_MAX_ROWS) : total;
	}

	private int[] panelBounds(float cx, float cy) {
		int rows = visiblePanelRows();
		int height = PANEL_TITLE_H + rows * PANEL_ROW_H + PANEL_PAD;

		double rad = Math.toRadians(panelAngleDeg);
		float dirX = (float) Math.cos(rad);
		float dirY = (float) Math.sin(rad);

		float clearRadius = radiusForLevel(panelLevel) + CHILD_BAND * 0.6f + PANEL_CLEAR_GAP;

		float panelCx = cx + dirX * clearRadius;
		float panelCy = cy + dirY * clearRadius;

		int px = Math.round(panelCx - PANEL_WIDTH / 2f);
		int py = Math.round(panelCy - height / 2f);

		int uiW = getUiWidth();
		int uiH = getUiHeight();

		int maxPx = uiW - PANEL_WIDTH - PANEL_SCREEN_MARGIN;
		int maxPy = uiH - height - PANEL_SCREEN_MARGIN;
		px = Mth.clamp(px, PANEL_SCREEN_MARGIN, Math.max(PANEL_SCREEN_MARGIN, maxPx));
		py = Mth.clamp(py, PANEL_SCREEN_MARGIN, Math.max(PANEL_SCREEN_MARGIN, maxPy));

		return new int[]{px, py, PANEL_WIDTH, height};
	}

	private int rowIndexAt(int rowsTop, double uiY) {
		return (int) Math.floor((uiY - rowsTop) / PANEL_ROW_H);
	}

	private void drawPanel(GuiGraphics graphics, float cx, float cy, double mouseX, double mouseY) {
		List<RadialNode> opts = panelOptions;
		int visibleRows = visiblePanelRows();
		int maxScroll = Math.max(0, opts.size() - visibleRows);
		panelScroll = Mth.clamp(panelScroll, 0, maxScroll);
		int start = panelScrollable ? panelScroll : 0;

		int[] b = panelBounds(cx, cy);
		int px = b[0], py = b[1], pw = b[2], ph = b[3];

		graphics.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xFF000000);
		graphics.fill(px, py, px + pw, py + ph, 0xF0141420);
		graphics.fill(px, py, px + pw, py + PANEL_TITLE_H, 0x30FFFFFF);

		Style style = Style.EMPTY.withFont(DMZ_FONT);
		Component title = (panelTitle != null ? panelTitle : Component.translatable("gui.dragonminez.radial.options")).copy().withStyle(style);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, title, px + pw / 2, py + 4, 0xFFFFFF, 0x000000);

		int rowsTop = py + PANEL_TITLE_H;
		int textWidth = pw - 12 - (maxScroll > 0 ? 4 : 0);
		for (int r = 0; r < visibleRows; r++) {
			int i = start + r;
			RadialNode node = opts.get(i);
			int ry = rowsTop + r * PANEL_ROW_H;
			boolean isDragged = !panelScrollable && dragging && i == dragIndex;
			int drawY = isDragged ? (int) Math.round(dragCurrentY - PANEL_ROW_H / 2.0) : ry;

			boolean hovered = !dragging && mouseX >= px && mouseX <= px + pw && mouseY >= ry && mouseY < ry + PANEL_ROW_H;
			if (hovered) graphics.fill(px + 1, ry, px + pw - 1, ry + PANEL_ROW_H - 1, 0x55000000);
			if (isDragged) graphics.fill(px + 1, drawY, px + pw - 1, drawY + PANEL_ROW_H - 1, 0x33FFFFFF);
			if (node.active(statsData)) drawRowBorder(graphics, px + 1, drawY, px + pw - 1, drawY + PANEL_ROW_H - 1, 0xFF3BE05A);

			int color = node.labelColor(statsData);
			String text = this.font.plainSubstrByWidth(node.label(statsData).getString(), textWidth);
			TextUtil.drawStringWithBorder(graphics, this.font, Component.literal(text).withStyle(style), px + 6, drawY + 3, color, 0x000000);
		}

		if (panelScrollable && maxScroll > 0) {
			int trackX = px + pw - 3;
			int trackTop = rowsTop;
			int trackH = visibleRows * PANEL_ROW_H;
			graphics.fill(trackX, trackTop, trackX + 2, trackTop + trackH, 0x40FFFFFF);
			int thumbH = Math.max(6, trackH * visibleRows / opts.size());
			int thumbY = trackTop + (trackH - thumbH) * panelScroll / maxScroll;
			graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, 0xC0FFFFFF);
		}

		if (!panelScrollable && dragging && dragIndex >= 0) {
			int target = Mth.clamp(rowIndexAt(rowsTop, mouseY), 0, opts.size());
			int lineY = rowsTop + target * PANEL_ROW_H;
			graphics.fill(px + 2, lineY - 1, px + pw - 2, lineY, 0xFFFFFFFF);
		}
	}

	private void drawRowBorder(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
		graphics.fill(x0, y0, x1, y0 + 1, color);
		graphics.fill(x0, y1 - 1, x1, y1, color);
		graphics.fill(x0, y0, x0 + 1, y1, color);
		graphics.fill(x1 - 1, y0, x1, y1, color);
	}

	private void reorderOption(int from, int to) {
		if (openMore == null) return;
		List<RadialNode> opts = openMore.options();
		if (from < 0 || from >= opts.size() || from == to) return;
		RadialNode moved = opts.remove(from);
		if (to > from) to--;
		to = Mth.clamp(to, 0, opts.size());
		opts.add(to, moved);

		List<String> keys = new ArrayList<>();
		boolean orderable = true;
		for (RadialNode node : opts) {
			String key = node.orderKey();
			if (key.isEmpty()) {
				orderable = false;
				break;
			}
			keys.add(key);
		}
		String categoryKey = openMore.categoryKey();
		if (orderable) {
			RadialLayoutStore.setOrder(categoryKey, keys);
			RadialLayoutStore.save();
		}
		buildBaseNodes();
		MoreNode refreshed = findMoreNode(categoryKey);
		if (refreshed != null) {
			openMore = refreshed;
			panelOptions = refreshed.options();
			refreshFrozenHoverFor(refreshed);
		}
	}

	private void refreshFrozenHoverFor(MoreNode target) {
		Hover rebuilt = new Hover();
		for (RadialNode base : baseNodes) {
			if (findPathTo(base, target, rebuilt.path)) {
				rebuilt.path.add(0, base);
				rebuilt.deepest = target;
				break;
			}
		}
		frozenHover = rebuilt;
	}

	private boolean findPathTo(RadialNode current, RadialNode target, List<RadialNode> outPath) {
		if (current == target) return true;
		for (RadialNode child : current.children(statsData)) {
			if (findPathTo(child, target, outPath)) {
				outPath.add(0, child);
				return true;
			}
		}
		return false;
	}

	private MoreNode findMoreNode(String categoryKey) {
		for (RadialNode base : baseNodes) {
			for (RadialNode child : base.children(statsData)) {
				if (child instanceof MoreNode more && more.categoryKey().equals(categoryKey)) return more;
			}
		}
		return null;
	}

	private float computeOpenScale() {
		long ms = System.currentTimeMillis();
		if (closing) {
			float p = Math.min(1.0f, (float) (ms - closeStartTime) / ANIMATION_DURATION);
			return easeOut(Math.max(0.0f, 1.0f - p));
		}
		return easeOut(Math.min(1.0f, (float) (ms - openTime) / ANIMATION_DURATION));
	}

	private boolean isActiveSlot(RadialNode node) {
		return node.visible(statsData) && node.interactive(statsData);
	}

	private void drawBaseSectors(GuiGraphics graphics, float cx, float cy, Hover hover, float openScale) {
		for (int i = 0; i < baseNodes.size(); i++) {
			RadialNode node = baseNodes.get(i);
			boolean active = isActiveSlot(node);
			float highlight = 0f;
			float hoverAmt = 0f;
			float[] color = PANEL_INACTIVE;
			if (active) {
				updateAnim(node, hover);
				if (node instanceof AbstractRadialNode a) {
					highlight = a.animHighlight;
					hoverAmt = a.animScale;
				}
				color = lerpColor(PANEL, PANEL_HOVER, Math.max(highlight * 0.5f, hoverAmt));
			}
			float center = baseCenter(i);
			float rIn = (R_INNER - 2f * hoverAmt) * openScale;
			float rOut = (R_OUTER + 4f * hoverAmt) * openScale;
			float half = 22.5f - SECTOR_GAP_DEG / 2f;
			fillSector(graphics, cx, cy, rIn, rOut, center - half, center + half, color);
		}
	}

	private void drawBaseFaces(GuiGraphics graphics, float cx, float cy, float openScale) {
		float radius = (R_INNER + R_OUTER) / 2f * openScale;
		for (int i = 0; i < baseNodes.size(); i++) {
			RadialNode node = baseNodes.get(i);
			if (!isActiveSlot(node)) continue;
			float hoverAmt = node instanceof AbstractRadialNode a ? a.animScale : 0f;
			drawFace(graphics, cx, cy, node, baseCenter(i), radius, ICON_BASE, hoverAmt, 1.0f, (360f / SLOTS) / 2f);
		}
	}

	private void drawChildSectors(GuiGraphics graphics, float cx, float cy, RadialNode parent, float parentCenter, int level, Hover hover, boolean facesPass, float openScale) {
		float expand = parent instanceof AbstractRadialNode a ? a.animExpand : 0f;
		if (expand < 0.01f) return;

		List<RadialNode> vis = visibleChildren(parent);
		int k = vis.size();
		if (k == 0) return;

		float t = easeOut(expand);
		float ringInner = (R_OUTER + (level - 1) * CHILD_BAND) * openScale;
		float ringOuter = ringInner + CHILD_BAND * openScale * t;
		float faceRadius = (ringInner + ringOuter) / 2f;
		float half = childDrawnHalfDeg(level);

		for (int j = 0; j < k; j++) {
			RadialNode child = vis.get(j);
			float center = parentCenter + (j - (k - 1) / 2.0f) * childArcDeg(level);
			if (!facesPass) {
				updateAnim(child, hover);
				float highlight = child instanceof AbstractRadialNode a ? a.animHighlight : 0f;
				float hoverAmt = child instanceof AbstractRadialNode a2 ? a2.animScale : 0f;
				float[] color = lerpColor(CHILD_PANEL, CHILD_HOVER, Math.max(highlight * 0.5f, hoverAmt));
				color = new float[]{color[0], color[1], color[2], color[3] * t};
				float grow = 4f * hoverAmt;
				fillSector(graphics, cx, cy, ringInner, ringOuter + grow, center - half, center + half, color);
			} else if (t >= 0.4f) {
				float hoverAmt = child instanceof AbstractRadialNode a ? a.animScale : 0f;
				drawFace(graphics, cx, cy, child, center, faceRadius, ICON_CHILD, hoverAmt, t, childArcDeg(level) / 2f);
			}
			drawChildSectors(graphics, cx, cy, child, center, level + 1, hover, facesPass, openScale);
		}
	}

	private void fillSector(GuiGraphics graphics, float cx, float cy, float rIn, float rOut, float startDeg, float endDeg, float[] color) {
		if (rOut <= rIn || color[3] <= 0.01f) return;
		Matrix4f mat = graphics.pose().last().pose();

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);

		Tesselator tess = Tesselator.getInstance();
		BufferBuilder buf = tess.getBuilder();
		buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

		int steps = Math.max(2, (int) Math.ceil(Math.abs(endDeg - startDeg) / 5.0));
		float r = color[0], g = color[1], b = color[2], a = color[3];
		for (int s = 0; s < steps; s++) {
			double a0 = Math.toRadians(startDeg + (endDeg - startDeg) * s / steps);
			double a1 = Math.toRadians(startDeg + (endDeg - startDeg) * (s + 1) / steps);
			float cos0 = (float) Math.cos(a0), sin0 = (float) Math.sin(a0);
			float cos1 = (float) Math.cos(a1), sin1 = (float) Math.sin(a1);
			float ix0 = cx + cos0 * rIn, iy0 = cy + sin0 * rIn;
			float ox0 = cx + cos0 * rOut, oy0 = cy + sin0 * rOut;
			float ix1 = cx + cos1 * rIn, iy1 = cy + sin1 * rIn;
			float ox1 = cx + cos1 * rOut, oy1 = cy + sin1 * rOut;
			buf.vertex(mat, ix0, iy0, 0).color(r, g, b, a).endVertex();
			buf.vertex(mat, ox0, oy0, 0).color(r, g, b, a).endVertex();
			buf.vertex(mat, ox1, oy1, 0).color(r, g, b, a).endVertex();
			buf.vertex(mat, ix0, iy0, 0).color(r, g, b, a).endVertex();
			buf.vertex(mat, ox1, oy1, 0).color(r, g, b, a).endVertex();
			buf.vertex(mat, ix1, iy1, 0).color(r, g, b, a).endVertex();
		}
		tess.end();

		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	private void drawFace(GuiGraphics graphics, float cx, float cy, RadialNode node, float angleDeg, float radius, float iconBase, float hoverAmt, float alpha, float sectorHalfDeg) {
		double rad = Math.toRadians(angleDeg);
		float x = (float) (cx + Math.cos(rad) * radius);
		float y = (float) (cy + Math.sin(rad) * radius);
		int maxWidth = Math.max(28, Math.round(2f * radius * (float) Math.sin(Math.toRadians(sectorHalfDeg)) * 1.12f));
		String faceText = node.faceText(statsData);
		if (faceText != null) drawFaceText(graphics, faceText, x, y - 3f, iconBase, hoverAmt, node.labelColor(statsData), alpha);
		else drawIcon(graphics, node.icon(statsData), x, y - 3f, iconBase, hoverAmt, node.iconTint(statsData), alpha);
		drawLabel(graphics, node, x, y - 3f, iconBase, hoverAmt, alpha, maxWidth);
	}

	private void drawFaceText(GuiGraphics graphics, String text, float x, float y, float iconBase, float hoverAmt, int color, float alpha) {
		if (alpha < 0.4f) return;
		float scale = 1.5f + 0.25f * hoverAmt;
		Style style = Style.EMPTY.withFont(DMZ_FONT);
		Component line = Component.literal(text).withStyle(style);
		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0);
		graphics.pose().scale(scale, scale, 1f);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, line, 0, -this.font.lineHeight / 2, color, 0x000000);
		graphics.pose().popPose();
	}

	private void drawIcon(GuiGraphics graphics, ResourceLocation icon, float x, float y, float iconBase, float hoverAmt, int tint, float alpha) {
		if (icon == null) return;
		float size = iconBase + 4f * hoverAmt;
		float half = size / 2f;
		ResourceLocation safe = DMZSkinLayer.getSafeTexture(icon);

		float r = 1f, g = 1f, b = 1f;
		if (tint >= 0) {
			r = ((tint >> 16) & 0xFF) / 255f;
			g = ((tint >> 8) & 0xFF) / 255f;
			b = (tint & 0xFF) / 255f;
		}

		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(r, g, b, alpha);
		graphics.blit(safe, Math.round(x - half), Math.round(y - half), Math.round(size), Math.round(size), 0.0F, 0.0F, 18, 18, 18, 18);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
	}

	private void drawLabel(GuiGraphics graphics, RadialNode node, float x, float y, float iconBase, float hoverAmt, float alpha, int maxWidth) {
		if (alpha < 0.4f) return;
		Component label = node.label(statsData);
		if (label == null || label.getString().isEmpty()) return;

		int color = node.labelColor(statsData);
		Style style = Style.EMPTY.withFont(DMZ_FONT);
		List<String> lines = TextUtil.wrap(this.font, label.getString(), maxWidth, style);
		int lineHeight = this.font.lineHeight;
		int startY = Math.round(y + (iconBase + 4f * hoverAmt) / 2f + 2f);
		int centerX = Math.round(x);
		for (int i = 0; i < lines.size(); i++) {
			Component line = Component.literal(lines.get(i)).withStyle(style);
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, line, centerX, startY + i * lineHeight, color, 0x000000);
		}
	}

	private void updateAnim(RadialNode node, Hover hover) {
		if (!(node instanceof AbstractRadialNode a)) return;
		boolean hovered = node == hover.deepest;
		boolean inPath = hover.path.contains(node);
		boolean lit = hovered || inPath;
		a.animHighlight = approach(a.animHighlight, lit ? 1f : 0f);
		a.animScale = approach(a.animScale, hovered ? 1f : 0f);
		a.animExpand = approach(a.animExpand, (lit && node.expandable(statsData)) ? 1f : 0f);
	}

	private List<RadialNode> visibleChildren(RadialNode node) {
		List<RadialNode> out = new ArrayList<>();
		for (RadialNode child : node.children(statsData)) {
			if (child.visible(statsData)) out.add(child);
		}
		return out;
	}

	private Hover resolveHover(float cx, float cy, double mouseX, double mouseY, float openScale) {
		Hover result = new Hover();
		double dx = mouseX - cx;
		double dy = mouseY - cy;
		double d = Math.sqrt(dx * dx + dy * dy);
		double ang = Math.toDegrees(Math.atan2(dy, dx));

		float inner = R_INNER * openScale;
		float outer = R_OUTER * openScale;
		float band = CHILD_BAND * openScale;

		if (d < inner) {
			chain.clear();
			return result;
		}

		int level = d < outer ? 0 : (int) (1 + Math.floor((d - outer) / band));

		if (level == 0) {
			int slot = baseSlotAt(ang);
			RadialNode node = baseNodes.get(slot);
			chain.clear();
			if (!isActiveSlot(node)) return result;
			chain.add(node);
			result.deepest = node;
			result.path.add(node);
			result.deepestAngleDeg = baseCenter(slot);
			result.deepestLevel = 0;
			return result;
		}

		if (chain.isEmpty()) {
			int slot = baseSlotAt(ang);
			RadialNode node = baseNodes.get(slot);
			if (!isActiveSlot(node)) return result;
			chain.add(node);
		}
		while (chain.size() > level + 1) chain.remove(chain.size() - 1);

		RadialNode base = chain.get(0);
		result.path.add(base);
		result.deepest = base;
		result.deepestAngleDeg = baseCenter(baseNodes.indexOf(base));
		result.deepestLevel = 0;

		float center = baseCenter(baseNodes.indexOf(base));
		RadialNode current = base;
		for (int l = 1; l <= level; l++) {
			if (!current.expandable(statsData)) break;
			List<RadialNode> vis = visibleChildren(current);
			int k = vis.size();
			if (k == 0) break;

			int idx;
			if (l == level) {
				idx = pickChildByAngle(vis, center, ang, l);
				if (idx < 0) break;
				if (chain.size() > l) chain.set(l, vis.get(idx));
				else chain.add(vis.get(idx));
			} else {
				RadialNode committed = l < chain.size() ? chain.get(l) : null;
				idx = committed != null ? vis.indexOf(committed) : -1;
				if (idx < 0) {
					idx = pickChildByAngle(vis, center, ang, l);
					if (idx < 0) break;
					if (l < chain.size()) chain.set(l, vis.get(idx));
					else chain.add(vis.get(idx));
				}
			}

			RadialNode child = vis.get(idx);
			center = center + (idx - (k - 1) / 2.0f) * childArcDeg(l);
			result.path.add(child);
			result.deepest = child;
			result.deepestAngleDeg = center;
			result.deepestLevel = l;
			current = child;
		}

		while (chain.size() > result.path.size()) chain.remove(chain.size() - 1);
		return result;
	}

	private int pickChildByAngle(List<RadialNode> children, float parentCenter, double ang, int level) {
		int k = children.size();
		float arc = childArcDeg(level);
		float best = arc / 2f;
		int bestIdx = -1;
		for (int j = 0; j < k; j++) {
			float center = parentCenter + (j - (k - 1) / 2.0f) * arc;
			float diff = (float) absAngleDiff(ang, center);
			if (diff < best) {
				best = diff;
				bestIdx = j;
			}
		}
		return bestIdx;
	}

	private static float baseArcLength() {
		return (R_INNER + R_OUTER) / 2f * (float) Math.toRadians(360f / SLOTS);
	}

	private static float childArcDeg(int level) {
		float midRadius = R_OUTER + (level - 1) * CHILD_BAND + CHILD_BAND / 2f;
		float fullDeg = (float) Math.toDegrees(1.25f * baseArcLength() / midRadius);
		return Math.min(fullDeg, 360f / SLOTS);
	}

	private static float childDrawnHalfDeg(int level) {
		return childArcDeg(level) / 2f - SECTOR_GAP_DEG / 2f;
	}

	private static int baseSlotAt(double angleDeg) {
		return Math.floorMod(Math.round((float) ((angleDeg + 90.0) / 45.0)), SLOTS);
	}

	private static float baseCenter(int index) {
		return -90f + index * (360f / SLOTS);
	}

	private static double absAngleDiff(double a, double b) {
		double d = ((a - b) % 360.0 + 540.0) % 360.0 - 180.0;
		return Math.abs(d);
	}

	private float approach(float current, float target) {
		float step = Math.min(1.0f, frameDt * ANIM_SPEED);
		return current + (target - current) * step;
	}

	private static float easeOut(float t) {
		float inv = 1.0f - t;
		return 1.0f - inv * inv * inv;
	}

	private static float[] lerpColor(float[] a, float[] b, float t) {
		t = Math.max(0f, Math.min(1f, t));
		return new float[]{
				a[0] + (b[0] - a[0]) * t,
				a[1] + (b[1] - a[1]) * t,
				a[2] + (b[2] - a[2]) * t,
				a[3] + (b[3] - a[3]) * t
		};
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (closing || statsData == null) return true;
		double ux = toUiX(mouseX), uy = toUiY(mouseY);
		float cx = getUiWidth() / 2f, cy = getUiHeight() / 2f;

		if (panelOptions != null) {
			int[] b = panelBounds(cx, cy);
			boolean inside = ux >= b[0] && ux <= b[0] + b[2] && uy >= b[1] && uy <= b[1] + b[3];
			if (!inside) {
				closePanel();
				return true;
			}
			int rowsTop = b[1] + PANEL_TITLE_H;
			if (panelScrollable) {
				int rel = rowIndexAt(rowsTop, uy);
				int i = panelScroll + rel;
				if (rel >= 0 && rel < visiblePanelRows() && i < panelOptions.size()) {
					RadialNode row = panelOptions.get(i);
					if (row.interactive(statsData)) row.onSelect(statsData);
				}
				return true;
			}
			int idx = rowIndexAt(rowsTop, uy);
			if (idx >= 0 && idx < panelOptions.size()) {
				dragIndex = idx;
				dragStartY = uy;
				dragCurrentY = uy;
				dragging = false;
			}
			return true;
		}

		Hover hover = resolveHover(cx, cy, ux, uy, computeOpenScale());
		RadialNode node = hover.deepest;
		if (node instanceof MoreNode more) {
			openPanel(more.options(), Component.translatable("gui.dragonminez.radial.options"), hover, false);
			openMore = more;
			return true;
		}
		if (node instanceof ReleaseNode release) {
			if (release.active(statsData)) {
				release.onSelect(statsData);
				return true;
			}
			openPanel(release.buildOptions(statsData), release.label(statsData), hover, true);
			return true;
		}
		if (node != null && node.interactive(statsData) && !node.expandable(statsData)) {
			node.onSelect(statsData);
			return true;
		}
		return true;
	}

	private void openPanel(List<RadialNode> options, Component title, Hover hover, boolean scrollable) {
		panelOptions = options;
		panelTitle = title;
		panelScrollable = scrollable;
		panelScroll = 0;
		panelAngleDeg = hover.deepestAngleDeg;
		panelLevel = hover.deepestLevel;
		frozenHover = hover;
	}

	private void closePanel() {
		panelOptions = null;
		panelTitle = null;
		panelScrollable = false;
		panelScroll = 0;
		openMore = null;
		dragIndex = -1;
		dragging = false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (panelOptions != null && !panelScrollable && dragIndex >= 0) {
			double uy = toUiY(mouseY);
			if (Math.abs(uy - dragStartY) > 3) dragging = true;
			dragCurrentY = uy;
			return true;
		}
		return true;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (panelOptions != null && !panelScrollable && dragIndex >= 0) {
			double uy = toUiY(mouseY);
			if (!dragging) {
				RadialNode row = panelOptions.get(dragIndex);
				if (row.interactive(statsData)) row.onSelect(statsData);
			} else {
				int[] b = panelBounds(getUiWidth() / 2f, getUiHeight() / 2f);
				int rowsTop = b[1] + PANEL_TITLE_H;
				int target = Mth.clamp(rowIndexAt(rowsTop, uy), 0, panelOptions.size());
				reorderOption(dragIndex, target);
			}
			dragIndex = -1;
			dragging = false;
			return true;
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (panelOptions != null && panelScrollable) {
			int maxScroll = Math.max(0, panelOptions.size() - visiblePanelRows());
			panelScroll = Mth.clamp(panelScroll - (int) Math.signum(delta), 0, maxScroll);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == KeyBinds.UTILITY_MENU.getKey().getValue()) return true;
		if (keyCode == 256) {
			if (panelOptions != null) {
				closePanel();
				return true;
			}
			onClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void tick() {
		super.tick();
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> this.statsData = data);
		}
		if (closing && System.currentTimeMillis() - closeStartTime >= ANIMATION_DURATION) forceClose();
	}

	@Override
	public void onClose() {
		startClosingAnimation();
	}

	public void startClosingAnimation() {
		if (closing) return;
		closing = true;
		closeStartTime = System.currentTimeMillis();
	}

	private void forceClose() {
		if (this.minecraft != null) this.minecraft.setScreen(null);
	}

	public static boolean isUtilityMenuReopenBlocked() {
		return System.currentTimeMillis() < utilityMenuReopenBlockedUntilMs;
	}

	public static void initMenuSlots() {
	}

	public static void addMenuSlot(IUtilityMenuSlot menuSlot) {
		ADDON_SLOTS.add(menuSlot);
	}

	private static final class Hover {
		private final List<RadialNode> path = new ArrayList<>();
		private RadialNode deepest = null;
		private float deepestAngleDeg = 0f;
		private int deepestLevel = 0;
	}
}