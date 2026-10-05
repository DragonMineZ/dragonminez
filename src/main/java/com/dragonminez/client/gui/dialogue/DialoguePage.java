package com.dragonminez.client.gui.dialogue;

import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
final class DialoguePage {
	final String id;
	final Supplier<Component> line;
	final Supplier<List<DialogueOption>> options;
	final DialogueQuest quest;
	boolean root;
	int inputDigits;
	Component inputHint;

	private DialoguePage(String id, Supplier<Component> line, Supplier<List<DialogueOption>> options, DialogueQuest quest) {
		this.id = id;
		this.line = line;
		this.options = options;
		this.quest = quest;
	}

	static DialoguePage speech(String id, Supplier<Component> line, Supplier<List<DialogueOption>> options) {
		return new DialoguePage(id, line, options, null);
	}

	static DialoguePage quest(DialogueQuest quest, Supplier<List<DialogueOption>> options) {
		return new DialoguePage(DialogueQuest.optionId(quest.id), Component::empty, options, quest);
	}

	DialoguePage asRoot() {
		this.root = true;
		return this;
	}

	DialoguePage withNumberInput(int digits, Component hint) {
		this.inputDigits = digits;
		this.inputHint = hint;
		return this;
	}

	boolean isQuest() {
		return quest != null;
	}

	boolean hasInput() {
		return inputDigits > 0;
	}
}
