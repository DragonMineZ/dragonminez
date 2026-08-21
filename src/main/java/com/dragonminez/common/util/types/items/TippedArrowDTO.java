package com.dragonminez.common.util.types.items;

import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class TippedArrowDTO extends PotionDTO {
    public TippedArrowDTO(String potion, Integer count, Map<String, Integer> mobEffects) {
        super("tipped_arrow", "minecraft:tipped_arrow", count, potion, mobEffects);
    }

    @Override
    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this, TippedArrowDTO.class);
    }
}
