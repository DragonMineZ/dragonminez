package com.dragonminez.common.init;

import com.dragonminez.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class MainDamageTypes {

    public static final ResourceKey<DamageType> KIBLAST = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "kiblast"));

	public static DamageSource kiblast(Level level, Entity projectile, Entity owner) {
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(KIBLAST), projectile, owner);
	}

	public static boolean isKiblastDamage(DamageSource source) {
		return source.typeHolder().is(KIBLAST);
	}

	public static void register() {}
}

