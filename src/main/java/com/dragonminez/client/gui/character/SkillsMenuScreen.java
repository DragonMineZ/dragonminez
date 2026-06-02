package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.ClippableTextureButton;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.BaseMenuScreen;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.network.C2S.*;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.skills.Skill;
import com.dragonminez.common.stats.skills.Skills;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.dragonminez.common.util.TransformationsHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class SkillsMenuScreen extends BaseMenuScreen {

	private static final ResourceLocation STAT_BUTTONS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");
	private static final ResourceLocation MENU_BIG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menubig.png");
	private static final ResourceLocation MENU_SMALL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menusmall.png");
	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");
	private static final ResourceLocation EXCLAMATION_MARK = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/quest/exclamation_mark_quest.png");

	private static final int SKILL_ITEM_HEIGHT = 20;
	private static final int MAX_VISIBLE_SKILLS = 8;
	private static final int TECHNIQUE_BIND_SLOT_COUNT = 5;
	private static final String NEW_SKILL_ENTRY = "__new_skill__";
	private static final List<String> PREVIEW_FORM_TYPE_ORDER = List.of("superforms", "androidforms", "legendaryforms", "godforms");

	private enum SkillCategory {SKILLS, KI, FORMS, STRIKE}

	private SkillCategory currentCategory = SkillCategory.SKILLS;

	private StatsData statsData;
	private int tickCount = 0;

	private String selectedSkill = null;

	private float targetScroll = 0;
	private float currentScroll = 0;
	private float maxScroll = 0;

	private float targetDescScroll = 0;
	private float currentDescScroll = 0;
	private float maxDescScroll = 0;

	private boolean isDraggingMainScroll = false;
	private boolean isDraggingDescScroll = false;
	private boolean isBinding = false;
	private boolean isImportingTechnique = false;

	private ClippableTextureButton skillsButton, kiButton, formsButton, stacksButton;
	private CustomTextureButton btnDmg, btnSize, btnSpeed, btnPen, btnCast, btnCd;
	private float buttonRevealProgress = 0.0f;
	private float formsTransitionProgress = 0.0f;
	private float leftPanelHoverProgress = 0.0f;

	private int currentLeftX = 12;
	private int currentRightX = 0;

	private EditBox techniqueImportBox;
	private Component actionStatusText = Component.empty();
	private int actionStatusTimer = 0;
	private int actionStatusColor = 0xFFFFFF;

	private static Component pendingImportStatusText = null;
	private static int pendingImportStatusColor = 0xFFFFFF;

	private TexturedTextButton upgradeButton;

	private float formsPanX = 0;
	private float formsPanY = 0;
	private float formsZoom = 1.0f;
	private boolean isDraggingForms = false;
	private double dragFormStartX, dragFormStartY;
	private float dragFormStartPanX, dragFormStartPanY;
	private String selectedFormGroup = null;
	private String selectedFormName = null;
	private long lastFormClickTime = 0;
	private final List<FormNode> formNodes = new ArrayList<>();

	private static class FormNode {
		String group;
		String formType;
		FormConfig.FormData data;
		int x;
		int y;

		FormNode(String group, String formType, FormConfig.FormData data, int x, int y) {
			this.group = group;
			this.formType = formType;
			this.data = data;
			this.x = x;
			this.y = y;
		}
	}

	public SkillsMenuScreen() {
		super(Component.literal("Skills"));
	}

	@Override
	protected void init() {
		super.init();
		updateStatsData();
		initDynamicButtons();
		if (currentCategory == SkillCategory.FORMS) buildFormsTree();
	}

	@Override
	public void tick() {
		super.tick();
		tickCount++;
		if (actionStatusTimer > 0) actionStatusTimer--;
		consumePendingImportStatus();

		if (tickCount >= 10) {
			tickCount = 0;
			updateStatsData();
			if (!isBinding && !isImportingTechnique) refreshButtons();
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

	private void buildFormsTree() {
		formNodes.clear();
		if (statsData == null) return;
		String race = statsData.getCharacter().getRaceName().toLowerCase(Locale.ROOT);
		List<TransformationsHelper.OrderedFormEntry> orderedForms = TransformationsHelper.getOrderedFormsForRace(race, PREVIEW_FORM_TYPE_ORDER);
		Map<String, List<FormConfig.FormData>> groupedForms = new LinkedHashMap<>();

		for (TransformationsHelper.OrderedFormEntry entry : orderedForms) {
			if (entry.getFormData() == null) continue;
			groupedForms.computeIfAbsent(entry.getGroupName(), k -> new ArrayList<>()).add(entry.getFormData());
		}

		for (String stackSkill : ConfigManager.getSkillsConfig().getStackSkills()) {
			FormConfig stackGroup = ConfigManager.getStackFormGroup(stackSkill);
			if (stackGroup == null) continue;
			List<FormConfig.FormData> stackForms = new ArrayList<>(stackGroup.getForms().values());
			if (stackForms.isEmpty()) continue;
			groupedForms.computeIfAbsent(stackGroup.getGroupName(), k -> new ArrayList<>()).addAll(stackForms);
		}

		int groupY = -(groupedForms.size() * 80) / 2;

		for (Map.Entry<String, List<FormConfig.FormData>> group : groupedForms.entrySet()) {
			int formX = -(group.getValue().size() * 80) / 2;
			String groupName = group.getKey();
			String type = "superforms";
			FormConfig fc = ConfigManager.getFormGroup(race, groupName);
			if (fc == null) fc = ConfigManager.getStackFormGroup(groupName);
			if (fc != null && fc.getFormType() != null) type = fc.getFormType();

			for (FormConfig.FormData form : group.getValue()) {
				formNodes.add(new FormNode(groupName, type, form, formX, groupY));
				formX += 80;
			}
			groupY += 80;
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
					targetDescScroll = 0;
					currentDescScroll = 0;
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
					targetDescScroll = 0;
					currentDescScroll = 0;
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
					selectedFormName = null;
					selectedFormGroup = null;
					buildFormsTree();
					targetScroll = 0;
					currentScroll = 0;
					targetDescScroll = 0;
					currentDescScroll = 0;
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
					currentCategory = SkillCategory.STRIKE;
					selectedSkill = null;
					targetScroll = 0;
					currentScroll = 0;
					targetDescScroll = 0;
					currentDescScroll = 0;
					refreshButtons();
				})
				.build();

		this.addRenderableWidget(skillsButton);
		this.addRenderableWidget(kiButton);
		this.addRenderableWidget(formsButton);
		this.addRenderableWidget(stacksButton);
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
							&& !skillsConfig.getStrikeSkills().contains(name)
							&& !skillsConfig.getStackSkills().contains(name)
							&& !skillsConfig.getFormSkills().contains(name)) {
						skillNames.add(name);
					}
				});
				break;
			case KI:
				skillNames.add(NEW_SKILL_ENTRY);
				statsData.getTechniques().getUnlockedTechniques().forEach((id, technique) -> {
					if (technique instanceof KiAttackData) skillNames.add(id);
				});
				break;
			case FORMS:
				skills.getAllSkills().forEach((name, skill) -> {
					if (skillsConfig.getFormSkills().contains(name) || skillsConfig.getStackSkills().contains(name)) {
						skillNames.add(name);
					}
				});
				break;
			case STRIKE:
				statsData.getTechniques().getUnlockedTechniques().forEach((id, technique) -> {
					if (technique instanceof StrikeAttackData) skillNames.add(id);
				});
				break;
		}

		skillNames.sort((a, b) -> getDisplayNameForEntry(a).compareToIgnoreCase(getDisplayNameForEntry(b)));
		if (skillNames.remove(NEW_SKILL_ENTRY)) skillNames.add(0, NEW_SKILL_ENTRY);
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
		if (btnDmg != null) this.removeWidget(btnDmg);
		if (btnSize != null) this.removeWidget(btnSize);
		if (btnSpeed != null) this.removeWidget(btnSpeed);
		if (btnPen != null) this.removeWidget(btnPen);
		if (btnCast != null) this.removeWidget(btnCast);
		if (btnCd != null) this.removeWidget(btnCd);

		this.isBinding = false;
		this.techniqueImportBox = null;
		this.upgradeButton = null;
		this.btnDmg = null;
		this.btnSize = null;
		this.btnSpeed = null;
		this.btnPen = null;
		this.btnCast = null;
		this.btnCd = null;

		initDynamicButtons();
		initNavigationButtons();

		if (currentCategory == SkillCategory.FORMS) return;

		initUpgradeButton();
		initCreateSkillButton();
		initBindButtons();
		initTechniqueUpgradeButtons();
	}

	private void initTechniqueUpgradeButtons() {
		if (selectedSkill == null || statsData == null || (currentCategory != SkillCategory.KI && currentCategory != SkillCategory.STRIKE)) return;
		if (NEW_SKILL_ENTRY.equals(selectedSkill)) return;

		TechniqueData tech = statsData.getTechniques().getUnlockedTechniques().get(selectedSkill);
		if (tech == null) return;

		int rightPanelX = getUiWidth() - 158;
		int centerY = getUiHeight() / 2;
		int rightPanelY = centerY - 105;

		int btnX = rightPanelX + 115;
		int yOffset = rightPanelY + (tech instanceof KiAttackData ? 92 : 80);

		if (tech instanceof KiAttackData) {
			if (shouldShowTechniqueUpgradeButton(tech, "damage")) {
				btnDmg = createUpgradeBtn(btnX, yOffset, "damage", true);
				this.addRenderableWidget(btnDmg);
			}
			yOffset += 12;

			if (shouldShowTechniqueUpgradeButton(tech, "size")) {
				btnSize = createUpgradeBtn(btnX, yOffset, "size", true);
				this.addRenderableWidget(btnSize);
			}
			yOffset += 12;

			if (shouldShowTechniqueUpgradeButton(tech, "speed")) {
				btnSpeed = createUpgradeBtn(btnX, yOffset, "speed", true);
				this.addRenderableWidget(btnSpeed);
			}
			yOffset += 12;

			if (shouldShowTechniqueUpgradeButton(tech, "armor_pen")) {
				btnPen = createUpgradeBtn(btnX, yOffset, "armor_pen", true);
				this.addRenderableWidget(btnPen);
			}
			yOffset += 12;
		} else {
			if (shouldShowTechniqueUpgradeButton(tech, "damage")) {
				btnDmg = createUpgradeBtn(btnX, yOffset, "damage", true);
				this.addRenderableWidget(btnDmg);
			}
			yOffset += 12;
		}

		if (shouldShowTechniqueUpgradeButton(tech, "cast")) {
			btnCast = createUpgradeBtn(btnX, yOffset, "cast", true);
			this.addRenderableWidget(btnCast);
		}
		yOffset += 12;

		if (shouldShowTechniqueUpgradeButton(tech, "cooldown")) {
			btnCd = createUpgradeBtn(btnX, yOffset, "cooldown", true);
			this.addRenderableWidget(btnCd);
		}
	}

	private CustomTextureButton createUpgradeBtn(int x, int y, String statName, boolean active) {
		var btn = new CustomTextureButton.Builder()
				.position(x, y - 1)
				.size(14, 11)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 0, 0, 10)
				.textureSize(10, 10)
				.onPress(button -> {
					NetworkHandler.INSTANCE.sendToServer(new UpgradeTechniqueC2S(selectedSkill, statName));
				})
				.build();
		btn.active = active;
		return btn;
	}

	private boolean shouldShowTechniqueUpgradeButton(TechniqueData tech, String statName) {
		if (!hasEnoughTechniqueXpForUpgrade(tech, statName)) return false;
		if (tech instanceof KiAttackData kiAttackData) return kiAttackData.canUpgradeStat(statName);
		return "damage".equals(statName) || "cast".equals(statName) || "cooldown".equals(statName);
	}

	private boolean hasEnoughTechniqueXpForUpgrade(TechniqueData tech, String statName) {
		return tech.getExperience() >= getTechniqueUpgradeXpCost(tech, statName);
	}

	private int getTechniqueUpgradeXpCost(TechniqueData tech, String statName) {
		if (tech instanceof KiAttackData kiAttackData) return kiAttackData.getUpgradeXpCost(statName);
		if (tech instanceof StrikeAttackData strikeAttackData) return strikeAttackData.getUpgradeXpCost(statName);
		return 100;
	}

	private void initBindButtons() {
		if (selectedSkill == null || statsData == null || (currentCategory != SkillCategory.KI && currentCategory != SkillCategory.STRIKE)) return;
		if (NEW_SKILL_ENTRY.equals(selectedSkill)) return;

		int rightPanelX = getUiWidth() - 158;
		int centerY = getUiHeight() / 2;
		int rightPanelY = centerY - 105;
		TechniqueData tech = statsData.getTechniques().getUnlockedTechniques().get(selectedSkill);
		boolean isKi = tech instanceof KiAttackData;
		boolean canShare = isKi && !PredefinedTechniques.isPredefinedTechniqueId(selectedSkill);

		if (!isBinding) {
			int yPos = rightPanelY + 185;

			if (canShare) {
				var importButton = new CustomTextureButton.Builder()
						.position(rightPanelX + 11, yPos)
						.size(20, 20)
						.texture(BUTTONS_TEXTURE)
						.textureCoords(162, 0, 162, 20)
						.textureSize(20, 20)
						.message(Component.empty())
						.onPress(btn -> {
							if (!isImportingTechnique) {
								isImportingTechnique = true;
								refreshButtons();
							} else {
								attemptTechniqueImport();
							}
						})
						.build();
				this.addRenderableWidget(importButton);
			}

			var bindButton = new TexturedTextButton.Builder()
					.position(rightPanelX + 35, yPos)
					.size(74, 20)
					.texture(BUTTONS_TEXTURE)
					.textureCoords(0, 28, 0, 48)
					.textureSize(74, 20)
					.message(tr("gui.dragonminez.skills.bind_to_slot"))
					.onPress(btn -> {
						isBinding = true;
						this.clearWidgets();
						initDynamicButtons();
						initNavigationButtons();
						initUpgradeButton();
						initBindButtons();
					})
					.build();
			this.addRenderableWidget(bindButton);

			if (canShare) {
				var exportButton = new CustomTextureButton.Builder()
						.position(rightPanelX + 114, yPos)
						.size(20, 20)
						.texture(BUTTONS_TEXTURE)
						.textureCoords(182, 0, 182, 20)
						.textureSize(20, 20)
						.message(Component.empty())
						.onPress(btn -> {
							if (tech instanceof KiAttackData kiAttackData) {
								Minecraft.getInstance().keyboardHandler.setClipboard(kiAttackData.generateExportCode());
								setActionStatus(tr("gui.dragonminez.skills.status.copied"), 0x55FF55);
							}
						})
						.build();
				this.addRenderableWidget(exportButton);
			}

			if (canShare && isImportingTechnique) {
				techniqueImportBox = new EditBox(this.font, rightPanelX + 7, yPos + 22, 123, 12, Component.empty());
				techniqueImportBox.setMaxLength(65536);
				this.addRenderableWidget(techniqueImportBox);
			}

			addRenderableWidget(new CustomTextureButton.Builder()
					.position(rightPanelX + 119, yPos - 14)
					.size(14, 11)
					.texture(STAT_BUTTONS)
					.textureCoords(10, 0, 10, 10)
					.textureSize(10, 10)
					.onPress(btn -> {
						NetworkHandler.sendToServer(new DeleteTechniqueC2S(selectedSkill));
						selectedSkill = null;
					})
					.build());
		} else {
			for (int i = 0; i < TECHNIQUE_BIND_SLOT_COUNT; i++) {
				final int slotIndex = i;
				var slotBtn = new TexturedTextButton.Builder()
						.position(rightPanelX + 40 + (i * 12), rightPanelY + 185)
						.size(10, 10)
						.texture(BUTTONS_TEXTURE)
						.textureCoords(152, 0, 152, 10)
						.textureSize(10, 10)
						.message(Component.literal(String.valueOf(i + 1)))
						.onPress(btn -> {
							NetworkHandler.INSTANCE.sendToServer(new EquipTechniqueC2S(slotIndex, selectedSkill));
							isBinding = false;
							isImportingTechnique = false;
							refreshButtons();
						})
						.build();
				this.addRenderableWidget(slotBtn);
			}
		}
	}

	private void initCreateSkillButton() {
		if (statsData == null || currentCategory != SkillCategory.KI || !NEW_SKILL_ENTRY.equals(selectedSkill)) return;

		int rightPanelX = getUiWidth() - 158;
		int centerY = getUiHeight() / 2;
		int rightPanelY = centerY - 105;

		var createButton = new TexturedTextButton.Builder()
				.position(rightPanelX + 35, rightPanelY + 185)
				.size(74, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(tr("gui.dragonminez.skills.create_skill"))
				.onPress(btn -> openTechniqueCreator())
				.build();
		this.addRenderableWidget(createButton);
	}

	private void initUpgradeButton() {
		if (selectedSkill == null || statsData == null) return;

		Skill skill = statsData.getSkills().getSkill(selectedSkill);
		if (skill == null) return;

		int rightPanelX = getUiWidth() - 158;
		int centerY = getUiHeight() / 2;
		int rightPanelY = centerY - 105;

		int cost = getUpgradeCost(selectedSkill, skill.getLevel());
		float currentTPS = statsData.getResources().getTrainingPoints();
		boolean canUpgrade = !skill.isMaxLevel() && currentTPS >= cost;
		if (cost == -1 || cost == Integer.MAX_VALUE) return;

		if (!skill.isMaxLevel()) {
			upgradeButton = new TexturedTextButton.Builder()
					.position(rightPanelX + 35, rightPanelY + 196)
					.size(74, 20)
					.texture(BUTTONS_TEXTURE)
					.textureCoords(0, 28, 0, 48)
					.textureSize(74, 20)
					.message(tr("gui.dragonminez.skills.upgrade"))
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
		return getUpgradeCostForTargetLevel(skillName, currentLevel);
	}

	private int getUpgradeCostForTargetLevel(String skillName, int targetLevel) {
		if (skillName == null || skillName.isEmpty()) return Integer.MAX_VALUE;

		skillName = skillName.toLowerCase(Locale.ROOT);

		boolean isForm = ConfigManager.getSkillsConfig().getFormSkills().contains(skillName);
		boolean isStack = ConfigManager.getSkillsConfig().getStackSkills().contains(skillName);

		if (isForm) {
			var raceConfig = ConfigManager.getRaceCharacter(statsData.getCharacter().getRaceName());
			if (raceConfig == null) return Integer.MAX_VALUE;
			Integer[] costs = raceConfig.getFormSkillTpCosts(skillName);
			if (costs != null && targetLevel >= 0 && targetLevel < costs.length) {
				Integer cost = costs[targetLevel];
				return cost != null ? cost : Integer.MAX_VALUE;
			}
			return Integer.MAX_VALUE;
		}

		if (isStack) {
			var skillData = ConfigManager.getSkillsConfig().getSkillCosts(skillName);
			if (skillData != null && skillData.getCosts() != null) {
				List<Integer> costs = skillData.getCosts();
				if (targetLevel >= 0 && targetLevel < costs.size()) {
					Integer cost = costs.get(targetLevel);
					return cost != null ? cost : Integer.MAX_VALUE;
				}
			}
			return Integer.MAX_VALUE;
		}

		var skillData = ConfigManager.getSkillsConfig().getSkills().get(skillName);
		if (skillData != null && skillData.getCosts() != null) {
			List<Integer> costs = skillData.getCosts();
			if (targetLevel >= 0 && targetLevel < costs.size()) {
				Integer cost = costs.get(targetLevel);
				return cost != null ? cost : Integer.MAX_VALUE;
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
		applyZoom(graphics, partialTick);

		float step = Math.max(0.01f, 0.07f + (partialTick * 0.01f));
		formsTransitionProgress = approach01(formsTransitionProgress, currentCategory == SkillCategory.FORMS ? 1.0f : 0.0f, step);

		int baseLeftOffset = getLeftPanelSwitchOffset(partialTick);

		boolean nearLeftEdge = uiMouseX <= 36;
		boolean overLeftPanel = uiMouseX >= currentLeftX && uiMouseX < currentLeftX + 141 + 42 && uiMouseY >= getUiHeight() / 2 - 105 && uiMouseY < getUiHeight() / 2 + 108;
		float leftTarget = (nearLeftEdge || overLeftPanel) ? 1.0f : 0.0f;
		leftPanelHoverProgress = approach01(leftPanelHoverProgress, leftTarget, step);

		int hiddenTravel = 141 - 24;
		int extraLeftOffset = (int) (-hiddenTravel * (1.0f - easeInOutCubic(leftPanelHoverProgress)) * easeInOutCubic(formsTransitionProgress));

		currentLeftX = 12 + baseLeftOffset + extraLeftOffset;

		int rightOffset = getRightPanelSwitchOffset(partialTick);
		int extraRightOffset = (int) (200 * easeInOutCubic(formsTransitionProgress));
		currentRightX = getUiWidth() - 158 + rightOffset + extraRightOffset;

		updateButtonAnimations(uiMouseX, uiMouseY, partialTick);

		if (formsTransitionProgress > 0.0f) {
			RenderSystem.enableBlend();
			renderFormsTree(graphics, uiMouseX, uiMouseY);
			RenderSystem.disableBlend();
		}

		float currentModelX = Mth.lerp(easeInOutCubic(formsTransitionProgress), getUiWidth() / 2 + 5, getUiWidth() - 80);
		renderPlayerModel(graphics, (int) currentModelX, getUiHeight() / 2 + 70, 75, uiMouseX, uiMouseY, formsTransitionProgress > 0.5f);

		renderLeftPanel(graphics, currentLeftX, getUiHeight() / 2 - 105, uiMouseX, uiMouseY);

		if (formsTransitionProgress < 1.0f) renderRightPanel(graphics, currentRightX, getUiHeight() / 2 - 105, uiMouseX, uiMouseY);

		super.render(graphics, uiMouseX, uiMouseY, partialTick);
		endUiScale(graphics);
	}

	private void renderFormsTree(GuiGraphics graphics, int mouseX, int mouseY) {
		int alpha = (int) (formsTransitionProgress * 255);
		int gridColor = (alpha << 24) | 0x222244;

		int spacing = 20;
		int offsetX = ((int) formsPanX) % spacing;
		int offsetY = ((int) formsPanY) % spacing;

		for (int x = offsetX - spacing; x < getUiWidth(); x += spacing) graphics.fill(x, 0, x + 1, getUiHeight(), gridColor);
		for (int y = offsetY - spacing; y < getUiHeight(); y += spacing) graphics.fill(0, y, getUiWidth(), y + 1, gridColor);

		graphics.pose().pushPose();
		graphics.pose().translate(getUiWidth() / 2f + formsPanX, getUiHeight() / 2f + formsPanY, 0);

		for (int i = 0; i < formNodes.size() - 1; i++) {
			FormNode current = formNodes.get(i);
			FormNode next = formNodes.get(i + 1);

			if (current.group.equals(next.group)) {
				int cx = (int) ((current.x + 16) * formsZoom);
				int cy = (int) ((current.y + 16) * formsZoom);
				int nx = (int) ((next.x + 16) * formsZoom);
				int ny = (int) ((next.y + 16) * formsZoom);

				drawThickLine(graphics, cx, cy, nx, ny, Math.max(1, (int) (3 * formsZoom)), (alpha << 24) | 0x555555);
			}
		}

		FormNode hovered = null;

		for (FormNode node : formNodes) {
			int nx = (int) (node.x * formsZoom);
			int ny = (int) (node.y * formsZoom);
			int size = (int) (32 * formsZoom);

			int screenNx = (int) (getUiWidth() / 2f + formsPanX + nx);
			int screenNy = (int) (getUiHeight() / 2f + formsPanY + ny);

			boolean isHovered = mouseX >= screenNx && mouseX <= screenNx + size && mouseY >= screenNy && mouseY <= screenNy + size;
			if (isHovered) hovered = node;

			Skill skill = statsData.getSkills().getSkill(node.formType);
			int currentLevel = skill != null ? skill.getLevel() : 0;

			int requiredLevel = node.data.getUnlockOnSkillLevel();
			boolean unlocked = currentLevel >= requiredLevel;
			boolean canPurchaseLevel = requiredLevel == currentLevel + 1;

			boolean selected = node.group.equalsIgnoreCase(selectedFormGroup) && node.data.getName().equalsIgnoreCase(selectedFormName);

			int borderColor = selected ? ((alpha << 24) | 0xFFFF00) : (unlocked ? ((alpha << 24) | 0x00AA00) : ((alpha << 24) | 0x333333));
			int bgColor = (alpha << 24) | 0x111111;

			graphics.fill(nx - 2, ny - 2, nx + size + 2, ny + size + 2, borderColor);
			graphics.fill(nx, ny, nx + size, ny + size, bgColor);

			ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/icons/" + node.formType.toLowerCase(Locale.ROOT) + ".png");

			if (unlocked) {
				float[] rgb = node.data.getRgbAuraColor();
				if (rgb != null && rgb.length >= 3) RenderSystem.setShaderColor(rgb[0], rgb[1], rgb[2], formsTransitionProgress);
				else RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, formsTransitionProgress);
			} else RenderSystem.setShaderColor(0.4f, 0.4f, 0.4f, formsTransitionProgress);

			graphics.blit(icon, nx + (int) (4 * formsZoom), ny + (int) (4 * formsZoom), 0, 0, (int) (24 * formsZoom), (int) (24 * formsZoom), (int) (24 * formsZoom), (int) (24 * formsZoom));
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

			int targetLevel = Math.max(0, requiredLevel - 1);
			int cost = getUpgradeCostForTargetLevel(node.formType, targetLevel);
			boolean isStack = ConfigManager.getSkillsConfig().getStackSkills().contains(node.formType.toLowerCase(Locale.ROOT));

			if (!unlocked && canPurchaseLevel && cost != -1 && cost != Integer.MAX_VALUE && statsData.getResources().getTrainingPoints() >= cost && !isStack) {
				int exX = nx + size - (int) (6 * formsZoom);
				int exY = ny - (int) (10 * formsZoom);

				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, formsTransitionProgress);
				graphics.blit(EXCLAMATION_MARK, exX, exY, (int) (6 * formsZoom), (int) (15 * formsZoom), 0, 0, 97, 250, 97, 250);
				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			}
		}

		graphics.pose().popPose();

		if (hovered != null && formsTransitionProgress > 0.8f) {
			List<Component> lines = new ArrayList<>();
			String race = statsData.getCharacter().getRaceName().toLowerCase(Locale.ROOT);
			boolean isStack = ConfigManager.getSkillsConfig().getStackSkills().contains(hovered.formType.toLowerCase(Locale.ROOT));

			if (isStack) {
				lines.add(Component.translatable("race.dragonminez.stack.group." + hovered.group).withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD));
				lines.add(Component.translatable("race.dragonminez.stack.form." + hovered.group + "." + hovered.data.getName()).withStyle(ChatFormatting.AQUA));
			} else {
				lines.add(Component.translatable("race.dragonminez." + race + ".group." + hovered.group).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
				lines.add(Component.translatable("race.dragonminez." + race + ".form." + hovered.group + "." + hovered.data.getName()).withStyle(ChatFormatting.YELLOW));
			}

			Skill skill = statsData.getSkills().getSkill(hovered.formType);

			int currentLevel = skill != null ? skill.getLevel() : 0;
			int requiredLevel = hovered.data.getUnlockOnSkillLevel();

			boolean unlocked = currentLevel >= requiredLevel;
			boolean canPurchaseLevel = requiredLevel == currentLevel + 1;

			lines.add(Component.translatable("skill.dragonminez." + hovered.formType).withStyle(ChatFormatting.GRAY).append(": " + requiredLevel).withStyle(ChatFormatting.GRAY));

			int targetLevel = Math.max(0, requiredLevel - 1);
			int cost = getUpgradeCostForTargetLevel(hovered.formType, targetLevel);

			if (unlocked) lines.add(Component.translatable("gui.dragonminez.skills.purchased").withStyle(ChatFormatting.GREEN));
			else if (cost == -1 || cost == Integer.MAX_VALUE) lines.add(Component.translatable("gui.dragonminez.skills.priceless").withStyle(ChatFormatting.DARK_RED));
			else if (!isStack) {
				lines.add(Component.translatable("gui.dragonminez.quests.rewards.tps", cost).withStyle(ChatFormatting.AQUA));
				if (canPurchaseLevel && statsData.getResources().getTrainingPoints() >= cost) lines.add(Component.translatable("gui.dragonminez.skills.doubleclick_buy").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
			}

			TextUtil.renderAdvancedTooltip(graphics, this.font, mouseX, mouseY, getUiWidth(), getUiHeight(), null, lines, null, 0xFFFFFF);
		}
	}

	private void drawThickLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int thickness, int color) {
		int dx = Math.abs(x2 - x1);
		int dy = Math.abs(y2 - y1);
		int steps = Math.max(1, Math.max(dx, dy));
		int half = Math.max(0, thickness / 2);

		for (int i = 0; i <= steps; i++) {
			float t = i / (float) steps;
			int x = Math.round(x1 + (x2 - x1) * t);
			int y = Math.round(y1 + (y2 - y1) * t);
			graphics.fill(x - half, y - half, x + half + 1, y + half + 1, color);
		}
	}

	private FormNode getHoveredFormNode(double uiMouseX, double uiMouseY) {
		for (FormNode node : formNodes) {
			int nx = (int) (node.x * formsZoom);
			int ny = (int) (node.y * formsZoom);
			int size = (int) (32 * formsZoom);
			int screenNx = (int) (getUiWidth() / 2f + formsPanX + nx);
			int screenNy = (int) (getUiHeight() / 2f + formsPanY + ny);

			if (uiMouseX >= screenNx && uiMouseX <= screenNx + size && uiMouseY >= screenNy && uiMouseY <= screenNy + size) return node;
		}
		return null;
	}

	private void updateButtonAnimations(int mouseX, int mouseY, float partialTick) {
		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;

		int hiddenX = currentLeftX + 122;
		int visibleX = currentLeftX + 141;
		int buttonWidth = 26;
		int panelWidth = 141;
		int panelHeight = 213;

		int hotZoneX = hiddenX;
		int hotZoneY = leftPanelY + 6;
		int hotZoneWidth = (visibleX - hiddenX) + buttonWidth;
		int hotZoneHeight = 133;

		boolean overPanel = mouseX >= currentLeftX && mouseX < currentLeftX + panelWidth &&
				mouseY >= leftPanelY && mouseY < leftPanelY + panelHeight;
		boolean overHotZone = mouseX >= hotZoneX && mouseX < hotZoneX + hotZoneWidth &&
				mouseY >= hotZoneY && mouseY < hotZoneY + hotZoneHeight;
		boolean shouldReveal = overPanel || overHotZone;

		float step = Math.max(0.01f, 0.07f + (partialTick * 0.01f));
		buttonRevealProgress = approach01(buttonRevealProgress, shouldReveal ? 1.0f : 0.0f, step);
		float animProgress = easeInOutCubic(buttonRevealProgress);

		int newX = hiddenX + (int) ((visibleX - hiddenX) * animProgress);
		skillsButton.setX(newX);
		kiButton.setX(newX);
		formsButton.setX(newX);
		stacksButton.setX(newX);

		int scissorXScreen = toScreenCoord(currentLeftX + 141);
		int scissorYScreen = toScreenCoord(0);
		int scissorRight = toScreenCoord(getUiWidth());
		int scissorBottom = toScreenCoord(getUiHeight());
		skillsButton.setScissorRect(scissorXScreen, scissorYScreen, scissorRight, scissorBottom);
		kiButton.setScissorRect(scissorXScreen, scissorYScreen, scissorRight, scissorBottom);
		formsButton.setScissorRect(scissorXScreen, scissorYScreen, scissorRight, scissorBottom);
		stacksButton.setScissorRect(scissorXScreen, scissorYScreen, scissorRight, scissorBottom);
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

	private void renderLeftPanel(GuiGraphics graphics, int panelX, int panelY, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		graphics.blit(MENU_BIG, panelX, panelY, 0, 0, 141, 213, 256, 256);
		graphics.blit(MENU_BIG, panelX + 17, panelY + 10, 142, 22, 107, 21, 256, 256);

		renderSkillsList(graphics, panelX, panelY, mouseX, mouseY);
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
				if (currentCategory == SkillCategory.KI || currentCategory == SkillCategory.STRIKE)
					displayName = getDisplayNameForEntry(skillName);
				else displayName = tr("skill.dragonminez." + skillName).getString();

				TextUtil.drawStringWithBorder(graphics, this.font, txt(displayName), panelX + 15, itemY + 5, color);

				if (currentCategory == SkillCategory.KI || currentCategory == SkillCategory.STRIKE) {
					TechniqueData technique = NEW_SKILL_ENTRY.equals(skillName) ? null : statsData.getTechniques().getUnlockedTechniques().get(skillName);
					if (technique != null) {
						String xpText = String.valueOf(technique.getExperience());
						int xpX = panelX + 130 - this.font.width(xpText);
						TextUtil.drawStringWithBorder(graphics, this.font, txt(xpText), xpX, itemY + 5, color);
					}
				} else if (skill != null) {
					String levelText = String.valueOf(skill.getLevel());
					int levelX = panelX + 130 - this.font.width(levelText);
					TextUtil.drawStringWithBorder(graphics, this.font, txt(levelText),
							levelX, itemY + 5, color);
				}
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
			case FORMS -> title = "gui.dragonminez.skills.tab.forms";
			case STRIKE -> title = "gui.dragonminez.skills.tab.strikeattacks";
		}

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr(title)
				.withStyle(style -> style.withBold(true)), panelX + 68, getUiHeight() / 2 - 88, 0xFBC51C);
	}

	private void renderRightPanel(GuiGraphics graphics, int panelX, int panelY, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

		if (currentCategory == SkillCategory.KI || currentCategory == SkillCategory.STRIKE) {
			graphics.blit(MENU_BIG, panelX, panelY, 0, 0, 141, 213, 256, 256);
			graphics.blit(MENU_BIG, panelX + 17, panelY + 10, 142, 22, 107, 21, 256, 256);
		} else {
			graphics.blit(MENU_SMALL, panelX, panelY, 0, 0, 141, 94, 256, 256);
			graphics.blit(MENU_BIG, panelX + 17, panelY + 10, 142, 22, 107, 21, 256, 256);
			graphics.blit(MENU_SMALL, panelX, panelY + 96, 0, 0, 141, 94, 256, 256);
			graphics.blit(MENU_SMALL, panelX, panelY + 190, 0, 154, 141, 32, 256, 256);
		}

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.info").withStyle(style -> style.withBold(true)), panelX + 70, panelY + 16, 0xFFFFD700);

		if (selectedSkill != null && statsData != null) {
			if (currentCategory == SkillCategory.KI && NEW_SKILL_ENTRY.equals(selectedSkill))
				renderNewSkillPlaceholder(graphics, panelX, panelY);
			else if (currentCategory == SkillCategory.KI || currentCategory == SkillCategory.STRIKE)
				renderTechniqueDetails(graphics, panelX, panelY, mouseX, mouseY);
			else renderSkillDetails(graphics, panelX, panelY);
		}
	}

	private void renderNewSkillPlaceholder(GuiGraphics graphics, int panelX, int panelY) {
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.skills.new_skill").withStyle(ChatFormatting.BOLD), panelX + 70, panelY + 48, 0xFFFFFFFF);
	}

	private void renderTechniqueDetails(GuiGraphics graphics, int panelX, int panelY, int mouseX, int mouseY) {
		TechniqueData tech = statsData.getTechniques().getUnlockedTechniques().get(selectedSkill);
		if (tech == null) return;

		int yOffset = panelY + 40;
		int xpReq = 100;
		int castTimeTicks = tech.getCastTime();
		int cooldownTicks = tech.getCooldown();

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr(tech.getName()).withStyle(ChatFormatting.BOLD), panelX + 70, yOffset, 0xFFFFFFFF);
		yOffset += 12;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.xp", tech.getExperience()), panelX + 70, yOffset, 0xFF55FF55);
		yOffset += 16;

		if (tech instanceof KiAttackData ki) {
			xpReq = ki.getUpgradeXpCost("damage");
			castTimeTicks = ki.getActualCastTime();
			cooldownTicks = ki.getActualCooldown();
			int scaledKiDamage = (int) (statsData.getKiDamage() * ki.getDamageMultiplier());

			TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.type").append(": ").append(tr("technique.type." + ki.getKiType().name().toLowerCase())), panelX + 15, yOffset, 0xDDDDDD);
			yOffset += 12;

			String utilKey = ki.getUtility() == KiAttackData.Utility.HEAL ? "gui.dragonminez.technique.heal" : "gui.dragonminez.technique.damage";
			TextUtil.drawStringWithBorder(graphics, this.font, tr(utilKey).append(": ").append(txt(String.valueOf(scaledKiDamage))), panelX + 15, yOffset, 0xFFFFFF);
			yOffset += 12;

			TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.size").append(": ").append(txt(String.format(Locale.US, "%.1f", ki.getSize()))), panelX + 15, yOffset, 0xFFFFFF);
			yOffset += 12;
			TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.speed").append(": ").append(txt(String.format(Locale.US, "%.1f", ki.getSpeed()))), panelX + 15, yOffset, 0xFFFFFF);
			yOffset += 12;
			TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.armor_pen").append(": ").append(txt(String.valueOf(ki.getArmorPenetration()))), panelX + 15, yOffset, 0xFFFFFF);
			yOffset += 12;
			TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.cast_time").append(": ").append(txt(String.format(Locale.US, "%.1fs", castTimeTicks / 20.0f))), panelX + 15, yOffset, 0xFFFFFF);
			yOffset += 12;
		} else if (tech instanceof StrikeAttackData st) {
			castTimeTicks = st.getActualCastTime();
			cooldownTicks = st.getActualCooldown();
			int scaledStrikeDamage = (int) (statsData.getStrikeDamage() * st.getDamageMultiplier());
			TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.type").append(": ").append(tr("technique.type.strike")), panelX + 15, yOffset, 0xDDDDDD);
			yOffset += 12;
			TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.damage").append(": ").append(txt(String.valueOf(scaledStrikeDamage))), panelX + 15, yOffset, 0xFFFFFF);
			yOffset += 12;
			TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.cast_time").append(": ").append(txt(String.format(Locale.US, "%.1fs", castTimeTicks / 20.0f))), panelX + 15, yOffset, 0xFFFFFF);
			yOffset += 12;
		}

		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.cooldown").append(": ").append(txt(String.format(Locale.US, "%.1fs", cooldownTicks / 20.0f))), panelX + 15, yOffset, 0xFFFFFF);
		yOffset += 16;

		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.energy_cost").append(": ").append(txt(String.format(Locale.US, "%.1f", tech.getCalculatedCost(statsData)))), panelX + 15, yOffset, 0xFFAAAA);
		yOffset += 16;

		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.req_xp", xpReq), panelX + 15, yOffset, 0xFFAAAAAA);
		renderActionStatus(graphics, panelX, panelY);
	}

	private void attemptTechniqueImport() {
		if (statsData == null || techniqueImportBox == null) return;
		String code = techniqueImportBox.getValue() != null ? techniqueImportBox.getValue().trim() : "";
		if (code.isEmpty()) {
			setActionStatus(tr("gui.dragonminez.skills.status.invalid_code"), 0xFF5555);
			return;
		}
		NetworkHandler.INSTANCE.sendToServer(new ImportTechniqueC2S(code));
	}

	private void setActionStatus(Component text, int color) {
		actionStatusText = text;
		actionStatusColor = color;
		actionStatusTimer = 60;
	}

	private void renderActionStatus(GuiGraphics graphics, int panelX, int panelY) {
		if (actionStatusTimer <= 0) return;
		int alpha = (int) Math.max(0, Math.min(255, (actionStatusTimer / 60.0f) * 255.0f));
		if (alpha <= 0) return;
		int colorWithAlpha = (alpha << 24) | (actionStatusColor & 0x00FFFFFF);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		int yPos = panelY + 208;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, actionStatusText, panelX + 70, yPos, colorWithAlpha);
		RenderSystem.disableBlend();
	}

	private void consumePendingImportStatus() {
		if (pendingImportStatusText == null) return;
		setActionStatus(pendingImportStatusText, pendingImportStatusColor);
		pendingImportStatusText = null;
	}

	public static void handleTechniqueImportResult(com.dragonminez.common.network.S2C.TechniqueImportResultS2C.Status status, int value) {
		switch (status) {
			case INVALID -> {
				pendingImportStatusText = Component.translatable("gui.dragonminez.skills.status.invalid_code");
				pendingImportStatusColor = 0xFF5555;
			}
			case NOT_ENOUGH_TP -> {
				pendingImportStatusText = Component.translatable("gui.dragonminez.skills.status.not_enough_tp", value);
				pendingImportStatusColor = 0xFF5555;
			}
			case IMPORTED -> {
				pendingImportStatusText = Component.translatable("gui.dragonminez.skills.status.imported_used_tp", value);
				pendingImportStatusColor = 0x55FF55;
			}
		}
	}

	private void renderSkillDetails(GuiGraphics graphics, int panelX, int panelY) {
		Skill skill = statsData.getSkills().getSkill(selectedSkill);
		if (skill == null && !selectedSkill.startsWith("racial_")) return;
		GeneralServerConfig.RacialSkillsConfig config = ConfigManager.getServerConfig().getRacialSkills();
		String displayName = tr("skill.dragonminez." + selectedSkill).getString();

		String description = "";
		if (selectedSkill.startsWith("racial_")) {
			switch (selectedSkill) {
				case "racial_human" -> {
					int regen = (int) Math.round((config.getHumanKiRegenBoost() - 1.0) * 100);
					description = tr("skill.dragonminez.racial_human.desc", regen).getString();
				}
				case "racial_saiyan" -> {
					int zenkaiHealth = (int) Math.round((config.getSaiyanZenkaiHealthRegen() * 100));
					int zenkaiStat = (int) Math.round((config.getSaiyanZenkaiStatBoost() * 100));
					int cooldown = config.getSaiyanZenkaiCooldownSeconds();
					description = tr("skill.dragonminez.racial_saiyan.desc", zenkaiHealth, zenkaiStat, cooldown).getString();
				}
				case "racial_namekian" -> {
					int assimHealth = (int) Math.round(config.getNamekianAssimilationHealthRegen() * 100);
					int assimStat = (int) Math.round(config.getNamekianAssimilationStatBoost() * 100);
					description = tr("skill.dragonminez.racial_namekian.desc", assimHealth, assimStat).getString();
				}
				case "racial_frostdemon" -> {
					int tpBoost = (int) Math.round((config.getFrostDemonTPBoost() - 1.0) * 100);
					description = tr("skill.dragonminez.racial_frostdemon.desc", tpBoost).getString();
				}
				case "racial_bioandroid" -> {
					int drainRatio = (int) Math.round(config.getBioAndroidDrainRatio() * 100);
					int cooldown = config.getBioAndroidCooldownSeconds();
					description = tr("skill.dragonminez.racial_bioandroid.desc", drainRatio, cooldown).getString();
				}
				case "racial_majin" -> {
					int absHealth = (int) Math.round(config.getMajinAbsorptionHealthRegen() * 100);
					int absStat = (int) Math.round(config.getMajinAbsorptionStatCopy() * 100);
					description = tr("skill.dragonminez.racial_majin.desc", absHealth, absStat).getString();
				}
			}
		} else description = tr("skill.dragonminez." + selectedSkill + ".desc").getString();

		int startY = panelY + 40;

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(displayName).withStyle(ChatFormatting.BOLD), panelX + 72, startY, 0xFFFFFFFF);

		if (skill != null) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.skills.level", skill.getLevel(), skill.getMaxLevel()), panelX + 72, startY + 12, 0xFFAAAAAA);
			int upgradeCost = getUpgradeCost(selectedSkill, skill.getLevel());
			if (upgradeCost != Integer.MAX_VALUE && upgradeCost > 0) {
				TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt("%d TPS".formatted(getUpgradeCost(selectedSkill, skill.getLevel()))), panelX + 72, startY + 24, 0xFFAAAAAA);
			}
		} else {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.skills.racial"), panelX + 72, startY + 12, 0xFF55FF55);
		}

		List<String> wrappedDesc = wrapText(description, 120);

		int descY = startY + 70;
		int boxX = panelX + 13;
		int boxW = 130;
		int lineHeight = this.font.lineHeight + 2;
		int viewHeight = 6 * lineHeight;
		int totalContentHeight = wrappedDesc.size() * lineHeight;

		maxDescScroll = Math.max(0, totalContentHeight - viewHeight);
		targetDescScroll = Mth.clamp(targetDescScroll, 0, maxDescScroll);
		float tickDelta = Minecraft.getInstance().getDeltaFrameTime();
		currentDescScroll = Mth.lerp(tickDelta * 0.4f, currentDescScroll, targetDescScroll);

		TextUtil.renderScrollableText(graphics, this.font, wrappedDesc, boxX, descY, boxW, viewHeight, currentDescScroll, maxDescScroll, 0xFFCCCCCC);
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
		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;
		int scrollAmount = (int) Math.signum(delta);

		if (uiMouseX >= currentLeftX && uiMouseX <= currentLeftX + 141 && uiMouseY >= leftPanelY && uiMouseY <= leftPanelY + 213) {
			targetScroll = Mth.clamp(targetScroll - (scrollAmount * SKILL_ITEM_HEIGHT * 2), 0, maxScroll);
			return true;
		}

		if (formsTransitionProgress < 1.0f) {
			int descBoxX = currentRightX + 10;
			int descBoxY = (centerY - 105) + 110;
			int descBoxW = 136;
			int descBoxH = 6 * 12;

			if (uiMouseX >= descBoxX && uiMouseX <= descBoxX + descBoxW && uiMouseY >= descBoxY && uiMouseY <= descBoxY + descBoxH) {
				targetDescScroll = Mth.clamp(targetDescScroll - (scrollAmount * 12 * 2), 0, maxDescScroll);
				return true;
			}

			if (uiMouseX >= currentRightX && uiMouseX <= currentRightX + 141 && uiMouseY >= leftPanelY && uiMouseY <= leftPanelY + 213) {
				return true;
			}
		}

		if (currentCategory == SkillCategory.FORMS) {
			formsZoom = Math.max(0.25f, Math.min(2.0f, formsZoom + scrollAmount * 0.1f));
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	private float calculateScrollPercent(double uiMouseY, int startY, int scrollBarHeight) {
		float percent = (float) (uiMouseY - startY) / scrollBarHeight;
		return Mth.clamp(percent, 0.0f, 1.0f);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;

		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);

		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;

		int startY = leftPanelY + 30;
		int scrollBarHeight = MAX_VISIBLE_SKILLS * SKILL_ITEM_HEIGHT;
		int scrollBarX = currentLeftX + 135;

		if (maxScroll > 0 && uiMouseX >= scrollBarX - 5 && uiMouseX <= scrollBarX + 10 && uiMouseY >= startY && uiMouseY <= startY + scrollBarHeight) {
			isDraggingMainScroll = true;
			targetScroll = calculateScrollPercent(uiMouseY, startY, scrollBarHeight) * maxScroll;
			return true;
		}

		List<String> skillNames = getVisibleSkillNames();

		if (uiMouseX >= currentLeftX + 10 && uiMouseX <= currentLeftX + 100 && uiMouseY >= startY && uiMouseY <= startY + scrollBarHeight) {
			int index = (int) ((uiMouseY - startY + currentScroll) / SKILL_ITEM_HEIGHT);

			if (index >= 0 && index < skillNames.size()) {
				selectedSkill = skillNames.get(index);

				if (currentCategory == SkillCategory.FORMS) {
					selectedFormName = selectedSkill;

					for (FormNode fn : formNodes) {
						if (fn.data.getName().equals(selectedFormName)) {
							selectedFormGroup = fn.group;
							formsPanX = -fn.x;
							formsPanY = -fn.y;
							break;
						}
					}
				} else {
					targetDescScroll = 0;
					currentDescScroll = 0;
				}

				refreshButtons();
				return true;
			}
		}

		if (uiMouseX >= currentLeftX && uiMouseX <= currentLeftX + 141 && uiMouseY >= leftPanelY && uiMouseY <= leftPanelY + 213) {
			return true;
		}

		if (formsTransitionProgress < 1.0f) {
			int descBoxY = (centerY - 105) + 110;
			int descBoxH = 6 * 12;
			int descScrollBarX = currentRightX + 140;

			if (maxDescScroll > 0 && uiMouseX >= descScrollBarX - 5 && uiMouseX <= descScrollBarX + 10 && uiMouseY >= descBoxY && uiMouseY <= descBoxY + descBoxH) {
				isDraggingDescScroll = true;
				targetDescScroll = calculateScrollPercent(uiMouseY, descBoxY, descBoxH) * maxDescScroll;
				return true;
			}

			if (uiMouseX >= currentRightX && uiMouseX <= currentRightX + 141 && uiMouseY >= leftPanelY && uiMouseY <= leftPanelY + 213) {
				return true;
			}
		}

		if (currentCategory == SkillCategory.FORMS && button == 0) {
			FormNode clicked = getHoveredFormNode(uiMouseX, uiMouseY);

			if (clicked != null) {
				long now = System.currentTimeMillis();
				boolean sameSelection = clicked.group.equalsIgnoreCase(selectedFormGroup) && clicked.data.getName().equalsIgnoreCase(selectedFormName);

				if (sameSelection && (now - lastFormClickTime < 500)) {
					Skill skill = statsData.getSkills().getSkill(clicked.formType);
					int currentLevel = skill != null ? skill.getLevel() : 0;
					int requiredLevel = clicked.data.getUnlockOnSkillLevel();
					boolean canPurchaseLevel = requiredLevel == currentLevel + 1;
					int targetLevel = Math.max(0, requiredLevel - 1);
					int cost = getUpgradeCostForTargetLevel(clicked.formType, targetLevel);

					if (canPurchaseLevel && cost != -1 && cost != Integer.MAX_VALUE && statsData.getResources().getTrainingPoints() >= cost) {
						NetworkHandler.INSTANCE.sendToServer(new UpdateSkillC2S(UpdateSkillC2S.SkillAction.UPGRADE, clicked.formType, cost));
						updateStatsData();
					}
				} else {
					selectedFormName = clicked.data.getName();
					selectedFormGroup = clicked.group;
					selectedSkill = selectedFormName;
					refreshButtons();
				}
				lastFormClickTime = now;
				return true;
			} else {
				isDraggingForms = true;
				dragFormStartX = uiMouseX;
				dragFormStartY = uiMouseY;
				dragFormStartPanX = formsPanX;
				dragFormStartPanY = formsPanY;

				return true;
			}
		}

		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		double uiMouseY = toUiY(mouseY);
		double uiMouseX = toUiX(mouseX);

		if (isDraggingForms && button == 0) {
			formsPanX = dragFormStartPanX + (float) (uiMouseX - dragFormStartX);
			formsPanY = dragFormStartPanY + (float) (uiMouseY - dragFormStartY);
			return true;
		}

		if (isDraggingMainScroll && maxScroll > 0) {
			int centerY = getUiHeight() / 2;
			int startY = (centerY - 105) + 30;
			int scrollBarHeight = MAX_VISIBLE_SKILLS * SKILL_ITEM_HEIGHT;
			targetScroll = calculateScrollPercent(uiMouseY, startY, scrollBarHeight) * maxScroll;
			return true;
		}

		if (isDraggingDescScroll && maxDescScroll > 0) {
			int centerY = getUiHeight() / 2;
			int descBoxY = (centerY - 105) + 110;
			int descBoxH = 6 * 12;
			targetDescScroll = calculateScrollPercent(uiMouseY, descBoxY, descBoxH) * maxDescScroll;
			return true;
		}

		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (isDraggingForms) {
			isDraggingForms = false;
			return true;
		}
		if (isDraggingMainScroll || isDraggingDescScroll) {
			isDraggingMainScroll = false;
			isDraggingDescScroll = false;
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	private String getDisplayNameForEntry(String entryId) {
		if (NEW_SKILL_ENTRY.equals(entryId)) return tr("gui.dragonminez.skills.new_skill").getString();
		if (statsData == null) return entryId;
		TechniqueData technique = statsData.getTechniques().getUnlockedTechniques().get(entryId);
		if (technique == null || technique.getName() == null || technique.getName().isEmpty()) return entryId;
		String rawName = technique.getName();
		if (rawName.contains(".")) return tr(rawName).getString();
		return rawName;
	}

	private void openTechniqueCreator() {
		if (this.minecraft != null) this.minecraft.setScreen(new TechniqueCreatorScreen(this));
	}

	private void renderPlayerModel(GuiGraphics graphics, int x, int y, int scale, float mouseX, float mouseY, boolean isFormPreview) {
		LivingEntity player = Minecraft.getInstance().player;
		if (player == null) return;

		String activeFormGroupO = null;
		String activeFormO = null;
		String activeStackFormGroupO = null;
		String activeStackFormO = null;
		Character character = null;

		if (isFormPreview && selectedFormName != null && selectedFormGroup != null) {
			var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
			if (stats != null) {
				character = stats.getCharacter();
				activeFormGroupO = character.getActiveFormGroup();
				activeFormO = character.getActiveForm();
				activeStackFormGroupO = character.getActiveStackFormGroup();
				activeStackFormO = character.getActiveStackForm();

				character.clearActiveForm();
				character.clearActiveStackForm();

				if (ConfigManager.getSkillsConfig().getStackSkills().contains(selectedFormName)) character.setActiveStackForm(selectedFormGroup, selectedFormName);
				else character.setActiveForm(selectedFormGroup, selectedFormName);
			}
		}

		int adjustedScale = getAdjustedModelScale(scale);

		float xRotation = (float) Math.atan((double) ((float) y - mouseY) / 40.0F);
		float yRotation = (float) Math.atan((double) ((float) x - mouseX) / 40.0F);

		Quaternionf pose = (new Quaternionf()).rotateZ((float) Math.PI);
		Quaternionf cameraOrientation = (new Quaternionf()).rotateX(xRotation * 20.0F * ((float) Math.PI / 180F));
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

		if (character != null) {
			character.clearActiveForm();
			character.clearActiveStackForm();
			character.setActiveForm(activeFormGroupO, activeFormO);
			character.setActiveStackForm(activeStackFormGroupO, activeStackFormO);
		}
	}
}