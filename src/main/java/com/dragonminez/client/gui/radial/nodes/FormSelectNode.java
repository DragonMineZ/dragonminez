package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.client.gui.radial.FormPreview;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.network.C2S.ExecuteActionC2S;
import com.dragonminez.common.network.C2S.SelectFormC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.extras.ActionMode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class FormSelectNode extends AbstractRadialNode {

	private final String race;
	private final String group;
	private final String form;
	private final boolean stack;
	private final ResourceLocation icon;
	private final int tint;

	public FormSelectNode(String race, String group, String form, boolean stack) {
		this.race = race;
		this.group = group;
		this.form = form;
		this.stack = stack;

		FormConfig config = stack ? ConfigManager.getStackFormGroup(group) : ConfigManager.getFormGroup(race, group);
		String formType = config != null ? config.getFormType() : "";
		this.icon = iconForFormType(formType);

		FormConfig.FormData formData = stack ? ConfigManager.getStackForm(group, form) : ConfigManager.getForm(race, group, form);
		this.tint = tintOf(formData);
	}

	@Override
	public String orderKey() {
		return (stack ? "stack:" : "form:") + group + ":" + form;
	}

	@Override
	public Component label(StatsData stats) {
		return stack
				? Component.translatable("race.dragonminez.stack.form." + group + "." + form)
				: Component.translatable("race.dragonminez." + race + ".form." + group + "." + form);
	}

	@Override
	public ResourceLocation icon(StatsData stats) {
		return icon;
	}

	@Override
	public int iconTint(StatsData stats) {
		return tint;
	}

	@Override
	public boolean active(StatsData stats) {
		var character = stats.getCharacter();
		ActionMode mode = stats.getStatus().getSelectedAction();
		if (stack) {
			return mode == ActionMode.STACK
					&& group.equalsIgnoreCase(character.getSelectedStackFormGroup())
					&& form.equalsIgnoreCase(character.getSelectedStackForm());
		}
		return mode == ActionMode.FORM
				&& group.equalsIgnoreCase(character.getSelectedFormGroup())
				&& form.equalsIgnoreCase(character.getSelectedForm());
	}

	@Override
	public int labelColor(StatsData stats) {
		return active(stats) ? GREEN : RED;
	}

	@Override
	public FormPreview preview(StatsData stats) {
		return new FormPreview(group, form, stack);
	}

	@Override
	public void onSelect(StatsData stats) {
		NetworkHandler.sendToServer(new SelectFormC2S(group, form, stack));
		playToggle(true);
	}

	@Override
	public void onDoubleSelect(StatsData stats) {
		NetworkHandler.sendToServer(new SelectFormC2S(group, form, stack));
		NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.INSTANT_TRANSFORM));
		playToggle(true);
	}
}
