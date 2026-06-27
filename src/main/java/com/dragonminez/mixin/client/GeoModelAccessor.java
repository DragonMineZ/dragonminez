package com.dragonminez.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import software.bernie.geckolib.model.GeoModel;

@Mixin(GeoModel.class)
public interface GeoModelAccessor {
	@Accessor("lastRenderedInstance")
	void dmz$setLastRenderedInstance(long value);
}
