package com.dragonminez.client.gui.config;

import com.dragonminez.client.util.ScrollbarState;
import com.dragonminez.common.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@OnlyIn(Dist.CLIENT)
public class DMZModConfigScreen extends Screen {

	private static final int ROW_HEIGHT = 12;
	private static final int LIST_TOP = 40;
	private static final int LIST_BOTTOM_MARGIN = 40;

	private final Screen parent;
	private final List<String> allFiles = new ArrayList<>();
	private final List<String> files = new ArrayList<>();
	private EditBox searchBox;
	private final ScrollbarState scrollBar = new ScrollbarState().minThumb(20).step(ROW_HEIGHT);

	public DMZModConfigScreen(Screen parent) {
		super(Component.translatable("gui.dragonminez.modconfig.title"));
		this.parent = parent;
	}

	private int listBottom() {
		return this.height - LIST_BOTTOM_MARGIN;
	}

	@Override
	protected void init() {
		super.init();
		allFiles.clear();
		allFiles.addAll(ConfigManager.getAvailableConfigFiles());
		allFiles.sort(String::compareToIgnoreCase);

		searchBox = new EditBox(this.font, this.width / 2 - 100, 20, 200, 14,
				Component.translatable("gui.dragonminez.modconfig.search"));
		searchBox.setHint(Component.translatable("gui.dragonminez.modconfig.search"));
		searchBox.setResponder(v -> {
			applyFilter(v);
			scrollBar.reset();
		});
		addWidget(searchBox);

		applyFilter("");

		int bottomY = this.height - 28;
		if (this.minecraft != null && this.minecraft.player != null) {
			addRenderableWidget(Button.builder(Component.translatable("gui.dragonminez.modconfig.reload"),
							b -> runReload())
					.bounds(this.width / 2 - 154, bottomY, 150, 20).build());
			addRenderableWidget(Button.builder(Component.translatable("gui.dragonminez.modconfig.done"),
							b -> onClose())
					.bounds(this.width / 2 + 4, bottomY, 150, 20).build());
		} else {
			addRenderableWidget(Button.builder(Component.translatable("gui.dragonminez.modconfig.done"),
							b -> onClose())
					.bounds(this.width / 2 - 75, bottomY, 150, 20).build());
		}
	}

	private void applyFilter(String query) {
		files.clear();
		String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		for (String f : allFiles) {
			if (q.isEmpty() || f.toLowerCase(Locale.ROOT).contains(q)) files.add(f);
		}
	}

	private void runReload() {
		if (this.minecraft != null && this.minecraft.player != null) {
			this.minecraft.player.connection.sendCommand("dmzreload");
			onClose();
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
		searchBox.render(graphics, mouseX, mouseY, partialTick);

		int top = LIST_TOP;
		int bottom = listBottom();
		scrollBar.layout(0, top, this.width, bottom - top, files.size() * ROW_HEIGHT).barAt(this.width / 2 + 156);
		boolean overList = scrollBar.isInView(mouseX, mouseY);
		double contentMouseY = scrollBar.toContent(mouseY);
		scrollBar.beginClip(graphics);
		for (int i = 0; i < files.size(); i++) {
			int y = top + i * ROW_HEIGHT;
			if (!scrollBar.isVisible(y, ROW_HEIGHT)) continue;
			boolean hovered = overList && mouseX >= this.width / 2 - 150 && mouseX <= this.width / 2 + 150
					&& contentMouseY >= y && contentMouseY < y + ROW_HEIGHT;
			int color = hovered ? 0xFFFFD700 : 0xFFCCCCCC;
			graphics.drawString(this.font, files.get(i), this.width / 2 - 150, y + 2, color);
		}
		scrollBar.endClip(graphics);

		if (files.isEmpty()) {
			graphics.drawCenteredString(this.font, Component.translatable("gui.dragonminez.modconfig.empty"),
					this.width / 2, top + 10, 0xFF888888);
		}

		scrollBar.renderBar(graphics, mouseX, mouseY);

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (scrollBar.mouseClicked(mouseX, mouseY, button)) return true;
		if (button == 0) {
			int top = LIST_TOP;
			int bottom = listBottom();
			if (mouseX >= this.width / 2.0 - 150 && mouseX <= this.width / 2.0 + 150
					&& mouseY >= top && mouseY < bottom) {
				int index = (int) ((mouseY - top + scrollBar.scroll()) / ROW_HEIGHT);
				if (index >= 0 && index < files.size() && this.minecraft != null) {
					this.minecraft.setScreen(new DMZConfigEditScreen(this, files.get(index)));
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (scrollBar.mouseDragged(mouseX, mouseY)) return true;
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (scrollBar.mouseReleased()) return true;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (scrollBar.scrollWheel(delta)) return true;
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) this.minecraft.setScreen(parent);
	}
}
