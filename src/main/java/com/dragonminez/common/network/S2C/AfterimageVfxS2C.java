package com.dragonminez.common.network.S2C;

import com.dragonminez.client.render.effects.AfterimageEffect;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AfterimageVfxS2C {

	private static final int MAX_IMAGES = 8;

	private final int entityId;
	private final int durationTicks;
	private final Vec3[] positions;
	private final float[] yaws;
	private final boolean breakLockOn;

	public AfterimageVfxS2C(int entityId, int durationTicks, Vec3[] positions, float[] yaws, boolean breakLockOn) {
		this.entityId = entityId;
		this.durationTicks = durationTicks;
		this.positions = positions;
		this.yaws = yaws;
		this.breakLockOn = breakLockOn;
	}

	public AfterimageVfxS2C(FriendlyByteBuf buf) {
		this.entityId = buf.readInt();
		this.durationTicks = buf.readInt();
		int count = Math.min(MAX_IMAGES, Math.max(0, buf.readByte()));
		this.positions = new Vec3[count];
		this.yaws = new float[count];
		for (int i = 0; i < count; i++) {
			this.positions[i] = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
			this.yaws[i] = buf.readFloat();
		}
		this.breakLockOn = buf.readBoolean();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeInt(entityId);
		buf.writeInt(durationTicks);
		int count = Math.min(MAX_IMAGES, Math.min(positions.length, yaws.length));
		buf.writeByte(count);
		for (int i = 0; i < count; i++) {
			buf.writeDouble(positions[i].x);
			buf.writeDouble(positions[i].y);
			buf.writeDouble(positions[i].z);
			buf.writeFloat(yaws[i]);
		}
		buf.writeBoolean(breakLockOn);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> AfterimageEffect.start(entityId, durationTicks, positions, yaws, breakLockOn)));
		ctx.get().setPacketHandled(true);
	}
}
