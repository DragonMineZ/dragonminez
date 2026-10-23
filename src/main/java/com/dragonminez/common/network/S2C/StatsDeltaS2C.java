package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.dragonminez.compat.DistExecutor;
import com.dragonminez.compat.network.NetworkEvent;

import java.util.function.Supplier;

public class StatsDeltaS2C {
	public static final String CHARACTER_PATCH = "CharacterPatch";
	public static final String CHARACTER_REMOVED = "CharacterRemoved";

	private final int playerId;
	private final CompoundTag nbt;

	public StatsDeltaS2C(int playerId, CompoundTag nbt) {
		this.playerId = playerId;
		this.nbt = nbt;
	}

	public StatsDeltaS2C forTrackers() {
		return new StatsDeltaS2C(this.playerId, StatsSyncS2C.withoutQuestHistory(this.nbt));
	}

	public static boolean hasCharacterPatch(CompoundTag delta) {
		return delta.contains(CHARACTER_PATCH) || delta.contains(CHARACTER_REMOVED);
	}

	public static void applyCharacterPatch(CompoundTag character, CompoundTag delta, boolean copy) {
		CompoundTag patch = delta.getCompound(CHARACTER_PATCH);
		for (String key : patch.getAllKeys()) character.put(key, copy ? patch.get(key).copy() : patch.get(key));
		ListTag removed = delta.getList(CHARACTER_REMOVED, Tag.TAG_STRING);
		for (int i = 0; i < removed.size(); i++) character.remove(removed.getString(i));
	}

	public static void encode(StatsDeltaS2C msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.playerId);
		buf.writeNbt(msg.nbt);
	}

	public static StatsDeltaS2C decode(FriendlyByteBuf buf) {
		return new StatsDeltaS2C(buf.readInt(), buf.readNbt());
	}

	public static void handle(StatsDeltaS2C msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleStatsDeltaPacket(msg.playerId, msg.nbt)));
		ctx.get().setPacketHandled(true);
	}
}
