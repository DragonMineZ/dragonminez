package com.dragonminez.common.util.types.items;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public final class SplashPotionDTO extends PotionDTO {
    private static final String ITEM_TYPE = "splash_potion";
    private static final ResourceLocation SPLASH_POTION_ID = ForgeRegistries.ITEMS.getKey(Items.SPLASH_POTION);

    public SplashPotionDTO(ResourceLocation potion, Integer count, List<PotionEffectDTO> mobEffects) {
        super(ITEM_TYPE, SPLASH_POTION_ID, count, potion, mobEffects);
    }
}
