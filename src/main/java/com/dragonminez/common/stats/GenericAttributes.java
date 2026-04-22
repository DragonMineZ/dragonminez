package com.dragonminez.common.stats;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.mixin.common.RangedAttributeMixin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class GenericAttributes {

    @SubscribeEvent
    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        Attribute armorAttribute = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.parse("minecraft:generic.armor"));
        Attribute armorToughnessAttribute = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.parse("minecraft:generic.armor_toughness"));
        Attribute maxHealth = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.parse("minecraft:generic.max_health"));

        if (armorAttribute instanceof RangedAttribute rangedAttribute) {
            ((RangedAttributeMixin) rangedAttribute).setMaxValue(Float.MAX_VALUE);
        }

        if (armorToughnessAttribute instanceof RangedAttribute rangedAttribute) {
            ((RangedAttributeMixin) rangedAttribute).setMaxValue(Float.MAX_VALUE);
        }

        if (maxHealth instanceof RangedAttribute rangedAttribute) {
            ((RangedAttributeMixin) rangedAttribute).setMaxValue(Float.MAX_VALUE);
        }

        double mainStatMax = getConfiguredMainStatMax();
        setMaxIfRanged(MainAttributes.STRENGTH.get(), mainStatMax);
        setMaxIfRanged(MainAttributes.STRIKE_POWER.get(), mainStatMax);
        setMaxIfRanged(MainAttributes.RESISTANCE.get(), mainStatMax);
        setMaxIfRanged(MainAttributes.VITALITY.get(), mainStatMax);
        setMaxIfRanged(MainAttributes.KI_POWER.get(), mainStatMax);
        setMaxIfRanged(MainAttributes.ENERGY.get(), mainStatMax);

        setMaxIfRanged(MainAttributes.MAX_ENERGY.get(), Float.MAX_VALUE);
        setMaxIfRanged(MainAttributes.MAX_STAMINA.get(), Float.MAX_VALUE);
        setMaxIfRanged(MainAttributes.MAX_POISE.get(), Float.MAX_VALUE);
        setMaxIfRanged(MainAttributes.MELEE_DAMAGE.get(), Float.MAX_VALUE);
        setMaxIfRanged(MainAttributes.STRIKE_DAMAGE.get(), Float.MAX_VALUE);
        setMaxIfRanged(MainAttributes.KI_DAMAGE.get(), Float.MAX_VALUE);
        setMaxIfRanged(MainAttributes.DEFENSE.get(), Float.MAX_VALUE);

        setMaxIfRanged(EntityAttributes.KI_BLAST_DAMAGE.get(), Float.MAX_VALUE);
        setMaxIfRanged(EntityAttributes.FLY_SPEED.get(), Float.MAX_VALUE);
        setMaxIfRanged(EntityAttributes.KI_BLAST_SPEED.get(), Float.MAX_VALUE);
    }

    private static double getConfiguredMainStatMax() {
        if (ConfigManager.getServerConfig() != null && ConfigManager.getServerConfig().getGameplay() != null) {
            return ConfigManager.getServerConfig().getGameplay().getMaxValue();
        }
        return 10000.0;
    }

    private static void setMaxIfRanged(Attribute attribute, double maxValue) {
        if (attribute instanceof RangedAttribute rangedAttribute) {
            ((RangedAttributeMixin) rangedAttribute).setMaxValue(maxValue);
        }
    }
}
