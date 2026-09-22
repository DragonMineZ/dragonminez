package com.dragonminez.client.gui.config;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.hud.AlternativeHUD;
import com.dragonminez.client.gui.hud.BabaReturnTimerHUD;
import com.dragonminez.client.gui.hud.HudSmoother;
import com.dragonminez.client.gui.hud.HudStyle;
import com.dragonminez.client.gui.hud.KiReserveHUD;
import com.dragonminez.client.gui.hud.MinecraftHUD;
import com.dragonminez.client.gui.hud.ModernHUD;
import com.dragonminez.client.gui.hud.PartyHUD;
import com.dragonminez.client.gui.hud.QuestNoticeHUD;
import com.dragonminez.client.gui.hud.RageMeterHUD;
import com.dragonminez.client.gui.hud.ScouterHUD;
import com.dragonminez.client.gui.hud.TechniqueHotbarHUD;
import com.dragonminez.client.gui.hud.TrackedQuestHUD;
import com.dragonminez.client.gui.hud.WorldBossContributionHUD;
import com.dragonminez.client.gui.hud.XenoverseHUD;
import com.dragonminez.client.gui.hud.layout.HudElement;
import com.dragonminez.client.gui.hud.layout.HudLayout;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.HudPlacement;
import com.dragonminez.common.init.MainSounds;
import com.google.gson.Gson;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class HudEditorScreen extends Screen {
	private static final int DIM_COLOR = 0xA8000000;
	private static final int BOX_COLOR = 0x70FFFFFF;
	private static final int BOX_HOVER_COLOR = 0xFFFFFFFF;
	private static final int BOX_SELECTED_COLOR = 0xFF4FC3FF;
	private static final int BOX_FILL_HOVER = 0x18FFFFFF;
	private static final int BOX_FILL_SELECTED = 0x284FC3FF;
	private static final int GUIDE_COLOR = 0xFFFF4FD8;
	private static final float SNAP_DISTANCE = 5.0f;
	private static final float SCREEN_MARGIN = 2.0f;
	private static final float ELEMENT_GAP = 2.0f;
	private static final float HANDLE = 5.0f;
	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");
	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");
	private static final int BUTTON_WIDTH = 74;
	private static final int BUTTON_GAP = 4;
	private static final int TITLE_Y = 6;
	private static final int STYLE_ROW_Y = 18;
	private static final int ACTION_ROW_Y = 42;
	private static final int HINT_BOTTOM_OFFSET = 62;

	private static final int HISTORY_LIMIT = 5;
	private static final long NUDGE_MERGE_MS = 800L;
	private static final float INACTIVE_BUTTON_ALPHA = 0.4f;
	private static final Gson HISTORY_GSON = new Gson();

	private enum Drag { NONE, MOVE, RESIZE }

	private record Snapshot(HudStyle style, Map<String, Map<String, HudPlacement>> layout) {
		private String key() {
			return style.configName() + HISTORY_GSON.toJson(layout);
		}
	}

	private final Screen parent;
	private final Snapshot initial;
	private final Deque<Snapshot> undoStack = new ArrayDeque<>();
	private final Deque<Snapshot> redoStack = new ArrayDeque<>();
	private Snapshot pending;
	private HudElement lastNudged;
	private long lastNudgeMs;
	private final HudSmoother panelAlpha = new HudSmoother(0.08f, 0.01f);
	private HudStyle style;
	private final List<Float> guidesX = new ArrayList<>();
	private final List<Float> guidesY = new ArrayList<>();

	private HudElement selected;
	private Drag drag = Drag.NONE;
	private int handle = -1;
	private float grabX;
	private float grabY;
	private HudLayout.Box dragStart;
	private final Map<HudElement, HudLayout.Box> groupStart = new EnumMap<>(HudElement.class);
	private boolean groupDrag;
	private final List<TexturedTextButton> buttons = new ArrayList<>();
	private TexturedTextButton styleButton;
	private TexturedTextButton toggleButton;
	private TexturedTextButton extrasButton;
	private TexturedTextButton undoButton;
	private TexturedTextButton redoButton;
	private boolean extras;
	private boolean confirmed;

	public HudEditorScreen(Screen parent) {
		super(Component.translatable("gui.dragonminez.hud_editor.title"));
		this.parent = parent;
		ConfigManager.reloadHudLayoutConfig();
		this.style = HudStyle.current();
		this.initial = snapshot();
	}

	private Snapshot snapshot() {
		Map<String, Map<String, HudPlacement>> copy = new LinkedHashMap<>();
		ConfigManager.getHudLayoutConfig().getLayout().forEach((group, placements) -> {
			Map<String, HudPlacement> inner = new LinkedHashMap<>();
			placements.forEach((id, placement) -> inner.put(id, placement.copy()));
			copy.put(group, inner);
		});
		return new Snapshot(style, copy);
	}

	private void restore(Snapshot snapshot) {
		Map<String, Map<String, HudPlacement>> layout = ConfigManager.getHudLayoutConfig().getLayout();
		layout.clear();
		snapshot.layout().forEach((group, placements) -> {
			Map<String, HudPlacement> inner = new LinkedHashMap<>();
			placements.forEach((id, placement) -> inner.put(id, placement.copy()));
			layout.put(group, inner);
		});
		style = snapshot.style();
		ConfigManager.getHudLayoutConfig().setStyle(style.configName());
		if (styleButton != null) styleButton.setMessage(styleLabel());
		if (selected != null && !elements().contains(selected)) selected = null;
	}

	private void record(Snapshot before) {
		if (before == null || before.key().equals(snapshot().key())) return;
		undoStack.addLast(before);
		while (undoStack.size() > HISTORY_LIMIT) undoStack.removeFirst();
		redoStack.clear();
	}

	private void act(Runnable change) {
		Snapshot before = snapshot();
		change.run();
		record(before);
		lastNudged = null;
	}

	private void undo() {
		if (undoStack.isEmpty() || drag != Drag.NONE) return;
		redoStack.addLast(snapshot());
		restore(undoStack.removeLast());
		lastNudged = null;
	}

	private void redo() {
		if (redoStack.isEmpty() || drag != Drag.NONE) return;
		undoStack.addLast(snapshot());
		restore(redoStack.removeLast());
		lastNudged = null;
	}

	@Override
	protected void init() {
		super.init();
		HudLayout.setPreview(true);
		buttons.clear();
		this.styleButton = button(styleLabel(), b -> act(this::cycleStyle));
		this.extrasButton = button(extrasLabel(), b -> toggleExtras());
		this.undoButton = button(tr("gui.dragonminez.hud_editor.undo", undoStack.size()), b -> undo());
		this.redoButton = button(tr("gui.dragonminez.hud_editor.redo", redoStack.size()), b -> redo());
		button(tr("gui.dragonminez.hud_editor.done"), b -> confirm());
		button(tr("gui.dragonminez.hud_editor.cancel"), b -> cancel());
		button(tr("gui.dragonminez.hud_editor.reset"), b -> act(this::reset));
		this.toggleButton = button(tr("gui.dragonminez.hud_editor.hide"), b -> act(this::toggleVisible));
		panelAlpha.snap(1.0f);
		layoutPanel();
	}

	private TexturedTextButton button(MutableComponent message, TexturedTextButton.OnPress onPress) {
		TexturedTextButton created = new TexturedTextButton.Builder()
				.position(0, 0)
				.size(BUTTON_WIDTH, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(BUTTON_WIDTH, 20)
				.message(message)
				.onPress(onPress)
				.build();
		buttons.add(created);
		return this.addRenderableWidget(created);
	}

	private MutableComponent tr(String key, Object... args) {
		return Component.translatable(key, args).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	private MutableComponent styleLabel() {
		return tr(style.translationKey());
	}

	private MutableComponent extrasLabel() {
		return tr(extras ? "gui.dragonminez.hud_editor.main" : "gui.dragonminez.hud_editor.extras");
	}

	private void toggleExtras() {
		extras = !extras;
		selected = null;
		extrasButton.setMessage(extrasLabel());
	}

	private List<HudElement> elements() {
		return extras ? HudLayout.extraElements(style) : HudLayout.elements(style);
	}

	private int styleRowLeft() {
		int labelWidth = this.font.width(tr("gui.dragonminez.hud_editor.style"));
		return (this.width - (labelWidth + (BUTTON_GAP + BUTTON_WIDTH) * 4)) / 2;
	}

	private void layoutPanel() {
		int labelWidth = this.font.width(tr("gui.dragonminez.hud_editor.style"));
		styleButton.setPosition(styleRowLeft() + labelWidth + BUTTON_GAP, STYLE_ROW_Y);
		int rowX = styleRowLeft() + labelWidth + BUTTON_GAP;
		extrasButton.setPosition(rowX + (BUTTON_WIDTH + BUTTON_GAP), STYLE_ROW_Y);
		undoButton.setPosition(rowX + (BUTTON_WIDTH + BUTTON_GAP) * 2, STYLE_ROW_Y);
		redoButton.setPosition(rowX + (BUTTON_WIDTH + BUTTON_GAP) * 3, STYLE_ROW_Y);
		int x = (this.width - (BUTTON_WIDTH * 4 + BUTTON_GAP * 3)) / 2;
		for (TexturedTextButton button : buttons) {
			if (button == styleButton || button == extrasButton || button == undoButton || button == redoButton) continue;
			button.setPosition(x, ACTION_ROW_Y);
			x += BUTTON_WIDTH + BUTTON_GAP;
		}
	}

	private void cycleStyle() {
		HudStyle[] all = HudStyle.values();
		style = all[(style.ordinal() + 1) % all.length];
		ConfigManager.getHudLayoutConfig().setStyle(style.configName());
		selected = null;
		styleButton.setMessage(styleLabel());
	}

	private HudLayout.Box box(HudElement element) {
		return HudLayout.baseBox(style, element, this.width, this.height);
	}

	private void confirm() {
		confirmed = true;
		ConfigManager.saveHudLayoutConfig();
		this.minecraft.setScreen(parent);
	}

	private void cancel() {
		restore(initial);
		confirmed = true;
		this.minecraft.setScreen(parent);
	}

	private void reset() {
		if (selected != null && selected.isSkill()) for (HudElement skill : HudElement.skills()) HudLayout.resetPlacement(style, skill);
		else if (selected != null) HudLayout.resetPlacement(style, selected);
		else for (HudElement element : elements()) HudLayout.resetPlacement(style, element);
	}

	private void toggleVisible() {
		if (selected == null) return;
		HudPlacement placement = HudLayout.placement(style, selected).copy();
		placement.setVisible(!placement.getVisible());
		HudLayout.setPlacement(style, selected, placement);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, this.width, this.height, DIM_COLOR);

		if (extras) {
			TrackedQuestHUD.render(graphics, partialTick, this.width, this.height);
			QuestNoticeHUD.render(graphics, partialTick, this.width, this.height);
			ScouterHUD.render(graphics, partialTick, this.width, this.height);
			BabaReturnTimerHUD.render(graphics, partialTick, this.width, this.height);
			WorldBossContributionHUD.render(graphics, partialTick, this.width, this.height);
		} else {
			XenoverseHUD.render(graphics, partialTick, this.width, this.height);
			AlternativeHUD.render(graphics, partialTick, this.width, this.height);
			ModernHUD.render(graphics, partialTick, this.width, this.height);
			MinecraftHUD.render(graphics, partialTick, this.width, this.height);
			KiReserveHUD.render(graphics, partialTick, this.width, this.height);
			RageMeterHUD.render(graphics, partialTick, this.width, this.height);
			PartyHUD.render(graphics, partialTick, this.width, this.height);
			TechniqueHotbarHUD.render(graphics, partialTick, this.width, this.height);
		}

		graphics.pose().pushPose();
		graphics.pose().translate(0.0f, 0.0f, 400.0f);
		HudElement hovered = drag == Drag.NONE ? elementAt(mouseX, mouseY) : selected;
		for (HudElement element : elements()) drawBox(graphics, element, element == hovered);
		for (float x : guidesX) graphics.fill(Math.round(x), 0, Math.round(x) + 1, this.height, GUIDE_COLOR);
		for (float y : guidesY) graphics.fill(0, Math.round(y), this.width, Math.round(y) + 1, GUIDE_COLOR);

		toggleButton.active = selected != null;
		undoButton.active = !undoStack.isEmpty();
		redoButton.active = !redoStack.isEmpty();
		undoButton.setMessage(tr("gui.dragonminez.hud_editor.undo", undoStack.size()));
		redoButton.setMessage(tr("gui.dragonminez.hud_editor.redo", redoStack.size()));
		boolean visible = selected == null || HudLayout.placement(style, selected).getVisible();
		toggleButton.setMessage(tr(visible ? "gui.dragonminez.hud_editor.hide" : "gui.dragonminez.hud_editor.show"));

		float alpha = panelAlpha.update(drag == Drag.NONE ? 1.0f : 0.0f);
		layoutPanel();
		for (TexturedTextButton button : buttons) {
			button.setAlpha(button.active ? alpha : alpha * INACTIVE_BUTTON_ALPHA);
			button.visible = alpha > 0.02f;
		}

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.hud_editor.title"), this.width / 2, TITLE_Y, 0xFFFFD700);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.hud_editor.hint"), this.width / 2, this.height - HINT_BOTTOM_OFFSET, 0xFFC8D0DC);
		boolean skillHint = (selected != null && selected.isSkill()) || (hovered != null && hovered.isSkill());
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr(skillHint ? "gui.dragonminez.hud_editor.hint_skills" : "gui.dragonminez.hud_editor.hint_history", HISTORY_LIMIT),
				this.width / 2, this.height - HINT_BOTTOM_OFFSET + 11, 0xFFC8D0DC);
		int labelAlpha = Math.round(alpha * 255.0f);
		if (labelAlpha >= 8) {
			TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.hud_editor.style"), styleRowLeft(), STYLE_ROW_Y + 6,
					(labelAlpha << 24) | 0xFFFFFF, labelAlpha << 24);
		}
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.pose().popPose();
	}

	private void drawBox(GuiGraphics graphics, HudElement element, boolean hovered) {
		HudLayout.Box box = box(element);
		boolean isSelected = element == selected;
		int x0 = Math.round(box.x()), y0 = Math.round(box.y()), x1 = Math.round(box.right()), y1 = Math.round(box.bottom());
		int color = isSelected ? BOX_SELECTED_COLOR : hovered ? BOX_HOVER_COLOR : BOX_COLOR;

		if (isSelected || hovered) graphics.fill(x0, y0, x1, y1, isSelected ? BOX_FILL_SELECTED : BOX_FILL_HOVER);
		graphics.fill(x0, y0, x1, y0 + 1, color);
		graphics.fill(x0, y1 - 1, x1, y1, color);
		graphics.fill(x0, y0, x0 + 1, y1, color);
		graphics.fill(x1 - 1, y0, x1, y1, color);

		if (isSelected || hovered) {
			String label = Component.translatable(element.translationKey()).getString()
					+ (box.hotbar() ? "" : String.format(Locale.ROOT, "  x%.2f", box.scale() / HudLayout.unit(element, this.height)))
					+ (box.visible() ? "" : "  -");
			int labelY = y0 - 10 >= 0 ? y0 - 10 : y1 + 2;
			MutableComponent labelText = Component.literal(label).withStyle(Style.EMPTY.withFont(DMZ_FONT));
			TextUtil.drawStringWithBorder(graphics, this.font, labelText, Mth.clamp(x0, 0, Math.max(0, this.width - this.font.width(labelText))), labelY, color);
		}
		if (isSelected) {
			for (int i = 0; i < 8; i++) {
				float[] point = handlePoint(box, i);
				graphics.fill(Math.round(point[0] - HANDLE / 2.0f), Math.round(point[1] - HANDLE / 2.0f),
						Math.round(point[0] + HANDLE / 2.0f), Math.round(point[1] + HANDLE / 2.0f), 0xFFFFFFFF);
			}
		}
	}

	private static float[] handlePoint(HudLayout.Box box, int index) {
		float fx = index == 0 || index == 6 || index == 7 ? 0.0f : index == 1 || index == 5 ? 0.5f : 1.0f;
		float fy = index <= 2 ? 0.0f : index == 3 || index == 7 ? 0.5f : 1.0f;
		return new float[]{box.x() + box.width() * fx, box.y() + box.height() * fy, fx, fy};
	}

	private HudElement elementAt(double mouseX, double mouseY) {
		HudElement best = null;
		float bestArea = Float.MAX_VALUE;
		for (HudElement element : elements()) {
			HudLayout.Box box = box(element);
			if (!box.contains(mouseX, mouseY)) continue;
			float area = box.width() * box.height();
			if (element == selected || area < bestArea) {
				best = element;
				bestArea = element == selected ? -1.0f : area;
			}
		}
		return best;
	}

	private int handleAt(double mouseX, double mouseY) {
		if (selected == null) return -1;
		HudLayout.Box box = box(selected);
		for (int i = 0; i < 8; i++) {
			float[] point = handlePoint(box, i);
			if (Math.abs(mouseX - point[0]) <= HANDLE && Math.abs(mouseY - point[1]) <= HANDLE) return i;
		}
		return -1;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;
		if (button == 1) {
			HudElement target = elementAt(mouseX, mouseY);
			if (target == null || !HudLayout.canMirror(style, target)) return false;
			selected = target;
			act(() -> {
				if (target.isSkill()) mirrorSkills();
				else {
					HudPlacement placement = HudLayout.placement(style, target).copy();
					placement.setMirrored(!placement.getMirrored());
					HudLayout.setPlacement(style, target, placement);
				}
			});
			this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(MainSounds.PIP_MENU.get(), 1.0F));
			return true;
		}
		if (button != 0) return false;

		int grabbed = handleAt(mouseX, mouseY);
		if (grabbed >= 0) {
			drag = Drag.RESIZE;
			handle = grabbed;
			pending = snapshot();
			dragStart = box(selected);
			captureGroup(selected.isSkill());
			return true;
		}

		selected = elementAt(mouseX, mouseY);
		if (selected == null) return false;
		dragStart = box(selected);
		captureGroup(selected.isSkill() && hasShiftDown());
		grabX = (float) mouseX - dragStart.x();
		grabY = (float) mouseY - dragStart.y();
		drag = Drag.MOVE;
		pending = snapshot();
		return true;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (pending != null) {
			record(pending);
			pending = null;
			lastNudged = null;
		}
		drag = Drag.NONE;
		handle = -1;
		groupDrag = false;
		groupStart.clear();
		guidesX.clear();
		guidesY.clear();
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (selected == null || drag == Drag.NONE) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
		if (drag == Drag.MOVE) move((float) mouseX - grabX, (float) mouseY - grabY);
		else resize((float) mouseX, (float) mouseY);
		return true;
	}

	private void captureGroup(boolean active) {
		groupStart.clear();
		groupDrag = active;
		if (!active) return;
		for (HudElement skill : HudElement.skills()) groupStart.put(skill, box(skill));
	}

	private void mirrorSkills() {
		boolean mirrored = !HudLayout.placement(style, HudElement.SKILL_1).getMirrored();
		Map<HudElement, HudLayout.Box> boxes = new EnumMap<>(HudElement.class);
		float left = Float.MAX_VALUE;
		float right = -Float.MAX_VALUE;
		for (HudElement skill : HudElement.skills()) {
			HudLayout.Box box = box(skill);
			boxes.put(skill, box);
			left = Math.min(left, box.x());
			right = Math.max(right, box.right());
		}
		for (Map.Entry<HudElement, HudLayout.Box> entry : boxes.entrySet()) {
			HudLayout.Box box = entry.getValue();
			float x = Mth.clamp(left + right - box.right(), 0.0f, Math.max(0.0f, this.width - box.width()));
			HudPlacement placement = HudLayout.toPlacement(entry.getKey(), x, box.y(), box.width(), box.height(), box.scale(), this.width, this.height, box.visible());
			placement.setMirrored(mirrored);
			HudLayout.setPlacement(style, entry.getKey(), placement);
		}
	}

	private void storeSibling(HudElement element, float x, float y, float width, float height, float scale) {
		x = Mth.clamp(x, 0.0f, Math.max(0.0f, this.width - width));
		y = Mth.clamp(y, 0.0f, Math.max(0.0f, this.height - height));
		HudPlacement current = HudLayout.placement(style, element);
		HudPlacement placement = HudLayout.toPlacement(element, x, y, width, height, scale, this.width, this.height, current.getVisible());
		placement.setMirrored(current.getMirrored());
		HudLayout.setPlacement(style, element, placement);
	}

	private void move(float x, float y) {
		HudLayout.Box current = box(selected);
		float width = current.hotbar() ? dragStart.width() : current.width();
		float height = current.hotbar() ? dragStart.height() : current.height();
		guidesX.clear();
		guidesY.clear();

		float[] snappedX = snap(x, width, true);
		float[] snappedY = snap(y, height, false);
		x = Mth.clamp(snappedX[0], 0.0f, Math.max(0.0f, this.width - width));
		y = Mth.clamp(snappedY[0], 0.0f, Math.max(0.0f, this.height - height));
		if (!Float.isNaN(snappedX[1])) guidesX.add(snappedX[1]);
		if (!Float.isNaN(snappedY[1])) guidesY.add(snappedY[1]);

		boolean visible = HudLayout.placement(style, selected).getVisible();
		HudPlacement placement = HudLayout.toPlacement(selected, x, y, width, height, current.hotbar() ? 1.0f : current.scale(), this.width, this.height, visible);
		if (isOnHotbar(x, y, width, height)) placement.hotbar();
		store(placement);

		if (!groupDrag) return;
		float dx = x - dragStart.x();
		float dy = y - dragStart.y();
		for (Map.Entry<HudElement, HudLayout.Box> entry : groupStart.entrySet()) {
			if (entry.getKey() == selected) continue;
			HudLayout.Box start = entry.getValue();
			storeSibling(entry.getKey(), start.x() + dx, start.y() + dy, start.width(), start.height(), start.scale());
		}
	}

	private void store(HudPlacement placement) {
		placement.setMirrored(HudLayout.placement(style, selected).getMirrored());
		HudLayout.setPlacement(style, selected, placement);
	}

	private boolean isOnHotbar(float x, float y, float width, float height) {
		if (selected != HudElement.MC_LEFT && selected != HudElement.MC_RIGHT) return false;
		float centerX = this.width / 2.0f;
		boolean bottom = Math.abs(y + height - this.height) <= 1.0f || Math.abs(y - (this.height - HudLayout.HOTBAR_HEIGHT)) <= 1.0f;
		boolean side = selected == HudElement.MC_LEFT
				? Math.abs(x + width - (centerX - HudLayout.HOTBAR_HALF_WIDTH)) <= 0.5f
				: Math.abs(x - (centerX + HudLayout.HOTBAR_HALF_WIDTH)) <= 0.5f;
		return bottom && side && Math.abs(HudLayout.placement(style, selected).getScale() - 1.0f) < 0.01f;
	}

	private float[] snap(float position, float size, boolean horizontal) {
		float screen = horizontal ? this.width : this.height;
		float unit = HudLayout.unit(selected, this.height);
		float margin = SCREEN_MARGIN * unit;
		float gap = ELEMENT_GAP * unit;
		List<float[]> candidates = new ArrayList<>();
		candidates.add(new float[]{0.0f, 0.0f, 0.0f});
		candidates.add(new float[]{margin, 0.0f, margin});
		candidates.add(new float[]{screen - size, 0.0f, screen - 1.0f});
		candidates.add(new float[]{screen - margin - size, 0.0f, screen - margin});
		candidates.add(new float[]{(screen - size) / 2.0f, 0.0f, screen / 2.0f});

		for (HudElement element : elements()) {
			if (element == selected || (groupDrag && element.isSkill())) continue;
			HudLayout.Box other = box(element);
			if (!other.visible()) continue;
			float near = horizontal ? other.x() : other.y();
			float far = horizontal ? other.right() : other.bottom();
			candidates.add(new float[]{far + gap, 0.0f, far});
			candidates.add(new float[]{near - gap - size, 0.0f, near});
			candidates.add(new float[]{near, 0.0f, near});
			candidates.add(new float[]{far - size, 0.0f, far});
			candidates.add(new float[]{(near + far - size) / 2.0f, 0.0f, (near + far) / 2.0f});
		}

		if (style == HudStyle.VANILLA && (selected == HudElement.MC_LEFT || selected == HudElement.MC_RIGHT)) {
			if (horizontal) {
				float centerX = this.width / 2.0f;
				candidates.add(new float[]{centerX - HudLayout.HOTBAR_HALF_WIDTH - size, 0.0f, centerX - HudLayout.HOTBAR_HALF_WIDTH});
				candidates.add(new float[]{centerX + HudLayout.HOTBAR_HALF_WIDTH, 0.0f, centerX + HudLayout.HOTBAR_HALF_WIDTH});
			} else {
				candidates.add(new float[]{this.height - HudLayout.HOTBAR_HEIGHT, 0.0f, this.height - HudLayout.HOTBAR_HEIGHT});
			}
		}

		float best = position;
		float guide = Float.NaN;
		float bestDistance = SNAP_DISTANCE;
		for (float[] candidate : candidates) {
			float distance = Math.abs(candidate[0] - position);
			if (distance <= bestDistance) {
				bestDistance = distance;
				best = candidate[0];
				guide = candidate[2];
			}
		}
		return new float[]{best, guide};
	}

	private void resize(float mouseX, float mouseY) {
		float[] size = HudLayout.baseSize(style, selected);
		float[] grabbed = handlePoint(dragStart, handle);
		float fixedX = dragStart.x() + dragStart.width() * (1.0f - grabbed[2]);
		float fixedY = dragStart.y() + dragStart.height() * (1.0f - grabbed[3]);

		float scaleX = grabbed[2] == 0.5f ? 0.0f : Math.abs(mouseX - fixedX) / size[0];
		float scaleY = grabbed[3] == 0.5f ? 0.0f : Math.abs(mouseY - fixedY) / size[1];
		float unit = HudLayout.unit(selected, this.height);
		float stored = Mth.clamp(Math.max(scaleX, scaleY) / unit, HudLayout.MIN_SCALE, HudLayout.MAX_SCALE);
		stored = Math.round(stored * 20.0f) / 20.0f;
		float scale = Math.min(stored * unit, Math.min(this.width * 0.95f / size[0], this.height * 0.95f / size[1]));

		float width = size[0] * scale;
		float height = size[1] * scale;
		float x = grabbed[2] == 0.0f ? fixedX - width : grabbed[2] == 1.0f ? fixedX : dragStart.centerX() - width / 2.0f;
		float y = grabbed[3] == 0.0f ? fixedY - height : grabbed[3] == 1.0f ? fixedY : dragStart.centerY() - height / 2.0f;
		x = Mth.clamp(x, 0.0f, Math.max(0.0f, this.width - width));
		y = Mth.clamp(y, 0.0f, Math.max(0.0f, this.height - height));

		boolean visible = HudLayout.placement(style, selected).getVisible();
		store(HudLayout.toPlacement(selected, x, y, width, height, scale, this.width, this.height, visible));

		if (!groupDrag) return;
		float ratio = scale / dragStart.scale();
		float originX = x - dragStart.x() * ratio;
		float originY = y - dragStart.y() * ratio;
		for (Map.Entry<HudElement, HudLayout.Box> entry : groupStart.entrySet()) {
			if (entry.getKey() == selected) continue;
			HudLayout.Box start = entry.getValue();
			storeSibling(entry.getKey(), originX + start.x() * ratio, originY + start.y() * ratio, size[0] * scale, size[1] * scale, scale);
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			confirm();
			return true;
		}
		if (hasControlDown() && keyCode == GLFW.GLFW_KEY_Z) {
			if (hasShiftDown()) redo();
			else undo();
			return true;
		}
		if (hasControlDown() && keyCode == GLFW.GLFW_KEY_Y) {
			redo();
			return true;
		}
		if (selected != null) {
			int step = hasShiftDown() ? 5 : 1;
			int dx = keyCode == GLFW.GLFW_KEY_LEFT ? -step : keyCode == GLFW.GLFW_KEY_RIGHT ? step : 0;
			int dy = keyCode == GLFW.GLFW_KEY_UP ? -step : keyCode == GLFW.GLFW_KEY_DOWN ? step : 0;
			if (dx != 0 || dy != 0) {
				HudLayout.Box current = box(selected);
				dragStart = current;
				boolean visible = HudLayout.placement(style, selected).getVisible();
				float x = Mth.clamp(current.x() + dx, 0.0f, Math.max(0.0f, this.width - current.width()));
				float y = Mth.clamp(current.y() + dy, 0.0f, Math.max(0.0f, this.height - current.height()));
				long now = System.currentTimeMillis();
				boolean merge = lastNudged == selected && now - lastNudgeMs <= NUDGE_MERGE_MS;
				Snapshot before = merge ? null : snapshot();
				store(HudLayout.toPlacement(selected, x, y, current.width(), current.height(), current.scale(), this.width, this.height, visible));
				record(before);
				lastNudged = selected;
				lastNudgeMs = now;
				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void removed() {
		HudLayout.setPreview(false);
		if (!confirmed) ConfigManager.saveHudLayoutConfig();
		super.removed();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
