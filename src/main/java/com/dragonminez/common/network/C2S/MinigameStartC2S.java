package com.dragonminez.common.network.C2S;

import com.dragonminez.common.training.MinigameOrigin;
import com.dragonminez.common.training.MinigameSessionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MinigameStartC2S {
	private final String minigameId;
	private final MinigameOrigin origin;
	private final int challengeStage;
	private final String option;
	private final byte[] payload;

	public MinigameStartC2S(String minigameId, MinigameOrigin origin, int challengeStage, String option, byte[] payload) {
		this.minigameId = minigameId;
		this.origin = origin;
		this.challengeStage = challengeStage;
		this.option = option == null ? "" : option;
		this.payload = payload == null ? new byte[0] : payload;
	}

	public MinigameStartC2S(FriendlyByteBuf buf) {
		this.minigameId = buf.readUtf(32);
		this.origin = MinigameOrigin.byId(buf.readByte());
		this.challengeStage = buf.readVarInt();
		this.option = buf.readUtf(128);
		this.payload = buf.readByteArray(1 << 18);
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeUtf(minigameId, 32);
		buf.writeByte(origin.ordinal());
		buf.writeVarInt(challengeStage);
		buf.writeUtf(option, 128);
		buf.writeByteArray(payload);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player != null) MinigameSessionManager.start(player, minigameId, origin, challengeStage, option, payload);
		});
		ctx.get().setPacketHandled(true);
	}
}
