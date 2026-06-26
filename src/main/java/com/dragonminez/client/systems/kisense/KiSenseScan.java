package com.dragonminez.client.systems.kisense;

import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.TransformationsHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class KiSenseScan {

	private static final int HEAVY_SCAN_INTERVAL = 100;
	private static final double TRANSFORM_ALERT_FACTOR = 1.15;

	private static final Set<Integer> COMBAT_ENTITIES = new HashSet<>();
	private static final Set<Integer> SEARCH_ENTITIES = new HashSet<>();
	private static final Map<Integer, Float> SENSED_BP = new HashMap<>();
	private static final Map<Integer, Float> CACHED_BP = new HashMap<>();

	private static int scanTickCounter = 0;
	private static float cachedMyBP = 0f;
	private static boolean firstScanDone = false;
	private static boolean suppressAlerts = false;

	private KiSenseScan() {}

	public static Set<Integer> getCombatEntities() {
		return COMBAT_ENTITIES;
	}

	public static Set<Integer> getSearchEntities() {
		return SEARCH_ENTITIES;
	}

	public static float getMyBP() {
		return cachedMyBP;
	}

	public static float getCachedBP(int entityId) {
		return CACHED_BP.getOrDefault(entityId, 0f);
	}

	public static void forceRescan() {
		scanTickCounter = 0;
		firstScanDone = false;
		suppressAlerts = true;
	}

	public static boolean canTarget(LivingEntity target, StatsData myData) {
		if (!(target instanceof Player targetPlayer)) return true;
		StatsData targetData = StatsProvider.get(StatsCapability.INSTANCE, targetPlayer).orElse(null);
		if (targetData == null) return true;
		if (targetData.getStatus().isAndroidUpgraded()) return false;
		if (TransformationsHelper.hasGodFormActive(targetData) && myData.getSkills().getSkillLevel("godforms") <= 0) return false;
		return true;
	}

	public static double combatRange(StatsData data, int level) {
		double range = 10.0 + 3.0 * level;
		if (data.getStatus().isAndroidUpgraded()) range += 10.0;
		return range;
	}

	public static double searchRange(StatsData data, int level) {
		double range = 30.0 + 20.0 * level;
		if (data.getStatus().isAndroidUpgraded()) range += 10.0;
		return range;
	}

	public static void clear() {
		COMBAT_ENTITIES.clear();
		SEARCH_ENTITIES.clear();
		SENSED_BP.clear();
		CACHED_BP.clear();
		scanTickCounter = 0;
		firstScanDone = false;
		suppressAlerts = false;
	}

	public static void tick(Player player, StatsData data, KiSenseState.Mode mode) {
		if (mode == KiSenseState.Mode.NONE) {
			clear();
			return;
		}

		int level = data.getSkills().getSkillLevel("kisense");
		if (level <= 0) {
			clear();
			return;
		}

		if (firstScanDone && scanTickCounter++ < HEAVY_SCAN_INTERVAL) return;
		scanTickCounter = 0;
		firstScanDone = true;

		heavyScan(player, data, mode, level);
	}

	private static void heavyScan(Player player, StatsData data, KiSenseState.Mode mode, int level) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;

		cachedMyBP = data.getBattlePower();
		boolean android = data.getStatus().isAndroidUpgraded();

		double combatRange = combatRange(data, level);
		double searchRange = searchRange(data, level);

		COMBAT_ENTITIES.clear();
		SEARCH_ENTITIES.clear();

		AABB box = player.getBoundingBox().inflate(searchRange);
		Set<Integer> currentSensed = new HashSet<>();
		CACHED_BP.clear();
		boolean alertCandidate = false;

		for (LivingEntity entity : mc.level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
			boolean visible = !entity.isInvisible() || !entity.isInvisibleTo(player);
			if (!visible) continue;
			if (!canTarget(entity, data)) continue;

			double dist = player.distanceTo(entity);
			float bp = getEntityBP(entity);
			CACHED_BP.put(entity.getId(), bp);

			if (dist <= searchRange) {
				currentSensed.add(entity.getId());
				if (shouldAlert(player, entity, bp)) alertCandidate = true;
				SENSED_BP.put(entity.getId(), bp);
			}

			if (mode == KiSenseState.Mode.SEARCH && dist <= searchRange) {
				SEARCH_ENTITIES.add(entity.getId());
			}

			if (mode == KiSenseState.Mode.COMBAT && dist <= combatRange) {
				if (player.hasLineOfSight(entity) || android) COMBAT_ENTITIES.add(entity.getId());
			}
		}

		SENSED_BP.keySet().retainAll(currentSensed);

		if (alertCandidate && !suppressAlerts) player.playSound(MainSounds.LOCKON.get());
		suppressAlerts = false;
	}

	private static boolean shouldAlert(Player player, LivingEntity entity, float bp) {
		Float old = SENSED_BP.get(entity.getId());
		boolean threatening = bp > cachedMyBP;
		boolean entered = old == null;
		boolean surged = old != null && bp > old * TRANSFORM_ALERT_FACTOR;
		return threatening && (entered || surged) && !isInView(player, entity);
	}

	private static boolean isInView(Player player, LivingEntity entity) {
		Vec3 look = player.getViewVector(1.0F).normalize();
		Vec3 toEntity = entity.position().add(0, entity.getBbHeight() * 0.5, 0).subtract(player.getEyePosition()).normalize();
		boolean inFrustum = look.dot(toEntity) > 0.6;
		return inFrustum && player.hasLineOfSight(entity);
	}

	public static float getEntityBP(LivingEntity entity) {
		try {
			if (entity instanceof Player player) {
				if (isCloaked(player)) return 0f;
				return StatsProvider.get(StatsCapability.INSTANCE, player).map(StatsData::getBattlePower).orElse(0f);
			}
			if (entity instanceof IBattlePower bpEntity) {
				float entityBp = (float) bpEntity.getBattlePower();
				return Math.min(entityBp, Integer.MAX_VALUE);
			}
		} catch (Exception ignored) {}
		return 0f;
	}

	private static boolean isCloaked(Player target) {
		return CuriosApi.getCuriosInventory(target)
				.map(inv -> inv.getCurios().get("head_tech"))
				.map(handler -> handler.getStacks().getStackInSlot(0))
				.map(stack -> stack.getItem() == MainItems.ANTI_KI_CLOAK.get())
				.orElse(false);
	}

	public static ItemStack scouterStack(Player player) {
		return CuriosApi.getCuriosInventory(player)
				.map(inv -> inv.getCurios().get("head_tech"))
				.map(handler -> handler.getStacks().getStackInSlot(0))
				.orElse(ItemStack.EMPTY);
	}
}
