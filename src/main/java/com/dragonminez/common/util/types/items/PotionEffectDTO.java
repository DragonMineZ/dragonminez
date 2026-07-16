package com.dragonminez.common.util.types.items;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;

@Getter
@Setter
@NoArgsConstructor
public class PotionEffectDTO {
    private ResourceLocation effect;
    private Integer duration;
    private Integer amplifier;
    private Boolean ambient = false;
    private Boolean visible = true;
    private Boolean showIcon = true;
}
