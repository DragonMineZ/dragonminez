package com.dragonminez.client.gui.character.tab;

import com.dragonminez.Reference;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class CharacterTab {
	public enum ModelView { FULL_BODY, HEAD }

	public static final CharacterTab PRESET = new CharacterTab("preset", "gui.dragonminez.customization.body_type", ModelView.FULL_BODY, 0);
	public static final CharacterTab HAIR = new CharacterTab("hair", "gui.dragonminez.customization.hair", ModelView.HEAD, 0);
	public static final CharacterTab EYES = new CharacterTab("eyes", "gui.dragonminez.customization.eyes", ModelView.HEAD, 0);
	public static final CharacterTab FACE = new CharacterTab("face", "gui.dragonminez.customization.nose", ModelView.HEAD, 0);
	public static final CharacterTab BODY = new CharacterTab("body", "gui.dragonminez.customization.tattoo", ModelView.FULL_BODY, 0);
	public static final CharacterTab AURA_CLASS = new CharacterTab("aura_class", "gui.dragonminez.customization.class", ModelView.FULL_BODY, -55);

	public static final List<CharacterTab> BUILT_IN = List.of(PRESET, HAIR, EYES, FACE, BODY, AURA_CLASS);

	private final ResourceLocation id;
	private final Component title;
	private final ModelView modelView;
	private final int modelOffsetX;
	private final CharacterTabDefinition definition;

	private CharacterTab(String path, String titleKey, ModelView modelView, int modelOffsetX) {
		this.id = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, path);
		this.title = Component.translatable(titleKey);
		this.modelView = modelView;
		this.modelOffsetX = modelOffsetX;
		this.definition = null;
	}

	CharacterTab(CharacterTabDefinition definition) {
		this.id = definition.id();
		this.title = null;
		this.modelView = null;
		this.modelOffsetX = 0;
		this.definition = definition;
	}

	public ResourceLocation getId() {
		return id;
	}

	public Component getTitle() {
		if (definition == null) return title;
		Component addonTitle = definition.title();
		return addonTitle != null ? addonTitle : Component.empty();
	}

	public ModelView getModelView() {
		if (definition == null) return modelView;
		ModelView addonView = definition.modelView();
		return addonView != null ? addonView : ModelView.FULL_BODY;
	}

	public int getModelOffsetX() {
		return definition == null ? modelOffsetX : definition.modelOffsetX();
	}

	public boolean isBuiltIn() {
		return definition == null;
	}

	public CharacterTabDefinition getDefinition() {
		return definition;
	}

	@Override
	public String toString() {
		return id.toString();
	}
}
