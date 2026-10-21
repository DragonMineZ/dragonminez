package com.dragonminez.common.network.C2S;

import com.dragonminez.common.training.MinigameEvent;
import com.dragonminez.common.training.MinigameSessionManager;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Getter
public class MinigameInputC2S {
	public static final int MAX_EVENTS = 4096;

	private final int sessionId;
	private final int fromTick;
	private final int toTick;
	private final boolean finished;
	private final long checksum;
	private final List<MinigameEvent> events;
	private final boolean malformed;

	public MinigameInputC2S(int sessionId, int fromTick, int toTick, boolean finished, long checksum, List<MinigameEvent> events) {
		this.sessionId = sessionId;
		this.fromTick = fromTick;
		this.toTick = toTick;
		this.finished = finished;
		this.checksum = checksum;
		this.events = events;
		this.malformed = false;
	}

	public MinigameInputC2S(FriendlyByteBuf buf) {
		this.sessionId = buf.readVarInt();
		this.fromTick = buf.readVarInt();
		this.toTick = buf.readVarInt();
		this.finished = buf.readBoolean();
		this.checksum = buf.readLong();
		int count = buf.readVarInt();
		boolean bad = count < 0 || count > MAX_EVENTS;
		List<MinigameEvent> list = new ArrayList<>(bad ? 0 : count);
		int tick = fromTick;
		for (int i = 0; !bad && i < count; i++) {
			int delta = buf.readVarInt();
			if (delta < 0) {
				bad = true;
				break;
			}
			tick += delta;
			int type = buf.readUnsignedByte();
			int key = buf.readUnsignedByte();
			int frac = buf.readUnsignedByte();
			int x = 0, y = 0;
			if (type == MinigameEvent.CLICK) {
				x = buf.readShort();
				y = buf.readShort();
			}
			MinigameEvent event = new MinigameEvent(tick, type, key, frac, x, y);
			if (!event.isValid()) bad = true;
			else list.add(event);
		}
		this.events = list;
		this.malformed = bad;
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(sessionId);
		buf.writeVarInt(fromTick);
		buf.writeVarInt(toTick);
		buf.writeBoolean(finished);
		buf.writeLong(checksum);
		buf.writeVarInt(events.size());
		int previous = fromTick;
		for (MinigameEvent event : events) {
			buf.writeVarInt(Math.max(0, event.tick - previous));
			previous = event.tick;
			buf.writeByte(event.type);
			buf.writeByte(event.key);
			buf.writeByte(event.frac);
			if (event.type == MinigameEvent.CLICK) {
				buf.writeShort(event.x);
				buf.writeShort(event.y);
			}
		}
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player != null) MinigameSessionManager.input(player, this);
		});
		ctx.get().setPacketHandled(true);
	}
}
