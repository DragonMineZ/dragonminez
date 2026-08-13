package com.dragonminez.mixin.sable;

import com.dragonminez.common.compat.SableUdpNoise;
import io.netty.channel.ChannelHandlerContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin: do not escalate foreign UDP noise on Sable's server UDP channel.
 *
 * <p>The test itself lives in {@link SableUdpNoise} rather than here: a mixin may only declare
 * {@code private} static methods, and holding it here as a package-private static made Mixin reject
 * this whole mixin at apply time.
 */
@Mixin(targets = "dev.ryanhcode.sable.network.udp.handler.SableUDPChannelHandlerServer", remap = false)
public abstract class SableUDPChannelHandlerServerMixin {

	@Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
	private void dragonminez$ignoreForeignUdpNoise(ChannelHandlerContext ctx, Throwable cause, CallbackInfo ci) {
		if (SableUdpNoise.isInvalidPacketIdNoise(cause)) {
			ci.cancel();
		}
	}
}
