package com.dragonminez.common.network.C2S;

import com.dragonminez.server.events.players.combat.EvasionAttackHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EvasionCastC2S {

	private final String techniqueId;
	private final int targetId;

	public EvasionCastC2S(String techniqueId) {
		this(techniqueId, -1);
	}

	public EvasionCastC2S(String techniqueId, int targetId) {
		this.techniqueId = techniqueId;
		this.targetId = targetId;
	}

	public EvasionCastC2S(FriendlyByteBuf buf) {
		this.techniqueId = buf.readUtf();
		this.targetId = buf.readInt();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeUtf(this.techniqueId);
		buf.writeInt(this.targetId);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player != null) EvasionAttackHandler.cast(player, techniqueId, targetId);
		});
		ctx.get().setPacketHandled(true);
	}
}
