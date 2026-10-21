package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DragonSkyS2C {
	private final ResourceLocation dimension;
	private final boolean active;

	public DragonSkyS2C(ResourceLocation dimension, boolean active) {
		this.dimension = dimension;
		this.active = active;
	}

	public DragonSkyS2C(FriendlyByteBuf buf) {
		this.dimension = buf.readResourceLocation();
		this.active = buf.readBoolean();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeResourceLocation(dimension);
		buf.writeBoolean(active);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClientPacketHandler.handleDragonSky(dimension, active)));
		context.setPacketHandled(true);
	}
}
