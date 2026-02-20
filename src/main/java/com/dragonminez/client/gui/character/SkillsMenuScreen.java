package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.ClippableTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.C2S.UpdateSkillC2S;
import com.dragonminez.common.stats.Skill;
import com.dragonminez.common.stats.Skills;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
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
public class SkillsMenuScreen extends BaseMenuScreen {

    private static final ResourceLocation MENU_BIG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
            "textures/gui/menu/menubig.png");
    private static final ResourceLocation MENU_SMALL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
            "textures/gui/menu/menusmall.png");
	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/buttons/characterbuttons.png");

    private static final int SKILL_ITEM_HEIGHT = 20;
    private static final int MAX_VISIBLE_SKILLS = 8;
    private static final int BUTTON_ANIM_TIME = 5;

    private enum SkillCategory { SKILLS, KI, FORMS, STACKS }
    private SkillCategory currentCategory = SkillCategory.SKILLS;

    private StatsData statsData;
    private int tickCount = 0;

    private String selectedSkill = null;
    private int scrollOffset = 0;
    private int maxScroll = 0;
	private int descScrollOffset = 0;
	private int maxDescScroll = 0;

    private ClippableTextureButton skillsButton, kiButton, formsButton, stacksButton;
    private int animTick = 0;
    private boolean isHotZoneHovered = false;

    private TexturedTextButton upgradeButton;

    public SkillsMenuScreen() {
        super(Component.literal("Skills"));
    }

    @Override
    protected void init() {
        super.init();
        updateStatsData();
        initDynamicButtons();
        updateSkillsList();
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

        if (isHotZoneHovered) {
            if (animTick < BUTTON_ANIM_TIME) animTick++;
        } else {
            if (animTick > 0) animTick--;
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
                    scrollOffset = 0;
					descScrollOffset = 0;
                    updateSkillsList();
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
                    scrollOffset = 0;
					descScrollOffset = 0;
                    updateSkillsList();
                    refreshButtons();
                })
                .build();

        formsButton = new ClippableTextureButton.Builder()
                .position(hiddenX, buttonY + 64)
                .size(26, 32)
                .texture(MENU_BIG)
                .textureCoords(198, 44, 198, 44)
                .clipping(true, scissorXScreen, scissorYScreen, scissorRight, scissorBottom)
                .onPress(btn -> {
                    currentCategory = SkillCategory.FORMS;
                    selectedSkill = null;
                    scrollOffset = 0;
					descScrollOffset = 0;
                    updateSkillsList();
                    refreshButtons();
                })
                .build();

        stacksButton = new ClippableTextureButton.Builder()
                .position(hiddenX, buttonY + 96)
                .size(26, 32)
                .texture(MENU_BIG)
                .textureCoords(226, 44, 226, 44)
                .clipping(true, scissorXScreen, scissorYScreen, scissorRight, scissorBottom)
                .onPress(btn -> {
                    currentCategory = SkillCategory.STACKS;
                    selectedSkill = null;
                    scrollOffset = 0;
                    descScrollOffset = 0;
                    updateSkillsList();
                    refreshButtons();
                })
                .build();

        this.addRenderableWidget(skillsButton);
        this.addRenderableWidget(kiButton);
        this.addRenderableWidget(formsButton);
        this.addRenderableWidget(stacksButton);
    }

    private void updateSkillsList() {
        List<String> skillNames = getVisibleSkillNames();
        maxScroll = Math.max(0, skillNames.size() - MAX_VISIBLE_SKILLS);
    }

    private List<String> getVisibleSkillNames() {
        if (statsData == null) return new ArrayList<>();

        Skills skills = statsData.getSkills();
        List<String> skillNames = new ArrayList<>();

        var skillsConfig = ConfigManager.getSkillsConfig();
        switch (currentCategory) {
            case SKILLS:
                skills.getAllSkills().forEach((name, skill) -> {
                    if (!skillsConfig.getKiSkills().contains(name)
                            && !skillsConfig.getStackSkills().contains(name)
                            && !skillsConfig.getFormSkills().contains(name)) {
                        skillNames.add(name);
                    }
                });
                break;
            case KI:
                skills.getAllSkills().forEach((name, skill) -> {
                    if (skillsConfig.getKiSkills().contains(name)) {
                        skillNames.add(name);
                    }
                });
                break;
            case FORMS:
                skills.getAllSkills().forEach((name, skill) -> {
                    if (skillsConfig.getFormSkills().contains(name)) {
                        skillNames.add(name);
                    }
                });
                break;
            case STACKS:
                skills.getAllSkills().forEach((name, skill) -> {
                    if (skillsConfig.getStackSkills().contains(name)) {
                        skillNames.add(name);
                    }
                });
                break;
        }

		skillNames.sort(String::compareToIgnoreCase);
		if (currentCategory == SkillCategory.SKILLS) {
			String race = statsData.getCharacter().getRaceName().toLowerCase();
			if (!race.isEmpty() && !ConfigManager.getRaceCharacter(race).getRacialSkill().isEmpty()) {
				skillNames.add(0, "racial_" + ConfigManager.getRaceCharacter(race).getRacialSkill());
			}
		}
        return skillNames;
    }

    private void refreshButtons() {
        this.clearWidgets();
		if (upgradeButton != null) this.removeWidget(upgradeButton);

		this.upgradeButton = null;
        initDynamicButtons();
        initNavigationButtons();
        initUpgradeButton();
    }

    private void initUpgradeButton() {
        if (selectedSkill == null || statsData == null) return;

        Skill skill = statsData.getSkills().getSkill(selectedSkill);
        if (skill == null) return;

        int rightPanelX = getUiWidth() - 158;
        int centerY = getUiHeight() / 2;
        int rightPanelY = centerY - 105;

        int cost = getUpgradeCost(selectedSkill, skill.getLevel());
        int currentTPS = statsData.getResources().getTrainingPoints();
        boolean canUpgrade = !skill.isMaxLevel() && currentTPS >= cost;
		if (cost == -1 || cost == Integer.MAX_VALUE) return;

        if (!skill.isMaxLevel()) {
			upgradeButton = new TexturedTextButton.Builder()
					.position(rightPanelX + 35, rightPanelY + 196)
					.size(74, 20)
					.texture(BUTTONS_TEXTURE)
					.textureCoords(0, 28, 0, 48)
					.textureSize(74, 20)
					.message(Component.translatable("gui.dragonminez.skills.upgrade"))
					.onPress(btn -> {
						if (canUpgrade) {
							NetworkHandler.INSTANCE.sendToServer(new UpdateSkillC2S(UpdateSkillC2S.SkillAction.UPGRADE, selectedSkill, cost));
							updateStatsData();
						}
					})
					.build();

			upgradeButton.active = canUpgrade;
			this.addRenderableWidget(upgradeButton);
		}
    }

    private int getUpgradeCost(String skillName, int currentLevel) {
        if (ConfigManager.getSkillsConfig().getFormSkills().contains(skillName)) {
			var raceConfig = ConfigManager.getRaceCharacter(statsData.getCharacter().getRaceName());
			Integer[] costs = raceConfig.getFormSkillTpCosts(skillName);
			if (costs != null && currentLevel + 1 <= costs.length) {
				return costs[currentLevel];
			} else {
				return Integer.MAX_VALUE;
			}
		} else {
			var skillConfig = ConfigManager.getSkillsConfig();
			var skillData = skillConfig.getSkills().get(skillName);

			if (skillData != null && skillData.getCosts() != null) {
				var costs = skillData.getCosts();
				if (currentLevel + 1 <= costs.size()) {
					return costs.get(currentLevel);
				}
			}
			return Integer.MAX_VALUE;
		}
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int uiMouseX = (int) Math.round(toUiX(mouseX));
        int uiMouseY = (int) Math.round(toUiY(mouseY));

        beginUiScale(graphics);

        updateButtonAnimations(uiMouseX, uiMouseY, partialTick);

        renderPlayerModel(graphics, getUiWidth() / 2 + 5, getUiHeight() / 2 + 70, 75, uiMouseX, uiMouseY);

        renderLeftPanel(graphics, uiMouseX, uiMouseY);
        renderRightPanel(graphics, uiMouseX, uiMouseY);

        super.render(graphics, uiMouseX, uiMouseY, partialTick);
        endUiScale(graphics);
    }

    private void updateButtonAnimations(int mouseX, int mouseY, float partialTick) {
        int leftPanelX = 12;
        int centerY = getUiHeight() / 2;
        int leftPanelY = centerY - 105;

        int hotZoneX = leftPanelX + 122;
        int hotZoneY = leftPanelY + 6;
        int hotZoneWidth = 48;
        int hotZoneHeight = 133;

        isHotZoneHovered = mouseX >= hotZoneX && mouseX < hotZoneX + hotZoneWidth &&
                           mouseY >= hotZoneY && mouseY < hotZoneY + hotZoneHeight;

        float animProgress = (animTick + (isHotZoneHovered ? partialTick : -partialTick)) / BUTTON_ANIM_TIME;
        animProgress = Mth.clamp(animProgress, 0.0f, 1.0f);

        int hiddenX = leftPanelX + 122;
        int visibleX = leftPanelX + 141;

        int newX = hiddenX + (int)((visibleX - hiddenX) * animProgress);
        skillsButton.setX(newX);
        kiButton.setX(newX);
        formsButton.setX(newX);
        stacksButton.setX(newX);
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
        int visibleStart = scrollOffset;
        int visibleEnd = Math.min(visibleStart + MAX_VISIBLE_SKILLS, skillNames.size());

        graphics.enableScissor(
            toScreenCoord(panelX + 5),
            toScreenCoord(startY),
            toScreenCoord(panelX + 179),
            toScreenCoord(startY + (MAX_VISIBLE_SKILLS * SKILL_ITEM_HEIGHT))
        );

        for (int i = visibleStart; i < visibleEnd; i++) {
            String skillName = skillNames.get(i);
            int itemY = startY + ((i - visibleStart) * SKILL_ITEM_HEIGHT);

            boolean isSelected = skillName.equals(selectedSkill);
            boolean isHovered = mouseX >= panelX + 10 && mouseX <= panelX + 100 &&
                              mouseY >= itemY && mouseY <= itemY + SKILL_ITEM_HEIGHT;

            int color = isSelected ? 0xFFFFAA00 : (isHovered ? 0xFFAAAAAA : 0xFFFFFFFF);

            Skill skill = statsData.getSkills().getSkill(skillName);
            String displayName = Component.translatable("skill.dragonminez." + skillName).getString();

            drawStringWithBorder(graphics, Component.literal(displayName), panelX + 15, itemY + 5, color);

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

		String title = "";
		switch (currentCategory) {
			case SKILLS -> title = "gui.dragonminez.skills.tab.skills";
			case KI -> title = "gui.dragonminez.skills.tab.kiattacks";
			case FORMS -> title = "gui.dragonminez.skills.tab.forms";
            case STACKS -> title = "gui.dragonminez.skills.tab.stacks";
		}

		drawCenteredStringWithBorder(graphics, Component.translatable(title)
				.withStyle(style -> style.withBold(true)), 80, getUiHeight() / 2 - 88, 0xFBC51C);
    }

    private void renderRightPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int rightPanelX = getUiWidth() - 158;
        int centerY = getUiHeight() / 2;
        int rightPanelY = centerY - 105;

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.blit(MENU_SMALL, rightPanelX, rightPanelY, 0, 0, 141, 94, 256, 256);
		graphics.blit(MENU_BIG, getUiWidth() - 141, centerY - 95, 142, 22, 107, 21, 256, 256);
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
        if (skill == null && !selectedSkill.startsWith("racial_")) return;
		GeneralServerConfig.RacialSkillsConfig config = ConfigManager.getServerConfig().getRacialSkills();
        String displayName = Component.translatable("skill.dragonminez." + selectedSkill).getString();

		String description = "";
		if (selectedSkill.startsWith("racial_")) {
			switch (selectedSkill) {
				case "racial_human" -> {
					int regen = (int) Math.round((config.getHumanKiRegenBoost() - 1.0) * 100);
					description = Component.translatable("skill.dragonminez.racial_human.desc", regen).getString();
				}
				case "racial_saiyan" -> {
					int zenkaiHealth = (int) Math.round((config.getSaiyanZenkaiHealthRegen() * 100));
					int zenkaiStat = (int) Math.round((config.getSaiyanZenkaiStatBoost() * 100));
					int cooldown = config.getSaiyanZenkaiCooldownSeconds();
					description = Component.translatable("skill.dragonminez.racial_saiyan.desc", zenkaiHealth, zenkaiStat, cooldown).getString();
				}
				case "racial_namekian" -> {
					int assimHealth = (int) Math.round(config.getNamekianAssimilationHealthRegen() * 100);
					int assimStat = (int) Math.round(config.getNamekianAssimilationStatBoost() * 100);
					description = Component.translatable("skill.dragonminez.racial_namekian.desc", assimHealth, assimStat).getString();
				}
				case "racial_frostdemon" -> {
					int tpBoost = (int) Math.round((config.getFrostDemonTPBoost() - 1.0) * 100);
					description = Component.translatable("skill.dragonminez.racial_frostdemon.desc", tpBoost).getString();
				}
				case "racial_bioandroid" -> {
					int drainRatio = (int) Math.round(config.getBioAndroidDrainRatio() * 100);
					int cooldown = config.getBioAndroidCooldownSeconds();
					description = Component.translatable("skill.dragonminez.racial_bioandroid.desc", drainRatio, cooldown).getString();
				}
				case "racial_majin" -> {
					int absHealth = (int) Math.round(config.getMajinAbsorptionHealthRegen() * 100);
					int absStat = (int) Math.round(config.getMajinAbsorptionStatCopy() * 100);
					description = Component.translatable("skill.dragonminez.racial_majin.desc", absHealth, absStat).getString();
				}
			}
		} else description = Component.translatable("skill.dragonminez." + selectedSkill + ".desc").getString();

        int startY = panelY + 40;

        drawCenteredStringWithBorder(graphics, Component.literal(displayName).withStyle(ChatFormatting.BOLD), panelX + 72, startY, 0xFFFFFFFF);

		if (skill != null) {
			drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.skills.level", skill.getLevel(), skill.getMaxLevel()), panelX + 72, startY + 12, 0xFFAAAAAA);
			if (getUpgradeCost(selectedSkill, skill.getLevel()) != Integer.MAX_VALUE) {
				drawCenteredStringWithBorder(graphics, Component.literal("%d TPS".formatted(getUpgradeCost(selectedSkill, skill.getLevel()))), panelX + 72, startY + 24, 0xFFAAAAAA);
			}
		} else {
			drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.skills.racial"), panelX + 72, startY + 12, 0xFF55FF55);
		}

		List<String> wrappedDesc = wrapText(description, 120);

		int descY = startY + 70;
		int maxLines = 6;
		maxDescScroll = Math.max(0, wrappedDesc.size() - maxLines);

		int boxX = panelX + 13;
		int boxY = descY;
		int boxW = 130;
		int boxH = maxLines * 12;

		graphics.enableScissor(toScreenCoord(boxX), toScreenCoord(boxY), toScreenCoord(boxX + boxW), toScreenCoord(boxY + boxH));

		int visibleEnd = Math.min(descScrollOffset + maxLines, wrappedDesc.size());
		for (int i = descScrollOffset; i < visibleEnd; i++) {
			String line = wrappedDesc.get(i);
			int lineY = descY + ((i - descScrollOffset) * 12);
			drawStringWithBorder(graphics, Component.literal(line), panelX + 13, lineY, 0xFFCCCCCC);
		}

		graphics.disableScissor();
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
        double uiMouseX = toUiX(mouseX);
        double uiMouseY = toUiY(mouseY);
        int leftPanelX = 12;
        int centerY = getUiHeight() / 2;
        int leftPanelY = centerY - 105;
		int rightPanelX = getUiWidth() - 158;

		int descBoxX = rightPanelX + 10;
		int descBoxY = (centerY - 105) + 110;
		int descBoxW = 136;
		int descBoxH = 6 * 12;

		if (uiMouseX >= descBoxX && uiMouseX <= descBoxX + descBoxW &&
				uiMouseY >= descBoxY && uiMouseY <= descBoxY + descBoxH) {

			descScrollOffset = Math.max(0, Math.min(maxDescScroll, descScrollOffset - (int)delta));
			return true;
		}

		if (uiMouseX >= leftPanelX && uiMouseX <= leftPanelX + 184 &&
				uiMouseY >= leftPanelY + 40 && uiMouseY <= leftPanelY + 239) {

			scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)delta));
			return true;
		}

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double uiMouseX = toUiX(mouseX);
        double uiMouseY = toUiY(mouseY);
        int leftPanelX = 12;
        int centerY = getUiHeight() / 2;
        int leftPanelY = centerY - 105;

        List<String> skillNames = getVisibleSkillNames();
        int startY = leftPanelY + 30;
        int visibleStart = scrollOffset;
        int visibleEnd = Math.min(visibleStart + MAX_VISIBLE_SKILLS, skillNames.size());

        for (int i = visibleStart; i < visibleEnd; i++) {
            int itemY = startY + ((i - visibleStart) * SKILL_ITEM_HEIGHT);

            if (uiMouseX >= leftPanelX + 10 && uiMouseX <= leftPanelX + 100 &&
                uiMouseY >= itemY && uiMouseY <= itemY + SKILL_ITEM_HEIGHT - 5) {

                selectedSkill = skillNames.get(i);
				descScrollOffset = 0;
                refreshButtons();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

	private void renderPlayerModel(GuiGraphics graphics, int x, int y, int scale, float mouseX, float mouseY) {
		LivingEntity player = Minecraft.getInstance().player;
		if (player == null) return;

		int adjustedScale = getAdjustedModelScale(scale);

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
		InventoryScreen.renderEntityInInventory(graphics, x, y, adjustedScale, pose, cameraOrientation, player);
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
}
