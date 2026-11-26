package com.dragonminez.common.util.types.items;

import net.minecraft.resources.ResourceLocation;
import java.util.List;

public class LingeringPotionDTO extends PotionDTO {
	public static final String ITEM_TYPE = "lingering_potion";
	private static final ResourceLocation LINGERING_POTION_ID = ResourceLocation.withDefaultNamespace("lingering_potion");

	public LingeringPotionDTO(ResourceLocation potion, int count, List<PotionEffectDTO> mobEffects) {
		super(ITEM_TYPE, LINGERING_POTION_ID, count, potion, mobEffects);
	}

	public LingeringPotionDTO() {
	}
}
