package com.dragonminez.common.network.S2C;

import com.dragonminez.client.systems.worldboss.ClientWorldBossContribution;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class WorldBossContributionS2C {
	public static final int MAX_ENTRIES = 32;

	public record Entry(UUID id, String name, float points, float share, int auraRgb, boolean knockedOut) {
		public void encode(FriendlyByteBuf buf) {
			buf.writeUUID(id);
			buf.writeUtf(name);
			buf.writeFloat(points);
			buf.writeFloat(share);
			buf.writeInt(auraRgb);
			buf.writeBoolean(knockedOut);
		}

		public static Entry decode(FriendlyByteBuf buf) {
			return new Entry(buf.readUUID(), buf.readUtf(), buf.readFloat(), buf.readFloat(), buf.readInt(), buf.readBoolean());
		}
	}

	private final String bossKey;
	private final String bossNameKey;
	private final long elapsedTicks;
	private final boolean finished;
	private final boolean cleared;
	private final List<Entry> entries;

	public WorldBossContributionS2C(String bossKey, String bossNameKey, long elapsedTicks, boolean finished, boolean cleared, List<Entry> entries) {
		this.bossKey = bossKey;
		this.bossNameKey = bossNameKey;
		this.elapsedTicks = elapsedTicks;
		this.finished = finished;
		this.cleared = cleared;
		this.entries = entries == null ? new ArrayList<>() : entries;
	}

	public WorldBossContributionS2C(FriendlyByteBuf buf) {
		this.bossKey = buf.readUtf();
		this.bossNameKey = buf.readUtf();
		this.elapsedTicks = buf.readVarLong();
		this.finished = buf.readBoolean();
		this.cleared = buf.readBoolean();
		int size = Math.min(buf.readVarInt(), MAX_ENTRIES);
		this.entries = new ArrayList<>(size);
		for (int i = 0; i < size; i++) entries.add(Entry.decode(buf));
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeUtf(bossKey);
		buf.writeUtf(bossNameKey);
		buf.writeVarLong(Math.max(0L, elapsedTicks));
		buf.writeBoolean(finished);
		buf.writeBoolean(cleared);
		int size = Math.min(entries.size(), MAX_ENTRIES);
		buf.writeVarInt(size);
		for (int i = 0; i < size; i++) entries.get(i).encode(buf);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClientWorldBossContribution.accept(bossKey, bossNameKey, elapsedTicks, finished, cleared, entries)));
		ctx.get().setPacketHandled(true);
	}
}
