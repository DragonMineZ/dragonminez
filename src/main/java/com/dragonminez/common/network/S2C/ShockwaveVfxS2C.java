package com.dragonminez.common.network.S2C;

import com.dragonminez.client.render.effects.ShockwaveEffect;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ShockwaveVfxS2C {

	private final double x;
	private final double y;
	private final double z;
	private final float scale;
	private final int color;
	private final int lifetime;

	public ShockwaveVfxS2C(double x, double y, double z, float scale, int color, int lifetime) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.scale = scale;
		this.color = color;
		this.lifetime = lifetime;
	}

	public ShockwaveVfxS2C(FriendlyByteBuf buf) {
		this.x = buf.readDouble();
		this.y = buf.readDouble();
		this.z = buf.readDouble();
		this.scale = buf.readFloat();
		this.color = buf.readInt();
		this.lifetime = buf.readVarInt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeFloat(scale);
		buf.writeInt(color);
		buf.writeVarInt(lifetime);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ShockwaveEffect.spawn(x, y, z, scale, color, lifetime)));
		ctx.get().setPacketHandled(true);
	}
}
