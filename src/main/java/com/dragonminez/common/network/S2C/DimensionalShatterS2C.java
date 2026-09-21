package com.dragonminez.common.network.S2C;

import com.dragonminez.client.render.effects.DimensionalShatterEffect;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DimensionalShatterS2C {

	private final int entityId;
	private final double x;
	private final double y;
	private final double z;
	private final float width;
	private final float height;
	private final boolean assemble;

	public DimensionalShatterS2C(int entityId, double x, double y, double z, float width, float height, boolean assemble) {
		this.entityId = entityId;
		this.x = x;
		this.y = y;
		this.z = z;
		this.width = width;
		this.height = height;
		this.assemble = assemble;
	}

	public DimensionalShatterS2C(FriendlyByteBuf buf) {
		this.entityId = buf.readVarInt();
		this.x = buf.readDouble();
		this.y = buf.readDouble();
		this.z = buf.readDouble();
		this.width = buf.readFloat();
		this.height = buf.readFloat();
		this.assemble = buf.readBoolean();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(entityId);
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeFloat(width);
		buf.writeFloat(height);
		buf.writeBoolean(assemble);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> DimensionalShatterEffect.spawn(entityId, x, y, z, width, height, assemble)));
		ctx.get().setPacketHandled(true);
	}
}
