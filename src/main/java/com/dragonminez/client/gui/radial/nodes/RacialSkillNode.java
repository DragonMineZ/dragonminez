package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.C2S.ExecuteActionC2S;
import com.dragonminez.common.network.C2S.SwitchActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.extras.ActionMode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class RacialSkillNode extends AbstractRadialNode {

	private boolean isTailRace(StatsData stats) {
		String race = stats.getCharacter().getRaceName();
		String form = stats.getCharacter().getActiveForm();
		return "saiyan".equals(race) || stats.getCharacter().isHasSaiyanTail() || (form != null && form.contains("oozaru"));
	}

	private String racialSkill(StatsData stats) {
		String race = stats.getCharacter().getRaceName();
		return ConfigManager.getRaceCharacter(race) == null ? "" : ConfigManager.getRaceCharacter(race).getRacialSkill();
	}

	private boolean isActionRacial(StatsData stats) {
		String skill = racialSkill(stats);
		return "namekian".equals(skill) || "bioandroid".equals(skill) || "majin".equals(skill);
	}

	@Override
	public Component label(StatsData stats) {
		if (isActionRacial(stats)) return Component.translatable("gui.action.dragonminez.racial." + racialSkill(stats));
		if (isTailRace(stats)) return Component.translatable("gui.action.dragonminez.tail");
		return Component.empty();
	}

	@Override
	public ResourceLocation icon(StatsData stats) {
		return icon("racial");
	}

	@Override
	public boolean visible(StatsData stats) {
		if (stats.getCharacter() == null) return false;
		String race = stats.getCharacter().getRaceName();
		if (race == null || race.isEmpty()) return false;
		return isTailRace(stats) || isActionRacial(stats);
	}

	@Override
	public boolean active(StatsData stats) {
		if (isActionRacial(stats)) return stats.getStatus().getSelectedAction() == ActionMode.RACIAL;
		if (isTailRace(stats)) return stats.getStatus().isTailVisible();
		return false;
	}

	@Override
	public int labelColor(StatsData stats) {
		return active(stats) ? GREEN : RED;
	}

	@Override
	public void onSelect(StatsData stats) {
		if (isActionRacial(stats)) {
			boolean wasActive = stats.getStatus().getSelectedAction() == ActionMode.RACIAL;
			NetworkHandler.sendToServer(new SwitchActionC2S(ActionMode.RACIAL));
			playToggle(!wasActive);
		} else if (isTailRace(stats)) {
			boolean wasActive = stats.getStatus().isTailVisible();
			NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.TOGGLE_TAIL));
			playToggle(!wasActive);
		}
	}
}
