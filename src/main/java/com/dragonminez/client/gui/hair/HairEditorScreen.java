package com.dragonminez.client.gui.hair;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.render.hair.HairEditSession;
import com.dragonminez.client.render.hair.HairPickRecorder;
import com.dragonminez.client.render.hair.HairRenderContext;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairCodec;
import com.dragonminez.common.hair.HairPresets;
import com.dragonminez.common.hair.HairSanitizer;
import com.dragonminez.common.hair.HairStrand;
import com.dragonminez.common.hair.HairStrandSizing;
import com.dragonminez.common.hair.HairStyleSlot;
import com.dragonminez.common.network.C2S.StatsSyncC2S;
import com.dragonminez.common.network.C2S.UpdateCustomHairC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.character.Character;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;

public class HairEditorScreen extends ScaledScreen {
	private static final ResourceLocation STAT_BUTTONS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");
	private static final ResourceLocation PANORAMA = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/background/roshi");
	private static final String HAIR_SALON_URL = "https://dragonminez.com/hairsalon";

	private static final int MARGIN = 8;
	private static final int GAP = 6;
	private static final int TOP_BAR_HEIGHT = 16;
	private static final int PANEL_TOP = 28;
	private static final int CLICK_DRAG_THRESHOLD = 3;
	private static final float MIN_ZOOM = 95.0f;
	private static final float MAX_ZOOM = 260.0f;
	private static final float ZOOM_STEP = 20.0f;
	private static final float ORBIT_SPEED = 1.0f;
	private static final int STATUS_TICKS = 60;

	private final PanoramaRenderer panorama = new PanoramaRenderer(new CubeMap(PANORAMA));
	private final Screen previousScreen;
	private final Character character;
	private final HairEditorState state;
	private final HairInspectorPanel inspector;
	private final HairOutlinerPanel outliner;
	private final HairColorPicker colorPicker = new HairColorPicker();
	private final HairViewportTool viewportTool;

	private float yaw = 180.0f;
	private float pitch;
	private float targetZoom = 150.0f;
	private float zoom = 150.0f;
	private int panX;
	private int panY;

	private boolean pressingViewport;
	private boolean pressingSecondary;
	private boolean orbiting;
	private boolean panning;
	private HairViewportTool.Mode pendingTool;
	private double pressX;
	private double pressY;
	private double lastX;
	private double lastY;

	private boolean confirmClose;
	private boolean closing;
	private Component statusText = Component.empty();
	private int statusColor = HairEditorUi.SUCCESS;
	private int statusTimer;

	private int viewportX0;
	private int viewportX1;
	private int viewportY0;
	private int viewportY1;

	public HairEditorScreen(Screen previousScreen, Character character) {
		super(HairEditorUi.tr("gui.dragonminez.hair_editor.title"));
		this.previousScreen = previousScreen;
		this.character = character;

		EnumMap<HairStyleSlot, CustomHair> styles = new EnumMap<>(HairStyleSlot.class);
		boolean fromPreset = character.getHairId() > 0;
		for (HairStyleSlot slot : HairStyleSlot.values()) {
			CustomHair source = fromPreset ? HairPresets.get(character.getHairId(), slot) : character.getOwnHairStyle(slot);
			CustomHair copy = source != null ? source.copy() : new CustomHair();
			if (character.getHairColor() != null) copy.setGlobalColor(character.getHairColor());
			styles.put(slot, copy);
		}
		this.state = new HairEditorState(styles, character.isRenderHairBase(), fromPreset);
		this.inspector = new HairInspectorPanel(state, Minecraft.getInstance().font, this::openColorPicker);
		this.outliner = new HairOutlinerPanel(state, Minecraft.getInstance().font, new HairOutlinerPanel.Listener() {
			public void onSelectionChanged() {
				inspector.requestRebuild();
			}

			public void onCodeAction(HairOutlinerPanel.CodeAction action) {
				handleCodeAction(action);
			}
		});
		this.viewportTool = new HairViewportTool(state);
	}

	protected void init() {
		super.init();
		clearWidgets();
		if (minecraft != null && minecraft.player != null) {
			HairEditSession.begin(minecraft.player.getUUID(), state.styles(), state.isHairBase());
		}
		layout();

		int buttonY = viewportY1 - 24;
		addRenderableWidget(new TexturedTextButton.Builder()
				.position(viewportX1 - 152, buttonY)
				.size(74, 20)
				.texture(STAT_BUTTONS)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(HairEditorUi.tr("gui.dragonminez.hair_editor.save"))
				.onPress(button -> save())
				.build());
		addRenderableWidget(new TexturedTextButton.Builder()
				.position(viewportX1 - 76, buttonY)
				.size(74, 20)
				.texture(STAT_BUTTONS)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(HairEditorUi.tr("gui.dragonminez.hair_editor.hair_salon"))
				.onPress(button -> openHairSalon())
				.build());
	}

	public void removed() {
		super.removed();
		HairEditSession.end();
	}

	public boolean isPauseScreen() {
		return false;
	}

	public void tick() {
		super.tick();
		if (statusTimer > 0) statusTimer--;
	}

	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		panorama.render(partialTick, 1.0f);
		renderCinematicBars(graphics);
		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));

		beginUiScale(graphics);
		layout();
		zoom = Mth.lerp(0.3f, zoom, targetZoom);

		renderViewport(graphics, mouseX, mouseY);
		outliner.render(graphics, uiMouseX, uiMouseY);
		inspector.render(graphics, uiMouseX, uiMouseY, scissorBox());
		renderTopBar(graphics, uiMouseX, uiMouseY);
		renderViewportOverlay(graphics, uiMouseX, uiMouseY);

		graphics.pose().pushPose();
		graphics.pose().translate(0.0f, 0.0f, 400.0f);
		super.render(graphics, uiMouseX, uiMouseY, partialTick);
		graphics.pose().popPose();

		colorPicker.render(graphics, font, uiMouseX, uiMouseY);
		if (confirmClose) renderConfirmClose(graphics, uiMouseX, uiMouseY);
		endUiScale(graphics);
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		double uiX = toUiX(mouseX);
		double uiY = toUiY(mouseY);
		if (confirmClose) return clickConfirmClose(uiX, uiY);
		if (colorPicker.isOpen()) return colorPicker.mouseClicked(uiX, uiY, button);
		if (super.mouseClicked(mouseX, mouseY, button)) return true;
		if (clickTopBar(uiX, uiY)) return true;
		if (outliner.mouseClicked(uiX, uiY, button)) return true;
		if (inspector.mouseClicked(uiX, uiY, button)) return true;
		if (clickViewButtons(uiX, uiY)) return true;
		if (!insideViewport(uiX, uiY)) return false;

		orbiting = false;
		panning = false;
		if (button == 0) {
			pressingViewport = true;
			pendingTool = hasShiftDown() ? HairViewportTool.Mode.GROW : (hasControlDown() ? HairViewportTool.Mode.CURVE : (hasAltDown() ? HairViewportTool.Mode.ROTATE : null));
		} else if (button == 1) {
			pressingSecondary = true;
		} else {
			panning = true;
		}
		pressX = mouseX;
		pressY = mouseY;
		lastX = uiX;
		lastY = uiY;
		return true;
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		double uiX = toUiX(mouseX);
		double uiY = toUiY(mouseY);
		if (confirmClose) return true;
		if (colorPicker.isOpen()) return colorPicker.mouseDragged(uiX, uiY);
		if (viewportTool.isActive()) {
			viewportTool.drag(mouseX, mouseY);
			return true;
		}
		if (inspector.mouseDragged(uiX, uiY)) return true;

		boolean moved = Math.hypot(mouseX - pressX, mouseY - pressY) > CLICK_DRAG_THRESHOLD;
		if (pressingViewport && pendingTool != null) {
			if (moved) {
				beginTool(pendingTool, pressX, pressY);
				pendingTool = null;
				if (viewportTool.isActive()) viewportTool.drag(mouseX, mouseY);
			}
			return true;
		}
		if (pressingSecondary && moved) {
			pressingSecondary = false;
			panning = true;
		}
		if (pressingViewport) {
			if (!orbiting && moved) orbiting = true;
			if (orbiting) {
				yaw -= (float) (uiX - lastX) * ORBIT_SPEED;
				pitch = Mth.clamp(pitch + (float) (uiY - lastY) * ORBIT_SPEED, -90.0f, 90.0f);
			}
			lastX = uiX;
			lastY = uiY;
			return true;
		}
		if (panning) {
			panX += (int) Math.round(uiX - lastX);
			panY += (int) Math.round(uiY - lastY);
			lastX = uiX;
			lastY = uiY;
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (colorPicker.isOpen()) colorPicker.mouseReleased();
		if (viewportTool.isActive()) {
			viewportTool.finish();
			pressingViewport = false;
			pendingTool = null;
			inspector.requestRebuild();
			return true;
		}
		inspector.mouseReleased();

		if (pressingViewport && !orbiting) pickSelection(mouseX, mouseY);
		if (pressingSecondary) editSegmentCount(mouseX, mouseY);
		pressingViewport = false;
		pressingSecondary = false;
		pendingTool = null;
		orbiting = false;
		panning = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		double uiX = toUiX(mouseX);
		double uiY = toUiY(mouseY);
		if (confirmClose || colorPicker.isOpen()) return true;
		if (inspector.mouseScrolled(uiX, uiY, delta)) return true;
		if (outliner.mouseScrolled(uiX, uiY, delta)) return true;
		if (insideViewport(uiX, uiY)) {
			targetZoom = Mth.clamp(targetZoom + (float) delta * ZOOM_STEP, MIN_ZOOM, MAX_ZOOM);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (confirmClose) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) confirmClose = false;
			return true;
		}
		if (colorPicker.isOpen() && colorPicker.keyPressed(keyCode)) return true;
		if (inspector.keyPressed(keyCode)) return true;

		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			if (viewportTool.isActive()) {
				viewportTool.cancel();
				inspector.requestRebuild();
			} else if (state.hasSelection()) {
				state.clearSelection();
				inspector.requestRebuild();
			} else {
				requestClose();
			}
			return true;
		}
		if (hasControlDown() && keyCode == GLFW.GLFW_KEY_Z) {
			if (hasShiftDown() ? state.redo() : state.undo()) inspector.requestRebuild();
			return true;
		}
		if (hasControlDown() && keyCode == GLFW.GLFW_KEY_Y) {
			if (state.redo()) inspector.requestRebuild();
			return true;
		}
		if (hasControlDown() && keyCode == GLFW.GLFW_KEY_C && state.hasSelection()) {
			state.copyStrand();
			showStatus(HairEditorUi.tr("gui.dragonminez.hair_editor.status.copied"), HairEditorUi.SUCCESS);
			return true;
		}
		if (hasControlDown() && keyCode == GLFW.GLFW_KEY_V && state.hasSelection()) {
			state.pasteStrand();
			inspector.requestRebuild();
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_DELETE && state.hasSelection()) {
			state.clearSelectedStrand();
			inspector.requestRebuild();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	public boolean charTyped(char character, int modifiers) {
		if (colorPicker.isOpen() && colorPicker.charTyped(character)) return true;
		if (inspector.charTyped(character)) return true;
		return super.charTyped(character, modifiers);
	}

	public void onClose() {
		requestClose();
	}

	private void layout() {
		int uiWidth = getUiWidth();
		int uiHeight = getUiHeight();
		int available = uiWidth - MARGIN * 2 - GAP * 2;
		int outlinerWidth = Mth.clamp(Math.round(available * 0.26f), 120, 150);
		int inspectorWidth = Mth.clamp(Math.round(available * 0.32f), 140, 190);
		int panelHeight = uiHeight - PANEL_TOP - MARGIN;

		outliner.setBounds(MARGIN, PANEL_TOP, outlinerWidth, panelHeight);
		inspector.setBounds(uiWidth - MARGIN - inspectorWidth, PANEL_TOP, inspectorWidth, panelHeight);
		viewportX0 = outliner.right() + GAP;
		viewportX1 = inspector.x() - GAP;
		viewportY0 = PANEL_TOP;
		viewportY1 = uiHeight - MARGIN;
	}

	private void renderViewport(GuiGraphics graphics, int rawMouseX, int rawMouseY) {
		LocalPlayer player = minecraft != null ? minecraft.player : null;
		if (player == null) return;

		int centerX = (viewportX0 + viewportX1) / 2 + panX;
		int baseY = (int) (getUiHeight() / 2.0f + 112 + (zoom - MIN_ZOOM) * 2.436f) + panY;

		float bodyYaw = player.yBodyRot;
		float bodyYawO = player.yBodyRotO;
		float entityYaw = player.getYRot();
		float entityPitch = player.getXRot();
		float entityPitchO = player.xRotO;
		float headYawO = player.yHeadRotO;
		float headYaw = player.yHeadRot;

		player.yBodyRot = yaw;
		player.yBodyRotO = yaw;
		player.setYRot(yaw);
		player.setXRot(pitch);
		player.xRotO = pitch;
		player.yHeadRot = yaw;
		player.yHeadRotO = yaw;

		graphics.enableScissor(toScreenCoord(viewportX0), toScreenCoord(viewportY0), toScreenCoord(viewportX1), toScreenCoord(viewportY1));
		graphics.pose().pushPose();
		graphics.pose().translate(0.0f, 0.0f, 150.0f);
		try (HairRenderContext.Scope scope = HairRenderContext.editorPreview(state)) {
			InventoryScreen.renderEntityInInventory(graphics, centerX, baseY, Math.round(zoom), new Quaternionf().rotateZ((float) Math.PI), new Quaternionf(), player);
		} finally {
			graphics.pose().popPose();
			graphics.disableScissor();
			player.yBodyRot = bodyYaw;
			player.yBodyRotO = bodyYawO;
			player.setYRot(entityYaw);
			player.setXRot(entityPitch);
			player.xRotO = entityPitchO;
			player.yHeadRotO = headYawO;
			player.yHeadRot = headYaw;
		}

		updateHover(rawMouseX, rawMouseY);
	}

	private void updateHover(double rawMouseX, double rawMouseY) {
		if (viewportTool.isActive() || !insideViewport(toUiX(rawMouseX), toUiY(rawMouseY)) || colorPicker.isOpen()) {
			state.setHover(null, -1);
			return;
		}
		HairPickRecorder.Hit hit = state.pickRecorder().pick(rawMouseX, rawMouseY, (face, index) -> !state.isLocked(face, index));
		state.setHover(hit != null ? hit.face() : null, hit != null ? hit.index() : -1);
	}

	private void pickSelection(double rawMouseX, double rawMouseY) {
		if (!insideViewport(toUiX(rawMouseX), toUiY(rawMouseY))) return;
		HairPickRecorder.Hit hit = state.pickRecorder().pick(rawMouseX, rawMouseY, (face, index) -> !state.isLocked(face, index));
		if (hit == null) {
			if (state.hasSelection()) HairEditorSounds.deselect();
			state.clearSelection();
		} else if (state.selectedFace() == hit.face() && state.selectedIndex() == hit.index() && hasAltDown()) {
			state.selectSegment(hit.segment());
			HairEditorSounds.select();
		} else {
			state.select(hit.face(), hit.index());
			if (hasAltDown()) state.selectSegment(hit.segment());
			HairEditorSounds.select();
		}
		inspector.requestRebuild();
	}

	private void beginTool(HairViewportTool.Mode mode, double rawMouseX, double rawMouseY) {
		HairPickRecorder recorder = state.pickRecorder();
		HairPickRecorder.Hit hit = recorder.pick(rawMouseX, rawMouseY, (face, index) -> !state.isLocked(face, index));
		if (hit != null && (hit.face() != state.selectedFace() || hit.index() != state.selectedIndex())) {
			state.select(hit.face(), hit.index());
			HairEditorSounds.select();
		}
		if (!state.hasSelection() || state.isLocked(state.selectedFace(), state.selectedIndex())) return;
		if (viewportTool.begin(recorder, state.selectedFace(), state.selectedIndex(), mode, rawMouseX, rawMouseY)) inspector.requestRebuild();
	}

	private void editSegmentCount(double rawMouseX, double rawMouseY) {
		if (!insideViewport(toUiX(rawMouseX), toUiY(rawMouseY)) || !state.hasSelection()) return;
		HairPickRecorder.Hit hit = state.pickRecorder().pick(rawMouseX, rawMouseY, (face, index) -> !state.isLocked(face, index));
		if (hit == null || hit.face() != state.selectedFace() || hit.index() != state.selectedIndex()) return;
		HairStrand strand = state.selectedStrand();
		if (strand == null || !strand.isVisible()) return;
		HairStrand preview = strand.copy();
		boolean changed = hasShiftDown()
				? HairStrandSizing.removeSegment(preview)
				: HairStrandSizing.addSegment(preview, state.limits().getMaxSegments(), state.limits().getMaxLength(state.slot()));
		if (!changed) {
			HairEditorSounds.limit();
			return;
		}
		state.recordStep();
		state.modifySelected(target -> target.copyFrom(preview));
		HairEditorSounds.click();
		inspector.requestRebuild();
	}

	private void renderTopBar(GuiGraphics graphics, int mouseX, int mouseY) {
		int top = 6;
		HairEditorUi.button(graphics, font, HairEditorUi.tr("gui.dragonminez.hair_editor.cancel"), MARGIN, top, 60, TOP_BAR_HEIGHT, HairEditorUi.inside(mouseX, mouseY, MARGIN, top, 60, TOP_BAR_HEIGHT), false, true);
		Component title = state.isDirty() ? HairEditorUi.txt(getTitle().getString() + " *") : getTitle();
		TextUtil.drawCenteredStringWithBorder(graphics, font, title, getUiWidth() / 2, top + 4, HairEditorUi.TITLE);

		int x = getUiWidth() - MARGIN;
		for (TopBarControl control : TopBarControl.values()) {
			int width = control.width;
			x -= width;
			boolean active = switch (control) {
				case HAIR_BASE -> state.isHairBase();
				case PHYSICS -> state.physicsEnabled();
				case MIRROR -> state.isMirror();
				default -> false;
			};
			boolean enabled = switch (control) {
				case UNDO -> state.canUndo();
				case REDO -> state.canRedo();
				default -> true;
			};
			HairEditorUi.button(graphics, font, HairEditorUi.tr(control.key), x, top, width, TOP_BAR_HEIGHT, HairEditorUi.inside(mouseX, mouseY, x, top, width, TOP_BAR_HEIGHT), active, enabled);
			x -= 3;
		}
	}

	private boolean clickTopBar(double mouseX, double mouseY) {
		int top = 6;
		if (HairEditorUi.inside(mouseX, mouseY, MARGIN, top, 60, TOP_BAR_HEIGHT)) {
			HairEditorSounds.click();
			requestClose();
			return true;
		}
		int x = getUiWidth() - MARGIN;
		for (TopBarControl control : TopBarControl.values()) {
			x -= control.width;
			if (HairEditorUi.inside(mouseX, mouseY, x, top, control.width, TOP_BAR_HEIGHT)) {
				switch (control) {
					case UNDO -> {
						if (state.undo()) {
							HairEditorSounds.click();
							inspector.requestRebuild();
						}
					}
					case REDO -> {
						if (state.redo()) {
							HairEditorSounds.click();
							inspector.requestRebuild();
						}
					}
					case MIRROR -> {
						state.setMirror(!state.isMirror());
						HairEditorSounds.toggle(state.isMirror());
					}
					case PHYSICS -> {
						state.setPhysics(!state.physicsEnabled());
						HairEditorSounds.toggle(state.physicsEnabled());
					}
					case HAIR_BASE -> {
						state.setHairBase(!state.isHairBase());
						HairEditSession.setRenderHairBase(state.isHairBase());
						HairEditorSounds.toggle(state.isHairBase());
					}
				}
				return true;
			}
			x -= 3;
		}
		return false;
	}

	private void renderViewportOverlay(GuiGraphics graphics, int mouseX, int mouseY) {
		int x = viewportX0 + 2;
		int y = viewportY0 + 2;
		for (ViewPreset preset : ViewPreset.values()) {
			int width = Math.max(26, font.width(HairEditorUi.tr(preset.key)) + 8);
			HairEditorUi.button(graphics, font, HairEditorUi.tr(preset.key), x, y, width, 13, HairEditorUi.inside(mouseX, mouseY, x, y, width, 13), false, true);
			x += width + 2;
		}
		if (statusTimer > 0) {
			TextUtil.drawCenteredStringWithBorder(graphics, font, statusText, (viewportX0 + viewportX1) / 2, viewportY0 + 20, statusColor);
		}
		Component hint = HairEditorUi.tr(viewportTool.isActive() ? "gui.dragonminez.hair_editor.hint.tool" : "gui.dragonminez.hair_editor.hint.viewport");
		String hintText = HairEditorUi.trimToWidth(font, hint.getString(), viewportX1 - viewportX0 - 160);
		graphics.drawString(font, HairEditorUi.txt(hintText), viewportX0 + 3, viewportY1 - 11, HairEditorUi.MUTED, true);
	}

	private boolean clickViewButtons(double mouseX, double mouseY) {
		int x = viewportX0 + 2;
		int y = viewportY0 + 2;
		for (ViewPreset preset : ViewPreset.values()) {
			int width = Math.max(26, font.width(HairEditorUi.tr(preset.key)) + 8);
			if (HairEditorUi.inside(mouseX, mouseY, x, y, width, 13)) {
				HairEditorSounds.click();
				yaw = preset.yaw;
				pitch = 0.0f;
				if (preset == ViewPreset.RESET) {
					panX = 0;
					panY = 0;
					targetZoom = 150.0f;
				}
				return true;
			}
			x += width + 2;
		}
		return false;
	}

	private void openColorPicker(HairInspectorRows.ColorRow row, int anchorX, int anchorY) {
		colorPicker.open(anchorX, anchorY, getUiWidth(), getUiHeight(), row.currentHex(), row.fallbackHex(), row::set);
	}

	private void handleCodeAction(HairOutlinerPanel.CodeAction action) {
		Minecraft mc = Minecraft.getInstance();
		switch (action) {
			case COPY_STYLE -> {
				mc.keyboardHandler.setClipboard(HairCodec.toCode(state.style()));
				showStatus(HairEditorUi.tr("gui.dragonminez.hair_editor.status.copied"), HairEditorUi.SUCCESS);
			}
			case COPY_FULL -> {
				mc.keyboardHandler.setClipboard(HairCodec.toFullSetCode(state.styles()));
				showStatus(HairEditorUi.tr("gui.dragonminez.hair_editor.status.copied"), HairEditorUi.SUCCESS);
			}
			case PASTE_STYLE -> {
				String code = mc.keyboardHandler.getClipboard();
				CustomHair imported = HairCodec.isFullSetCode(code) ? fullSetSlot(code) : HairCodec.fromCode(code);
				if (imported == null) {
					showStatus(HairEditorUi.tr("gui.dragonminez.hair_editor.status.invalid"), HairEditorUi.WARNING);
					return;
				}
				HairSanitizer.sanitize(imported, state.slot(), state.limits());
				state.replaceCurrentStyle(imported);
				inspector.requestRebuild();
				showStatus(HairEditorUi.tr("gui.dragonminez.hair_editor.status.imported"), HairEditorUi.SUCCESS);
			}
			case PASTE_FULL -> {
				String code = mc.keyboardHandler.getClipboard();
				EnumMap<HairStyleSlot, CustomHair> imported = HairCodec.fromFullSetCode(code);
				if (imported == null) {
					showStatus(HairEditorUi.tr("gui.dragonminez.hair_editor.status.invalid"), HairEditorUi.WARNING);
					return;
				}
				for (HairStyleSlot slot : HairStyleSlot.values()) HairSanitizer.sanitize(imported.get(slot), slot, state.limits());
				state.replaceStyles(imported);
				inspector.requestRebuild();
				showStatus(HairEditorUi.tr("gui.dragonminez.hair_editor.status.imported"), HairEditorUi.SUCCESS);
			}
		}
	}

	private CustomHair fullSetSlot(String code) {
		EnumMap<HairStyleSlot, CustomHair> set = HairCodec.fromFullSetCode(code);
		return set != null ? set.get(state.slot()) : null;
	}

	private void showStatus(Component text, int color) {
		statusText = text;
		statusColor = color;
		statusTimer = STATUS_TICKS;
	}

	private void save() {
		inspector.commitTextEdit();
		character.setHairId(0);
		for (HairStyleSlot slot : HairStyleSlot.values()) character.setHairStyle(slot, state.styles().get(slot).copy());
		if (state.hairBaseChanged()) {
			character.setRenderHairBase(state.isHairBase());
			NetworkHandler.sendToServer(new StatsSyncC2S(character));
		}
		for (HairStyleSlot slot : HairStyleSlot.values()) {
			if (state.isSlotDirty(slot)) NetworkHandler.sendToServer(new UpdateCustomHairC2S(slot, state.styles().get(slot)));
		}
		close();
	}

	private void requestClose() {
		if (viewportTool.isActive()) viewportTool.cancel();
		if (state.isDirty()) {
			confirmClose = true;
			return;
		}
		close();
	}

	private void close() {
		if (closing || minecraft == null) return;
		closing = true;
		HairEditSession.end();
		minecraft.setScreen(previousScreen);
	}

	private void openHairSalon() {
		if (minecraft == null) return;
		minecraft.setScreen(new ConfirmLinkScreen(confirmed -> {
			if (confirmed) Util.getPlatform().openUri(HAIR_SALON_URL);
			minecraft.setScreen(this);
		}, HAIR_SALON_URL, true));
	}

	private void renderConfirmClose(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.pose().pushPose();
		graphics.pose().translate(0.0f, 0.0f, 500.0f);
		graphics.fill(0, 0, getUiWidth(), getUiHeight(), 0x90000000);
		int width = 220;
		int height = 64;
		int x = (getUiWidth() - width) / 2;
		int y = (getUiHeight() - height) / 2;
		HairEditorUi.smallPanel(graphics, x, y, width, height);
		TextUtil.drawCenteredStringWithBorder(graphics, font, HairEditorUi.tr("gui.dragonminez.hair_editor.confirm_close"), x + width / 2, y + 12, HairEditorUi.TITLE);
		int buttonWidth = (width - 24) / 3;
		ConfirmChoice[] choices = ConfirmChoice.values();
		for (int i = 0; i < choices.length; i++) {
			int buttonX = x + 6 + i * (buttonWidth + 6);
			HairEditorUi.button(graphics, font, HairEditorUi.tr(choices[i].key), buttonX, y + 38, buttonWidth, 16, HairEditorUi.inside(mouseX, mouseY, buttonX, y + 38, buttonWidth, 16), false, true);
		}
		graphics.pose().popPose();
	}

	private boolean clickConfirmClose(double mouseX, double mouseY) {
		int width = 220;
		int height = 64;
		int x = (getUiWidth() - width) / 2;
		int y = (getUiHeight() - height) / 2;
		int buttonWidth = (width - 24) / 3;
		ConfirmChoice[] choices = ConfirmChoice.values();
		for (int i = 0; i < choices.length; i++) {
			int buttonX = x + 6 + i * (buttonWidth + 6);
			if (!HairEditorUi.inside(mouseX, mouseY, buttonX, y + 38, buttonWidth, 16)) continue;
			HairEditorSounds.click();
			confirmClose = false;
			switch (choices[i]) {
				case SAVE -> save();
				case DISCARD -> close();
				case KEEP -> {
				}
			}
			return true;
		}
		return true;
	}

	private boolean insideViewport(double uiX, double uiY) {
		return uiX >= viewportX0 && uiX < viewportX1 && uiY >= viewportY0 && uiY < viewportY1;
	}

	private HairInspectorPanel.ScissorBox scissorBox() {
		return new HairInspectorPanel.ScissorBox() {
			public void enable(GuiGraphics graphics, int x0, int y0, int x1, int y1) {
				graphics.enableScissor(toScreenCoord(x0), toScreenCoord(y0), toScreenCoord(x1), toScreenCoord(y1));
			}

			public void disable(GuiGraphics graphics) {
				graphics.disableScissor();
			}
		};
	}

	private void renderCinematicBars(GuiGraphics graphics) {
		int barHeight = (int) (height * 0.12);
		graphics.fill(0, 0, width, barHeight - 60, 0xFF000000);
		graphics.fillGradient(0, barHeight - 60, width, barHeight, 0xFF000000, 0x00000000);
		graphics.fillGradient(0, height - barHeight, width, height - barHeight + 60, 0x00000000, 0xFF000000);
		graphics.fill(0, height - barHeight + 60, width, height, 0xFF000000);
	}

	private enum TopBarControl {
		HAIR_BASE("gui.dragonminez.hair_editor.hairbase", 58),
		PHYSICS("gui.dragonminez.hair_editor.physics", 52),
		MIRROR("gui.dragonminez.hair_editor.mirror", 48),
		REDO("gui.dragonminez.hair_editor.redo", 36),
		UNDO("gui.dragonminez.hair_editor.undo", 36);

		private final String key;
		private final int width;

		TopBarControl(String key, int width) {
			this.key = key;
			this.width = width;
		}
	}

	private enum ViewPreset {
		FRONT("gui.dragonminez.hair_editor.view.front", 180.0f),
		BACK("gui.dragonminez.hair_editor.view.back", 0.0f),
		LEFT("gui.dragonminez.hair_editor.view.left", 90.0f),
		RIGHT("gui.dragonminez.hair_editor.view.right", 270.0f),
		RESET("gui.dragonminez.hair_editor.view.reset", 180.0f);

		private final String key;
		private final float yaw;

		ViewPreset(String key, float yaw) {
			this.key = key;
			this.yaw = yaw;
		}
	}

	private enum ConfirmChoice {
		SAVE("gui.dragonminez.hair_editor.save"),
		DISCARD("gui.dragonminez.hair_editor.discard"),
		KEEP("gui.dragonminez.hair_editor.keep_editing");

		private final String key;

		ConfirmChoice(String key) {
			this.key = key;
		}
	}
}
