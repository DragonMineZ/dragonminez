package com.dragonminez.common.network.C2S;

import com.dragonminez.server.events.players.CombatEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DashC2S {
	private final float xInput;
	private final float zInput;
	private final boolean isDoubleDash;

	public DashC2S(float xInput, float zInput, boolean isDoubleDash) {
		this.xInput = xInput;
		this.zInput = zInput;
		this.isDoubleDash = isDoubleDash;
	}

	public DashC2S(FriendlyByteBuf buffer) {
		this.xInput = buffer.readFloat();
		this.zInput = buffer.readFloat();
		this.isDoubleDash = buffer.readBoolean();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeFloat(xInput);
		buffer.writeFloat(zInput);
		buffer.writeBoolean(isDoubleDash);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player != null) {
				CombatEvent.handleDash(player, xInput, zInput, isDoubleDash);
			}
		});
		context.setPacketHandled(true);
	}
}
