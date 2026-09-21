package com.dragonminez.common.combat.util;

import com.dragonminez.Reference;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.SwordSlashS2C;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class SwordSlashManager {

	public static final float[] ROLL_SEQUENCE = {0.0F, 90.0F, 35.0F, -35.0F, 0.0F, 90.0F};

	public interface HitHandler {
		boolean hit(LivingEntity victim);
	}

	private static final class Cut {
		final ServerLevel level;
		final Entity owner;
		final Vec3 dir;
		final double speed;
		final double maxDistance;
		final double hitRadius;
		final HitHandler handler;
		final Set<Integer> alreadyHit = new HashSet<>();
		Vec3 pos;
		double traveled;

		private Cut(ServerLevel level, Entity owner, Vec3 pos, Vec3 dir, double speed, double maxDistance,
					double hitRadius, HitHandler handler) {
			this.level = level;
			this.owner = owner;
			this.pos = pos;
			this.dir = dir;
			this.speed = speed;
			this.maxDistance = maxDistance;
			this.hitRadius = hitRadius;
			this.handler = handler;
		}
	}

	private static final List<Cut> CUTS = new ArrayList<>();

	private SwordSlashManager() {}

	public static float rollFor(int index) {
		return ROLL_SEQUENCE[Math.floorMod(index, ROLL_SEQUENCE.length)];
	}

	public static void launch(ServerLevel level, Entity owner, Vec3 origin, Vec3 direction, float roll, float radius,
							  int color, double speed, double maxDistance, double hitRadius, HitHandler handler) {
		if (direction.lengthSqr() < 1.0E-6) return;
		Vec3 dir = direction.normalize();
		int lifetime = (int) Math.ceil(maxDistance / speed);

		SwordSlashS2C packet = new SwordSlashS2C(origin.x, origin.y, origin.z,
				(float) dir.x, (float) dir.y, (float) dir.z, (float) speed, roll, radius, color, lifetime);
		if (owner instanceof ServerPlayer) NetworkHandler.sendToTrackingEntityAndSelf(packet, owner);
		else NetworkHandler.sendToTrackingEntity(packet, owner);

		CUTS.add(new Cut(level, owner, origin, dir, speed, maxDistance, hitRadius, handler));
	}

	@SubscribeEvent
	public static void onLevelTick(TickEvent.LevelTickEvent event) {
		if (event.phase != TickEvent.Phase.END || CUTS.isEmpty()) return;
		if (!(event.level instanceof ServerLevel level)) return;

		Iterator<Cut> it = CUTS.iterator();
		while (it.hasNext()) {
			Cut cut = it.next();
			if (cut.level != level) continue;

			if (cut.owner == null || cut.owner.isRemoved()) {
				it.remove();
				continue;
			}

			cut.pos = cut.pos.add(cut.dir.scale(cut.speed));
			cut.traveled += cut.speed;

			AABB box = new AABB(cut.pos, cut.pos).inflate(cut.hitRadius);
			for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box)) {
				if (victim == cut.owner || !victim.isAlive()) continue;
				if (cut.alreadyHit.contains(victim.getId())) continue;

				Vec3 center = victim.getBoundingBox().getCenter();
				double reach = cut.hitRadius + victim.getBbWidth() * 0.5D;
				if (center.distanceToSqr(cut.pos) > reach * reach) continue;

				if (cut.handler.hit(victim)) cut.alreadyHit.add(victim.getId());
			}

			if (cut.traveled >= cut.maxDistance) it.remove();
		}
	}
}
