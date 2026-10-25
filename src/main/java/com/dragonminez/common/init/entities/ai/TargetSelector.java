package com.dragonminez.common.init.entities.ai;

import com.dragonminez.common.quest.PartyManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class TargetSelector {

    private static final double SWITCH_MARGIN = 2.0D;

    private TargetSelector() {}

    public static List<ServerPlayer> rivalParty(LivingEntity target) {
        List<ServerPlayer> out = new ArrayList<>();
        if (target instanceof ServerPlayer sp) {
            List<ServerPlayer> members = PartyManager.getAllPartyMembers(sp);
            if (members != null) out.addAll(members);
            if (!out.contains(sp)) out.add(sp);
        }
        return out;
    }

    public static List<LivingEntity> candidates(Mob self, double range, Predicate<LivingEntity> allowed) {
        List<LivingEntity> out = new ArrayList<>();
        for (Player player : self.level().getEntitiesOfClass(Player.class, self.getBoundingBox().inflate(range))) {
            if (!(player instanceof ServerPlayer sp)) continue;
            if (!sp.isAlive() || sp.isSpectator() || sp.isCreative()) continue;
            if (allowed != null && !allowed.test(sp)) continue;
            out.add(sp);
        }
        LivingEntity current = self.getTarget();
        if (current != null && current.isAlive() && !out.contains(current) && (allowed == null || allowed.test(current))) {
            if (self.distanceTo(current) <= range * 1.25D) out.add(current);
        }
        return out;
    }

    public static LivingEntity choose(Mob self, AiProfile profile, CombatMemory memory, long now, double range,
                                      Predicate<LivingEntity> allowed) {
        LivingEntity current = self.getTarget();
        boolean currentValid = current != null && current.isAlive() && self.distanceTo(current) <= range * 1.25D
                && !(current instanceof Player p && (p.isSpectator() || p.isCreative()));
        List<LivingEntity> cands = candidates(self, range, allowed);
        if (cands.isEmpty()) return currentValid ? current : null;

        if (profile.tier == AiTier.NOVICE) {
            return currentValid ? current : nearest(self, cands);
        }

        if (profile.tier == AiTier.SMART) {
            if (memory != null && memory.lastAttacker() != null && memory.ticksSinceAttacked(now) < 100) {
                for (LivingEntity c : cands) if (c.getUUID().equals(memory.lastAttacker())) return c;
            }
            return currentValid ? current : nearest(self, cands);
        }

        LivingEntity best = null;
        double bestScore = -Double.MAX_VALUE;
        double currentScore = -Double.MAX_VALUE;
        for (LivingEntity c : cands) {
            double score = score(self, c, memory, now, range);
            if (c == current) currentScore = score;
            if (score > bestScore) {
                bestScore = score;
                best = c;
            }
        }
        if (currentValid && best != current) {
            boolean retained = memory != null && memory.targetLockedWithin(profile.targetRetention, now);
            if (retained && bestScore - currentScore < SWITCH_MARGIN * 2.0D) return current;
            if (bestScore - currentScore < SWITCH_MARGIN) return current;
        }
        return best != null ? best : current;
    }

    private static double score(Mob self, LivingEntity c, CombatMemory memory, long now, double range) {
        double dist = self.distanceTo(c);
        double score = 3.0D - (dist / Math.max(1.0D, range)) * 3.0D;
        float hpPct = c.getMaxHealth() > 0.0F ? c.getHealth() / c.getMaxHealth() : 1.0F;
        score += (1.0F - hpPct) * 2.5D;
        score += self.getSensing().hasLineOfSight(c) ? 0.8D : -1.0D;
        PlayerRead read = PlayerRead.of(c);
        if (read.committedCast) score += 1.5D;
        if (read.transforming) score += 1.0D;
        if (read.blocking) score -= 0.8D;
        if (memory != null && memory.lastAttacker() != null && memory.lastAttacker().equals(c.getUUID())
                && memory.ticksSinceAttacked(now) < 60) {
            score += 1.0D;
        }
        if (c == self.getTarget()) score += 0.6D;
        return score;
    }

    public static LivingEntity nearest(Mob self, List<LivingEntity> cands) {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity c : cands) {
            double d = self.distanceToSqr(c);
            if (d < bestDist) {
                bestDist = d;
                best = c;
            }
        }
        return best;
    }
}
