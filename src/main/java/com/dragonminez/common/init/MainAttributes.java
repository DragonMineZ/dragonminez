package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MainAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
        DeferredRegister.create(ForgeRegistries.ATTRIBUTES, Reference.MOD_ID);

    public static final RegistryObject<Attribute> STRENGTH = registerMainStat("strength", "attribute.dragonminez.strength", 5.0);
    public static final RegistryObject<Attribute> STRIKE_POWER = registerMainStat("strike_power", "attribute.dragonminez.strike_power", 5.0);
    public static final RegistryObject<Attribute> RESISTANCE = registerMainStat("resistance", "attribute.dragonminez.resistance", 5.0);
    public static final RegistryObject<Attribute> VITALITY = registerMainStat("vitality", "attribute.dragonminez.vitality", 5.0);
    public static final RegistryObject<Attribute> KI_POWER = registerMainStat("ki_power", "attribute.dragonminez.ki_power", 5.0);
    public static final RegistryObject<Attribute> ENERGY = registerMainStat("energy", "attribute.dragonminez.energy", 5.0);

    public static final RegistryObject<Attribute> MAX_ENERGY = register("max_energy", "attribute.dragonminez.max_energy", 20.0, 0.0, 2000000000.0);
    public static final RegistryObject<Attribute> MAX_STAMINA = register("max_stamina", "attribute.dragonminez.max_stamina", 20.0, 0.0, 2000000000.0);
    public static final RegistryObject<Attribute> MAX_POISE = register("max_poise", "attribute.dragonminez.max_poise", 25.0, 0.0, 2000000000.0);
    public static final RegistryObject<Attribute> MELEE_DAMAGE = register("melee_damage", "attribute.dragonminez.melee_damage", 1.0, 0.0, 2000000000.0);
    public static final RegistryObject<Attribute> STRIKE_DAMAGE = register("strike_damage", "attribute.dragonminez.strike_damage", 1.0, 0.0, 2000000000.0);
    public static final RegistryObject<Attribute> KI_DAMAGE = register("ki_damage", "attribute.dragonminez.ki_damage", 0.0, 0.0, 2000000000.0);
    public static final RegistryObject<Attribute> DEFENSE = register("defense", "attribute.dragonminez.defense", 0.0, 0.0, 2000000000.0);

    public static final RegistryObject<Attribute> CRIT_CHANCE = register("crit_chance", "attribute.dragonminez.critical_chance", 0.05D, 0.0, 1.0D);
    public static final RegistryObject<Attribute> CRIT_DAMAGE = register("crit_damage", "attribute.dragonminez.critical_damage", 1.5D, 1.0D, 100.0D);

    private static RegistryObject<Attribute> registerMainStat(String registryName, String translationKey, double defaultValue) {
        return register(registryName, translationKey, defaultValue, 0.0, getConfiguredMainStatMax());
    }

    private static double getConfiguredMainStatMax() {
        if (ConfigManager.getServerConfig() != null && ConfigManager.getServerConfig().getGameplay() != null) {
            return ConfigManager.getServerConfig().getGameplay().getMaxValue();
        }
        return 10000.0;
    }

    private static RegistryObject<Attribute> register(String registryName, String translationKey, double defaultValue, double min, double max) {
        return ATTRIBUTES.register(registryName,
            () -> new RangedAttribute(translationKey, defaultValue, min, max).setSyncable(true));
    }
}
