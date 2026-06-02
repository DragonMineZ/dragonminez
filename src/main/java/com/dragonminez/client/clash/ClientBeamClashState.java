package com.dragonminez.client.clash;

import lombok.Getter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-side mirror of the local player's beam-clash state, fed by {@code BeamClashStateS2C}
 * and read by the QTE HUD overlay and the input handler.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientBeamClashState {

	@Getter
	private static volatile boolean active = false;
	private static volatile float meterPhase = 0.0f;
	private static volatile float sweetLow = 0.0f;
	private static volatile float sweetHigh = 0.0f;
	private static volatile float advantage = 0.5f;
	private static volatile int beamColor = 0xFFFFFF;
	private static volatile int opponentId = -1;

	private ClientBeamClashState() {
	}

	public static void update(boolean active, float meterPhase, float sweetLow, float sweetHigh, float advantage, int beamColor, int opponentId) {
		ClientBeamClashState.active = active;
		ClientBeamClashState.meterPhase = meterPhase;
		ClientBeamClashState.sweetLow = sweetLow;
		ClientBeamClashState.sweetHigh = sweetHigh;
		ClientBeamClashState.advantage = advantage;
		ClientBeamClashState.beamColor = beamColor;
		ClientBeamClashState.opponentId = opponentId;
	}

	public static void clear() {
		active = false;
	}

	public static float meterPhase() {
		return meterPhase;
	}

	public static float sweetLow() {
		return sweetLow;
	}

	public static float sweetHigh() {
		return sweetHigh;
	}

	public static float advantage() {
		return advantage;
	}

	public static int beamColor() {
		return beamColor;
	}

	public static int opponentId() {
		return opponentId;
	}
}
