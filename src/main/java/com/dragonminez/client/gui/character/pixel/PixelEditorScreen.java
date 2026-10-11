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
import net.minecraft.Util;
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
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class PixelEditorScreen extends ScaledScreen {
	public enum Mode { EYES, HAIR, TATTOO }

	private enum TopButton { UNDO, REDO, CLEAR, LAYER, HAIR, ERASER, SAVE, CANCEL }

	private enum Tool { BRUSH, AXIS_LINE }

	private enum ViewPreset {
		FRONT(0.0f, 0.0f),
		BACK(180.0f, 0.0f),
		LEFT(90.0f, 0.0f),
		RIGHT(-90.0f, 0.0f),
		TOP(0.0f, 90.0f),
		BOTTOM(0.0f, -90.0f),
		RESET(0.0f, 0.0f);

		private final float yaw;
		private final float pitch;

		ViewPreset(float yaw, float pitch) {
			this.yaw = yaw;
			this.pitch = pitch;
		}

		private String key() {
			return "gui.dragonminez.pixel_editor.view." + name().toLowerCase();
		}
	}

	private enum BodyPart {
		HEAD("head", 9, 0, 17, 16),
		RIGHT_ARM("right_arm", 0, 17, 8, 24),
		BODY("body", 9, 17, 17, 24),
		LEFT_ARM("left_arm", 27, 17, 8, 24),
		RIGHT_LEG("right_leg", 9, 42, 8, 24),
		LEFT_LEG("left_leg", 18, 42, 8, 24);

		private final String bone;
		private final int x;
		private final int y;
		private final int width;
		private final int height;

		BodyPart(String bone, int x, int y, int width, int height) {
			this.bone = bone;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}
	}

	private record Placement(TopButton button, int x, int width) {}

	private record ViewButton(ViewPreset preset, int x, int y, int width) {}

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
	private static final double MODEL_DEPTH = 4000.0D;
	private static final int VIEW_BUTTON_HEIGHT = 13;
	private static final int DOLL_WIDTH = 35;
	private static final int DOLL_HEIGHT = 66;
	private static final int DOLL_LABEL_HEIGHT = 11;
	private static final int CONNECT_DISTANCE = 8;
	private static final long DOUBLE_CLICK_MILLIS = 250L;
	private static final double DOUBLE_CLICK_SLOP = 3.0D;
	private static final float INTERIOR_PROBE = 0.005f;
	private static final float FACING_THRESHOLD = -0.05f;
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
	private float yaw;
	private float pitch;
	private boolean orbitFlipped;
	private final Vector3f orbitTarget = new Vector3f();
	private boolean orbitTargetInitialized;
	private int modelScale;
	private boolean viewInitialized;
	private final List<ViewButton> viewButtons = new ArrayList<>();
	private final EnumSet<BodyPart> hiddenParts = EnumSet.noneOf(BodyPart.class);
	private Set<String> hiddenBones = Set.of();

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
	private long lastClickMillis;
	private double lastClickX;
	private double lastClickY;
	private PixelEditHistory.Snapshot lastClickBefore;
	private boolean lastClickRecorded;
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
		if (mode == Mode.TATTOO) pixels.tattooForEdit(pixels.getTattooSize() > 0 && canUseTattooSize(pixels.getTattooSize()) ? pixels.getTattooSize() : SkinPixels.TATTOO_SIZES[0]);
	}

	private static boolean canUseTattooSize(int size) {
		LocalPlayer player = Minecraft.getInstance().player;
		return SkinPixels.canUseTattooSize(player != null ? player.getGameProfile().getName() : null, size);
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
			targetZoom = defaultZoom();
			zoom = targetZoom;
		}
		layoutViewButtons();
	}

	private float defaultZoom() {
		float height = viewY1 - viewY0 - 16;
		return Mth.clamp(mode == Mode.EYES ? height * 1.35f : height / 2.0f, MIN_ZOOM, MAX_ZOOM);
	}

	private void layoutViewButtons() {
		viewButtons.clear();
		int startX = viewX0 + 6;
		int limitX = dollX() - 6;
		int x = startX;
		int y = viewY0 + 6;
		for (ViewPreset preset : ViewPreset.values()) {
			int width = Math.max(26, this.font.width(tr(preset.key())) + 8);
			if (x > startX && x + width > limitX) {
				x = startX;
				y += VIEW_BUTTON_HEIGHT + 2;
			}
			viewButtons.add(new ViewButton(preset, x, y, width));
			x += width + 2;
		}
	}

	private int dollX() {
		return viewX1 - 8 - DOLL_WIDTH;
	}

	private int dollY() {
		return viewY0 + 6 + DOLL_LABEL_HEIGHT;
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
		SkinPaintContext.clearPickedBones();
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
		lastClickBefore = null;
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
		boolean changed = differsFrom(strokeBefore);
		if (changed) history.record(strokeBefore);
		if (strokeTool == Tool.BRUSH && !strokeMoved) {
			lastClickBefore = strokeBefore;
			lastClickRecorded = changed;
		} else {
			lastClickMillis = 0L;
			lastClickBefore = null;
		}
		anchorX = lastPaintX;
		anchorY = lastPaintY;
		strokeBefore = null;
	}

	private boolean isDoubleClick(double uiX, double uiY) {
		if (lastClickBefore == null || lastClickMillis <= 0L) return false;
		if (Util.getMillis() - lastClickMillis > DOUBLE_CLICK_MILLIS) return false;
		return Math.hypot(uiX - lastClickX, uiY - lastClickY) <= DOUBLE_CLICK_SLOP;
	}

	private void fillBoneFace(int x, int y) {
		PixelEditHistory.Snapshot before = lastClickBefore;
		boolean recorded = lastClickRecorded;
		lastClickMillis = 0L;
		lastClickBefore = null;
		List<int[]> rects = boneFaceAt(x, y);
		if (rects.isEmpty() || before == null) return;
		restore(before);
		erasing = eraser || hasAltDown();
		for (int[] rect : rects) {
			for (int py = rect[1]; py < rect[3]; py++) {
				for (int px = rect[0]; px < rect[2]; px++) paintTexel(px, py);
			}
		}
		if (!recorded && differsFrom(before)) history.record(before);
		anchorX = x;
		anchorY = y;
	}

	private List<int[]> boneFaceAt(int x, int y) {
		int res = resolution();
		float u = (x + 0.5f) / res;
		float v = (y + 0.5f) / res;
		Quaternionf view = new Quaternionf().rotateX(-pitch * Mth.DEG_TO_RAD).rotateY(yaw * Mth.DEG_TO_RAD);
		GeoBone hitBone = null;
		GeoQuad hitQuad = null;
		int hitAxis = -1;
		float hitScore = Float.MAX_VALUE;
		for (GeoBone bone : SkinPaintContext.pickedBones()) {
			for (GeoCube cube : bone.getCubes()) {
				Vector3f center = cubeCenter(cube);
				for (GeoQuad quad : cube.quads()) {
					if (quad == null) continue;
					float[] uv = uvBounds(quad);
					float area = (uv[2] - uv[0]) * (uv[3] - uv[1]);
					if (area <= 0.0f || u < uv[0] || u >= uv[2] || v < uv[1] || v >= uv[3]) continue;
					int axis = faceAxis(quad, center);
					float score = area;
					if (view.transform(axisVector(axis)).z >= FACING_THRESHOLD) score += 2.0f;
					if (isInterior(bone, cube, quad, axis)) score += 4.0f;
					if (score < hitScore) {
						hitScore = score;
						hitBone = bone;
						hitQuad = quad;
						hitAxis = axis;
					}
				}
			}
		}
		if (hitBone == null) return List.of();

		List<int[]> rects = new ArrayList<>();
		for (GeoCube cube : hitBone.getCubes()) {
			Vector3f center = cubeCenter(cube);
			for (GeoQuad quad : cube.quads()) {
				if (quad == null) continue;
				if (quad != hitQuad && (faceAxis(quad, center) != hitAxis || isInterior(hitBone, cube, quad, hitAxis))) continue;
				float[] uv = uvBounds(quad);
				if ((uv[2] - uv[0]) * (uv[3] - uv[1]) <= 0.0f) continue;
				rects.add(new int[]{Math.round(uv[0] * res), Math.round(uv[1] * res), Math.round(uv[2] * res), Math.round(uv[3] * res)});
			}
		}
		return rects;
	}

	private static float[] uvBounds(GeoQuad quad) {
		float minU = Float.MAX_VALUE;
		float minV = Float.MAX_VALUE;
		float maxU = -Float.MAX_VALUE;
		float maxV = -Float.MAX_VALUE;
		for (GeoVertex vertex : quad.vertices()) {
			minU = Math.min(minU, vertex.texU());
			minV = Math.min(minV, vertex.texV());
			maxU = Math.max(maxU, vertex.texU());
			maxV = Math.max(maxV, vertex.texV());
		}
		return new float[]{minU, minV, maxU, maxV};
	}

	private static Vector3f quadCenter(GeoQuad quad) {
		Vector3f center = new Vector3f();
		for (GeoVertex vertex : quad.vertices()) center.add(vertex.position());
		return center.div(quad.vertices().length);
	}

	private static Vector3f cubeCenter(GeoCube cube) {
		float[] bounds = cubeBounds(cube);
		return new Vector3f((bounds[0] + bounds[3]) / 2.0f, (bounds[1] + bounds[4]) / 2.0f, (bounds[2] + bounds[5]) / 2.0f);
	}

	private static float[] cubeBounds(GeoCube cube) {
		float[] bounds = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
		for (GeoQuad quad : cube.quads()) {
			if (quad == null) continue;
			for (GeoVertex vertex : quad.vertices()) {
				Vector3f p = vertex.position();
				bounds[0] = Math.min(bounds[0], p.x);
				bounds[1] = Math.min(bounds[1], p.y);
				bounds[2] = Math.min(bounds[2], p.z);
				bounds[3] = Math.max(bounds[3], p.x);
				bounds[4] = Math.max(bounds[4], p.y);
				bounds[5] = Math.max(bounds[5], p.z);
			}
		}
		return bounds;
	}

	private static int faceAxis(GeoQuad quad, Vector3f cubeCenter) {
		Vector3f offset = quadCenter(quad).sub(cubeCenter);
		float ax = Math.abs(offset.x);
		float ay = Math.abs(offset.y);
		float az = Math.abs(offset.z);
		if (Math.max(ax, Math.max(ay, az)) < 1.0e-4f) {
			return switch (quad.direction()) {
				case EAST -> 0;
				case WEST -> 1;
				case UP -> 2;
				case DOWN -> 3;
				case SOUTH -> 4;
				case NORTH -> 5;
			};
		}
		if (ax >= ay && ax >= az) return offset.x >= 0.0f ? 0 : 1;
		if (ay >= az) return offset.y >= 0.0f ? 2 : 3;
		return offset.z >= 0.0f ? 4 : 5;
	}

	private static Vector3f axisVector(int axis) {
		float sign = (axis & 1) == 0 ? 1.0f : -1.0f;
		return switch (axis >> 1) {
			case 0 -> new Vector3f(sign, 0.0f, 0.0f);
			case 1 -> new Vector3f(0.0f, sign, 0.0f);
			default -> new Vector3f(0.0f, 0.0f, sign);
		};
	}

	private static boolean isInterior(GeoBone bone, GeoCube cube, GeoQuad quad, int axis) {
		if (cube.rotation().lengthSqr() > 1.0e-6) return false;
		Vector3f probe = quadCenter(quad).add(axisVector(axis).mul(INTERIOR_PROBE));
		for (GeoCube other : bone.getCubes()) {
			if (other == cube || other.rotation().lengthSqr() > 1.0e-6) continue;
			float[] bounds = cubeBounds(other);
			if (probe.x > bounds[0] && probe.y > bounds[1] && probe.z > bounds[2] && probe.x < bounds[3] && probe.y < bounds[4] && probe.z < bounds[5]) return true;
		}
		return false;
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
		lastClickBefore = null;
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
		lastClickBefore = null;
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
		lastClickBefore = null;
		HairEditorSounds.click();
	}

	private void setTattooSize(int size) {
		if (mode != Mode.TATTOO || size == pixels.getTattooSize()) return;
		if (!canUseTattooSize(size)) {
			HairEditorSounds.limit();
			return;
		}
		PixelEditHistory.Snapshot before = snapshot();
		pixels.tattooForEdit(size);
		history.record(before);
		anchorX = -1;
		anchorY = -1;
		lastClickBefore = null;
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
		lastClickBefore = null;
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

	private void applyView(ViewPreset preset) {
		yaw = preset.yaw;
		pitch = preset.pitch;
		if (preset == ViewPreset.RESET) {
			orbitTargetInitialized = false;
			targetZoom = defaultZoom();
		}
		HairEditorSounds.click();
	}

	private void togglePart(BodyPart part, boolean solo) {
		if (solo) {
			EnumSet<BodyPart> others = EnumSet.complementOf(EnumSet.of(part));
			boolean alreadySolo = !hiddenParts.contains(part) && hiddenParts.containsAll(others);
			hiddenParts.clear();
			if (!alreadySolo) hiddenParts.addAll(others);
		} else if (!hiddenParts.remove(part)) {
			hiddenParts.add(part);
		}
		Set<String> bones = new HashSet<>();
		for (BodyPart hidden : hiddenParts) bones.add(hidden.bone);
		hiddenBones = Set.copyOf(bones);
		HairEditorSounds.toggle(!hiddenParts.contains(part));
	}

	private Quaternionf orbitRotation() {
		return new Quaternionf().rotateX(pitch * Mth.DEG_TO_RAD).rotateY(yaw * Mth.DEG_TO_RAD);
	}

	private void panBy(double dx, double dy) {
		if (modelScale <= 0) return;
		Vector3f delta = new Vector3f((float) dx, (float) -dy, 0.0f).div(modelScale);
		orbitRotation().conjugate().transform(delta);
		orbitTarget.sub(-delta.x, delta.y, -delta.z);
	}

	private void resetOrbitTarget(LivingEntity player) {
		float height = mode == Mode.EYES ? 1.62f : 0.9f;
		orbitTarget.set(0.0f, height * visualScale(player) / 0.9375f, 0.0f);
		orbitTargetInitialized = true;
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
		renderViewport(graphics, uiMouseX, uiMouseY);

		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 400.0D);
		graphics.flush();
		RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
		super.render(graphics, uiMouseX, uiMouseY, partialTick);
		if (mode == Mode.TATTOO) renderColorExtras(graphics, uiMouseX, uiMouseY);
		if (!confirmClose) renderPartTooltip(graphics, uiMouseX, uiMouseY);
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
		if (!layered) return;
		Component text = tr("gui.dragonminez.pixel_editor.help.locked");
		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 900.0D);
		TextUtil.drawStringWithBorder(graphics, this.font, text, viewX1 - 6 - this.font.width(text), viewY1 - 16, HairEditorUi.MUTED);
		graphics.pose().popPose();
	}

	private void renderViewportOverlay(GuiGraphics graphics, int mouseX, int mouseY) {
		boolean interactive = !confirmClose && !painting && !orbiting && !panning;
		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 900.0D);
		for (ViewButton button : viewButtons) {
			boolean hovered = interactive && HairEditorUi.inside(mouseX, mouseY, button.x(), button.y(), button.width(), VIEW_BUTTON_HEIGHT);
			HairEditorUi.button(graphics, this.font, tr(button.preset().key()), button.x(), button.y(), button.width(), VIEW_BUTTON_HEIGHT, hovered, false, true);
		}

		int dollX = dollX();
		int dollY = dollY();
		Component label = tr("gui.dragonminez.pixel_editor.parts");
		int labelWidth = this.font.width(label);
		int labelX = Math.min(dollX + (DOLL_WIDTH - labelWidth) / 2, viewX1 - 6 - labelWidth);
		TextUtil.drawStringWithBorder(graphics, this.font, label, labelX, dollY - DOLL_LABEL_HEIGHT, HairEditorUi.TITLE);
		for (BodyPart part : BodyPart.values()) {
			int px = dollX + part.x;
			int py = dollY + part.y;
			boolean hovered = interactive && HairEditorUi.inside(mouseX, mouseY, px, py, part.width, part.height);
			boolean visible = !hiddenParts.contains(part);
			HairEditorUi.button(graphics, this.font, Component.empty(), px, py, part.width, part.height, hovered, visible, visible || hovered);
			if (hovered) HairEditorUi.outline(graphics, px - 1, py - 1, part.width + 2, part.height + 2, HairEditorUi.HOVER_TEXT);
		}
		graphics.pose().popPose();
	}

	private BodyPart partAt(double mouseX, double mouseY) {
		int dollX = dollX();
		int dollY = dollY();
		for (BodyPart part : BodyPart.values()) {
			if (HairEditorUi.inside(mouseX, mouseY, dollX + part.x, dollY + part.y, part.width, part.height)) return part;
		}
		return null;
	}

	private ViewButton viewButtonAt(double mouseX, double mouseY) {
		for (ViewButton button : viewButtons) {
			if (HairEditorUi.inside(mouseX, mouseY, button.x(), button.y(), button.width(), VIEW_BUTTON_HEIGHT)) return button;
		}
		return null;
	}

	private boolean overViewportOverlay(double mouseX, double mouseY) {
		if (HairEditorUi.inside(mouseX, mouseY, dollX() - 2, dollY() - DOLL_LABEL_HEIGHT, DOLL_WIDTH + 4, DOLL_HEIGHT + DOLL_LABEL_HEIGHT + 2)) return true;
		return viewButtonAt(mouseX, mouseY) != null;
	}

	private boolean handleViewportOverlayClick(double mouseX, double mouseY) {
		ViewButton button = viewButtonAt(mouseX, mouseY);
		if (button != null) {
			applyView(button.preset());
			return true;
		}
		BodyPart part = partAt(mouseX, mouseY);
		if (part != null) {
			togglePart(part, hasShiftDown());
			return true;
		}
		return overViewportOverlay(mouseX, mouseY);
	}

	private void renderPartTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
		if (painting || orbiting || panning) return;
		BodyPart part = partAt(mouseX, mouseY);
		if (part == null) return;
		boolean hidden = hiddenParts.contains(part);
		TextUtil.renderAdvancedTooltip(graphics, this.font, mouseX, mouseY, getUiWidth(), getUiHeight(),
				tr("gui.dragonminez.pixel_editor.part." + part.bone),
				List.of(tr("gui.dragonminez.pixel_editor.part." + (hidden ? "hidden" : "visible")).withStyle(style -> style.withColor(hidden ? HairEditorUi.DISABLED : HairEditorUi.SUCCESS))),
				List.of(tr("gui.dragonminez.pixel_editor.part.hint").withStyle(style -> style.withColor(HairEditorUi.MUTED))),
				HairEditorUi.HOVER_TEXT);
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
		boolean lockedHovered = false;
		for (int i = 0; i < SkinPixels.TATTOO_SIZES.length; i++) {
			int size = SkinPixels.TATTOO_SIZES[i];
			int bx = startX + i * (buttonWidth + 3);
			boolean hovered = HairEditorUi.inside(mouseX, mouseY, bx, buttonY, buttonWidth, TOP_BAR_HEIGHT);
			boolean allowed = canUseTattooSize(size);
			if (hovered && !allowed) lockedHovered = true;
			HairEditorUi.button(graphics, this.font, txt(String.valueOf(size)), bx, buttonY, buttonWidth, TOP_BAR_HEIGHT, hovered, size == pixels.getTattooSize(), allowed);
		}
		if (lockedHovered && !confirmClose) {
			TextUtil.renderAdvancedTooltip(graphics, this.font, mouseX, mouseY, getUiWidth(), getUiHeight(),
					tr("gui.dragonminez.pixel_editor.resolution"), List.of(tr("gui.dragonminez.pixel_editor.resolution.locked")), null, HairEditorUi.WARNING);
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

	private void renderViewport(GuiGraphics graphics, int mouseX, int mouseY) {
		HairEditorUi.panel(graphics, viewX0, viewY0, viewX1 - viewX0, viewY1 - viewY0);
		int innerX0 = viewX0 + 4;
		int innerY0 = viewY0 + 4;
		int innerX1 = viewX1 - 4;
		int innerY1 = viewY1 - 4;
		LocalPlayer player = Minecraft.getInstance().player;

		graphics.enableScissor(toScreenCoord(innerX0), toScreenCoord(innerY0), toScreenCoord(innerX1), toScreenCoord(innerY1));
		try {
			double pickX = toUiX(screenMouseX);
			double pickY = toUiY(screenMouseY);
			boolean overOverlay = !painting && overViewportOverlay(pickX, pickY);
			boolean wantPick = player != null && !confirmClose && !orbiting && !panning && !overOverlay && (painting || !hasControlDown()) && insideViewport(pickX, pickY);
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
		graphics.flush();
		RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
		renderViewportOverlay(graphics, mouseX, mouseY);
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

		if (!orbitTargetInitialized) resetOrbitTarget(player);
		int scale = adjustedModelScale(player, Math.round(zoom));
		modelScale = scale;
		int centerX = (viewX0 + viewX1) / 2;
		int centerY = (viewY0 + viewY1) / 2;
		Quaternionf modelPose = new Quaternionf().rotateZ((float) Math.PI).rotateX(-pitch * Mth.DEG_TO_RAD).rotateY(yaw * Mth.DEG_TO_RAD);
		Vector3f pivot = modelPose.transform(new Vector3f(orbitTarget)).mul(scale, scale, -scale);

		float bodyYaw = player.yBodyRot;
		float bodyYawO = player.yBodyRotO;
		float entityYaw = player.getYRot();
		float entityPitch = player.getXRot();
		float entityPitchO = player.xRotO;
		float headYawO = player.yHeadRotO;
		float headYaw = player.yHeadRot;

		player.yBodyRot = 180.0f;
		player.yBodyRotO = 180.0f;
		player.setYRot(180.0f);
		player.setXRot(0.0f);
		player.xRotO = 0.0f;
		player.yHeadRot = 180.0f;
		player.yHeadRotO = 180.0f;

		List<SkinPaintContext.Overlay> overlays = pick ? List.of() : markOverlays(player);
		SkinPaintContext.Overlay highlight = !pick && hoverValid ? new SkinPaintContext.Overlay(HIGHLIGHT_LOCATION, WHITE_RGB, 1.0f) : null;

		graphics.pose().pushPose();
		graphics.pose().translate(-pivot.x, -pivot.y, MODEL_DEPTH - pivot.z);
		DMZSkinLayer.PREVIEW_MODE = previewApplied;
		try (SkinPaintContext.Scope ignored = SkinPaintContext.begin(pick, PICK_LOCATION, showHair, overlayLayer, hiddenBones, overlays, highlight);
		     HairRenderContext.Scope hairScope = HairRenderContext.editorPreview(hairPreview)) {
			InventoryScreen.renderEntityInInventory(graphics, centerX, centerY, scale, modelPose, new Quaternionf(), player);
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
		float visual = visualScale(player);
		if (visual <= 0.9375f) return baseScale;
		return Math.max(1, Math.round(baseScale * 0.9375f / visual));
	}

	private static float visualScale(LivingEntity player) {
		float[] visual = {0.9375f};
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			Float[] resolved = stats.getCharacter().getResolvedModelScaling();
			float scaleX = resolved != null && resolved.length > 0 && resolved[0] != null && resolved[0] > 0.0f ? resolved[0] : 0.9375f;
			float scaleY = resolved != null && resolved.length > 1 && resolved[1] != null && resolved[1] > 0.0f ? resolved[1] : 0.9375f;
			visual[0] = Math.max(0.1f, (scaleX + scaleY) / 2.0f);
		});
		return visual[0];
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
		if (button == 0 && handleViewportOverlayClick(uiX, uiY)) return true;

		orbiting = false;
		panning = false;
		pressHoverValid = hoverValid;
		pressHoverX = hoverX;
		pressHoverY = hoverY;
		if (button == 0) {
			if (hoverValid && !hasControlDown()) {
				if (!shiftDown() && isDoubleClick(uiX, uiY)) {
					fillBoneFace(hoverX, hoverY);
					return true;
				}
				beginStroke(shiftDown() ? Tool.AXIS_LINE : Tool.BRUSH);
				lastClickMillis = Util.getMillis();
				lastClickX = uiX;
				lastClickY = uiY;
				lastClickBefore = null;
			} else {
				pressingViewport = true;
				lastClickMillis = 0L;
			}
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
			if (!orbiting && moved) {
				orbiting = true;
				orbitFlipped = Mth.cos(pitch * Mth.DEG_TO_RAD) < 0.0f;
			}
			if (orbiting) {
				float dx = (float) (uiX - lastX) * ORBIT_SPEED;
				float dy = (float) (uiY - lastY) * ORBIT_SPEED;
				yaw = Mth.wrapDegrees(yaw + (orbitFlipped ? -dx : dx));
				pitch = Mth.wrapDegrees(pitch + dy);
			}
			lastX = uiX;
			lastY = uiY;
			return true;
		}
		if (panning) {
			panBy(uiX - lastX, uiY - lastY);
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
