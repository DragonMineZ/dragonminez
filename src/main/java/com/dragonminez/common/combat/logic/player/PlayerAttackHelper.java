package com.dragonminez.common.combat.logic.player;

import com.dragonminez.common.combat.logic.weapon.WeaponRegistry;
import com.dragonminez.common.combat.weapon.WeaponAttributes;
import com.dragonminez.common.combat.player.AttackHand;
import com.dragonminez.common.combat.player.ComboState;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.skills.Skills;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static net.minecraft.world.entity.EquipmentSlot.MAINHAND;

public class PlayerAttackHelper {

    public static float getDualWieldingAttackDamageMultiplier(Player player, AttackHand hand) {
        return isDualWielding(player) ? (hand.isOffHand() ? 0.9f : 1.0f) : 1.0f;
    }

    public static boolean shouldAttackWithOffHand(Player player, int comboCount) {
        return isDualWielding(player) && comboCount % 2 == 1;
    }

    public static boolean isDualWielding(Player player) {
        if (isKiWeaponActive(player)) {
            var mainAttributes = getKiWeaponAttributes(player);
            if (mainAttributes == null || mainAttributes.isTwoHanded()) return false;

            var offStack = player.getOffhandItem();
            if (offStack.isEmpty() || offStack.getItem() instanceof ShieldItem) return false;

            var offAttributes = WeaponRegistry.getAttributes(offStack);
            if (offAttributes == null || offAttributes.isTwoHanded()) return false;

            String mainCategory = mainAttributes.category();
            String offCategory = offAttributes.category();
            return mainCategory != null && !mainCategory.isEmpty() && mainCategory.equals(offCategory);
        }

        boolean mainEmpty = player.getMainHandItem().isEmpty();
        boolean offEmpty = player.getOffhandItem().isEmpty();

        if (mainEmpty || offEmpty) return false;
        if (player.getOffhandItem().getItem() instanceof ShieldItem) return false;

        var mainAttributes = WeaponRegistry.getAttributes(player.getMainHandItem());
        var offAttributes = WeaponRegistry.getAttributes(player.getOffhandItem());
        return mainAttributes != null && !mainAttributes.isTwoHanded() && offAttributes != null && !offAttributes.isTwoHanded();
    }

    public static boolean isTwoHandedWielding(Player player) {
        if (isKiWeaponActive(player)) {
            var kiAttributes = getKiWeaponAttributes(player);
            return kiAttributes != null && kiAttributes.isTwoHanded();
        }
        var mainAttributes = WeaponRegistry.getAttributes(player.getMainHandItem());
        if (mainAttributes != null) return mainAttributes.isTwoHanded();
        return false;
    }

    public static float getAttackCooldownTicksCapped(Player player) {
        float intervalCap = ConfigManager.getCombatConfig().getAttackIntervalCap();
        float rawDelay = player.getCurrentItemAttackStrengthDelay();
        if (!Float.isFinite(rawDelay) || rawDelay <= 0.0F) rawDelay = intervalCap;
        float capped = Math.max(rawDelay, intervalCap);
        return Math.max(2.0f, Mth.clamp(capped, intervalCap, 200.0F));
    }

    public static AttackHand getCurrentAttack(Player player, int comboCount) {
        if (isDualWielding(player)) {
            boolean isOffHand = shouldAttackWithOffHand(player, comboCount);
            var itemStack = isOffHand ? player.getOffhandItem() : player.getMainHandItem();
            WeaponAttributes attributes;

            if (itemStack.isEmpty()) {
                attributes = resolveEmptyHandAttributes(player);
            } else attributes = WeaponRegistry.getAttributes(itemStack);

            if (attributes != null && attributes.attacks() != null) {
                int handSpecificComboCount = ((isOffHand && comboCount > 0) ? (comboCount - 1) : (comboCount)) / 2;
                var attackSelection = selectAttack(handSpecificComboCount, attributes, player, isOffHand);
                if (attackSelection == null) return null;
                var attack = attackSelection.attack;
                var combo = attackSelection.comboState;
                return new AttackHand(attack, combo, isOffHand, attributes, itemStack);
            }
        } else {
            var itemStack = player.getMainHandItem();
            WeaponAttributes attributes;

            if (itemStack.isEmpty()) {
                attributes = resolveEmptyHandAttributes(player);
            } else attributes = WeaponRegistry.getAttributes(itemStack);

            if (attributes != null && attributes.attacks() != null) {
                var attackSelection = selectAttack(comboCount, attributes, player, false);
                if (attackSelection == null) return null;
                var attack = attackSelection.attack;
                var combo = attackSelection.comboState;
                return new AttackHand(attack, combo, false, attributes, itemStack);
            }
        }
        return null;
    }

    private record AttackSelection(WeaponAttributes.Attack attack, ComboState comboState) { }

    @Nullable
    private static AttackSelection selectAttack(int comboCount, WeaponAttributes attributes, Player player, boolean isOffHandAttack) {
        var attacks = attributes.attacks();

        attacks = Arrays.stream(attacks)
                .filter(attack -> attack.conditions() == null || attack.conditions().length == 0 || evaluateConditions(attack.conditions(), player, isOffHandAttack))
                .toArray(WeaponAttributes.Attack[]::new);

        if (comboCount < 0) comboCount = 0;

        if (attacks.length == 0) return null;
        int index = comboCount % attacks.length;
        return new AttackSelection(attacks[index], new ComboState(index + 1, attacks.length));
    }

    private static boolean evaluateConditions(String[] conditions, Player player, boolean isOffHandAttack) {
        return Arrays.stream(conditions).allMatch(condition -> evaluateCondition(condition, player, isOffHandAttack));
    }

    private static boolean evaluateCondition(String raw, Player player, boolean isOffHandAttack) {
        if (raw == null || raw.isBlank()) return true;

        String type;
        String[] args;
        int colon = raw.indexOf(':');
        if (colon >= 0) {
            type = raw.substring(0, colon).trim().toUpperCase();
            args = Arrays.stream(raw.substring(colon + 1).split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
        } else {
            type = raw.trim().toUpperCase();
            args = new String[0];
        }

        switch (type) {
            case "SKILL_ACTIVE" -> {
                if (args.length < 1) return true;
                var skills = getSkills(player);
                return skills != null && skills.isSkillActive(args[0]);
            }
            case "SKILL_LEVEL" -> {
                if (args.length < 2) return true;
                var skills = getSkills(player);
                if (skills == null) return false;
                try {
                    return skills.getSkillLevel(args[0]) >= Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }

        WeaponAttributes.Condition condition;
        try {
            condition = WeaponAttributes.Condition.valueOf(type);
        } catch (IllegalArgumentException e) {
            return true;
        }

        switch (condition) {
            case NOT_DUAL_WIELDING -> {
                return !isDualWielding(player);
            }
            case DUAL_WIELDING_ANY -> {
                return isDualWielding(player);
            }
            case DUAL_WIELDING_SAME -> {
                return isDualWielding(player) && (player.getMainHandItem().getItem() == player.getOffhandItem().getItem());
            }
            case DUAL_WIELDING_SAME_CATEGORY -> {
                if (!isDualWielding(player)) return false;
                var mainHandAttributes = isKiWeaponActive(player)
                        ? getKiWeaponAttributes(player)
                        : WeaponRegistry.getAttributes(player.getMainHandItem());
                var offHandAttributes = WeaponRegistry.getAttributes(player.getOffhandItem());
                if (mainHandAttributes == null || offHandAttributes == null
                        || mainHandAttributes.category() == null || mainHandAttributes.category().isEmpty()
                        || offHandAttributes.category() == null || offHandAttributes.category().isEmpty()) {
                    return false;
                }
                return mainHandAttributes.category().equals(offHandAttributes.category());
            }
            case NO_OFFHAND_ITEM -> {
                var offhandStack = player.getOffhandItem();
                return offhandStack.isEmpty();
            }
            case OFF_HAND_SHIELD -> {
                var offhandStack = player.getOffhandItem();
                return !offhandStack.isEmpty() && offhandStack.getItem() instanceof ShieldItem;
            }
            case MAIN_HAND_ONLY -> {
                return !isOffHandAttack;
            }
            case OFF_HAND_ONLY -> {
                return isOffHandAttack;
            }
            case MOUNTED -> {
                return player.getVehicle() != null;
            }
            case NOT_MOUNTED -> {
                return player.getVehicle() == null;
            }
            case SNEAKING -> {
                return player.isCrouching();
            }
            case NOT_SNEAKING -> {
                return !player.isCrouching();
            }
        }
        return true;
    }

    public static boolean canAttack(Player player) {
        return true;
    }

    public static boolean isChargingTechnique(Player player) {
        var stats = StatsProvider.get(StatsCapability.INSTANCE, player).resolve().orElse(null);
        if (stats == null) return false;
        var techniques = stats.getTechniques();
        return techniques.isTechniqueCharging() || techniques.isTechniqueChargeActive();
    }

    public static double getEffectiveAttackRange(Player player, double weaponAttackRange) {
        var entityReachAttr = player.getAttribute(ForgeMod.ENTITY_REACH.get());
        if (entityReachAttr == null) return Math.max(0.0D, weaponAttackRange);
        double defaultEntityReach = ForgeMod.ENTITY_REACH.get().getDefaultValue();
        double currentEntityReach = entityReachAttr.getValue();
        double effectiveRange = weaponAttackRange + (currentEntityReach - defaultEntityReach);
        return Math.max(0.0D, effectiveRange);
    }

    @Nullable
    private static WeaponAttributes getActiveFormComboAttributes(Player player) {
        var statsOpt = StatsProvider.get(StatsCapability.INSTANCE, player);
        if (!statsOpt.isPresent()) return null;

        var stats = statsOpt.resolve().orElse(null);
        if (stats == null) return null;

        var character = stats.getCharacter();
        if (character == null) return null;

        var formData = character.getActiveFormData();
        if (formData == null) formData = character.getActiveStackFormData();
        if (formData == null) return null;

        String formCombo = formData.getFormCombo();
        if (formCombo == null || formCombo.isBlank()) return null;

        String comboRaw = formCombo.trim().toLowerCase();
        Set<ResourceLocation> candidates = new LinkedHashSet<>();

        ResourceLocation parsed = ResourceLocation.tryParse(comboRaw);
        if (parsed != null) candidates.add(parsed);

        if (!comboRaw.contains(":")) {
            candidates.add(ResourceLocation.fromNamespaceAndPath("dragonminez", comboRaw));
            candidates.add(ResourceLocation.fromNamespaceAndPath("minecraft", comboRaw));
        }

        for (ResourceLocation candidate : candidates) {
            WeaponAttributes attributes = WeaponRegistry.getAttributes(candidate);
            if (attributes != null && attributes.attacks() != null) return attributes;
        }

        return null;
    }

    @Nullable
    private static Skills getSkills(Player player) {
        var statsOpt = StatsProvider.get(StatsCapability.INSTANCE, player);
        if (!statsOpt.isPresent()) return null;
        var stats = statsOpt.resolve().orElse(null);
        return stats != null ? stats.getSkills() : null;
    }

    public static boolean isKiWeaponActive(Player player) {
        if (!player.getMainHandItem().isEmpty()) return false;
        var statsOpt = StatsProvider.get(StatsCapability.INSTANCE, player);
        if (!statsOpt.isPresent()) return false;
        var stats = statsOpt.resolve().orElse(null);
        if (stats == null || !stats.getSkills().isSkillActive("kimanipulation")) return false;

        String type = stats.getStatus().getKiWeaponType();
        if (type == null || type.equalsIgnoreCase("none")) return false;
        return ConfigManager.getCombatConfig().getKiWeaponConfig(type) != null;
    }

    @Nullable
    public static WeaponAttributes getKiWeaponAttributes(Player player) {
        var statsOpt = StatsProvider.get(StatsCapability.INSTANCE, player);
        if (!statsOpt.isPresent()) return null;
        var stats = statsOpt.resolve().orElse(null);
        if (stats == null) return null;

        String type = stats.getStatus().getKiWeaponType();
        if (type == null) return null;

        var cfg = ConfigManager.getCombatConfig().getKiWeaponConfig(type);
        String combo = cfg != null ? cfg.getWeaponCombo() : null;
        if (combo != null && !combo.isBlank()) {
            ResourceLocation comboId = ResourceLocation.tryParse(combo.trim().toLowerCase());
            if (comboId != null) {
                var attributes = WeaponRegistry.getAttributes(comboId);
                if (attributes != null && attributes.attacks() != null) return attributes;
            }
        }
        return WeaponRegistry.getAttributes(ResourceLocation.fromNamespaceAndPath("dragonminez", "fist"));
    }

    @Nullable
    private static WeaponAttributes resolveEmptyHandAttributes(Player player) {
        if (isKiWeaponActive(player)) {
            var kiAttributes = getKiWeaponAttributes(player);
            if (kiAttributes != null && kiAttributes.attacks() != null) return kiAttributes;
        }
        var attributes = getActiveFormComboAttributes(player);
        if (attributes == null || attributes.attacks() == null) {
            attributes = WeaponRegistry.getAttributes(ResourceLocation.fromNamespaceAndPath("dragonminez", "fist"));
        }
        return attributes;
    }

    private static final Object attributesLock = new Object();

    public static void offhandAttributes(Player player, Runnable runnable) {
        synchronized (attributesLock) {
            setAttributesForOffHandAttack(player, true);
            runnable.run();
            setAttributesForOffHandAttack(player, false);
        }
    }

    public static void setAttributesForOffHandAttack(Player player, boolean useOffHand) {
        var mainHandStack = player.getMainHandItem();
        var offHandStack = player.getOffhandItem();
        ItemStack add;
        ItemStack remove;
        if (useOffHand) {
            remove = mainHandStack;
            add = offHandStack;
        } else {
            remove = offHandStack;
            add = mainHandStack;
        }
        player.getAttributes().removeAttributeModifiers(remove.getAttributeModifiers(MAINHAND));
        player.getAttributes().addTransientAttributeModifiers(add.getAttributeModifiers(MAINHAND));
    }
}
