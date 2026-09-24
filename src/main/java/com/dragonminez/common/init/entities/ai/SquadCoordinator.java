package com.dragonminez.common.init.entities.ai;

import com.dragonminez.common.init.entities.animal.DinoFlyEntity;
import com.dragonminez.common.init.entities.animal.DinoGlobalEntity;
import com.dragonminez.common.init.entities.animal.SabertoothEntity;
import com.dragonminez.common.init.entities.bioandroid.CellJrEntity;
import com.dragonminez.common.init.entities.namek.NamekWarriorEntity;
import com.dragonminez.common.init.entities.redribbon.RedRibbonEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SquadCoordinator {

    public static final String TOURNAMENT_MATCH_TAG = "dmz_tournament_match";
    private static final double FIRING_LINE_TOLERANCE = 1.6D;
    private static final double MELEE_CROWD_RADIUS = 3.5D;

    private SquadCoordinator() {}

    public static String factionKey(Mob mob) {
        if (mob instanceof DBSagasEntity saga) {
            if (saga.hasQuestTeam()) return "team:" + saga.getQuestTeam();
            if (saga.getPersistentData().contains(DBSagasEntity.RAID_ID_TAG)) {
                return "raid:" + saga.getPersistentData().getString(DBSagasEntity.RAID_ID_TAG);
            }
            if (saga.getPersistentData().contains(TOURNAMENT_MATCH_TAG)) {
                return "match:" + saga.getPersistentData().getString(TOURNAMENT_MATCH_TAG);
            }
        }
        if (mob instanceof CellJrEntity jr) {
            LivingEntity owner = jr.resolveOwner();
            return owner != null ? "celljr:" + owner.getUUID() : "celljr";
        }
        if (mob instanceof SabertoothEntity) return "sabertooth";
        if (mob instanceof DinoGlobalEntity || mob instanceof DinoFlyEntity) return "dino";
        if (mob instanceof RedRibbonEntity) return "red_ribbon";
        if (mob instanceof NamekWarriorEntity) return "namek";
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        String path = key != null ? key.getPath() : "unknown";
        int end = path.length();
        while (end > 0 && Character.isDigit(path.charAt(end - 1))) end--;
        return "type:" + path.substring(0, end);
    }

    public static boolean isAlly(Mob a, Mob b) {
        if (a == b) return true;
        if (a.isAlliedTo(b)) return true;
        return factionKey(a).equals(factionKey(b));
    }

    public static List<Mob> allies(Mob self, double radius) {
        List<Mob> out = new ArrayList<>();
        for (Mob other : self.level().getEntitiesOfClass(Mob.class, self.getBoundingBox().inflate(radius))) {
            if (other == self || !other.isAlive()) continue;
            if (isAlly(self, other)) out.add(other);
        }
        return out;
    }

    public static List<Mob> engaging(List<Mob> allies, LivingEntity target) {
        List<Mob> out = new ArrayList<>();
        if (target == null) return out;
        for (Mob ally : allies) if (ally.getTarget() == target) out.add(ally);
        return out;
    }

    public static int meleeCrowd(List<Mob> allies, LivingEntity target) {
        if (target == null) return 0;
        int count = 0;
        for (Mob ally : allies) {
            if (ally.getTarget() == target && ally.distanceTo(target) <= MELEE_CROWD_RADIUS) count++;
        }
        return count;
    }

    public static boolean inFiringLine(Mob self, LivingEntity target, List<Mob> allies) {
        if (target == null || allies.isEmpty()) return false;
        Vec3 start = self.getEyePosition();
        Vec3 end = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 seg = end.subtract(start);
        double lenSqr = seg.lengthSqr();
        if (lenSqr < 1.0E-4D) return false;
        for (Mob ally : allies) {
            Vec3 c = ally.position().add(0.0D, ally.getBbHeight() * 0.5D, 0.0D);
            double t = c.subtract(start).dot(seg) / lenSqr;
            if (t <= 0.05D || t >= 0.95D) continue;
            Vec3 closest = start.add(seg.scale(t));
            double tol = FIRING_LINE_TOLERANCE + ally.getBbWidth() * 0.5D;
            if (closest.distanceToSqr(c) < tol * tol) return true;
        }
        return false;
    }

    public static boolean allyCastingHeavy(List<Mob> allies, LivingEntity target) {
        for (Mob ally : allies) {
            if (!(ally instanceof DBSagasEntity saga)) continue;
            if (saga.getTarget() != target) continue;
            if (saga.isCasting()) {
                DBSagasEntity.KiSkillType type = DBSagasEntity.KiSkillType.fromId(saga.getSkillType());
                if (type == null || type.getTier() != DBSagasEntity.Tier.WEAK) return true;
            }
            if (saga.isComboing()) return true;
        }
        return false;
    }

    public static LivingEntity sharedFocus(List<Mob> allies) {
        Map<LivingEntity, Integer> votes = new HashMap<>();
        for (Mob ally : allies) {
            LivingEntity t = ally.getTarget();
            if (t != null && t.isAlive()) votes.merge(t, 1, Integer::sum);
        }
        LivingEntity best = null;
        int bestVotes = 0;
        for (Map.Entry<LivingEntity, Integer> e : votes.entrySet()) {
            if (e.getValue() > bestVotes) {
                best = e.getKey();
                bestVotes = e.getValue();
            }
        }
        return best;
    }

    public static Vec3 flankPoint(Mob self, LivingEntity target, List<Mob> allies, double ringRadius) {
        List<Mob> members = new ArrayList<>(engaging(allies, target));
        members.add(self);
        if (members.size() <= 1) return null;
        members.sort(Comparator.comparing(Mob::getUUID));
        int n = members.size();
        int index = 0;
        UUID mine = self.getUUID();
        for (int i = 0; i < n; i++) {
            if (members.get(i).getUUID().equals(mine)) {
                index = i;
                break;
            }
        }
        Mob anchor = members.get(0);
        double anchorAngle = Math.atan2(anchor.getZ() - target.getZ(), anchor.getX() - target.getX());
        double angle = anchorAngle + (Math.PI * 2.0D / n) * index;
        return new Vec3(target.getX() + Math.cos(angle) * ringRadius, target.getY(), target.getZ() + Math.sin(angle) * ringRadius);
    }
}
