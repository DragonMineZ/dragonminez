package com.dragonminez.common.util.types.items;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SplashPotionDTO extends PotionDTO {

	public static final String ITEM_TYPE = "splash_potion";
	private static final ResourceLocation SPLASH_POTION_ID = new ResourceLocation("minecraft", "splash_potion");

	public SplashPotionDTO(ResourceLocation potion, int count, List<PotionEffectDTO> mobEffects) {
		super(ITEM_TYPE, SPLASH_POTION_ID, count, potion, mobEffects);
	}
}
