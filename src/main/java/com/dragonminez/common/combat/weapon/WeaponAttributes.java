package com.dragonminez.common.combat.weapon;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class WeaponAttributes {

    private final double attack_range;
    @Nullable
    private final String pose;
    @Nullable
    private final String off_hand_pose;
    private final Boolean two_handed;
    @Nullable
    private final String category;
    private final Attack[] attacks;

    @Nullable
    private final Double crit_chance;
    @Nullable
    private final Double crit_damage;

    public WeaponAttributes(double attack_range, @Nullable String pose, @Nullable String off_hand_pose, Boolean isTwoHanded, String category, Attack[] attacks, @Nullable Double crit_chance, @Nullable Double crit_damage) {
        this.attack_range = attack_range;
        this.pose = pose;
        this.off_hand_pose = off_hand_pose;
        this.two_handed = isTwoHanded;
        this.category = category;
        this.attacks = attacks;
        this.crit_chance = crit_chance;
        this.crit_damage = crit_damage;
    }

    public WeaponAttributes(double attack_range, @Nullable String pose, @Nullable String off_hand_pose, Boolean isTwoHanded, String category, Attack[] attacks) {
        this(attack_range, pose, off_hand_pose, isTwoHanded, category, attacks, null, null);
    }

    public static final class Attack {
        private Condition[] conditions;
        private HitBoxShape hitbox;
        private double damage_multiplier = 1;
        private double angle = 0;

        /**
         * Formula to calculate attack cooldown in ticks: (1 / (4 - ATTACK_SPEED)) * 20
         */
        private double upswing = 0;
        private String animation = null;
        private Sound swing_sound = null;
        private Sound impact_sound = null;
        public Attack() { }

        public Attack(
                Condition[] conditions,
                HitBoxShape hitbox,
                double damage_multiplier,
                double angle,
                double upswing,
                String animation,
                Sound swing_sound,
                Sound impact_sound
        ) {
            this.conditions = conditions;
            this.hitbox = hitbox;
            this.damage_multiplier = damage_multiplier;
            this.angle = angle;
            this.upswing = upswing;
            this.animation = animation;
            this.swing_sound = swing_sound;
            this.impact_sound = impact_sound;
        }

        @Nullable
        public Condition[] conditions() {
            return conditions;
        }

        public HitBoxShape hitbox() {
            return hitbox;
        }

        public double damageMultiplier() {
            return damage_multiplier;
        }

        public double angle() {
            return angle;
        }

        public double upswing() {
            return upswing;
        }

        public String animation() {
            return animation;
        }

        public Sound swingSound() {
            return swing_sound;
        }

        public Sound impactSound() {
            return impact_sound;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Attack) obj;
            return Objects.equals(this.hitbox, that.hitbox) &&
                    Double.doubleToLongBits(this.damage_multiplier) == Double.doubleToLongBits(that.damage_multiplier) &&
                    Double.doubleToLongBits(this.angle) == Double.doubleToLongBits(that.angle) &&
                    Double.doubleToLongBits(this.upswing) == Double.doubleToLongBits(that.upswing) &&
                    Objects.equals(this.animation, that.animation) &&
                    Objects.equals(this.swing_sound, that.swing_sound) &&
                    Objects.equals(this.impact_sound, that.impact_sound);
        }

        @Override
        public int hashCode() {
            return Objects.hash(hitbox, damage_multiplier, angle, upswing, animation, swing_sound, impact_sound);
        }

        @Override
        public String toString() {
            return "Attack[" + "hitbox=" + hitbox + ", " + "damage_multiplier=" + damage_multiplier + ", " + "angle=" + angle + ", " +
                    "upswing=" + upswing + ", " + "animation=" + animation + ", " + "swing_sound=" + swing_sound + ", " + "impact_sound=" + impact_sound + ']';
        }
    }

    public enum HitBoxShape {
        FORWARD_BOX,
        VERTICAL_PLANE,
        HORIZONTAL_PLANE
    }

    public enum Condition {
        /**
         * Fulfilled if the player is not dual wielding weapons
         */
        NOT_DUAL_WIELDING,
        /**
         * Fulfilled if the player is dual wielding any weapons
         */
        DUAL_WIELDING_ANY,
        /**
         * Fulfilled if the player is dual wielding items with matching ids
         */
        DUAL_WIELDING_SAME,
        /**
         * Fulfilled if the player is dual wielding items with matching categories
         * (Category of an item is specified at `WeaponAttributes.category`)
         */
        DUAL_WIELDING_SAME_CATEGORY,
        /**
         * Fulfilled if the player has not item at all in the off-hand
         */
        NO_OFFHAND_ITEM,
        /**
         * Fulfilled if the player has a shield in the off-hand
         */
        OFF_HAND_SHIELD,
        /**
         * Fulfilled for attacks performed with main-hand only
         */
        MAIN_HAND_ONLY,
        /**
         * Fulfilled for attacks performed with off-hand only
         */
        OFF_HAND_ONLY,
        /**
         * Fulfilled if the player is riding some entity
         */
        MOUNTED,
        /**
         * Fulfilled if the player is not riding any entity
         */
        NOT_MOUNTED,
        /**
         * Fulfilled if the player is sneaking
         */
        SNEAKING,
        /**
         * Fulfilled if the player is not sneaking
         */
        NOT_SNEAKING
    }

    public static final class Sound {
        private String id = null;
        private float volume = 1;
        private float pitch = 1;
        public Sound() { }
        public Sound(String id) { this.id = id; }
        private float randomness = 0.1F;
        public String id() { return id; }
        public float volume() { return volume; }
        public float pitch() { return pitch; }
        public float randomness() { return randomness; }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Sound) obj;
            return Objects.equals(this.id, that.id) &&
                    Float.floatToIntBits(this.volume) == Float.floatToIntBits(that.volume) &&
                    Float.floatToIntBits(this.pitch) == Float.floatToIntBits(that.pitch) &&
                    Float.floatToIntBits(this.randomness) == Float.floatToIntBits(that.randomness);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, volume, pitch, randomness);
        }

        @Override
        public String toString() {
            return "SoundV2[" + "id=" + id + ", " + "volume=" + volume + ", " + "pitch=" + pitch + ", " + "randomness=" + randomness + ']';
        }
    }

    public double attackRange() { return attack_range; }
    @Nullable
    public String pose() { return pose; }
    @Nullable
    public String offHandPose() { return off_hand_pose; }
    @Nullable
    public String category() { return category; }
    public boolean isTwoHanded() { return two_handed != null ? two_handed.booleanValue() : false; }
    public Boolean two_handed() { return two_handed; }
    public Attack[] attacks() { return attacks; }

    @Nullable
    public Double crit_chance() { return crit_chance; }
    @Nullable
    public Double crit_damage() { return crit_damage; }

    public double getSafeCritChance() { return crit_chance != null ? crit_chance : 0.0D; }
    public double getSafeCritDamage() { return crit_damage != null ? crit_damage : 0.0D; }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (WeaponAttributes) obj;
        return Double.doubleToLongBits(this.attack_range) == Double.doubleToLongBits(that.attack_range) &&
                Objects.equals(this.pose, that.pose) && Objects.equals(this.two_handed, that.two_handed) &&
                Objects.equals(this.attacks, that.attacks) &&
                Objects.equals(this.crit_chance, that.crit_chance) &&
                Objects.equals(this.crit_damage, that.crit_damage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attack_range, two_handed, attacks, crit_chance, crit_damage);
    }

    @Override
    public String toString() {
        return "WeaponAttributes[" + "attack_range=" + attack_range + ", " + "pose=" + pose + ", " + "isTwoHanded=" + two_handed + ", " + "attacks=" + attacks + ", " + "crit_chance=" + crit_chance + ", " + "crit_damage=" + crit_damage + ']';
    }
}