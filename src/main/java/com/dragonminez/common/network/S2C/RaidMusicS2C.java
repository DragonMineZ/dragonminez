package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RaidMusicS2C {

	private final String soundId;

	public RaidMusicS2C(String soundId) {
		this.soundId = soundId == null ? "" : soundId;
	}

	public RaidMusicS2C(FriendlyByteBuf buf) {
		this.soundId = buf.readUtf();
	}

	public static void encode(RaidMusicS2C msg, FriendlyByteBuf buf) {
		buf.writeUtf(msg.soundId);
	}

	public static void handle(RaidMusicS2C msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClientPacketHandler.handleRaidMusic(msg.soundId)));
		ctx.get().setPacketHandled(true);
	}
}
