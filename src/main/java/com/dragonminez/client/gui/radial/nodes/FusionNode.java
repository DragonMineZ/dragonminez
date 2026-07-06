package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.common.network.C2S.SwitchActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.extras.ActionMode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class FusionNode extends AbstractRadialNode {

	@Override
	public Component label(StatsData stats) {
		return Component.translatable("gui.action.dragonminez.fusion");
	}

	@Override
	public ResourceLocation icon(StatsData stats) {
		return icon("fusion");
	}

	@Override
	public boolean visible(StatsData stats) {
		return stats.getSkills().hasSkill("fusion");
	}

	@Override
	public boolean active(StatsData stats) {
		return stats.getStatus().getSelectedAction() == ActionMode.FUSION;
	}

	@Override
	public int labelColor(StatsData stats) {
		return active(stats) ? GREEN : RED;
	}

	@Override
	public void onSelect(StatsData stats) {
		boolean wasActive = stats.getStatus().getSelectedAction() == ActionMode.FUSION;
		NetworkHandler.sendToServer(new SwitchActionC2S(ActionMode.FUSION));
		playToggle(!wasActive);
	}
}
