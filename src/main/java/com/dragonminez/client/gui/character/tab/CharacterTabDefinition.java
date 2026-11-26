package com.dragonminez.client.gui.character.tab;

import com.dragonminez.common.stats.character.Character;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface CharacterTabDefinition {
	ResourceLocation id();

	Component title();

	default ResourceLocation insertAfter() {
		return null;
	}

	default boolean isAvailable(Character character) {
		return true;
	}

	default CharacterTab.ModelView modelView() {
		return CharacterTab.ModelView.FULL_BODY;
	}

	default int modelOffsetX() {
		return 0;
	}

	default void onOpen(TabContext context) {
	}

	default void initWidgets(TabContext context) {
	}

	default void render(GuiGraphics graphics, TabContext context, int mouseX, int mouseY) {
	}

	default void onConfirm(Character character) {
	}

	default void onCancel() {
	}
}
