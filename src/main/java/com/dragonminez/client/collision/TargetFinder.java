package com.dragonminez.client.collision;

import com.dragonminez.common.combat.weapon.WeaponAttributes.Attack;
import com.dragonminez.client.util.AttackRangeExtensions;
import com.dragonminez.common.combat.collision.CollisionHelper;
import com.dragonminez.common.combat.collision.MeleeHitbox;
import com.dragonminez.common.combat.collision.OrientedBoundingBox;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TargetFinder {
    public static class TargetResult {
        public List<Entity> entities;
        public OrientedBoundingBox obb;
        public TargetResult(List<Entity> entities, OrientedBoundingBox obb) {
            this.entities = entities;
            this.obb = obb;
        }
    }

    public static TargetResult findAttackTargetResult(Player player, Entity cursorTarget, Attack attack, double attackRange) {
        Vec3 origin = MeleeHitbox.origin(player, player.getXRot(), player.getYRot());
        List<Entity> entities = getInitialTargets(player, cursorTarget, attackRange);

        if (!AttackRangeExtensions.sources().isEmpty()) {
            AttackRangeExtensions.Context context = new AttackRangeExtensions.Context(player, attackRange);
            for (var source : AttackRangeExtensions.sources()) {
                var modifier = source.apply(context);
                if (modifier != null) {
                    if (modifier.operation() == AttackRangeExtensions.Operation.ADD) attackRange += modifier.value();
                    else attackRange *= modifier.value();
                }
            }
        }

        OrientedBoundingBox obb = MeleeHitbox.build(player, origin, attack, attackRange, player.getXRot(), player.getYRot());

        entities.sort((e1, e2) -> {
            if (e1 == cursorTarget) return -1;
            if (e2 == cursorTarget) return 1;
            return Double.compare(e1.distanceToSqr(player), e2.distanceToSqr(player));
        });

        List<Entity> validTargets = filterTargetsByOBB(entities, origin, obb);

        return new TargetResult(validTargets, obb);
    }

    private static List<Entity> getInitialTargets(Player player, Entity cursorTarget, double attackRange) {
        var box = player.getBoundingBox().inflate(attackRange + 1.0);
        return player.level().getEntitiesOfClass(Entity.class, box)
                .stream()
                .filter(e -> e != player && e.isAttackable() && !e.isSpectator())
                .filter(e -> TargetHelper.getRelation(player, e) != TargetHelper.Relation.FRIENDLY)
                .collect(Collectors.toList());
    }

    private static List<Entity> filterTargetsByOBB(List<Entity> entities, Vec3 origin, OrientedBoundingBox obb) {
        List<Entity> valid = new ArrayList<>();
        Set<Entity> owners = new HashSet<>();
        for (Entity entity : entities) {
            Entity owner = TargetHelper.resolveHittable(entity);
            if (owners.contains(owner)) continue;
            if (!obb.intersects(entity.getBoundingBox())) continue;
            Vec3 distanceVector = CollisionHelper.distanceVector(origin, entity.getBoundingBox());
            Vec3 closestPoint = origin.add(distanceVector);
            if (!rayContainsNoObstacle(origin, closestPoint)) continue;
            owners.add(owner);
            valid.add(entity);
        }
        return valid;
    }

    private static boolean rayContainsNoObstacle(Vec3 start, Vec3 end) {
        var client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return true;
        var hit = client.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player));
        return hit.getType() != HitResult.Type.BLOCK;
    }
}
