package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.dragonminez.compat.DistExecutor;
import com.dragonminez.compat.network.NetworkEvent;

import java.util.function.Supplier;

public class TechniqueImportResultS2C {
	public enum Status {
		INVALID,
		NOT_ENOUGH_TP,
		IMPORTED
	}

	private final Status status;
	private final int value;

	public TechniqueImportResultS2C(Status status, int value) {
		this.status = status;
		this.value = value;
	}

	public TechniqueImportResultS2C(FriendlyByteBuf buf) {
		this.status = buf.readEnum(Status.class);
		this.value = buf.readInt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeEnum(status);
		buf.writeInt(value);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClientPacketHandler.handleTechniqueImportResult(status, value)));
		ctx.get().setPacketHandled(true);
	}
}
