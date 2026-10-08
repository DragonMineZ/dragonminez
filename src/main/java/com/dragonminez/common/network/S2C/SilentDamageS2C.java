package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SilentDamageS2C {
	private final float amount;

	public SilentDamageS2C(float amount) {
		this.amount = amount;
	}

	public SilentDamageS2C(FriendlyByteBuf buf) {
		this.amount = buf.readFloat();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeFloat(amount);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClientPacketHandler.handleSilentDamage(amount)));
		context.setPacketHandled(true);
	}
}
