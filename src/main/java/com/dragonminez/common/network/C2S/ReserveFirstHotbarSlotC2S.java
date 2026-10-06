package com.dragonminez.common.network.C2S;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ReserveFirstHotbarSlotC2S {
	public static final String PLAYER_DATA_KEY = "dmz_reserve_first_hotbar_slot";
	private final boolean enabled;

	public ReserveFirstHotbarSlotC2S(boolean enabled) {
		this.enabled = enabled;
	}

	public ReserveFirstHotbarSlotC2S(FriendlyByteBuf buffer) {
		this.enabled = buffer.readBoolean();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBoolean(this.enabled);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player != null) player.getPersistentData().putBoolean(PLAYER_DATA_KEY, this.enabled);
		});
		ctx.get().setPacketHandled(true);
	}
}
