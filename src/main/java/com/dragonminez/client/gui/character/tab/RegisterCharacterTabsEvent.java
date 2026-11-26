package com.dragonminez.client.gui.character.tab;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

@OnlyIn(Dist.CLIENT)
public class RegisterCharacterTabsEvent extends Event implements IModBusEvent {
	public CharacterTab register(CharacterTabDefinition definition) {
		return CharacterTabs.register(definition);
	}
}
