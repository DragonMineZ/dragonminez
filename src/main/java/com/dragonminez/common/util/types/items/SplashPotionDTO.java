package com.dragonminez.common.util.types.items;

import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class SplashPotionDTO extends PotionDTO {
    public SplashPotionDTO(String potion, Map<String, Integer> mobEffects) {
        super("splash_potion", "minecraft:splash_potion", 1, potion, mobEffects);
    }

    @Override
    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this, SplashPotionDTO.class);
    }
}
