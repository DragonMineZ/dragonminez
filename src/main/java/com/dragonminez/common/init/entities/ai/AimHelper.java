package com.dragonminez.common.init.entities.ai;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class AimHelper {

    private static final double MAX_LEAD_TICKS = 25.0D;

    private AimHelper() {}

    public static Vec3 aimPoint(LivingEntity self, LivingEntity target, Vec3 targetVelocity, AiProfile profile,
                                double projectileBlocksPerTick, RandomSource random) {
        Vec3 point = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        if (profile.leadAim && profile.aimLeadFactor > 0.0F && targetVelocity != null) {
            double dist = self.distanceTo(target);
            double speed = Math.max(0.3D, projectileBlocksPerTick);
            double lead = Mth.clamp(dist / speed, 0.0D, MAX_LEAD_TICKS) * profile.aimLeadFactor;
            point = point.add(targetVelocity.scale(lead));
        }
        if (profile.aimErrorDegrees > 0.0F) {
            Vec3 eye = self.getEyePosition();
            Vec3 rel = point.subtract(eye);
            double len = rel.length();
            if (len > 1.0E-3D) {
                double yawErr = Math.toRadians((random.nextDouble() * 2.0D - 1.0D) * profile.aimErrorDegrees);
                double pitchErr = Math.toRadians((random.nextDouble() * 2.0D - 1.0D) * profile.aimErrorDegrees * 0.5D);
                double yaw = Math.atan2(rel.z, rel.x) + yawErr;
                double horiz = Math.sqrt(rel.x * rel.x + rel.z * rel.z);
                double pitch = Math.atan2(rel.y, horiz) + pitchErr;
                double cosP = Math.cos(pitch);
                point = eye.add(Math.cos(yaw) * cosP * len, Math.sin(pitch) * len, Math.sin(yaw) * cosP * len);
            }
        }
        return point;
    }

    public static void face(Mob self, Vec3 point) {
        Vec3 eye = self.getEyePosition();
        double dx = point.x - eye.x;
        double dy = point.y - eye.y;
        double dz = point.z - eye.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float pitch = (float) -(Mth.atan2(dy, horiz) * (180.0D / Math.PI));
        self.setYRot(yaw);
        self.setYHeadRot(yaw);
        self.setYBodyRot(yaw);
        self.setXRot(Mth.clamp(pitch, -89.0F, 89.0F));
        self.yRotO = yaw;
        self.xRotO = self.getXRot();
    }
}
