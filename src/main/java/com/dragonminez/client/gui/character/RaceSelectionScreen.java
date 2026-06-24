package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.events.ForgeClientEvents;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.hair.HairManager;
import com.dragonminez.common.network.C2S.StatsSyncC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class RaceSelectionScreen extends ScaledScreen {

	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/buttons/characterbuttons.png");
	private static final ResourceLocation MENU_BIG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/menu/menubig.png");

	private final Map<String, PanoramaRenderer> panoramaCache = new HashMap<>();
	private PanoramaRenderer currentPanorama;
	private PanoramaRenderer previousPanorama;
	private float panoramaFade = 1.0f;
	private float carouselAnim = 0.0f;

	protected static boolean GLOBAL_SWITCHING = false;

	private final Character character;
	private int selectedRaceIndex = 0;
	private boolean isSwitchingMenu = false;

	private float playerRotation = 180.0f;
	private boolean isDraggingModel = false;
	private double lastMouseX = 0;

	private CustomTextureButton leftButton;
	private CustomTextureButton rightButton;
	private TexturedTextButton selectButton;

	private enum TransitionState { NONE, OPENING, CLOSING }
	private static final long OPEN_ANIMATION_DURATION = 200;
	private static final long CLOSE_ANIMATION_DURATION = 120;
	private long animationStartTime;
	private TransitionState transitionState = TransitionState.NONE;
	private Screen pendingScreen;
	private boolean closeCommitted;

	public RaceSelectionScreen(Character character) {
		super(Component.translatable("gui.dragonminez.character_creation.title"));
		this.character = character;
		List<String> races = getAvailableRaces();
		for (int i = 0; i < races.size(); i++) {
			if (races.get(i).equals(character.getRace())) {
				selectedRaceIndex = i;
				break;
			}
		}
	}

	private List<String> getAvailableRaces() {
		return ConfigManager.getLoadedRaces();
	}

	private PanoramaRenderer getPanorama(String raceName) {
		return panoramaCache.computeIfAbsent(raceName, k -> {
			ResourceLocation testLoc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/background/" + k + "_panorama_0.png");
			boolean exists = false;
			if (Minecraft.getInstance().getResourceManager() != null) exists = Minecraft.getInstance().getResourceManager().getResource(testLoc).isPresent();

			ResourceLocation baseLoc = exists
					? ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/background/" + k + "_panorama")
					: ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/background/roshi");

			return new PanoramaRenderer(new CubeMap(baseLoc));
		});
	}

	@Override
	protected void init() {
		super.init();
		startOpenTransition();

		List<String> races = getAvailableRaces();
		if (!races.isEmpty()) {
			currentPanorama = getPanorama(races.get(selectedRaceIndex));
			previousPanorama = currentPanorama;
		}

		int centerX = getUiWidth() / 2;
		int centerY = getUiHeight() / 2;

		leftButton = new CustomTextureButton.Builder()
				.position(centerX - 60 - 25, centerY + 88)
				.size(20, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(32, 0, 32, 14)
				.textureSize(8, 14)
				.onPress(btn -> previousRace())
				.build();

		rightButton = new CustomTextureButton.Builder()
				.position(centerX - 60 + 138, centerY + 88)
				.size(20, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(20, 0, 20, 14)
				.textureSize(8, 14)
				.onPress(btn -> nextRace())
				.build();

		selectButton = new TexturedTextButton.Builder()
				.position(getUiWidth() - 85, getUiHeight() - 25)
				.size(74, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(tr("gui.dragonminez.customization.select"))
				.onPress(btn -> selectRace())
				.build();

		addRenderableWidget(leftButton);
		addRenderableWidget(rightButton);
		addRenderableWidget(selectButton);
	}

	@Override
	public void tick() {
		super.tick();

		if (transitionState == TransitionState.OPENING && getTransitionProgress() >= 1.0f) transitionState = TransitionState.NONE;
		if (transitionState != TransitionState.CLOSING || closeCommitted) return;
		if (getTransitionProgress() >= 1.0f) {
			closeCommitted = true;
			if (this.minecraft != null) this.minecraft.setScreen(pendingScreen);
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		float tickDelta = Minecraft.getInstance().getDeltaFrameTime();

		if (panoramaFade < 1.0f) panoramaFade = Math.min(1.0f, panoramaFade + (tickDelta * 0.05f));

		if (Math.abs(carouselAnim) > 0.001f) carouselAnim = Mth.lerp(tickDelta * 0.35f, carouselAnim, 0.0f);
		else carouselAnim = 0.0f;

		renderPanorama(graphics, partialTick);
		this.renderCinematicBars(graphics);

		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));

		beginUiScale(graphics);

		renderCarouselElements(graphics, uiMouseX, uiMouseY);

		super.render(graphics, uiMouseX, uiMouseY, partialTick);

		renderRaceInfo(graphics);
		renderRacialInfo(graphics);

		endUiScale(graphics);
	}

	private void renderCarouselElements(GuiGraphics graphics, int mouseX, int mouseY) {
		List<String> races = getAvailableRaces();
		if (races.isEmpty()) return;

		int centerX = getUiWidth() / 2;
		int centerY = getUiHeight() / 2 + 92;
		int modelBaseY = getUiHeight() / 2 + 70;

		String originalRace = races.get(selectedRaceIndex);

		List<Integer> carouselIndices = new ArrayList<>(List.of(-2, -1, 0, 1, 2));
		carouselIndices.sort((a, b) -> {
			float absA = Math.abs(a + carouselAnim);
			float absB = Math.abs(b + carouselAnim);
			return Float.compare(absB, absA);
		});

		for (int i : carouselIndices) {
			float visualPos = i + carouselAnim;
			float absPos = Math.abs(visualPos);

			if (absPos > 1.5f) continue;

			float scale = Math.max(0.5f, 1.0f - (absPos * 0.35f));
			float alpha = Math.max(0.0f, 1.0f - (absPos * 0.6f));

			if (alpha <= 0.05f) continue;

			int raceIndex = (selectedRaceIndex + i) % races.size();
			if (raceIndex < 0) raceIndex += races.size();
			String raceName = races.get(raceIndex);

			float xOffset = visualPos * 130f;
			applyRaceDefaults(raceName);
			int modelX = (int) (centerX + 5 + xOffset);

			RenderSystem.setShaderColor(alpha, alpha, alpha, 1.0f);
			renderPlayerModel(graphics, modelX, modelBaseY, (int)(75 * scale), mouseX, mouseY);
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

			graphics.pose().pushPose();
			graphics.pose().translate(centerX + xOffset, centerY, 0);
			graphics.pose().scale(scale, scale, 1.0f);

			RenderSystem.enableBlend();
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
			graphics.blit(MENU_BIG, -74, -7, 0, 215, 149, 21);
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			RenderSystem.disableBlend();

			int alphaHex = (int)(alpha * 255) << 24;
			int color = (0x00FFFFFF & 0xFF7CFDD6) | alphaHex;
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("race." + Reference.MOD_ID + "." + raceName), 0, 0, color);

			graphics.pose().popPose();
		}

		applyRaceDefaults(originalRace);
	}

	private void renderCinematicBars(GuiGraphics guiGraphics) {
		int totalBarHeight = (int) (this.height * 0.12);
		int fadeSize = 60;
		if (totalBarHeight <= fadeSize) totalBarHeight = fadeSize + 1;

		int solidHeight = totalBarHeight - fadeSize;
		int colorSolid = 0xFF000000;
		int colorTransparent = 0x00000000;

		guiGraphics.fill(0, 0, this.width, solidHeight, colorSolid);
		guiGraphics.fillGradient(0, solidHeight, this.width, solidHeight + fadeSize, colorSolid, colorTransparent);
		int bottomBarStartY = this.height - totalBarHeight;
		guiGraphics.fillGradient(0, bottomBarStartY, this.width, bottomBarStartY + fadeSize, colorTransparent, colorSolid);
		guiGraphics.fill(0, bottomBarStartY + fadeSize, this.width, this.height, colorSolid);
	}

	private void renderPanorama(GuiGraphics graphics, float partialTick) {
		if (previousPanorama != null && panoramaFade < 1.0f) {
			previousPanorama.render(partialTick, 1.0f);
			if (currentPanorama != null) currentPanorama.render(partialTick, panoramaFade);
		} else if (currentPanorama != null) currentPanorama.render(partialTick, 1.0f);
	}

	private void renderRaceInfo(GuiGraphics graphics) {
		List<String> races = getAvailableRaces();
		if (races.isEmpty()) return;
		if (selectedRaceIndex >= races.size()) selectedRaceIndex = 0;
		String currentRace = races.get(selectedRaceIndex);

		int uiHeight = getUiHeight();

		int panelWidth = 130;
		int marginFromEdge = 10;
		int boxStartX = marginFromEdge;
		int centerX = boxStartX + (panelWidth / 2);
		int startY = (uiHeight / 2) - 50;

		String rawName = Component.translatable("race." + Reference.MOD_ID + "." + currentRace).getString();
		String boldAndColoredName = "\u00A7l" + rawName.replaceAll("(?i)(\u00A7[0-9a-fr])", "$1\u00A7l");
		Component raceName = txt(boldAndColoredName);

		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 400.0D);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, raceName, centerX, startY - 12, 0xFF7CFDD6);

		Component description = tr("race." + Reference.MOD_ID + "." + currentRace + ".desc");
		List<String> wrappedDesc = wrapText(description.getString(), panelWidth);

		int textY = startY;
		for (String line : wrappedDesc) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(line), centerX, textY, 0xFFCCCCCC);
			textY += 12;
		}

		graphics.pose().popPose();
	}

	private void renderRacialInfo(GuiGraphics graphics) {
		List<String> races = getAvailableRaces();
		if (races.isEmpty()) return;
		if (selectedRaceIndex >= races.size()) selectedRaceIndex = 0;
		String currentRace = races.get(selectedRaceIndex);

		if (ConfigManager.getRaceCharacter(currentRace) == null) return;
		GeneralServerConfig.RacialSkillsConfig config = ConfigManager.getServerConfig().getRacialSkills();
		String racialSkill = ConfigManager.getRaceCharacter(currentRace).getRacialSkill();
		if (racialSkill == null || racialSkill.isEmpty()) return;

		String titleKey = "skill.dragonminez.racial_" + racialSkill;
		String descKey = "skill.dragonminez.racial_" + racialSkill + ".desc";

		Component titleComp = tr(titleKey);
		String description = "";

		switch (racialSkill) {
			case "human" -> {
				int regen = (int) Math.round((config.getHumanKiRegenBoost() - 1.0) * 100);
				description = tr(descKey, regen).getString();
			}
			case "saiyan" -> {
				int zenkaiHealth = (int) Math.round(config.getSaiyanZenkaiHealthRegen() * 100);
				int zenkaiStat = (int) Math.round(config.getSaiyanZenkaiStatBoost() * 100);
				int cooldown = config.getSaiyanZenkaiCooldownSeconds();
				description = tr(descKey, zenkaiHealth, zenkaiStat, cooldown).getString();
			}
			case "namekian" -> {
				int assimHealth = (int) Math.round(config.getNamekianAssimilationHealthRegen() * 100);
				int assimStat = (int) Math.round(config.getNamekianAssimilationStatBoost() * 100);
				description = tr(descKey, assimHealth, assimStat).getString();
			}
			case "frostdemon" -> {
				int tpBoost = (int) Math.round((config.getFrostDemonTPBoost() - 1.0) * 100);
				description = tr(descKey, tpBoost).getString();
			}
			case "bioandroid" -> {
				int drainRatio = (int) Math.round(config.getBioAndroidDrainRatio() * 100);
				int cooldown = config.getBioAndroidCooldownSeconds();
				description = tr(descKey, drainRatio, cooldown).getString();
			}
			case "majin" -> {
				int absHealth = (int) Math.round(config.getMajinAbsorptionHealthRegen() * 100);
				int absStat = (int) Math.round(config.getMajinAbsorptionStatCopy() * 100);
				description = tr(descKey, absHealth, absStat).getString();
			}
			default -> description = tr(descKey).getString();
		}

		int uiWidth = getUiWidth();
		int uiHeight = getUiHeight();

		int panelWidth = 130;
		int marginFromEdge = 68;
		int boxStartX = uiWidth - marginFromEdge - panelWidth;
		int centerX = boxStartX + (panelWidth / 2);
		int startY = (uiHeight / 2) - 50;

		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 400.0D);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, titleComp.copy().withStyle(ChatFormatting.BOLD), centerX + 60, startY - 12, 0xFF55FF55);
		List<String> wrappedDesc = wrapText(description, panelWidth);
		int textY = startY;

		for (String line : wrappedDesc) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(line), centerX + 60, textY, 0xFFCCCCCC);
			textY += 12;
		}

		graphics.pose().popPose();
	}

	private void renderPlayerModel(GuiGraphics graphics, int x, int y, int scale, float mouseX, float mouseY) {
		LivingEntity player = Minecraft.getInstance().player;
		if (player == null) return;
		int adjustedScale = getAdjustedModelScale(scale);
		Quaternionf pose = (new Quaternionf()).rotateZ((float)Math.PI);
		Quaternionf cameraOrientation = (new Quaternionf()).rotateX(0);
		pose.mul(cameraOrientation);

		float yBodyRotO = player.yBodyRot;
		float yRotO = player.getYRot();
		float xRotO = player.getXRot();
		float yHeadRotO = player.yHeadRotO;
		float yHeadRot = player.yHeadRot;

		player.yBodyRot = playerRotation;
		player.setYRot(playerRotation);
		player.setXRot(0);
		player.yHeadRot = playerRotation;
		player.yHeadRotO = playerRotation;

		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 150.0D);
		InventoryScreen.renderEntityInInventory(graphics, x, y, adjustedScale, pose, cameraOrientation, player);
		graphics.pose().popPose();

		player.yBodyRot = yBodyRotO;
		player.setYRot(yRotO);
		player.setXRot(xRotO);
		player.yHeadRotO = yHeadRotO;
		player.yHeadRot = yHeadRot;
	}

	protected int getAdjustedModelScale(int baseScale) {
		var player = Minecraft.getInstance().player;
		if (player == null) return baseScale;

		final float[] inverseScale = {1.0f};
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			var character = stats.getCharacter();

			Float[] resolved = character.getResolvedModelScaling();
			float currentScale = (resolved[0] + resolved[1]) / 2.0f;

			if (currentScale > 1.0f) inverseScale[0] = 0.9375f / currentScale;
		});

		return (int)(baseScale * inverseScale[0]);
	}

	private List<String> wrapText(String text, int maxWidth) {
		List<String> lines = new ArrayList<>();
		String[] words = text.split(" ");
		StringBuilder currentLine = new StringBuilder();

		for (String word : words) {
			String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
			if (this.font.width(testLine) <= maxWidth) {
				if (!currentLine.isEmpty()) currentLine.append(" ");
				currentLine.append(word);
			} else {
				if (!currentLine.isEmpty()) lines.add(currentLine.toString());
				currentLine = new StringBuilder(word);
			}
		}

		if (!currentLine.isEmpty()) lines.add(currentLine.toString());
		return lines;
	}

	private void applyRaceDefaults(String race) {
		character.setRace(race);
		RaceCharacterConfig config = ConfigManager.getRaceCharacter(race);
		if (config != null) {
			character.setBodyColor(config.getDefaultBodyColor());
			character.setBodyColor2(config.getDefaultBodyColor2());
			character.setBodyColor3(config.getDefaultBodyColor3());
			character.setHairColor(config.getDefaultHairColor());
			character.setEye1Color(config.getDefaultEye1Color());
			character.setEye2Color(config.getDefaultEye2Color());
			character.setAuraColor(config.getDefaultAuraColor());
			character.setBodyType(config.getDefaultBodyType());
			character.setHairId(config.getDefaultHairType());

			if (HairManager.canUseHair(character)) {
				character.setActiveHeadBone("hair");
				character.setRenderHairBase(true);
			} else if (config.getHeadBones() != null && config.getHeadBones().length > 0) {
				String firstExtraBone = "";
				if (character.areExtraHeadBonesEnabled()) {
					for (String bone : config.getHeadBones()) {
						if (bone != null && !bone.isEmpty() && !bone.equals("hair")) {
							firstExtraBone = bone;
							break;
						}
					}
				}
				character.setActiveHeadBone(firstExtraBone);
			} else {
				character.setActiveHeadBone("");
			}

			character.setEyesType(config.getDefaultEyesType());
			character.setNoseType(config.getDefaultNoseType());
			character.setMouthType(config.getDefaultMouthType());
			character.setTattooType(config.getDefaultTattooType());
		}
	}

	private void updateCharacterRace() {
		List<String> races = getAvailableRaces();
		if (races.isEmpty()) return;
		applyRaceDefaults(races.get(selectedRaceIndex));
		NetworkHandler.sendToServer(new StatsSyncC2S(character));
	}

	private void previousRace() {
		List<String> races = getAvailableRaces();
		if (races.isEmpty()) return;

		previousPanorama = currentPanorama;
		panoramaFade = 0.0f;
		carouselAnim = -1.0f;

		selectedRaceIndex = (selectedRaceIndex - 1 + races.size()) % races.size();
		updateCharacterRace();

		currentPanorama = getPanorama(races.get(selectedRaceIndex));
	}

	private void nextRace() {
		List<String> races = getAvailableRaces();
		if (races.isEmpty()) return;

		previousPanorama = currentPanorama;
		panoramaFade = 0.0f;
		carouselAnim = 1.0f;

		selectedRaceIndex = (selectedRaceIndex + 1) % races.size();
		updateCharacterRace();

		currentPanorama = getPanorama(races.get(selectedRaceIndex));
	}

	private void selectRace() {
		List<String> races = getAvailableRaces();
		if (races.isEmpty()) return;
		String selectedRace = races.get(selectedRaceIndex);
		character.setRace(selectedRace);

		if (this.minecraft != null) {
			isSwitchingMenu = true;
			GLOBAL_SWITCHING = true;
			startCloseTransition(new CharacterCustomizationScreen(this, character));
		}

		NetworkHandler.sendToServer(new StatsSyncC2S(character));
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (transitionState == TransitionState.CLOSING) return true;
		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);
		int centerX = getUiWidth() / 2 + 5;
		int centerY = getUiHeight() / 2 + 70;
		int modelRadius = 60;

		if (uiMouseX >= centerX - modelRadius && uiMouseX <= centerX + modelRadius &&
				uiMouseY >= centerY - 100 && uiMouseY <= centerY + 20) {
			isDraggingModel = true;
			lastMouseX = uiMouseX;
			return true;
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (transitionState == TransitionState.CLOSING) return true;
		isDraggingModel = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (transitionState == TransitionState.CLOSING) return true;
		if (isDraggingModel) {
			double uiMouseX = toUiX(mouseX);
			double deltaX = uiMouseX - lastMouseX;
			playerRotation -= (float)(deltaX * 0.8);
			lastMouseX = uiMouseX;
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (transitionState == TransitionState.CLOSING) return true;
		if (keyCode == 256 && this.minecraft != null) {
			if (ConfigManager.getServerConfig().getGameplay().getForceCharacterCreation()) {
				ForgeClientEvents.requestCharacterCreationReopen();
				this.minecraft.setScreen(new PauseScreen(true));
			} else {
				startCloseTransition(null);
			}
			return true;
		}

		if (keyCode == 263) {
			previousRace();
			return true;
		}
		if (keyCode == 262) {
			nextRace();
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		startCloseTransition(null);
	}

	private void startOpenTransition() {
		transitionState = TransitionState.OPENING;
		animationStartTime = System.currentTimeMillis();
		pendingScreen = null;
		closeCommitted = false;
	}

	private void startCloseTransition(Screen nextScreen) {
		if (transitionState == TransitionState.CLOSING) return;
		pendingScreen = nextScreen;
		transitionState = TransitionState.CLOSING;
		animationStartTime = System.currentTimeMillis();
		closeCommitted = false;
	}

	private float getTransitionProgress() {
		long duration = transitionState == TransitionState.CLOSING ? CLOSE_ANIMATION_DURATION : OPEN_ANIMATION_DURATION;
		if (duration <= 0L) return 1.0f;
		long elapsed = System.currentTimeMillis() - animationStartTime;
		return net.minecraft.util.Mth.clamp(elapsed / (float) duration, 0.0f, 1.0f);
	}
}