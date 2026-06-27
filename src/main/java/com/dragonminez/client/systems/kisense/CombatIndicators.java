package com.dragonminez.client.systems.kisense;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class CombatIndicators {

	private static final float EPSILON = 0.05f;
	public static final long POPUP_LIFETIME = 30L;
	private static final long ACCUM_RESET = 40L;
	private static final Random RAND = new Random();

	public record DamagePopup(float value, boolean heal, long bornTick, float offX, float offY) {}

	private static final class Track {
		boolean initialized;
		float lastHp;
		float accumDamage;
		float accumHeal;
		long damageTick;
		long healTick;
		float damageOffX, damageOffY;
		float healOffX, healOffY;
		final List<DamagePopup> popups = new ArrayList<>();
	}

	private static final Map<Integer, Track> TRACKS = new HashMap<>();

	private CombatIndicators() {}

	public static void clear() {
		TRACKS.clear();
	}

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			TRACKS.clear();
			return;
		}

		long now = mc.level.getGameTime();
		var combat = KiSenseScan.getCombatEntities();

		for (int id : combat) {
			Entity entity = mc.level.getEntity(id);
			if (!(entity instanceof LivingEntity living)) continue;

			Track track = TRACKS.computeIfAbsent(id, k -> new Track());
			float hp = living.getHealth();

			if (track.initialized) {
				float delta = hp - track.lastHp;
				if (delta < -EPSILON) registerChange(track, -delta, false, now);
				else if (delta > EPSILON) registerChange(track, delta, true, now);
			}
			track.lastHp = hp;
			track.initialized = true;
		}

		Iterator<Map.Entry<Integer, Track>> it = TRACKS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, Track> entry = it.next();
			Track track = entry.getValue();

			if (now - track.damageTick > ACCUM_RESET) track.accumDamage = 0;
			if (now - track.healTick > ACCUM_RESET) track.accumHeal = 0;
			track.popups.removeIf(p -> now - p.bornTick() > POPUP_LIFETIME);

			boolean stale = !combat.contains(entry.getKey());
			boolean empty = track.accumDamage <= 0 && track.accumHeal <= 0 && track.popups.isEmpty();
			if (stale && empty) it.remove();
		}
	}

	private static void registerChange(Track track, float amount, boolean heal, long now) {
		if (heal) {
			if (track.accumHeal <= 0) {
				track.healOffX = randX();
				track.healOffY = randY();
			}
			track.accumHeal += amount;
			track.healTick = now;
		} else {
			if (track.accumDamage <= 0) {
				track.damageOffX = randX();
				track.damageOffY = randY();
			}
			track.accumDamage += amount;
			track.damageTick = now;
		}
		track.popups.add(new DamagePopup(amount, heal, now, randX(), randY()));
	}

	private static float randX() {
		return (RAND.nextFloat() - 0.5f) * 26f;
	}

	private static float randY() {
		return -14f - RAND.nextFloat() * 12f;
	}

	public static float getAccumDamage(int id) {
		Track t = TRACKS.get(id);
		return t != null ? t.accumDamage : 0;
	}

	public static float getAccumHeal(int id) {
		Track t = TRACKS.get(id);
		return t != null ? t.accumHeal : 0;
	}

	public static long getDamageTick(int id) {
		Track t = TRACKS.get(id);
		return t != null ? t.damageTick : 0;
	}

	public static long getHealTick(int id) {
		Track t = TRACKS.get(id);
		return t != null ? t.healTick : 0;
	}

	public static float getDamageOffX(int id) {
		Track t = TRACKS.get(id);
		return t != null ? t.damageOffX : 0;
	}

	public static float getDamageOffY(int id) {
		Track t = TRACKS.get(id);
		return t != null ? t.damageOffY : 0;
	}

	public static float getHealOffX(int id) {
		Track t = TRACKS.get(id);
		return t != null ? t.healOffX : 0;
	}

	public static float getHealOffY(int id) {
		Track t = TRACKS.get(id);
		return t != null ? t.healOffY : 0;
	}

	public static List<DamagePopup> getPopups(int id) {
		Track t = TRACKS.get(id);
		return t != null ? t.popups : List.of();
	}
}
