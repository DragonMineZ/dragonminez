package com.dragonminez.common.network.S2C;

import com.dragonminez.client.animation.IPlayerAnimatable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class TriggerAnimationS2C {

	public enum AnimationType {
		EVASION, DASH, KI_BLAST_SHOT, COMBO
	}

	private final UUID playerUUID;
	private final AnimationType animationType;
	private final int variant;
	private final int entityId;

	public TriggerAnimationS2C(UUID playerUUID, AnimationType animationType, int variant) {
		this.playerUUID = playerUUID;
		this.animationType = animationType;
		this.variant = variant;
		this.entityId = -1;
	}

	public TriggerAnimationS2C(UUID playerUUID, AnimationType animationType, int variant, int entityId) {
		this.playerUUID = playerUUID;
		this.animationType = animationType;
		this.variant = variant;
		this.entityId = entityId;
	}

	public TriggerAnimationS2C(FriendlyByteBuf buffer) {
		this.playerUUID = buffer.readUUID();
		this.animationType = buffer.readEnum(AnimationType.class);
		this.variant = buffer.readInt();
		this.entityId = buffer.readInt();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUUID(playerUUID);
		buffer.writeEnum(animationType);
		buffer.writeInt(variant);
		buffer.writeInt(entityId);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			if (Minecraft.getInstance().level != null) {
				Player player = Minecraft.getInstance().level.getPlayerByUUID(playerUUID);
				if (player instanceof AbstractClientPlayer clientPlayer && clientPlayer instanceof IPlayerAnimatable animatable) {
					switch (animationType) {
						case EVASION -> animatable.dragonminez$triggerEvasion();
						case DASH -> animatable.dragonminez$triggerDash(variant);
						case KI_BLAST_SHOT -> animatable.dragonminez$setShootingKi(variant == 0);
						case COMBO -> animatable.dragonminez$triggerCombo(variant);
					}
				}
			}
		}));
		context.setPacketHandled(true);
	}
}
