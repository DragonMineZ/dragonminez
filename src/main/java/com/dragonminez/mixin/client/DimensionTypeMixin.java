package com.dragonminez.mixin.client;

import com.dragonminez.server.world.dimension.CustomSpecialEffects;
import com.dragonminez.server.world.dimension.NamekDimension;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DimensionType.class)
public abstract class DimensionTypeMixin {
	@Inject(method = "timeOfDay", at = @At("RETURN"), cancellable = true)
	private void dragonminez$porungaNight(long dayTime, CallbackInfoReturnable<Float> cir) {
		if (!CustomSpecialEffects.NamekEffects.isPorungaNightActive() || !RenderSystem.isOnRenderThread()) return;
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null || level.dimensionType() != (Object) this || !NamekDimension.NAMEK_KEY.equals(level.dimension())) return;
		cir.setReturnValue(CustomSpecialEffects.NamekEffects.applyPorungaNight(cir.getReturnValueF()));
	}
}
