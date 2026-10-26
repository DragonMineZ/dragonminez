package com.dragonminez.client.gui.character.tab;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

@OnlyIn(Dist.CLIENT)
public class RegisterCharacterTabsEvent extends Event implements IModBusEvent {
	public CharacterTab register(CharacterTabDefinition definition) {
		return CharacterTabs.register(definition);
	}
}
