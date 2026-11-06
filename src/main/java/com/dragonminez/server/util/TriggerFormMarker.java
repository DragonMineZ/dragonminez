package com.dragonminez.server.util;

import com.dragonminez.common.stats.StatsData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class TriggerFormMarker {
	private static final String TAG_ROOT = "dmzTriggerForm";
	private static final String TAG_FORM_GROUP = "formGroup";
	private static final String TAG_FORM = "form";
	private static final String TAG_STACK_GROUP = "stackGroup";
	private static final String TAG_STACK = "stack";

	private TriggerFormMarker() {}

	public static void markForm(Player player, String group, String form) {
		CompoundTag root = root(player);
		root.putString(TAG_FORM_GROUP, group);
		root.putString(TAG_FORM, form);
		player.getPersistentData().put(TAG_ROOT, root);
	}

	public static void markStack(Player player, String group, String form) {
		CompoundTag root = root(player);
		root.putString(TAG_STACK_GROUP, group);
		root.putString(TAG_STACK, form);
		player.getPersistentData().put(TAG_ROOT, root);
	}

	public static boolean isTriggeredForm(Player player, StatsData data) {
		var character = data.getCharacter();
		return character.hasActiveForm() && matches(player, TAG_FORM_GROUP, TAG_FORM, character.getActiveFormGroup(), character.getActiveForm());
	}

	public static boolean isTriggeredStack(Player player, StatsData data) {
		var character = data.getCharacter();
		return character.hasActiveStackForm() && matches(player, TAG_STACK_GROUP, TAG_STACK, character.getActiveStackFormGroup(), character.getActiveStackForm());
	}

	public static void reconcile(Player player, StatsData data) {
		if (!player.getPersistentData().contains(TAG_ROOT)) return;
		CompoundTag root = player.getPersistentData().getCompound(TAG_ROOT);
		if (root.contains(TAG_FORM) && !isTriggeredForm(player, data)) {
			root.remove(TAG_FORM_GROUP);
			root.remove(TAG_FORM);
		}
		if (root.contains(TAG_STACK) && !isTriggeredStack(player, data)) {
			root.remove(TAG_STACK_GROUP);
			root.remove(TAG_STACK);
		}
		if (root.isEmpty()) player.getPersistentData().remove(TAG_ROOT);
	}

	private static boolean matches(Player player, String groupKey, String formKey, String group, String form) {
		if (!player.getPersistentData().contains(TAG_ROOT)) return false;
		CompoundTag root = player.getPersistentData().getCompound(TAG_ROOT);
		return root.contains(formKey)
				&& root.getString(groupKey).equalsIgnoreCase(group)
				&& root.getString(formKey).equalsIgnoreCase(form);
	}

	private static CompoundTag root(Player player) {
		return player.getPersistentData().getCompound(TAG_ROOT);
	}
}
