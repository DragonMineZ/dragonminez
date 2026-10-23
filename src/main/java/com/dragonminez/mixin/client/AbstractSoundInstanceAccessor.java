package com.dragonminez.mixin.client;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractSoundInstance.class)
public interface AbstractSoundInstanceAccessor {
	@Accessor("volume")
	float dmz$getVolume();

	@Accessor("volume")
	void dmz$setVolume(float volume);
}
