package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.common.network.C2S.RacialSlotActionC2S;
import com.dragonminez.common.network.C2S.SwitchActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.racial.RacialData;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.extras.ActionMode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class BioSkillChoiceNode extends AbstractRadialNode {

	private final boolean explode;

	public BioSkillChoiceNode(boolean explode) {
		this.explode = explode;
	}

	@Override
	public Component label(StatsData stats) {
		return Component.translatable("gui.action.dragonminez.racial.bioandroid." + (explode ? "explode" : "semi"));
	}

	@Override
	public ResourceLocation icon(StatsData stats) {
		return icon("racial");
	}

	@Override
	public boolean active(StatsData stats) {
		return explode == RacialData.BIO_SKILL_EXPLODE.equals(stats.getRacialData().getBioSelectedSkill());
	}

	@Override
	public int labelColor(StatsData stats) {
		return active(stats) ? GREEN : 0xFFFFFF;
	}

	@Override
	public void onSelect(StatsData stats) {

		if (stats.getStatus().getSelectedAction() != ActionMode.RACIAL) {
			NetworkHandler.sendToServer(new SwitchActionC2S(ActionMode.RACIAL));
		}
		if (active(stats)) {
			playToggle(true);
			return;
		}

		stats.getRacialData().setBioSelectedSkill(explode ? RacialData.BIO_SKILL_EXPLODE : RacialData.BIO_SKILL_DRAIN);
		NetworkHandler.sendToServer(new RacialSlotActionC2S(RacialSlotActionC2S.SlotAction.SELECT_BIO_SKILL, explode ? 1 : 0));
		playToggle(true);
	}
}
