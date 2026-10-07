package com.dragonminez.common.combat.collision;

import com.dragonminez.common.combat.weapon.WeaponAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class MeleeHitbox {
	private MeleeHitbox() {}

	public static boolean isSpinAttack(WeaponAttributes.Attack attack) {
		return attack.angle() > 180;
	}

	public static Vec3 origin(Entity attacker, float pitch, float yaw) {
		Vec3 look = Vec3.directionFromRotation(pitch, yaw);
		Vec3 horizontalLook = new Vec3(look.x, 0.0, look.z);
		if (horizontalLook.lengthSqr() < 1.0E-6) horizontalLook = look;
		horizontalLook = horizontalLook.normalize();

		return attacker.position()
				.add(0, attacker.getBbHeight() * 0.8, 0)
				.add(horizontalLook.scale(attacker.getBbWidth() * 0.5));
	}

	public static OrientedBoundingBox build(Entity attacker, Vec3 origin, WeaponAttributes.Attack attack, double attackRange, float pitch, float yaw) {
		boolean spin = isSpinAttack(attack);
		Vec3 size = WeaponHitBoxes.createHitbox(attack.hitbox(), attackRange, spin);
		OrientedBoundingBox obb = new OrientedBoundingBox(origin, size, pitch, yaw);
		if (!spin) obb.offsetAlongAxisZ(obb.extent.z - attacker.getBbWidth() * 0.3);
		return obb.updateVertex();
	}
}
