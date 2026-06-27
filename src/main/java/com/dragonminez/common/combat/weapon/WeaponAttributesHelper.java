package com.dragonminez.common.combat.weapon;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import net.minecraft.world.item.ItemStack;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class WeaponAttributesHelper {

    public static WeaponAttributes override(WeaponAttributes a, WeaponAttributes b) {
        var attackRange = b.attackRange() > 0 ? b.attackRange() : a.attackRange();
        var pose = b.pose() != null ? b.pose() : a.pose();
        var off_hand_pose = b.offHandPose() != null ? b.offHandPose() : a.offHandPose();
        var isTwoHanded = b.two_handed() != null ? b.two_handed() : a.two_handed();
        var category = b.category() != null ? b.category() : a.category();

        Double critChance = b.crit_chance() != null ? b.crit_chance() : a.crit_chance();
        Double critDamage = b.crit_damage() != null ? b.crit_damage() : a.crit_damage();

        var attacks = a.attacks();
        if (b.attacks() != null && b.attacks().length > 0) {
            var overrideAttacks = new ArrayList<WeaponAttributes.Attack>();
            for (int i = 0; i < b.attacks().length; ++i) {
                var base = (a.attacks() != null && a.attacks().length > i)
                        ? a.attacks()[i]
                        : new WeaponAttributes.Attack(null, null, 0, 0, 0, null, null, null);
                var override = b.attacks()[i];
                var attack = new WeaponAttributes.Attack(
                        override.conditions() != null ? override.conditions() : base.conditions(),
                        override.hitbox() != null ? override.hitbox() : base.hitbox(),
                        override.damageMultiplier() != 0 ? override.damageMultiplier() : base.damageMultiplier(),
                        override.angle() != 0 ? override.angle() : base.angle(),
                        override.upswing() != 0 ? override.upswing() : base.upswing(),
                        override.animation() != null ? override.animation() : base.animation(),
                        override.swingSound() != null ? override.swingSound() : base.swingSound(),
                        override.impactSound() != null ? override.impactSound() : base.impactSound());
                overrideAttacks.add(attack);
            }
            attacks = overrideAttacks.toArray(new WeaponAttributes.Attack[0]);
        }

        return new WeaponAttributes(attackRange, pose, off_hand_pose, isTwoHanded, category, attacks, critChance, critDamage);
    }

    private static final String nbtTag = "dragonminez_weapon_attributes";

    public static AttributesContainer getContainerFromNBT(ItemStack itemStack) {
        var tag = itemStack.getTag();
        if (!itemStack.isEmpty() && tag != null && tag.contains(nbtTag)) {
            var json = tag.getString(nbtTag);
            if (!json.isBlank()) {
                var gson = new Gson();
                return gson.fromJson(json, attributesContainerFileFormat);
            }
        }
        return null;
    }

    private static final Type attributesContainerFileFormat = new TypeToken<AttributesContainer>() {}.getType();

    public static AttributesContainer decode(Reader reader) {
        var gson = new Gson();
        AttributesContainer container = gson.fromJson(reader, attributesContainerFileFormat);
        return normalizeContainer(container);
    }

    public static AttributesContainer decode(JsonReader json) {
        var gson = new Gson();
        AttributesContainer container = gson.fromJson(json, attributesContainerFileFormat);
        return normalizeContainer(container);
    }

    public static String encode(AttributesContainer container) {
        var gson = new Gson();
        return gson.toJson(container);
    }

    private static AttributesContainer normalizeContainer(AttributesContainer container) {
        if (container == null || container.attributes() == null) return container;

        var attributes = container.attributes();
        var pose = sanitizeAnimationId(attributes.pose());
        var offHandPose = sanitizeAnimationId(attributes.offHandPose());

        WeaponAttributes.Attack[] attacks = null;
        if (attributes.attacks() != null) {
            attacks = new WeaponAttributes.Attack[attributes.attacks().length];
            for (int i = 0; i < attributes.attacks().length; i++) {
                var attack = attributes.attacks()[i];
                if (attack == null) {
                    attacks[i] = null;
                    continue;
                }
                attacks[i] = new WeaponAttributes.Attack(
                        attack.conditions(),
                        attack.hitbox(),
                        attack.damageMultiplier(),
                        attack.angle(),
                        attack.upswing(),
                        sanitizeAnimationId(attack.animation()),
                        attack.swingSound(),
                        attack.impactSound()
                );
            }
        }

        var normalized = new WeaponAttributes(
                attributes.attackRange(),
                pose.isBlank() ? null : pose,
                offHandPose.isBlank() ? null : offHandPose,
                attributes.two_handed(),
                attributes.category(),
                attacks,
                attributes.crit_chance(),
                attributes.crit_damage()
        );
        return new AttributesContainer(container.parent(), normalized);
    }

    private static String sanitizeAnimationId(String animationId) {
        if (animationId == null) {
            return "";
        }
        String id = animationId.trim();
        if (id.startsWith("dragonminez:")) {
            id = id.substring("dragonminez:".length());
        }
        if (id.startsWith("animation.base.")) {
            id = id.substring("animation.base.".length());
        }
        if (id.startsWith("combat.")) {
            id = id.substring("combat.".length());
        }
        return id;
    }
}