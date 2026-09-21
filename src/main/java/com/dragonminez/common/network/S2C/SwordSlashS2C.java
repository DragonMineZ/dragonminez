package com.dragonminez.common.network.S2C;

import com.dragonminez.client.render.effects.SwordSlashEffect;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SwordSlashS2C {

	private final double x, y, z;
	private final float dx, dy, dz;
	private final float speed;
	private final float roll;
	private final float radius;
	private final int color;
	private final int lifetime;

	public SwordSlashS2C(double x, double y, double z, float dx, float dy, float dz,
						 float speed, float roll, float radius, int color, int lifetime) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		this.speed = speed;
		this.roll = roll;
		this.radius = radius;
		this.color = color;
		this.lifetime = lifetime;
	}

	public SwordSlashS2C(FriendlyByteBuf buf) {
		this.x = buf.readDouble();
		this.y = buf.readDouble();
		this.z = buf.readDouble();
		this.dx = buf.readFloat();
		this.dy = buf.readFloat();
		this.dz = buf.readFloat();
		this.speed = buf.readFloat();
		this.roll = buf.readFloat();
		this.radius = buf.readFloat();
		this.color = buf.readInt();
		this.lifetime = buf.readVarInt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeFloat(dx);
		buf.writeFloat(dy);
		buf.writeFloat(dz);
		buf.writeFloat(speed);
		buf.writeFloat(roll);
		buf.writeFloat(radius);
		buf.writeInt(color);
		buf.writeVarInt(lifetime);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> SwordSlashEffect.spawn(x, y, z, dx, dy, dz, speed, roll, radius, color, lifetime)));
		ctx.get().setPacketHandled(true);
	}
}
