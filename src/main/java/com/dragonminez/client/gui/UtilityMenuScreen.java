package com.dragonminez.client.gui;

import com.dragonminez.client.gui.utilitymenu.ButtonInfo;
import com.dragonminez.client.gui.utilitymenu.IUtilityMenuSlot;
import com.dragonminez.client.gui.utilitymenu.menuslots.*;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.C2S.ExecuteActionC2S;
import com.dragonminez.common.network.C2S.SwitchActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.ActionMode;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.TransformationsHelper;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class UtilityMenuScreen extends Screen {
	private List<IUtilityMenuSlot> menuSlots = new ArrayList<>();

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

		IUtilityMenuSlot emptySlot = new EmptyMenuSlot();
		menuSlots.add(0, new KaiokenMenuSlot());
		menuSlots.add(1, new SuperformMenuSlot());
		menuSlots.add(2, new FusionMenuSlot());
		menuSlots.add(3, emptySlot);
		menuSlots.add(4, emptySlot);
		menuSlots.add(5, new KiManipulationMenuSlot());
		menuSlots.add(6, new RacialActionMenuSlot());
		menuSlots.add(7, new DescendFormMenuSlot());
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
		IUtilityMenuSlot menuSlot = menuSlots.get(index);
		if (menuSlot != null) {
			ButtonInfo buttonInfo = menuSlot.render(statsData);
			if (buttonInfo != null) {
				if (buttonInfo.isSelected()) {
					buttonInfo.setColor(0x2BFF00);
				}

				if (!buttonInfo.getLine1().getString().isEmpty()) {
					drawCenteredStringWithBorder(graphics, buttonInfo.getLine1(), x + BUTTON_WIDTH / 2, y + 12, 0xFFFFFF, 0x000000);
					if (index == 5) graphics.drawCenteredString(font, buttonInfo.getLine2(), x + BUTTON_WIDTH / 2, y + 30, statsData.getSkills().isSkillActive("kimanipulation") ? 0x2BFF00 : 0xFF1B00);
					else graphics.drawCenteredString(font, buttonInfo.getLine2(), x + BUTTON_WIDTH / 2, y + 30, buttonInfo.isSelected() ? buttonInfo.getColor() : 0xFF1B00);
				}
			}
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
		IUtilityMenuSlot menuSlot = menuSlots.get(index);
		if (menuSlot != null) {
			menuSlot.handle(statsData);
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