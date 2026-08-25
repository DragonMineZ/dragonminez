package com.dragonminez.common.combat.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

import java.util.ArrayList;
import java.util.List;

public final class MultipartTargeting {

    private MultipartTargeting() {
    }

    public static List<LivingEntity> collectTargets(Level level, AABB box) {
        List<LivingEntity> out = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, box));
        if (level instanceof ServerLevel serverLevel) {
            for (PartEntity<?> part : serverLevel.getPartEntities()) {
                if (part.getParent() instanceof LivingEntity parent
                        && !out.contains(parent)
                        && part.getBoundingBox().intersects(box)) {
                    out.add(parent);
                }
            }
        }
        return out;
    }

    public static List<AABB> hitBoxes(Entity target) {
        if (target.isMultipartEntity() && target.getParts() != null) {
            List<AABB> boxes = new ArrayList<>();
            for (PartEntity<?> part : target.getParts()) {
                boxes.add(part.getBoundingBox());
            }
            if (!boxes.isEmpty()) return boxes;
        }
        return List.of(target.getBoundingBox());
    }

    public static boolean withinRadius(Entity target, Vec3 center, double radius) {
        double r2 = radius * radius;
        for (AABB box : hitBoxes(target)) {
            double dx = Math.max(Math.max(box.minX - center.x, 0.0), center.x - box.maxX);
            double dy = Math.max(Math.max(box.minY - center.y, 0.0), center.y - box.maxY);
            double dz = Math.max(Math.max(box.minZ - center.z, 0.0), center.z - box.maxZ);
            if (dx * dx + dy * dy + dz * dz <= r2) return true;
        }
        return false;
    }
}
