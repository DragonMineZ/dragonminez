package com.dragonminez.client.util;

import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.util.lists.SaiyanForms;

import java.util.Locale;

public final class SaiyanTailRules {
	private static final String SSJ4_KEY = "ssj4";

	private SaiyanTailRules() {}

	public static boolean ssj4ForcesTail(Character character) {
		String form = character.getActiveForm();
		if (form != null) {
			String lowered = form.toLowerCase(Locale.ROOT);
			if (lowered.contains(SaiyanForms.SUPER_SAIYAN_4) || lowered.contains(SSJ4_KEY)) return true;
		}
		return isSsj4Model(character.hasActiveStackForm() ? character.getActiveStackFormData() : null)
				|| isSsj4Model(character.hasActiveForm() ? character.getActiveFormData() : null);
	}

	public static boolean hasTail(Character character) {
		return character.isHasSaiyanTail() || ssj4ForcesTail(character);
	}

	private static boolean isSsj4Model(FormConfig.FormData form) {
		return form != null && Boolean.TRUE.equals(form.hasCustomModel()) && form.getCustomModel() != null
				&& form.getCustomModel().toLowerCase(Locale.ROOT).contains(SSJ4_KEY);
	}
}
