package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import com.dragonminez.common.network.ITTargetEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OpenITMenuS2C {

	private final List<ITTargetEntry> entries;

	public OpenITMenuS2C(List<ITTargetEntry> entries) {
		this.entries = entries;
	}

	public OpenITMenuS2C(FriendlyByteBuf buf) {
		int count = buf.readVarInt();
		this.entries = new ArrayList<>(count);
		for (int i = 0; i < count; i++) entries.add(ITTargetEntry.read(buf));
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(entries.size());
		for (ITTargetEntry entry : entries) entry.write(buf);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		NetworkEvent.Context context = ctx.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClientPacketHandler.handleOpenITMenu(entries)));
		context.setPacketHandled(true);
	}
}
