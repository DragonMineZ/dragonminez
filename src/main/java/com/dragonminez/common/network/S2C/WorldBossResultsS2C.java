package com.dragonminez.common.network.S2C;

import com.dragonminez.client.gui.worldboss.WorldBossResultsScreen;
import com.dragonminez.common.worldboss.WorldBossResults;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WorldBossResultsS2C {
	private final WorldBossResults results;

	public WorldBossResultsS2C(WorldBossResults results) {
		this.results = results;
	}

	public WorldBossResultsS2C(FriendlyByteBuf buf) {
		this.results = WorldBossResults.decode(buf);
	}

	public void encode(FriendlyByteBuf buf) {
		results.encode(buf);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> WorldBossResultsScreen.open(results)));
		ctx.get().setPacketHandled(true);
	}
}
