package com.dragonminez.common.compat;

import io.netty.handler.codec.DecoderException;

import java.io.IOException;

/**
 * Recognises foreign UDP traffic arriving on a Sable channel.
 *
 * <p>A UDP port receives whatever the network sends at it, so an open Sable channel picks up stray
 * datagrams that are not Sable packets at all. Sable's {@code exceptionCaught} escalates those, and
 * the two {@code SableUDPChannelHandler*Mixin}s exist to swallow them instead.
 *
 * <p><b>This lives outside the mixin package on purpose.</b> Both mixins need the same test, but a
 * mixin class may only declare {@code private} static methods — a package-private one makes Mixin
 * reject the entire mixin at apply time with "contains non-private static method", which silently
 * removed the UDP noise handling and left connections failing their keep-alives and dropping to
 * TCP. A plain class on the classpath is callable from both and carries no such restriction.
 */
public final class SableUdpNoise {

	private SableUdpNoise() {
	}

	/** Whether this failure is just a non-Sable datagram being decoded as one. */
	public static boolean isInvalidPacketIdNoise(Throwable cause) {
		Throwable t = cause;
		while (t != null) {
			if (t instanceof DecoderException || t instanceof IOException) {
				String msg = t.getMessage();
				if (msg != null && msg.contains("invalid packet ID")) {
					return true;
				}
			}
			t = t.getCause();
		}
		return false;
	}
}
