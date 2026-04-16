package com.dragonminez.common.combat.logic.weapon;

import com.dragonminez.common.combat.util.PatternMatching;
import com.dragonminez.common.config.CombatConfig.CompatibilitySpecifier;
import com.dragonminez.common.config.ConfigManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class WeaponAttributesFallback {
    public static void initialize() {
        var config = ConfigManager.getCombatConfig();
        for (var itemId : ForgeRegistries.ITEMS.getKeys()) {
            var item = ForgeRegistries.ITEMS.getValue(itemId);
            if (PatternMatching.matches(itemId.toString(), config.getBlacklistItemIdRegex())) continue;

            List<CompatibilitySpecifier> specifiers = null;
            if (hasAttributeModifier(item, Attributes.ATTACK_DAMAGE)) specifiers = config.getFallbackCompatibility();
            if (specifiers == null) continue;

            for (var fallbackOption : specifiers) {
                if (WeaponRegistry.getAttributes(itemId) == null && PatternMatching.matches(itemId.toString(), fallbackOption.getItem_id_regex())) {
                    var container = WeaponRegistry.containers.get(ResourceLocation.parse(fallbackOption.getWeapon_attributes()));
                    if (container != null) {
                        WeaponRegistry.resolveAndRegisterAttributes(itemId, container);
                        break;
                    }
                }
            }
        }
    }

    private static boolean hasAttributeModifier(Item item, Attribute searchedAttribute) {
        if (item == null) return false;
        var searchedAttributeId = ForgeRegistries.ATTRIBUTES.getKey(searchedAttribute);
        var attributes = item.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND);

        for (var entry : attributes.entries()) {
            var attribute = entry.getKey();
            var attributeId = ForgeRegistries.ATTRIBUTES.getKey(attribute);
            if (attributeId != null && attributeId.equals(searchedAttributeId)) return true;
        }
        return false;
    }
}