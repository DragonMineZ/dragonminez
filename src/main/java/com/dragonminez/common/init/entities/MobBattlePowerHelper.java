package com.dragonminez.common.init.entities;

import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.MainAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public final class MobBattlePowerHelper {

	// Escala aditiva alineada con el BP nativo de DMZ para mobs (naive = health + attack*5).
	private static final double ATTACK_WEIGHT = 5.0;
	private static final double ARMOR_WEIGHT = 5.0;
	private static final double RANGED_WEIGHT = 5.0;
	private static final double MOVEMENT_WEIGHT = 15.0;

	private static final ResourceLocation AUTOLEVELING_PROJECTILE_DAMAGE = new ResourceLocation("autoleveling", "monster.projectile_damage_bonus");
	private static final ResourceLocation AUTOLEVELING_EXPLOSION_DAMAGE = new ResourceLocation("autoleveling", "monster.explosion_damage_bonus");

	private MobBattlePowerHelper() {}

	public static boolean isDmzManaged(LivingEntity entity) {
		if (entity instanceof Player) return true;
		ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
		return key != null && "dragonminez".equals(key.getNamespace());
	}

	public static int calculate(LivingEntity entity) {
		double maxHealth = attributeValue(entity, Attributes.MAX_HEALTH);
		double attackDamage = attributeValue(entity, Attributes.ATTACK_DAMAGE);
		double armor = attributeValue(entity, Attributes.ARMOR);
		double armorToughness = attributeValue(entity, Attributes.ARMOR_TOUGHNESS);
		double movementSpeed = movementOrFlyingSpeed(entity);

		double kiPower = positivePower(entity, MainAttributes.KI_DAMAGE.get());
		if (kiPower <= 0.0) kiPower = positivePower(entity, EntityAttributes.KI_BLAST_DAMAGE.get());
		double projectileDamage = autoLevelingBonus(entity, AUTOLEVELING_PROJECTILE_DAMAGE);
		double explosionDamage = autoLevelingBonus(entity, AUTOLEVELING_EXPLOSION_DAMAGE);
		double rangedPower = kiPower + projectileDamage + explosionDamage;

		// Escala aditiva alineada con el BP nativo de DMZ (health + attack*5), enriquecida con armadura,
		// velocidad y daño a distancia. Sin la curva del jugador para no inflar mobs débiles (p.ej. ovejas).
		double battlePower = maxHealth
				+ attackDamage * ATTACK_WEIGHT
				+ (armor + armorToughness) * ARMOR_WEIGHT
				+ rangedPower * RANGED_WEIGHT
				+ movementSpeed * MOVEMENT_WEIGHT;

		if (!Double.isFinite(battlePower) || battlePower < 5.0) return 5;
		if (battlePower >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
		return (int) Math.max(5L, Math.round(battlePower));
	}

	private static double movementOrFlyingSpeed(LivingEntity entity) {
		AttributeInstance movement = entity.getAttribute(Attributes.MOVEMENT_SPEED);
		if (movement != null) return sanitize(movement.getValue());
		return attributeValue(entity, EntityAttributes.FLY_SPEED.get());
	}

	private static double autoLevelingBonus(LivingEntity entity, ResourceLocation id) {
		if (!ModList.get().isLoaded("autoleveling")) return 0.0;
		Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(id);
		if (attribute == null) return 0.0;
		AttributeInstance instance = entity.getAttribute(attribute);
		return instance == null ? 0.0 : Math.max(0.0, sanitize(instance.getValue()) - 1.0);
	}

	private static double positivePower(LivingEntity entity, Attribute attribute) {
		AttributeInstance instance = entity.getAttribute(attribute);
		if (instance == null) return 0.0;
		return Math.max(0.0, sanitize(instance.getValue()));
	}

	private static double attributeValue(LivingEntity entity, Attribute attribute) {
		AttributeInstance instance = entity.getAttribute(attribute);
		return instance == null ? 0.0 : sanitize(instance.getValue());
	}

	private static double sanitize(double value) {
		return Double.isFinite(value) ? value : 0.0;
	}
}
