package com.dragonminez.common.network.S2C;

import com.dragonminez.client.systems.impactframes.ImpactFrame;
import com.dragonminez.client.systems.impactframes.ImpactFramesHandler;
import com.dragonminez.common.config.ConfigManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TriggerImpactFrameS2C {

	private final float threshold;
	private final float lerp;
	private final int duration;
	private final boolean invert;

	public TriggerImpactFrameS2C(float threshold, float lerp, int duration, boolean invert) {
		this.threshold = threshold;
		this.lerp = lerp;
		this.duration = duration;
		this.invert = invert;
	}

	public TriggerImpactFrameS2C(FriendlyByteBuf buf) {
		this.threshold = buf.readFloat();
		this.lerp = buf.readFloat();
		this.duration = buf.readInt();
		this.invert = buf.readBoolean();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeFloat(threshold);
		buf.writeFloat(lerp);
		buf.writeInt(duration);
		buf.writeBoolean(invert);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			if (ConfigManager.getUserConfig().isImpactFramesEnabled()) {
				ImpactFramesHandler.addImpactFrame(new ImpactFrame(threshold, lerp, duration, invert));
			}
		});
		ctx.get().setPacketHandled(true);
	}
}