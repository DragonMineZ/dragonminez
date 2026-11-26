package com.dragonminez.client.util;

import net.minecraft.network.chat.Component;

public final class BonusNameFormatter {

	private BonusNameFormatter() {
	}

	public static String display(String bonusName) {
		if (bonusName == null || bonusName.isEmpty()) return "";
		if (bonusName.startsWith("Zenkai_Temp_")) return Component.translatable("gui.dragonminez.bonus.zenkai_temp").getString();
		if (bonusName.startsWith("CellJr_")) return Component.translatable("gui.dragonminez.bonus.cell_jr").getString();
		return bonusName.replace("_", " ");
	}
}
