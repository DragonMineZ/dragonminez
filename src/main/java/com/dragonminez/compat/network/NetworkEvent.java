package com.dragonminez.compat.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Compatibility shim for Forge NetworkEvent.Context.
 */
public final class NetworkEvent {
	private NetworkEvent() {}

	public static final class Context {
		private final IPayloadContext payloadContext;
		private final ServerPlayer sender;

		public Context(IPayloadContext payloadContext) {
			this.payloadContext = payloadContext;
			this.sender = payloadContext.player() instanceof ServerPlayer sp ? sp : null;
		}

		public Context(ServerPlayer sender) {
			this.payloadContext = null;
			this.sender = sender;
		}

		public void enqueueWork(Runnable task) {
			if (payloadContext != null) {
				payloadContext.enqueueWork(task);
			} else {
				task.run();
			}
		}

		public void setPacketHandled(boolean handled) {
			// no-op on NeoForge payload system
		}

		public ServerPlayer getSender() {
			return sender;
		}

		public IPayloadContext getPayloadContext() {
			return payloadContext;
		}
	}

	public static Supplier<Context> wrap(IPayloadContext ctx) {
		Context context = new Context(ctx);
		return () -> context;
	}
}
