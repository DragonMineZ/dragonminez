package com.dragonminez.client.gui;

import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.C2S.ExecuteActionC2S;
import com.dragonminez.common.network.C2S.SwitchActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.ActionMode;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class UtilityMenuScreen extends Screen {

	private long openTime;
	private static final long ANIMATION_DURATION = 100;
	private StatsData statsData;

	private final int BUTTON_WIDTH = 105;
	private final int BUTTON_HEIGHT = 70;
	private final int GAP = 5;

	public UtilityMenuScreen() {
		super(Component.literal("Menu"));
		this.openTime = System.currentTimeMillis();
	}

	@Override
	protected void init() {
		super.init();
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> this.statsData = data);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (statsData == null) return;

		long elapsed = System.currentTimeMillis() - openTime;
		float scale = Math.min(1.0f, (float) elapsed / ANIMATION_DURATION);

		PoseStack pose = graphics.pose();
		pose.pushPose();

		int centerX = this.width / 2;
		int centerY = this.height / 2;

		pose.translate(centerX, centerY, 0);
		pose.scale(scale, scale, 1.0f);
		pose.translate(-centerX, -centerY, 0);

		renderGrid(graphics, centerX, centerY, mouseX, mouseY);

		pose.popPose();
	}

	private void renderGrid(GuiGraphics graphics, int centerX, int centerY, int mouseX, int mouseY) {
		int[][] positions = {
				{-1, -1}, {0, -1}, {1, -1},
				{-1, 0},           {1, 0},
				{-1, 1},  {0, 1},  {1, 1}
		};

		for (int i = 0; i < 8; i++) {
			int col = positions[i][0];
			int row = positions[i][1];

			int x = centerX + (col * (BUTTON_WIDTH + GAP)) - (BUTTON_WIDTH / 2);
			int y = centerY + (row * (BUTTON_HEIGHT + GAP)) - (BUTTON_HEIGHT / 2);

			boolean isHovered = mouseX >= x && mouseX <= x + BUTTON_WIDTH && mouseY >= y && mouseY <= y + BUTTON_HEIGHT;
			int color = isHovered ? 0x80FFFFFF : 0x60000000;

			graphics.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, color);

			renderButtonContent(graphics, i, x, y);
		}
	}

	private void renderButtonContent(GuiGraphics graphics, int index, int x, int y) {
		Component line1 = Component.empty();
		Component line2 = Component.empty();
		int color = 0xFFFFFF;
		boolean isSelected = false;

		ActionMode currentMode = statsData.getStatus().getSelectedAction();
		String race = statsData.getCharacter().getRaceName();

		switch (index) {
			case 0 -> {
				if (statsData.getSkills().hasSkill("kaioken")) {
					line1 = Component.translatable("gui.action.dragonminez.kaioken").withStyle(ChatFormatting.BOLD);
					line2 = Component.translatable("gui.action.dragonminez." + (statsData.getStatus().getSelectedAction() == ActionMode.KAIOKEN ? "true" : "false"));
					isSelected = currentMode == ActionMode.KAIOKEN;
				}
			}
			case 1 -> {
				if (statsData.getSkills().getSkillLevel("superform") >= 1 || statsData.getSkills().getSkillLevel("legendaryforms") >= 1 || statsData.getSkills().getSkillLevel("godform") >= 1) {
					line1 = Component.translatable("race.dragonminez." + race + ".group." + statsData.getCharacter().getSelectedFormGroup()).withStyle(ChatFormatting.BOLD);
					line2 = Component.translatable("race.dragonminez." + race + ".form." + statsData.getCharacter().getSelectedFormGroup() + "." + statsData.getCharacter().getActiveForm());
					isSelected = currentMode == ActionMode.FORM;
				}
			}
			case 2 -> {
				if (statsData.getSkills().hasSkill("fusion")) {
					line1 = Component.translatable("gui.action.dragonminez.fusion").withStyle(ChatFormatting.BOLD);
					line2 = Component.translatable("gui.action.dragonminez." + statsData.getSkills().isSkillActive("fusion"));
					isSelected = currentMode == ActionMode.FUSION;
				}
			}
			case 5 -> {
				if (statsData.getSkills().hasSkill("kimanipulation") && statsData.getSkills().hasSkill("kicontrol")) {
					String weaponType = statsData.getStatus().getKiWeaponType();
					line1 = Component.translatable("skill.dragonminez.kiweapon." + weaponType).withStyle(ChatFormatting.BOLD);
					line2 = Component.translatable("gui.action.dragonminez." + statsData.getSkills().isSkillActive("kimanipulation"));
				}
			}
			case 6 -> {
				if ("saiyan".equals(race)) {
					line1 = Component.translatable("gui.action.dragonminez.tail").withStyle(ChatFormatting.BOLD);
					line2 = Component.translatable("gui.action.dragonminez." + (statsData.getStatus().isTailVisible()));
					isSelected = statsData.getStatus().isTailVisible();
				} else if ("namekian".equals(race) || "bioandroid".equals(race) || "majin".equals(race)) {
					line1 = Component.translatable("gui.action.dragonminez.racial." + race).withStyle(ChatFormatting.BOLD);
					line2 = Component.translatable("gui.action.dragonminez." + (statsData.getStatus().getSelectedAction() == ActionMode.RACIAL ? "true" : "false"));
					isSelected = currentMode == ActionMode.RACIAL;
				}
			}
			case 7 -> {
				if ("frostdemon".equals(race) || "majin".equals(race) || "bioandroid".equals(race)) {
					line1 = Component.translatable("gui.action.dragonminez.descend").withStyle(ChatFormatting.BOLD);
					line2 = Component.translatable("gui.action.dragonminez.revert_form");
				}
			}
		}

		if (isSelected) color = 0x2BFF00;

		if (!line1.getString().isEmpty()) {
			drawCenteredStringWithBorder(graphics, line1, x + BUTTON_WIDTH / 2, y + 12, 0xFFFFFF, 0x000000);
			if (index == 5) graphics.drawCenteredString(font, line2, x + BUTTON_WIDTH / 2, y + 30, statsData.getSkills().isSkillActive("kimanipulation") ? 0x2BFF00 : 0xFF1B00);
			else graphics.drawCenteredString(font, line2, x + BUTTON_WIDTH / 2, y + 30, isSelected ? color : 0xFF1B00);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int centerX = this.width / 2;
		int centerY = this.height / 2;

		int[][] positions = {
				{-1, -1}, {0, -1}, {1, -1},
				{-1, 0},           {1, 0},
				{-1, 1},  {0, 1},  {1, 1}
		};

		for (int i = 0; i < 8; i++) {
			int col = positions[i][0];
			int row = positions[i][1];

			int x = centerX + (col * (BUTTON_WIDTH + GAP)) - (BUTTON_WIDTH / 2);
			int y = centerY + (row * (BUTTON_HEIGHT + GAP)) - (BUTTON_HEIGHT / 2);

			if (mouseX >= x && mouseX <= x + BUTTON_WIDTH && mouseY >= y && mouseY <= y + BUTTON_HEIGHT) {
				handleSlotClick(i);
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private void handleSlotClick(int index) {
		String race = statsData.getCharacter().getRaceName();
		Minecraft mc = Minecraft.getInstance();

		switch (index) {
			case 0 -> {
				if (statsData.getSkills().hasSkill("kaioken")) {
					boolean wasActive = statsData.getStatus().getSelectedAction() == ActionMode.KAIOKEN;
					NetworkHandler.sendToServer(new SwitchActionC2S(ActionMode.KAIOKEN));
					playToggleSound(mc, !wasActive);
				}
			}
			case 1 -> {
				if (statsData.getSkills().getSkillLevel("superform") >= 1 || statsData.getSkills().getSkillLevel("legendaryforms") >= 1 || statsData.getSkills().getSkillLevel("godform") >= 1) {
					boolean wasActive = statsData.getStatus().getSelectedAction() == ActionMode.FORM;
					NetworkHandler.sendToServer(new SwitchActionC2S(ActionMode.FORM));
					playToggleSound(mc, !wasActive);
				}
			}
			case 2 -> {
				if (statsData.getSkills().hasSkill("fusion")) {
					boolean wasActive = statsData.getStatus().getSelectedAction() == ActionMode.FUSION;
					NetworkHandler.sendToServer(new SwitchActionC2S(ActionMode.FUSION));
					playToggleSound(mc, !wasActive);
				}
			}
			case 5 -> {
				if (statsData.getSkills().hasSkill("kimanipulation") && statsData.getSkills().hasSkill("kicontrol")) {
					boolean wasActive = statsData.getSkills().isSkillActive("kimanipulation");
					NetworkHandler.sendToServer(new ExecuteActionC2S("toggle_ki_weapon"));
					playToggleSound(mc, !wasActive);
				}
			}
			case 6 -> {
				if ("saiyan".equals(race)) {
					boolean wasActive = statsData.getStatus().isTailVisible();
					NetworkHandler.sendToServer(new ExecuteActionC2S("toggle_tail"));
					playToggleSound(mc, !wasActive);
				}
				else if ("namekian".equals(race) || "bioandroid".equals(race) || "majin".equals(race)) {
					boolean wasActive = statsData.getStatus().getSelectedAction() == ActionMode.RACIAL;
					NetworkHandler.sendToServer(new SwitchActionC2S(ActionMode.RACIAL));
					playToggleSound(mc, !wasActive);
				}
			}
			case 7 -> {
				if ("frostdemon".equals(race) || "majin".equals(race) || "bioandroid".equals(race)) {
					NetworkHandler.sendToServer(new ExecuteActionC2S("descend"));
					// Descender siempre es desactivar, entonces suena OFF
					playToggleSound(mc, false);
				}
			}
		}
	}

	private void playToggleSound(Minecraft mc, boolean turnedOn) {
		if (mc.player != null) {
			if (turnedOn) {
				mc.player.playSound(MainSounds.SWITCH_ON.get(), 1.0F, 1.0F);
			} else {
				mc.player.playSound(MainSounds.SWITCH_OFF.get(), 1.0F, 1.0F);
			}
		}
	}

	@Override
	public void tick() {
		super.tick();
		Minecraft mc = Minecraft.getInstance();

		boolean isMenuKeyDown = InputConstants.isKeyDown(
				mc.getWindow().getWindow(),
				KeyBinds.UTILITY_MENU.getKey().getValue()
		);

		if (!isMenuKeyDown) {
			this.onClose();
		}
	}

	public void drawCenteredStringWithBorder(GuiGraphics graphics, Component text, int x, int y, int color, int borderColor) {
		graphics.drawString(font, text, x - font.width(text) / 2 - 1, y, borderColor, false);
		graphics.drawString(font, text, x - font.width(text) / 2 + 1, y, borderColor, false);
		graphics.drawString(font, text, x - font.width(text) / 2, y - 1, borderColor, false);
		graphics.drawString(font, text, x - font.width(text) / 2, y + 1, borderColor, false);
		graphics.drawString(font, text, x - font.width(text) / 2, y, color, false);
	}
}