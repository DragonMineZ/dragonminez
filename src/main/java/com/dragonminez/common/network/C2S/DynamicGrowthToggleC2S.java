package com.dragonminez.common.network.C2S;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.extras.DynamicGrowthStat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DynamicGrowthToggleC2S {

	private final String stat;
	private final boolean enabled;

	public DynamicGrowthToggleC2S(String stat, boolean enabled) {
		this.stat = stat;
		this.enabled = enabled;
	}

	public DynamicGrowthToggleC2S(FriendlyByteBuf buffer) {
		this.stat = buffer.readUtf();
		this.enabled = buffer.readBoolean();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUtf(stat);
		buffer.writeBoolean(enabled);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;
			if (!ConfigManager.getServerConfig().getDynamicGrowth().isEnabled()) return;

			DynamicGrowthStat parsed;
			try {
				parsed = DynamicGrowthStat.valueOf(stat);
			} catch (IllegalArgumentException e) {
				return;
			}

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				data.getDynamicGrowth().setGrowthEnabled(parsed, enabled);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		});
		context.setPacketHandled(true);
	}
}
