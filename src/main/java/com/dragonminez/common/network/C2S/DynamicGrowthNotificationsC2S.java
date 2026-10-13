package com.dragonminez.common.network.C2S;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.PacketRateLimiter;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DynamicGrowthNotificationsC2S {

	private final boolean enabled;

	public DynamicGrowthNotificationsC2S(boolean enabled) {
		this.enabled = enabled;
	}

	public DynamicGrowthNotificationsC2S(FriendlyByteBuf buffer) {
		this.enabled = buffer.readBoolean();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBoolean(enabled);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;
			if (!PacketRateLimiter.allow(player.getUUID(), "dynamicgrowthnotificationsc2s", player.level().getGameTime(), 4)) return;
			if (!ConfigManager.getServerConfig().getDynamicGrowth().isEnabled()) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				data.getDynamicGrowth().setStatGainNotificationsEnabled(enabled);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		});
		context.setPacketHandled(true);
	}
}
