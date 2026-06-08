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

public class StackFormMenuSlot extends AbstractMenuSlot implements IUtilityMenuSlot {
	private boolean hasAvailableStackForm(StatsData statsData) {
		String firstGroup = TransformationsHelper.getGroupWithFirstAvailableStackForm(statsData);
		String firstForm = TransformationsHelper.getFirstAvailableStackForm(statsData);
		return firstGroup != null && !firstGroup.isEmpty() && firstForm != null && !firstForm.isEmpty();
	}

	private String getDisplayGroup(StatsData statsData) {
		String group = statsData.getCharacter().getSelectedStackFormGroup();
		if (group == null || group.isEmpty()) {
			group = TransformationsHelper.getGroupWithFirstAvailableStackForm(statsData);
		}
		return group;
	}

	private String getDisplayForm(StatsData statsData) {
		String form = statsData.getCharacter().getSelectedStackForm();
		if (form == null || form.isEmpty()) {
			form = TransformationsHelper.getFirstAvailableStackForm(statsData);
		}
		return form;
	}

	@Override
	public ButtonInfo render(StatsData statsData) {
		ActionMode currentMode = statsData.getStatus().getSelectedAction();

		if (!hasAvailableStackForm(statsData)) {
			return new ButtonInfo();
		}

		String group = getDisplayGroup(statsData);
		String form = getDisplayForm(statsData);
		if (group == null || group.isEmpty() || form == null || form.isEmpty()) {
			return new ButtonInfo();
		}

		return new ButtonInfo(
				Component.translatable("race.dragonminez.stack.group." + group).withStyle(ChatFormatting.BOLD),
				Component.translatable("race.dragonminez.stack.form." + group + "." + form),
				currentMode == ActionMode.STACK);
	}

	@Override
	public void handle(StatsData statsData, boolean rightClick) {
		if (!hasAvailableStackForm(statsData)) {
			return;
		}

		String group = getDisplayGroup(statsData);
		String form = getDisplayForm(statsData);
		if (group == null || group.isEmpty() || form == null || form.isEmpty()) {
			return;
		}

		boolean wasActive = statsData.getStatus().getSelectedAction() == ActionMode.STACK;
		if (!wasActive) {
			NetworkHandler.sendToServer(new SwitchActionC2S(ActionMode.STACK));
			playToggleSound(true);
		} else {
			NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.CYCLE_STACK_FORM_GROUP, rightClick));
			playToggleSound(true);
		}
	}
}
