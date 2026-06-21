package com.dragonminez.client.gui.config;

import com.dragonminez.common.config.ConfigManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
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
import java.util.Map;
import java.util.function.Consumer;


@OnlyIn(Dist.CLIENT)
public class DMZConfigEditScreen extends Screen {

	private static final int ROW_HEIGHT = 18;
	private static final int LIST_TOP = 32;
	private static final int LIST_BOTTOM_MARGIN = 40;
	private static final long FEEDBACK_DURATION_MS = 2500L;

	private final Screen parent;
	private final String configPath;
	private final List<Field> fields = new ArrayList<>();

	private JsonObject root;
	private int scrollOffset = 0;
	private Component feedback;
	private int feedbackColor;
	private long feedbackUntil;

	public DMZConfigEditScreen(Screen parent, String configPath) {
		super(Component.literal(configPath));
		this.parent = parent;
		this.configPath = configPath;
	}

	private int listBottom() {
		return this.height - LIST_BOTTOM_MARGIN;
	}

	private int visibleRows() {
		return Math.max(1, (listBottom() - LIST_TOP) / ROW_HEIGHT);
	}

	private int maxScroll() {
		return Math.max(0, fields.size() - visibleRows());
	}

	@Override
	protected void init() {
		super.init();
		fields.clear();

		if (root == null) {
			String json = ConfigManager.getSpecificConfigJson(configPath);
			try {
				root = json != null ? JsonParser.parseString(json).getAsJsonObject() : new JsonObject();
			} catch (Exception e) {
				root = new JsonObject();
			}
		}

		flatten(root, "");

		int boxX = this.width / 2 + 10;
		int boxW = Math.min(160, this.width / 2 - 20);
		for (Field field : fields) {
			EditBox box = new EditBox(this.font, boxX, 0, boxW, 14, Component.literal(field.label));
			box.setMaxLength(256);
			box.setValue(field.initialValue);
			box.visible = false;
			field.box = box;
			addWidget(box);
		}

		int bottomY = this.height - 28;
		boolean inGame = this.minecraft != null && this.minecraft.player != null;
		if (inGame) {
			addRenderableWidget(Button.builder(Component.translatable("gui.dragonminez.modconfig.save"), b -> save(false))
					.bounds(this.width / 2 - 154, bottomY, 100, 20).build());
			addRenderableWidget(Button.builder(Component.translatable("gui.dragonminez.modconfig.save_reload"), b -> save(true))
					.bounds(this.width / 2 - 50, bottomY, 104, 20).build());
			addRenderableWidget(Button.builder(Component.translatable("gui.dragonminez.modconfig.back"), b -> onClose())
					.bounds(this.width / 2 + 58, bottomY, 96, 20).build());
		} else {
			addRenderableWidget(Button.builder(Component.translatable("gui.dragonminez.modconfig.save"), b -> save(false))
					.bounds(this.width / 2 - 154, bottomY, 150, 20).build());
			addRenderableWidget(Button.builder(Component.translatable("gui.dragonminez.modconfig.back"), b -> onClose())
					.bounds(this.width / 2 + 4, bottomY, 150, 20).build());
		}
	}

	private void flatten(JsonElement element, String prefix) {
		if (element.isJsonObject()) {
			JsonObject obj = element.getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
				String key = entry.getKey();
				JsonElement child = entry.getValue();
				String label = prefix.isEmpty() ? key : prefix + "." + key;
				if (child.isJsonPrimitive()) {
					fields.add(new Field(label, child.getAsString(), child.getAsJsonPrimitive(),
							v -> obj.add(key, v)));
				} else if (child.isJsonObject() || child.isJsonArray()) {
					flatten(child, label);
				}
			}
		} else if (element.isJsonArray()) {
			JsonArray arr = element.getAsJsonArray();
			for (int i = 0; i < arr.size(); i++) {
				JsonElement child = arr.get(i);
				String label = prefix + "[" + i + "]";
				final int index = i;
				if (child.isJsonPrimitive()) {
					fields.add(new Field(label, child.getAsString(), child.getAsJsonPrimitive(),
							v -> arr.set(index, v)));
				} else if (child.isJsonObject() || child.isJsonArray()) {
					flatten(child, label);
				}
			}
		}
	}

	private void save(boolean reload) {
		for (Field field : fields) {
			field.apply();
		}
		boolean ok = ConfigManager.saveRawConfig(configPath, root.toString());
		if (ok) {
			if (reload && this.minecraft != null && this.minecraft.player != null) {
				this.minecraft.player.connection.sendCommand("dmzreload");
				onClose();
				return;
			}
			setFeedback(Component.translatable("gui.dragonminez.modconfig.saved"), 0xFF55FF55);
		} else {
			setFeedback(Component.translatable("gui.dragonminez.modconfig.save_failed"), 0xFFFF5555);
		}
	}

	private void setFeedback(Component text, int color) {
		this.feedback = text;
		this.feedbackColor = color;
		this.feedbackUntil = System.currentTimeMillis() + FEEDBACK_DURATION_MS;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFD700);

		int top = LIST_TOP;
		int bottom = listBottom();
		int start = scrollOffset;
		int end = Math.min(fields.size(), start + visibleRows());

		graphics.enableScissor(0, top, this.width, bottom);
		for (int i = 0; i < fields.size(); i++) {
			Field field = fields.get(i);
			if (i < start || i >= end) {
				field.box.visible = false;
				continue;
			}
			int y = top + (i - start) * ROW_HEIGHT;
			graphics.drawString(this.font, trim(field.label, this.width / 2 - 24), 14, y + 5, 0xFFFFFFFF);
			field.box.visible = true;
			field.box.setY(y + 1);
			field.box.render(graphics, mouseX, mouseY, partialTick);
		}
		graphics.disableScissor();

		if (fields.isEmpty()) {
			graphics.drawCenteredString(this.font, Component.translatable("gui.dragonminez.modconfig.no_fields"),
					this.width / 2, top + 10, 0xFF888888);
		}

		if (maxScroll() > 0) {
			int barX = this.width - 8;
			int trackH = bottom - top;
			graphics.fill(barX, top, barX + 3, bottom, 0xFF333333);
			float pct = (float) scrollOffset / maxScroll();
			int handleH = Math.max(20, (int) (trackH * ((float) visibleRows() / fields.size())));
			int handleY = top + (int) ((trackH - handleH) * pct);
			graphics.fill(barX, handleY, barX + 3, handleY + handleH, 0xFFAAAAAA);
		}

		if (feedback != null && System.currentTimeMillis() < feedbackUntil) {
			graphics.drawCenteredString(this.font, feedback, this.width / 2, this.height - 38, feedbackColor);
		}

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private String trim(String text, int maxWidth) {
		if (this.font.width(text) <= maxWidth) return text;
		while (text.length() > 1 && this.font.width(text + "...") > maxWidth) {
			text = text.substring(0, text.length() - 1);
		}
		return text + "...";
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		int max = maxScroll();
		if (max > 0 && mouseY >= LIST_TOP && mouseY < listBottom()) {
			scrollOffset = Math.max(0, Math.min(max, scrollOffset - (int) Math.signum(delta)));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) this.minecraft.setScreen(parent);
	}

	private static class Field {
		final String label;
		final String initialValue;
		final boolean wasBoolean;
		final boolean wasNumber;
		final Consumer<JsonElement> setter;
		EditBox box;

		Field(String label, String initialValue, JsonPrimitive original, Consumer<JsonElement> setter) {
			this.label = label;
			this.initialValue = initialValue;
			this.wasBoolean = original.isBoolean();
			this.wasNumber = original.isNumber();
			this.setter = setter;
		}

		void apply() {
			String value = box.getValue();
			setter.accept(coerce(value));
		}

		private JsonElement coerce(String value) {
			if (wasBoolean) {
				return new JsonPrimitive("true".equalsIgnoreCase(value.trim()));
			}
			if (wasNumber) {
				try {
					return JsonParser.parseString(value.trim());
				} catch (Exception ignored) {
					return new JsonPrimitive(initialValue);
				}
			}
			if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
				return new JsonPrimitive(Boolean.parseBoolean(value.toLowerCase(Locale.ROOT)));
			}
			try {
				Double.parseDouble(value.trim());
				return JsonParser.parseString(value.trim());
			} catch (Exception ignored) {
				return new JsonPrimitive(value);
			}
		}
	}
}
