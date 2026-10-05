package com.dragonminez.client.gui.dialogue;

import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.IntSupplier;

@OnlyIn(Dist.CLIENT)
final class DialogueOption {
	enum Tone { NORMAL, PRIMARY, DANGER, DONE }

	final String id;
	final Component label;
	final Runnable action;
	DialogueSkin.Icon icon = DialogueSkin.Icon.NONE;
	int iconColor = DialogueSkin.TEXT;
	Tone tone = Tone.NORMAL;
	boolean enabled = true;
	boolean submenu;
	Component lockTitle;
	List<Component> lockLines = List.of();
	Runnable lockedAction;
	IntSupplier cooldownTicks;
	long cooldownEndMs;

	private DialogueOption(String id, Component label, Runnable action) {
		this.id = id;
		this.label = label;
		this.action = action;
	}

	static DialogueOption of(String id, Component label, Runnable action) {
		return new DialogueOption(id, label, action);
	}

	DialogueOption icon(DialogueSkin.Icon icon, int color) {
		this.icon = icon;
		this.iconColor = color;
		return this;
	}

	DialogueOption tone(Tone tone) {
		this.tone = tone;
		return this;
	}

	DialogueOption submenu() {
		this.submenu = true;
		return this;
	}

	DialogueOption enabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	DialogueOption locked(Component title, List<Component> lines) {
		this.enabled = false;
		this.lockTitle = title;
		this.lockLines = lines;
		return this;
	}

	DialogueOption onLocked(Runnable action) {
		this.lockedAction = action;
		return this;
	}

	DialogueOption cooldown(IntSupplier syncedTicks) {
		this.cooldownTicks = syncedTicks;
		return this;
	}

	DialogueOption cooldownUntil(long endMs) {
		this.cooldownEndMs = endMs;
		return this;
	}

	boolean onCooldown(long now) {
		if (cooldownEndMs > 0L && now < cooldownEndMs) return true;
		return cooldownTicks != null && cooldownTicks.getAsInt() > 0;
	}

	boolean available(long now) {
		return enabled && !onCooldown(now);
	}
}
