package com.dragonminez.common.combat.logic.weapon;

import com.dragonminez.common.combat.util.PatternMatching;
import com.dragonminez.common.config.CombatConfig.CompatibilitySpecifier;
import com.dragonminez.common.config.ConfigManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;

public class WeaponAttributesFallback {
    public static void initialize() {
        var config = ConfigManager.getCombatConfig();
        for (var itemId : BuiltInRegistries.ITEM.keySet()) {
            var item = BuiltInRegistries.ITEM.get(itemId);
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

    private static boolean hasAttributeModifier(Item item, Holder<Attribute> searchedAttribute) {
        if (item == null) return false;
        var stack = item.getDefaultInstance();
        final boolean[] found = {false};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attr, mod) -> {
            if (attr.equals(searchedAttribute)) found[0] = true;
        });
        return found[0];
    }
}