package com.dragonminez.client.gui.character.tab;

import com.dragonminez.client.gui.character.CharacterCustomizationScreen;
import com.dragonminez.common.stats.character.Character;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Style;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface TabContext {
	Character character();

	CharacterCustomizationScreen screen();

	boolean isRecustomization();

	<T extends GuiEventListener & Renderable & NarratableEntry> T addWidget(T widget);

	Font font();

	Style textStyle();

	int panelX();

	int panelY();

	int panelWidth();

	int panelHeight();

	int contentTop();

	int uiWidth();

	int uiHeight();

	void markChanged();

	void refreshWidgets();
}
