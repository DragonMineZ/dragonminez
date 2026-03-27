package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PlayerAnimationsSync {
	private final UUID playerUUID;
	private final boolean isFlying;

	public PlayerAnimationsSync(UUID playerUUID, boolean isFlying) {
		this.playerUUID = playerUUID;
		this.isFlying = isFlying;
	}

	public PlayerAnimationsSync(FriendlyByteBuf buf) {
		this.playerUUID = buf.readUUID();
		this.isFlying = buf.readBoolean();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeUUID(playerUUID);
		buf.writeBoolean(isFlying);
	}

	public boolean handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClientPacketHandler.handlePlayerAnimationsSyncPacket(playerUUID, isFlying)));
		ctx.get().setPacketHandled(true);
		return true;
	}
}
