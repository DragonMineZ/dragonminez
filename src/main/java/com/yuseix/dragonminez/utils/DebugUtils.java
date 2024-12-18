package com.yuseix.dragonminez.utils;

import net.minecraft.ChatFormatting;

public final class DebugUtils {

	private static final String PREFIX = ChatFormatting.YELLOW + "[DragonMine Z] " + ChatFormatting.RESET;

	public static void dmzLog(String message) {
		System.out.println("[DMZ-DEBUG] " + message);
	}

}
