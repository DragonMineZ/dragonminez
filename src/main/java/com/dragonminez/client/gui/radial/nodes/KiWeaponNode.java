package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.common.network.C2S.SelectKiWeaponC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class KiWeaponNode extends AbstractRadialNode {

	private final String type;

	public KiWeaponNode(String type) {
		this.type = type;
	}

	@Override
	public Component label(StatsData stats) {
		return Component.translatable("skill.dragonminez.kiweapon." + type);
	}

	@Override
	public ResourceLocation icon(StatsData stats) {
		return icon("radial_kiweapon");
	}

	@Override
	public boolean active(StatsData stats) {
		return stats.getSkills().isSkillActive("kimanipulation")
				&& type.equalsIgnoreCase(stats.getStatus().getKiWeaponType());
	}

	@Override
	public int labelColor(StatsData stats) {
		return active(stats) ? GREEN : RED;
	}

	@Override
	public void onSelect(StatsData stats) {
		boolean wasActive = active(stats);
		NetworkHandler.sendToServer(new SelectKiWeaponC2S(type));
		playToggle(!wasActive);
	}
}
