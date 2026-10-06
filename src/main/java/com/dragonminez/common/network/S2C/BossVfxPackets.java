package com.dragonminez.common.network.S2C;

import com.dragonminez.client.render.effects.GaleEffect;
import com.dragonminez.client.render.effects.TelegraphEffect;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class BossVfxPackets {

	private BossVfxPackets() {}

	public static class LaneTelegraphS2C {

		private final int ownerId;
		private final double startX;
		private final double startY;
		private final double startZ;
		private final double endX;
		private final double endY;
		private final double endZ;
		private final float halfWidth;
		private final int color;
		private final int lifetime;
		private final boolean locked;

		public LaneTelegraphS2C(int ownerId, double startX, double startY, double startZ, double endX, double endY, double endZ,
								float halfWidth, int color, int lifetime, boolean locked) {
			this.ownerId = ownerId;
			this.startX = startX;
			this.startY = startY;
			this.startZ = startZ;
			this.endX = endX;
			this.endY = endY;
			this.endZ = endZ;
			this.halfWidth = halfWidth;
			this.color = color;
			this.lifetime = lifetime;
			this.locked = locked;
		}

		public LaneTelegraphS2C(FriendlyByteBuf buf) {
			this.ownerId = buf.readInt();
			this.startX = buf.readDouble();
			this.startY = buf.readDouble();
			this.startZ = buf.readDouble();
			this.endX = buf.readDouble();
			this.endY = buf.readDouble();
			this.endZ = buf.readDouble();
			this.halfWidth = buf.readFloat();
			this.color = buf.readInt();
			this.lifetime = buf.readVarInt();
			this.locked = buf.readBoolean();
		}

		public void encode(FriendlyByteBuf buf) {
			buf.writeInt(ownerId);
			buf.writeDouble(startX);
			buf.writeDouble(startY);
			buf.writeDouble(startZ);
			buf.writeDouble(endX);
			buf.writeDouble(endY);
			buf.writeDouble(endZ);
			buf.writeFloat(halfWidth);
			buf.writeInt(color);
			buf.writeVarInt(lifetime);
			buf.writeBoolean(locked);
		}

		public void handle(Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
					() -> () -> TelegraphEffect.spawnLane(ownerId, startX, startY, startZ, endX, endY, endZ, halfWidth, color, lifetime, locked)));
			ctx.get().setPacketHandled(true);
		}
	}

	public static class GaleS2C {

		private final int sourceId;
		private final double x;
		private final double y;
		private final double z;
		private final float dirX;
		private final float dirZ;
		private final float radius;
		private final float intensity;
		private final int duration;

		public GaleS2C(int sourceId, double x, double y, double z, float dirX, float dirZ, float radius, float intensity, int duration) {
			this.sourceId = sourceId;
			this.x = x;
			this.y = y;
			this.z = z;
			this.dirX = dirX;
			this.dirZ = dirZ;
			this.radius = radius;
			this.intensity = intensity;
			this.duration = duration;
		}

		public GaleS2C(FriendlyByteBuf buf) {
			this.sourceId = buf.readInt();
			this.x = buf.readDouble();
			this.y = buf.readDouble();
			this.z = buf.readDouble();
			this.dirX = buf.readFloat();
			this.dirZ = buf.readFloat();
			this.radius = buf.readFloat();
			this.intensity = buf.readFloat();
			this.duration = buf.readVarInt();
		}

		public void encode(FriendlyByteBuf buf) {
			buf.writeInt(sourceId);
			buf.writeDouble(x);
			buf.writeDouble(y);
			buf.writeDouble(z);
			buf.writeFloat(dirX);
			buf.writeFloat(dirZ);
			buf.writeFloat(radius);
			buf.writeFloat(intensity);
			buf.writeVarInt(duration);
		}

		public void handle(Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
					() -> () -> GaleEffect.start(sourceId, x, y, z, dirX, dirZ, radius, intensity, duration)));
			ctx.get().setPacketHandled(true);
		}
	}
}
