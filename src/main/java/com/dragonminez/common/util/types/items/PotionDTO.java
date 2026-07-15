package com.dragonminez.common.util.types.items;

import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class PotionDTO extends GenericItemDTO {
    protected String potion;
    protected Map<String, Integer> mobEffects = new HashMap<>();

    public PotionDTO(String potion, Map<String, Integer> mobEffects) {
        super("potion", "minecraft:potion", 1);
        this.potion = potion;
        this.mobEffects = mobEffects;
    }

    public PotionDTO(String itemType, String itemId, int count, String potion, Map<String, Integer> mobEffects) {
        super(itemType, itemId, count);
        this.potion = potion;
        this.mobEffects = mobEffects;
    }

    @Override
    public ItemStack getItemStack() {
        var itemStack = super.getItemStack();
        if (potion != null && !potion.isBlank()) {
            PotionUtils.setPotion(itemStack, Potion.byName(this.potion));
        } else {
            PotionUtils.setPotion(itemStack, Potions.EMPTY);
        }
        if (!mobEffects.isEmpty()) {
            PotionUtils.setCustomEffects(itemStack, getMobEffects());
        }
        return itemStack;
    }

    @Override
    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this, PotionDTO.class);
    }

    protected Collection<MobEffectInstance> getMobEffects() {
        return this.mobEffects.keySet().stream().map(key -> this.getMobEffect(key, this.mobEffects.get(key))).collect(Collectors.toList());
    }

    protected MobEffectInstance getMobEffect(String id, Integer amplifier) {
        var resourceLocation = ResourceLocation.tryParse(id);
        if (resourceLocation != null) {
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(resourceLocation);
            if (effect != null) {
                return new MobEffectInstance(effect, amplifier);
            }
            return null;
        }
        return null;
    }
}
