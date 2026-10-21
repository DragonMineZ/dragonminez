package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class ReviveTargetsS2C {

	public record Entry(UUID id, String name) {}

	private final List<Entry> entries;

	public ReviveTargetsS2C(List<Entry> entries) {
		this.entries = entries;
	}

	public ReviveTargetsS2C(FriendlyByteBuf buf) {
		int count = buf.readVarInt();
		this.entries = new ArrayList<>(count);
		for (int i = 0; i < count; i++) entries.add(new Entry(buf.readUUID(), buf.readUtf()));
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(entries.size());
		for (Entry entry : entries) {
			buf.writeUUID(entry.id());
			buf.writeUtf(entry.name());
		}
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		NetworkEvent.Context context = ctx.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClientPacketHandler.handleReviveTargets(entries)));
		context.setPacketHandled(true);
	}
}
