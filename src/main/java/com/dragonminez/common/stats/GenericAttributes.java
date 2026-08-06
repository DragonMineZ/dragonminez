package com.dragonminez.common.stats;

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

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class GenericAttributes {

	@SubscribeEvent
	public static void onLoadComplete(FMLLoadCompleteEvent event) {
		setMaxIfRanged(Attributes.ARMOR, Float.MAX_VALUE);
		setMaxIfRanged(Attributes.ARMOR_TOUGHNESS, Float.MAX_VALUE);
		setMaxIfRanged(Attributes.MAX_HEALTH, Float.MAX_VALUE);
		setMaxIfRanged(Attributes.ATTACK_DAMAGE, Float.MAX_VALUE);

		double mainStatMax = getConfiguredMainStatMax();
		setMaxIfRanged(MainAttributes.STRENGTH, mainStatMax);
		setMaxIfRanged(MainAttributes.STRIKE_POWER, mainStatMax);
		setMaxIfRanged(MainAttributes.RESISTANCE, mainStatMax);
		setMaxIfRanged(MainAttributes.VITALITY, mainStatMax);
		setMaxIfRanged(MainAttributes.KI_POWER, mainStatMax);
		setMaxIfRanged(MainAttributes.ENERGY, mainStatMax);

		setMaxIfRanged(MainAttributes.MAX_ENERGY, Float.MAX_VALUE);
		setMaxIfRanged(MainAttributes.MAX_STAMINA, Float.MAX_VALUE);
		setMaxIfRanged(MainAttributes.MAX_POISE, Float.MAX_VALUE);
		setMaxIfRanged(MainAttributes.MELEE_DAMAGE, Float.MAX_VALUE);
		setMaxIfRanged(MainAttributes.STRIKE_DAMAGE, Float.MAX_VALUE);
		setMaxIfRanged(MainAttributes.KI_DAMAGE, Float.MAX_VALUE);
		setMaxIfRanged(MainAttributes.DEFENSE, Float.MAX_VALUE);

		setMaxIfRanged(EntityAttributes.KI_BLAST_DAMAGE, Float.MAX_VALUE);
		setMaxIfRanged(EntityAttributes.FLY_SPEED, Float.MAX_VALUE);
		setMaxIfRanged(EntityAttributes.KI_BLAST_SPEED, Float.MAX_VALUE);
	}

	private static double getConfiguredMainStatMax() {
		if (ConfigManager.getServerConfig() != null && ConfigManager.getServerConfig().getGameplay() != null) {
			return ConfigManager.getServerConfig().getGameplay().getMaxValue();
		}
		return 10000.0;
	}

	private static void setMaxIfRanged(Holder<Attribute> attribute, double maxValue) {
		if (attribute.value() instanceof RangedAttribute rangedAttribute) {
			((RangedAttributeMixin) (Object) rangedAttribute).setMaxValue(maxValue);
		}
	}
}
