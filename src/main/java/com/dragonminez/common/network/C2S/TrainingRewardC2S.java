package com.dragonminez.common.network.C2S;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TrainingRewardC2S {
	private final String minigameId;
	private final int levelsCleared;

	public TrainingRewardC2S(String minigameId, int levelsCleared) {
		this.minigameId = minigameId;
		this.levelsCleared = levelsCleared;
	}

	public TrainingRewardC2S(FriendlyByteBuf buf) {
		this.minigameId = buf.readUtf(32);
		this.levelsCleared = buf.readInt();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeUtf(minigameId, 32);
		buf.writeInt(levelsCleared);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().setPacketHandled(true);
	}
}
