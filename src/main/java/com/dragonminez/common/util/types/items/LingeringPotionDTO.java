package com.dragonminez.common.util.types.items;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class LingeringPotionDTO extends PotionDTO {

	public static final String ITEM_TYPE = "lingering_potion";
	private static final ResourceLocation LINGERING_POTION_ID = new ResourceLocation("minecraft", "lingering_potion");

	public LingeringPotionDTO(ResourceLocation potion, int count, List<PotionEffectDTO> mobEffects) {
		super(ITEM_TYPE, LINGERING_POTION_ID, count, potion, mobEffects);
	}
}
