package com.dragonminez.common.network.S2C;

import com.dragonminez.client.animation.IPlayerAnimatable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
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
		ctx.get().enqueueWork(() -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level != null) {
				Player player = mc.level.getPlayerByUUID(playerUUID);
				if (player instanceof AbstractClientPlayer clientPlayer && clientPlayer instanceof IPlayerAnimatable animatable) {
					animatable.dragonminez$setCreativeFlying(isFlying);
				}
			}
		});
		ctx.get().setPacketHandled(true);
		return true;
	}
}
