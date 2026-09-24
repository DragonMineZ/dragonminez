package com.dragonminez.client.clash;

import com.dragonminez.common.combat.clash.ClashMeter;
import com.dragonminez.common.network.C2S.BeamClashInputC2S;
import com.dragonminez.common.network.NetworkHandler;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientBeamClashState {
	public static final long FEEDBACK_NANOS = 750_000_000L;

	private static final float RENDER_TIME_STALE_TICKS = 2.0f;

	@Getter
	private static volatile boolean active = false;
	private static volatile long startGameTime = 0L;
	private static volatile long meterSeed = 0L;
	private static volatile float advantage = 0.5f;
	private static volatile int selfColor = 0xFFFFFF;
	private static volatile int foeColor = 0xFFFFFF;
	private static volatile int opponentId = -1;
	private static volatile Vec3 clashPoint = null;

	private static volatile long exhaustedUntil = 0L;
	private static volatile int exhaustDuration = 0;

	private static int consumedCycle = -1;
	private static float lastRenderTime = Float.NaN;
	private static long feedbackNanos = 0L;
	private static ClashMeter.Grade feedbackGrade = null;

	private ClientBeamClashState() {
	}

	public static void update(long startGameTime, long meterSeed, float advantage, int selfColor, int foeColor,
	                          int opponentId, Vec3 clashPoint) {
		boolean sameClash = active && ClientBeamClashState.startGameTime == startGameTime
				&& ClientBeamClashState.meterSeed == meterSeed;
		if (!sameClash) resetLocal();
		ClientBeamClashState.startGameTime = startGameTime;
		ClientBeamClashState.meterSeed = meterSeed;
		ClientBeamClashState.advantage = advantage;
		ClientBeamClashState.selfColor = selfColor;
		ClientBeamClashState.foeColor = foeColor;
		ClientBeamClashState.opponentId = opponentId;
		ClientBeamClashState.clashPoint = clashPoint;
		exhaustedUntil = 0L;
		active = true;
	}

	public static void clear(int exhaustTicks) {
		active = false;
		clashPoint = null;
		resetLocal();
		Minecraft mc = Minecraft.getInstance();
		if (exhaustTicks > 0 && mc.level != null) {
			exhaustDuration = exhaustTicks;
			exhaustedUntil = mc.level.getGameTime() + exhaustTicks;
		} else {
			exhaustDuration = 0;
			exhaustedUntil = 0L;
		}
	}

	public static void clear() {
		clear(0);
	}

	private static void resetLocal() {
		consumedCycle = -1;
		lastRenderTime = Float.NaN;
		feedbackNanos = 0L;
		feedbackGrade = null;
	}

	public static boolean isExhausted() {
		if (exhaustedUntil <= 0L) return false;
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null && mc.level.getGameTime() < exhaustedUntil;
	}

	public static float exhaustRemaining01(float partialTick) {
		if (!isExhausted() || exhaustDuration <= 0) return 0.0f;
		Minecraft mc = Minecraft.getInstance();
		float remaining = (exhaustedUntil - mc.level.getGameTime()) - partialTick;
		return Math.max(0.0f, Math.min(1.0f, remaining / exhaustDuration));
	}

	public static float simTime(float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return 0.0f;
		return (float) (mc.level.getGameTime() - startGameTime) + partialTick;
	}

	public static void noteRenderTime(float time) {
		lastRenderTime = time;
	}

	public static void onLocalPress() {
		if (!active) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;

		float now = simTime(0.0f);
		float pressTime = now - 0.5f;
		if (!Float.isNaN(lastRenderTime) && lastRenderTime <= now && now - lastRenderTime <= RENDER_TIME_STALE_TICKS) {
			pressTime = lastRenderTime;
		}
		pressTime = Math.max(0.0f, pressTime);

		ClashMeter.Sample sample = ClashMeter.sample(meterSeed, pressTime);
		if (sample.cycle().index() > consumedCycle) {
			consumedCycle = sample.cycle().index();
			feedbackGrade = sample.grade();
			feedbackNanos = System.nanoTime();
		}
		NetworkHandler.sendToServer(new BeamClashInputC2S(pressTime, sample.marker()));
	}

	public static ClashMeter.Grade feedbackGrade() {
		return feedbackAge01() < 1.0f ? feedbackGrade : null;
	}

	public static float feedbackAge01() {
		if (feedbackGrade == null) return 1.0f;
		long age = System.nanoTime() - feedbackNanos;
		if (age < 0L) return 1.0f;
		return Math.min(1.0f, age / (float) FEEDBACK_NANOS);
	}

	public static long meterSeed() {
		return meterSeed;
	}

	public static long startGameTime() {
		return startGameTime;
	}

	public static float advantage() {
		return advantage;
	}

	public static int selfColor() {
		return selfColor;
	}

	public static int foeColor() {
		return foeColor;
	}

	public static int opponentId() {
		return opponentId;
	}

	public static Vec3 clashPoint() {
		return clashPoint;
	}
}
