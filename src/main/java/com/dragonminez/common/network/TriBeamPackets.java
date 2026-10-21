package com.dragonminez.common.network;

import com.dragonminez.client.events.ClientStatsEvents;
import com.dragonminez.server.events.players.combat.KiTechniqueHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class TriBeamPackets {

	private TriBeamPackets() {}

	public static class FollowUpWindowS2C {
		private final int windowTicks;

		public FollowUpWindowS2C(int windowTicks) {
			this.windowTicks = windowTicks;
		}

		public static FollowUpWindowS2C decode(FriendlyByteBuf buf) {
			return new FollowUpWindowS2C(buf.readVarInt());
		}

		public void encode(FriendlyByteBuf buf) {
			buf.writeVarInt(Math.max(0, windowTicks));
		}

		public void handle(Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
					() -> () -> ClientStatsEvents.setTriBeamFollowUpWindow(windowTicks)));
			ctx.get().setPacketHandled(true);
		}
	}

	public static class FollowUpC2S {

		public FollowUpC2S() {}

		public static FollowUpC2S decode(FriendlyByteBuf buf) {
			return new FollowUpC2S();
		}

		public void encode(FriendlyByteBuf buf) {}

		public void handle(Supplier<NetworkEvent.Context> ctx) {
			NetworkEvent.Context context = ctx.get();
			context.enqueueWork(() -> {
				ServerPlayer player = context.getSender();
				if (player == null) return;
				if (!PacketRateLimiter.allow(player.getUUID(), "tri_beam_follow_up", player.level().getGameTime(), 4L)) return;
				KiTechniqueHandler.TriBeam.requestFollowUp(player);
			});
			context.setPacketHandled(true);
		}
	}
}
