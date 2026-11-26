package com.dragonminez.client.systems;

import com.dragonminez.Reference;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class KiBurstShakeState {

	private static final int OPENING_SHAKE_TICKS = 8;
	private static final int FULL_SHAKE_TICKS = 16;

	public static final class Burst {
		private int ticksRemaining;
		private final int totalTicks;
		private final boolean full;
		private final float radius;

		private Burst(int totalTicks, boolean full, float radius) {
			this.ticksRemaining = totalTicks;
			this.totalTicks = totalTicks;
			this.full = full;
			this.radius = radius;
		}

		public float fade() {
			if (totalTicks <= 0) return 0.0f;
			return Math.max(0.0f, (float) ticksRemaining / (float) totalTicks);
		}

		public boolean isFull() {
			return full;
		}

		public float radius() {
			return radius;
		}
	}

	private static final Map<Integer, Burst> ACTIVE = new ConcurrentHashMap<>();

	private KiBurstShakeState() {}

	public static void start(int entityId, boolean full, float radius) {
		ACTIVE.put(entityId, new Burst(full ? FULL_SHAKE_TICKS : OPENING_SHAKE_TICKS, full, radius));
	}

	public static Map<Integer, Burst> active() {
		return ACTIVE;
	}

	public static void clear() {
		ACTIVE.clear();
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Pre event) {
		if (Minecraft.getInstance().level == null) {
			if (!ACTIVE.isEmpty()) ACTIVE.clear();
			return;
		}
		if (Minecraft.getInstance().isPaused() || ACTIVE.isEmpty()) return;

		Iterator<Map.Entry<Integer, Burst>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Burst burst = it.next().getValue();
			burst.ticksRemaining--;
			if (burst.ticksRemaining <= 0) it.remove();
		}
	}
}
