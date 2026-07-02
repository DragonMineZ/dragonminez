package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.network.C2S.SelectKiWeaponC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class KiWeaponNode extends AbstractRadialNode {
	private final String type;
	private final int tint;

	public KiWeaponNode(String type) {
		this.type = type;
		StatsData stats = null;
		if (Minecraft.getInstance().player != null)
			stats = StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player).orElse(null);

		if (stats != null) {
			if (stats.getCharacter().hasActiveStackForm() && stats.getCharacter().getActiveStackFormData() != null) {
				tint = tintOf(stats.getCharacter().getActiveStackFormData().getRgbAuraColor());
			} else if (stats.getCharacter().hasActiveForm() && stats.getCharacter().getActiveFormData() != null) {
				tint = tintOf(stats.getCharacter().getActiveFormData().getRgbAuraColor());
			} else tint = tintOf(stats.getCharacter().getRgbAuraColor());
		} else tint = -1;
	}

	@Override
	public Component label(StatsData stats) {
		return Component.translatable("skill.dragonminez.kiweapon." + type);
	}

	@Override
	public ResourceLocation icon(StatsData stats) {
		return icon("kiweapon");
	}

	@Override
	public int iconTint(StatsData stats) {
		return tint;
	}

	protected static int tintOf(float[] rgb) {
		if (rgb == null || rgb.length < 3) return -1;
		int r = Math.round(rgb[0] * 255.0f);
		int g = Math.round(rgb[1] * 255.0f);
		int b = Math.round(rgb[2] * 255.0f);
		return (r << 16) | (g << 8) | b;
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
