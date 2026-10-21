package com.dragonminez.common.network.S2C;

import com.dragonminez.client.render.effects.ImpactBurstEffect;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ImpactBurstVfxS2C {

	private final double x;
	private final double y;
	private final double z;
	private final float dirX;
	private final float dirY;
	private final float dirZ;
	private final float scale;
	private final int color1;
	private final int color2;
	private final boolean twoTone;
	private final int lifetime;

	public ImpactBurstVfxS2C(Vec3 pos, Vec3 direction, float scale, int color1, int color2, boolean twoTone, int lifetime) {
		this.x = pos.x;
		this.y = pos.y;
		this.z = pos.z;
		this.dirX = (float) direction.x;
		this.dirY = (float) direction.y;
		this.dirZ = (float) direction.z;
		this.scale = scale;
		this.color1 = color1;
		this.color2 = color2;
		this.twoTone = twoTone;
		this.lifetime = lifetime;
	}

	public ImpactBurstVfxS2C(FriendlyByteBuf buf) {
		this.x = buf.readDouble();
		this.y = buf.readDouble();
		this.z = buf.readDouble();
		this.dirX = buf.readFloat();
		this.dirY = buf.readFloat();
		this.dirZ = buf.readFloat();
		this.scale = buf.readFloat();
		this.color1 = buf.readInt();
		this.color2 = buf.readInt();
		this.twoTone = buf.readBoolean();
		this.lifetime = buf.readVarInt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeFloat(dirX);
		buf.writeFloat(dirY);
		buf.writeFloat(dirZ);
		buf.writeFloat(scale);
		buf.writeInt(color1);
		buf.writeInt(color2);
		buf.writeBoolean(twoTone);
		buf.writeVarInt(lifetime);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ImpactBurstEffect.spawn(x, y, z, dirX, dirY, dirZ, scale, color1, color2, twoTone, lifetime)));
		ctx.get().setPacketHandled(true);
	}
}
