package com.dragonminez.common.util.types.items;

import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
public class PotionDTO extends GenericItemDTO {
    private static final String ITEM_TYPE = "potion";
    private static final ResourceLocation POTION_ID = ForgeRegistries.ITEMS.getKey(Items.POTION);

    protected ResourceLocation potion;
    protected List<PotionEffectDTO> mobEffects = new ArrayList<>();

    public PotionDTO(ResourceLocation potion, List<PotionEffectDTO> mobEffects) {
        super(ITEM_TYPE, POTION_ID, 1);
        this.potion = potion;
        this.mobEffects = mobEffects;
    }

    public PotionDTO(String itemType, ResourceLocation itemId, int count, ResourceLocation potion, List<PotionEffectDTO> mobEffects) {
        super(itemType, itemId, count);
        this.potion = potion;
        this.mobEffects = mobEffects;
    }

    @Override
    public ItemStack getItemStack() {
        var itemStack = super.getItemStack();
        if (itemStack.getItem() instanceof PotionItem) {
            this.applyPotionEffects(itemStack);
        }
        return itemStack;
    }

    protected void applyPotionEffects(ItemStack itemStack) {
        Potion potion = Potions.EMPTY;
        if (this.potion != null) {
            potion = ForgeRegistries.POTIONS.getValue(this.potion);
            if (potion == null) {
                throw new JsonSyntaxException(getErrorPrefix() + " unknown potion '" + this.potion + "'.");
            }
        }

        PotionUtils.setPotion(itemStack, potion);
        if (this.mobEffects != null && !this.mobEffects.isEmpty()) {
            PotionUtils.setCustomEffects(itemStack, this.getMobEffects());
        }
    }

    protected List<MobEffectInstance> getMobEffects() {
        return this.mobEffects.stream().map(this::getMobEffect).toList();
    }

    protected final MobEffectInstance getMobEffect(PotionEffectDTO potionEffect) {
        if (potionEffect == null) {
            throw new JsonSyntaxException(getErrorPrefix() + " potion effect entry must not be null.");
        }

        if (potionEffect.getEffect() == null) {
            throw new JsonSyntaxException(getErrorPrefix() + " mobEffects contains an entry with a null effect id.");
        }

        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(potionEffect.getEffect());
        if (effect == null) {
            throw new JsonSyntaxException(getErrorPrefix() + " unknown mob effect '" + potionEffect.getEffect() + "'.");
        }

        if (potionEffect.getDuration() == null || potionEffect.getDuration() < 1) {
            throw new JsonSyntaxException(getErrorPrefix() + " mob effect '" + potionEffect.getEffect() + "' has invalid duration " + potionEffect.getDuration() + ".");
        }

        if (potionEffect.getAmplifier() == null || potionEffect.getAmplifier() < 0) {
            throw new JsonSyntaxException(getErrorPrefix() + " mob effect '" + potionEffect.getEffect() + "' has invalid amplifier " + potionEffect.getAmplifier() + ".");
        }

        return new MobEffectInstance(
                effect,
                potionEffect.getDuration(),
                potionEffect.getAmplifier(),
                potionEffect.getAmbient(),
                potionEffect.getVisible(),
                potionEffect.getShowIcon()
        );
    }
}
