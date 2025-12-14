package com.dragonminez.client.gui;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.C2S.UpgradeSkillC2S;
import com.dragonminez.common.stats.Skill;
import com.dragonminez.common.stats.Skills;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class SkillsMenuScreen extends Screen {

    private static final ResourceLocation MENU_GRANDE = new ResourceLocation(Reference.MOD_ID,
            "textures/gui/menu/menugrande.png");
    private static final ResourceLocation MENU_PEQUENO = new ResourceLocation(Reference.MOD_ID,
            "textures/gui/menu/menupequeno.png");
    private static final ResourceLocation BUTTONS_TEXTURE = new ResourceLocation(Reference.MOD_ID,
            "textures/gui/buttons/characterbuttons.png");
	private static final ResourceLocation SCREEN_BUTTONS = new ResourceLocation(Reference.MOD_ID,
			"textures/gui/buttons/menubuttons.png");

    private static final int SKILL_ITEM_HEIGHT = 20;
    private static final int MAX_VISIBLE_SKILLS = 8;

    private StatsData statsData;
    private int oldGuiScale;
    private int tickCount = 0;

    private boolean showingSkills = true;
    private String selectedSkill = null;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private Button skillsTabButton;
    private Button formsTabButton;
    private Button upgradeButton;

    public SkillsMenuScreen(int oldGuiScale) {
        super(Component.translatable("gui.dragonminez.skills.title"));
        this.oldGuiScale = oldGuiScale;
    }

    @Override
    protected void init() {
        super.init();
        updateStatsData();
        initTabButtons();
        initNavigationButtons();
        updateSkillsList();
    }

	private void initNavigationButtons() {
		int centerX = this.width / 2;
		int bottomY = this.height - 30;

		this.addRenderableWidget(
				new CustomTextureButton.Builder()
						.position(centerX - 70, bottomY)
						.size(20, 20)
						.texture(SCREEN_BUTTONS)
						.textureSize(20, 20)
						.textureCoords(0, 0, 0, 20)
						.onPress(btn -> {
							if (this.minecraft != null) {
								this.minecraft.setScreen(new CharacterStatsScreen(oldGuiScale));
							}
						})
						.build()
		);

		this.addRenderableWidget(
				new CustomTextureButton.Builder()
						.position(centerX - 30, bottomY)
						.size(20, 20)
						.texture(SCREEN_BUTTONS)
						.textureSize(20, 20)
						.textureCoords(20, 0, 20, 20)
						.onPress(btn -> {
							if (this.minecraft != null) {
								this.minecraft.setScreen(new SkillsMenuScreen(oldGuiScale));
							}
						})
						.build()
		);

		this.addRenderableWidget(
				new CustomTextureButton.Builder()
						.position(centerX + 10, bottomY)
						.size(20, 20)
						.texture(SCREEN_BUTTONS)
						.textureSize(20, 20)
						.textureCoords(60, 0, 60, 20)
						.onPress(btn -> {
							if (this.minecraft != null) {
								this.minecraft.setScreen(new QuestsMenuScreen(oldGuiScale));
							}
						})
						.build()
		);

		this.addRenderableWidget(
				new CustomTextureButton.Builder()
						.position(centerX + 50, bottomY)
						.size(20, 20)
						.texture(SCREEN_BUTTONS)
						.textureSize(20, 20)
						.textureCoords(100, 0, 100, 20)
						.onPress(btn -> {
							if (this.minecraft != null) {
								this.minecraft.setScreen(new ConfigMenuScreen(oldGuiScale));
							}
						})
						.build()
		);
	}

    @Override
    public void tick() {
        super.tick();
        tickCount++;

        if (tickCount >= 10) {
            tickCount = 0;
            updateStatsData();
        }
    }

    private void updateStatsData() {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                this.statsData = data;
            });
        }
    }

    private void initTabButtons() {
        int leftPanelX = 12;
        int centerY = this.height / 2;
        int leftPanelY = centerY - 105;

        skillsTabButton = Button.builder(Component.translatable("gui.dragonminez.skills.tab.skills"), btn -> {
            showingSkills = true;
            selectedSkill = null;
            scrollOffset = 0;
            updateSkillsList();
            refreshButtons();
        })
        .bounds(leftPanelX + 10, leftPanelY + 10, 60, 18)
        .build();

        formsTabButton = Button.builder(Component.translatable("gui.dragonminez.skills.tab.forms"), btn -> {
            showingSkills = false;
            selectedSkill = null;
            scrollOffset = 0;
            updateSkillsList();
            refreshButtons();
        })
        .bounds(leftPanelX + 75, leftPanelY + 10, 60, 18)
        .build();

        this.addRenderableWidget(skillsTabButton);
        this.addRenderableWidget(formsTabButton);
    }

    private void updateSkillsList() {
        List<String> skillNames = getVisibleSkillNames();
        maxScroll = Math.max(0, skillNames.size() - MAX_VISIBLE_SKILLS);
    }

    private List<String> getVisibleSkillNames() {
        if (statsData == null) return new ArrayList<>();

        Skills skills = statsData.getSkills();
        List<String> skillNames = new ArrayList<>();

        if (showingSkills) {
            skills.getAllSkills().forEach((name, skill) -> {
                if (!name.equals("superform") && !name.equals("godform") && !name.equals("legendaryforms")) {
                    skillNames.add(name);
                }
            });
        } else {
            if (skills.hasSkill("superform")) skillNames.add("superform");
            if (skills.hasSkill("godform")) skillNames.add("godform");
            if (skills.hasSkill("legendaryforms")) skillNames.add("legendaryforms");
        }

        return skillNames;
    }

    private void refreshButtons() {
        this.clearWidgets();
        initTabButtons();
        initNavigationButtons();
        initUpgradeButton();
    }

    private void initUpgradeButton() {
        if (selectedSkill == null || statsData == null) return;

        Skill skill = statsData.getSkills().getSkill(selectedSkill);
        if (skill == null) return;

        int rightPanelX = this.width - 158;
        int centerY = this.height / 2;
        int rightPanelY = centerY - 105;

        int cost = getUpgradeCost(selectedSkill, skill.getLevel());
        int currentTPS = statsData.getResources().getTrainingPoints();
        boolean canUpgrade = skill.getLevel() < skill.getMaxLevel() && currentTPS >= cost;

        upgradeButton = Button.builder(
            Component.translatable("gui.dragonminez.skills.upgrade")
                .append(" (")
                .append(String.valueOf(cost))
                .append(" TPS)"),
            btn -> {
                if (canUpgrade) {
                    NetworkHandler.INSTANCE.sendToServer(new UpgradeSkillC2S(selectedSkill));
                    updateStatsData();
                }
            })
        .bounds(rightPanelX + 25, rightPanelY + 199, 100, 15)
        .build();

        upgradeButton.active = canUpgrade;
        this.addRenderableWidget(upgradeButton);
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
        this.renderBackground(graphics);

        renderPlayerModel(graphics, this.width / 2 + 5, this.height / 2 + 70, 75, mouseX, mouseY);

        renderLeftPanel(graphics, mouseX, mouseY);
        renderRightPanel(graphics, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderLeftPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int leftPanelX = 12;
        int centerY = this.height / 2;
        int leftPanelY = centerY - 105;

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.blit(MENU_GRANDE, leftPanelX, leftPanelY, 0, 0, 149, 239, 256, 256);

        renderSkillsList(graphics, leftPanelX, leftPanelY, mouseX, mouseY);
    }

    private void renderSkillsList(GuiGraphics graphics, int panelX, int panelY, int mouseX, int mouseY) {
        List<String> skillNames = getVisibleSkillNames();

        int startY = panelY + 40;
        int visibleStart = scrollOffset;
        int visibleEnd = Math.min(visibleStart + MAX_VISIBLE_SKILLS, skillNames.size());

        graphics.enableScissor(panelX + 5, startY, panelX + 179, startY + (MAX_VISIBLE_SKILLS * SKILL_ITEM_HEIGHT));

        for (int i = visibleStart; i < visibleEnd; i++) {
            String skillName = skillNames.get(i);
            int itemY = startY + ((i - visibleStart) * SKILL_ITEM_HEIGHT);

            boolean isSelected = skillName.equals(selectedSkill);
            boolean isHovered = mouseX >= panelX + 10 && mouseX <= panelX + 174 &&
                              mouseY >= itemY && mouseY <= itemY + SKILL_ITEM_HEIGHT;

            int color = isSelected ? 0xFFFFAA00 : (isHovered ? 0xFFAAAAAA : 0xFFFFFFFF);

            Skill skill = statsData.getSkills().getSkill(skillName);
            String displayName = Component.translatable("skill.dragonminez." + skillName).getString();

            drawStringWithBorder(graphics, Component.literal(displayName),
                panelX + 15, itemY + 5, color);

            if (skill != null) {
                String levelText = String.valueOf(skill.getLevel());
                int levelX = panelX + 130 - this.font.width(levelText);
                drawStringWithBorder(graphics, Component.literal(levelText),
                    levelX, itemY + 5, color);
            }
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
    }

    private void renderRightPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int rightPanelX = this.width - 157;
        int centerY = this.height / 2;
        int rightPanelY = centerY - 105;

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.blit(MENU_PEQUENO, rightPanelX, rightPanelY, 0, 0, 145, 92, 256, 256);
		graphics.blit(MENU_PEQUENO, rightPanelX, rightPanelY + 94, 0, 0, 145, 92, 256, 256);
		graphics.blit(MENU_PEQUENO, rightPanelX, rightPanelY + 188, 0, 171, 145, 37, 256, 256);

		drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.skills.information"),
            rightPanelX + 74, rightPanelY + 18, 0xFFFFD700);

        if (selectedSkill != null && statsData != null) {
            renderSkillDetails(graphics, rightPanelX, rightPanelY);
        }
    }

    private void renderSkillDetails(GuiGraphics graphics, int panelX, int panelY) {
        Skill skill = statsData.getSkills().getSkill(selectedSkill);
        if (skill == null) return;

        String displayName = Component.translatable("skill.dragonminez." + selectedSkill).getString();
        String description = Component.translatable("skill.dragonminez." + selectedSkill + ".desc").getString();

        int startY = panelY + 40;

        drawCenteredStringWithBorder(graphics, Component.literal(displayName).withStyle(ChatFormatting.BOLD),
            panelX + 78, startY, 0xFFFFFFFF);

        drawCenteredStringWithBorder(graphics,
            Component.translatable("gui.dragonminez.skills.level", skill.getLevel(), skill.getMaxLevel()),
            panelX + 78, startY + 20, 0xFFAAAAAA);

        List<String> wrappedDesc = wrapText(description, 130);
        int descY = startY + 70;
        for (String line : wrappedDesc) {
            drawStringWithBorder(graphics, Component.literal(line), panelX + 15, descY, 0xFFCCCCCC);
            descY += 12;
        }
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (font.width(testLine) <= maxWidth) {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            }
        }

        if (currentLine.length() > 0) {
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
        int startY = leftPanelY + 40;
        int visibleStart = scrollOffset;
        int visibleEnd = Math.min(visibleStart + MAX_VISIBLE_SKILLS, skillNames.size());

        for (int i = visibleStart; i < visibleEnd; i++) {
            int itemY = startY + ((i - visibleStart) * SKILL_ITEM_HEIGHT);

            if (mouseX >= leftPanelX + 10 && mouseX <= leftPanelX + 174 &&
                mouseY >= itemY && mouseY <= itemY + SKILL_ITEM_HEIGHT) {

                selectedSkill = skillNames.get(i);
                refreshButtons();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

	private void renderPlayerModel(GuiGraphics graphics, int x, int y, int scale, float mouseX, float mouseY) {
		LivingEntity player = Minecraft.getInstance().player;
		if (player == null) return;

		float xRotation = (float) Math.atan((double)((float)y - mouseY) / 40.0F);
		float yRotation = (float) Math.atan((double)((float)x - mouseX) / 40.0F);

		Quaternionf pose = (new Quaternionf()).rotateZ((float)Math.PI);
		Quaternionf cameraOrientation = (new Quaternionf()).rotateX(xRotation * 20.0F * ((float)Math.PI / 180F));
		pose.mul(cameraOrientation);

		float yBodyRotO = player.yBodyRot;
		float yRotO = player.getYRot();
		float xRotO = player.getXRot();
		float yHeadRotO = player.yHeadRotO;
		float yHeadRot = player.yHeadRot;

		player.yBodyRot = 180.0F + yRotation * 20.0F;
		player.setYRot(180.0F + yRotation * 40.0F);
		player.setXRot(-xRotation * 20.0F);
		player.yHeadRot = player.getYRot();
		player.yHeadRotO = player.getYRot();

		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 150.0D);
		InventoryScreen.renderEntityInInventory(graphics, x, y, scale, pose, cameraOrientation, player);
		graphics.pose().popPose();

		player.yBodyRot = yBodyRotO;
		player.setYRot(yRotO);
		player.setXRot(xRotO);
		player.yHeadRotO = yHeadRotO;
		player.yHeadRot = yHeadRot;
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.options.guiScale().set(oldGuiScale);
            this.minecraft.resizeDisplay();
        }
        super.onClose();
    }

    public int getOldGuiScale() {
        return oldGuiScale;
    }
}

