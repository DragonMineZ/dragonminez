package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.techniques.KiAttackData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TechniqueCreatorScreen extends ScaledScreen {
	private static final ResourceLocation MENU_NPC = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menunpc.png");
	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");
	private static final NumberFormat COST_NUMBER_FORMAT = NumberFormat.getIntegerInstance(new Locale("es", "ES"));

	private static final int PANEL_W = 345;
	private static final int PANEL_H = 273;
	private static final int BUTTON_W = 74;
	private static final int BUTTON_GAP = 4;
	private static final int GOLD = 0xFFFFD700;
	private static final int DENIED = 0xFFFF6B6B;
	private static final int AFFORDABLE = 0xFF7CFDD6;

	private static final int HOLD_DELAY = 5;
	private static final int HOLD_INTERVAL = 2;

	private final Screen parent;
	private final TechniqueDraft draft;

	private int panelX, panelY;
	private Runnable heldAdjuster;
	private int holdTicks;

	private EditBox nameField;
	private CustomTextureButton utilityLeft, utilityRight;
	private CustomTextureButton sizeLeft, sizeRight;
	private CustomTextureButton speedLeft, speedRight;
	private CustomTextureButton armorLeft, armorRight;
	private TexturedTextButton createButton;

	public TechniqueCreatorScreen(Screen parent, TechniqueDraft draft) {
		super(Component.translatable("gui.dragonminez.skills.creator.title"));
		this.parent = parent;
		this.draft = draft;
	}

	@Override
	protected int getMinGuiWidth() {
		return 365;
	}

	@Override
	protected int getMinGuiHeight() {
		return 293;
	}

	@Override
	protected void init() {
		super.init();
		draft.recompute();
		clearWidgets();

		panelX = (getUiWidth() - PANEL_W) / 2;
		panelY = (getUiHeight() - PANEL_H) / 2;

		int r1 = panelY + 32;
		int r2 = panelY + 46;

		nameField = new EditBox(this.font, panelX + 138, panelY + 30, 70, 12, Component.empty());
		nameField.setMaxLength(24);
		nameField.setValue(draft.getName());
		nameField.setResponder(draft::setName);
		addRenderableWidget(nameField);

		utilityLeft = createArrowButton(panelX + 16, r2 + 2, true, btn -> draft.toggleUtility());
		utilityRight = createArrowButton(panelX + 118, r2 + 2, false, btn -> draft.toggleUtility());
		addRenderableWidget(utilityLeft);
		addRenderableWidget(utilityRight);

		int beLeft = panelX + 18;
		int beRight = panelX + 150;
		addRenderableWidget(arrow(beLeft, panelY + 128, true, () -> draft.adjustDamage(false, hasShiftDown())));
		addRenderableWidget(arrow(beRight, panelY + 128, false, () -> draft.adjustDamage(true, hasShiftDown())));

		sizeLeft = arrow(beLeft, panelY + 148, true, () -> draft.adjustSize(false, hasShiftDown()));
		sizeRight = arrow(beRight, panelY + 148, false, () -> draft.adjustSize(true, hasShiftDown()));
		addRenderableWidget(sizeLeft);
		addRenderableWidget(sizeRight);

		speedLeft = arrow(beLeft, panelY + 168, true, () -> draft.adjustSpeed(false, hasShiftDown()));
		speedRight = arrow(beRight, panelY + 168, false, () -> draft.adjustSpeed(true, hasShiftDown()));
		addRenderableWidget(speedLeft);
		addRenderableWidget(speedRight);

		armorLeft = arrow(beLeft, panelY + 188, true, () -> draft.adjustArmorPen(false, hasShiftDown()));
		armorRight = arrow(beRight, panelY + 188, false, () -> draft.adjustArmorPen(true, hasShiftDown()));
		addRenderableWidget(armorLeft);
		addRenderableWidget(armorRight);

		int seLeft = panelX + 192;
		int seRight = panelX + 326;
		addRenderableWidget(createArrowButton(seLeft, panelY + 128 + 2, true, btn -> draft.cycleSecondaryType()));
		addRenderableWidget(createArrowButton(seRight, panelY + 128 + 2, false, btn -> draft.cycleSecondaryType()));
		addRenderableWidget(createArrowButton(seLeft, panelY + 148 + 2, true, btn -> draft.cycleAffectedStat(-1)));
		addRenderableWidget(createArrowButton(seRight, panelY + 148 + 2, false, btn -> draft.cycleAffectedStat(1)));
		addRenderableWidget(arrow(seLeft, panelY + 168, true, () -> draft.adjustIntensity(false, hasShiftDown())));
		addRenderableWidget(arrow(seRight, panelY + 168, false, () -> draft.adjustIntensity(true, hasShiftDown())));
		addRenderableWidget(arrow(seLeft, panelY + 188, true, () -> draft.adjustDuration(false, hasShiftDown())));
		addRenderableWidget(arrow(seRight, panelY + 188, false, () -> draft.adjustDuration(true, hasShiftDown())));

		int btnY = getUiHeight() - 28;
		int buttonsX = panelX + (PANEL_W - (BUTTON_W * 3 + BUTTON_GAP * 2)) / 2;
		addRenderableWidget(createButton(buttonsX, btnY, tr("gui.dragonminez.skills.creator.back"), btn -> goBack()));
		createButton = createButton(buttonsX + BUTTON_W + BUTTON_GAP, btnY, tr("gui.dragonminez.skills.create_skill"), btn -> createSkill());
		addRenderableWidget(createButton);
		addRenderableWidget(createButton(buttonsX + (BUTTON_W + BUTTON_GAP) * 2, btnY, tr("gui.dragonminez.hair_editor.cancel"), btn -> onClose()));

		updateWidgetStates();
	}

	private TexturedTextButton createButton(int x, int y, MutableComponent message, TexturedTextButton.OnPress onPress) {
		return new TexturedTextButton.Builder()
				.position(x, y)
				.size(BUTTON_W, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(BUTTON_W, 20)
				.message(message)
				.onPress(onPress)
				.build();
	}

	private CustomTextureButton arrow(int x, int textY, boolean left, Runnable repeatable) {
		return createArrowButton(x, textY + 2, left, btn -> beginHold(repeatable));
	}

	private CustomTextureButton createArrowButton(int x, int y, boolean left, CustomTextureButton.OnPress onPress) {
		return new CustomTextureButton.Builder()
				.position(x, y - 4)
				.size(10, 15)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(left ? 32 : 20, 0, left ? 32 : 20, 14)
				.textureSize(8, 14)
				.message(Component.empty())
				.onPress(onPress)
				.build();
	}

	private void beginHold(Runnable action) {
		this.heldAdjuster = action;
		this.holdTicks = 0;
		action.run();
	}

	private void updateWidgetStates() {
		KiAttackData.KiType type = draft.getType();
		boolean utility = draft.allowsUtility();
		utilityLeft.visible = utility;
		utilityLeft.active = utility;
		utilityRight.visible = utility;
		utilityRight.active = utility;

		boolean useSize = KiAttackData.usesCustomSize(type);
		sizeLeft.visible = useSize;
		sizeRight.visible = useSize;

		boolean useSpeed = KiAttackData.usesCustomSpeed(type);
		speedLeft.visible = useSpeed;
		speedRight.visible = useSpeed;

		boolean useArmor = KiAttackData.usesCustomArmorPen(type);
		armorLeft.visible = useArmor;
		armorRight.visible = useArmor;

		createButton.active = draft.canAfford();
	}

	@Override
	public void tick() {
		super.tick();
		nameField.tick();
		if (heldAdjuster != null) {
			holdTicks++;
			if (holdTicks >= HOLD_DELAY && (holdTicks - HOLD_DELAY) % HOLD_INTERVAL == 0) heldAdjuster.run();
		}
		updateWidgetStates();
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		heldAdjuster = null;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	private void createSkill() {
		if (!draft.canAfford()) return;
		if (draft.isDuplicateName()) {
			if (this.minecraft != null && this.minecraft.player != null) {
				this.minecraft.player.displayClientMessage(tr("gui.dragonminez.skills.creator.duplicate", draft.resolveName()), true);
			}
			return;
		}
		NetworkHandler.INSTANCE.sendToServer(draft.toPacket());
		onClose();
	}

	private void goBack() {
		if (this.minecraft != null) this.minecraft.setScreen(new TechniqueStyleScreen(this.parent, this.draft));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(graphics);
		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));
		beginUiScale(graphics);

		HudRender.blit(graphics, MENU_NPC, panelX, panelY, 0, 0, PANEL_W, PANEL_H, 512, 512);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.skills.creator.title"), panelX + PANEL_W / 2, panelY + 12, GOLD);

		renderHeader(graphics);
		renderBaseEffects(graphics);
		renderSecondaryEffects(graphics);

		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 400.0D);
		super.render(graphics, uiMouseX, uiMouseY, partialTick);
		graphics.pose().popPose();

		if (!createButton.active && createButton.isHovered()) renderLockedTooltip(graphics, uiMouseX, uiMouseY);
		else renderEffectTooltip(graphics, uiMouseX, uiMouseY);

		endUiScale(graphics);
	}

	private void renderHeader(GuiGraphics graphics) {
		int r1 = panelY + 32;
		int r2 = panelY + 46;
		int r3 = panelY + 58;
		int r4 = panelY + 70;
		int leftCx = panelX + 72;
		int centerCx = panelX + 172;
		int rightCx = panelX + 274;

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.skills.creator.type").append(": ").append(tr(draft.typeKey())),
				leftCx, r1, 0xFFFFFFFF);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.skills.creator.utility").append(": ").append(tr("technique.utility." + draft.getEffectiveUtility().name().toLowerCase(Locale.ROOT))),
				leftCx, r2, draft.allowsUtility() ? 0xFFFFFFFF : 0xFF777777);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.skills.creator.ki_cost_label").append(" ").append(txt(COST_NUMBER_FORMAT.format(draft.getKiCost()))),
				centerCx, r2, 0xFFDDDDDD);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.technique.cooldown").append(": ").append(txt(String.valueOf(draft.getCooldown()))),
				centerCx, r3, 0xFFDDDDDD);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.skills.creator.tp_cost_label").append(" ").append(txt(COST_NUMBER_FORMAT.format(draft.getTpCost()))),
				centerCx, r4, draft.canAfford() ? 0xFFDDDDDD : DENIED);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.skills.creator.available_tp"), rightCx, r1, 0xFFCCCCCC);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(COST_NUMBER_FORMAT.format(Math.floor(draft.availableTrainingPoints()))),
				rightCx, r2, draft.canAfford() ? AFFORDABLE : DENIED);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.skills.creator.colors"), rightCx, r3, 0xFFCCCCCC);
		TechniqueDraft.ColorTarget[] targets = TechniqueDraft.ColorTarget.values();
		int swatch = 9;
		int gap = 5;
		int startX = rightCx - (targets.length * swatch + (targets.length - 1) * gap) / 2;
		for (int i = 0; i < targets.length; i++) {
			int sx = startX + i * (swatch + gap);
			int sy = r4 - 1;
			HudRender.rect(graphics, sx - 1, sy - 1, swatch + 2, swatch + 2, 0xFF000000);
			HudRender.rect(graphics, sx, sy, swatch, swatch, 0xFF000000 | draft.getColor(targets[i]));
		}
	}

	private void renderBaseEffects(GuiGraphics graphics) {
		int cx = panelX + 84;
		KiAttackData.KiType type = draft.getType();
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.base_effects"), cx, panelY + 108, GOLD);

		int damagePercent = Math.round(draft.getDamage() * 100.0f);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.technique.damage").append(": ").append(txt(damagePercent + "%")),
				cx, panelY + 128, 0xFFFFFFFF);

		int sizeColor = KiAttackData.usesCustomSize(type) ? 0xFFFFFFFF : 0xFF777777;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.technique.size").append(": ").append(txt(String.format(Locale.US, "%.1f", draft.getSize()))),
				cx, panelY + 148, sizeColor);

		int speedColor = KiAttackData.usesCustomSpeed(type) ? 0xFFFFFFFF : 0xFF777777;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.technique.speed").append(": ").append(txt(String.format(Locale.US, "%.1f", draft.getSpeed()))),
				cx, panelY + 168, speedColor);

		int armorColor = KiAttackData.usesCustomArmorPen(type) ? 0xFFFFFFFF : 0xFF777777;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.technique.armor_pen").append(": ").append(txt(draft.getArmorPen() + "%")),
				cx, panelY + 188, armorColor);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.technique.cast_time").append(": ").append(txt(String.valueOf(draft.getCast() / 20)).append("s")),
				cx, panelY + 208, 0xFFAACCFF);
	}

	private void renderSecondaryEffects(GuiGraphics graphics) {
		int cx = panelX + 259;
		boolean hasSecondary = draft.hasSecondaryEffect();
		int active = 0xFFFFFFFF;
		int inactive = 0xFF777777;

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.secondary_effects"), cx, panelY + 108, GOLD);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.technique.effect_type").append(": ").append(tr("gui.dragonminez.technique.effect_type." + draft.getSecondaryType().name().toLowerCase(Locale.ROOT))),
				cx, panelY + 128, active);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.technique.affected_stat").append(": ").append(tr("gui.dragonminez.technique.affected_stat." + draft.getAffectedStat().name().toLowerCase(Locale.ROOT))),
				cx, panelY + 148, hasSecondary ? active : inactive);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.technique.intensity").append(": ").append(txt(draft.getSecondaryIntensity() + "%")),
				cx, panelY + 168, hasSecondary ? active : inactive);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.technique.duration").append(": ").append(txt(draft.getSecondaryDuration() + "s")),
				cx, panelY + 188, hasSecondary ? active : inactive);
	}

	private void renderLockedTooltip(GuiGraphics graphics, int uiMouseX, int uiMouseY) {
		List<Component> lines = new ArrayList<>();
		lines.add(tr("gui.dragonminez.skills.creator.not_enough_tp",
				COST_NUMBER_FORMAT.format(Math.floor(draft.availableTrainingPoints())),
				COST_NUMBER_FORMAT.format(draft.getTpCost())));
		TextUtil.renderAdvancedTooltip(graphics, this.font, uiMouseX, uiMouseY, getUiWidth(), getUiHeight(),
				tr("gui.dragonminez.skills.create_skill"), lines, null, DENIED);
	}

	private void renderEffectTooltip(GuiGraphics graphics, int uiMouseX, int uiMouseY) {
		int rowY = panelY + 128;
		boolean hovering = uiMouseX >= panelX + 30 && uiMouseX <= panelX + 150
				&& uiMouseY >= rowY - 2 && uiMouseY <= rowY + 9;
		if (!hovering) return;

		boolean heal = draft.getEffectiveUtility() == KiAttackData.Utility.HEAL;
		String valueKey = heal ? "gui.dragonminez.technique.effect.tooltip.heal" : "gui.dragonminez.technique.effect.tooltip.damage";

		List<Component> desc = new ArrayList<>();
		desc.add(tr(valueKey, String.format(Locale.US, "%.1f", draft.damageOutput())));
		desc.add(tr("gui.dragonminez.technique.effect.tooltip.desc"));

		TextUtil.renderAdvancedTooltip(graphics, this.font, uiMouseX, uiMouseY, getUiWidth(), getUiHeight(),
				tr("gui.dragonminez.technique.damage"), desc, null, GOLD);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 256) {
			onClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) this.minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
