package com.dragonminez.common.network.C2S;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetReleaseLimitC2S {

	private final int limit;

	public SetReleaseLimitC2S(int limit) {
		this.limit = limit;
	}

	public SetReleaseLimitC2S(FriendlyByteBuf buffer) {
		this.limit = buffer.readVarInt();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeVarInt(limit);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;
			if (player.hasEffect(MainEffects.STUN.get())) return;
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				int potentialUnlockLevel = data.getSkills().hasSkill("potentialunlock") ? data.getSkills().getSkillLevel("potentialunlock") : 0;
				int maxRelease = 50 + (potentialUnlockLevel * 5);

				int newLimit;
				if (limit <= 0) {
					newLimit = 0;
				} else {
					int snapped = (limit / 5) * 5;
					newLimit = Math.max(5, Math.min(maxRelease, snapped));
				}

				data.getResources().setReleaseLimit(newLimit);
				if (newLimit > 0 && data.getResources().getPowerRelease() > newLimit) {
					data.getResources().setPowerRelease(newLimit);
				}

				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		});
		context.setPacketHandled(true);
	}
}
