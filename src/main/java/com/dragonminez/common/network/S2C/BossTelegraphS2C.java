package com.dragonminez.common.network.S2C;

import com.dragonminez.client.render.effects.TelegraphEffect;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BossTelegraphS2C {

	private final double x;
	private final double y;
	private final double z;
	private final float radius;
	private final int color;
	private final int lifetime;
	private final int followEntityId;

	public BossTelegraphS2C(double x, double y, double z, float radius, int color, int lifetime, int followEntityId) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.radius = radius;
		this.color = color;
		this.lifetime = lifetime;
		this.followEntityId = followEntityId;
	}

	public BossTelegraphS2C(FriendlyByteBuf buf) {
		this.x = buf.readDouble();
		this.y = buf.readDouble();
		this.z = buf.readDouble();
		this.radius = buf.readFloat();
		this.color = buf.readInt();
		this.lifetime = buf.readVarInt();
		this.followEntityId = buf.readInt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeFloat(radius);
		buf.writeInt(color);
		buf.writeVarInt(lifetime);
		buf.writeInt(followEntityId);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> TelegraphEffect.spawn(x, y, z, radius, color, lifetime, followEntityId)));
		ctx.get().setPacketHandled(true);
	}
}
