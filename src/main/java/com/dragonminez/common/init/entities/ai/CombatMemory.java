package com.dragonminez.common.init.entities.ai;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class CombatMemory {

    public enum Outcome { PENDING, HIT, MISS }

    public static final class ActionRecord {
        public final int id;
        public final boolean combo;
        public final long tick;
        public Outcome outcome = Outcome.PENDING;

        ActionRecord(int id, boolean combo, long tick) {
            this.id = id;
            this.combo = combo;
            this.tick = tick;
        }
    }

    public static final class AttackerProfile {
        public float strongestDamage;
        public long strongestTick = -1L;
        public long lastTechniqueTick = -1L;
        public int techniquesSeen;
        public long minInterval = Long.MAX_VALUE;
        public float lastChargeSeen = 1.0F;
        public long lastChargeTick = -1L;
        public boolean chargingLastSeen;
        public float totalDamageToUs;
        public int blockedOurHits;
    }

    private static final int MAX_RECORDS = 16;
    private static final int KI_OUTCOME_WINDOW = 110;
    private static final int COMBO_OUTCOME_WINDOW = 90;
    private static final int DAMAGE_WINDOW = 200;
    private static final int THREAT_TTL = 60;
    private static final int SKILL_SLOTS = 40;
    private static final int COMBO_OFFSET = 40;
    private static final int SLOTS = 64;

    private final ArrayDeque<ActionRecord> actions = new ArrayDeque<>();
    private ActionRecord pendingKi;
    private ActionRecord pendingCombo;
    private final int[] hits = new int[SLOTS];
    private final int[] misses = new int[SLOTS];
    private int consecutiveKiMisses;
    private long lastCastTick = -1L;
    private int lastCastId = -1;
    private long lastComboTick = -1L;
    private long lastComboHitTick = -1L;
    private long lastMeleeHitTick = -1L;
    private int meleeWhiffStreak;

    private final ArrayDeque<long[]> damageTaken = new ArrayDeque<>();
    private final Map<UUID, AttackerProfile> attackers = new HashMap<>();
    private final Map<UUID, Long> knownThreats = new HashMap<>();
    private final Map<UUID, Long> seenProjectiles = new HashMap<>();
    private final Map<UUID, Long> knownPending = new HashMap<>();
    private static final int SEEN_TTL = 400;
    private static final int PENDING_TTL = 100;

    private long lastBlindCastTick = -1L;
    private UUID lastAttacker;
    private long lastAttackedTick = -1L;
    private UUID lockedTargetId;
    private long targetLockedAt = -1L;

    private long lastZanzokenTick = -1L;
    private long lastBackstepTick = -1L;
    private long lastSidestepTick = -1L;
    private long lastKiteTick = -1L;
    private long lastTeleportTick = -1L;
    private long lastDefensiveBarrierTick = -1L;
    private long lastBlockedTick = -1L;
    private int timesTargetBlockedUs;

    private static int slot(int id, boolean combo) {
        int s = combo ? COMBO_OFFSET + id : id;
        return s >= 0 && s < SLOTS ? s : SLOTS - 1;
    }

    private void push(ActionRecord record) {
        this.actions.addLast(record);
        while (this.actions.size() > MAX_RECORDS) this.actions.pollFirst();
    }

    public void recordCast(int skillId, long now) {
        this.resolve(now);
        ActionRecord record = new ActionRecord(skillId, false, now);
        this.push(record);
        this.pendingKi = record;
        this.lastCastTick = now;
        this.lastCastId = skillId;
        if (skillId == 25) this.lastBlindCastTick = now;
        if (skillId == 6) this.lastDefensiveBarrierTick = now;
    }

    public void recordCombo(int comboId, long now) {
        this.resolve(now);
        ActionRecord record = new ActionRecord(comboId, true, now);
        this.push(record);
        this.pendingCombo = record;
        this.lastComboTick = now;
    }

    public void onDealtDamage(long now, boolean ki, boolean whileComboing) {
        if (ki && this.pendingKi != null && this.pendingKi.outcome != Outcome.HIT && now - this.pendingKi.tick <= KI_OUTCOME_WINDOW + 60) {
            this.pendingKi.outcome = Outcome.HIT;
            this.hits[slot(this.pendingKi.id, false)]++;
            this.consecutiveKiMisses = 0;
        }
        if (!ki && whileComboing && this.pendingCombo != null && this.pendingCombo.outcome != Outcome.HIT) {
            this.pendingCombo.outcome = Outcome.HIT;
            this.hits[slot(this.pendingCombo.id, true)]++;
            this.lastComboHitTick = now;
        }
    }

    public void recordMeleeResult(boolean hit, long now) {
        if (hit) {
            this.lastMeleeHitTick = now;
            this.meleeWhiffStreak = 0;
        } else {
            this.meleeWhiffStreak++;
        }
    }

    public void recordTargetBlocked(long now) {
        this.timesTargetBlockedUs++;
        this.lastBlockedTick = now;
    }

    public void resolve(long now) {
        if (this.pendingKi != null && this.pendingKi.outcome == Outcome.PENDING && now - this.pendingKi.tick > KI_OUTCOME_WINDOW) {
            this.pendingKi.outcome = Outcome.MISS;
            this.misses[slot(this.pendingKi.id, false)]++;
            this.consecutiveKiMisses++;
            this.pendingKi = null;
        }
        if (this.pendingCombo != null && this.pendingCombo.outcome == Outcome.PENDING && now - this.pendingCombo.tick > COMBO_OUTCOME_WINDOW) {
            this.pendingCombo.outcome = Outcome.MISS;
            this.misses[slot(this.pendingCombo.id, true)]++;
            this.pendingCombo = null;
        }
        this.pruneDamage(now);
    }

    public float hitRate(int skillId, boolean combo) {
        int s = slot(skillId, combo);
        return (this.hits[s] + 1.0F) / (this.hits[s] + this.misses[s] + 2.0F);
    }

    public Outcome lastOutcome(int skillId, boolean combo) {
        Iterator<ActionRecord> it = this.actions.descendingIterator();
        while (it.hasNext()) {
            ActionRecord r = it.next();
            if (r.combo == combo && r.id == skillId) return r.outcome;
        }
        return Outcome.PENDING;
    }

    public boolean lastKiMissedAgainstMovingTarget() {
        return this.consecutiveKiMisses > 0;
    }

    public int consecutiveKiMisses() {
        return this.consecutiveKiMisses;
    }

    public long ticksSinceLastCast(long now) {
        return this.lastCastTick < 0 ? Long.MAX_VALUE : now - this.lastCastTick;
    }

    public int lastCastId() {
        return this.lastCastId;
    }

    public long ticksSinceLastCombo(long now) {
        return this.lastComboTick < 0 ? Long.MAX_VALUE : now - this.lastComboTick;
    }

    public long ticksSinceComboHit(long now) {
        return this.lastComboHitTick < 0 ? Long.MAX_VALUE : now - this.lastComboHitTick;
    }

    public long ticksSinceMeleeHit(long now) {
        return this.lastMeleeHitTick < 0 ? Long.MAX_VALUE : now - this.lastMeleeHitTick;
    }

    public int meleeWhiffStreak() {
        return this.meleeWhiffStreak;
    }

    public void recordDamageTaken(float amount, UUID attacker, long now) {
        this.damageTaken.addLast(new long[]{now, Float.floatToIntBits(amount)});
        if (attacker != null) {
            this.lastAttacker = attacker;
            this.lastAttackedTick = now;
            this.attacker(attacker).totalDamageToUs += amount;
        }
        this.pruneDamage(now);
    }

    private void pruneDamage(long now) {
        while (!this.damageTaken.isEmpty() && now - this.damageTaken.peekFirst()[0] > DAMAGE_WINDOW) {
            this.damageTaken.pollFirst();
        }
    }

    public float damageTakenWithin(int ticks, long now) {
        float sum = 0.0F;
        for (long[] entry : this.damageTaken) {
            if (now - entry[0] <= ticks) sum += Float.intBitsToFloat((int) entry[1]);
        }
        return sum;
    }

    public UUID lastAttacker() {
        return this.lastAttacker;
    }

    public long ticksSinceAttacked(long now) {
        return this.lastAttackedTick < 0 ? Long.MAX_VALUE : now - this.lastAttackedTick;
    }

    public AttackerProfile attacker(UUID id) {
        return this.attackers.computeIfAbsent(id, k -> new AttackerProfile());
    }

    public AttackerProfile attackerOrNull(UUID id) {
        return id == null ? null : this.attackers.get(id);
    }

    public void observeTechniqueFired(UUID attacker, float damage, long now) {
        if (attacker == null) return;
        AttackerProfile p = this.attacker(attacker);
        if (p.lastTechniqueTick >= 0) p.minInterval = Math.min(p.minInterval, Math.max(1L, now - p.lastTechniqueTick));
        p.lastTechniqueTick = now;
        p.techniquesSeen++;
        if (damage >= p.strongestDamage * 0.9F) {
            p.strongestDamage = Math.max(p.strongestDamage, damage);
            p.strongestTick = now;
        }
    }

    public void observeCharge(UUID attacker, boolean charging, float percent, long now) {
        if (attacker == null) return;
        AttackerProfile p = this.attacker(attacker);
        if (charging) {
            p.lastChargeSeen = Math.max(0.0F, percent);
            p.lastChargeTick = now;
        }
        p.chargingLastSeen = charging;
    }

    public float lastChargeSeen(UUID attacker, long now) {
        AttackerProfile p = this.attackerOrNull(attacker);
        if (p == null || p.lastChargeTick < 0 || now - p.lastChargeTick > 80) return 1.0F;
        return p.lastChargeSeen;
    }

    public boolean expectsStrongerSoon(UUID attacker, float incomingDamage, int horizonTicks, long now) {
        AttackerProfile p = this.attackerOrNull(attacker);
        if (p == null || p.strongestTick < 0) return false;
        if (p.strongestDamage < incomingDamage * 1.4F) return false;
        long sinceStrong = now - p.strongestTick;
        long assumedCooldown = Math.max(300L, p.minInterval == Long.MAX_VALUE ? 300L : p.minInterval);
        if (sinceStrong < assumedCooldown * 0.4D) return false;
        return sinceStrong >= assumedCooldown || (assumedCooldown - sinceStrong) < horizonTicks;
    }

    public boolean isKnownThreat(UUID id, long now) {
        Long seen = this.knownThreats.get(id);
        if (seen == null) return false;
        if (now - seen > THREAT_TTL) {
            this.knownThreats.remove(id);
            return false;
        }
        return true;
    }

    public void markThreat(UUID id, long now) {
        this.knownThreats.put(id, now);
        if (this.knownThreats.size() > 32) {
            this.knownThreats.entrySet().removeIf(e -> now - e.getValue() > THREAT_TTL);
        }
    }

    public boolean isKnownPending(UUID id, long now) {
        Long seen = this.knownPending.get(id);
        if (seen == null) return false;
        if (now - seen > PENDING_TTL) {
            this.knownPending.remove(id);
            return false;
        }
        return true;
    }

    public void markPending(UUID id, long now) {
        this.knownPending.put(id, now);
        if (this.knownPending.size() > 32) {
            this.knownPending.entrySet().removeIf(e -> now - e.getValue() > PENDING_TTL);
        }
    }

    public boolean isSeenProjectile(UUID id, long now) {
        Long seen = this.seenProjectiles.get(id);
        if (seen == null) return false;
        if (now - seen > SEEN_TTL) {
            this.seenProjectiles.remove(id);
            return false;
        }
        return true;
    }

    public void markSeenProjectile(UUID id, long now) {
        this.seenProjectiles.put(id, now);
        if (this.seenProjectiles.size() > 48) {
            this.seenProjectiles.entrySet().removeIf(e -> now - e.getValue() > SEEN_TTL);
        }
    }

    public boolean blindedRecently(long now) {
        return this.lastBlindCastTick >= 0 && now - this.lastBlindCastTick < 400;
    }

    public void lockTarget(UUID targetId, long now) {
        if (targetId == null) {
            this.lockedTargetId = null;
            this.targetLockedAt = -1L;
            return;
        }
        if (!targetId.equals(this.lockedTargetId)) {
            this.lockedTargetId = targetId;
            this.targetLockedAt = now;
        }
    }

    public boolean targetLockedWithin(int ticks, long now) {
        return this.targetLockedAt >= 0 && now - this.targetLockedAt < ticks;
    }

    public UUID lockedTarget() {
        return this.lockedTargetId;
    }

    public void noteZanzoken(long now) { this.lastZanzokenTick = now; }
    public void noteBackstep(long now) { this.lastBackstepTick = now; }
    public void noteSidestep(long now) { this.lastSidestepTick = now; }
    public void noteKite(long now) { this.lastKiteTick = now; }
    public void noteTeleport(long now) { this.lastTeleportTick = now; }

    public long ticksSinceZanzoken(long now) { return this.lastZanzokenTick < 0 ? Long.MAX_VALUE : now - this.lastZanzokenTick; }
    public long ticksSinceBackstep(long now) { return this.lastBackstepTick < 0 ? Long.MAX_VALUE : now - this.lastBackstepTick; }
    public long ticksSinceSidestep(long now) { return this.lastSidestepTick < 0 ? Long.MAX_VALUE : now - this.lastSidestepTick; }
    public long ticksSinceKite(long now) { return this.lastKiteTick < 0 ? Long.MAX_VALUE : now - this.lastKiteTick; }
    public long ticksSinceTeleport(long now) { return this.lastTeleportTick < 0 ? Long.MAX_VALUE : now - this.lastTeleportTick; }
    public long ticksSinceDefensiveBarrier(long now) { return this.lastDefensiveBarrierTick < 0 ? Long.MAX_VALUE : now - this.lastDefensiveBarrierTick; }
    public long ticksSinceBlocked(long now) { return this.lastBlockedTick < 0 ? Long.MAX_VALUE : now - this.lastBlockedTick; }
    public int timesTargetBlockedUs() { return this.timesTargetBlockedUs; }

    public void inherit(CombatMemory other) {
        this.actions.addAll(other.actions);
        System.arraycopy(other.hits, 0, this.hits, 0, SLOTS);
        System.arraycopy(other.misses, 0, this.misses, 0, SLOTS);
        this.consecutiveKiMisses = other.consecutiveKiMisses;
        this.attackers.putAll(other.attackers);
        this.knownThreats.putAll(other.knownThreats);
        this.lastAttacker = other.lastAttacker;
        this.lastAttackedTick = other.lastAttackedTick;
        this.lockedTargetId = other.lockedTargetId;
        this.targetLockedAt = other.targetLockedAt;
        this.lastBlindCastTick = other.lastBlindCastTick;
        this.timesTargetBlockedUs = other.timesTargetBlockedUs;
    }
}
