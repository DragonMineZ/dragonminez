package com.dragonminez.client.render.hair;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class HairSimulation {
	public static final double STEP_SECONDS = 1.0 / 60.0;
	public static final double MAX_FRAME_SECONDS = 0.25;
	public static final double RESET_AFTER_SECONDS = 1.0;
	public static final double TELEPORT_DISTANCE = 6.0;
	public static final int FULL_SUBSTEPS = 4;
	public static final int REDUCED_SUBSTEPS = 1;
	public static final float MAX_STEP_DISTANCE = 0.25f;
	public static final float CORRECTION_EPSILON = 1.0e-4f;
	public static final float TIP_STIFFNESS_RATIO = 0.6f;
	public static final float GRAVITY_ACCELERATION = 20.0f;
	public static final float AIR_DRAG = 2.0f;
	public static final float COLLISION_PADDING = 0.02f;
	public static final float IDLE_WIND = 6.0f;
	public static final float AURA_WIND = 15.0f;
	public static final float AURA_LIFT = 8.0f;
	public static final float CHARGE_LIFT = 30.0f;
	public static final float CHARGE_FLICKER = 10.0f;
	public static final float IDLE_WIND_SPEED = 1.2f;
	public static final float CHARGE_WIND_SPEED = 9.0f;
	public static final float WIND_SEGMENT_PHASE = 0.6f;
	public static final float WIND_STRAND_PHASE = 0.37f;

	private static final long STATE_TTL_MS = 30_000L;
	private static final long CLEANUP_INTERVAL_MS = 5_000L;
	private static final Map<Integer, HairEntityState> STATES = new HashMap<>();
	private static long lastCleanupMs;

	private HairSimulation() {}

	public static HairEntityState stateFor(int entityId, long nowMs) {
		if (nowMs - lastCleanupMs >= CLEANUP_INTERVAL_MS) {
			cleanup(nowMs);
			lastCleanupMs = nowMs;
		}
		HairEntityState state = STATES.computeIfAbsent(entityId, id -> new HairEntityState());
		state.touch(nowMs);
		return state;
	}

	private static void cleanup(long nowMs) {
		Iterator<HairEntityState> iterator = STATES.values().iterator();
		while (iterator.hasNext()) {
			if (nowMs - iterator.next().lastSeenMs() > STATE_TTL_MS) iterator.remove();
		}
	}
}
