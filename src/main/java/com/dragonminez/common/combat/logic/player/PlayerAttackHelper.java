package com.dragonminez.common.combat.logic.player;

import com.dragonminez.common.combat.logic.weapon.WeaponRegistry;
import com.dragonminez.common.combat.weapon.WeaponAttributes;
import com.dragonminez.common.combat.player.AttackHand;
import com.dragonminez.common.combat.player.ComboState;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
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
        var mainAttributes = WeaponRegistry.getAttributes(player.getMainHandItem());
        var offAttributes = WeaponRegistry.getAttributes(player.getOffhandItem());
        return mainAttributes != null && !mainAttributes.isTwoHanded()
                && offAttributes != null && !offAttributes.isTwoHanded();
    }

    public static boolean isTwoHandedWielding(Player player) {
        var mainAttributes = WeaponRegistry.getAttributes(player.getMainHandItem());
        if (mainAttributes != null) return mainAttributes.isTwoHanded();
        return false;
    }

    public static float getAttackCooldownTicksCapped(Player player) {
        float intervalCap = ConfigManager.getCombatConfig().getAttackIntervalCap();
        float rawDelay = player.getCurrentItemAttackStrengthDelay();
        if (!Float.isFinite(rawDelay) || rawDelay <= 0.0F) rawDelay = intervalCap;
        float capped = Math.max(rawDelay, intervalCap);
        return Mth.clamp(capped, intervalCap, 200.0F);
    }

    public static AttackHand getCurrentAttack(Player player, int comboCount) {
        if (isDualWielding(player)) {
            boolean isOffHand = shouldAttackWithOffHand(player, comboCount);
            var itemStack = isOffHand
                    ? player.getOffhandItem()
                    : player.getMainHandItem();
            var attributes = WeaponRegistry.getAttributes(itemStack);

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
                attributes = getActiveFormComboAttributes(player);
                if (attributes == null || attributes.attacks() == null) attributes = WeaponRegistry.getAttributes(itemStack);
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

    private static boolean evaluateConditions(WeaponAttributes.Condition[] conditions, Player player, boolean isOffHandAttack) {
        return Arrays.stream(conditions).allMatch(condition -> evaluateCondition(condition, player, isOffHandAttack));
    }

    private static boolean evaluateCondition(WeaponAttributes.Condition condition, Player player, boolean isOffHandAttack) {
        if (condition == null) return true;

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
                var mainHandAttributes = WeaponRegistry.getAttributes(player.getMainHandItem());
                var offHandAttributes = WeaponRegistry.getAttributes(player.getOffhandItem());
                if (mainHandAttributes.category() == null || mainHandAttributes.category().isEmpty()
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
        }
        return true;
    }

    public static boolean canAttack(Player player) {
        return true;
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
