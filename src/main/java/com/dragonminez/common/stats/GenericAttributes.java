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
 * Raises vanilla / mod attribute hard caps so DMZ vitality and combat stats can exceed
 * Minecraft's default {@code generic.max_health} ceiling (1024, often 2048 with AttributeFix).
 *
 * <p>Other mods (notably AttributeFix) may rewrite the same fields after our load-complete
 * pass, so {@link #ensureAttributeCeilings()} is re-run on server start and whenever health
 * bonuses are applied.
 */
@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class GenericAttributes {
	/**
	 * Finite ceiling high enough for endgame vitality without using {@link Float#MAX_VALUE}
	 * (which can break float health sync / AttributeFix configs).
	 */
	public static final double COMBAT_ATTRIBUTE_MAX = 2_000_000_000.0D;

	private static volatile boolean gameBusHooked;
	private static volatile boolean loggedHealthCeiling;

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
		// Re-apply after AttributeFix / datapack attribute rewrites during load.
		ensureAttributeCeilings();
	}

	/** Idempotent: safe to call from login, tick, and stat updates. */
	public static void ensureAttributeCeilings() {
		setMaxIfRanged(Attributes.ARMOR, COMBAT_ATTRIBUTE_MAX);
		setMaxIfRanged(Attributes.ARMOR_TOUGHNESS, COMBAT_ATTRIBUTE_MAX);
		setMaxIfRanged(Attributes.MAX_HEALTH, COMBAT_ATTRIBUTE_MAX);
		setMaxIfRanged(Attributes.ATTACK_DAMAGE, COMBAT_ATTRIBUTE_MAX);

		double mainStatMax = getConfiguredMainStatMax();
		setMaxIfRanged(MainAttributes.STRENGTH, mainStatMax);
		setMaxIfRanged(MainAttributes.STRIKE_POWER, mainStatMax);
		setMaxIfRanged(MainAttributes.RESISTANCE, mainStatMax);
		setMaxIfRanged(MainAttributes.VITALITY, mainStatMax);
		setMaxIfRanged(MainAttributes.KI_POWER, mainStatMax);
		setMaxIfRanged(MainAttributes.ENERGY, mainStatMax);

		setMaxIfRanged(MainAttributes.MAX_ENERGY, COMBAT_ATTRIBUTE_MAX);
		setMaxIfRanged(MainAttributes.MAX_STAMINA, COMBAT_ATTRIBUTE_MAX);
		setMaxIfRanged(MainAttributes.MAX_POISE, COMBAT_ATTRIBUTE_MAX);
		setMaxIfRanged(MainAttributes.MELEE_DAMAGE, COMBAT_ATTRIBUTE_MAX);
		setMaxIfRanged(MainAttributes.STRIKE_DAMAGE, COMBAT_ATTRIBUTE_MAX);
		setMaxIfRanged(MainAttributes.KI_DAMAGE, COMBAT_ATTRIBUTE_MAX);
		setMaxIfRanged(MainAttributes.DEFENSE, COMBAT_ATTRIBUTE_MAX);

		setMaxIfRanged(EntityAttributes.KI_BLAST_DAMAGE, COMBAT_ATTRIBUTE_MAX);
		setMaxIfRanged(EntityAttributes.FLY_SPEED, COMBAT_ATTRIBUTE_MAX);
		setMaxIfRanged(EntityAttributes.KI_BLAST_SPEED, COMBAT_ATTRIBUTE_MAX);

		if (!loggedHealthCeiling && Attributes.MAX_HEALTH.value() instanceof RangedAttribute health) {
			loggedHealthCeiling = true;
			LogUtil.info(Env.COMMON, "MAX_HEALTH attribute ceiling is now {}", health.getMaxValue());
		}
	}

	private static double getConfiguredMainStatMax() {
		if (ConfigManager.getServerConfig() != null && ConfigManager.getServerConfig().getGameplay() != null) {
			return Math.max(1.0, ConfigManager.getServerConfig().getGameplay().getMaxValue());
		}
		return 10000.0;
	}

	private static void setMaxIfRanged(Holder<Attribute> attribute, double maxValue) {
		if (attribute == null || attribute.value() == null) return;
		if (!(attribute.value() instanceof RangedAttribute rangedAttribute)) return;
		// Only raise — never shrink another mod's higher ceiling.
		if (rangedAttribute.getMaxValue() >= maxValue) return;
		((RangedAttributeMixin) (Object) rangedAttribute).setMaxValue(maxValue);
	}
}
