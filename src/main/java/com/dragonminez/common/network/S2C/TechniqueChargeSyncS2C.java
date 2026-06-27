package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TechniqueChargeSyncS2C {
	private final int playerId;
	private final float percent;
	private final boolean charging;

	public TechniqueChargeSyncS2C(int playerId, float percent, boolean charging) {
		this.playerId = playerId;
		this.percent = percent;
		this.charging = charging;
	}

	public static void encode(TechniqueChargeSyncS2C msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.playerId);
		buf.writeFloat(msg.percent);
		buf.writeBoolean(msg.charging);
	}

	public static TechniqueChargeSyncS2C decode(FriendlyByteBuf buf) {
		return new TechniqueChargeSyncS2C(buf.readInt(), buf.readFloat(), buf.readBoolean());
	}

	public static void handle(TechniqueChargeSyncS2C msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClientPacketHandler.handleTechniqueChargeSync(msg.playerId, msg.percent, msg.charging)));
		ctx.get().setPacketHandled(true);
	}
}
