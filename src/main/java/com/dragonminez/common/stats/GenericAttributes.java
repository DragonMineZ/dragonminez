package com.dragonminez.common.stats;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.mixin.common.RangedAttributeMixin;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/**
 * Syncs Minecraft {@link RangedAttribute} hard ceilings so they can hold values the
 * <b>server config already allows</b>.
 *
 * <p><b>Game balance max stats</b> live only in config
 * ({@code gameplay.maxValue} via {@link ConfigManager}) and in
 * {@link StatsData#getConfiguredMaxValue()} / {@link com.dragonminez.common.stats.character.Stats}
 * clamp logic. This class does <b>not</b> invent a second max-stats setting.
 *
 * <p>It only:
 * <ul>
 *   <li>Mirrors {@code maxValue} onto main-stat attributes (STR…ENE) so bases are not
 *       sanitized below what config allows (registry-time max may be a bootstrap only)</li>
 *   <li>Raises engine ceilings on vanilla/derived attributes (HP, max energy, damage, …)
 *       so formulas that scale with high stats are not clamped by vanilla 1024 HP etc.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class GenericAttributes {
	/**
	 * Technical ceiling for <em>derived</em> / vanilla attributes (not the config max-stat).
	 * Large enough for endgame pools/damage without using {@link Float#MAX_VALUE}.
	 * Not used for living max_health — see {@link #MAX_HEALTH_ENGINE_CEILING}.
	 */
	private static final double ENGINE_DERIVED_ATTRIBUTE_CEILING = 2_000_000_000.0D;

	/**
	 * Living max_health only. 1.20.1 (vanilla 1024 / AttributeFix ~2048) kept this low enough
	 * that multiplayer join worked. Raising it to 2e9 with VIT=999999999 freezes rejoin after
	 * terrain. 2^20 is well above AttributeFix defaults and safe for entity HP sync.
	 * Main stats STR…ENE still use full config {@code maxValue} (can be 1e9).
	 */
	public static final double MAX_HEALTH_ENGINE_CEILING = 1_048_576.0D;

	private static volatile boolean gameBusHooked;
	private static volatile boolean loggedOnce;

	@SubscribeEvent
	public static void onLoadComplete(FMLLoadCompleteEvent event) {
		ensureAttributeCeilings();
		hookGameBus();
	}

	private static void hookGameBus() {
		if (gameBusHooked) return;
		gameBusHooked = true;
		NeoForge.EVENT_BUS.addListener(GenericAttributes::onServerAboutToStart);
	}

	private static void onServerAboutToStart(ServerAboutToStartEvent event) {
		// Config is loaded; re-apply so main-stat max tracks gameplay.maxValue.
		ensureAttributeCeilings();
	}

	/**
	 * Apply config + engine ceilings. Safe to call after config reload and before
	 * large admin stat writes. Does not replace config — only opens attribute clamps.
	 */
	public static void ensureAttributeCeilings() {
		// --- Main stats: only what config says ---
		double configuredMax = readConfiguredMaxValue();
		if (configuredMax > 0.0) {
			raiseMaxIfNeeded(MainAttributes.STRENGTH, configuredMax);
			raiseMaxIfNeeded(MainAttributes.STRIKE_POWER, configuredMax);
			raiseMaxIfNeeded(MainAttributes.RESISTANCE, configuredMax);
			raiseMaxIfNeeded(MainAttributes.VITALITY, configuredMax);
			raiseMaxIfNeeded(MainAttributes.KI_POWER, configuredMax);
			raiseMaxIfNeeded(MainAttributes.ENERGY, configuredMax);
		}

		// --- Derived / vanilla: engine room only (not gameplay maxValue) ---
		raiseMaxIfNeeded(Attributes.ARMOR, ENGINE_DERIVED_ATTRIBUTE_CEILING);
		raiseMaxIfNeeded(Attributes.ARMOR_TOUGHNESS, ENGINE_DERIVED_ATTRIBUTE_CEILING);
		// Not ENGINE_DERIVED_ATTRIBUTE_CEILING — full 2e9 living HP wedges rejoin; see MAX_HEALTH_ENGINE_CEILING.
		raiseMaxIfNeeded(Attributes.MAX_HEALTH, MAX_HEALTH_ENGINE_CEILING);
		raiseMaxIfNeeded(Attributes.ATTACK_DAMAGE, ENGINE_DERIVED_ATTRIBUTE_CEILING);

		raiseMaxIfNeeded(MainAttributes.MAX_ENERGY, ENGINE_DERIVED_ATTRIBUTE_CEILING);
		raiseMaxIfNeeded(MainAttributes.MAX_STAMINA, ENGINE_DERIVED_ATTRIBUTE_CEILING);
		raiseMaxIfNeeded(MainAttributes.MAX_POISE, ENGINE_DERIVED_ATTRIBUTE_CEILING);
		raiseMaxIfNeeded(MainAttributes.MELEE_DAMAGE, ENGINE_DERIVED_ATTRIBUTE_CEILING);
		raiseMaxIfNeeded(MainAttributes.STRIKE_DAMAGE, ENGINE_DERIVED_ATTRIBUTE_CEILING);
		raiseMaxIfNeeded(MainAttributes.KI_DAMAGE, ENGINE_DERIVED_ATTRIBUTE_CEILING);
		raiseMaxIfNeeded(MainAttributes.DEFENSE, ENGINE_DERIVED_ATTRIBUTE_CEILING);

		raiseMaxIfNeeded(EntityAttributes.KI_BLAST_DAMAGE, ENGINE_DERIVED_ATTRIBUTE_CEILING);
		raiseMaxIfNeeded(EntityAttributes.FLY_SPEED, ENGINE_DERIVED_ATTRIBUTE_CEILING);
		raiseMaxIfNeeded(EntityAttributes.KI_BLAST_SPEED, ENGINE_DERIVED_ATTRIBUTE_CEILING);

		if (!loggedOnce) {
			loggedOnce = true;
			double mainMax = MainAttributes.VITALITY.value() instanceof RangedAttribute ra
					? ra.getMaxValue() : -1;
			double hpMax = Attributes.MAX_HEALTH.value() instanceof RangedAttribute ra
					? ra.getMaxValue() : -1;
			LogUtil.info(Env.COMMON,
					"Attribute ceilings synced: config maxValue={} mainStatAttrMax={} maxHealthAttrMax={}",
					configuredMax > 0 ? configuredMax : "(config not ready)",
					mainMax, hpMax);
		}
	}

	/** Same source of truth as {@link StatsData#getConfiguredMaxValue()} when config is live. */
	private static double readConfiguredMaxValue() {
		if (ConfigManager.getServerConfig() != null
				&& ConfigManager.getServerConfig().getGameplay() != null) {
			return Math.max(1.0, ConfigManager.getServerConfig().getGameplay().getMaxValue());
		}
		return -1.0; // not ready — skip main-stat sync this call
	}

	private static void raiseMaxIfNeeded(Holder<Attribute> attribute, double maxValue) {
		if (attribute == null || attribute.value() == null || maxValue <= 0.0) return;
		if (!(attribute.value() instanceof RangedAttribute rangedAttribute)) return;
		// Only raise — never shrink (config lower still enforced by Stats.clampStatValue).
		if (rangedAttribute.getMaxValue() >= maxValue) return;
		((RangedAttributeMixin) (Object) rangedAttribute).setMaxValue(maxValue);
	}
}
