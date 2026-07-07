package com.dragonminez.client.collision;

import com.dragonminez.common.combat.weapon.WeaponAttributes.Attack;
import com.dragonminez.client.util.AttackRangeExtensions;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
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
        Vec3 origin = getInitialTracingPoint(player);
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

        Vec3 size = WeaponHitBoxes.createHitbox(attack.hitbox(), attackRange, attack.angle() > 180);

        OrientedBoundingBox obb = new OrientedBoundingBox(origin, size, player.getXRot(), player.getYRot());
        boolean isSpinAttack = attack.angle() > 180;
        if (!isSpinAttack) {
            double forwardOffset = player.getBbWidth() * 0.3;
            obb.offsetAlongAxisZ(obb.extent.z - forwardOffset);
        }
        obb.updateVertex();

        List<Entity> validTargets = filterTargetsByOBB(entities, origin, obb);

        validTargets.sort((e1, e2) -> {
            if (e1 == cursorTarget) return -1;
            if (e2 == cursorTarget) return 1;
            return Double.compare(e1.distanceToSqr(player), e2.distanceToSqr(player));
        });

        return new TargetResult(validTargets, obb);
    }

    private static Vec3 getInitialTracingPoint(Player player) {
        Vec3 horizontalLook = new Vec3(player.getLookAngle().x, 0.0, player.getLookAngle().z);
        if (horizontalLook.lengthSqr() < 1.0E-6) {
            horizontalLook = player.getLookAngle();
        }
        horizontalLook = horizontalLook.normalize();

        double forwardOffset = player.getBbWidth() * 0.5;
        return player.position()
                .add(0, player.getBbHeight() * 0.8, 0)
                .add(horizontalLook.scale(forwardOffset));
    }

    private static List<Entity> getInitialTargets(Player player, Entity cursorTarget, double attackRange) {
        var box = player.getBoundingBox().inflate(attackRange + 1.0);
        List<Entity> targets = player.level().getEntitiesOfClass(Entity.class, box)
                .stream()
                .filter(e -> e != player && e.isAttackable() && !e.isSpectator())
                .filter(e -> TargetHelper.getRelation(player, e) != TargetHelper.Relation.FRIENDLY)
                .collect(Collectors.toList());

        if (player.level() instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
            for (net.minecraftforge.entity.PartEntity<?> part : clientLevel.getPartEntities()) {
                if (part.isAttackable() && !part.isSpectator() && part.getBoundingBox().intersects(box)
                        && TargetHelper.getRelation(player, part) != TargetHelper.Relation.FRIENDLY) {
                    targets.add(part);
                }
            }
        }
        return targets;
    }

    private static List<Entity> filterTargetsByOBB(List<Entity> entities, Vec3 origin, OrientedBoundingBox obb) {
        return entities.stream()
                .filter(entity -> {
                    if (!obb.intersects(entity.getBoundingBox())) return false;
                    Vec3 distanceVector = CollisionHelper.distanceVector(origin, entity.getBoundingBox());
                    Vec3 closestPoint = origin.add(distanceVector);
                    return rayContainsNoObstacle(origin, closestPoint);
                })
                .collect(Collectors.toList());
    }

    private static boolean rayContainsNoObstacle(Vec3 start, Vec3 end) {
        var client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return true;
        var hit = client.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player));
        return hit.getType() != HitResult.Type.BLOCK;
    }
}
