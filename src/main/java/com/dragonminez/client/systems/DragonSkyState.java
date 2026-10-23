package com.dragonminez.client.systems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class DragonSkyState {
	private static final float FADE_IN_TICKS = 60.0F;
	private static final float FADE_OUT_TICKS = 70.0F;

	private static ClientLevel syncedLevel;
	private static boolean active;
	private static float previous;
	private static float current;

	private DragonSkyState() {}

	public static void update(ResourceLocation dimension, boolean isActive) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null || !level.dimension().location().equals(dimension)) return;
		active = isActive;
		if (level != syncedLevel) {
			syncedLevel = level;
			previous = current = isActive ? 1.0F : 0.0F;
		}
	}

	public static void tick() {
		if (Minecraft.getInstance().level != syncedLevel) {
			syncedLevel = null;
			active = false;
			previous = current = 0.0F;
			return;
		}
		previous = current;
		current = active ? Math.min(1.0F, current + 1.0F / FADE_IN_TICKS) : Math.max(0.0F, current - 1.0F / FADE_OUT_TICKS);
	}

	public static float darkness(float partialTick) {
		if (syncedLevel == null || Minecraft.getInstance().level != syncedLevel) return 0.0F;
		float t = Mth.clamp(Mth.lerp(partialTick, previous, current), 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}
}
