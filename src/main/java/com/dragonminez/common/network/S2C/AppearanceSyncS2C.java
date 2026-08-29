package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import com.dragonminez.compat.DistExecutor;
import com.dragonminez.compat.network.NetworkEvent;

import java.util.function.Supplier;

public class AppearanceSyncS2C {
	private final int playerId;
	private final CompoundTag nbt;

	public AppearanceSyncS2C(ServerPlayer player) {
		this.playerId = player.getId();
		this.nbt = new CompoundTag();
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			this.nbt.put("Character", data.getCharacter().save());
		});
	}

	public AppearanceSyncS2C(int playerId, CompoundTag nbt) {
		this.playerId = playerId;
		this.nbt = nbt;
	}

	public static void encode(AppearanceSyncS2C msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.playerId);
		buf.writeNbt(msg.nbt);
	}

	public static AppearanceSyncS2C decode(FriendlyByteBuf buf) {
		return new AppearanceSyncS2C(buf.readInt(), buf.readNbt());
	}

	public static void handle(AppearanceSyncS2C msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleAppearanceSyncPacket(msg.playerId, msg.nbt)));
		ctx.get().setPacketHandled(true);
	}
}
