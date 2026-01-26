package com.dragonminez.client.gui;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.ClippableTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.BaseMenuScreen;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.C2S.UpdateSkillC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class MastersSkillsScreen extends BaseMenuScreen {

	private static final ResourceLocation MENU_BIG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/menu/menubig.png");
	private static final ResourceLocation MENU_SMALL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/menu/menusmall.png");
	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/buttons/characterbuttons.png");

	private static final int SKILL_ITEM_HEIGHT = 20;
	private static final int MAX_VISIBLE_SKILLS = 8;
	private static final int BUTTON_ANIM_TIME = 5;

	private enum SkillCategory { SKILLS, KI }
	private SkillCategory currentCategory = SkillCategory.SKILLS;

	private StatsData statsData;
	private int tickCount = 0;

	private String selectedSkill = null;
	private int scrollOffset = 0;
	private int maxScroll = 0;

	private ClippableTextureButton skillsButton, kiButton;
	private int animTick = 0;
	private boolean isHotZoneHovered = false;

	private TexturedTextButton purchaseButton;

	private final String masterName;
	private final LivingEntity masterEntity;

	public MastersSkillsScreen(int oldGuiScale, String masterName, LivingEntity masterEntity) {
		super(Component.literal(masterName), oldGuiScale);
		this.masterName = masterName;
		this.masterEntity = masterEntity;
	}

	@Override
	protected void init() {
		super.init();
		updateStatsData();
		initDynamicButtons();
		updateSkillsList();
	}

	@Override
	protected void initNavigationButtons() {}

	@Override
	public void tick() {
		super.tick();
		tickCount++;

		if (tickCount >= 10) {
			tickCount = 0;
			updateStatsData();
		}

		if (isHotZoneHovered) {
			if (animTick < BUTTON_ANIM_TIME) animTick++;
		} else {
			if (animTick > 0) animTick--;
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
		int centerY = this.height / 2;
		int leftPanelY = centerY - 105;

		int buttonY = leftPanelY + 6;
		int hiddenX = leftPanelX + 122;
		int scissorX = leftPanelX + 141;

		skillsButton = new ClippableTextureButton.Builder()
				.position(hiddenX, buttonY)
				.size(26, 32)
				.texture(MENU_BIG)
				.textureCoords(142, 44, 142, 44)
				.clipping(true, scissorX, 0, this.width, this.height)
				.onPress(btn -> {
					currentCategory = SkillCategory.SKILLS;
					selectedSkill = null;
					scrollOffset = 0;
					updateSkillsList();
					refreshButtons();
				})
				.build();

		// Botón de KI
        /*
        kiButton = new ClippableTextureButton.Builder()
                .position(hiddenX, buttonY + 32)
                .size(26, 32)
                .texture(MENU_BIG)
                .textureCoords(170, 44, 170, 44)
                .clipping(true, scissorX, 0, this.width, this.height)
                .onPress(btn -> {
                    currentCategory = SkillCategory.KI;
                    selectedSkill = null;
                    scrollOffset = 0;
                    updateSkillsList();
                    refreshButtons();
                })
                .build();
         */

		this.addRenderableWidget(skillsButton);
		// this.addRenderableWidget(kiButton);
	}

	private List<String> getMasterSkills() {
		List<String> skills = new ArrayList<>();

		switch (masterName.toLowerCase()) {
			case "roshi":
				skills.add("jump");
				skills.add("meditation");
				break;
			case "goku":
				skills.add("fly");
				skills.add("kicontrol");
				skills.add("kisense");
				skills.add("fusion");
				break;
			case "kingkai":
				skills.add("kaioken");
				skills.add("potentialunlock");
				skills.add("kimanipulation");
				break;
			default:
				skills.add("jump");
				break;
		}
		return skills;
	}

	private void updateSkillsList() {
		List<String> skillNames = getVisibleSkillNames();
		maxScroll = Math.max(0, skillNames.size() - MAX_VISIBLE_SKILLS);
	}

	private List<String> getVisibleSkillNames() {
		if (statsData == null) return new ArrayList<>();
		List<String> masterOfferings = getMasterSkills();
		List<String> visibleSkills = new ArrayList<>();

		switch (currentCategory) {
			case SKILLS:
				for (String skillId : masterOfferings) {
					if (!skillId.equals("superform") && !skillId.equals("godform")) {
						visibleSkills.add(skillId);
					}
				}
				break;
			case KI:
				// pa futuro w
				break;
		}

		return visibleSkills;
	}

	private void refreshButtons() {
		this.clearWidgets();
		initDynamicButtons();
		initPurchaseButton();
	}

	private void initPurchaseButton() {
		if (selectedSkill == null || statsData == null) return;

		Skill skill = statsData.getSkills().getSkill(selectedSkill);
		if (skill == null) {
			skill = new Skill(selectedSkill, 0, false, 10);
		}

		int rightPanelX = this.width - 158;
		int centerY = this.height / 2;
		int rightPanelY = centerY - 105;

		if (!statsData.getSkills().hasSkill(selectedSkill) || skill.getLevel() == 0) {
			int cost = getUpgradeCost(selectedSkill, 0);
			int currentTPS = statsData.getResources().getTrainingPoints();
			boolean canAfford = currentTPS >= cost && cost != Integer.MAX_VALUE && cost != -1;

			purchaseButton = new TexturedTextButton.Builder()
					.position(rightPanelX + 35, rightPanelY + 196)
					.size(74, 20)
					.texture(BUTTONS_TEXTURE)
					.textureCoords(0, 28, 0, 48)
					.textureSize(74, 20)
					.message(Component.translatable("gui.dragonminez.skills.purchase"))
					.onPress(btn -> {
						if (canAfford) {
							NetworkHandler.INSTANCE.sendToServer(new UpdateSkillC2S("purchase", selectedSkill, cost));
							updateStatsData();
							refreshButtons();
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
				return costs.get(currentLevel);
			}
		}
		return Integer.MAX_VALUE;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (!isAnimating()) this.renderBackground(graphics);
		PoseStack pose = graphics.pose();
		pose.pushPose();
		applyZoom(graphics);

		updateButtonAnimations(mouseX, mouseY, partialTick);
		renderMasterEntity(graphics, this.width / 2 + 5, this.height / 2 + 90, mouseX, mouseY);
		renderLeftPanel(graphics, mouseX, mouseY);
		renderRightPanel(graphics, mouseX, mouseY);
		super.render(graphics, mouseX, mouseY, partialTick);
		pose.popPose();
	}

	private void updateButtonAnimations(int mouseX, int mouseY, float partialTick) {
		int leftPanelX = 12;
		int centerY = this.height / 2;
		int leftPanelY = centerY - 105;

		int hotZoneX = leftPanelX + 122;
		int hotZoneY = leftPanelY + 6;
		int hotZoneWidth = 48;
		int hotZoneHeight = 100;

		isHotZoneHovered = mouseX >= hotZoneX && mouseX < hotZoneX + hotZoneWidth &&
				mouseY >= hotZoneY && mouseY < hotZoneY + hotZoneHeight;

		float animProgress = (animTick + (isHotZoneHovered ? partialTick : -partialTick)) / BUTTON_ANIM_TIME;
		animProgress = Mth.clamp(animProgress, 0.0f, 1.0f);

		int hiddenX = leftPanelX + 122;
		int visibleX = leftPanelX + 141;

		int newX = hiddenX + (int)((visibleX - hiddenX) * animProgress);
		skillsButton.setX(newX);
		if (kiButton != null) kiButton.setX(newX);
	}

	private void renderLeftPanel(GuiGraphics graphics, int mouseX, int mouseY) {
		int leftPanelX = 12;
		int centerY = this.height / 2;
		int leftPanelY = centerY - 105;

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		graphics.blit(MENU_BIG, leftPanelX, leftPanelY, 0, 0, 141, 213, 256, 256);
		graphics.blit(MENU_BIG, 29, centerY - 95, 142, 22, 107, 21, 256, 256);

		renderSkillsList(graphics, leftPanelX, leftPanelY, mouseX, mouseY);
	}

	private void renderSkillsList(GuiGraphics graphics, int panelX, int panelY, int mouseX, int mouseY) {
		List<String> skillNames = getVisibleSkillNames();

		int startY = panelY + 30;
		int visibleStart = scrollOffset;
		int visibleEnd = Math.min(visibleStart + MAX_VISIBLE_SKILLS, skillNames.size());

		graphics.enableScissor(panelX + 5, startY, panelX + 179, startY + (MAX_VISIBLE_SKILLS * SKILL_ITEM_HEIGHT));

		for (int i = visibleStart; i < visibleEnd; i++) {
			String skillName = skillNames.get(i);
			int itemY = startY + ((i - visibleStart) * SKILL_ITEM_HEIGHT);

			boolean isSelected = skillName.equals(selectedSkill);
			boolean isHovered = mouseX >= panelX + 10 && mouseX <= panelX + 100 &&
					mouseY >= itemY && mouseY <= itemY + SKILL_ITEM_HEIGHT;

			int color = isSelected ? 0xFFFFAA00 : (isHovered ? 0xFFAAAAAA : 0xFFFFFFFF);

			Skill skill = statsData.getSkills().getSkill(skillName);
			String displayName = Component.translatable("skill.dragonminez." + skillName).getString();

			drawStringWithBorder(graphics, Component.literal(displayName),
					panelX + 15, itemY + 5, color);

			String levelText;
			if (skill != null && skill.getLevel() > 0) {
				levelText = String.valueOf(skill.getLevel());
			} else {
				levelText = "0";
			}

			int levelX = panelX + 130 - this.font.width(levelText);
			drawStringWithBorder(graphics, Component.literal(levelText),
					levelX, itemY + 5, color);
		}

		graphics.disableScissor();

		if (maxScroll > 0) {
			int scrollBarX = panelX + 135;
			int scrollBarStartY = startY;
			int scrollBarHeight = MAX_VISIBLE_SKILLS * SKILL_ITEM_HEIGHT;
			int totalItems = skillNames.size();

			graphics.fill(scrollBarX, scrollBarStartY, scrollBarX + 3, scrollBarStartY + scrollBarHeight, 0xFF333333);

			float scrollPercent = (float) scrollOffset / maxScroll;
			float visiblePercent = (float) MAX_VISIBLE_SKILLS / totalItems;
			int indicatorHeight = Math.max(20, (int)(scrollBarHeight * visiblePercent));
			int indicatorY = scrollBarStartY + (int)((scrollBarHeight - indicatorHeight) * scrollPercent);

			graphics.fill(scrollBarX, indicatorY, scrollBarX + 3, indicatorY + indicatorHeight, 0xFFAAAAAA);
		}

		String title = "";
		switch (currentCategory) {
			case SKILLS -> title = "gui.dragonminez.skills.tab.skills";
			case KI -> title = "gui.dragonminez.skills.tab.kiattacks";
		}

		drawStringWithBorder(graphics, Component.translatable(title)
				.withStyle(style -> style.withBold(true)), 65, this.height/2 - 88, 0xFBC51C);
	}

	private void renderRightPanel(GuiGraphics graphics, int mouseX, int mouseY) {
		int rightPanelX = this.width - 158;
		int centerY = this.height / 2;
		int rightPanelY = centerY - 105;

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		graphics.blit(MENU_SMALL, rightPanelX, rightPanelY, 0, 0, 141, 94, 256, 256);
		graphics.blit(MENU_BIG, this.width - 141, centerY - 95, 142, 22, 107, 21, 256, 256);
		graphics.blit(MENU_SMALL, rightPanelX, rightPanelY + 96, 0, 0, 141, 94, 256, 256);
		graphics.blit(MENU_SMALL, rightPanelX, rightPanelY + 190, 0, 154, 141, 32, 256, 256);

		drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.character_stats.info")
				.withStyle(style -> style.withBold(true)), rightPanelX + 70, rightPanelY + 16, 0xFFFFD700);

		if (selectedSkill != null && statsData != null) {
			renderSkillDetails(graphics, rightPanelX, rightPanelY);
		}
	}

	private void renderSkillDetails(GuiGraphics graphics, int panelX, int panelY) {
		Skill skill = statsData.getSkills().getSkill(selectedSkill);
		if (skill == null) skill = new Skill(selectedSkill, 0, false, 10);

		String displayName = Component.translatable("skill.dragonminez." + selectedSkill).getString();
		String description = Component.translatable("skill.dragonminez." + selectedSkill + ".desc").getString();

		int startY = panelY + 40;

		drawCenteredStringWithBorder(graphics, Component.literal(displayName).withStyle(ChatFormatting.BOLD),
				panelX + 72, startY, 0xFFFFFFFF);

		Component levelComp;
		if (skill.getLevel() > 0) {
			levelComp = Component.translatable("gui.dragonminez.skills.level", skill.getLevel(), skill.getMaxLevel());
		} else {
			levelComp = Component.translatable("gui.dragonminez.skills.not_learned");
		}

		drawCenteredStringWithBorder(graphics, levelComp, panelX + 72, startY + 12, 0xFFAAAAAA);

		if (skill.getLevel() == 0) {
			int cost = getUpgradeCost(selectedSkill, 0);
			if (cost != Integer.MAX_VALUE && cost != -1) {
				drawCenteredStringWithBorder(graphics, Component.literal("%d TPS".formatted(cost)), panelX + 72, startY + 24, 0xFFAAAAAA);
			}
		} else {
			drawCenteredStringWithBorder(graphics,
					Component.translatable("gui.dragonminez.skills.already_learned"),
					panelX + 72, startY + 24, 0xFF55AA55);
		}

		List<String> wrappedDesc = wrapText(description);
		int descY = startY + 70;
		for (String line : wrappedDesc) {
			drawStringWithBorder(graphics, Component.literal(line), panelX + 13, descY, 0xFFCCCCCC);
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
		int leftPanelX = 12;
		int centerY = this.height / 2;
		int leftPanelY = centerY - 105;

		if (mouseX >= leftPanelX && mouseX <= leftPanelX + 184 &&
				mouseY >= leftPanelY + 40 && mouseY <= leftPanelY + 239) {

			scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)delta));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int leftPanelX = 12;
		int centerY = this.height / 2;
		int leftPanelY = centerY - 105;

		List<String> skillNames = getVisibleSkillNames();
		int startY = leftPanelY + 30;
		int visibleStart = scrollOffset;
		int visibleEnd = Math.min(visibleStart + MAX_VISIBLE_SKILLS, skillNames.size());

		for (int i = visibleStart; i < visibleEnd; i++) {
			int itemY = startY + ((i - visibleStart) * SKILL_ITEM_HEIGHT);

			if (mouseX >= leftPanelX + 10 && mouseX <= leftPanelX + 100 &&
					mouseY >= itemY && mouseY <= itemY + SKILL_ITEM_HEIGHT - 5) {

				selectedSkill = skillNames.get(i);
				refreshButtons();
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private void renderMasterEntity(GuiGraphics graphics, int x, int y, float mouseX, float mouseY) {
		if (masterEntity == null) return;

		float xRotation = (float) Math.atan((double)((float)y - mouseY) / 40.0F);
		float yRotation = (float) Math.atan((double)((float)x - mouseX) / 40.0F);

		Quaternionf pose = (new Quaternionf()).rotateZ((float)Math.PI);
		Quaternionf cameraOrientation = (new Quaternionf()).rotateX(xRotation * 20.0F * ((float)Math.PI / 180F));
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

	private void drawStringWithBorder(GuiGraphics graphics, Component text, int x, int y, int textColor) {
		int borderColor = 0xFF000000;
		graphics.drawString(this.font, text, x + 1, y, borderColor, false);
		graphics.drawString(this.font, text, x - 1, y, borderColor, false);
		graphics.drawString(this.font, text, x, y + 1, borderColor, false);
		graphics.drawString(this.font, text, x, y - 1, borderColor, false);
		graphics.drawString(this.font, text, x, y, textColor, false);
	}

	private void drawCenteredStringWithBorder(GuiGraphics graphics, Component text, int centerX, int y, int textColor) {
		int textWidth = this.font.width(text);
		int x = centerX - (textWidth / 2);
		drawStringWithBorder(graphics, text, x, y, textColor);
	}
}