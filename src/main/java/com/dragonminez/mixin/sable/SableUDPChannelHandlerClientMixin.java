package com.dragonminez.mixin.sable;

import com.dragonminez.common.compat.SableUdpNoise;
import io.netty.channel.ChannelHandlerContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin: client-side mirror of {@link SableUDPChannelHandlerServerMixin}.
 */
@Mixin(targets = "dev.ryanhcode.sable.network.udp.handler.SableUDPChannelHandlerClient", remap = false)
public abstract class SableUDPChannelHandlerClientMixin {

	@Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
	private void dragonminez$ignoreForeignUdpNoise(ChannelHandlerContext ctx, Throwable cause, CallbackInfo ci) {
		if (SableUdpNoise.isInvalidPacketIdNoise(cause)) {
			ci.cancel();
		}
	}
}
