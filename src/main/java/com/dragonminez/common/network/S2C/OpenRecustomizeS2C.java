package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenRecustomizeS2C {
	public OpenRecustomizeS2C() {
	}

	public OpenRecustomizeS2C(FriendlyByteBuf buf) {
	}

	public void encode(FriendlyByteBuf buf) {
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientPacketHandler::handleOpenRecustomizePacket));
		ctx.get().setPacketHandled(true);
	}
}
