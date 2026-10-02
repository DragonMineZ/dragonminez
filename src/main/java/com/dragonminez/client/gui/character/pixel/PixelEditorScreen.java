package com.dragonminez.client.gui.character.pixel;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.ColorSlider;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.character.util.FormPreview;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.gui.hair.HairEditorSounds;
import com.dragonminez.client.gui.hair.HairEditorUi;
import com.dragonminez.client.render.hair.HairEntityState;
import com.dragonminez.client.render.hair.HairPickRecorder;
import com.dragonminez.client.render.hair.HairRenderContext;
import com.dragonminez.client.render.layer.DMZSkinLayer;
import com.dragonminez.client.render.util.SkinPaintContext;
import com.dragonminez.client.render.util.SkinPixelTextures;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.SkinGathererProvider;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.client.util.TextureCounter;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairPresets;
import com.dragonminez.common.hair.HairStyleSlot;
import com.dragonminez.common.network.C2S.UpdateSkinPixelsC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.character.SkinPixels;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class PixelEditorScreen extends ScaledScreen {
	public enum Mode { EYES, HAIR, TATTOO }

	private enum TopButton { UNDO, REDO, CLEAR, LAYER, HAIR, ERASER, SAVE, CANCEL }

	private enum Tool { BRUSH, AXIS_LINE }

	private record Placement(TopButton button, int x, int width) {}

	private static final ResourceLocation BUTTONS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");
	private static final ResourceLocation PANORAMA = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/background/roshi");
	private static final ResourceLocation PICK_LOCATION = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "pixels/editor/pick");
	private static final ResourceLocation HIGHLIGHT_LOCATION = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "pixels/editor/highlight");
	private static final CustomHair EMPTY_HAIR = new CustomHair();

	private static final int MARGIN = 8;
	private static final int GAP = 6;
	private static final int TOP_BAR_Y = 8;
	private static final int TOP_BAR_HEIGHT = 16;
	private static final int PANEL_TOP = 30;
	private static final int LEFT_WIDTH = 134;
	private static final int MAX_HISTORY = 10;
	private static final int PICK_RES = 256;
	private static final int HIGHLIGHT_RES = 512;
	private static final int CLICK_DRAG_THRESHOLD = 3;
	private static final float MIN_ZOOM = 60.0f;
	private static final float MAX_ZOOM = 700.0f;
	private static final float ZOOM_FACTOR = 1.12f;
	private static final float ORBIT_SPEED = 1.0f;
	private static final int CONNECT_DISTANCE = 8;
	private static final int VIEW_BACKGROUND = 0xFF15191E;
	private static final int[] EYE_MARK_COLORS = {0, 0xE08A2E, 0x6EDCFF, 0xE84FE0};
	private static final int HAIR_MARK_COLOR = 0x5BE04F;
	private static final float[] WHITE_RGB = {1.0f, 1.0f, 1.0f};
	private static final float MARK_ALPHA = 0.85f;
	private static final int FRAME_OUTER = 0xFFFFFFFF;
	private static final int FRAME_INNER = 0xFF000000;
	private static final ByteBuffer PICK_BUFFER = BufferUtils.createByteBuffer(4);

	private final PanoramaRenderer panorama = new PanoramaRenderer(new CubeMap(PANORAMA));
	private final Screen previousScreen;
	private final Character character;
	private final Mode mode;
	private final SkinPixels pixels;
	private final SkinPixels backup;
	private final PixelEditHistory history = new PixelEditHistory(MAX_HISTORY);
	private final FormPreview formPreview;
	private final PaintHairPreview hairPreview = new PaintHairPreview();
	private final List<Placement> topBar = new ArrayList<>();
	private final boolean layered;

	private int step;
	private boolean overlayLayer;
	private boolean showHair;
	private boolean eraser;
	private int viewX0;
	private int viewY0;
	private int viewX1;
	private int viewY1;
	private float targetZoom;
	private float zoom;
	private float yaw = 180.0f;
	private float pitch;
	private int panX;
	private int panY;
	private boolean viewInitialized;

	private double screenMouseX;
	private double screenMouseY;
	private int hoverX = -1;
	private int hoverY = -1;
	private boolean hoverValid;
	private int pressHoverX;
	private int pressHoverY;
	private boolean pressHoverValid;
	private boolean pressingViewport;
	private boolean pressingSecondary;
	private boolean orbiting;
	private boolean panning;
	private double pressX;
	private double pressY;
	private double lastX;
	private double lastY;

	private boolean painting;
	private boolean erasing;
	private boolean strokeMoved;
	private Tool strokeTool;
	private int strokeStartX;
	private int strokeStartY;
	private int lastPaintX;
	private int lastPaintY;
	private PixelEditHistory.Snapshot strokeBefore;
	private int anchorX = -1;
	private int anchorY = -1;
	private boolean confirmClose;

	private ResourceLocation baseTexture;
	private DynamicTexture pickTexture;
	private DynamicTexture highlightTexture;
	private int highlightFactor = 1;
	private int highlightCellRes = SkinPixels.SKIN_SIZE;
	private final List<Integer> highlightCells = new ArrayList<>();

	private CustomTextureButton prevStepButton;
	private CustomTextureButton nextStepButton;
	private CustomTextureButton prevFormButton;
	private CustomTextureButton nextFormButton;
	private ColorSlider hueSlider;
	private ColorSlider saturationSlider;
	private ColorSlider valueSlider;
	private EditBox hexField;
	private boolean updatingFromCode;
	private int paintColor = 0xFF000000;

	public PixelEditorScreen(Screen previousScreen, Character character, Mode mode) {
		super(HairEditorUi.tr("gui.dragonminez.pixel_editor.title." + mode.name().toLowerCase()));
		this.previousScreen = previousScreen;
		this.character = character;
		this.mode = mode;
		this.pixels = character.getSkinPixels();
		this.backup = pixels.copy();
		this.formPreview = new FormPreview(character.getRaceName());
		this.layered = SkinGathererProvider.rendersPlayerSkin(character) || SkinGathererProvider.modelFamily(TextureCounter.faceRaceKey(character)).equals("human");
		this.showHair = mode != Mode.HAIR;
		if (mode == Mode.TATTOO) pixels.tattooForEdit(pixels.getTattooSize() > 0 ? pixels.getTattooSize() : SkinPixels.TATTOO_SIZES[0]);
	}

	private final class PaintHairPreview implements HairRenderContext.Preview {
		private final HairEntityState simulation = new HairEntityState();

		@Override
		public CustomHair style() {
			CustomHair style = character.getHairId() > 0 ? HairPresets.get(character.getHairId(), HairStyleSlot.BASE) : character.getOwnHairStyle(HairStyleSlot.BASE);
			return style != null ? style : EMPTY_HAIR;
		}

		@Override
		public HairStyleSlot slot() {
			return HairStyleSlot.BASE;
		}

		@Override
		public boolean physicsEnabled() {
			return false;
		}

		@Override
		public String globalColor() {
			return character.getHairColor();
		}

		@Override
		public boolean bodyHidden() {
			return false;
		}

		@Override
		public HairEntityState simulationState() {
			return simulation;
		}

		@Override
		public float highlight(CustomHair.HairFace face, int index, int segment) {
			return 0.0f;
		}

		@Override
		public boolean isHidden(CustomHair.HairFace face, int index) {
			return false;
		}

		@Override
		public HairPickRecorder pickRecorder() {
			return null;
		}
	}

	@Override
	protected int getMinGuiWidth() {
		return 420;
	}

	@Override
	protected void init() {
		super.init();
		clearWidgets();
		layout();
		gatherBaseTexture();
		ensurePickTexture();

		int arrowY = previewRowY() + 1;
		prevFormButton = addRenderableWidget(arrowButton(MARGIN + 6, arrowY, true, b -> formPreview.cycle(-1)));
		nextFormButton = addRenderableWidget(arrowButton(MARGIN + LEFT_WIDTH - 16, arrowY, false, b -> formPreview.cycle(1)));

		if (mode != Mode.TATTOO) {
			int stepY = PANEL_TOP + 28;
			prevStepButton = addRenderableWidget(arrowButton(MARGIN + 6, stepY, true, b -> setStep(step - 1)));
			nextStepButton = addRenderableWidget(arrowButton(MARGIN + LEFT_WIDTH - 16, stepY, false, b -> setStep(step + 1)));
			updateStepButtons();
		} else {
			initColorPicker();
		}
	}

	private void layout() {
		viewX0 = MARGIN + LEFT_WIDTH + GAP;
		viewX1 = getUiWidth() - MARGIN;
		viewY0 = PANEL_TOP;
		viewY1 = getUiHeight() - MARGIN;
		if (!viewInitialized) {
			viewInitialized = true;
			float height = viewY1 - viewY0 - 16;
			targetZoom = Mth.clamp(mode == Mode.EYES ? height * 1.35f : height / 2.0f, MIN_ZOOM, MAX_ZOOM);
			zoom = targetZoom;
		}
	}

	private int previewRowY() {
		return getUiHeight() - MARGIN - 8 - 24;
	}

	private void gatherBaseTexture() {
		baseTexture = null;
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return;
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> SkinGathererProvider.INSTANCE.gatherBodyLayers(player, stats, 0.0f, (texture, color) -> {
			if (baseTexture == null) baseTexture = texture;
		}));
	}

	private void ensurePickTexture() {
		if (pickTexture != null) return;
		NativeImage alphaSource = layered ? SkinPixelTextures.download(baseTexture, SkinPixels.SKIN_SIZE) : null;
		NativeImage image = new NativeImage(NativeImage.Format.RGBA, PICK_RES, PICK_RES, false);
		int perSkin = PICK_RES / SkinPixels.SKIN_SIZE;
		for (int v = 0; v < PICK_RES; v++) {
			for (int u = 0; u < PICK_RES; u++) {
				int alpha = 255;
				if (layered && SkinAtlas.isOverlay(u / perSkin, v / perSkin)) {
					alpha = alphaSource != null ? alphaSource.getPixelRGBA(u / perSkin, v / perSkin) >>> 24 : 255;
					if (alpha < 128) alpha = 0;
				}
				image.setPixelRGBA(u, v, (alpha << 24) | (checksum(u, v) << 16) | (v << 8) | u);
			}
		}
		if (alphaSource != null) alphaSource.close();
		pickTexture = new DynamicTexture(image);
		Minecraft.getInstance().getTextureManager().register(PICK_LOCATION, pickTexture);
	}

	private static int checksum(int u, int v) {
		return (u * 7 + v * 13 + 0x5A) & 0xFF;
	}

	private void releaseTextures() {
		var manager = Minecraft.getInstance().getTextureManager();
		if (pickTexture != null) {
			manager.release(PICK_LOCATION);
			pickTexture = null;
		}
		if (highlightTexture != null) {
			manager.release(HIGHLIGHT_LOCATION);
			highlightTexture = null;
			highlightCells.clear();
		}
	}

	private CustomTextureButton arrowButton(int x, int y, boolean left, CustomTextureButton.OnPress onPress) {
		return new CustomTextureButton.Builder()
				.position(x, y)
				.size(10, 15)
				.texture(BUTTONS)
				.textureCoords(left ? 32 : 20, 0, left ? 32 : 20, 14)
				.textureSize(8, 14)
				.message(Component.empty())
				.onPress(onPress)
				.build();
	}

	private void initColorPicker() {
		int sliderX = MARGIN + 8;
		int sliderY = PANEL_TOP + 44;
		int sliderWidth = 78;
		float[] hsv = ColorUtils.hexToHsv(String.format("#%06X", paintColor & 0xFFFFFF));

		hueSlider = new ColorSlider.Builder()
				.position(sliderX, sliderY)
				.size(sliderWidth, 10)
				.range(0, 360)
				.value(Math.round(hsv[0]))
				.message(tr("gui.dragonminez.customization.hue"))
				.gradient(ColorSlider.Gradient.HUE)
				.onValueChange(v -> updateColorFromSliders())
				.build();
		saturationSlider = new ColorSlider.Builder()
				.position(sliderX, sliderY + 12)
				.size(sliderWidth, 10)
				.range(100, 0)
				.value(Math.round(hsv[1]))
				.message(tr("gui.dragonminez.customization.saturation"))
				.gradient(ColorSlider.Gradient.SATURATION)
				.onValueChange(v -> updateColorFromSliders())
				.build();
		valueSlider = new ColorSlider.Builder()
				.position(sliderX, sliderY + 24)
				.size(sliderWidth, 10)
				.range(100, 0)
				.value(Math.round(hsv[2]))
				.message(tr("gui.dragonminez.customization.value"))
				.gradient(ColorSlider.Gradient.VALUE)
				.onValueChange(v -> updateColorFromSliders())
				.build();
		saturationSlider.setCurrentHue(hsv[0]);
		valueSlider.setCurrentHue(hsv[0]);
		valueSlider.setCurrentSaturation(hsv[1]);
		addRenderableWidget(hueSlider);
		addRenderableWidget(saturationSlider);
		addRenderableWidget(valueSlider);

		hexField = new EditBox(this.font, sliderX, sliderY + 38, sliderWidth, 12, tr("gui.dragonminez.pixel_editor.color"));
		hexField.setMaxLength(7);
		hexField.setValue(String.format("#%06X", paintColor & 0xFFFFFF));
		hexField.setResponder(this::onHexFieldChange);
		addRenderableWidget(hexField);
	}

	private void updateColorFromSliders() {
		if (hueSlider == null) return;
		float h = hueSlider.getValue();
		float s = saturationSlider.getValue();
		float v = valueSlider.getValue();
		saturationSlider.setCurrentHue(h);
		valueSlider.setCurrentHue(h);
		valueSlider.setCurrentSaturation(s);
		String hex = ColorUtils.hsvToHex(h, s, v);
		paintColor = 0xFF000000 | ColorUtils.hexToInt(hex);
		updatingFromCode = true;
		if (hexField != null && !hexField.isFocused()) hexField.setValue(hex);
		updatingFromCode = false;
	}

	private void onHexFieldChange(String hex) {
		if (updatingFromCode) return;
		if (hex.startsWith("#")) hex = hex.substring(1);
		if (hex.length() != 6) return;
		try {
			float[] hsv = ColorUtils.hexToHsv("#" + hex);
			updatingFromCode = true;
			hueSlider.setValue(Math.round(hsv[0]));
			saturationSlider.setValue(Math.round(hsv[1]));
			valueSlider.setValue(Math.round(hsv[2]));
			saturationSlider.setCurrentHue(hsv[0]);
			valueSlider.setCurrentHue(hsv[0]);
			valueSlider.setCurrentSaturation(hsv[1]);
			paintColor = 0xFF000000 | ColorUtils.hexToInt("#" + hex);
		} catch (Exception ignored) {
		} finally {
			updatingFromCode = false;
		}
	}

	private int stepCount() {
		return mode == Mode.EYES ? 3 : (mode == Mode.HAIR ? 2 : 1);
	}

	private void setStep(int newStep) {
		int clamped = Mth.clamp(newStep, 0, stepCount() - 1);
		if (clamped == step) return;
		step = clamped;
		if (mode == Mode.HAIR) overlayLayer = step == 1;
		anchorX = -1;
		anchorY = -1;
		updateStepButtons();
		HairEditorSounds.select();
	}

	private void updateStepButtons() {
		if (prevStepButton != null) prevStepButton.visible = step > 0;
		if (nextStepButton != null) nextStepButton.visible = step < stepCount() - 1;
	}

	private byte marker() {
		if (mode == Mode.HAIR) return SkinPixels.MARK_HAIR;
		return switch (step) {
			case 1 -> SkinPixels.MARK_SCLERA;
			case 2 -> SkinPixels.MARK_IRIS;
			default -> SkinPixels.MARK_BROW;
		};
	}

	private int markerColor(byte mark) {
		if (mode == Mode.HAIR) return HAIR_MARK_COLOR;
		return mark > 0 && mark < EYE_MARK_COLORS.length ? EYE_MARK_COLORS[mark] : 0xFFFFFF;
	}

	private static float[] rgb(int color) {
		return new float[]{((color >> 16) & 0xFF) / 255.0f, ((color >> 8) & 0xFF) / 255.0f, (color & 0xFF) / 255.0f};
	}

	private int resolution() {
		return mode == Mode.TATTOO ? Math.max(SkinPixels.TATTOO_SIZES[0], pixels.getTattooSize()) : SkinPixels.SKIN_SIZE;
	}

	private byte[] marks() {
		return mode == Mode.EYES ? pixels.eyeMarksForEdit() : pixels.hairMarksForEdit();
	}

	private boolean insideCanvas(int x, int y) {
		int res = resolution();
		return x >= 0 && y >= 0 && x < res && y < res;
	}

	private boolean insideViewport(double mouseX, double mouseY) {
		return mouseX >= viewX0 && mouseX < viewX1 && mouseY >= viewY0 && mouseY < viewY1;
	}

	private boolean canPaint(int x, int y) {
		if (!insideCanvas(x, y)) return false;
		if (!layered) return true;
		if (mode == Mode.TATTOO) {
			int scale = resolution() / SkinPixels.SKIN_SIZE;
			return SkinAtlas.isBase(x / scale, y / scale);
		}
		return overlayLayer ? SkinAtlas.isOverlay(x, y) : SkinAtlas.isBase(x, y);
	}

	private boolean shiftDown() {
		return hasShiftDown();
	}

	private PixelEditHistory.Snapshot snapshot() {
		if (mode == Mode.TATTOO) {
			int size = resolution();
			return new PixelEditHistory.Snapshot(size, pixels.tattooForEdit(size).clone());
		}
		return new PixelEditHistory.Snapshot(SkinPixels.SKIN_SIZE, marks().clone());
	}

	private void restore(PixelEditHistory.Snapshot snapshot) {
		if (snapshot == null) return;
		if (mode == Mode.TATTOO) {
			pixels.setTattoo(snapshot.size(), snapshot.data().clone());
			return;
		}
		byte[] target = marks();
		System.arraycopy(snapshot.data(), 0, target, 0, Math.min(target.length, snapshot.data().length));
		pixels.markChanged();
	}

	private boolean differsFrom(PixelEditHistory.Snapshot snapshot) {
		if (snapshot == null) return false;
		if (mode == Mode.TATTOO) return snapshot.size() != pixels.getTattooSize() || !Arrays.equals(snapshot.data(), pixels.getTattoo());
		return !Arrays.equals(snapshot.data(), marks());
	}

	private void paintTexel(int x, int y) {
		if (!canPaint(x, y)) return;
		if (mode == Mode.TATTOO) {
			int size = resolution();
			byte[] rgba = pixels.tattooForEdit(size);
			int index = (y * size + x) * 4;
			if (erasing) {
				rgba[index] = 0;
				rgba[index + 1] = 0;
				rgba[index + 2] = 0;
				rgba[index + 3] = 0;
			} else {
				rgba[index] = (byte) ((paintColor >> 16) & 0xFF);
				rgba[index + 1] = (byte) ((paintColor >> 8) & 0xFF);
				rgba[index + 2] = (byte) (paintColor & 0xFF);
				rgba[index + 3] = (byte) 0xFF;
			}
		} else {
			marks()[y * SkinPixels.SKIN_SIZE + x] = erasing ? SkinPixels.MARK_NONE : marker();
		}
		pixels.markChanged();
	}

	private void paintLine(int x0, int y0, int x1, int y1) {
		int dx = Math.abs(x1 - x0);
		int dy = -Math.abs(y1 - y0);
		int sx = x0 < x1 ? 1 : -1;
		int sy = y0 < y1 ? 1 : -1;
		int err = dx + dy;
		int x = x0;
		int y = y0;
		while (true) {
			paintTexel(x, y);
			if (x == x1 && y == y1) break;
			int e2 = 2 * err;
			if (e2 >= dy) {
				err += dy;
				x += sx;
			}
			if (e2 <= dx) {
				err += dx;
				y += sy;
			}
		}
	}

	private void paintAxisLine(int x0, int y0, int x1, int y1) {
		if (Math.abs(x1 - x0) >= Math.abs(y1 - y0)) paintLine(x0, y0, x1, y0);
		else paintLine(x0, y0, x0, y1);
	}

	private void beginStroke(Tool tool) {
		if (!pressHoverValid) return;
		painting = true;
		strokeTool = tool;
		strokeMoved = false;
		erasing = eraser || hasAltDown();
		strokeStartX = pressHoverX;
		strokeStartY = pressHoverY;
		lastPaintX = pressHoverX;
		lastPaintY = pressHoverY;
		strokeBefore = snapshot();
		if (tool == Tool.BRUSH) paintTexel(pressHoverX, pressHoverY);
	}

	private void updateStroke() {
		if (!painting || !hoverValid) return;
		int x = hoverX;
		int y = hoverY;
		if (x == lastPaintX && y == lastPaintY) return;
		strokeMoved = true;
		if (strokeTool == Tool.AXIS_LINE) {
			restore(strokeBefore);
			paintAxisLine(strokeStartX, strokeStartY, x, y);
		} else if (Math.max(Math.abs(x - lastPaintX), Math.abs(y - lastPaintY)) <= CONNECT_DISTANCE) {
			paintLine(lastPaintX, lastPaintY, x, y);
		} else {
			paintTexel(x, y);
		}
		lastPaintX = x;
		lastPaintY = y;
	}

	private void endStroke() {
		painting = false;
		if (strokeTool == Tool.AXIS_LINE && !strokeMoved) {
			restore(strokeBefore);
			if (anchorX >= 0 && anchorY >= 0) paintLine(anchorX, anchorY, strokeStartX, strokeStartY);
			else paintTexel(strokeStartX, strokeStartY);
			lastPaintX = strokeStartX;
			lastPaintY = strokeStartY;
		}
		if (differsFrom(strokeBefore)) history.record(strokeBefore);
		anchorX = lastPaintX;
		anchorY = lastPaintY;
		strokeBefore = null;
	}

	private void undo() {
		PixelEditHistory.Snapshot snapshot = history.undo(snapshot());
		if (snapshot == null) {
			HairEditorSounds.limit();
			return;
		}
		restore(snapshot);
		anchorX = -1;
		anchorY = -1;
		HairEditorSounds.click();
	}

	private void redo() {
		PixelEditHistory.Snapshot snapshot = history.redo(snapshot());
		if (snapshot == null) {
			HairEditorSounds.limit();
			return;
		}
		restore(snapshot);
		anchorX = -1;
		anchorY = -1;
		HairEditorSounds.click();
	}

	private void clearActive() {
		PixelEditHistory.Snapshot before = snapshot();
		if (mode == Mode.TATTOO) {
			pixels.setTattoo(resolution(), new byte[resolution() * resolution() * 4]);
		} else {
			byte[] target = marks();
			byte mark = marker();
			for (int i = 0; i < target.length; i++) {
				if (target[i] != mark) continue;
				if (canPaint(i % SkinPixels.SKIN_SIZE, i / SkinPixels.SKIN_SIZE)) target[i] = SkinPixels.MARK_NONE;
			}
			pixels.markChanged();
		}
		if (differsFrom(before)) history.record(before);
		anchorX = -1;
		anchorY = -1;
		HairEditorSounds.click();
	}

	private void setTattooSize(int size) {
		if (mode != Mode.TATTOO || size == pixels.getTattooSize()) return;
		PixelEditHistory.Snapshot before = snapshot();
		pixels.tattooForEdit(size);
		history.record(before);
		anchorX = -1;
		anchorY = -1;
		HairEditorSounds.toggle(true);
	}

	private void toggleLayer() {
		overlayLayer = !overlayLayer;
		if (mode == Mode.HAIR) {
			step = overlayLayer ? 1 : 0;
			updateStepButtons();
		}
		anchorX = -1;
		anchorY = -1;
		HairEditorSounds.toggle(overlayLayer);
	}

	private void toggleHair() {
		showHair = !showHair;
		HairEditorSounds.toggle(showHair);
	}

	private void toggleEraser() {
		eraser = !eraser;
		HairEditorSounds.toggle(eraser);
	}

	private void save() {
		NetworkHandler.sendToServer(new UpdateSkinPixelsC2S(pixels));
		HairEditorSounds.click();
		closeScreen();
	}

	private void cancel() {
		pixels.copyFrom(backup);
		closeScreen();
	}

	private void requestClose() {
		if (pixels.sameAs(backup)) {
			cancel();
			return;
		}
		confirmClose = true;
	}

	private void closeScreen() {
		releaseTextures();
		if (this.minecraft != null) this.minecraft.setScreen(previousScreen);
	}

	@Override
	public void removed() {
		releaseTextures();
		super.removed();
	}

	@Override
	public void onClose() {
		requestClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		panorama.render(partialTick, 1.0f);
		renderCinematicBars(graphics);

		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));

		beginUiScale(graphics);
		layout();
		zoom = Mth.lerp(0.3f, zoom, targetZoom);
		renderTopBar(graphics, uiMouseX, uiMouseY);
		renderLeftPanel(graphics);
		renderViewport(graphics);

		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 400.0D);
		graphics.flush();
		RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
		super.render(graphics, uiMouseX, uiMouseY, partialTick);
		if (mode == Mode.TATTOO) renderColorExtras(graphics, uiMouseX, uiMouseY);
		graphics.pose().popPose();

		if (confirmClose) renderConfirmClose(graphics, uiMouseX, uiMouseY);
		endUiScale(graphics);
	}

	private void renderCinematicBars(GuiGraphics graphics) {
		int totalBarHeight = Math.max(61, (int) (this.height * 0.12));
		int fadeSize = 60;
		int solidHeight = totalBarHeight - fadeSize;
		graphics.fill(0, 0, this.width, solidHeight, 0xFF000000);
		graphics.fillGradient(0, solidHeight, this.width, solidHeight + fadeSize, 0xFF000000, 0x00000000);
		int bottomBarStartY = this.height - totalBarHeight;
		graphics.fillGradient(0, bottomBarStartY, this.width, bottomBarStartY + fadeSize, 0x00000000, 0xFF000000);
		graphics.fill(0, bottomBarStartY + fadeSize, this.width, this.height, 0xFF000000);
	}

	private void renderTopBar(GuiGraphics graphics, int mouseX, int mouseY) {
		topBar.clear();
		int x = MARGIN;
		x = placeTopButton(TopButton.UNDO, x, 38);
		x = placeTopButton(TopButton.REDO, x, 38);
		x = placeTopButton(TopButton.CLEAR, x, 40);
		x += 4;
		if (layered) x = placeTopButton(TopButton.LAYER, x, 52);
		x = placeTopButton(TopButton.HAIR, x, 36);
		placeTopButton(TopButton.ERASER, x, 46);
		int right = getUiWidth() - MARGIN;
		topBar.add(new Placement(TopButton.CANCEL, right - 48, 48));
		topBar.add(new Placement(TopButton.SAVE, right - 48 - 3 - 48, 48));

		for (Placement placement : topBar) {
			boolean hovered = HairEditorUi.inside(mouseX, mouseY, placement.x(), TOP_BAR_Y, placement.width(), TOP_BAR_HEIGHT);
			boolean enabled = switch (placement.button()) {
				case UNDO -> history.canUndo();
				case REDO -> history.canRedo();
				default -> true;
			};
			boolean active = switch (placement.button()) {
				case LAYER -> overlayLayer;
				case HAIR -> showHair;
				case ERASER -> eraser;
				default -> false;
			};
			HairEditorUi.button(graphics, this.font, topButtonLabel(placement.button()), placement.x(), TOP_BAR_Y, placement.width(), TOP_BAR_HEIGHT, hovered, active, enabled);
		}
	}

	private int placeTopButton(TopButton button, int x, int width) {
		topBar.add(new Placement(button, x, width));
		return x + width + 3;
	}

	private Component topButtonLabel(TopButton button) {
		return switch (button) {
			case UNDO -> tr("gui.dragonminez.pixel_editor.undo");
			case REDO -> tr("gui.dragonminez.pixel_editor.redo");
			case CLEAR -> tr("gui.dragonminez.pixel_editor.clear");
			case LAYER -> tr("gui.dragonminez.pixel_editor.overlay");
			case HAIR -> tr("gui.dragonminez.pixel_editor.hair");
			case ERASER -> tr("gui.dragonminez.pixel_editor.eraser");
			case SAVE -> tr("gui.dragonminez.pixel_editor.save");
			case CANCEL -> tr("gui.dragonminez.pixel_editor.cancel");
		};
	}

	private void renderLeftPanel(GuiGraphics graphics) {
		int x = MARGIN;
		int y = PANEL_TOP;
		int width = LEFT_WIDTH;
		int height = getUiHeight() - PANEL_TOP - MARGIN;
		HairEditorUi.panel(graphics, x, y, width, height);
		HairEditorUi.header(graphics, this.font, getTitle(), x, y, width);

		int textX = x + 8;
		int textWidth = width - 16;
		if (mode != Mode.TATTOO) {
			String stepKey = "gui.dragonminez.pixel_editor.step." + mode.name().toLowerCase() + "." + (step + 1);
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.pixel_editor.step", step + 1, stepCount()), x + width / 2, y + 32, HairEditorUi.TITLE);
			int cursorY = y + 48;
			for (String line : TextUtil.wrap(this.font, tr(stepKey).getString(), textWidth, DMZ_FONT)) {
				TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(line), x + width / 2, cursorY, HairEditorUi.TEXT);
				cursorY += 10;
			}
			cursorY += 2;
			for (String line : TextUtil.wrap(this.font, tr(stepKey + ".desc").getString(), textWidth, DMZ_FONT)) {
				TextUtil.drawStringWithBorder(graphics, this.font, txt(line), textX, cursorY, HairEditorUi.MUTED);
				cursorY += 10;
			}
			cursorY += 4;
			HairEditorUi.divider(graphics, textX, x + width - 8, cursorY);
			cursorY += 5;
			if (mode == Mode.EYES) {
				cursorY = legendRow(graphics, textX, cursorY, SkinPixels.MARK_BROW, "gui.dragonminez.pixel_editor.legend.brow");
				cursorY = legendRow(graphics, textX, cursorY, SkinPixels.MARK_SCLERA, "gui.dragonminez.pixel_editor.legend.sclera");
				legendRow(graphics, textX, cursorY, SkinPixels.MARK_IRIS, "gui.dragonminez.pixel_editor.legend.iris");
			} else {
				legendRow(graphics, textX, cursorY, SkinPixels.MARK_HAIR, "gui.dragonminez.pixel_editor.legend.hair");
			}
		} else {
			TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.pixel_editor.color"), textX, y + 32, HairEditorUi.TITLE);
			TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.pixel_editor.resolution"), textX, y + 104, HairEditorUi.TITLE);
		}

		int previewY = previewRowY();
		HairEditorUi.divider(graphics, textX, x + width - 8, previewY - 4);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.pixel_editor.preview"), x + width / 2, previewY - 1, HairEditorUi.TITLE);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(formPreview.displayName()), x + width / 2, previewY + 10, HairEditorUi.TEXT);
		if (prevFormButton != null) prevFormButton.visible = formPreview.size() > 1;
		if (nextFormButton != null) nextFormButton.visible = formPreview.size() > 1;
	}

	private void renderHelp(GuiGraphics graphics) {
		String[] keys = layered
				? new String[]{"help.paint", "help.line", "help.view", "help.undo", "help.locked"}
				: new String[]{"help.paint", "help.line", "help.view", "help.undo"};
		int lineY = viewY1 - 6 - keys.length * 10;
		int right = viewX1 - 6;
		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 900.0D);
		for (String key : keys) {
			Component text = tr("gui.dragonminez.pixel_editor." + key);
			TextUtil.drawStringWithBorder(graphics, this.font, text, right - this.font.width(text), lineY, HairEditorUi.MUTED);
			lineY += 10;
		}
		graphics.pose().popPose();
	}

	private int legendRow(GuiGraphics graphics, int x, int y, byte mark, String key) {
		boolean current = mode == Mode.HAIR || mark == marker();
		graphics.fill(x, y, x + 8, y + 8, 0xFF000000 | markerColor(mark));
		HairEditorUi.outline(graphics, x - 1, y - 1, 10, 10, current ? 0xFFFFFFFF : HairEditorUi.DIVIDER);
		TextUtil.drawStringWithBorder(graphics, this.font, tr(key), x + 13, y, current ? HairEditorUi.TEXT : HairEditorUi.MUTED);
		return y + 12;
	}

	private void renderColorExtras(GuiGraphics graphics, int mouseX, int mouseY) {
		int previewX = MARGIN + 8 + 82;
		int previewY = PANEL_TOP + 44;
		int previewSize = 34;
		graphics.fill(previewX - 1, previewY - 1, previewX + previewSize + 1, previewY + previewSize + 1, 0xFFFFFFFF);
		graphics.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, paintColor);

		int buttonY = PANEL_TOP + 116;
		int buttonWidth = 36;
		int startX = MARGIN + 8;
		for (int i = 0; i < SkinPixels.TATTOO_SIZES.length; i++) {
			int size = SkinPixels.TATTOO_SIZES[i];
			int bx = startX + i * (buttonWidth + 3);
			boolean hovered = HairEditorUi.inside(mouseX, mouseY, bx, buttonY, buttonWidth, TOP_BAR_HEIGHT);
			HairEditorUi.button(graphics, this.font, txt(String.valueOf(size)), bx, buttonY, buttonWidth, TOP_BAR_HEIGHT, hovered, size == pixels.getTattooSize(), true);
		}
	}

	private boolean handleResolutionClick(double mouseX, double mouseY) {
		if (mode != Mode.TATTOO) return false;
		int buttonY = PANEL_TOP + 116;
		int buttonWidth = 36;
		int startX = MARGIN + 8;
		for (int i = 0; i < SkinPixels.TATTOO_SIZES.length; i++) {
			int bx = startX + i * (buttonWidth + 3);
			if (HairEditorUi.inside(mouseX, mouseY, bx, buttonY, buttonWidth, TOP_BAR_HEIGHT)) {
				setTattooSize(SkinPixels.TATTOO_SIZES[i]);
				return true;
			}
		}
		return false;
	}

	private void renderViewport(GuiGraphics graphics) {
		HairEditorUi.panel(graphics, viewX0, viewY0, viewX1 - viewX0, viewY1 - viewY0);
		int innerX0 = viewX0 + 4;
		int innerY0 = viewY0 + 4;
		int innerX1 = viewX1 - 4;
		int innerY1 = viewY1 - 4;
		LocalPlayer player = Minecraft.getInstance().player;

		graphics.enableScissor(toScreenCoord(innerX0), toScreenCoord(innerY0), toScreenCoord(innerX1), toScreenCoord(innerY1));
		try {
			boolean wantPick = player != null && !confirmClose && !orbiting && !panning && insideViewport(toUiX(screenMouseX), toUiY(screenMouseY));
			hoverValid = false;
			if (wantPick) {
				graphics.fill(innerX0, innerY0, innerX1, innerY1, VIEW_BACKGROUND);
				renderModel(graphics, player, true);
				readPick();
			}
			updateStroke();
			updateHighlight();

			graphics.flush();
			RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
			graphics.fill(innerX0, innerY0, innerX1, innerY1, VIEW_BACKGROUND);
			if (player != null) renderModel(graphics, player, false);
		} finally {
			graphics.disableScissor();
		}
		renderHelp(graphics);
	}

	private void readPick() {
		Window window = Minecraft.getInstance().getWindow();
		double scale = window.getGuiScale();
		int px = (int) Math.floor(screenMouseX * scale);
		int py = window.getHeight() - 1 - (int) Math.floor(screenMouseY * scale);
		if (px < 0 || py < 0 || px >= window.getWidth() || py >= window.getHeight()) return;
		PICK_BUFFER.clear();
		GL11.glReadPixels(px, py, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, PICK_BUFFER);
		int r = PICK_BUFFER.get(0) & 0xFF;
		int g = PICK_BUFFER.get(1) & 0xFF;
		int b = PICK_BUFFER.get(2) & 0xFF;
		if (b != checksum(r, g)) return;
		int res = resolution();
		hoverX = r * res / PICK_RES;
		hoverY = g * res / PICK_RES;
		hoverValid = true;
	}

	private void updateHighlight() {
		var manager = Minecraft.getInstance().getTextureManager();
		if (highlightTexture == null) {
			highlightTexture = new DynamicTexture(new NativeImage(NativeImage.Format.RGBA, HIGHLIGHT_RES, HIGHLIGHT_RES, true));
			manager.register(HIGHLIGHT_LOCATION, highlightTexture);
			highlightCells.clear();
		}
		NativeImage image = highlightTexture.getPixels();
		if (image == null) return;

		int dirtyX0 = HIGHLIGHT_RES;
		int dirtyY0 = HIGHLIGHT_RES;
		int dirtyX1 = 0;
		int dirtyY1 = 0;
		for (int cell : highlightCells) {
			int bx = (cell % highlightCellRes) * highlightFactor;
			int by = (cell / highlightCellRes) * highlightFactor;
			image.fillRect(bx, by, highlightFactor, highlightFactor, 0);
			dirtyX0 = Math.min(dirtyX0, bx);
			dirtyY0 = Math.min(dirtyY0, by);
			dirtyX1 = Math.max(dirtyX1, bx + highlightFactor);
			dirtyY1 = Math.max(dirtyY1, by + highlightFactor);
		}
		highlightCells.clear();

		int res = resolution();
		highlightCellRes = res;
		highlightFactor = Math.max(1, HIGHLIGHT_RES / res);
		if (hoverValid && !confirmClose) {
			if (!painting && shiftDown() && anchorX >= 0 && anchorY >= 0) collectLine(anchorX, anchorY, hoverX, hoverY, res);
			highlightCells.add(hoverY * res + hoverX);
			for (int cell : highlightCells) {
				int bx = (cell % res) * highlightFactor;
				int by = (cell / res) * highlightFactor;
				drawFrame(image, bx, by, highlightFactor);
				dirtyX0 = Math.min(dirtyX0, bx);
				dirtyY0 = Math.min(dirtyY0, by);
				dirtyX1 = Math.max(dirtyX1, bx + highlightFactor);
				dirtyY1 = Math.max(dirtyY1, by + highlightFactor);
			}
		}
		if (dirtyX1 > dirtyX0 && dirtyY1 > dirtyY0) {
			highlightTexture.bind();
			image.upload(0, dirtyX0, dirtyY0, dirtyX0, dirtyY0, dirtyX1 - dirtyX0, dirtyY1 - dirtyY0, false, false);
		}
	}

	private static void drawFrame(NativeImage image, int x0, int y0, int size) {
		if (size <= 2) {
			image.fillRect(x0, y0, size, size, FRAME_OUTER);
			return;
		}
		int x1 = x0 + size - 1;
		int y1 = y0 + size - 1;
		for (int i = 0; i < size; i++) {
			image.setPixelRGBA(x0 + i, y0, FRAME_OUTER);
			image.setPixelRGBA(x0 + i, y1, FRAME_OUTER);
			image.setPixelRGBA(x0, y0 + i, FRAME_OUTER);
			image.setPixelRGBA(x1, y0 + i, FRAME_OUTER);
		}
		if (size < 6) return;
		for (int i = 1; i < size - 1; i++) {
			image.setPixelRGBA(x0 + i, y0 + 1, FRAME_INNER);
			image.setPixelRGBA(x0 + i, y1 - 1, FRAME_INNER);
			image.setPixelRGBA(x0 + 1, y0 + i, FRAME_INNER);
			image.setPixelRGBA(x1 - 1, y0 + i, FRAME_INNER);
		}
	}

	private void collectLine(int x0, int y0, int x1, int y1, int res) {
		int dx = Math.abs(x1 - x0);
		int dy = -Math.abs(y1 - y0);
		int sx = x0 < x1 ? 1 : -1;
		int sy = y0 < y1 ? 1 : -1;
		int err = dx + dy;
		int x = x0;
		int y = y0;
		while (true) {
			if (canPaint(x, y)) highlightCells.add(y * res + x);
			if (x == x1 && y == y1) break;
			int e2 = 2 * err;
			if (e2 >= dy) {
				err += dy;
				x += sx;
			}
			if (e2 <= dx) {
				err += dx;
				y += sy;
			}
		}
	}

	private List<SkinPaintContext.Overlay> markOverlays(LocalPlayer player) {
		List<SkinPaintContext.Overlay> overlays = new ArrayList<>();
		if (mode == Mode.TATTOO || !formPreview.current().isBase()) return overlays;
		if (mode == Mode.EYES) {
			for (byte mark = SkinPixels.MARK_BROW; mark <= SkinPixels.MARK_IRIS; mark++) {
				ResourceLocation mask = SkinPixelTextures.eyeMask(player, pixels, mark);
				if (mask != null) overlays.add(new SkinPaintContext.Overlay(mask, rgb(markerColor(mark)), MARK_ALPHA));
			}
		} else {
			ResourceLocation mask = SkinPixelTextures.hairMask(player, pixels);
			if (mask != null) overlays.add(new SkinPaintContext.Overlay(mask, rgb(HAIR_MARK_COLOR), MARK_ALPHA));
		}
		return overlays;
	}

	private void renderModel(GuiGraphics graphics, LocalPlayer player, boolean pick) {
		FormPreview.Snapshot snapshot = formPreview.apply(player);
		boolean previewApplied = !formPreview.current().isBase();

		int scale = adjustedModelScale(player, Math.round(zoom));
		int centerX = (viewX0 + viewX1) / 2 + panX;
		int centerY = (viewY0 + viewY1) / 2 + panY;
		int baseY = centerY + Math.round(scale * (mode == Mode.EYES ? 1.62f : 0.9f));

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

		List<SkinPaintContext.Overlay> overlays = pick ? List.of() : markOverlays(player);
		SkinPaintContext.Overlay highlight = !pick && hoverValid ? new SkinPaintContext.Overlay(HIGHLIGHT_LOCATION, WHITE_RGB, 1.0f) : null;

		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 150.0D);
		DMZSkinLayer.PREVIEW_MODE = previewApplied;
		try (SkinPaintContext.Scope ignored = SkinPaintContext.begin(pick, PICK_LOCATION, showHair, overlayLayer, overlays, highlight);
		     HairRenderContext.Scope hairScope = HairRenderContext.editorPreview(hairPreview)) {
			InventoryScreen.renderEntityInInventory(graphics, centerX, baseY, scale, new Quaternionf().rotateZ((float) Math.PI), new Quaternionf(), player);
		} finally {
			DMZSkinLayer.PREVIEW_MODE = false;
			graphics.pose().popPose();
			formPreview.restore(player, snapshot);

			player.yBodyRot = bodyYaw;
			player.yBodyRotO = bodyYawO;
			player.setYRot(entityYaw);
			player.setXRot(entityPitch);
			player.xRotO = entityPitchO;
			player.yHeadRotO = headYawO;
			player.yHeadRot = headYaw;
		}
	}

	private int adjustedModelScale(LivingEntity player, int baseScale) {
		float[] visual = {0.9375f};
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			Float[] resolved = stats.getCharacter().getResolvedModelScaling();
			float scaleX = resolved != null && resolved.length > 0 && resolved[0] != null && resolved[0] > 0.0f ? resolved[0] : 0.9375f;
			float scaleY = resolved != null && resolved.length > 1 && resolved[1] != null && resolved[1] > 0.0f ? resolved[1] : 0.9375f;
			visual[0] = Math.max(0.1f, (scaleX + scaleY) / 2.0f);
		});
		if (visual[0] <= 0.9375f) return baseScale;
		return Math.max(1, Math.round(baseScale * 0.9375f / visual[0]));
	}

	private void renderConfirmClose(GuiGraphics graphics, int mouseX, int mouseY) {
		int width = 150;
		int height = 58;
		int x = (getUiWidth() - width) / 2;
		int y = (getUiHeight() - height) / 2;
		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 600.0D);
		graphics.fill(0, 0, getUiWidth(), getUiHeight(), 0x90000000);
		HairEditorUi.smallPanel(graphics, x, y, width, height);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.pixel_editor.confirm.title"), x + width / 2, y + 14, HairEditorUi.TITLE);
		int buttonWidth = 62;
		int buttonY = y + height - 26;
		int discardX = x + 10;
		int keepX = x + width - 10 - buttonWidth;
		HairEditorUi.button(graphics, this.font, tr("gui.dragonminez.pixel_editor.confirm.discard"), discardX, buttonY, buttonWidth, 16, HairEditorUi.inside(mouseX, mouseY, discardX, buttonY, buttonWidth, 16), false, true);
		HairEditorUi.button(graphics, this.font, tr("gui.dragonminez.pixel_editor.confirm.keep"), keepX, buttonY, buttonWidth, 16, HairEditorUi.inside(mouseX, mouseY, keepX, buttonY, buttonWidth, 16), false, true);
		graphics.pose().popPose();
	}

	private boolean handleConfirmClick(double mouseX, double mouseY) {
		int width = 150;
		int height = 58;
		int x = (getUiWidth() - width) / 2;
		int y = (getUiHeight() - height) / 2;
		int buttonWidth = 62;
		int buttonY = y + height - 26;
		int discardX = x + 10;
		int keepX = x + width - 10 - buttonWidth;
		if (HairEditorUi.inside(mouseX, mouseY, discardX, buttonY, buttonWidth, 16)) {
			HairEditorSounds.click();
			confirmClose = false;
			cancel();
		} else if (HairEditorUi.inside(mouseX, mouseY, keepX, buttonY, buttonWidth, 16)) {
			HairEditorSounds.click();
			confirmClose = false;
		}
		return true;
	}

	private boolean handleTopBarClick(double mouseX, double mouseY) {
		for (Placement placement : topBar) {
			if (!HairEditorUi.inside(mouseX, mouseY, placement.x(), TOP_BAR_Y, placement.width(), TOP_BAR_HEIGHT)) continue;
			switch (placement.button()) {
				case UNDO -> undo();
				case REDO -> redo();
				case CLEAR -> clearActive();
				case LAYER -> toggleLayer();
				case HAIR -> toggleHair();
				case ERASER -> toggleEraser();
				case SAVE -> save();
				case CANCEL -> requestClose();
			}
			return true;
		}
		return false;
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		screenMouseX = mouseX;
		screenMouseY = mouseY;
		super.mouseMoved(mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		screenMouseX = mouseX;
		screenMouseY = mouseY;
		double uiX = toUiX(mouseX);
		double uiY = toUiY(mouseY);
		if (confirmClose) return handleConfirmClick(uiX, uiY);
		if (super.mouseClicked(mouseX, mouseY, button)) return true;
		if (button == 0 && handleTopBarClick(uiX, uiY)) return true;
		if (button == 0 && handleResolutionClick(uiX, uiY)) return true;
		if (!insideViewport(uiX, uiY)) return false;

		orbiting = false;
		panning = false;
		pressHoverValid = hoverValid;
		pressHoverX = hoverX;
		pressHoverY = hoverY;
		if (button == 0) {
			if (hoverValid) beginStroke(shiftDown() ? Tool.AXIS_LINE : Tool.BRUSH);
			else pressingViewport = true;
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

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		screenMouseX = mouseX;
		screenMouseY = mouseY;
		double uiX = toUiX(mouseX);
		double uiY = toUiY(mouseY);
		if (confirmClose) return true;

		if (painting) return true;
		boolean moved = Math.hypot(mouseX - pressX, mouseY - pressY) > CLICK_DRAG_THRESHOLD;
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

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (painting) endStroke();
		pressingViewport = false;
		pressingSecondary = false;
		orbiting = false;
		panning = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		double uiX = toUiX(mouseX);
		double uiY = toUiY(mouseY);
		if (!confirmClose && insideViewport(uiX, uiY)) {
			targetZoom = Mth.clamp(targetZoom * (delta > 0 ? ZOOM_FACTOR : 1.0f / ZOOM_FACTOR), MIN_ZOOM, MAX_ZOOM);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (confirmClose) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) confirmClose = false;
			return true;
		}
		if (hexField != null && hexField.isFocused()) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
				hexField.setFocused(false);
				return true;
			}
			return super.keyPressed(keyCode, scanCode, modifiers);
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			requestClose();
			return true;
		}
		if (hasControlDown() && keyCode == GLFW.GLFW_KEY_Z) {
			if (hasShiftDown()) redo();
			else undo();
			return true;
		}
		if (hasControlDown() && (keyCode == GLFW.GLFW_KEY_Y || keyCode == GLFW.GLFW_KEY_X)) {
			redo();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
}
