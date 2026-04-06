package com.dragonminez.common.network.C2S;

import com.dragonminez.server.events.players.CombatEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CombatStylePreferenceC2S {
	private final boolean useDMZCombatStyle;

	public CombatStylePreferenceC2S(boolean useDMZCombatStyle) {
		this.useDMZCombatStyle = useDMZCombatStyle;
	}

	public CombatStylePreferenceC2S(FriendlyByteBuf buffer) {
		this.useDMZCombatStyle = buffer.readBoolean();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBoolean(useDMZCombatStyle);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;
			CombatEvent.setCombatStyleEnabled(player, useDMZCombatStyle);
		});
		context.setPacketHandled(true);
	}
}

