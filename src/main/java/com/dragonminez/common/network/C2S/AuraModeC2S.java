package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.PacketRateLimiter;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AuraModeC2S {

	private static final int TYPE_LENGTH = 32;

	private final boolean aura3D;
	private final String aura3DType;

	public AuraModeC2S(boolean aura3D, String aura3DType) {
		this.aura3D = aura3D;
		this.aura3DType = aura3DType;
	}

	public AuraModeC2S(FriendlyByteBuf buffer) {
		this.aura3D = buffer.readBoolean();
		this.aura3DType = buffer.readUtf(TYPE_LENGTH);
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBoolean(aura3D);
		buffer.writeUtf(aura3DType != null ? aura3DType : "", TYPE_LENGTH);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;
			if (!PacketRateLimiter.allow(player.getUUID(), "auraMode", player.level().getGameTime(), 5L)) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				var character = data.getCharacter();
				String previousType = character.getAura3DType();
				character.setAura3DType(aura3DType);
				if (character.isAura3D() == aura3D && previousType.equals(character.getAura3DType())) return;
				character.setAura3D(aura3D);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		});
		context.setPacketHandled(true);
	}
}
