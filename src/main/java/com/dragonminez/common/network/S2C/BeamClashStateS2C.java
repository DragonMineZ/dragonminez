package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@Getter
public class BeamClashStateS2C {

	private final boolean active;
	private final float meterPhase;
	private final float sweetLow;
	private final float sweetHigh;
	private final float advantage; // 0..1, > 0.5 = winning
	private final int beamColor;
	private final int opponentEntityId; // for cinematic framing; -1 if none

	public BeamClashStateS2C(boolean active, float meterPhase, float sweetLow, float sweetHigh, float advantage, int beamColor, int opponentEntityId) {
		this.active = active;
		this.meterPhase = meterPhase;
		this.sweetLow = sweetLow;
		this.sweetHigh = sweetHigh;
		this.advantage = advantage;
		this.beamColor = beamColor;
		this.opponentEntityId = opponentEntityId;
	}

	public static BeamClashStateS2C inactive() {
		return new BeamClashStateS2C(false, 0.0f, 0.0f, 0.0f, 0.5f, 0xFFFFFF, -1);
	}

	public static void encode(BeamClashStateS2C msg, FriendlyByteBuf buf) {
		buf.writeBoolean(msg.active);
		buf.writeFloat(msg.meterPhase);
		buf.writeFloat(msg.sweetLow);
		buf.writeFloat(msg.sweetHigh);
		buf.writeFloat(msg.advantage);
		buf.writeInt(msg.beamColor);
		buf.writeInt(msg.opponentEntityId);
	}

	public static BeamClashStateS2C decode(FriendlyByteBuf buf) {
		return new BeamClashStateS2C(
				buf.readBoolean(),
				buf.readFloat(),
				buf.readFloat(),
				buf.readFloat(),
				buf.readFloat(),
				buf.readInt(),
				buf.readInt()
		);
	}

	public static void handle(BeamClashStateS2C msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClientPacketHandler.handleBeamClashState(msg)));
		ctx.get().setPacketHandled(true);
	}
}
