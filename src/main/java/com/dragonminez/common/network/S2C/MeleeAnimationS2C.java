package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MeleeAnimationS2C {

	private final int entityId;
	private final String animationName;
	private final boolean isOffhand;
	private final float speedMultiplier;

	public MeleeAnimationS2C(int entityId, String animationName, boolean isOffhand, float speedMultiplier) {
		this.entityId = entityId;
		this.animationName = animationName != null ? animationName : "";
		this.isOffhand = isOffhand;
		this.speedMultiplier = speedMultiplier;
	}

	public MeleeAnimationS2C(FriendlyByteBuf buffer) {
		this.entityId = buffer.readInt();
		this.animationName = buffer.readUtf();
		this.isOffhand = buffer.readBoolean();
		this.speedMultiplier = buffer.readFloat();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeInt(this.entityId);
		buffer.writeUtf(this.animationName);
		buffer.writeBoolean(this.isOffhand);
		buffer.writeFloat(this.speedMultiplier);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ClientPacketHandler.handleMeleeAnimationPacket(this.entityId, this.animationName, this.isOffhand, this.speedMultiplier);
		});
		ctx.get().setPacketHandled(true);
	}
}