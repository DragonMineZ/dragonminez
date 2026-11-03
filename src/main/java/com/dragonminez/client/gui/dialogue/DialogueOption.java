package com.dragonminez.client.gui.dialogue;

import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.IntSupplier;

@OnlyIn(Dist.CLIENT)
public final class DialogueOption {
	public enum Tone { NORMAL, PRIMARY, DANGER, DONE }

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

	public static DialogueOption of(String id, Component label, Runnable action) {
		return new DialogueOption(id, label, action);
	}

	public String getId() {
		return id;
	}

	DialogueOption icon(DialogueSkin.Icon icon, int color) {
		this.icon = icon;
		this.iconColor = color;
		return this;
	}

	public DialogueOption tone(Tone tone) {
		this.tone = tone;
		return this;
	}

	public DialogueOption submenu() {
		this.submenu = true;
		return this;
	}

	public DialogueOption enabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	public DialogueOption locked(Component title, List<Component> lines) {
		this.enabled = false;
		this.lockTitle = title;
		this.lockLines = lines;
		return this;
	}

	public DialogueOption onLocked(Runnable action) {
		this.lockedAction = action;
		return this;
	}

	public DialogueOption cooldown(IntSupplier syncedTicks) {
		this.cooldownTicks = syncedTicks;
		return this;
	}

	public DialogueOption cooldownUntil(long endMs) {
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
