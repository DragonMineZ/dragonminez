package com.dragonminez.common.stats;

import com.dragonminez.Reference;
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
		Attribute armorAttribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("minecraft:generic.armor"));
		Attribute armorToughnessAttribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("minecraft:generic.armor_toughness"));
		Attribute maxHealth = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("minecraft:generic.max_health"));
		Attribute dmzHealth = MainAttributes.DMZ_HEALTH.get();

		if (armorAttribute instanceof RangedAttribute) {
			RangedAttributeMixin accessor = (RangedAttributeMixin) armorAttribute;
			accessor.setMaxValue(1000000.0);
		}

		if (armorToughnessAttribute instanceof RangedAttribute) {
			RangedAttributeMixin accessor = (RangedAttributeMixin) armorToughnessAttribute;
			accessor.setMaxValue(1000000.0);
		}

		if (maxHealth instanceof RangedAttribute) {
			RangedAttributeMixin accessor = (RangedAttributeMixin) maxHealth;
			accessor.setMaxValue(2000000000.0);
		}

		if (dmzHealth instanceof RangedAttribute) {
			RangedAttributeMixin accessor = (RangedAttributeMixin) dmzHealth;
			accessor.setMaxValue(2000000000.0);
		}
	}
}
