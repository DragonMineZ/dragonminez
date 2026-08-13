package com.dragonminez.mixin.sable;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mixin soft-compat for Sable UDP decoder — <b>no home-grown packet-id table</b>.
 *
 * <p>Upstream already does the correct check:
 * {@code packetID >= SableUDPPacketType.VALUES.length} (enum-driven).
 * That throws {@code IOException: Received an invalid packet ID: …} for foreign UDP
 * (e.g. legacy query {@code 254}).
 *
 * <p>We only turn that throw into a silent drop. When Sable adds enum constants,
 * {@code VALUES.length} grows and valid ids keep working — we never hardcode 6 or 32.
 *
 * <p><b>Why this logs.</b> Dropping silently made a real failure undiagnosable: if the other end
 * speaks a Sable whose packet ids run past this build's enum, its keep-alives land here and vanish
 * without a trace, so the client never answers, the server gives up after ~25s and kicks it to TCP,
 * and not one line appears in any log. The drop is still right — genuine foreign traffic does arrive
 * on an open UDP port — but it has to be visible. A rate limit keeps a noisy port from flooding the
 * log while still showing the first few immediately.
 */
@Mixin(targets = "dev.ryanhcode.sable.network.udp.SableUDPPacketDecoder", remap = false)
public abstract class SableUDPPacketDecoderMixin {

	private static final Logger DRAGONMINEZ$LOGGER =
			LoggerFactory.getLogger("dragonminez/sable-udp");

	/** Show the first few in full, then one in every {@code THEREAFTER} to keep the log readable. */
	private static final long DRAGONMINEZ$ALWAYS_LOG_FIRST = 5L;
	private static final long DRAGONMINEZ$THEREAFTER = 200L;

	private static final AtomicLong DRAGONMINEZ$DROPPED = new AtomicLong();

	/**
	 * Sable's decode builds {@code new IOException(...)} only for ids outside
	 * {@code SableUDPPacketType.VALUES}. Cancel that path so the datagram is discarded without
	 * killing the UDP pipeline — but say so first.
	 */
	@Inject(
			method = "decode(Lio/netty/channel/ChannelHandlerContext;Lio/netty/channel/socket/DatagramPacket;Ljava/util/List;)V",
			at = @At(value = "NEW", target = "java/io/IOException"),
			cancellable = true,
			remap = false,
			require = 1
	)
	private void dragonminez$softDropUnknownEnumId(
			ChannelHandlerContext ctx,
			DatagramPacket msg,
			List<?> out,
			CallbackInfo ci
	) {
		long seen = DRAGONMINEZ$DROPPED.incrementAndGet();
		if (seen <= DRAGONMINEZ$ALWAYS_LOG_FIRST || seen % DRAGONMINEZ$THEREAFTER == 0L) {
			DRAGONMINEZ$LOGGER.warn(
					"Dropped a Sable UDP datagram from {} with unknown packet id {} ({} so far)."
							+ " If this is the other end of your connection rather than stray"
							+ " traffic, its Sable is speaking ids this build does not know -"
							+ " check both sides run the same Sable version.",
					dragonminez$sender(msg), dragonminez$packetId(msg), seen);
		}
		ci.cancel();
	}

	/**
	 * The id byte Sable just consumed.
	 *
	 * <p>Read absolutely rather than with {@code readByte()}: the reader index belongs to the method
	 * being injected into, and moving it would corrupt any later handling of this buffer.
	 */
	private static String dragonminez$packetId(DatagramPacket msg) {
		try {
			ByteBuf content = msg.content();
			int index = content.readerIndex() - 1;
			if (index < 0 || index >= content.writerIndex()) return "?";
			return Integer.toString(content.getUnsignedByte(index));
		} catch (Throwable ignored) {
			return "?";
		}
	}

	private static String dragonminez$sender(DatagramPacket msg) {
		try {
			return String.valueOf(msg.sender());
		} catch (Throwable ignored) {
			return "?";
		}
	}
}
