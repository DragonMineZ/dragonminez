package com.dragonminez.common.network.S2C;

import com.dragonminez.client.render.effects.ClawSlashEffect;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClawSlashVfxS2C {

	private final double x;
	private final double y;
	private final double z;
	private final float scale;
	private final int color;
	private final int lifetime;
	private final int count;
	private final boolean burst;

	public ClawSlashVfxS2C(double x, double y, double z, float scale, int color, int lifetime, int count, boolean burst) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.scale = scale;
		this.color = color;
		this.lifetime = lifetime;
		this.count = count;
		this.burst = burst;
	}

	public ClawSlashVfxS2C(FriendlyByteBuf buf) {
		this.x = buf.readDouble();
		this.y = buf.readDouble();
		this.z = buf.readDouble();
		this.scale = buf.readFloat();
		this.color = buf.readInt();
		this.lifetime = buf.readVarInt();
		this.count = buf.readVarInt();
		this.burst = buf.readBoolean();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeFloat(scale);
		buf.writeInt(color);
		buf.writeVarInt(lifetime);
		buf.writeVarInt(count);
		buf.writeBoolean(burst);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClawSlashEffect.spawn(x, y, z, scale, color, lifetime, count, burst)));
		ctx.get().setPacketHandled(true);
	}
}
