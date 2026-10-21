package com.dragonminez.common.network.C2S;

import com.dragonminez.common.combat.clash.BeamClashManager;
import com.dragonminez.common.network.PacketRateLimiter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BeamClashInputC2S {
	private final float pressTime;
	private final float marker;

	public BeamClashInputC2S(float pressTime, float marker) {
		this.pressTime = pressTime;
		this.marker = marker;
	}

	public BeamClashInputC2S(FriendlyByteBuf buf) {
		this.pressTime = buf.readFloat();
		this.marker = buf.readFloat();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeFloat(pressTime);
		buf.writeFloat(marker);
	}

	public static void handle(BeamClashInputC2S msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;
			if (!PacketRateLimiter.allow(player.getUUID(), "beam_clash_press", player.level().getGameTime(), 1L)) return;
			BeamClashManager.handlePlayerPress(player, msg.pressTime, msg.marker);
		});
		ctx.get().setPacketHandled(true);
	}
}
