package com.dragonminez.common.combat.logic.player;

import com.dragonminez.common.config.ConfigManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

public class TargetHelper {
    public enum Relation {
        FRIENDLY, NEUTRAL, HOSTILE;

        public static Relation coalesce(Relation value, Relation fallback) {
            if (value != null) {
                return value;
            }
            return fallback;
        }
    }

    public static Relation getRelation(Player attacker, Entity target) {
        if (attacker == target) {
            return Relation.FRIENDLY;
        }
        if (target instanceof TamableAnimal tameable) {
            var owner = tameable.getOwner();
            if (owner != null) {
                return getRelation(attacker, owner);
            }
        }
        if (target instanceof HangingEntity) {
            return Relation.NEUTRAL;
        }

        var config = ConfigManager.getCombatConfig();
        var casterTeam = attacker.getTeam();
        var targetTeam = target.getTeam();

        if (casterTeam == null || targetTeam == null) {
            var id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
            var mappedRelation = config.getPlayerRelations().get(id != null ? id.toString() : "");
            if (mappedRelation != null) return mappedRelation;
            if (target instanceof Animal) return Relation.coalesce(config.getPlayerRelationToPassives(), Relation.HOSTILE);
            if (target instanceof Monster) return Relation.coalesce(config.getPlayerRelationToHostiles(), Relation.HOSTILE);
            return Relation.coalesce(config.getPlayerRelationToOther(), Relation.HOSTILE);
        } else return attacker.isAlliedTo(target) ? Relation.FRIENDLY : Relation.HOSTILE;
    }

    public static boolean isAttackableMount(Entity entity) {
        if (entity instanceof Monster || isEntityHostileVehicle(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()) != null ? ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString() : "")) {
            return true;
        }
        return ConfigManager.getCombatConfig().getAllowAttackingMount();
    }

    public static boolean isEntityHostileVehicle(String entityName) {
        return false;
    }
}