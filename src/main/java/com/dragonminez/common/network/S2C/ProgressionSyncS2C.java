package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ProgressionSyncS2C {
	private final int playerId;
	private final CompoundTag nbt;

	public ProgressionSyncS2C(ServerPlayer player) {
		this.playerId = player.getId();
		this.nbt = new CompoundTag();
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			this.nbt.put("Stats", data.getStats().save());
			this.nbt.put("BonusStats", data.getBonusStats().save());
			this.nbt.put("Skills", data.getSkills().save());
			this.nbt.put("Techniques", data.getTechniques().save());
			this.nbt.put("PlayerQuestData", data.getPlayerQuestData().serializeNBT());
			this.nbt.put("Training", data.getTraining().save());
		});
	}

	public ProgressionSyncS2C(int playerId, CompoundTag nbt) {
		this.playerId = playerId;
		this.nbt = nbt;
	}

	public static void encode(ProgressionSyncS2C msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.playerId);
		buf.writeNbt(msg.nbt);
	}

	public static ProgressionSyncS2C decode(FriendlyByteBuf buf) {
		return new ProgressionSyncS2C(buf.readInt(), buf.readNbt());
	}

	public static void handle(ProgressionSyncS2C msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleStatsSyncPacket(msg.playerId, msg.nbt)));
		ctx.get().setPacketHandled(true);
	}
}