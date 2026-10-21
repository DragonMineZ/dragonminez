package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenMinigameS2C {
	private final String minigameId;

	public OpenMinigameS2C(String minigameId) {
		this.minigameId = minigameId;
	}

	public OpenMinigameS2C(FriendlyByteBuf buf) {
		this.minigameId = buf.readUtf(32);
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeUtf(minigameId, 32);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleOpenMinigame(minigameId)));
		ctx.get().setPacketHandled(true);
	}
}
