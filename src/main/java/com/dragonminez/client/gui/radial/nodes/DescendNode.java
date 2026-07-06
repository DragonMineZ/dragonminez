package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.common.network.C2S.ExecuteActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class DescendNode extends AbstractRadialNode {

	@Override
	public Component label(StatsData stats) {
		return Component.translatable("gui.action.dragonminez.descend");
	}

	@Override
	public ResourceLocation icon(StatsData stats) {
		return icon("descend");
	}

	@Override
	public boolean visible(StatsData stats) {
		boolean activeForm = stats.getCharacter().getActiveForm() != null && !stats.getCharacter().getActiveForm().isEmpty();
		boolean activeStackForm = stats.getCharacter().getActiveStackForm() != null && !stats.getCharacter().getActiveStackForm().isEmpty();
		boolean isAndroidBaseForm = stats.getStatus().isAndroidUpgraded() && "androidbase".equalsIgnoreCase(stats.getCharacter().getActiveForm());
		if (activeStackForm || (activeForm && !isAndroidBaseForm)) return true;
		return stats.getResources().getPowerRelease() > 0;
	}

	@Override
	public int labelColor(StatsData stats) {
		return RED;
	}

	@Override
	public void onSelect(StatsData stats) {
		NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.FORCE_DESCEND, false));
		playToggle(false);
	}
}
