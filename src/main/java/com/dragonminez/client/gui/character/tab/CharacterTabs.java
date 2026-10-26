package com.dragonminez.client.gui.character.tab;

import com.dragonminez.common.stats.character.Character;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModLoader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public final class CharacterTabs {
	private static volatile List<CharacterTab> addonTabs = List.of();
	private static boolean registrationPosted;

	private CharacterTabs() {
	}

	public static synchronized CharacterTab register(CharacterTabDefinition definition) {
		Objects.requireNonNull(definition, "definition");
		ResourceLocation id = Objects.requireNonNull(definition.id(), "definition id");
		if (byId(id) != null) throw new IllegalArgumentException("Character tab '" + id + "' is already registered");
		CharacterTab tab = new CharacterTab(definition);
		List<CharacterTab> updated = new ArrayList<>(addonTabs);
		updated.add(tab);
		addonTabs = List.copyOf(updated);
		return tab;
	}

	public static List<CharacterTab> getAddonTabs() {
		return addonTabs;
	}

	public static CharacterTab byId(ResourceLocation id) {
		if (id == null) return null;
		for (CharacterTab tab : CharacterTab.BUILT_IN) if (tab.getId().equals(id)) return tab;
		for (CharacterTab tab : addonTabs) if (tab.getId().equals(id)) return tab;
		return null;
	}

	public static void postRegistrationEvent() {
		synchronized (CharacterTabs.class) {
			if (registrationPosted) return;
			registrationPosted = true;
		}
		ModLoader.get().postEventWrapContainerInModOrder(new RegisterCharacterTabsEvent());
	}

	public static void insertAvailableTabs(List<CharacterTab> tabs, Character character) {
		List<CharacterTab> pending = new ArrayList<>();
		for (CharacterTab tab : addonTabs) {
			if (tab.getDefinition().isAvailable(character)) pending.add(tab);
		}

		boolean placedAny = true;
		while (placedAny && !pending.isEmpty()) {
			placedAny = false;
			Iterator<CharacterTab> iterator = pending.iterator();
			while (iterator.hasNext()) {
				CharacterTab tab = iterator.next();
				int anchorIndex = indexOf(tabs, resolveAnchor(tab.getDefinition().insertAfter(), tabs));
				if (anchorIndex < 0) continue;
				int insertAt = anchorIndex + 1;
				while (insertAt < tabs.size() && !tabs.get(insertAt).isBuiltIn()) insertAt++;
				tabs.add(insertAt, tab);
				iterator.remove();
				placedAny = true;
			}
		}
		tabs.addAll(pending);
	}

	private static ResourceLocation resolveAnchor(ResourceLocation anchor, List<CharacterTab> tabs) {
		if (anchor == null) return null;
		int builtInIndex = indexOf(CharacterTab.BUILT_IN, anchor);
		if (builtInIndex < 0) return anchor;
		for (int i = builtInIndex; i >= 0; i--) {
			CharacterTab candidate = CharacterTab.BUILT_IN.get(i);
			if (tabs.contains(candidate)) return candidate.getId();
		}
		return null;
	}

	private static int indexOf(List<CharacterTab> tabs, ResourceLocation id) {
		if (id == null) return -1;
		for (int i = 0; i < tabs.size(); i++) {
			if (tabs.get(i).getId().equals(id)) return i;
		}
		return -1;
	}
}
