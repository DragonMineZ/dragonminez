package com.dragonminez.client.gui.utilitymenu.menuslots;

import com.dragonminez.client.gui.utilitymenu.AbstractMenuSlot;
import com.dragonminez.client.gui.utilitymenu.ButtonInfo;
import com.dragonminez.client.gui.utilitymenu.IUtilityMenuSlot;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.C2S.ExecuteActionC2S;
import com.dragonminez.common.network.C2S.SwitchActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class SuperformMenuSlot extends AbstractMenuSlot implements IUtilityMenuSlot {
	private boolean hasAvailableSuperform(StatsData statsData) {
		String firstGroup = TransformationsHelper.getGroupWithFirstAvailableForm(statsData);
		String firstForm = TransformationsHelper.getFirstAvailableForm(statsData);
		return firstGroup != null && !firstGroup.isEmpty() && firstForm != null && !firstForm.isEmpty();
	}

	private String getDisplayGroup(StatsData statsData) {
		String group = statsData.getCharacter().getSelectedFormGroup();
		if (group == null || group.isEmpty()) {
			group = TransformationsHelper.getGroupWithFirstAvailableForm(statsData);
		}
		return group;
	}

	private String getDisplayForm(StatsData statsData) {
		String form = statsData.getCharacter().getSelectedForm();
		if (form == null || form.isEmpty()) {
			form = TransformationsHelper.getFirstAvailableForm(statsData);
		}
		return form;
	}

	@Override
	public ButtonInfo render(StatsData statsData) {
		ActionMode currentMode = statsData.getStatus().getSelectedAction();
		String race = statsData.getCharacter().getRaceName();

		if (!hasAvailableSuperform(statsData)) {
			return new ButtonInfo();
		}

		String group = getDisplayGroup(statsData);
		String form = getDisplayForm(statsData);
		if (group == null || group.isEmpty() || form == null || form.isEmpty()) {
			return new ButtonInfo();
		}

		return new ButtonInfo(
				Component.translatable("race.dragonminez." + race + ".group." + group).withStyle(ChatFormatting.BOLD),
				Component.translatable("race.dragonminez." + race + ".form." + group + "." + form),
				currentMode == ActionMode.FORM);
	}

	@Override
	public void handle(StatsData statsData, boolean rightClick) {
		if (!hasAvailableSuperform(statsData)) {
			return;
		}

		String group = getDisplayGroup(statsData);
		String form = getDisplayForm(statsData);
		if (group == null || group.isEmpty() || form == null || form.isEmpty()) {
			return;
		}

		boolean wasActive = statsData.getStatus().getSelectedAction() == ActionMode.FORM;
		if (wasActive && statsData.getCharacter().hasActiveForm()) {
			if (TransformationsHelper.canDescend(statsData)) {
				NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.DESCEND));
				playToggleSound(false);
			}
		} else if (!wasActive) {
			NetworkHandler.sendToServer(new SwitchActionC2S(ActionMode.FORM));
			playToggleSound(true);
		} else {
			NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.CYCLE_FORM_GROUP, rightClick));
			playToggleSound(true);
		}
	}
}
