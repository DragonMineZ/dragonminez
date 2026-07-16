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
public final class TippedArrowDTO extends PotionDTO {
    private static final String ITEM_TYPE = "tipped_arrow";
    private static final ResourceLocation TIPPED_ARROW_ID = ForgeRegistries.ITEMS.getKey(Items.TIPPED_ARROW);

    public TippedArrowDTO(ResourceLocation potion, Integer count, List<PotionEffectDTO> mobEffects) {
        super(ITEM_TYPE, TIPPED_ARROW_ID, count, potion, mobEffects);
    }
}
