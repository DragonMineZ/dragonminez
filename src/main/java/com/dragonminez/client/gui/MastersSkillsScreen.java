package com.dragonminez.client.gui;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.ClippableTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.BaseMenuScreen;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.SkillsConfig;
import com.dragonminez.common.network.C2S.UpdateSkillC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.skills.Skill;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class MastersSkillsScreen extends BaseMenuScreen {

	private static final ResourceLocation MENU_BIG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/menu/menubig.png");
	private static final ResourceLocation MENU_SMALL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/menu/menusmall.png");
	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/buttons/characterbuttons.png");
	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");

	private static final int SKILL_ITEM_HEIGHT = 20;
	private static final int MAX_VISIBLE_SKILLS = 8;

	private enum SkillCategory {SKILLS, KI}

	private SkillCategory currentCategory = SkillCategory.SKILLS;

	private StatsData statsData;
	private int tickCount = 0;

	private String selectedSkill = null;
	private float targetScroll = 0;
	private float currentScroll = 0;
	private float maxScroll = 0;

	private ClippableTextureButton skillsButton, kiButton;
	private float buttonRevealProgress = 0.0f;

	private TexturedTextButton purchaseButton;
	private boolean isDraggingScroll = false;

	private final String masterName;
	private final LivingEntity masterEntity;

	public MastersSkillsScreen(String masterName, LivingEntity masterEntity) {
		super(Component.literal(masterName).withStyle(Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth"))));
		this.masterName = masterName;
		this.masterEntity = masterEntity;
	}

	@Override
	protected void init() {
		super.init();
		updateStatsData();
		initDynamicButtons();
	}

	@Override
	protected void initNavigationButtons() {
	}

	@Override
	public void tick() {
		super.tick();
		tickCount++;

		if (tickCount >= 10) {
			tickCount = 0;
			updateStatsData();
			refreshButtons();
		}
	}

	private void updateStatsData() {
		var player = Minecraft.getInstance().player;
		if (player != null) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> this.statsData = data);
		}
	}

	private void initDynamicButtons() {
		int leftPanelX = 12;
		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;

		int buttonY = leftPanelY + 6;
		int hiddenX = leftPanelX + 122;

		int scissorX = leftPanelX + 141;
		int scissorXScreen = toScreenCoord(scissorX);
		int scissorYScreen = toScreenCoord(0);
		int scissorRight = toScreenCoord(getUiWidth());
		int scissorBottom = toScreenCoord(getUiHeight());

		skillsButton = new ClippableTextureButton.Builder()
				.position(hiddenX, buttonY)
				.size(26, 32)
				.texture(MENU_BIG)
				.textureCoords(142, 44, 142, 44)
				.clipping(true, scissorXScreen, scissorYScreen, scissorRight, scissorBottom)
				.onPress(btn -> {
					currentCategory = SkillCategory.SKILLS;
					selectedSkill = null;
					targetScroll = 0;
					currentScroll = 0;
					refreshButtons();
				})
				.build();

		kiButton = new ClippableTextureButton.Builder()
				.position(hiddenX, buttonY + 32)
				.size(26, 32)
				.texture(MENU_BIG)
				.textureCoords(170, 44, 170, 44)
				.clipping(true, scissorXScreen, scissorYScreen, scissorRight, scissorBottom)
				.onPress(btn -> {
					currentCategory = SkillCategory.KI;
					selectedSkill = null;
					targetScroll = 0;
					currentScroll = 0;
					refreshButtons();
				})
				.build();

		this.addRenderableWidget(skillsButton);
		this.addRenderableWidget(kiButton);
	}

	private List<String> getMasterSkills() {
		Map<String, List<String>> skillOfferings = ConfigManager.getSkillsConfig().getSkillOfferings();
		return skillOfferings.getOrDefault(
				masterName.toLowerCase(),
				skillOfferings.get("default")
		);
	}

	private List<String> getVisibleSkillNames() {
		if (statsData == null) return new ArrayList<>();
		List<String> masterOfferings = getMasterSkills();
		List<String> visibleSkills = new ArrayList<>();
		SkillsConfig skillsConfig = ConfigManager.getSkillsConfig();
		String playerRace = getPlayerRaceName();

		switch (currentCategory) {
			case SKILLS:
				for (String skillId : masterOfferings) {
					if (!skillsConfig.isSkillAllowedForRace(skillId, playerRace)) continue;

					if (!skillsConfig.getKiSkills().contains(skillId) && !skillsConfig.getFormSkills().contains(skillId) &&
							!skillsConfig.getStackSkills().contains(skillId)) {
						visibleSkills.add(skillId);
					}
				}
				break;
			case KI:
				for (String skillId : masterOfferings) {
					if (!skillsConfig.isSkillAllowedForRace(skillId, playerRace)) continue;

					if (skillsConfig.getKiSkills().contains(skillId)) {
						visibleSkills.add(skillId);
					}
				}
				break;
		}

		return visibleSkills;
	}

	private String getPlayerRaceName() {
		if (statsData == null || statsData.getCharacter() == null) return "";
		String raceName = statsData.getCharacter().getRaceName();
		return raceName != null ? raceName.toLowerCase(Locale.ROOT) : "";
	}

	private void refreshButtons() {
		this.clearWidgets();
		initDynamicButtons();
		initPurchaseButton();
	}

	private void initPurchaseButton() {
		if (selectedSkill == null || statsData == null) return;
		if (!ConfigManager.getSkillsConfig().isSkillAllowedForRace(selectedSkill, getPlayerRaceName())) return;

		Skill skill = statsData.getSkills().getSkill(selectedSkill);
		if (skill == null) {
			skill = new Skill(selectedSkill, 0, false, 10);
		}

		int rightPanelX = getUiWidth() - 158;
		int centerY = getUiHeight() / 2;
		int rightPanelY = centerY - 105;

		if (!statsData.getSkills().hasSkill(selectedSkill) || skill.getLevel() == 0) {
			int cost = getUpgradeCost(selectedSkill, 0);
			float currentTPS = statsData.getResources().getTrainingPoints();
			boolean canAfford = currentTPS >= cost;
			if (cost == -1 || cost == Integer.MAX_VALUE) return;

			purchaseButton = new TexturedTextButton.Builder()
					.position(rightPanelX + 35, rightPanelY + 196)
					.size(74, 20)
					.texture(BUTTONS_TEXTURE)
					.textureCoords(0, 28, 0, 48)
					.textureSize(74, 20)
					.message(tr("gui.dragonminez.skills.purchase"))
					.onPress(btn -> {
						if (canAfford) {
							NetworkHandler.INSTANCE.sendToServer(new UpdateSkillC2S(UpdateSkillC2S.SkillAction.PURCHASE, selectedSkill, cost));
							updateStatsData();
						}
					})
					.build();

			purchaseButton.active = canAfford;
			this.addRenderableWidget(purchaseButton);
		}
	}

	private int getUpgradeCost(String skillName, int currentLevel) {
		var skillConfig = ConfigManager.getSkillsConfig();
		var skillData = skillConfig.getSkills().get(skillName);

		if (skillData != null && skillData.getCosts() != null) {
			var costs = skillData.getCosts();
			if (currentLevel < costs.size()) {
				return costs.get(currentLevel) != null ? costs.get(currentLevel) : Integer.MAX_VALUE;
			}
		}
		return Integer.MAX_VALUE;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (isNotAnimating()) this.renderBackground(graphics);

		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));

		beginUiScale(graphics);
		applyZoom(graphics);

		updateButtonAnimations(uiMouseX, uiMouseY, partialTick);
		renderMasterEntity(graphics, getUiWidth() / 2 + 5, getUiHeight() / 2 + 90, uiMouseX, uiMouseY);
		renderLeftPanel(graphics, uiMouseX, uiMouseY);
		renderRightPanel(graphics, uiMouseX, uiMouseY);
		super.render(graphics, uiMouseX, uiMouseY, partialTick);
		endUiScale(graphics);
	}

	private void updateButtonAnimations(int mouseX, int mouseY, float partialTick) {
		int leftPanelX = 12;
		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;

		int hiddenX = leftPanelX + 122;
		int visibleX = leftPanelX + 141;
		int buttonWidth = 26;
		int panelWidth = 141;
		int panelHeight = 213;

		int hotZoneX = hiddenX;
		int hotZoneY = leftPanelY + 6;
		int hotZoneWidth = (visibleX - hiddenX) + buttonWidth;
		int hotZoneHeight = 100;

		boolean overPanel = mouseX >= leftPanelX && mouseX < leftPanelX + panelWidth &&
				mouseY >= leftPanelY && mouseY < leftPanelY + panelHeight;
		boolean overHotZone = mouseX >= hotZoneX && mouseX < hotZoneX + hotZoneWidth &&
				mouseY >= hotZoneY && mouseY < hotZoneY + hotZoneHeight;
		boolean shouldReveal = overPanel || overHotZone;

		float step = Math.max(0.01f, 0.07f + (partialTick * 0.01f));
		buttonRevealProgress = approach01(buttonRevealProgress, shouldReveal ? 1.0f : 0.0f, step);
		float animProgress = easeInOutCubic(buttonRevealProgress);

		int newX = hiddenX + (int) ((visibleX - hiddenX) * animProgress);
		skillsButton.setX(newX);
		if (kiButton != null) kiButton.setX(newX);

		int scissorX = leftPanelX + 141;
		int scissorXScreen = toScreenCoord(scissorX);
		int scissorYScreen = toScreenCoord(0);
		int scissorRight = toScreenCoord(getUiWidth());
		int scissorBottom = toScreenCoord(getUiHeight());
		skillsButton.setScissorRect(scissorXScreen, scissorYScreen, scissorRight, scissorBottom);
		if (kiButton != null) kiButton.setScissorRect(scissorXScreen, scissorYScreen, scissorRight, scissorBottom);
	}

	private float approach01(float current, float target, float step) {
		if (current < target) return Math.min(target, current + step);
		if (current > target) return Math.max(target, current - step);
		return current;
	}

	private float easeInOutCubic(float t) {
		if (t <= 0.0f) return 0.0f;
		if (t >= 1.0f) return 1.0f;
		return t < 0.5f ? 4.0f * t * t * t : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 3.0f) / 2.0f;
	}

	private void renderLeftPanel(GuiGraphics graphics, int mouseX, int mouseY) {
		int leftPanelX = 12;
		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		graphics.blit(MENU_BIG, leftPanelX, leftPanelY, 0, 0, 141, 213, 256, 256);
		graphics.blit(MENU_BIG, 29, centerY - 95, 142, 22, 107, 21, 256, 256);

		renderSkillsList(graphics, leftPanelX, leftPanelY, mouseX, mouseY);
	}

	private void renderSkillsList(GuiGraphics graphics, int panelX, int panelY, int mouseX, int mouseY) {
		List<String> skillNames = getVisibleSkillNames();

		int startY = panelY + 30;
		int viewHeight = MAX_VISIBLE_SKILLS * SKILL_ITEM_HEIGHT;
		int totalHeight = skillNames.size() * SKILL_ITEM_HEIGHT;

		maxScroll = Math.max(0, totalHeight - viewHeight);
		targetScroll = Mth.clamp(targetScroll, 0, maxScroll);
		float tickDelta = Minecraft.getInstance().getDeltaFrameTime();
		currentScroll = Mth.lerp(tickDelta * 0.4f, currentScroll, targetScroll);

		graphics.enableScissor(
				toScreenCoord(panelX + 5),
				toScreenCoord(startY),
				toScreenCoord(panelX + 179),
				toScreenCoord(startY + viewHeight)
		);

		graphics.pose().pushPose();
		graphics.pose().translate(0, -currentScroll, 0);

		for (int i = 0; i < skillNames.size(); i++) {
			String skillName = skillNames.get(i);
			int itemY = startY + (i * SKILL_ITEM_HEIGHT);

			if (itemY + SKILL_ITEM_HEIGHT >= startY + currentScroll && itemY <= startY + viewHeight + currentScroll) {
				boolean isSelected = skillName.equals(selectedSkill);
				boolean isHovered = mouseX >= panelX + 10 && mouseX <= panelX + 100 &&
						mouseY >= itemY - currentScroll && mouseY <= itemY + SKILL_ITEM_HEIGHT - currentScroll;

				int color = isSelected ? 0xFFFFAA00 : (isHovered ? 0xFFAAAAAA : 0xFFFFFFFF);

				Skill skill = statsData.getSkills().getSkill(skillName);
				String displayName;
				if (currentCategory == SkillCategory.KI) displayName = Component.translatable("technique.dragonminez." + skillName).getString();
				else displayName = Component.translatable("skill.dragonminez." + skillName).getString();

				TextUtil.drawStringWithBorder(graphics, this.font, txt(displayName), panelX + 15, itemY + 5, color);

				String levelText;
				if (skill != null && skill.getLevel() > 0) {
					levelText = String.valueOf(skill.getLevel());
				} else {
					levelText = "0";
				}

				int levelX = panelX + 130 - this.font.width(levelText);
				TextUtil.drawStringWithBorder(graphics, this.font, txt(levelText), levelX, itemY + 5, color);
			}
		}

		graphics.pose().popPose();
		graphics.disableScissor();

		if (maxScroll > 0) {
			int scrollBarX = panelX + 135;
			graphics.fill(scrollBarX, startY, scrollBarX + 3, startY + viewHeight, 0xFF333333);

			float scrollPercent = currentScroll / maxScroll;
			float visiblePercent = (float) viewHeight / totalHeight;
			int indicatorHeight = Math.max(20, (int) (viewHeight * visiblePercent));
			int indicatorY = startY + (int) ((viewHeight - indicatorHeight) * scrollPercent);

			graphics.fill(scrollBarX, indicatorY, scrollBarX + 3, indicatorY + indicatorHeight, 0xFFAAAAAA);
		}

		String title = "";
		switch (currentCategory) {
			case SKILLS -> title = "gui.dragonminez.skills.tab.skills";
			case KI -> title = "gui.dragonminez.skills.tab.kiattacks";
		}

		TextUtil.drawStringWithBorder(graphics, this.font, tr(title).withStyle(style -> style.withBold(true)), 65, getUiHeight() / 2 - 88, 0xFBC51C);
	}

	private void renderRightPanel(GuiGraphics graphics, int mouseX, int mouseY) {
		int rightPanelX = getUiWidth() - 158;
		int centerY = getUiHeight() / 2;
		int rightPanelY = centerY - 105;

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

		if (currentCategory == SkillCategory.KI) {
			graphics.blit(MENU_BIG, rightPanelX, rightPanelY, 0, 0, 141, 213, 256, 256);
			graphics.blit(MENU_BIG, getUiWidth() - 141, centerY - 95, 142, 22, 107, 21, 256, 256);
		} else {
			graphics.blit(MENU_SMALL, rightPanelX, rightPanelY, 0, 0, 141, 94, 256, 256);
			graphics.blit(MENU_BIG, getUiWidth() - 141, centerY - 95, 142, 22, 107, 21, 256, 256);
			graphics.blit(MENU_SMALL, rightPanelX, rightPanelY + 96, 0, 0, 141, 94, 256, 256);
			graphics.blit(MENU_SMALL, rightPanelX, rightPanelY + 190, 0, 154, 141, 32, 256, 256);
		}

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.info")
				.withStyle(style -> style.withBold(true)), rightPanelX + 70, rightPanelY + 16, 0xFFFFD700);

		if (selectedSkill != null && statsData != null) {
			if (currentCategory == SkillCategory.KI) {
				renderTechniqueDetails(graphics, rightPanelX, rightPanelY);
			} else {
				renderSkillDetails(graphics, rightPanelX, rightPanelY);
			}
		}
	}

	private void renderTechniqueDetails(GuiGraphics graphics, int panelX, int panelY) {
		KiAttackData tech = PredefinedTechniques.REGISTRY.get(selectedSkill);
		if (tech == null) return;

		int yOffset = panelY + 40;

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr(tech.getName()).withStyle(ChatFormatting.BOLD), panelX + 70, yOffset, 0xFFFFFFFF); yOffset += 24;

		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.type").append(": ").append(tr("technique.type." + tech.getKiType().name().toLowerCase())), panelX + 15, yOffset, 0xDDDDDD); yOffset += 12;
		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.utility").append(": ").append(tr("technique.utility." + tech.getUtility().name().toLowerCase())), panelX + 15, yOffset, 0xDDDDDD); yOffset += 12;
		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.damage").append(": ").append(txt(String.format(Locale.US, "%.0f%%", tech.getDamageMultiplier() * 100.0f))), panelX + 15, yOffset, 0xFFFFFF); yOffset += 12;
		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.size").append(": ").append(txt(String.format(Locale.US, "%.1f", tech.getSize()))), panelX + 15, yOffset, 0xFFFFFF); yOffset += 12;
		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.speed").append(": ").append(txt(String.format(Locale.US, "%.1f", tech.getSpeed()))), panelX + 15, yOffset, 0xFFFFFF); yOffset += 12;
		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.armor_pen").append(": ").append(txt(String.valueOf(tech.getArmorPenetration()))), panelX + 15, yOffset, 0xFFFFFF); yOffset += 12;
		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.cast_time").append(": ").append(txt(tech.getCastTime() + "t")), panelX + 15, yOffset, 0xFFFFFF); yOffset += 12;
		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.cooldown").append(": ").append(txt(tech.getCooldown() + "t")), panelX + 15, yOffset, 0xFFFFFF); yOffset += 16;

		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.energy_cost").append(": ").append(txt(String.format(Locale.US, "%.1f", tech.getCalculatedCost(statsData)))), panelX + 15, yOffset, 0xFFAAAA); yOffset += 16;

		boolean learned = statsData.getTechniques().getUnlockedTechniques().containsKey(selectedSkill);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, learned ? tr("gui.dragonminez.skills.already_learned") : tr("gui.dragonminez.skills.not_learned"), panelX + 70, yOffset, learned ? 0xFF55AA55 : 0xFFAAAAAA);

		if (!learned) {
			int tpCost = getUpgradeCost(selectedSkill, 0);
			if (tpCost != Integer.MAX_VALUE && tpCost != -1) {
				TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(tpCost + " TPS"), panelX + 70, yOffset + 12, 0xFFAAAAAA);
			}
		}
	}

	private void renderSkillDetails(GuiGraphics graphics, int panelX, int panelY) {
		Skill skill = statsData.getSkills().getSkill(selectedSkill);
		if (skill == null) skill = new Skill(selectedSkill, 0, false, 10);

		String displayName = Component.translatable("skill.dragonminez." + selectedSkill).getString();
		String description = Component.translatable("skill.dragonminez." + selectedSkill + ".desc").getString();

		int startY = panelY + 40;

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(displayName).withStyle(ChatFormatting.BOLD),
				panelX + 72, startY, 0xFFFFFFFF);

		Component levelComp;
		if (skill.getLevel() > 0) {
			levelComp = tr("gui.dragonminez.skills.level", skill.getLevel(), skill.getMaxLevel());
		} else {
			levelComp = tr("gui.dragonminez.skills.not_learned");
		}

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, levelComp, panelX + 72, startY + 12, 0xFFAAAAAA);

		if (skill.getLevel() == 0) {
			int cost = getUpgradeCost(selectedSkill, 0);
			if (cost != Integer.MAX_VALUE && cost != -1) {
				TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt("%d TPS".formatted(cost)), panelX + 72, startY + 24, 0xFFAAAAAA);
			}
		} else {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font,
					tr("gui.dragonminez.skills.already_learned"),
					panelX + 72, startY + 24, 0xFF55AA55);
		}

		List<String> wrappedDesc = wrapText(description);
		int descY = startY + 70;
		for (String line : wrappedDesc) {
			TextUtil.drawStringWithBorder(graphics, this.font, txt(line), panelX + 13, descY, 0xFFCCCCCC);
			descY += 12;
		}
	}

	private List<String> wrapText(String text) {
		List<String> lines = new ArrayList<>();
		String[] words = text.split(" ");
		StringBuilder currentLine = new StringBuilder();

		for (String word : words) {
			String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
			if (font.width(testLine) <= 130) {
				if (!currentLine.isEmpty()) currentLine.append(" ");
				currentLine.append(word);
			} else {
				if (!currentLine.isEmpty()) {
					lines.add(currentLine.toString());
					currentLine = new StringBuilder(word);
				} else {
					lines.add(word);
				}
			}
		}
		if (!currentLine.isEmpty()) {
			lines.add(currentLine.toString());
		}
		return lines;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);
		int leftPanelX = 12;
		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;

		if (uiMouseX >= leftPanelX && uiMouseX <= leftPanelX + 184 &&
				uiMouseY >= leftPanelY + 40 && uiMouseY <= leftPanelY + 239) {

			int scrollAmount = (int) Math.signum(delta);
			targetScroll = Mth.clamp(targetScroll - (scrollAmount * SKILL_ITEM_HEIGHT * 2), 0, maxScroll);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	private float calculateScrollPercent(double uiMouseY, int startY, int scrollBarHeight) {
		float percent = (float)(uiMouseY - startY) / scrollBarHeight;
		return Mth.clamp(percent, 0.0f, 1.0f);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);
		int leftPanelX = 12;
		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;

		int startY = leftPanelY + 30;
		int viewHeight = MAX_VISIBLE_SKILLS * SKILL_ITEM_HEIGHT;
		int scrollBarX = leftPanelX + 135;

		if (maxScroll > 0 && uiMouseX >= scrollBarX - 5 && uiMouseX <= scrollBarX + 10 &&
				uiMouseY >= startY && uiMouseY <= startY + viewHeight) {
			isDraggingScroll = true;
			targetScroll = calculateScrollPercent(uiMouseY, startY, viewHeight) * maxScroll;
			return true;
		}

		List<String> skillNames = getVisibleSkillNames();
		if (uiMouseX >= leftPanelX + 10 && uiMouseX <= leftPanelX + 100 &&
				uiMouseY >= startY && uiMouseY <= startY + viewHeight) {

			int index = (int) ((uiMouseY - startY + currentScroll) / SKILL_ITEM_HEIGHT);

			if (index >= 0 && index < skillNames.size()) {
				selectedSkill = skillNames.get(index);
				refreshButtons();
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (isDraggingScroll && maxScroll > 0) {
			double uiMouseY = toUiY(mouseY);
			int centerY = getUiHeight() / 2;
			int startY = (centerY - 105) + 30;
			int viewHeight = MAX_VISIBLE_SKILLS * SKILL_ITEM_HEIGHT;

			targetScroll = calculateScrollPercent(uiMouseY, startY, viewHeight) * maxScroll;
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (isDraggingScroll) {
			isDraggingScroll = false;
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	private void renderMasterEntity(GuiGraphics graphics, int x, int y, float mouseX, float mouseY) {
		if (masterEntity == null) return;

		float xRotation = (float) Math.atan((double) ((float) y - mouseY) / 40.0F);
		float yRotation = (float) Math.atan((double) ((float) x - mouseX) / 40.0F);

		Quaternionf pose = (new Quaternionf()).rotateZ((float) Math.PI);
		Quaternionf cameraOrientation = (new Quaternionf()).rotateX(xRotation * 20.0F * ((float) Math.PI / 180F));
		pose.mul(cameraOrientation);

		float yBodyRotO = masterEntity.yBodyRot;
		float yRotO = masterEntity.getYRot();
		float xRotO = masterEntity.getXRot();
		float yHeadRotO = masterEntity.yHeadRotO;
		float yHeadRot = masterEntity.yHeadRot;

		masterEntity.yBodyRot = 180.0F + yRotation * 20.0F;
		masterEntity.setYRot(180.0F + yRotation * 40.0F);
		masterEntity.setXRot(-xRotation * 20.0F);
		masterEntity.yHeadRot = masterEntity.getYRot();
		masterEntity.yHeadRotO = masterEntity.getYRot();

		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 150.0D);
		InventoryScreen.renderEntityInInventory(graphics, x, y, 100, pose, cameraOrientation, masterEntity);
		graphics.pose().popPose();

		masterEntity.yBodyRot = yBodyRotO;
		masterEntity.setYRot(yRotO);
		masterEntity.setXRot(xRotO);
		masterEntity.yHeadRotO = yHeadRotO;
		masterEntity.yHeadRot = yHeadRot;
	}

	public MutableComponent tr(String key, Object... args) {
		return Component.translatable(key, args).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	public MutableComponent txt(String text) {
		return Component.literal(text).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}
}