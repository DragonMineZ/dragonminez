package com.dragonminez.common.combat.logic.player;

import com.dragonminez.common.combat.player.AttackHand;
import com.dragonminez.common.network.C2S.CombatAttackRequestC2S;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class MeleeWindupTracker {
	private static final long TICK_NANOS = TimeUnit.MILLISECONDS.toNanos(50);
	private static final long MAX_PENDING_NANOS = TimeUnit.SECONDS.toNanos(2);
	private static final long MIN_START_GAP_NANOS = TimeUnit.MILLISECONDS.toNanos(100);
	private static final long MAX_DEFER_NANOS = TimeUnit.SECONDS.toNanos(1);
	private static final int WINDUP_TOLERANCE_TICKS = 1;
	private static final int ATTACK_RATE_TOLERANCE_TICKS = 2;

	private record Start(long arrivalNanos, int comboCount, int selectedSlot) { }

	private record Deferred(CombatAttackRequestC2S request, long dueNanos) { }

	private static final Map<UUID, Start> STARTS = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> LAST_START_ARRIVAL = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> LAST_HIT_SCHEDULED = new ConcurrentHashMap<>();
	private static final Map<UUID, Deferred> DEFERRED = new ConcurrentHashMap<>();

	private MeleeWindupTracker() { }

	public static boolean canStrike(ServerPlayer player) {
		if (player.isBlocking()) return false;
		return StatsProvider.get(StatsCapability.INSTANCE, player)
				.map(stats -> !stats.getStatus().isStunned() && !stats.getStatus().isBlocking())
				.orElse(false);
	}

	public static boolean registerStart(ServerPlayer player, int comboCount, int selectedSlot, long arrivalNanos) {
		UUID id = player.getUUID();
		Long lastStart = LAST_START_ARRIVAL.get(id);
		if (lastStart != null && arrivalNanos - lastStart < MIN_START_GAP_NANOS) return false;
		LAST_START_ARRIVAL.put(id, arrivalNanos);
		STARTS.put(id, new Start(arrivalNanos, comboCount, selectedSlot));
		return true;
	}

	public static void submitHit(ServerPlayer player, CombatAttackRequestC2S request, long arrivalNanos) {
		UUID id = player.getUUID();
		flushDeferred(player);

		Start start = STARTS.remove(id);
		if (start == null || start.comboCount() != request.getComboCount() || start.selectedSlot() != request.getSelectedSlot()) return;
		if (arrivalNanos - start.arrivalNanos() > MAX_PENDING_NANOS) return;

		AttackHand hand = PlayerAttackHelper.getCurrentAttack(player, request.getComboCount());
		if (hand == null) return;
		int windupTicks = PlayerAttackHelper.getMeleeAttackWindupTicks(PlayerAttackHelper.getAttackCooldownTicksCapped(player), hand);
		long dueNanos = start.arrivalNanos() + Math.max(0, windupTicks - WINDUP_TOLERANCE_TICKS) * TICK_NANOS;

		long minIntervalTicks = Math.max(0, (long) Math.floor(player.getCurrentItemAttackStrengthDelay()) - ATTACK_RATE_TOLERANCE_TICKS);
		Long lastHit = LAST_HIT_SCHEDULED.get(id);
		if (lastHit != null) dueNanos = Math.max(dueNanos, lastHit + minIntervalTicks * TICK_NANOS);
		if (dueNanos - arrivalNanos > MAX_DEFER_NANOS) return;
		LAST_HIT_SCHEDULED.put(id, dueNanos);

		if (System.nanoTime() >= dueNanos) CombatAttackRequestC2S.processAttackRequest(player, request);
		else DEFERRED.put(id, new Deferred(request, dueNanos));
	}

	public static void tick(ServerPlayer player) {
		Deferred deferred = DEFERRED.get(player.getUUID());
		if (deferred == null || System.nanoTime() < deferred.dueNanos()) return;
		DEFERRED.remove(player.getUUID());
		runDeferred(player, deferred);
	}

	public static void cancel(ServerPlayer player) {
		STARTS.remove(player.getUUID());
		DEFERRED.remove(player.getUUID());
	}

	public static void clear(UUID id) {
		STARTS.remove(id);
		DEFERRED.remove(id);
		LAST_START_ARRIVAL.remove(id);
		LAST_HIT_SCHEDULED.remove(id);
	}

	private static void flushDeferred(ServerPlayer player) {
		Deferred deferred = DEFERRED.remove(player.getUUID());
		if (deferred != null) runDeferred(player, deferred);
	}

	private static void runDeferred(ServerPlayer player, Deferred deferred) {
		if (!canStrike(player)) return;
		if (deferred.request().getSelectedSlot() != player.getInventory().selected) return;
		CombatAttackRequestC2S.processAttackRequest(player, deferred.request());
	}
}
