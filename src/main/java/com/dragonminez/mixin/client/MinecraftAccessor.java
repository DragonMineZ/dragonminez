package com.dragonminez.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
	@Accessor("missTime")
	int getAttackCooldown();

	@Accessor("missTime")
	void setAttackCooldown(int attackCooldown);
}