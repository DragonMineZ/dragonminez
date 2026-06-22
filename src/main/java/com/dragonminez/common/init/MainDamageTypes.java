package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.OzaruFistEntity;
import com.dragonminez.common.init.entities.ki.SPDragonFistEntity;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import com.dragonminez.common.stats.techniques.TechniqueData;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Locale;

public class MainDamageTypes {

    public static final ResourceKey<DamageType> KIBLAST = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "kiblast"));
    public static final ResourceKey<DamageType> STRIKE_ATTACK = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "strike_attack"));

	public static DamageSource kiblast(Level level, Entity projectile, Entity owner) {
		Holder<DamageType> holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(KIBLAST);
		String messageId = resolveKiMessageId(projectile);
		boolean randomVariant = messageId.startsWith("kiblast.");
		Component techniqueName = (randomVariant || messageId.equals("soul_punisher")) ? resolveKiName(projectile, owner) : null;
		return new DMZDamageSource(holder, projectile, owner, messageId, randomVariant, techniqueName);
	}

	private static String resolveKiMessageId(Entity projectile) {
		if (projectile instanceof OzaruFistEntity) return "strike_attack.oozaru_fist";
		if (projectile instanceof SPDragonFistEntity) return "strike_attack.dragon_fist";
		if (projectile instanceof AbstractKiProjectile proj) {
			if ("soul_punisher".equals(proj.getTechniqueId())) return "soul_punisher";
			AbstractKiProjectile.KiType type = proj.getKiType();
			return "kiblast." + (type != null ? type.name().toLowerCase(Locale.ROOT) : "small_ball");
		}
		return "kiblast.small_ball";
	}

	private static Component resolveKiName(Entity projectile, Entity owner) {
		String techId = projectile instanceof AbstractKiProjectile proj ? proj.getTechniqueId() : null;
		if (techId == null || techId.isEmpty()) return Component.translatable("death.attack.dmz.ki_generic");
		if (PredefinedTechniques.isPredefinedTechniqueId(techId)) {
			KiAttackData data = PredefinedTechniques.REGISTRY.get(techId);
			if (data != null && data.getName() != null && !data.getName().isEmpty()) return Component.translatable(data.getName());
		}
		Component custom = resolveCustomName(owner, techId);
		return custom != null ? custom : Component.translatable("death.attack.dmz.ki_generic");
	}

	private static Component resolveCustomName(Entity owner, String techId) {
		if (!(owner instanceof Player player)) return null;
		Component[] result = {null};
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			TechniqueData tech = stats.getTechniques().getUnlockedTechniques().get(techId);
			if (tech != null && tech.getName() != null && !tech.getName().isEmpty()) {
				result[0] = Component.literal(tech.getName());
			}
		});
		return result[0];
	}

	public static DamageSource strikeAttack(Level level, Entity attacker, String strikeId) {
		Holder<DamageType> holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(STRIKE_ATTACK);
		String id = (strikeId != null && PredefinedTechniques.STRIKE_IDS.contains(strikeId)) ? strikeId : "generic";
		return new DMZDamageSource(holder, attacker, attacker, "strike_attack." + id, false, null);
	}

	public static boolean isKiblastDamage(DamageSource source) {
		return source.typeHolder().is(KIBLAST);
	}

	public static boolean isStrikeAttackDamage(DamageSource source) {
		return source.typeHolder().is(STRIKE_ATTACK);
	}

	public static void register() {}
}
