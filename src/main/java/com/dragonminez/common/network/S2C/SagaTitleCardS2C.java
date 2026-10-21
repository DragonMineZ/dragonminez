package com.dragonminez.common.network.S2C;

import com.dragonminez.client.gui.hud.SagaTitleCardHUD;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Triggers the cinematic saga title-card splash when a player starts a saga's first quest. */
@Getter
public class SagaTitleCardS2C {

	private final String sagaName;

	public SagaTitleCardS2C(String sagaName) {
		this.sagaName = sagaName == null ? "" : sagaName;
	}

	public static void encode(SagaTitleCardS2C msg, FriendlyByteBuf buf) {
		buf.writeUtf(msg.sagaName);
	}

	public static SagaTitleCardS2C decode(FriendlyByteBuf buf) {
		return new SagaTitleCardS2C(buf.readUtf());
	}

	public static void handle(SagaTitleCardS2C msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> SagaTitleCardHUD.show(msg.sagaName)));
		ctx.get().setPacketHandled(true);
	}
}
