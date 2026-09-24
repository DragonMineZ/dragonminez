package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@Getter
public class BeamClashStateS2C {
	private final boolean active;
	private final long startGameTime;
	private final long meterSeed;
	private final float advantage;
	private final int selfColor;
	private final int foeColor;
	private final int opponentEntityId;
	private final double clashX;
	private final double clashY;
	private final double clashZ;
	private final int exhaustTicks;

	public BeamClashStateS2C(boolean active, long startGameTime, long meterSeed, float advantage, int selfColor, int foeColor,
	                         int opponentEntityId, double clashX, double clashY, double clashZ, int exhaustTicks) {
		this.active = active;
		this.startGameTime = startGameTime;
		this.meterSeed = meterSeed;
		this.advantage = advantage;
		this.selfColor = selfColor;
		this.foeColor = foeColor;
		this.opponentEntityId = opponentEntityId;
		this.clashX = clashX;
		this.clashY = clashY;
		this.clashZ = clashZ;
		this.exhaustTicks = exhaustTicks;
	}

	public static BeamClashStateS2C inactive(int exhaustTicks) {
		return new BeamClashStateS2C(false, 0L, 0L, 0.5f, 0xFFFFFF, 0xFFFFFF, -1, 0.0, 0.0, 0.0, exhaustTicks);
	}

	public Vec3 clashPoint() {
		return new Vec3(clashX, clashY, clashZ);
	}

	public static void encode(BeamClashStateS2C msg, FriendlyByteBuf buf) {
		buf.writeBoolean(msg.active);
		buf.writeLong(msg.startGameTime);
		buf.writeLong(msg.meterSeed);
		buf.writeFloat(msg.advantage);
		buf.writeInt(msg.selfColor);
		buf.writeInt(msg.foeColor);
		buf.writeInt(msg.opponentEntityId);
		buf.writeDouble(msg.clashX);
		buf.writeDouble(msg.clashY);
		buf.writeDouble(msg.clashZ);
		buf.writeVarInt(msg.exhaustTicks);
	}

	public static BeamClashStateS2C decode(FriendlyByteBuf buf) {
		return new BeamClashStateS2C(
				buf.readBoolean(),
				buf.readLong(),
				buf.readLong(),
				buf.readFloat(),
				buf.readInt(),
				buf.readInt(),
				buf.readInt(),
				buf.readDouble(),
				buf.readDouble(),
				buf.readDouble(),
				buf.readVarInt()
		);
	}

	public static void handle(BeamClashStateS2C msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClientPacketHandler.handleBeamClashState(msg)));
		ctx.get().setPacketHandled(true);
	}
}
